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

import static com.pitchenga.Duration.*;
import static com.pitchenga.Pitch.*;
import static com.pitchenga.Tone.*;

public class Pitchenga extends JFrame implements PitchDetectionHandler {

    private static final boolean debug = "true".equalsIgnoreCase(System.getProperty("com.pitchenga.debug"));
    private static final Pitch[] PITCHES = Pitch.values();
    private static final Tone[] TONES = Tone.values();
    private static final Button[] BUTTONS = Button.values();
    private static final Integer[] ALL_OCTAVES = Arrays.stream(PITCHES).map(pitch -> pitch.octave).filter(octave -> octave >= 0).distinct().toArray(Integer[]::new);
    //fixme: Move to Scale
    private static final Pitch[] CHROMATIC_SCALE = Arrays.stream(TONES).map(tone -> tone.getFugue().pitch).toArray(Pitch[]::new);
    private static final Pitch[] DO_MAJ_SCALE = Arrays.stream(TONES).filter(tone -> tone.diatonic).map(tone -> tone.getFugue().pitch).toArray(Pitch[]::new);
    private static final Pitch[] SHARPS_SCALE = Arrays.stream(TONES).filter(tone -> !tone.diatonic).map(tone -> tone.getFugue().pitch).toArray(Pitch[]::new);
    private static final Map<Integer, Button> KEY_BY_CODE = Arrays.stream(Button.values()).collect(Collectors.toMap(button -> button.keyEventCode, button -> button));
    public static final Font COURIER = new Font("Courier", Font.BOLD, 16);

    private final Setup setup = Setup.create();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService asyncExecutor = Executors.newSingleThreadScheduledExecutor();
    //fixme: Bigger queue, but process them all in one go so that the buzzer goes off only once when multiple keys pressed
    private final BlockingQueue<Runnable> playQueue = new ArrayBlockingQueue<>(1);
    private final ExecutorService playExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, playQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    private final Random random = new Random();
    private volatile AudioDispatcher audioDispatcher;
    //fixme: +Selectors for instruments +Random instrument: 1) guitar/piano/sax 2) more 3) all
    private final MidiChannel piano;
    private final MidiChannel brightPiano;
    private final MidiChannel guitar;

    private final AtomicReference<Tone> lastGuess = new AtomicReference<>(null);
    private volatile Pitch lastPitch;
    private volatile long lastGuessTimestampMs = System.currentTimeMillis();
    private final List<Pair<Pitch, Double>> guessQueue = new ArrayList<>();
    private final Queue<Pitch> riddleQueue = new LinkedBlockingQueue<>();
    private final AtomicReference<Pitch> riddle = new AtomicReference<>(null);
    private final AtomicReference<Pitch> prevRiddle = new AtomicReference<>(null);
    private final AtomicReference<Pitch> prevPrevRiddle = new AtomicReference<>(null);
    private volatile long riddleTimestampMs = System.currentTimeMillis();
    private volatile long penaltyRiddleTimestampMs = System.currentTimeMillis();
    private volatile boolean frozen = false;
    private final AtomicInteger idCounter = new AtomicInteger(-1);
    private final Map<Button, Integer> pressedKeyToMidi = new HashMap<>(); // To ignore OS's key repeating when holding and to remember the modified midi code to release
    private volatile boolean fall = false; // Control - octave down
    private volatile boolean lift = false; // Shift - octave up

