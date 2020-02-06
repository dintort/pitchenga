package com.pitchenga;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.pitchenga.Defaults.*;
import static com.pitchenga.Interval.frt;
import static com.pitchenga.Interval.sxt;
import static com.pitchenga.Tone.*;

//fixme: Split view and controller
public class Pitchenga extends JFrame implements PitchDetectionHandler {

    private static final boolean debug = "true".equalsIgnoreCase(System.getProperty("com.pitchenga.debug"));
    private static final Pitch[] PITCHES = Pitch.values();
    private static final Tone[] TONES = Tone.values();
    private static final Key[] KEYS = Key.values();
    private static final Integer[] ALL_OCTAVES = Arrays.stream(PITCHES).map(Pitch::getOctave).distinct().toArray(Integer[]::new);
    private static final Integer MIN_OCTAVE = Arrays.stream(ALL_OCTAVES).min(Integer::compare).orElse(0);
    private static final Integer MAX_OCTAVE = Arrays.stream(ALL_OCTAVES).max(Integer::compare).orElse(0);
    private static final Tone[] CHROMATIC_SCALE = TONES;
    private static final Tone[] DIATONIC_SCALE = Arrays.stream(TONES).filter(Tone::isDiatonic).collect(Collectors.toList()).toArray(new Tone[0]);
    private static final Tone[] SHARPS_SCALE = Arrays.stream(TONES).filter(tone -> !tone.isDiatonic()).collect(Collectors.toList()).toArray(new Tone[0]);
    private static final Map<Integer, Key> KEY_BY_CODE = Arrays.stream(Key.values()).collect(Collectors.toMap(Key::getKeyEventCode, key -> key));
    public static final Font COURIER = new Font("Courier", Font.BOLD, 16);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService asyncExecutor = Executors.newSingleThreadScheduledExecutor();
    private final BlockingQueue<Runnable> keyQueue = new ArrayBlockingQueue<>(1);
    private final ExecutorService keyExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, keyQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    private final Random random = new Random();
    private volatile AudioDispatcher audioDispatcher;
    //fixme: +Selectors for instruments
    private final MidiChannel piano;
    private final MidiChannel brightPiano;
    private final MidiChannel guitar;

    private final AtomicReference<Tone> lastGuess = new AtomicReference<>(null);
    private volatile long lastGuessTimestampMs = System.currentTimeMillis();
    private final List<Pair<Pitch, Double>> guessQueue = new ArrayList<>();
    private final Queue<Pitch> riddleQueue = new LinkedBlockingQueue<>();
    private final AtomicReference<Pitch> riddle = new AtomicReference<>(null);
    private final AtomicReference<Pitch> prevRiddle = new AtomicReference<>(null);
    private volatile long riddleTimestampMs = System.currentTimeMillis();
    private volatile long penaltyRiddleTimestampMs = System.currentTimeMillis();
    private volatile boolean frozen = false; //fixme: Remove as playing on audio dispatcher thread?
    private final AtomicInteger idCounter = new AtomicInteger(-1);