    private final Circle circle;
    private final JSpinner penaltyFactorSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 9, 1));
    private final JToggleButton[] octaveToggles = new JToggleButton[ALL_OCTAVES.length];
    private final JSpinner[] toneSpinners = Arrays.stream(TONES)
            .map(tone -> new JSpinner(new SpinnerNumberModel(0, 0, 9, 1)))
            .collect(Collectors.toList()).toArray(new JSpinner[0]);
    private volatile boolean toneSpinnersFrozen = false;
    private final JSpinner[] penaltySpinners = new JSpinner[TONES.length];
    private final List<List<Triplet<Pitch, Pitch, Pitch>>> penaltyLists = Arrays.stream(TONES).map(tone -> new ArrayList<Triplet<Pitch, Pitch, Pitch>>()).collect(Collectors.toList());
    private final Set<Triplet<Pitch, Pitch, Pitch>> penaltyReminders = new LinkedHashSet<>();
    private final JComboBox<PitchEstimationAlgorithm> pitchAlgoCombo = new JComboBox<>();
    private final JComboBox<Hinter> hinterCombo = new JComboBox<>();
    private final JComboBox<Pacer> pacerCombo = new JComboBox<>();
    private final JComboBox<RiddleRinger> riddleRingerCombo = new JComboBox<>();
    private final JComboBox<GuessRinger> guessRingerCombo = new JComboBox<>();
    private final JComboBox<Riddler> riddlerCombo = new JComboBox<>();
    private final JComboBox<Mixer.Info> inputCombo = new JComboBox<>();
    private final JToggleButton playButton = new JToggleButton();
    private final JToggleButton[] keyButtons = new JToggleButton[Button.values().length];
    private final JLabel frequencyLabel = new JLabel("0000.00");
    private final JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL);
    private final JTextArea textArea = new JTextArea();
    //    private final JTextPane text = new JTextPane();

    //fixme: Random within all scales - repeated  5 times, then switch to another random scale +blues scales
    //fixme: Labels on the circle's circles
    //fixme: Continuous gradient ring around the circle +slider
    //fixme: Change the slider knob color as well
    //fixme: Profiling
    //fixme: Korg PX5D is recognized, but no audio is coming - same problem in Pod Farm, but not in Garage Band
    //fixme: Neither of the pitch detection algorithms seem to work for bass guitar. MPM does not seem to work below mi2.
    //fixme: Colored waveform visualization
    //fixme: Solfege sound bank for midi +Les Paul +"monitoring" mode where it plays solfege on pitch detection
    //fixme: Audio output as input +actual monitoring
    //fixme: MP3 player
    //fixme: Text editor +converter for chords from multi-line to single-line
    //fixme: Midi instrument in
    //fixme: Midi/gpx files as the riddle source
    //fixme: Colored notes in the transcribe log
    //fixme: Sliding mouse while holding the button over the piano should activate the keys
    //fixme: Audio input stops working sometimes
    //fixme: Midi stops working sometimes or gets delayed especially after waking up from sleep
    //fixme: Load/save/reset; auto-save to home folder
    //fixme: Port to iOS and Android
    //fixme: Documentation / how to play
    //fixme: Visualize chords - like this, but adjust the palette: https://glasses.withinmyworld.org/index.php/2012/08/18/chord-colors-perfect-pitch-and-synesthesia/#.XkVt9y2ZO24
    //fixme: Alternative color schemes from config files. E.g. https://www.nature.com/articles/s41598-017-18150-y/figures/2;  .put("Do", new Color(253, 203, 3)).put("Ra", new Color(65, 3, 75)).put("Re", new Color(3, 179, 253)).put("Me", new Color(244, 56, 6)).put("Mi", new Color(250, 111, 252)).put("Fa", new Color(2, 252, 37)).put("Fi", new Color(3, 88, 69)).put("So", new Color(252, 2, 2)).put("Le", new Color(16, 24, 106)).put("La", new Color(251, 245, 173)).put("Se", new Color(2, 243, 252)).put("Si", new Color(219, 192, 244))
    //fixme: Split view and controller
    public Pitchenga() {
        super("Pitchenga");
        this.circle = new Circle();
        MidiChannel[] midiChannels = initMidi();
        piano = midiChannels[0];
        brightPiano = midiChannels[1];
        guitar = midiChannels[2];

        initGui();
        initKeyboard();
        updateMixer();
    }

    private void play(Pitch guess) {
        try {
            if (frozen || !isPlaying()) {
                return;
            }
            Pitch riddle = riddle();
            if (riddle == null) {
                return;
            }
            boolean success = getPacer().check.apply(new Pair<>(guess, riddle));
            debug(String.format("Play: [%s] %s [%.2fHz] : %s", riddle, guess, riddle.frequency, success));
            if (success) {
                correct(riddle);
            } else if (guess != null) {
                incorrect(riddle);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void correct(Pitch riddle) {
        Pitch prevRiddle = updatePenalties(riddle);
        this.prevRiddle.set(riddle);
        this.prevPrevRiddle.set(prevRiddle);
        this.riddle.set(null);

        GuessRinger guessRinger = getGuessRinger();
        transcribe(riddle, false);
        fugue(guitar, guessRinger.ring.apply(riddle), true);

        this.playQueue.clear();
        //fixme: This will stack overflow in the auto-play mode
        //fixme: It keeps playing playing the old game after changing settings
        play(null);
    }

    private Pitch updatePenalties(Pitch riddle) {
        Pitch prevRiddle = this.prevRiddle.get();
        Pitch prevPrevRiddle = this.prevPrevRiddle.get();
        if (penaltyRiddleTimestampMs != riddleTimestampMs) {
            List<Triplet<Pitch, Pitch, Pitch>> penaltyList = penaltyLists.get(riddle.tone.ordinal());
            for (Iterator<Triplet<Pitch, Pitch, Pitch>> iterator = penaltyList.iterator(); iterator.hasNext(); ) {
                Triplet<Pitch, Pitch, Pitch> penalty = iterator.next();
                if (penalty.first.equals(prevPrevRiddle)
                        && penalty.second.equals(prevRiddle)
                        && penalty.third.equals(riddle)) {
                    debug("Removing penalty " + penalty);
                    iterator.remove();
                    debug("Remaining penalties for " + riddle.tone + ": " + penaltyList);
                    penaltyReminders.add(penalty);
                    break;
                }
            }
            SwingUtilities.invokeLater(this::updatePenaltySpinners);
        }
        return prevRiddle;
    }

    private void incorrect(Pitch riddle) {
        frozen = true;
        try {
            fugue(piano, getRiddleRinger().ring.apply(riddle), false);
        } finally {
            frozen = false;
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
                if (riddle != None) {
                    this.riddle.set(riddle);
                    this.riddleTimestampMs = System.currentTimeMillis();
                }
                scheduleHint();
                frozen = true;
                try {
                    fugue(piano, getRiddleRinger().ring.apply(riddle), false);
                    playQueue.clear();
                } finally {
                    frozen = false;
                }
            }
        }
        return this.riddle.get();
    }

    @Override
    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent event) {
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
                                transcribe(guess, false);
                                if (!frozen && getPacer() == Pacer.Answer) {
                                    playExecutor.execute(() -> play(guess));
                                }
                            }
                        } else {
                            guessQueue.clear();
                            guessQueue.add(new Pair<>(guess, rms));
                        }
                    }
                    if (maxRms > rmsThreshold) {
                        updatePitch(guess, pitchDetectionResult.getPitch(), pitchDetectionResult.getProbability(), rms, false);
                    }
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void updatePitch(Pitch guess, float frequency, float probability, double rms, boolean isKeyboard) {
        double diff = frequency - guess.frequency;
        Pitch pitchy;
        if (diff < 0) {
            pitchy = transposePitch(guess, 0, -1);
        } else {
            pitchy = transposePitch(guess, 0, +1);
        }
        double pitchyDiff = Math.abs(guess.frequency - pitchy.frequency);
        double accuracy = Math.abs(diff) / pitchyDiff;
        double pitchiness = accuracy * 20;
        Color toneColor = guess.tone.color;
        Color guessColor;
        Color pitchinessColor;
        if (Math.abs(diff) < 0.000000000042) {
            guessColor = pitchinessColor = toneColor;
        } else {
            //fixme: Unit test for interpolation, e.g. direction
            guessColor = interpolateColor(accuracy, toneColor, pitchy.tone.color);
            pitchinessColor = interpolateColor(pitchiness, toneColor, pitchy.tone.color);
        }
        if (debug && !isPlaying()) {
            debug(String.format(" %s | pitch=%.2fHz | probability=%.2f | rms=%.2f | diff=%.2f | pitchyDiff=%.2f | accuracy=%.2f | pitchiness=%.2f | guessRoundedColor=%s | pitchyColor=%s | guessColor=%s | borderColor=%s",
                    guess, frequency, probability, rms, diff, pitchyDiff, accuracy, pitchiness, info(toneColor), info(pitchy.tone.color), info(guessColor), info(pitchinessColor)));
        }
        SwingUtilities.invokeLater(() -> {
            updateSlider(guess, frequency, isKeyboard);
            frequencyLabel.setText(String.format("%07.2f", frequency));
            if (!isPlaying()) {
                updatePianoButtons(guess.tone.getKey());
                if (!isKeyboard) {
                    circle.setTone(guess.tone, guessColor, pitchinessColor);
                }
            }
            circle.setFillColor(guessColor);
        });
    }

    private void updateSlider(Pitch pitch, float frequency, boolean isKeyboard) {
        if (!isKeyboard) {
            Pitch previous = this.lastPitch;
            this.lastPitch = pitch;
            //To ignore the octave jitter as the base frequency might be detected differently
            if (previous != null
                    && previous.tone == pitch.tone
                    && previous.octave != pitch.octave) {
                frequency = previous.frequency;
                pitch = previous;
            }
        }
        int value = convertPitchToSlider(pitch, frequency);
        pitchSlider.setValue(value);
    }

    private int convertPitchToSlider(Pitch pitch, float frequency) {
        int value = pitch.midi * 100;
        //fixme: Polyphonic - multiple dots on the slider
        //fixme: Extract duplication
        if (frequency != 0) {
            double diff = frequency - pitch.frequency;
            Pitch pitchy;
            if (diff < 0) {
                pitchy = transposePitch(pitch, 0, -1);
            } else {
                pitchy = transposePitch(pitch, 0, +1);
            }
            double pitchyDiff = Math.abs(pitch.frequency - pitchy.frequency);
            double accuracy = Math.abs(diff) / pitchyDiff;
            accuracy = accuracy * 100;
            if (pitch.frequency < frequency) {
                value += accuracy;
            } else {
                value -= accuracy;
            }
        }
        return value;
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
            double diff = Math.abs(aPitch.frequency - pitch);
            if (diff < 5) {
                if (guess != null) {
                    if (Math.abs(guess.frequency - pitch) < diff) {
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
        toneSpinnersFrozen = true;
        try {
            Arrays.asList(toneSpinners).forEach(spinner -> spinner.setValue(0));
            Pitch[][] scale = getRiddler().scale;
            for (Pitch[] row : scale) {
                for (Pitch pitch : row) {
                    if (pitch != null && pitch != None) {
                        JSpinner spinner = toneSpinners[pitch.tone.ordinal()];
                        int value = (int) spinner.getValue();
                        spinner.setValue(value + 1);
                    }
                }
            }
            circle.setScaleTones(getScaleTones());
        } finally {
            toneSpinnersFrozen = false;
        }
    }

    private void updatePenaltySpinners() {
        for (int i = 0; i < penaltySpinners.length; i++) {
            JSpinner penaltySpinner = penaltySpinners[i];
            List<Triplet<Pitch, Pitch, Pitch>> penaltyList = penaltyLists.get(i);
            int value = penaltyList.size();
            penaltySpinner.setValue(value);
        }
    }

    private void scheduleHint() {
        long riddleTimestampMs = System.currentTimeMillis();
        this.riddleTimestampMs = riddleTimestampMs;
        SwingUtilities.invokeLater(circle::setTones);
        Hinter hinter = getHinter();
        if (hinter == Hinter.Never) {
            return;
        }
        asyncExecutor.schedule(() -> SwingUtilities.invokeLater(() -> {
            if (isPlaying() && riddleTimestampMs == this.riddleTimestampMs) {
                Pitch riddle = this.riddle.get();
                if (riddle != null) {
                    circle.setTones(riddle.tone);
                    Pitch prevRiddle = this.prevRiddle.get();
                    Pitch prevPrevRiddle = this.prevPrevRiddle.get();
                    if (prevRiddle != null && prevPrevRiddle != null) {
                        List<Triplet<Pitch, Pitch, Pitch>> penaltyList = penaltyLists.get(riddle.tone.ordinal());
                        int penaltyFactor = (int) penaltyFactorSpinner.getValue();
                        if (penaltyFactor > 0) {
                            penaltyRiddleTimestampMs = riddleTimestampMs;
                            Triplet<Pitch, Pitch, Pitch> penalty = new Triplet<>(prevPrevRiddle, prevRiddle, riddle);
                            debug("New penalty: " + penalty + ", other penalties for " + riddle.tone + ": " + penaltyList);
                            for (int i = 0; i < penaltyFactor; i++) {
                                penaltyList.add(penalty);
                            }
                            updatePenaltySpinners();
                        }
                    }
                }
            }
        }), hinter.delayMs, TimeUnit.MILLISECONDS);
    }

    private List<Pitch> shuffle() {
        List<Tone> tones = getScaleTones();
        //noinspection CollectionAddedToSelf
        tones.addAll(tones); // Better shuffling - allows the same note twice in a row
        Collections.shuffle(tones);
        List<Pitch> shuffled = addOctaves(tones);
        debug(shuffled + " are the new riddles without penalties");

        List<Pitch> result = createPenalties();
        result.addAll(shuffled);
        debug(result + " are the new riddles with penalties");

        Set<Triplet<Pitch, Pitch, Pitch>> reminders = new LinkedHashSet<>(penaltyReminders);
        for (Triplet<Pitch, Pitch, Pitch> reminder : reminders) {
            result.add(reminder.first);
            result.add(reminder.second);
            result.add(reminder.third);
        }
        debug(result + " are the new riddles with penalties and reminders");
        return result;
    }

    private List<Tone> getScaleTones() {
        List<Tone> tones = new ArrayList<>(toneSpinners.length * 2);
        for (int i = 0; i < TONES.length; i++) {
            int count = (int) toneSpinners[i].getValue();
            for (int j = 0; j < count; j++) {
                tones.add(TONES[i]);
            }
        }
        return tones;
    }

    private List<Pitch> createPenalties() {
        List<Pitch> result = new ArrayList<>();
        for (List<Triplet<Pitch, Pitch, Pitch>> aPenaltyList : penaltyLists) {
            List<Triplet<Pitch, Pitch, Pitch>> penaltyList = new LinkedList<>(aPenaltyList);
            Collections.shuffle(penaltyList);
            Triplet<Pitch, Pitch, Pitch> prevPenalty = null;
            int prevCount = 0;
            for (int i = 0; i < penaltyList.size(); i++) {
                Triplet<Pitch, Pitch, Pitch> penalty = penaltyList.get(i);
                int transpose = 0;
                if (prevPenalty != null && prevPenalty.equals(penalty)) {
                    int mod = prevCount++ % 3;
                    if (mod == 1) {
                        transpose = +1;
                    } else if (mod == 2) {
                        transpose = -1;
                    }
                    debug("Transpose penalty=" + transpose);
                } else {
                    prevCount = 0;
                }
                List<Integer> selectedOctaves = getSelectedOctaves();
                Pitch first = transposePitch(penalty.first, transpose, 0);
                Pitch second = transposePitch(penalty.second, transpose, 0);
                Pitch third = transposePitch(penalty.third, transpose, 0);
                if (!selectedOctaves.contains(first.octave)) {
                    first = penalty.first;
                }
                if (!selectedOctaves.contains(second.octave)) {
                    second = penalty.second;
                }
                if (!selectedOctaves.contains(third.octave)) {
                    third = penalty.third;
                }
                result.add(first);
                result.add(second);
                result.add(third);
                prevPenalty = penalty;
                if (i >= 7) { // Enough is enough
                    break;
                }
            }
        }
        return result;
    }

    private List<Pitch> order() {
        return Arrays.stream(getRiddler().scale)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }

    private List<Pitch> addOctaves(List<Tone> scale) {
        return scale.stream()
                .map(tone -> {
                    List<Integer> selectedOctaves = getSelectedOctaves();
                    Pitch pitch;
                    String name = tone.name();
                    if (selectedOctaves.isEmpty()) {
                        pitch = tone.getFugue().pitch;
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

    private List<Integer> getSelectedOctaves() {
        List<Integer> selectedOctaves = new ArrayList<>(ALL_OCTAVES.length);
        for (int i = 0; i < ALL_OCTAVES.length; i++) {
            JToggleButton octaveToggle = octaveToggles[i];
            if (octaveToggle.isSelected()) {
                selectedOctaves.add(ALL_OCTAVES[i]);
            }
        }
        return selectedOctaves;
    }

    private void fugue(MidiChannel midiChannel, Object[] fugue, boolean flashColors) {
        frozen = true;
        try {
            debug("Fugue=" + Arrays.toString(fugue));
            Pitch prev = null;
            for (Object next : fugue) {
                if (next == null) {
                    Thread.sleep(four);
                } else if (next instanceof Pitch) {
                    if (prev != null) {
                        Thread.sleep(four);
                        midiChannel.noteOff(prev.midi);
                    }
                    Pitch pitch = (Pitch) next;
                    if (pitch == None) {
                        Thread.sleep(sixteen);
                    } else {
                        midiChannel.noteOn(pitch.midi, 127);
                    }
                    if (flashColors) {
                        SwingUtilities.invokeLater(() -> {
                            updatePianoButton(pitch.tone.getKey(), true);
                            circle.setTones(pitch.tone);
                        });
                    }
                    prev = pitch;
                } else if (next instanceof Integer) {
                    Thread.sleep((Integer) next);
                    if (prev != null) {
                        midiChannel.noteOff(prev.midi);
                        if (flashColors) {
                            Button button = prev.tone.getKey();
                            SwingUtilities.invokeLater(() -> updatePianoButton(button, false));
                        }
                        prev = null;
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported element=" + next.getClass());
                }
            }
            if (prev != null) {
                Thread.sleep(four);
                midiChannel.noteOff(prev.midi);
                if (flashColors) {
                    Button button = prev.tone.getKey();
                    SwingUtilities.invokeLater(() -> updatePianoButton(button, false));
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
            Tone previous = lastGuess.getAndSet(guess.tone);
            if (force || previous == null || !previous.equals(guess.tone)) {
                long previousTimestampMs = lastGuessTimestampMs;
                lastGuessTimestampMs = System.currentTimeMillis();
                if (lastGuessTimestampMs - previousTimestampMs > 500) {
                    text("\n");
                }
//                String text = "<span style=\"background-color: #FFFF00\">This text is highlighted in yellow.</span>";
//                text(text);
                text(guess.tone.label);
//                text(guess.getTone().name().toLowerCase(), Color.LIGHT_GRAY, guess.getTone().getColor());
                text("\n");
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
            textArea.append(message);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
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

            Pitchenga pitchenga = new Pitchenga();
            pitchenga.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            pitchenga.pack();
            Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            //fixme: Change to center when saving to file is implemented
//        this.setSize((int) screenSize.getWidth(), (int) screenSize.getHeight());
            int width = 730;
            pitchenga.setSize(width, (int) screenSize.getHeight());
//        setLocation(screen.width / 2 - getSize().width / 2, screen.height / 2 - getSize().height / 2);
            //fixme: Should resize relatively + have a slider for the user to resize
//        riddlePanel.add(Box.createVerticalStrut((int) (pitchenga.getSize().getHeight() / 3)));

            pitchenga.setLocation(screenSize.width - pitchenga.getSize().width, screenSize.height / 2 - pitchenga.getSize().height / 2);
//            pitchenga.setLocation(0, screenSize.height / 2 - pitchenga.getSize().height / 2);
//        pitchenga.setLocation(10, screenSize.height / 2 - getSize().height / 2);
            pitchenga.setVisible(pitchenga.setup.mainFrameVisible);

            if (!pitchenga.setup.mainFrameVisible) {
                JFrame frame = new JFrame("Test");
                frame.setVisible(true);
            }
        });
    }

    private void initGui() {
        initIcon();
        this.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        this.add(mainPanel);
        mainPanel.setBackground(Color.DARK_GRAY);
        mainPanel.setLayout(new BorderLayout());

        JPanel guessPanel = new JPanel();
        mainPanel.add(guessPanel, BorderLayout.CENTER);
        guessPanel.setBackground(Color.DARK_GRAY);
        guessPanel.setLayout(new BorderLayout());

        guessPanel.add(circle, BorderLayout.CENTER);

        mainPanel.add(initPitchSlider(), BorderLayout.WEST);
        mainPanel.add(initTextArea(), BorderLayout.EAST);

        for (Button button : Button.values()) {
            keyButtons[button.ordinal()] = new JToggleButton(button.label);
        }
        JPanel pianoPanel = new JPanel(new BorderLayout());
        mainPanel.add(pianoPanel, BorderLayout.SOUTH);
        pianoPanel.add(initControlPanel(), BorderLayout.NORTH);
        pianoPanel.add(initChromaticPiano(), BorderLayout.CENTER);
        pianoPanel.add(initDiatonicPiano(), BorderLayout.SOUTH);

        updateToneSpinners();
    }

    private JScrollPane initTextArea() {
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
//        text.setContentType("text/html");
        textArea.setFont(COURIER);
        textArea.setEditable(false);
        textArea.setForeground(Color.LIGHT_GRAY);
        textArea.setBackground(Color.DARK_GRAY);
        textArea.setBorder(null);
//        text("<html>");
        for (int i = 0; i < 500; i++) { //There must be a better way
            text("    \n");
        }
        return scroll;
    }

    private JComponent initPitchSlider() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel frequencyPanel = new JPanel();
        panel.add(frequencyPanel, BorderLayout.SOUTH);
        frequencyPanel.setOpaque(false);
        frequencyPanel.add(frequencyLabel);
        frequencyLabel.setFont(COURIER);
        frequencyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        frequencyLabel.setForeground(Color.LIGHT_GRAY);

        panel.add(pitchSlider, BorderLayout.CENTER);
        pitchSlider.setEnabled(false);
        pitchSlider.setValue(0);
        pitchSlider.getModel().setMinimum(convertPitchToSlider(Pitch.Do1, 0f));
        pitchSlider.getModel().setMaximum(convertPitchToSlider(Pitch.Re6, 0f));
        pitchSlider.setMajorTickSpacing(100);
        pitchSlider.setPaintTicks(true);
        Hashtable<Integer, JLabel> dictionary = new Hashtable<>(Arrays.stream(PITCHES).collect(Collectors.toMap(
                pitch -> convertPitchToSlider(pitch, 0f),
                pitch -> {
                    String text = pitch.label;
                    JLabel label = new JLabel(text);
                    label.setFont(COURIER);
                    label.setOpaque(true);
//                    label.setForeground(pitch.tone.fontColor);
                    label.setBackground(pitch.tone.color);
//                    label.setBackground(pitch.tone.fontColor);
//                    label.setForeground(Color.WHITE);
                    return label;
                })));
        pitchSlider.setLabelTable(dictionary);
        pitchSlider.setPaintLabels(true);
        //fixme: Scroll without bars
//        JScrollPane scroll = new JScrollPane(pitchSlider);
        return panel;
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
                if (!playButton.hasFocus()) {
                    playButton.requestFocus();
                    playButton.setSelected(!playButton.isSelected());
                    handlePlayButton();
                }
            }
            if (event.getKeyCode() == KeyEvent.VK_SHIFT) {
                lift = pressed;
            }
            if (event.getKeyCode() == KeyEvent.VK_CONTROL) {
                fall = pressed;
            }
            Button button = KEY_BY_CODE.get(event.getKeyCode());
            if (button == null) {
                return false;
            }
            handleKey(button, pressed);
            return true;
        });
    }

    private void handleKey(Button button, boolean pressed) {
        if (button.pitch == null) {
            return;
        }
//        if (pressed /* && isFuguePiano */) {
//            fugue(guitar, button.pitch.tone.fugue.tune, true);
//            return;
//        }
        updatePianoButton(button, pressed);
        Pitch pitch = button.pitch;
        if (fall) {
            pitch = transposePitch(button.pitch, -1, 0);
        }
        if (lift) {
            pitch = transposePitch(button.pitch, 1, 0);
        }
        int midi = pitch.midi;
        if (pressed) {
            updatePitch(pitch, pitch.frequency, 1, 42, true);
            if (!pressedKeyToMidi.containsKey(button)) { // Cannot just put() and check the previous value because it overrides the modified midi via OS's key repetition
                pressedKeyToMidi.put(button, midi);
                transcribe(pitch, true);
                brightPiano.noteOn(midi, 127);
            }
        } else {
            Integer modifiedMidi = pressedKeyToMidi.remove(button);
            if (modifiedMidi != null) {
                midi = modifiedMidi;
            }
            brightPiano.noteOff(midi);
            playExecutor.execute(() -> play(button.pitch));
        }
        Tone[] tones = pressedKeyToMidi.keySet().stream().map(k -> k.pitch.tone).toArray(Tone[]::new);
        circle.setTones(tones);
    }

    private void updatePianoButton(Button button, boolean pressed) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalMonitorStateException();
        }
        keyButtons[button.ordinal()].setSelected(pressed);
    }

    private void updatePianoButtons(Button button) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalMonitorStateException();
        }
        for (Button k : BUTTONS) {
            JToggleButton keyButton = keyButtons[k.ordinal()];
            keyButton.setSelected(k.pitch != null && k.pitch.tone.equals(button.pitch.tone));
        }
    }

    private JPanel initControlPanel() {
        JPanel controlPanelPanel = new JPanel();
        controlPanelPanel.setBackground(Color.DARK_GRAY);
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(Color.DARK_GRAY);
        controlPanelPanel.add(controlPanel);
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.add(initButtonsPanel());
        controlPanel.add(initInputCombo());
        controlPanel.add(initPitchAlgoCombo());
        controlPanel.add(initHinterCombo());
        controlPanel.add(initPacerCombo());
        controlPanel.add(initRiddleRingerCombo());
        controlPanel.add(initGuessRingerCombo());
        controlPanel.add(initRiddlerCombo());
        controlPanel.add(initOctavesPanel());
        return controlPanelPanel;
    }

    private JPanel initOctavesPanel() {
        JPanel octavesPanel = new JPanel();
        octavesPanel.setBackground(Color.DARK_GRAY);
        JPanel penaltyFactorPanel = new JPanel();
        penaltyFactorPanel.setBackground(Color.DARK_GRAY);
        octavesPanel.add(penaltyFactorPanel);
        penaltyFactorPanel.add(new JLabel("Penalty Factor:"));
        penaltyFactorPanel.add(penaltyFactorSpinner);
        penaltyFactorSpinner.setValue(setup.defaultPenaltyFactor);

        octavesPanel.add(new JLabel(" "));
        octavesPanel.add(new JLabel("Octaves:"));

        for (int i = 0; i < ALL_OCTAVES.length; i++) {
            Integer octave = ALL_OCTAVES[i];
            JToggleButton toggle = new JToggleButton("" + octave);
            octaveToggles[i] = toggle;
            octavesPanel.add(toggle);
            toggle.setSelected(Arrays.asList(setup.defaultOctaves).contains(octave));
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

    private JPanel initChromaticPiano() {
        JPanel panel = new JPanel(new GridLayout(1, TONES.length));
        panel.setBackground(Color.DARK_GRAY);

        for (Tone tone : TONES) {
            JPanel colorPanel = new JPanel();
            panel.add(colorPanel);
            colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.Y_AXIS));
            colorPanel.setBackground(tone.color);

            JPanel spinnerPanel = new JPanel();
            colorPanel.add(spinnerPanel);
            spinnerPanel.setOpaque(false);
            JSpinner toneSpinner = toneSpinners[tone.ordinal()];
            spinnerPanel.add(toneSpinner);
            ((JSpinner.DefaultEditor) toneSpinner.getEditor()).getTextField().setEditable(false);
            ((JSpinner.DefaultEditor) toneSpinner.getEditor()).getTextField().setFont(new Font("SansSerif", Font.PLAIN, 9));
            toneSpinner.setAlignmentX(Component.CENTER_ALIGNMENT);
            toneSpinner.addChangeListener(event -> {
                circle.setScaleTones(getScaleTones());
                if (!toneSpinnersFrozen) {
                    playExecutor.execute(() -> {
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

            JLabel colorLabel = new JLabel(tone.label);
            colorPanel.add(colorLabel);
            colorLabel.setFont(COURIER);
            colorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            colorLabel.setForeground(Color.WHITE);
            colorLabel.setBackground(Color.BLACK);
            colorLabel.setOpaque(true);

            colorPanel.add(Box.createVerticalStrut(10));

            Button button = null;
            for (Button aButton : Button.values()) {
                if (aButton.pitch != null
                        && aButton.row == 0
                        && aButton.pitch.tone.equals(tone)) {
                    button = aButton;
                    break;
                }
            }
            if (button != null) {
                Button theButton = button;
                JToggleButton keyButton = keyButtons[button.ordinal()];
                colorPanel.add(keyButton);
                keyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                keyButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        handleKey(theButton, true);
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        handleKey(theButton, false);
                    }
                });
                keyButton.setForeground(button == Button.n05 ? Color.BLACK : Color.WHITE);
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

    //fixme: Variable-length, e.g one octave for vertical phone orientation, 1,5 for horizontal, arbitrary
    private JPanel initDiatonicPiano() {
        JPanel panel = new JPanel(new GridLayout(2, 1));

        JPanel topPanelPanel = new JPanel(new BorderLayout());
        panel.add(topPanelPanel);
        topPanelPanel.setBackground(Color.DARK_GRAY);
        JPanel topPanel = new JPanel();
        panel.add(topPanelPanel, BorderLayout.NORTH);

        Component frontStrut = Box.createHorizontalStrut(20);
        topPanelPanel.add(frontStrut, BorderLayout.EAST);
        frontStrut.setBackground(Color.DARK_GRAY);

        topPanelPanel.add(topPanel, BorderLayout.CENTER);
        topPanel.setBackground(Color.DARK_GRAY);
        topPanel.setLayout(new GridLayout(1, 11));

        Component rearStrut = Box.createHorizontalStrut(20);
        topPanelPanel.add(rearStrut, BorderLayout.WEST);
        rearStrut.setBackground(Color.DARK_GRAY);

        JPanel bottomPanel = new JPanel();
        panel.add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.setBackground(Color.DARK_GRAY);
        bottomPanel.setLayout(new GridLayout(1, 11));

        Button[] buttons = Button.values();
        for (Button button : buttons) {
            if (button.row < 1) {
                continue;
            }
            JPanel colorPanel = new JPanel();
            if (button.pitch == null) {
                topPanel.add(colorPanel);
                colorPanel.setBackground(Color.DARK_GRAY);
            }
            if (button.pitch != null) {
                if (button.row == 1) {
                    topPanel.add(colorPanel);
                } else {
                    bottomPanel.add(colorPanel);
                }
            }
            colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.Y_AXIS));
            Color color = button.pitch == null ? Color.DARK_GRAY : button.pitch.tone.color;
            colorPanel.setBackground(color);

            colorPanel.add(Box.createVerticalStrut(5));
            JLabel colorLabel = new JLabel(button.pitch == null ? "    " : button.pitch.tone.label);
            colorPanel.add(colorLabel);
            colorLabel.setFont(COURIER);
            colorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            colorLabel.setForeground(Color.WHITE);
            colorLabel.setBackground(Color.BLACK);
            colorLabel.setOpaque(button.pitch != null);
            colorPanel.add(Box.createVerticalStrut(5));

            if (button.label != null) {
                JToggleButton keyButton = keyButtons[button.ordinal()];
                colorPanel.add(keyButton);
                keyButton.setAlignmentX(Component.CENTER_ALIGNMENT);

                if (button.pitch != null) {
                    keyButton.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            handleKey(button, true);
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            handleKey(button, false);
                        }
                    });
                    keyButton.setForeground(button == Button.F || button == Button.J ? Color.BLACK : Color.WHITE);
                    keyButton.setBackground(Color.DARK_GRAY);
                    keyButton.setEnabled(false);
                }
            }

            colorPanel.add(Box.createVerticalStrut(5));

            colorPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleKey(button, true);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleKey(button, false);
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
        pacerCombo.setSelectedItem(setup.defaultPacer);
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
                            Integer[] octaves = riddler.octaves;
                            if (riddler.octaves == null) {
                                octaves = setup.defaultOctaves;
                            }
                            List<Integer> riddlerOctaves = Arrays.asList(octaves);
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
        riddlerCombo.setSelectedItem(setup.defaultRiddler);
        return riddlerCombo;
    }

    private JComboBox<Hinter> initHinterCombo() {
        for (Hinter hinter : Hinter.values()) {
            hinterCombo.addItem(hinter);
        }
        hinterCombo.setMaximumRowCount(Hinter.values().length);
        hinterCombo.setSelectedItem(setup.defaultHinter);
        hinterCombo.addItemListener(event -> scheduleHint());
        return hinterCombo;
    }

    private JComboBox<GuessRinger> initGuessRingerCombo() {
        for (GuessRinger guessRinger : GuessRinger.values()) {
            guessRingerCombo.addItem(guessRinger);
        }
        guessRingerCombo.setMaximumRowCount(GuessRinger.values().length);
        guessRingerCombo.setSelectedItem(setup.defaultGuessRinger);
        return guessRingerCombo;
    }

    private JComboBox<RiddleRinger> initRiddleRingerCombo() {
        for (RiddleRinger ringer : RiddleRinger.values()) {
            riddleRingerCombo.addItem(ringer);
        }
        riddleRingerCombo.setMaximumRowCount(RiddleRinger.values().length);
        riddleRingerCombo.setSelectedItem(setup.defaultRiddleRinger);
        return riddleRingerCombo;
    }

    private JComboBox<PitchEstimationAlgorithm> initPitchAlgoCombo() {
        for (PitchEstimationAlgorithm pitchAlgo : PitchEstimationAlgorithm.values()) {
            pitchAlgoCombo.addItem(pitchAlgo);
        }
        pitchAlgoCombo.setMaximumRowCount(PitchEstimationAlgorithm.values().length);
        pitchAlgoCombo.setSelectedItem(setup.defaultPitchAlgo);
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

    private JPanel initButtonsPanel() {
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
        executor.execute(() -> SwingUtilities.invokeLater(circle::clear));
        if (playing) {
            resetGame();
            playButton.setText("Stop");
            executor.execute(() -> play(null));
        } else {
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
        penaltyReminders.clear();
        SwingUtilities.invokeLater(this::updatePenaltySpinners);
    }

    @SuppressWarnings("RedundantSuppression")
    private Mixer.Info getDefaultInput(List<Mixer.Info> inputs) {
        //noinspection ConstantConditions
        if (setup.defaultAudioInput != null) {
            return setup.defaultAudioInput;
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
            if (mixerInfo == null || mixerInfo == Setup.NO_AUDIO_INPUT) {
                System.out.println("No audio input selected, play using keyboard or mouse");
                System.out.println("To play using a musical instrument please select an audio input");
                return;
            }
            PitchEstimationAlgorithm pitchAlgoOrNull = (PitchEstimationAlgorithm) pitchAlgoCombo.getSelectedItem();
            PitchEstimationAlgorithm pitchAlgo = pitchAlgoOrNull == null ? setup.defaultPitchAlgo : pitchAlgoOrNull;
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
                    System.out.println("Listening to " + mixer.getMixerInfo().getName());
                    audioDispatcher.run();
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Stopped listening to " + mixer.getMixerInfo().getName());
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
        result.add(Setup.NO_AUDIO_INPUT);
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

    public static Pitch transposePitch(Pitch pitch, int octaves, int steps) {
        steps += TONES.length * octaves;
        int ordinal = pitch.ordinal() + steps;
        while (ordinal < 0) {
            ordinal += TONES.length;
        }
        while (ordinal >= PITCHES.length) {
            ordinal -= TONES.length;
        }
        return PITCHES[ordinal];
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
        Image image = Toolkit.getDefaultToolkit().getImage(Circle.class.getResource("/pitchenga.png"));
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

    //fixme: Move to Scale
    private static final Pitch[][] CHROMATIC_SCALE_MI3_LA5_MI3 = new Pitch[][]{
            {Mi3, Fa3, Fi3, So3, Le3, None, La3, None},
            {La3, Se3, Si3, Do4, Ra4, None, Re4, None},
            {Re4, Me4, Mi4, Fa4, Fi4, None, So4, None},
            {So4, Le4, La4, Se4, Si4, None, Do5, None},
            {Si4, Do5, Ra5, Re5, Me5, None, Mi5, None},
            {Mi5, Fa5, Fi5, So5, Le5, None, La5, None},

            {La5, Le5, So5, Fi5, Fa5, None, Mi5, None},
            {Mi5, Me5, Re5, Ra5, Do5, None, Si4, None},
            {Do5, Si4, Se4, La4, Le4, None, So4, None},
            {So4, Fi4, Fa4, Mi4, Me4, None, Re4, None},
            {Re4, Ra4, Do4, Si3, Se3, None, La3, None},
            {La3, Le3, So3, Fi3, Fa3, None, Mi3, None},
    };

    private static final Pitch[][] CHROMATIC_SCALE_MI3_LA5_MI3_UP_DOWN_UP = new Pitch[][]{
            {Mi3, Fa3, Fi3, So3, Le3, None, La3, None},
            {La3, Le3, So3, Fi3, Fa3, None, Mi3, None},
            {Mi3, Fa3, Fi3, So3, Le3, None, La3, None},

            {La3, Se3, Si3, Do4, Ra4, None, Re4, None},
            {Re4, Ra4, Do4, Si3, Se3, None, La3, None},
            {La3, Se3, Si3, Do4, Ra4, None, Re4, None},

            {Re4, Me4, Mi4, Fa4, Fi4, None, So4, None},
            {So4, Fi4, Fa4, Mi4, Me4, None, Re4, None},
            {Re4, Me4, Mi4, Fa4, Fi4, None, So4, None},

            {So4, Le4, La4, Se4, Si4, None, Do5, None},
            {Do5, Si4, Se4, La4, Le4, None, So4, None},
            {So4, Le4, La4, Se4, Si4, None, Do5, None},

            {Si4, Do5, Ra5, Re5, Me5, None, Mi5, None},
            {Mi5, Me5, Re5, Ra5, Do5, None, Si4, None},
            {Si4, Do5, Ra5, Re5, Me5, None, Mi5, None},

            {Mi5, Fa5, Fi5, So5, Le5, None, La5, None},
            {La5, Le5, So5, Fi5, Fa5, None, Mi5, None},
            {Mi5, Fa5, Fi5, So5, Le5, None, La5, None},


            {La5, Le5, So5, Fi5, Fa5, None, Mi5, None},
            {Mi5, Fa5, Fi5, So5, Le5, None, La5, None},
            {La5, Le5, So5, Fi5, Fa5, None, Mi5, None},

            {Mi5, Me5, Re5, Ra5, Do5, None, Si4, None},
            {Si4, Do5, Ra5, Re5, Me5, None, Mi5, None},
            {Mi5, Me5, Re5, Ra5, Do5, None, Si4, None},

            {Do5, Si4, Se4, La4, Le4, None, So4, None},
            {So4, Le4, La4, Se4, Si4, None, Do5, None},
            {Do5, Si4, Se4, La4, Le4, None, So4, None},

            {So4, Fi4, Fa4, Mi4, Me4, None, Re4, None},
            {Re4, Me4, Mi4, Fa4, Fi4, None, So4, None},
            {So4, Fi4, Fa4, Mi4, Me4, None, Re4, None},

            {Re4, Ra4, Do4, Si3, Se3, None, La3, None},
            {La3, Se3, Si3, Do4, Ra4, None, Re4, None},
            {Re4, Ra4, Do4, Si3, Se3, None, La3, None},

            {La3, Le3, So3, Fi3, Fa3, None, Mi3, None},
            {Mi3, Fa3, Fi3, So3, Le3, None, La3, None},
            {La3, Le3, So3, Fi3, Fa3, None, Mi3, None},
    };

    @SuppressWarnings("unused") //fixme: They are all used in the combo box!
    public enum Riddler {
        Chromatic("Chromatic - main octaves", new Pitch[][]{CHROMATIC_SCALE}, Pitchenga::shuffle, null),
        ChromaticOneOctave("Chromatic - 1 octave", new Pitch[][]{CHROMATIC_SCALE}, Pitchenga::shuffle, new Integer[0]),
        DoMaj("Do maj - main octaves", new Pitch[][]{DO_MAJ_SCALE}, Pitchenga::shuffle, null),
        DoMajOneOctave("Do maj - 1 octave", new Pitch[][]{DO_MAJ_SCALE}, Pitchenga::shuffle, new Integer[0]),
        ChromaticWithDoubledDiatonic("Chromatic with doubled Do maj - main octaves", new Pitch[][]{CHROMATIC_SCALE, DO_MAJ_SCALE}, Pitchenga::shuffle, null),
        ChromaticWithDoubledSharps("Chromatic with doubled sharps - main octaves", new Pitch[][]{CHROMATIC_SCALE, SHARPS_SCALE}, Pitchenga::shuffle, null),
        ChromaticScaleUpDown("Chromatic scale Mi3-Le5-Mi3", CHROMATIC_SCALE_MI3_LA5_MI3, Pitchenga::order, new Integer[]{3, 4, 5}),
        ChromaticScaleUpDownUp("Chromatic scale Mi3-Le5-Mi3 extended", CHROMATIC_SCALE_MI3_LA5_MI3_UP_DOWN_UP, Pitchenga::order, new Integer[]{3, 4, 5}),
        //fixme: Add scales C, Am, D, etc
        //fixme: Add random within scales
        SharpsOnly("Sharps only - main octaves", new Pitch[][]{SHARPS_SCALE}, Pitchenga::shuffle, null),
        LaDo("Step 1) La, Do", new Pitch[][]{{La0, Do0}}, Pitchenga::shuffle, null),
        SoLaDo("Step 2) So, La, Do", new Pitch[][]{{So0, La0, Do0}}, Pitchenga::shuffle, null),
        MiSoLaDo("Step 3) Mi, So, La, Do", new Pitch[][]{{Mi0, Mi0, So0, La0, Do0}}, Pitchenga::shuffle, null),
        FaMiSoLaDo("Step 4) Fa, Mi, So, La, Do", new Pitch[][]{{Fa0, Fa0, Mi0, So0, La0, Do0}}, Pitchenga::shuffle, null),
        ReFaMiSoLaDo("Step 5) Re, Fa, Mi, So, La, Do", new Pitch[][]{{Re0, Re0, Fa0, Mi0, So0, La0, Do0}}, Pitchenga::shuffle, null),
        SiReFaMiSoLaDo("Step 6) Si, Re, Fa, Mi, So, La, Do", new Pitch[][]{{Si0, Si0, Re0, Fa0, Mi0, So0, La0, Do0}}, Pitchenga::shuffle, null),
        DiatonicPlusLe("Step 7) Diatonic + Le", new Pitch[][]{DO_MAJ_SCALE, {Le0, Le0}}, Pitchenga::shuffle, null),
        DiatonicPlusFiLe("Step 8) Diatonic + Fi, Le", new Pitch[][]{DO_MAJ_SCALE, {Fi0, Fi0, Le0}}, Pitchenga::shuffle, null),
        DiatonicPlusRaFiLe("Step 9) Diatonic + Ra, Fi, Le", new Pitch[][]{DO_MAJ_SCALE, {Ra0, Ra0, Fi0, Le0}}, Pitchenga::shuffle, null),
        DiatonicPlusSeRaFiLe("Step 10) Diatonic + Se, Ra, Fi, Le", new Pitch[][]{DO_MAJ_SCALE, {Se0, Se0, Ra0, Fi0, Le0}}, Pitchenga::shuffle, null),
        DiatonicPlusMeSeRaFiLe("Step 11) Diatonic + Me, Se, Ra, Fi, Le", new Pitch[][]{DO_MAJ_SCALE, {Me0, Me0, Se0, Ra0, Fi0, Le0}}, Pitchenga::shuffle, null);

        private final String name;
        private final Pitch[][] scale;
        private final Function<Pitchenga, List<Pitch>> riddleAction;
        private final Integer[] octaves;

        Riddler(String name, Pitch[][] scale, Function<Pitchenga, List<Pitch>> riddleAction, Integer[] octaves) {
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
        None("Ring nothing", pitch -> new Object[]{thirtyTwo}),
        Tune("Ring mnemonic tune", pitch -> pitch.tone.getFugue().tune),
        Tone("Ring tone", pitch -> new Object[]{pitch.tone.getFugue().pitch, sixteen, eight}),
        ToneAndDo("Ring tone and Do", pitch -> new Object[]{pitch.tone.getFugue().pitch, eight, Do.getFugue().pitch, eight}),
        JustDo("Ring Do", pitch -> new Object[]{Do.getFugue().pitch, thirtyTwo, sixteen}),
        JustRa("Ring Ra", pitch -> new Object[]{Ra.getFugue().pitch, thirtyTwo, sixteen}),
        JustRe("Ring Re", pitch -> new Object[]{Re.getFugue().pitch, thirtyTwo, sixteen}),
        JustMe("Ring Me", pitch -> new Object[]{Me.getFugue().pitch, thirtyTwo, sixteen}),
        JustMi("Ring Mi", pitch -> new Object[]{Mi.getFugue().pitch, thirtyTwo, sixteen}),
        JustFa("Ring Fa", pitch -> new Object[]{Fa.getFugue().pitch, thirtyTwo, sixteen}),
        JustFi("Ring Fi", pitch -> new Object[]{Fi.getFugue().pitch, thirtyTwo, sixteen}),
        JustSo("Ring So", pitch -> new Object[]{So.getFugue().pitch, thirtyTwo, sixteen}),
        JustLe("Ring Le", pitch -> new Object[]{Le.getFugue().pitch, thirtyTwo, sixteen}),
        JustLa("Ring La", pitch -> new Object[]{La.getFugue().pitch, thirtyTwo, sixteen}),
        JustSe("Ring Se", pitch -> new Object[]{Se.getFugue().pitch, thirtyTwo, sixteen}),
        JustSi("Ring Si", pitch -> new Object[]{Si.getFugue().pitch, thirtyTwo, sixteen}),
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
        Tone("Riddle tone", pitch -> new Object[]{pitch, eight}),
        ShortToneAndLongPause("Riddle shorter tone with longer pause (for acoustic instruments)", pitch -> new Object[]{pitch, eight, four, sixteen}), //Otherwise the game plays with itself through the microphone by picking up the "tail". This could probably be improved with a shorter midi decay.
        ToneAndDo("Riddle tone and Do", pitch -> new Object[]{pitch.tone.getFugue().pitch, Do.getFugue().pitch, sixteen, four}),
        ;
        private final String name;
        private final Function<Pitch, Object[]> ring;

        RiddleRinger(String name, Function<Pitch, Object[]> ring) {
            this.name = name;
            this.ring = ring;
        }

        private static Object[] transposeTune(Pitch pitch) {
            int shift = pitch.octave - pitch.tone.getFugue().pitch.octave;
            return transposeFugue(pitch.tone.getFugue().tune, shift);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @SuppressWarnings("unused") //fixme: They are all used in the combo box!
    public enum Hinter {
        Always("Hint: immediately", 0),
        Delayed100("Hint: after 100 ms", 100),
        Delayed200("Hint: after 200 ms", 200),
        Delayed300("Hint: after 300 ms", 300),
        Delayed500("Hint: after 500 ms", 500),
        Delayed1000("Hint: after 1 second", 1000),
        Delayed2000("Hint: after 2 seconds", 2000),
        Never("Hint: never", Integer.MAX_VALUE);

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
        Tempo25("Tempo 25", pair -> pace(25)),
        Tempo30("Tempo 30", pair -> pace(30)),
        Tempo35("Tempo 35", pair -> pace(35)),
        Tempo40("Tempo 40", pair -> pace(40)),
        Tempo45("Tempo 45", pair -> pace(45)),
        Tempo50("Tempo 50", pair -> pace(50)),
        Tempo55("Tempo 55", pair -> pace(55)),
        Tempo60("Tempo 60", pair -> pace(60)),
        Tempo65("Tempo 65", pair -> pace(65)),
        Tempo70("Tempo 70", pair -> pace(70)),
        Tempo75("Tempo 75", pair -> pace(75)),
        Tempo80("Tempo 80", pair -> pace(80)),
        Tempo85("Tempo 85", pair -> pace(85)),
        Tempo90("Tempo 90", pair -> pace(90)),
        Tempo95("Tempo 95", pair -> pace(95)),
        Tempo100("Tempo 100", pair -> pace(100)),
        Tempo105("Tempo 105", pair -> pace(105)),
        Tempo110("Tempo 110", pair -> pace(110)),
        Tempo115("Tempo 115", pair -> pace(115)),
        Tempo120("Tempo 120", pair -> pace(120)),
        Tempo125("Tempo 125", pair -> pace(125)),
        Tempo130("Tempo 130", pair -> pace(130)),
        Tempo135("Tempo 135", pair -> pace(135)),
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
            return pair.left != null && pair.left.tone.equals(pair.right.tone);
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