    private final JPanel guessPanel = new JPanel();
    private final JLabel guessLabel = new JLabel();
    private final JPanel pitchyPanel = new JPanel();
    private final JSpinner penaltyFactorSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 9, 1));
    private final JToggleButton[] octaveToggles = new JToggleButton[ALL_OCTAVES.length];
    private final JSpinner[] toneSpinners = Arrays.stream(TONES)
            .map(tone -> new JSpinner(new SpinnerNumberModel(0, 0, 9, 1)))
            .collect(Collectors.toList()).toArray(new JSpinner[0]);
    private volatile boolean toneSpinnersFrozen = false; //fixme: Maybe use "frozen" instead
    private final JSpinner[] penaltySpinners = new JSpinner[TONES.length];
    private final List<List<Pair<Pitch, Pitch>>> penaltyLists = Arrays.stream(TONES).map(tone -> new ArrayList<Pair<Pitch, Pitch>>()).collect(Collectors.toList());
    private final Set<Pair<Pitch, Pitch>> penaltyReminders = new LinkedHashSet<>();
    private final JComboBox<PitchEstimationAlgorithm> pitchAlgoCombo = new JComboBox<>();
    private final JComboBox<Hinter> hinterCombo = new JComboBox<>();
    private final JComboBox<Pacer> pacerCombo = new JComboBox<>();
    private final JComboBox<RiddleRinger> riddleRingerCombo = new JComboBox<>();
    private final JComboBox<GuessRinger> guessRingerCombo = new JComboBox<>();
    private final JComboBox<Riddler> riddlerCombo = new JComboBox<>();
    private final JComboBox<Mixer.Info> inputCombo = new JComboBox<>();
    private final JToggleButton playButton = new JToggleButton();
    private final JToggleButton[] keyButtons = new JToggleButton[Key.values().length];
    private final JTextArea text = new JTextArea();
    //    private final JTextPane text = new JTextPane();
    private final Set<Pitch> pressedKeys = new HashSet<>(); // To ignore OS's key repeating when holding

    public Pitchenga() {
        super("Pitchenga");
        MidiChannel[] midiChannels = initMidi();
        piano = midiChannels[0];
        brightPiano = midiChannels[1];
        guitar = midiChannels[2];

        initGui(); //fixme: +Gui-less mode
        initKeyboard();
        updateMixer();
    }

    //fixme: Need simpler threading, sometimes multiple things happen at the same time
    private void play(Pitch guess) {
        if (frozen || !isPlaying()) {
            return;
        }
        Pitch riddle = riddle();
        if (riddle == null) {
            return;
        }
        boolean success = getPacer().check.apply(new Pair<>(guess, riddle));
        debug(String.format("Play: [%s] %s [%.2fHz] : %s", riddle, guess, riddle.getFrequency(), success));
        if (success) {
            if (penaltyRiddleTimestampMs != riddleTimestampMs) {
                Pitch prevRiddle = this.prevRiddle.get();
                List<Pair<Pitch, Pitch>> penaltyList = penaltyLists.get(riddle.getTone().ordinal());
                for (Iterator<Pair<Pitch, Pitch>> iterator = penaltyList.iterator(); iterator.hasNext(); ) {
                    Pair<Pitch, Pitch> penalty = iterator.next();
                    if (penalty.left.equals(prevRiddle) && penalty.right.equals(riddle)) {
                        debug("Removing penalty " + riddle + " after " + prevRiddle);
                        iterator.remove();
                        penaltyReminders.add(penalty);
                        break;
                    }
                }
                SwingUtilities.invokeLater(this::updatePenaltySpinners);
            }
            this.prevRiddle.set(riddle);
            this.riddle.set(null);

            Object[] fugue = getGuessRinger().ring.apply(riddle);
            fugue(guitar, fugue, true);
            keyQueue.clear();
            //fixme: This will stack overflow in the auto-play mode
            play(null);
        } else if (guess != null) {
            //fixme: Move to hinter
            if (System.currentTimeMillis() - riddleTimestampMs >= getHinter().delayMs) {
                SwingUtilities.invokeLater(() -> {
                    guessLabel.setText("    ");
                    pitchyPanel.setBackground(riddle.getTone().getColor());
                });
            }
            frozen = true;
            try {
                fugue(piano, getRiddleRinger().ring.apply(riddle), false);
                //fixme: Does not help since switched to midi, so the game plays with itself
//                Thread.sleep(500); //Otherwise the mic picks up the "tail" of the riddle sound from the speakers.
//            } catch (InterruptedException e) {
//                e.printStackTrace();
            } finally {
                frozen = false;
            }
        }
    }

    private Pitch riddle() {
        while (this.riddle.get() == null) {
            if (riddleQueue.size() == 0) {
                Riddler riddler = getRiddler();
                List<Pitch> riddles = riddler.riddleAction.apply(this);
                debug(" " + riddles + " are the next riddles, riddler=" + riddler);
                riddleQueue.addAll(riddles);
            }
            debug(riddleQueue + " is the riddle queue");
            Pitch riddle = riddleQueue.poll();
            if (riddle != null) {
                debug(" [" + riddle + "] is the new riddle");
                this.riddle.set(riddle);
                SwingUtilities.invokeLater(() -> {
                    guessLabel.setText("    ");
                    //fixme: Move this logic to Hinter
                    if (getHinter().equals(Hinter.Always)) {
                        pitchyPanel.setBackground(riddle.getTone().getColor());
                    }
                });
                frozen = true;
                try {
                    fugue(piano, getRiddleRinger().ring.apply(riddle), false);
                    //fixme
//                    Thread.sleep(1000); //Otherwise the mic picks up the "tail" of the riddle sound from the speakers.
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
                } finally {
                    frozen = false;
                }
                scheduleHintAndPenalty();
            }
        }
        return this.riddle.get();
    }

    //fixme: Audio stops working sometimes especially when GarageBand is running
    @Override
    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent event) {
        if (frozen) {
            return;
        }
        double rms = event.getRMS() * 100;
        try {
            if (pitchDetectionResult.getPitch() != -1) {
                Pitch guess = matchPitch(pitchDetectionResult.getPitch());
                if (guess != null) {
                    double maxRms = rms;
                    double rmsThreshold = 0.1;
                    if (guessQueue.size() < 2) {
                        guessQueue.add(new Pair<>(guess, rms));
                    } else {
                        boolean same = true;
                        for (Pair<Pitch, Double> pitchRms : guessQueue) {
                            if (!guess.equals(pitchRms.left)) {
                                same = false;
                                maxRms = Math.max(maxRms, pitchRms.right);
                                break;
                            }
                        }
                        if (same) {
                            if (maxRms > rmsThreshold) {
                                if (isPlaying()) {
                                    updateGuessColor(guess, pitchDetectionResult.getPitch(), pitchDetectionResult.getProbability(), rms);
                                }
                                SwingUtilities.invokeLater(() -> updatePianoButtons(guess.getTone().getKey()));
                                //fixme: +"Monitoring" mode with a toggle button
                                transcribe(guess, false);
                                play(guess);
                            }
                        } else {
                            guessQueue.clear();
                            guessQueue.add(new Pair<>(guess, rms));
                        }
                    }
                    if (!isPlaying() && maxRms > rmsThreshold) {
                        updateGuessColor(guess, pitchDetectionResult.getPitch(), pitchDetectionResult.getProbability(), rms);
                    }
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    //fixme: Add strobe tuner
    private void updateGuessColor(Pitch guess, float pitch, float probability, double rms) {
        double diff = pitch - guess.getFrequency();
        Pitch pitchy;
        if (diff < 0) {
            pitchy = transposePitch(guess, 0, -1);
        } else {
            pitchy = transposePitch(guess, 0, +1);
        }
        double pitchyDiff = Math.abs(guess.getFrequency() - pitchy.getFrequency());
        double accuracy = Math.abs(diff) / pitchyDiff;
        double pitchiness = accuracy * 20;
        Color guessColor;
        Color pitchyColor;
        if (Math.abs(diff) < 0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000314159) {
            guessColor = pitchyColor = guess.getTone().getColor();
        } else {
            //fixme: Unit test for interpolation direction
            // pitchyColor = interpolateColor(pitchiness, guess.getTone().getColor(), pitchy.getTone().getColor()); // E.g. this should fail or something
            // guessColor = interpolateColor(accuracy, guess.getTone().getColor(), pitchy.getTone().getColor());
            guessColor = interpolateColor(accuracy, guess.getTone().getColor(), pitchy.getTone().getColor());
            pitchyColor = interpolateColor(pitchiness, guess.getTone().getColor(), pitchy.getTone().getColor());
        }
        if (debug) {
            debug(String.format(" %s | pitch=%.2fHz | m=%.2f | rms=%.2f | diff=%.2f | pitchyDiff=%.2f | accuracy=%.2f | pitchiness=%.2f | guessRoundedColor=%s | pitchyColor=%s | guessColor=%s | borderColor=%s",
                    guess, pitch, probability, rms, diff, pitchyDiff, accuracy, pitchiness, info(guess.getTone().getColor()), info(pitchy.getTone().getColor()), info(guessColor), info(pitchyColor)));
        }
        SwingUtilities.invokeLater(() -> {
            guessLabel.setText(guess.getTone().getSpacedName());
            pitchyPanel.setBackground(guess.getTone().getColor());
            setColor(guessColor);
            pitchyPanel.setBorder(BorderFactory.createLineBorder(pitchyColor, 5));
        });
    }

    private void setColor(Color guessColor) {
        //fixme: Fix layout - gap between pitchy panel and textArea
        guessPanel.setBackground(guessColor);
        text.setBackground(guessColor);
    }

    private Color interpolateColor(double ratio, Color color1, Color color2) {
        if (ratio > 1) {
            ratio = 1;
        }
        int red = (int) (color2.getRed() * ratio + color1.getRed() * (1 - ratio));
        int green = (int) (color2.getGreen() * ratio + color1.getGreen() * (1 - ratio));
        int blue = (int) (color2.getBlue() * ratio + color1.getBlue() * (1 - ratio));
        return new Color(red, green, blue);
    }

    private Pitch matchPitch(float pitch) {
        Pitch guess = null;
        for (Pitch aPitch : PITCHES) {
            double diff = Math.abs(aPitch.getFrequency() - pitch);
            if (diff < 5) {
                if (guess != null) {
                    if (Math.abs(guess.getFrequency() - pitch) < diff) {
                        aPitch = guess;
                    }
                }
                guess = aPitch;
            }
        }
        return guess;
    }

    private void updateToneSpinners() {
        if (!SwingUtilities.isEventDispatchThread()) {
            new IllegalMonitorStateException().printStackTrace();
        }
        toneSpinnersFrozen = true; // barf
        try {
            Arrays.asList(toneSpinners).forEach(spinner -> spinner.setValue(0));
            Tone[][] scale = getRiddler().scale;
            for (Tone[] row : scale) {
                for (Tone tone : row) {
                    JSpinner spinner = toneSpinners[tone.ordinal()];
                    int value = (int) spinner.getValue();
                    spinner.setValue(value + 1);
                }
            }
        } finally {
            toneSpinnersFrozen = false;
        }
    }

    private void updatePenaltySpinners() {
        for (int i = 0; i < penaltySpinners.length; i++) {
            JSpinner penaltySpinner = penaltySpinners[i];
            List<Pair<Pitch, Pitch>> penaltyList = penaltyLists.get(i);
            int value = penaltyList.size();
            penaltySpinner.setValue(value);
        }
    }

    private void scheduleHintAndPenalty() {
        long riddleTimestampMs = System.currentTimeMillis();
        this.riddleTimestampMs = riddleTimestampMs;
        Hinter hinter = getHinter();
        int delayMs = hinter.delayMs == 0 ? 1000 : hinter.delayMs;
        //fixme: Move some stuff to Hinter
        if (hinter.delayMs != Integer.MAX_VALUE) {
            asyncExecutor.schedule(() -> SwingUtilities.invokeLater(() -> {
                if (isPlaying() && riddleTimestampMs == this.riddleTimestampMs) {
                    Pitch riddle = this.riddle.get();
                    if (riddle != null) {
                        if (!hinter.equals(Hinter.Always)) {
                            pitchyPanel.setBackground(riddle.getTone().getColor());
                            guessLabel.setText("    ");
                        }
                        Pitch prevRiddle = this.prevRiddle.get();
                        if (prevRiddle != null) {
                            List<Pair<Pitch, Pitch>> penaltyList = penaltyLists.get(riddle.getTone().ordinal());
                            int penaltyFactor = (int) penaltyFactorSpinner.getValue();
                            if (penaltyFactor > 0) {
                                debug("New penalty " + riddle + " after " + prevRiddle);
                                penaltyRiddleTimestampMs = riddleTimestampMs;
                                for (int i = 0; i < penaltyFactor; i++) {
                                    penaltyList.add(new Pair<>(prevRiddle, riddle));
                                }
                                updatePenaltySpinners();
                            }
                        }
                    }
                }
            }), delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private List<Pitch> shuffle() {
        List<Tone> tones = new ArrayList<>(toneSpinners.length * 2);
        for (int i = 0; i < TONES.length; i++) {
            int count = (int) toneSpinners[i].getValue();
            for (int j = 0; j < count; j++) {
                tones.add(TONES[i]);
                tones.add(TONES[i]); // Better shuffling - allows the same note twice in a row
            }
        }
        Collections.shuffle(tones);
        List<Pitch> shuffled = addOctaves(tones);
        debug(shuffled + " are the new riddles without penalties");

        List<Pitch> result = new ArrayList<>(shuffled.size() * 2);
        for (List<Pair<Pitch, Pitch>> penaltyList : penaltyLists) {
            for (int i = 0; i < penaltyList.size(); i++) {
                Pair<Pitch, Pitch> penalty = penaltyList.get(i);
                result.add(penalty.left);
                result.add(penalty.right);
                if (i >= 7) {
                    break;
                }
            }
        }
        result.addAll(shuffled);
        debug(result + " are the new riddles with penalties");

        Set<Pair<Pitch, Pitch>> reminders = new LinkedHashSet<>(penaltyReminders);
        for (Pair<Pitch, Pitch> reminder : reminders) {
            result.add(reminder.left);
            result.add(reminder.right);
        }
        debug(result + " are the new riddles with penalties and reminders");

        return result;
    }

    private List<Pitch> addOctaves(List<Tone> scale) {
        return scale.stream()
                .map(tone -> {
                    List<Integer> selectedOctaves = new ArrayList<>(ALL_OCTAVES.length);
                    for (int i = 0; i < ALL_OCTAVES.length; i++) {
                        JToggleButton octaveToggle = octaveToggles[i];
                        if (octaveToggle.isSelected()) {
                            selectedOctaves.add(ALL_OCTAVES[i]);
                        }
                    }
                    Pitch pitch;
                    String name = tone.name();
                    if (selectedOctaves.isEmpty()) {
                        pitch = tone.getPitch();
                    } else {
                        int index = random.nextInt(selectedOctaves.size());
                        name = name + (int) selectedOctaves.get(index);
                        pitch = Pitch.valueOf(name);
                    }
                    debug("Pitch=" + pitch + ", name=" + name);
                    return pitch;
                })
                .collect(Collectors.toList());
    }

    private void fugue(MidiChannel midiChannel, Object[] fugue, boolean flashColors) {
        frozen = true;
        try {
            debug("Fugue=" + Arrays.toString(fugue));
            Pitch prev = null;
            for (Object next : fugue) {
                if (next == null) {
                    Thread.sleep(frt);
                } else if (next instanceof Pitch) {
                    if (prev != null) {
                        Thread.sleep(frt);
                        midiChannel.noteOff(prev.getMidi());
                    }
                    Pitch pitch = (Pitch) next;
                    midiChannel.noteOn(pitch.getMidi(), 127);
                    if (flashColors) {
                        SwingUtilities.invokeLater(() -> {
                            updatePianoButton(pitch.getTone().getKey(), true);
                            setColor(pitch.getTone().getColor());
                        });
                    }
                    prev = pitch;
                } else if (next instanceof Integer) {
                    Thread.sleep((Integer) next);
                    if (prev != null) {
                        midiChannel.noteOff(prev.getMidi());
                        if (flashColors) {
                            Key key = prev.getTone().getKey();
                            SwingUtilities.invokeLater(() -> updatePianoButton(key, false));
                        }
                        prev = null;
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported element=" + next.getClass());
                }
            }
            if (prev != null) {
                Thread.sleep(frt);
                midiChannel.noteOff(prev.getMidi());
                if (flashColors) {
                    Key key = prev.getTone().getKey();
                    SwingUtilities.invokeLater(() -> updatePianoButton(key, false));
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            frozen = false;
        }
    }

    private void transcribe(Pitch guess, boolean force) {
        SwingUtilities.invokeLater(() -> {
            //fixme: Wrap the notes into colored boxes (html?)
            Tone previous = lastGuess.getAndSet(guess.getTone());
            if (force || previous == null || !previous.equals(guess.getTone())) {
                long previousTimestampMs = lastGuessTimestampMs;
                lastGuessTimestampMs = System.currentTimeMillis();
                if (lastGuessTimestampMs - previousTimestampMs > 500) {
                    text("\n");
                }
                text("  ");
//                String text = "<span style=\"background-color: #FFFF00\">This text is highlighted in yellow.</span>";
//                text(text);
                text(guess.getTone().getSpacedName());
//                text(guess.getTone().name().toLowerCase(), Color.LIGHT_GRAY, guess.getTone().getColor());
                text("  \n");
            }
        });
    }

    private void text(String message) {
//        text(message, null, null);
//    }
//
//    private void text(String message, Color foreground, Color background) {
        if (SwingUtilities.isEventDispatchThread()) {
            //fixme: Discard oldest when becomes too big
//            StyledDocument document = text.getStyledDocument();
//            SimpleAttributeSet attributes = new SimpleAttributeSet();
//            if (foreground != null) {
//                StyleConstants.setForeground(attributes, foreground);
//            }
//            if (background != null) {
//                StyleConstants.setBackground(attributes, background);
//            }
//            StyleConstants.setBold(attributes, false);
//            try {
//                document.insertString(document.getLength(), message, attributes);
//            } catch (BadLocationException e) {
//                e.printStackTrace();
//            }
            text.append(" ");
            text.append(message);
            text.setCaretPosition(text.getDocument().getLength());
        }
    }

    private void out(String message) {
        System.out.print(Thread.currentThread().getName());
        System.out.print(": ");
        System.out.println(message);
        text(message);
        text("  \n");
    }

    public static void main(String... strings) throws InterruptedException, InvocationTargetException {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Pitchenga"); //fixme: Does not work

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.out.println("Thread=" + thread.getName() + ", error=" + throwable);
            throwable.printStackTrace();
        });

        SwingUtilities.invokeAndWait(() -> {
            UIManager.put("control", new Color(128, 128, 128));
            UIManager.put("info", new Color(128, 128, 128));
            UIManager.put("nimbusBase", new Color(18, 30, 49));
            UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
            UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
            UIManager.put("nimbusFocus", new Color(115, 164, 209));
            UIManager.put("nimbusGreen", new Color(176, 179, 50));
            UIManager.put("nimbusInfoBlue", new Color(66, 139, 221));
            UIManager.put("nimbusLightBackground", new Color(18, 30, 49));
            UIManager.put("nimbusOrange", new Color(191, 98, 4));
            UIManager.put("nimbusRed", new Color(169, 46, 34));
            UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
            UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
            UIManager.put("text", new Color(230, 230, 230));
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    try {
                        UIManager.setLookAndFeel(info.getClassName());
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
            new Pitchenga();
        });
    }

    private void initGui() {
        initIcon();
        this.setLayout(new BorderLayout());

        //fixme: +Color circle to the control panel +light them up with guess update
        this.add(createControlPanel(), BorderLayout.NORTH);

        JPanel centralPanel = new JPanel();
        this.add(centralPanel);
        centralPanel.setLayout(new BorderLayout());

        centralPanel.add(guessPanel, BorderLayout.CENTER);
        guessPanel.setBackground(Color.DARK_GRAY);
        guessPanel.setLayout(new BorderLayout());
        JPanel labelsPanel = new JPanel();
        guessPanel.add(labelsPanel, BorderLayout.NORTH);
        labelsPanel.setOpaque(false);
        labelsPanel.setLayout(new GridLayout(2, 1));

        labelsPanel.add(pitchyPanel);
        pitchyPanel.setBackground(Color.GRAY);
        pitchyPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        pitchyPanel.add(guessLabel);
        guessLabel.setOpaque(true);
        guessLabel.setForeground(Color.WHITE);
        guessLabel.setBackground(Color.BLACK);
        guessLabel.setFont(COURIER);

        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(null);
        guessPanel.add(scroll, BorderLayout.CENTER);

//        text.setContentType("text/html");
        text.setFont(COURIER);
        text.setEditable(false);
        text.setForeground(Color.LIGHT_GRAY);
        text.setBackground(Color.DARK_GRAY);
        text.setBorder(null);
//        text("<html>");
        for (int i = 0; i < 500; i++) { //There must be a better way
            text("\n");
        }

        //fixme: +Midi instrument in
        for (Key key : Key.values()) {
            JToggleButton keyButton = new JToggleButton(key.getLabel());
            keyButtons[key.ordinal()] = keyButton;
        }

        JPanel pianoPanelPanel = new JPanel(new BorderLayout());
        centralPanel.add(pianoPanelPanel, BorderLayout.SOUTH);
        pianoPanelPanel.add(createChromaticPiano(), BorderLayout.CENTER); //fixme: Remove the grey gap on the sides
        pianoPanelPanel.add(createTwoOctavesPiano(), BorderLayout.SOUTH);
//        pianoPanelPanel.add(createOneOctavePiano(), BorderLayout.SOUTH);

        updateToneSpinners();

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();

        Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        //fixme: Change to center when saving to file is implemented
//        this.setSize((int) screenSize.getWidth(), (int) screenSize.getHeight());
        this.setSize(730, (int) screenSize.getHeight());
//        setLocation(screen.width / 2 - getSize().width / 2, screen.height / 2 - getSize().height / 2);
        //fixme: Should resize relatively + have a slider for the user to resize
//        riddlePanel.add(Box.createVerticalStrut((int) (this.getSize().getHeight() / 3)));

        this.setLocation(screenSize.width - getSize().width - 10, screenSize.height / 2 - getSize().height / 2);
//        this.setLocation(10, screenSize.height / 2 - getSize().height / 2);
        this.setVisible(true);
    }

    private void initKeyboard() {
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventPostProcessor(event -> {
            boolean pressed;
            if (event.getID() == KeyEvent.KEY_PRESSED) {
                pressed = true;
            } else if (event.getID() == KeyEvent.KEY_RELEASED) {
                pressed = false;
            } else {
                return false;
            }
            debug("Frozen=" + frozen + ", key=" + event + ";");

            if (pressed && event.getKeyCode() == KeyEvent.VK_SPACE) {
                if (!playButton.hasFocus() /* && !otherButtons.haveFocus()*/) {
                    playButton.setSelected(!playButton.isSelected());
                    handlePlayButton();
                }
            }

            //fixme: Use key.ordinal() instead of the map
            Key key = KEY_BY_CODE.get(event.getKeyCode());
            if (key == null) {
                return false;
            }
            handleKey(key, pressed);
            return true;
        });
    }

    private void handleKey(Key key, boolean pressed) {
        if (key.getPitch() == null) {
            return;
        }
        updatePianoButton(key, pressed);
        if (pressed) {
            updateGuessColor(key.getPitch(), key.getPitch().getFrequency(), 1, 42);
            boolean added = pressedKeys.add(key.getPitch());
            if (added) {
                transcribe(key.getPitch(), true);
                brightPiano.noteOn(key.getPitch().getMidi(), 127);
            }
        } else {
            pressedKeys.remove(key.getPitch());
            brightPiano.noteOff(key.getPitch().getMidi());
            keyButtons[key.ordinal()].setSelected(false);
            keyExecutor.execute(() -> play(key.getPitch()));
        }
    }

    private void updatePianoButton(Key key, boolean pressed) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalMonitorStateException();
        }
        keyButtons[key.ordinal()].setSelected(pressed);
    }

    private void updatePianoButtons(Key key) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalMonitorStateException();
        }
        for (Key k : KEYS) {
            JToggleButton keyButton = keyButtons[k.ordinal()];
            keyButton.setSelected(k.getPitch() != null && k.getPitch().getTone().equals(key.getPitch().getTone()));
        }
    }

    private JPanel createControlPanel() {
        JPanel controlPanelPanel = new JPanel();
        controlPanelPanel.setBackground(Color.DARK_GRAY);
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(Color.DARK_GRAY);
        controlPanelPanel.add(controlPanel);
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.add(createButtonsPanel());
        controlPanel.add(initInputCombo());
        controlPanel.add(initPitchAlgoCombo());
        controlPanel.add(initHinterCombo());
        controlPanel.add(initPacerCombo());
        controlPanel.add(initRiddleRingerCombo());
        controlPanel.add(initGuessRingerCombo());
        controlPanel.add(initRiddlerCombo());
        controlPanel.add(createOctavesPanel());
        return controlPanelPanel;
    }

    private JPanel createOctavesPanel() {
        JPanel octavesPanel = new JPanel();
        octavesPanel.setBackground(Color.DARK_GRAY);
        JPanel penaltyFactorPanel = new JPanel();
        penaltyFactorPanel.setBackground(Color.DARK_GRAY);
        octavesPanel.add(penaltyFactorPanel);
        penaltyFactorPanel.add(new JLabel("Penalty Factor:"));
        penaltyFactorPanel.add(penaltyFactorSpinner);
        penaltyFactorSpinner.setValue(DEFAULT_PENALTY_FACTOR);

        octavesPanel.add(new JLabel(" "));
        octavesPanel.add(new JLabel("Octaves:"));

        for (int i = 0; i < ALL_OCTAVES.length; i++) {
            Integer octave = ALL_OCTAVES[i];
            JToggleButton toggle = new JToggleButton("" + octave);
            octaveToggles[i] = toggle;
            octavesPanel.add(toggle);
            toggle.setSelected(Arrays.asList(DEFAULT_OCTAVES).contains(octave));
            toggle.addItemListener(event -> {
                if (toneSpinnersFrozen) {
                    return;
                }
                executor.execute(() -> {
                    resetGame();
                    play(null);
                });
            });
        }
        return octavesPanel;
    }

    private JPanel createChromaticPiano() {
        JPanel panel = new JPanel(new GridLayout(1, 12));
        panel.setBackground(Color.DARK_GRAY);

        for (Tone tone : TONES) {
            JPanel colorPanel = new JPanel();
            panel.add(colorPanel);
            colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.Y_AXIS));
            colorPanel.setBackground(tone.getColor());

            JPanel spinnerPanel = new JPanel();
            colorPanel.add(spinnerPanel);
            spinnerPanel.setOpaque(false);
            JSpinner toneSpinner = toneSpinners[tone.ordinal()];
            spinnerPanel.add(toneSpinner);
            toneSpinner.setAlignmentX(Component.CENTER_ALIGNMENT);
            toneSpinner.addChangeListener(event -> {
                if (!toneSpinnersFrozen) {
                    keyExecutor.execute(() -> {
                        resetGame();
                        play(null);
                    });
                }
            });

            JPanel penaltyPanel = new JPanel();
            colorPanel.add(penaltyPanel);
            penaltyPanel.setOpaque(false);
            JSpinner penaltySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));
            penaltyPanel.add(penaltySpinner.getEditor());
            ((JSpinner.DefaultEditor) penaltySpinner.getEditor()).getTextField().setEditable(false);
            ((JSpinner.DefaultEditor) penaltySpinner.getEditor()).getTextField().setFont(new Font("SansSerif", Font.PLAIN, 9));
            penaltySpinners[tone.ordinal()] = penaltySpinner;
            penaltySpinner.addChangeListener(event -> updatePenaltySpinners());
            penaltySpinner.setAlignmentX(Component.CENTER_ALIGNMENT);

            colorPanel.add(Box.createVerticalStrut(5));

            JLabel colorLabel = new JLabel(tone.getSpacedName());
            colorPanel.add(colorLabel);
            colorLabel.setFont(COURIER);
            colorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            colorLabel.setForeground(Color.WHITE);
            colorLabel.setBackground(Color.BLACK);
            colorLabel.setOpaque(true);

            colorPanel.add(Box.createVerticalStrut(10));

            Key key = null;
            for (Key aKey : Key.values()) {
                if (aKey.getPitch() != null
                        && aKey.isChromaticPiano()
                        && aKey.getPitch().getTone().equals(tone)) {
                    key = aKey;
                    break;
                }
            }
            if (key != null) {
                Key theKey = key;
                JToggleButton keyButton = keyButtons[key.ordinal()];
                colorPanel.add(keyButton);
                keyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                keyButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        handleKey(theKey, true);
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        handleKey(theKey, false);
                    }
                });
                keyButton.setForeground(key == Key.n05 ? Color.BLACK : Color.WHITE);
                keyButton.setBackground(Color.DARK_GRAY);
                keyButton.setEnabled(false);
            }
            colorPanel.add(Box.createVerticalStrut(10));

            colorPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleKey(tone.getKey(), true);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleKey(tone.getKey(), false);
                }
            });
        }
        return panel;
    }

    // One-octave piano for vertical phone orientation
//    private JPanel createOneOctavePiano() {
//        JPanel panel = new JPanel(new GridLayout(2, 1));
//        JPanel topPanelPanel = new JPanel(new BorderLayout());
//        panel.add(topPanelPanel);
//        topPanelPanel.setBackground(Color.DARK_GRAY);
//        JPanel topPanel = new JPanel();
//        topPanelPanel.add(topPanel, BorderLayout.CENTER);
//        topPanel.setBackground(Color.DARK_GRAY);
//        topPanel.setLayout(new GridLayout(1, 7));
//        Component topStrut = Box.createHorizontalStrut(30);
//        topPanelPanel.add(topStrut, BorderLayout.EAST);
//        topStrut.setBackground(Color.DARK_GRAY);
//
//        JPanel bottomPanelPanel = new JPanel(new BorderLayout());
//        panel.add(bottomPanelPanel);
//        bottomPanelPanel.setBackground(Color.DARK_GRAY);
//        Component bottomStrut = Box.createHorizontalStrut(30);
//        bottomPanelPanel.add(bottomStrut, BorderLayout.WEST);
//        bottomStrut.setBackground(Color.DARK_GRAY);
//
//        JPanel bottomPanel = new JPanel();
//        bottomPanelPanel.add(bottomPanel, BorderLayout.CENTER);
//        bottomPanel.setBackground(Color.DARK_GRAY);
//        bottomPanel.setLayout(new GridLayout(1, 7));
//
//        Tone[] tones = Tone.values();
//        List<Tone> piano = new ArrayList<>(tones.length);
//        piano.addAll(Arrays.asList(Fi, Le, Se, null, Ra, Me, null));
//        piano.addAll(Arrays.asList(DIATONIC_SCALE));
//
//        for (Tone tone : piano) {
//            JPanel colorPanel = new JPanel();
//            if (tone == null) {
//                topPanel.add(colorPanel);
//                colorPanel.setBackground(Color.DARK_GRAY);
//                continue;
//            }
//            if (tone.isDiatonic()) {
//                bottomPanel.add(colorPanel);
//            } else {
//                topPanel.add(colorPanel);
//            }
//            colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.Y_AXIS));
//            Color color = tone.getColor();
//            colorPanel.setBackground(color);
//
//            colorPanel.add(Box.createVerticalStrut(5));
//
//            JLabel colorLabel = new JLabel(tone.getSpacedName());
//            colorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
//            colorPanel.add(colorLabel);
//            colorLabel.setForeground(Color.WHITE);
//            colorLabel.setBackground(Color.BLACK);
//            colorLabel.setOpaque(true);
//
//            colorPanel.add(Box.createVerticalStrut(5));
//
//            JToggleButton keyButton = new JToggleButton(tone.getKey().name());
//            keyButtons[tone.ordinal()] = keyButton;
//            colorPanel.add(keyButton);
//            keyButton.setAlignmentX(Component.CENTER_ALIGNMENT);

//            keyButton.addMouseListener(new MouseAdapter() {
//                @Override
//                public void mousePressed(MouseEvent e) {
//                    handleKey(tone.getKey(), true);
//                }
//
//                @Override
//                public void mouseReleased(MouseEvent e) {
//                    handleKey(tone.getKey(), false);
//                }
//            });
//
//            keyButton.setForeground(tone.getKey().equals(Key.J) ? Color.BLACK : Color.WHITE);
//            colorPanel.add(Box.createVerticalStrut(5));
//
//            colorPanel.addMouseListener(new MouseAdapter() {
//                @Override
//                public void mousePressed(MouseEvent e) {
//                    handleKey(tone.getKey(), true);
//                }
//
//                @Override
//                public void mouseReleased(MouseEvent e) {
//                    handleKey(tone.getKey(), false);
//                }
//            });
//        }
//        return panel;
//    }

    private JPanel createTwoOctavesPiano() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        JPanel topPanelPanel = new JPanel(new BorderLayout());
        panel.add(topPanelPanel);
        topPanelPanel.setBackground(Color.DARK_GRAY);
        JPanel topPanel = new JPanel();
        topPanelPanel.add(topPanel, BorderLayout.CENTER);
        topPanel.setBackground(Color.DARK_GRAY);
        topPanel.setLayout(new GridLayout(1, 7));
        Component topStrut = Box.createHorizontalStrut(20);
        topPanelPanel.add(topStrut, BorderLayout.EAST);
        topStrut.setBackground(Color.DARK_GRAY);

        JPanel bottomPanelPanel = new JPanel(new BorderLayout());
        panel.add(bottomPanelPanel);
        bottomPanelPanel.setBackground(Color.DARK_GRAY);
        Component bottomStrut = Box.createHorizontalStrut(20);
        bottomPanelPanel.add(bottomStrut, BorderLayout.WEST);
        bottomStrut.setBackground(Color.DARK_GRAY);

        JPanel bottomPanel = new JPanel();
        bottomPanelPanel.add(bottomPanel, BorderLayout.CENTER);
        bottomPanel.setBackground(Color.DARK_GRAY);
        bottomPanel.setLayout(new GridLayout(1, 7));

        Key[] keys = Key.values();
        for (Key key : keys) {
            if (key.isChromaticPiano()) {
                continue;
            }

            JPanel colorPanel = new JPanel();
            if (key.getPitch() == null) {
                topPanel.add(colorPanel);
                colorPanel.setBackground(Color.DARK_GRAY);
            }
            if (key.getPitch() != null) {
                if (key.getPitch().getTone().isDiatonic()) {
                    bottomPanel.add(colorPanel);
                } else {
                    topPanel.add(colorPanel);
                }
            }
            colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.Y_AXIS));
            Color color = key.getPitch() == null ? Color.DARK_GRAY : key.getPitch().getTone().getColor();
            colorPanel.setBackground(color);

            colorPanel.add(Box.createVerticalStrut(5));

            JLabel colorLabel = new JLabel(key.getPitch() == null ? "    " : key.getPitch().getTone().getSpacedName());
            colorPanel.add(colorLabel);
            colorLabel.setFont(COURIER);
            colorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            colorLabel.setForeground(Color.WHITE);
            colorLabel.setBackground(Color.BLACK);
            colorLabel.setOpaque(key.getPitch() != null);

            colorPanel.add(Box.createVerticalStrut(5));

            if (key.getLabel() != null) {
                JToggleButton keyButton = keyButtons[key.ordinal()];
                colorPanel.add(keyButton);
                keyButton.setAlignmentX(Component.CENTER_ALIGNMENT);

                if (key.getPitch() != null) {
                    keyButton.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            handleKey(key, true);
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            handleKey(key, false);
                        }
                    });
                    keyButton.setForeground(key == Key.F || key == Key.J ? Color.BLACK : Color.WHITE);
                    keyButton.setBackground(Color.DARK_GRAY);
                    keyButton.setEnabled(false);
                }
            }

            colorPanel.add(Box.createVerticalStrut(5));

            colorPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleKey(key, true);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleKey(key, false);
                }
            });
        }
        return panel;
    }

    private JComboBox<Pacer> initPacerCombo() {
        for (Pacer pacer : Pacer.values()) {
            pacerCombo.addItem(pacer);
        }
        pacerCombo.setMaximumRowCount(Pacer.values().length);
        pacerCombo.addItemListener(event -> executor.execute(() -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                resetGame();
                play(null);
            }
        }));
        pacerCombo.setSelectedItem(DEFAULT_PACER);
        return pacerCombo;
    }

    private JComboBox<Riddler> initRiddlerCombo() {
        for (Riddler riddler : Riddler.values()) {
            riddlerCombo.addItem(riddler);
        }
        riddlerCombo.setMaximumRowCount(Riddler.values().length);
        riddlerCombo.addItemListener(event -> executor.execute(() -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        toneSpinnersFrozen = true;
                        frozen = true;
                        try {
                            resetGame();
                            Riddler riddler = (Riddler) event.getItem();
                            List<Integer> riddlerOctaves = Arrays.asList(riddler.octaves);
                            for (int i = 0; i < ALL_OCTAVES.length; i++) {
                                Integer octave = ALL_OCTAVES[i];
                                octaveToggles[i].setSelected(riddlerOctaves.contains(octave));
                            }
                            updateToneSpinners();
                        } finally {
                            frozen = false;
                            toneSpinnersFrozen = false;
                        }
                    });
                } catch (InterruptedException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                play(null);
            }
        }));
        riddlerCombo.setSelectedItem(DEFAULT_RIDDLER);
        return riddlerCombo;
    }

    private JComboBox<Hinter> initHinterCombo() {
        for (Hinter hinter : Hinter.values()) {
            hinterCombo.addItem(hinter);
        }
        hinterCombo.setSelectedItem(DEFAULT_HINTER);
        hinterCombo.addItemListener(event -> scheduleHintAndPenalty());
        return hinterCombo;
    }

    private JComboBox<GuessRinger> initGuessRingerCombo() {
        for (GuessRinger guessRinger : GuessRinger.values()) {
            guessRingerCombo.addItem(guessRinger);
        }
        guessRingerCombo.setSelectedItem(DEFAULT_GUESS_RINGER);
        return guessRingerCombo;
    }

    private JComboBox<RiddleRinger> initRiddleRingerCombo() {
        for (RiddleRinger ringer : RiddleRinger.values()) {
            riddleRingerCombo.addItem(ringer);
        }
        riddleRingerCombo.setSelectedItem(DEFAULT_RIDDLE_RINGER);
        return riddleRingerCombo;
    }

    private JComboBox<PitchEstimationAlgorithm> initPitchAlgoCombo() {
        for (PitchEstimationAlgorithm pitchAlgo : PitchEstimationAlgorithm.values()) {
            pitchAlgoCombo.addItem(pitchAlgo);
        }
        pitchAlgoCombo.setSelectedItem(DEFAULT_PITCH_ALGO);
        pitchAlgoCombo.addActionListener(event -> executor.execute(this::updateMixer));
        return pitchAlgoCombo;
    }

    private JComboBox<Mixer.Info> initInputCombo() {
        List<Mixer.Info> inputs = getAvailableInputs();
        inputCombo.setMaximumRowCount(inputs.size());
        for (Mixer.Info input : inputs) {
            inputCombo.addItem(input);
        }
        Mixer.Info defaultInput = getDefaultInput(inputs);
        if (defaultInput != null) {
            inputCombo.setSelectedItem(defaultInput);
        }
        inputCombo.addActionListener(event -> updateMixer());
        return inputCombo;
    }

    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.DARK_GRAY);
        panel.setLayout(new GridLayout());

        panel.add(playButton);
        playButton.setText("Play");
        playButton.addActionListener(event -> handlePlayButton());

        JButton resetButton = new JButton("Reset");
        panel.add(resetButton);

        JButton loadButton = new JButton("Load");
        panel.add(loadButton);

        JButton saveButton = new JButton("Save");
        panel.add(saveButton);

        return panel;
    }

    private void handlePlayButton() {
        if (!SwingUtilities.isEventDispatchThread()) {
            new IllegalMonitorStateException().printStackTrace();
        }
        boolean playing = isPlaying();
        debug("running=" + playing);
        if (playing) {
            resetGame();
            playButton.setText("Stop");
            executor.execute(() -> play(null));
        } else {
            setColor(Color.DARK_GRAY);
            playButton.setText("Play");
        }
        debug("running=" + playing);
    }

    private Hinter getHinter() {
        return (Hinter) hinterCombo.getSelectedItem();
    }

    private Riddler getRiddler() {
        return (Riddler) riddlerCombo.getSelectedItem();
    }

    private RiddleRinger getRiddleRinger() {
        return (RiddleRinger) riddleRingerCombo.getSelectedItem();
    }

    private GuessRinger getGuessRinger() {
        return (GuessRinger) guessRingerCombo.getSelectedItem();
    }

    private Pacer getPacer() {
        return (Pacer) pacerCombo.getSelectedItem();
    }

    private static String info(Color color) {
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }

    private boolean isPlaying() {
        return playButton.isSelected();
    }

    private void resetGame() {
        riddle.set(null);
        prevRiddle.set(null);
        riddleQueue.clear();
        penaltyLists.forEach(List::clear);
        SwingUtilities.invokeLater(this::updatePenaltySpinners);
    }

    private Mixer.Info getDefaultInput(List<Mixer.Info> inputs) {
        //noinspection ConstantConditions
        if (DEFAULT_INPUT != null) {
            return DEFAULT_INPUT;
        }
        Mixer.Info defaultInput = null;
        if (inputs.size() > 0) {
            for (Mixer.Info mixerInfo : inputs) {
                if (mixerInfo.toString().toLowerCase().contains("default")) {
                    defaultInput = mixerInfo;
                    break;
                }
            }
            if (defaultInput == null) {
                for (Mixer.Info mixerInfo : inputs) {
                    if (mixerInfo.toString().toLowerCase().contains("primary")) {
                        defaultInput = mixerInfo;
                        break;
                    }
                }
            }
            if (defaultInput == null) {
                defaultInput = inputs.get(0);
            }
        }
        return defaultInput;
    }


    //fixme: Add selector for the output device
    private void updateMixer() {
        try {
            if (audioDispatcher != null) {
                audioDispatcher.stop();
            }
            Mixer.Info mixerInfo = (Mixer.Info) inputCombo.getSelectedItem();
            if (mixerInfo == null || mixerInfo == NO_AUDIO_INPUT) {
                out("No audio input selected, play using keyboard or mouse.");
                out("To play using a musical instrument please select an audio input.");
                return;
            }
            PitchEstimationAlgorithm pitchAlgoOrNull = (PitchEstimationAlgorithm) pitchAlgoCombo.getSelectedItem();
            PitchEstimationAlgorithm pitchAlgo = pitchAlgoOrNull == null ? DEFAULT_PITCH_ALGO : pitchAlgoOrNull;
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            float sampleRate = 44100;
            int bufferSize = 1024;
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, true);
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
            line.open(format, bufferSize);
            line.start();
            AudioInputStream stream = new AudioInputStream(line);
            JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
            audioDispatcher = new AudioDispatcher(audioStream, bufferSize, 0);
            audioDispatcher.addAudioProcessor(new PitchProcessor(pitchAlgo, sampleRate, bufferSize, this));
            Runnable dispatch = () -> {
                try {
                    SwingUtilities.invokeLater(() -> out("Listening to [" + mixer.getMixerInfo().getName() + "] with [" + pitchAlgo + "]"));
                    audioDispatcher.run();
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    SwingUtilities.invokeLater(() -> out("Stopped listening to [" + mixer.getMixerInfo().getName() + "] with [" + pitchAlgo + "]"));
                }
            };
            new Thread(dispatch, "pitchenga-mixer" + idCounter.incrementAndGet()).start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private List<Mixer.Info> getAvailableInputs() {
        List<Mixer.Info> result = new ArrayList<>();
        List<Mixer.Info> mixers = Arrays.stream(AudioSystem.getMixerInfo())
                .filter(mixer -> {
                    Mixer aMixer = AudioSystem.getMixer(mixer);
                    Line.Info[] targetLineInfo = aMixer.getTargetLineInfo();
                    debug();
                    debug(aMixer.getMixerInfo());
                    debug(Arrays.toString(targetLineInfo));
                    debug(aMixer.getLineInfo());
                    //fixme: Better filter mixers that support recording
                    return targetLineInfo.length != 0;
                })
                .collect(Collectors.toList());
        result.add(NO_AUDIO_INPUT);
        result.addAll(mixers);
        return result;
    }

    private MidiChannel[] initMidi() {
        try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            Instrument[] instruments = synthesizer.getDefaultSoundbank().getInstruments();
            MidiChannel[] channels = synthesizer.getChannels();
            Instrument brightPiano = instruments[2];
            if (synthesizer.loadInstrument(brightPiano)) {
                channels[1].programChange(brightPiano.getPatch().getProgram());
            }
            Instrument guitar = instruments[25];
            if (synthesizer.loadInstrument(guitar)) {
                channels[2].programChange(guitar.getPatch().getProgram());
            }
            return channels;
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    // This could be done much better
    private static Pitch transposePitch(Pitch pitch, int octaves, int steps) {
        Pitch result = pitch;
        if (octaves != 0) {
            int octave = pitch.getOctave() + octaves;
            octave = Math.max(MIN_OCTAVE, octave);
            octave = Math.min(MAX_OCTAVE, octave);
            String name = pitch.getTone().name() + octave;
            result = Pitch.valueOf(name);
        }
        if (steps > TONES.length || steps < -TONES.length) {
            throw new IllegalArgumentException("Cannot shift more than " + TONES.length + ": " + steps);
        }
        if (steps != 0) {
            int ordinal = pitch.getTone().ordinal() + steps;
            if (ordinal < 0) {
                ordinal += TONES.length;
            }
            if (ordinal > TONES.length - 1) {
                ordinal -= TONES.length;
            }
            Tone tone = TONES[ordinal];
            String name = tone.name() + pitch.getOctave();
            result = Pitch.valueOf(name);
        }
        return result;
    }

    private static Object[] transposeFugue(Object[] fugue, int shiftOctaves) {
        List<Object> transposed = new ArrayList<>(fugue.length);
        for (Object next : fugue) {
            if (next instanceof Pitch) {
                Pitch nextPitch = (Pitch) next;
                next = transposePitch(nextPitch, shiftOctaves, 0);
            }
            transposed.add(next);
        }
        debug("Transposed fugue: " + transposed);
        return transposed.toArray();
    }

    private static void debug(Object... messages) {
        if (!debug) {
            return;
        }
        if (messages == null || messages.length == 0) {
            System.out.println(Thread.currentThread().getName());
        }
        if (messages != null) {
            for (Object message : messages) {
                System.out.println(Thread.currentThread().getName() + ": " + message);
            }
        }
    }

    private void initIcon() {
        Image image = Toolkit.getDefaultToolkit().getImage(Circles.class.getResource("/pitchenga.png"));
        this.setIconImage(image);
        try {
            Class<?> clazz = Class.forName("com.apple.eawt.Application");
            if (clazz != null) {
                Method getApplication = clazz.getMethod("getApplication");
                Object application = getApplication.invoke(null);
                Method setDockIconImage = clazz.getMethod("setDockIconImage", Image.class);
                setDockIconImage.invoke(application, image);
            }
        } catch (Exception ignore) {
        }
    }

    @SuppressWarnings("unused") //fixme: They are all used in the combo box!
    public enum Riddler {
        Chromatic("Chromatic - " + DEFAULT_OCTAVES.length + " octaves", new Tone[][]{CHROMATIC_SCALE}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        ChromaticOneOctave("Chromatic - 1 octave", new Tone[][]{CHROMATIC_SCALE}, Pitchenga::shuffle, new Integer[0]),
        Diatonic("Diatonic - " + DEFAULT_OCTAVES.length + " octaves", new Tone[][]{DIATONIC_SCALE}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        DiatonicOneOctave("Diatonic - 1 octave", new Tone[][]{DIATONIC_SCALE}, Pitchenga::shuffle, new Integer[0]),
        ChromaticWithDoubledDiatonic("Chromatic with doubled diatonic - " + DEFAULT_OCTAVES.length + " octaves", new Tone[][]{CHROMATIC_SCALE, DIATONIC_SCALE}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        ChromaticWithDoubledSharps("Chromatic with doubled sharps - " + DEFAULT_OCTAVES.length + " octaves", new Tone[][]{CHROMATIC_SCALE, SHARPS_SCALE}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        //fixme: Add scales C, Am, D, etc
        //fixme: Add random within scales
        SharpsOnly("Sharps only - " + DEFAULT_OCTAVES.length + " octaves", new Tone[][]{SHARPS_SCALE}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        SoLaDo("Step 1) So, La, Do", new Tone[][]{{So, La, Do}}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        MiSoLaDo("Step 2) Mi*2, So, La, Do", new Tone[][]{{Mi, Mi, So, La, Do}}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        FaMiSoLaDo("Step 3) Fa*2, Mi, So, La, Do", new Tone[][]{{Fa, Fa, Mi, So, La, Do}}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        ReFaMiSoLaDo("Step 4) Re*2, Fa, Mi, So, La, Do", new Tone[][]{{Re, Re, Fa, Mi, So, La, Do}}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        SiReFaMiSoLaDo("Step 5) Si*2, Re, Fa, Mi, So, La, Do", new Tone[][]{{Si, Si, Re, Fa, Mi, So, La, Do}}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        DiatonicPlusLe("Step 6) Diatonic + Le*2", new Tone[][]{DIATONIC_SCALE, {Le, Le}}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        DiatonicPlusFiLe("Step 7) Diatonic + Fi*2, Le", new Tone[][]{DIATONIC_SCALE, {Fi, Fi, Le}}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        DiatonicPlusRaFiLe("Step 8) Diatonic + Ra*2, Fi, Le", new Tone[][]{DIATONIC_SCALE, {Ra, Ra, Fi, Le}}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        DiatonicPlusSeRaFiLe("Step 9) Diatonic + Se*2, Ra, Fi, Le", new Tone[][]{DIATONIC_SCALE, {Se, Se, Ra, Fi, Le}}, Pitchenga::shuffle, DEFAULT_OCTAVES),
        DiatonicPlusMeSeRaFiLe("Step 19) Diatonic + Me*2, Se, Ra, Fi, Le", new Tone[][]{DIATONIC_SCALE, {Me, Me, Se, Ra, Fi, Le}}, Pitchenga::shuffle, DEFAULT_OCTAVES);

        private final String name;
        private final Tone[][] scale;
        private final Function<Pitchenga, List<Pitch>> riddleAction;
        private final Integer[] octaves;

        Riddler(String name, Tone[][] scale, Function<Pitchenga, List<Pitch>> riddleAction, Integer[] octaves) {
            this.name = name;
            this.scale = scale;
            this.riddleAction = riddleAction;
            this.octaves = octaves;
        }

        public String toString() {
            return name;
        }
    }

    @SuppressWarnings("unused") //fixme: They are all used in the combo box!
    public enum GuessRinger {
        Tune("Ring mnemonic tune on correct answer", pitch -> pitch.getTone().getTune()),
        Tone("Ring tone on correct answer", pitch -> new Object[]{pitch.getTone().getPitch(), frt}),
        JustDo("Ring Do on correct answer", pitch -> new Object[]{Do.getPitch(), frt}),
        ToneAndDo("Ring tone and Do on correct answer", pitch -> new Object[]{pitch.getTone().getPitch(), Do.getPitch()}),
        None("Ring nothing on correct answer", pitch -> new Object[]{}),
        Pause("Short pause on correct answer", pitch -> new Object[]{sxt}),
        ;
        private final String name;
        private final Function<Pitch, Object[]> ring;

        GuessRinger(String name, Function<Pitch, Object[]> ring) {
            this.name = name;
            this.ring = ring;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @SuppressWarnings("unused") //fixme: They are all used in the combo box!
    public enum RiddleRinger {
        Tune("Riddle mnemonic tune", RiddleRinger::transposeTune),
        Tone("Riddle tone", pitch -> new Object[]{pitch, frt}),
        ToneAndDo("Riddle tone and Do", pitch -> new Object[]{pitch.getTone().getPitch(), Do.getPitch()}),
        ;
        private final String name;
        private final Function<Pitch, Object[]> ring;

        RiddleRinger(String name, Function<Pitch, Object[]> ring) {
            this.name = name;
            this.ring = ring;
        }

        private static Object[] transposeTune(Pitch pitch) {
            int shift = pitch.getOctave() - pitch.getTone().getPitch().getOctave();
            return transposeFugue(pitch.getTone().getTune(), shift);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @SuppressWarnings("unused") //fixme: They are all used in the combo box!
    public enum Hinter {
        Always("Always hint", 0),
        OneSec("Hint after 1 second", 1000),
        TwoSec("Hint after 2 seconds", 2000),
        ThreeSec("Hint after 3 seconds", 3000),
        FiveSec("Hint after 5 seconds", 5000),
        Never("No hint", Integer.MAX_VALUE);

        private final String name;
        private final int delayMs;

        Hinter(String name, int delayMs) {
            this.name = name;
            this.delayMs = delayMs;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    @SuppressWarnings("unused") //fixme: They are all used in the combo box!
    public enum Pacer {
        Answer("Answer to continue", Pacer::checkAnswer),
        Tempo20("Tempo 20", pair -> pace(20)),
        Tempo40("Tempo 40", pair -> pace(40)),
        Tempo60("Tempo 60", pair -> pace(60)),
        Tempo80("Tempo 80", pair -> pace(80)),
        Tempo100("Tempo 100", pair -> pace(100)),
        Tempo120("Tempo 120", pair -> pace(120)),
        Tempo140("Tempo 140", pair -> pace(140)),
        ;

        private static volatile long lastPacerTimestamp = System.currentTimeMillis();

        private static boolean pace(int bpm) {
            long delay = 60_000 / bpm;
            long prevTimestamp = lastPacerTimestamp;
            long elapsed = System.currentTimeMillis() - prevTimestamp;
            if (elapsed < delay) {
                long diff = delay - elapsed;
                try {
                    Thread.sleep(diff);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            lastPacerTimestamp = System.currentTimeMillis();
            return true;
        }

        private static boolean checkAnswer(Pair<Pitch, Pitch> pair) {
            return pair.left != null && pair.left.getTone().equals(pair.right.getTone());
        }

        private String name;
        private Function<Pair<Pitch, Pitch>, Boolean> check;

        Pacer(String name, Function<Pair<Pitch, Pitch>, Boolean> check) {
            this.name = name;
            this.check = check;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
