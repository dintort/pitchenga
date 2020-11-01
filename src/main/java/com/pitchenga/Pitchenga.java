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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.pitchenga.Duration.*;
import static com.pitchenga.Pitch.*;
import static com.pitchenga.Tone.*;

public class Pitchenga extends JFrame implements PitchDetectionHandler {

    private static final boolean debug = "true".equalsIgnoreCase(System.getProperty("com.pitchenga.debug"));
    private static final Pitch[] PITCHES = Pitch.values();
    private static final Tone[] TONES = Tone.values();
    private static final Fugue[] FUGUES = Fugue.values();
    private static final Button[] BUTTONS = Button.values();
    private static final Integer[] ALL_OCTAVES = Arrays.stream(PITCHES).map(pitch -> pitch.octave).filter(octave -> octave >= 0).distinct().toArray(Integer[]::new);
    //fixme: Move to Scale
    private static final Pitch[] CHROMATIC_SCALE = Arrays.stream(FUGUES).map(fugue -> fugue.pitch).toArray(Pitch[]::new);
    private static final Pitch[] DO_MAJ_SCALE = Arrays.stream(FUGUES).filter(fugue -> fugue.pitch.tone.diatonic).map(fugue -> fugue.pitch).toArray(Pitch[]::new);
    //    private static final Pitch[] DO_MAJ_HARM_SCALE = new Pitch[]{Do3, Re3, Mi3, Fa3, So3, Le3, Si3, Do4};
    //    private static final Pitch[] SHARPS_SCALE = Arrays.stream(TONES).filter(tone -> !tone.diatonic).map(tone -> tone.getFugue().pitch).toArray(Pitch[]::new);
    private static final Map<Integer, Button> BUTTON_BY_CODE = Arrays.stream(Button.values()).collect(Collectors.toMap(button -> button.keyEventCode, button -> button));
    public static final Font COURIER = new Font(Font.MONOSPACED, Font.BOLD, 20);
    public static final Font SERIF = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    private final Setup setup = Setup.create();
    private final boolean isPrimary;
    private final Pitchenga secondary;
    private final ScheduledExecutorService asyncExecutor = Executors.newSingleThreadScheduledExecutor(new Threads("pitchenga-async"));
    //fixme: Bigger queue, but process them all in one go so that the buzzer goes off only once when multiple keys pressed
    private final BlockingQueue<Runnable> playQueue = new ArrayBlockingQueue<>(1);
    private final ExecutorService playExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, playQueue, new Threads("pitchenga-play"), new ThreadPoolExecutor.DiscardOldestPolicy());
    private final Random random = new Random();
    private volatile AudioDispatcher audioDispatcher;
    //fixme: +Selectors for instruments +Random instrument: 1) guitar/piano/sax 2) more 3) all
    private final MidiChannel buzzInstrument;
    private final MidiChannel keyboardInstrument;
    private final MidiChannel ringInstrument;

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
    private volatile long lastPacerTimestampMs = System.currentTimeMillis();
    private volatile long lastBuzzTimestampMs;
    private volatile boolean frozen = false;
    private final AtomicInteger seriesCounter = new AtomicInteger(0);
    private final Map<Button, Integer> pressedButtonToMidi = new HashMap<>(); // To ignore OS's key repeating when holding, also used to remember the modified midi code to release
    private volatile boolean fall = false; // Control - octave down
    private volatile boolean lift = false; // Shift - octave up
    private volatile Integer octaveShift = 0;

    private final Circle circle;
    private final JSpinner penaltyFactorSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 9, 1));
    private final JToggleButton[] octaveToggles = new JToggleButton[ALL_OCTAVES.length];
    private final JSpinner[] toneSpinners = Arrays.stream(FUGUES)
            .map(tone -> new JSpinner(new SpinnerNumberModel(0, 0, 9, 1)))
            .collect(Collectors.toList()).toArray(new JSpinner[0]);
    private final List<List<Triplet<Pitch, Pitch, Pitch>>> penaltyLists = Arrays.stream(FUGUES).map(tone -> new ArrayList<Triplet<Pitch, Pitch, Pitch>>()).collect(Collectors.toList());
    private final Set<Triplet<Pitch, Pitch, Pitch>> penaltyReminders = new LinkedHashSet<>();
    private final JComboBox<PitchEstimationAlgorithm> pitchAlgoCombo = new JComboBox<>();
    private final JComboBox<Hinter> hinterCombo = new JComboBox<>();
    private final JComboBox<Pacer> pacerCombo = new JComboBox<>();
    private final JComboBox<Buzzer> buzzerCombo = new JComboBox<>();
    private final JComboBox<Ringer> ringerCombo = new JComboBox<>();
    private final JComboBox<Riddler> riddlerCombo = new JComboBox<>();
    private final JComboBox<Mixer.Info> inputCombo = new JComboBox<>();
    private final JToggleButton playButton = new JToggleButton();
    private final JToggleButton[] keyButtons = new JToggleButton[Button.values().length];
    private final JLabel frequencyLabel = new JLabel("0000.00");
    private final JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL);
    private final JTextArea textArea = new JTextArea();
    private final JPanel bottomPanel;
    //    private final JTextPane text = new JTextPane();

    //fixme: Foreground colors don't work
    //fixme: Update the logo with the fixed Me color
    //fixme: Adjust the font size based on window dimensions for the remaining labels
    //fixme: Triangles are a bit uneven
    //fixme: Load/save/reset; auto-save to home folder
    //fixme: Random within all scales - repeat 5 times, then switch to another random scale +blues scales
    //fixme: Continuous gradient ring around the circle +slider
    //fixme: Change the slider knob color as well
    //fixme: Profiling
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
    //fixme: Port to iOS and Android
    //fixme: Documentation / how to play
    //fixme: Visualize chords - like this, but adjust the palette: https://glasses.withinmyworld.org/index.php/2012/08/18/chord-colors-perfect-pitch-and-synesthesia/#.XkVt9y2ZO24
    //fixme: Alternative color schemes from config files. E.g. https://www.nature.com/articles/s41598-017-18150-y/figures/2;  .put("Do", new Color(253, 203, 3)).put("Ra", new Color(65, 3, 75)).put("Re", new Color(3, 179, 253)).put("Me", new Color(244, 56, 6)).put("Mi", new Color(250, 111, 252)).put("Fa", new Color(2, 252, 37)).put("Fi", new Color(3, 88, 69)).put("So", new Color(252, 2, 2)).put("Le", new Color(16, 24, 106)).put("La", new Color(251, 245, 173)).put("Se", new Color(2, 243, 252)).put("Si", new Color(219, 192, 244))
    //fixme: Split view and controller
    //fixme: Customizable note names
    public Pitchenga(boolean isPrimary, Pitchenga secondary) {
        super("Pitchenga");
        this.isPrimary = isPrimary;
        this.secondary = secondary;
        if (secondary != null) {
            secondary.setAutoRequestFocus(false);
        }
        this.circle = new Circle();
        this.bottomPanel = new JPanel(new BorderLayout());

        try {
            Soundbank soundfont = MidiSystem.getSoundbank(this.getClass().getResourceAsStream("/FluidR3_GM.sf2"));
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            synthesizer.loadAllInstruments(soundfont);
            MidiChannel[] channels = synthesizer.getChannels();
            this.buzzInstrument = channels[0];
            this.keyboardInstrument = channels[1];
            this.ringInstrument = channels[2];
            initMidiInstruments(synthesizer);
        } catch (MidiUnavailableException | InvalidMidiDataException | IOException e) {
            throw new RuntimeException(e);
        }
        initGui();
        initKeyboard();
        updateMixer();
    }

    private void play(Pitch guess, boolean exact) {
        try {
            lastPacerTimestampMs = System.currentTimeMillis();
            if (frozen || !isPlaying()) {
                return;
            }
            Pitch riddle = riddle();
            if (riddle == null) {
                return;
            }
            boolean success = true;
            Pacer pacer = getPacer();
            if (pacer == Pacer.Answer) {
                success = checkAnswer(riddle, guess, exact);
            } else {
                pace(pacer.bpm);
            }
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

        Ringer ringer = getRinger();
        if (getPacer() == Pacer.Answer) {
            transcribe(riddle, false);
        }
        fugue(ringInstrument, ringer.ring.apply(riddle), true);

        this.playQueue.clear();
        playExecutor.execute(() -> play(null, false));
    }

    private Pitch updatePenalties(Pitch riddle) {
        Pitch prevRiddle = this.prevRiddle.get();
        Pitch prevPrevRiddle = this.prevPrevRiddle.get();
        if (penaltyRiddleTimestampMs != riddleTimestampMs) {
            List<Triplet<Pitch, Pitch, Pitch>> penaltyList = penaltyLists.get(riddle.tone.getFugue().ordinal());
            for (Iterator<Triplet<Pitch, Pitch, Pitch>> iterator = penaltyList.iterator(); iterator.hasNext(); ) {
                Triplet<Pitch, Pitch, Pitch> penalty = iterator.next();
                if (penalty.first.equals(prevPrevRiddle)
                        && penalty.second.equals(prevRiddle)
                        && penalty.third.equals(riddle)) {
                    debug("Removing penalty " + penalty);
                    iterator.remove();
                    debug("Remaining penalties for " + riddle.tone.getFugue() + ": " + penaltyList);
                    penaltyReminders.add(penalty);
                    break;
                }
            }
        }
        return prevRiddle;
    }

    private void incorrect(Pitch riddle) {
        Hinter hinter = getHinter();
        if (hinter != Hinter.Never && System.currentTimeMillis() - riddleTimestampMs >= hinter.delayMs) {
            showHint(riddle);
        }
        long lastBuzzTimestampMs = this.lastBuzzTimestampMs;
        this.lastBuzzTimestampMs = System.currentTimeMillis();
        if (this.lastBuzzTimestampMs - lastBuzzTimestampMs > 500) {
            frozen = true;
            try {
                fugue(buzzInstrument, getBuzzer().buzz.apply(riddle), false);
            } finally {
                frozen = false;
            }
        }
    }

    private Pitch riddle() {
        while (this.riddle.get() == null) {
            if (riddleQueue.size() == 0) {
                Riddler riddler = getRiddler();
                List<Pitch> riddles = riddler.riddle.apply(this);
                seriesCounter.set(0);
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
                int seriesCount = seriesCounter.getAndIncrement();
                scheduleHint(riddle, seriesCount);
                frozen = true;
                try {
                    boolean flashColors = getHinter() == Hinter.Always;
                    this.lastBuzzTimestampMs = System.currentTimeMillis();
                    fugue(buzzInstrument, getBuzzer().buzz.apply(riddle), flashColors);
                    playQueue.clear();
                } finally {
                    frozen = false;
                }
            }
        }
        return this.riddle.get();
    }

    private void pace(int bpm) {
        long delay = 60_000 / bpm;
        long prevTimestamp = lastPacerTimestampMs;
        long elapsed = System.currentTimeMillis() - prevTimestamp;
        if (elapsed < delay) {
            long diff = delay - elapsed;
            try {
                Thread.sleep(diff);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lastPacerTimestampMs = System.currentTimeMillis();
    }

    private boolean checkAnswer(Pitch riddle, Pitch guess, boolean exact) {
        if (riddle == null || guess == null) {
            return false;
        }
        List<Integer> octaves = getSelectedOctaves();
        if (exact && octaves.isEmpty()) {
            return riddle.equals(guess);
        } else {
            return riddle.tone.equals(guess.tone);
        }
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
                                    playExecutor.execute(() -> play(guess, false));
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
        if (debug && !isPlaying() && isPrimary) {
            debug(String.format(" %s | pitch=%.2fHz | probability=%.2f | rms=%.2f | diff=%.2f | pitchyDiff=%.2f | accuracy=%.2f | pitchiness=%.2f | guessRoundedColor=%s | pitchyColor=%s | guessColor=%s | borderColor=%s",
                    guess, frequency, probability, rms, diff, pitchyDiff, accuracy, pitchiness, info(toneColor), info(pitchy.tone.color), info(guessColor), info(pitchinessColor)));
        }
        SwingUtilities.invokeLater(() -> {
            updateSlider(guess, frequency, isKeyboard);
            frequencyLabel.setText(String.format("%07.2f", frequency));
            if (!isPlaying()) {
                updatePianoButtons(guess.tone.getButton());
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
        Arrays.asList(toneSpinners).forEach(spinner -> spinner.setValue(0));
        Pitch[][] scale = getRiddler().scale;
        //fixme: Duplicated Do will act weird when multiple octaves
        for (Pitch[] row : scale) {
            for (Pitch pitch : row) {
                if (pitch != null && pitch != None) {
                    JSpinner spinner = toneSpinners[pitch.getFugue().ordinal()];
                    int value = (int) spinner.getValue();
                    spinner.setValue(value + 1);
                }
            }
        }
        circle.setScaleTones(getScaleTones());
    }

    private void scheduleHint(Pitch riddle, int seriesCount) {
        if (riddle == null) {
            return;
        }
        long riddleTimestampMs = System.currentTimeMillis();
        this.riddleTimestampMs = riddleTimestampMs;
        Hinter hinter = getHinter();
        if (hinter == Hinter.Always) {
            SwingUtilities.invokeLater(() -> showHint(riddle));
            return;
        } else if (hinter == Hinter.Series) {
            SwingUtilities.invokeLater(() -> {
                if (isShowSeriesHint(seriesCount)) {
                    showHint(riddle);
                } else {
                    circle.setTones();
                    circle.setFillColor(null);
                }
            });
            return;
        } else if (hinter == Hinter.Never) {
            return;
        } else {
            SwingUtilities.invokeLater(() -> {
                circle.setTones();
                if (getPacer() != Pacer.Answer) {
                    circle.setFillColor(null);
                }
            });
        }
        asyncExecutor.schedule(() -> SwingUtilities.invokeLater(() -> {
            if (isPlaying() && riddleTimestampMs == this.riddleTimestampMs) {
                showHint(riddle);
                Pitch prevRiddle = this.prevRiddle.get();
                Pitch prevPrevRiddle = this.prevPrevRiddle.get();
                int penaltyFactor = (int) penaltyFactorSpinner.getValue();
                if (prevRiddle != null && prevPrevRiddle != null && penaltyFactor > 0) {
                    penaltyRiddleTimestampMs = riddleTimestampMs;
                    List<Triplet<Pitch, Pitch, Pitch>> penaltyList = penaltyLists.get(riddle.getFugue().ordinal());
                    Triplet<Pitch, Pitch, Pitch> penalty = new Triplet<>(prevPrevRiddle, prevRiddle, riddle);
                    debug("New penalty: " + penalty + ", other penalties for " + riddle.getFugue() + ": " + penaltyList);
                    for (int i = 0; i < penaltyFactor; i++) {
                        penaltyList.add(penalty);
                    }
                }
            }
        }), hinter.delayMs, TimeUnit.MILLISECONDS);
    }

    private boolean isShowSeriesHint(int seriesCount) {
        int mod = seriesCount % (setup.repeat * setup.series);
        return mod >= setup.series;
    }

    private void showHint(Pitch riddle) {
        circle.setTones(riddle.tone);
        if (getPacer() != Pacer.Answer) {
            circle.setFillColor(riddle.tone.color);
        }
        pitchSlider.setValue(convertPitchToSlider(riddle, riddle.frequency));
        frequencyLabel.setText(String.format("%07.2f", riddle.frequency));
    }

    private List<Pitch> deduplicate(Supplier<List<Pitch>> supplier) {
        List<Pitch> pitches;
        int attempts = 1024;
        while (true) {
            attempts--;
            debug("De-duplicating, attempts=" + attempts);
            pitches = supplier.get();
            if (!hasDuplicates(pitches) || attempts < 0) {
                return pitches;
            }
        }
    }

    private boolean hasDuplicates(List<Pitch> pitches) {
        Pitch previous = null;
        for (Pitch pitch : pitches) {
            if (pitch.equals(previous)) {
                return true;
            }
            previous = pitch;
        }
        return false;
    }

    private List<Pitch> shuffle() {
        return deduplicate(() -> {
            //fixme: Restore the manual scales
//        List<Pitch> pitches = getScalePitches();
            List<Pitch> pitches = ordered();
            //fixme: +Spinner for series length +Spinner for repeats
            while (pitches.size() % setup.series != 0) {
                int index = random.nextInt(pitches.size());
                pitches.add(pitches.get(index));
            }
            Collections.shuffle(pitches);
            List<Pitch> shuffled = addOctaves(pitches);
            debug(shuffled + " are the new riddles without penalties");
            int size = shuffled.size();
            List<Pitch> multi = new ArrayList<>(size * setup.repeat);
            for (int i = 0; i < size; i += setup.series) {
                for (int j = 0; j < setup.repeat; j++) {
                    for (int k = 0; k < setup.series; k++) {
                        multi.add(shuffled.get(i + k));
                    }
                }
            }
            debug(multi + " are the new riddles multiplied");

            List<Pitch> result = createPenalties();
            result.addAll(multi);
            debug(result + " are the new riddles with penalties");

            Set<Triplet<Pitch, Pitch, Pitch>> reminders = new LinkedHashSet<>(penaltyReminders);
            for (Triplet<Pitch, Pitch, Pitch> reminder : reminders) {
                result.add(reminder.first);
                result.add(reminder.second);
                result.add(reminder.third);
            }
            debug(result + " are the new riddles with penalties and reminders");
            return result;
        });
    }

    private List<Pitch> shuffleGroupSeries(boolean shuffleGroups) {
        Pitch[][] scale = getRiddler().scale;
        List<List<Pitch>> listLists = Arrays.stream(scale)
                .flatMap(group -> {
                    List<Pitch> pitches = deduplicate(() -> {
                        List<Pitch> list = new LinkedList<>(Arrays.asList(group));
                        Collections.shuffle(list);
                        while (list.size() % setup.series != 0) {
                            int index = random.nextInt(list.size());
                            list.add(list.get(index));
                        }
                        return list;
                    });
                    List<List<Pitch>> lists = new ArrayList<>(pitches.size());
                    List<Pitch> list = null;
                    for (int i = 0; i < pitches.size(); i++) {
                        Pitch pitch = pitches.get(i);
                        if (list == null || i % setup.series == 0) {
                            list = new ArrayList<>(setup.series * setup.repeat);
                            lists.add(list);
                        }
                        list.add(pitch);
                    }
                    return lists.stream();
                })
                .collect(Collectors.toList());
        if (shuffleGroups) {
            Collections.shuffle(listLists);
        }
        debug(listLists);
        List<Pitch> result = listLists.stream()
                .map(list -> {
                    List<List<Pitch>> multi = new ArrayList<>();
                    for (int i = 0; i < setup.repeat; i++) {
                        multi.add(list);
                    }
                    return multi;
                })
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        debug(result + " are the new riddles multiplied");
        return result;
    }

    private List<Pitch> getScalePitches() {
        List<Pitch> pitches = new ArrayList<>(toneSpinners.length * 2);
        for (int i = 0; i < FUGUES.length; i++) {
            int count = (int) toneSpinners[i].getValue();
            for (int j = 0; j < count; j++) {
                pitches.add(FUGUES[i].pitch);
            }
        }
        return pitches;
    }

    private List<Pitch> createPenalties() {
        List<Pitch> result = new ArrayList<>();
        for (List<Triplet<Pitch, Pitch, Pitch>> penaltyList : penaltyLists) {
            for (Triplet<Pitch, Pitch, Pitch> penalty : penaltyList) {
                result.add(penalty.first);
                result.add(penalty.second);
                result.add(penalty.third);
            }
        }
        return result;
    }

    private List<Pitch> ordered() {
        return Arrays.stream(getRiddler().scale)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }

    private List<Pitch> addOctaves(List<Pitch> scale) {
        return scale.stream()
                .map(pitch -> {
                    List<Integer> selectedOctaves = getSelectedOctaves();
                    if (!selectedOctaves.isEmpty()) {
                        int index = random.nextInt(selectedOctaves.size());
                        String toneName = pitch.tone.name() + (int) selectedOctaves.get(index);
                        pitch = Pitch.valueOf(toneName);
                    }
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
                            updatePianoButton(pitch.tone.getButton(), true);
                            circle.setTone(pitch.tone, pitch.tone.color, pitch.tone.color);
                        });
                    }
                    prev = pitch;
                } else if (next instanceof Integer) {
                    Thread.sleep((Integer) next);
                    if (prev != null) {
                        midiChannel.noteOff(prev.midi);
                        if (flashColors) {
                            Button button = prev.tone.getButton();
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
                    Button button = prev.tone.getButton();
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
                text("  ");
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

    static {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Pitchenga"); //fixme: Does not work

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.out.println("Thread=" + thread.getName() + ", error=" + throwable);
            throwable.printStackTrace();
        });

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
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                try {
                    UIManager.setLookAndFeel(info.getClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public static void main(String... strings) throws InterruptedException, InvocationTargetException {
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.xrender", "f");
        SwingUtilities.invokeAndWait(() -> new Pitchenga(true, null));
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

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.add(initControlPanel(), BorderLayout.NORTH);
        bottomPanel.add(initChromaticPiano(), BorderLayout.CENTER);
        bottomPanel.add(initDiatonicPiano(), BorderLayout.SOUTH);
        updateToneSpinners();
        updateOctaveToggles(getRiddler());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        //fixme: Change to center when saving to file is implemented
//        this.setSize((int) screenSize.getWidth(), (int) screenSize.getHeight());
        int width = 670;
        setSize(width, (int) screenSize.getHeight());
//        setLocation(screen.width / 2 - getSize().width / 2, screen.height / 2 - getSize().height / 2);
        //fixme: Should resize relatively + have a slider for the user to resize
//        riddlePanel.add(Box.createVerticalStrut((int) (pitchenga.getSize().getHeight() / 3)));

//            setLocation(0, screenSize.height / 2 - getSize().height / 2);
        setLocation(screenSize.width - getSize().width, screenSize.height / 2 - getSize().height / 2);
//        pitchenga.setLocation(10, screenSize.height / 2 - getSize().height / 2);
        setVisible(setup.mainFrameVisible);

        if (!setup.mainFrameVisible) {
            JFrame frame = new JFrame("Test");
            frame.setVisible(true);
        }
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
            text("        \n");
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
                    JLabel label = new JLabel(pitch.label);
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
        if (!isPrimary) {
            return;
        }
        manager.addKeyEventPostProcessor(event -> {
            if (event.isMetaDown()) {
                return false;
            }
            boolean pressed;
            if (event.getID() == KeyEvent.KEY_PRESSED) {
                pressed = true;
            } else if (event.getID() == KeyEvent.KEY_RELEASED) {
                pressed = false;
            } else {
                return false;
            }
//            debug("Frozen=" + frozen + ", key=" + event + ";");
            if (pressed && event.getKeyCode() == KeyEvent.VK_SPACE) {
                if (!playButton.hasFocus()) {
                    playButton.setSelected(!playButton.isSelected());
                    playButton.requestFocus();
                }
            }
            if (pressed && event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                playButton.setSelected(false);
                playButton.requestFocus();
            }
            if (event.getKeyCode() == KeyEvent.VK_SHIFT) {
                lift = pressed;
            }
            if (event.getKeyCode() == KeyEvent.VK_CONTROL) {
                fall = pressed;
            }
            if (event.getKeyCode() == KeyEvent.VK_Z) {
                octaveShift = -4;
            } else if (event.getKeyCode() == KeyEvent.VK_X) {
                octaveShift = -3;
            } else if (event.getKeyCode() == KeyEvent.VK_C) {
                octaveShift = -2;
            } else if (event.getKeyCode() == KeyEvent.VK_V) {
                octaveShift = -1;
            } else if (event.getKeyCode() == KeyEvent.VK_B) {
                octaveShift = 0;
            } else if (event.getKeyCode() == KeyEvent.VK_N) {
                octaveShift = 1;
            } else if (event.getKeyCode() == KeyEvent.VK_M) {
                octaveShift = 2;
            } else if (event.getKeyCode() == KeyEvent.VK_COMMA) {
                octaveShift = 3;
            } else if (event.getKeyCode() == KeyEvent.VK_PERIOD) {
                octaveShift = 4;
            }
            if (pressed && (event.getKeyCode() == KeyEvent.VK_OPEN_BRACKET
                    || event.getKeyCode() == KeyEvent.VK_CLOSE_BRACKET)) {
                int index = pacerCombo.getSelectedIndex();
                index = event.getKeyCode() == KeyEvent.VK_OPEN_BRACKET ? index - 1 : index + 1;
                if (index >= 0 && index < pacerCombo.getItemCount()) {
                    pacerCombo.setSelectedIndex(index);
                    text("   ");
                    text(String.valueOf(getPacer().bpm));
                    text("\n");
                }
                return true;
            }
            Button button = BUTTON_BY_CODE.get(event.getKeyCode());
            if (button == null) {
                return false;
            }
            handleButton(button, pressed);
            return true;
        });
    }

    private void handleButton(Button button, boolean pressed) {
        if (button.pitch == null) {
            return;
        }
//        if (pressed /* && isFuguePiano */) {
//            fugue(guitar, button.pitch.tone.fugue.tune, true);
//            return;
//        }
        updatePianoButton(button, pressed);
        Pitch thePitch = button.pitch;
        if (octaveShift != 0) {
            thePitch = transposePitch(button.pitch, octaveShift, 0);
        }
        if (fall && lift) {
            thePitch = transposePitch(thePitch, 2, 0);
        } else if (fall) {
            thePitch = transposePitch(thePitch, -1, 0);
        } else if (lift) {
            thePitch = transposePitch(thePitch, 1, 0);
        }
        debug("octaveShift=" + octaveShift + ", fall=" + fall + ", lift=" + lift + ", pitch=" + button.pitch + ", transposed=" + thePitch);
        Pitch pitch = thePitch;
        int midi = pitch.midi;
        if (pressed) {
            updatePitch(pitch, pitch.frequency, 1, 42, true);
            if (!pressedButtonToMidi.containsKey(button)) { // Cannot just put() and check the previous value because it overrides the modified midi via OS's key repetition
                pressedButtonToMidi.put(button, midi);
                transcribe(pitch, true);
                keyboardInstrument.noteOn(midi, 127);
            }
        } else {
            Integer modifiedMidi = pressedButtonToMidi.remove(button);
            if (modifiedMidi != null) {
                midi = modifiedMidi;
            }
            keyboardInstrument.noteOff(midi);
            if (getPacer() == Pacer.Answer) {
                playExecutor.execute(() -> play(pitch, true));
            }
        }
        Tone[] tones = pressedButtonToMidi.keySet().stream().map(k -> k.pitch.tone).toArray(Tone[]::new);
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
        for (Button aButton : BUTTONS) {
            JToggleButton keyButton = keyButtons[aButton.ordinal()];
            keyButton.setSelected(aButton.pitch != null && aButton.pitch.tone.equals(button.pitch.tone));
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
        controlPanel.add(initBuzzerCombo());
        controlPanel.add(initRingerCombo());
        JPanel octavesPanel = initOctavesPanel();
        controlPanel.add(initRiddlerCombo());
        controlPanel.add(octavesPanel);
        return controlPanelPanel;
    }

    private JPanel initOctavesPanel() {
        JPanel octavesPanel = new JPanel();
        octavesPanel.setBackground(Color.DARK_GRAY);
        JPanel penaltyFactorPanel = new JPanel();
        penaltyFactorPanel.setBackground(Color.DARK_GRAY);
        octavesPanel.add(penaltyFactorPanel);
        penaltyFactorPanel.add(new JLabel("Penalty:"));
        penaltyFactorPanel.add(penaltyFactorSpinner);
        penaltyFactorSpinner.setValue(setup.defaultPenaltyFactor);
        ((JSpinner.DefaultEditor) penaltyFactorSpinner.getEditor()).getTextField().setEditable(false);
        ((JSpinner.DefaultEditor) penaltyFactorSpinner.getEditor()).getTextField().setFont(SERIF);
        ((JSpinner.DefaultEditor) penaltyFactorSpinner.getEditor()).getTextField().setFocusable(false);
        penaltyFactorSpinner.addChangeListener(event -> stop());

        octavesPanel.add(new JLabel(" "));
        octavesPanel.add(new JLabel("Octaves:"));

        for (int i = 0; i < ALL_OCTAVES.length; i++) {
            Integer octave = ALL_OCTAVES[i];
            JToggleButton toggle = new JToggleButton("" + octave);
            octaveToggles[i] = toggle;
            octavesPanel.add(toggle);
            toggle.setSelected(Arrays.asList(setup.defaultOctaves).contains(octave));
            toggle.addItemListener(event -> stop());
        }
        return octavesPanel;
    }

    private JPanel initChromaticPiano() {
        Fugue[] fugues = Fugue.values();
        JPanel panel = new JPanel(new GridLayout(1, fugues.length));
        panel.setBackground(Color.DARK_GRAY);

        for (Fugue fugue : fugues) {
            Tone tone = fugue.pitch.tone;
            JPanel colorPanel = new JPanel();
            panel.add(colorPanel);
            colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.Y_AXIS));
            colorPanel.setBackground(tone.color);

            JPanel spinnerPanel = new JPanel();
            colorPanel.add(spinnerPanel);
            spinnerPanel.setOpaque(false);
            JSpinner toneSpinner = toneSpinners[fugue.ordinal()];
            spinnerPanel.add(toneSpinner);
            ((JSpinner.DefaultEditor) toneSpinner.getEditor()).getTextField().setEditable(false);
            ((JSpinner.DefaultEditor) toneSpinner.getEditor()).getTextField().setFont(SERIF);
            ((JSpinner.DefaultEditor) toneSpinner.getEditor()).getTextField().setFocusable(false);
            toneSpinner.setAlignmentX(Component.CENTER_ALIGNMENT);
            toneSpinner.setFocusable(false);
            toneSpinner.addChangeListener(event -> {
                List<Tone> scaleTones = getScaleTones();
                circle.setScaleTones(scaleTones);
                stop();
            });

            colorPanel.add(Box.createVerticalStrut(5));

            JLabel toneLabel = new JLabel(tone.label);
            colorPanel.add(toneLabel);
            toneLabel.setFont(COURIER);
            toneLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            toneLabel.setForeground(Color.WHITE);
            toneLabel.setBackground(Color.BLACK);
            toneLabel.setOpaque(true);

            colorPanel.add(Box.createVerticalStrut(10));

            Button button = null;
            for (Button aButton : Button.values()) {
                if (aButton.pitch != null
                        && aButton.row == 0
                        && aButton.pitch.equals(fugue.pitch)) {
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
                        handleButton(theButton, true);
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        handleButton(theButton, false);
                    }
                });
                keyButton.setForeground(Color.WHITE);
                keyButton.setBackground(Color.DARK_GRAY);
                keyButton.setEnabled(false);

                colorPanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        handleButton(theButton, true);
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        handleButton(theButton, false);
                    }
                });

            }
            colorPanel.add(Box.createVerticalStrut(10));

        }
        return panel;
    }

    private List<Tone> getScaleTones() {
        return getScalePitches().stream().map(p -> p.tone).collect(Collectors.toList());
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
                            handleButton(button, true);
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            handleButton(button, false);
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
                    handleButton(button, true);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleButton(button, false);
                }
            });
        }
        return panel;
    }

    private JComboBox<Pacer> initPacerCombo() {
        for (Pacer pacer : Pacer.values()) {
            pacerCombo.addItem(pacer);
        }
        pacerCombo.setFocusable(false);
        pacerCombo.setMaximumRowCount(Pacer.values().length);
        pacerCombo.setSelectedItem(setup.defaultPacer);
        return pacerCombo;
    }

    private JComboBox<Riddler> initRiddlerCombo() {
        for (Riddler riddler : Riddler.values()) {
            riddlerCombo.addItem(riddler);
        }
        riddlerCombo.setFocusable(false);
        riddlerCombo.setMaximumRowCount(Riddler.values().length);
        riddlerCombo.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                stop();
                updateOctaveToggles((Riddler) event.getItem());
                updateToneSpinners();
            }
        });
        riddlerCombo.setSelectedItem(setup.defaultRiddler);
        return riddlerCombo;
    }

    private void updateOctaveToggles(Riddler riddler) {
        Integer[] octaves = riddler.octaves;
        if (riddler.octaves == null) {
            octaves = setup.defaultOctaves;
        }
        List<Integer> riddlerOctaves = Arrays.asList(octaves);
        for (int i = 0; i < ALL_OCTAVES.length; i++) {
            Integer octave = ALL_OCTAVES[i];
            octaveToggles[i].setSelected(riddlerOctaves.contains(octave));
        }
    }

    private JComboBox<Hinter> initHinterCombo() {
        for (Hinter hinter : Hinter.values()) {
            hinterCombo.addItem(hinter);
        }
        hinterCombo.setFocusable(false);
        hinterCombo.setMaximumRowCount(Hinter.values().length);
        hinterCombo.setSelectedItem(setup.defaultHinter);
        hinterCombo.addItemListener(event -> stop());
        return hinterCombo;
    }

    private JComboBox<Ringer> initRingerCombo() {
        for (Ringer ringer : Ringer.values()) {
            ringerCombo.addItem(ringer);
        }
        ringerCombo.setFocusable(false);
        ringerCombo.setMaximumRowCount(Ringer.values().length);
        ringerCombo.setSelectedItem(setup.defaultRinger);
        ringerCombo.addItemListener(event -> stop());
        return ringerCombo;
    }

    private JComboBox<Buzzer> initBuzzerCombo() {
        for (Buzzer ringer : Buzzer.values()) {
            buzzerCombo.addItem(ringer);
        }
        buzzerCombo.setFocusable(false);
        buzzerCombo.setMaximumRowCount(Buzzer.values().length);
        buzzerCombo.setSelectedItem(setup.defaultBuzzer);
        buzzerCombo.addItemListener(event -> stop());
        return buzzerCombo;
    }

    private JComboBox<PitchEstimationAlgorithm> initPitchAlgoCombo() {
        for (PitchEstimationAlgorithm pitchAlgo : PitchEstimationAlgorithm.values()) {
            pitchAlgoCombo.addItem(pitchAlgo);
        }
        pitchAlgoCombo.setFocusable(false);
        pitchAlgoCombo.setMaximumRowCount(PitchEstimationAlgorithm.values().length);
        pitchAlgoCombo.setSelectedItem(setup.defaultPitchAlgo);
        pitchAlgoCombo.addActionListener(event -> asyncExecutor.execute(this::updateMixer));
        return pitchAlgoCombo;
    }

    private JComboBox<Mixer.Info> initInputCombo() {
        List<Mixer.Info> inputs = getAvailableInputs();
        inputCombo.setFocusable(false);
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
        playButton.setEnabled(isPrimary);
        playButton.addItemListener(event -> handlePlayButton());

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
        circle.clear();
        if (secondary != null) {
            secondary.setVisible(!playing);
        }
        if (playing) {
            resetGame();
            playButton.setText("Stop");
            playExecutor.execute(() -> play(null, false));
            if (!getPacer().equals(Pacer.Answer)) {
                bottomPanel.setVisible(false);
            }
        } else {
            playButton.setText("Play");
            bottomPanel.setVisible(true);
        }
        debug("running=" + playing);
    }

    private void stop() {
        playButton.setSelected(false);
        playButton.requestFocus();
    }

    private Hinter getHinter() {
        return (Hinter) hinterCombo.getSelectedItem();
    }

    private Riddler getRiddler() {
        return (Riddler) riddlerCombo.getSelectedItem();
    }

    private Buzzer getBuzzer() {
        return (Buzzer) buzzerCombo.getSelectedItem();
    }

    private Ringer getRinger() {
        return (Ringer) ringerCombo.getSelectedItem();
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
    }

    private Mixer.Info getDefaultInput(List<Mixer.Info> inputs) {
        if (setup.defaultAudioInput != null) {
            return setup.defaultAudioInput;
        }
        String defaultInputName = System.getProperty("com.pitchenga.default.input");
        if ("NO_AUDIO_INPUT".equals(defaultInputName)) {
            return null;
        }
        Mixer.Info defaultInput = null;
        if (inputs.size() > 0) {
            if (defaultInputName != null && !defaultInputName.isEmpty()) {
                for (Mixer.Info mixerInfo : inputs) {
                    if (defaultInputName.equals(mixerInfo.getName())) {
                        defaultInput = mixerInfo;
                        break;
                    }
                }
            }
            if (defaultInput == null) {
                for (Mixer.Info mixerInfo : inputs) {
                    if (mixerInfo.toString().toLowerCase().contains("default")) {
                        defaultInput = mixerInfo;
                        break;
                    }
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
            new Thread(dispatch, "pitchenga-mixer" + Threads.ID_COUNTER.incrementAndGet()).start();
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

    private void initMidiInstruments(Synthesizer synthesizer) {
        javax.sound.midi.Instrument[] instruments = synthesizer.getLoadedInstruments();
        javax.sound.midi.Instrument instrument = instruments[setup.buzzInstrument];
        if (synthesizer.loadInstrument(instrument)) {
            buzzInstrument.programChange(instrument.getPatch().getProgram());
        }
        instrument = instruments[setup.keyboardInstrument];
        if (synthesizer.loadInstrument(instrument)) {
            keyboardInstrument.programChange(instrument.getPatch().getProgram());
        }
        instrument = instruments[setup.ringInstrument];
        if (synthesizer.loadInstrument(instrument)) {
            ringInstrument.programChange(instrument.getPatch().getProgram());
        }
    }

    public static Pitch transposePitch(Pitch pitch, int octaves, int steps) {
        steps += TONES.length * octaves;
        int ordinal = pitch.ordinal() + steps;
        while (ordinal < Do0.ordinal()) {
            ordinal += TONES.length;
        }
        while (ordinal >= PITCHES.length) {
            ordinal -= TONES.length;
        }
        return PITCHES[ordinal];
    }

    //fixme: Extract duplication
//    @SuppressWarnings("SameParameterValue")
//    public static Pitch[] transposeScale(Pitch[] scale, int shiftOctaves, int shiftSteps) {
//        List<Pitch> transposed = new ArrayList<>(scale.length);
//        for (Pitch next : scale) {
//            next = transposePitch(next, shiftOctaves, shiftSteps);
//            transposed.add(next);
//        }
//        debug("Transposed scale: " + transposed);
//        return transposed.toArray(new Pitch[0]);
//    }

    public static Object[] transposeFugue(Object[] fugue, int shiftOctaves) {
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

    public static Object[] transposeFugue(Pitch target, Object[] fugue) {
        int shiftOctaves = target.octave - target.tone.getFugue().pitch.octave;
        return transposeFugue(fugue, shiftOctaves);
    }

    private static Object[] transposeTune(Pitch pitch) {
        return transposeFugue(pitch, pitch.tone.getFugue().tune);
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
            Method getApplication = clazz.getMethod("getApplication");
            Object application = getApplication.invoke(null);
            Method setDockIconImage = clazz.getMethod("setDockIconImage", Image.class);
            setDockIconImage.invoke(application, image);
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

    public enum Riddler {
        ChromaticOneOctave("Chromatic - 1 octave",
                new Pitch[][]{CHROMATIC_SCALE}, Pitchenga::shuffle, new Integer[0]),
        Chromatic("Chromatic - main octaves",
                new Pitch[][]{CHROMATIC_SCALE}, Pitchenga::shuffle, null),
        DoMajOneOctave("Do maj - 1 octave",
                new Pitch[][]{DO_MAJ_SCALE}, Pitchenga::shuffle, new Integer[0]),
        DoMaj("Do maj - main octaves",
                new Pitch[][]{DO_MAJ_SCALE}, Pitchenga::shuffle, null),
        //        DoMajHarm("Do maj harm - main octaves",
//                new Pitch[][]{DO_MAJ_HARM_SCALE}, Pitchenga::shuffle, null),
//        RaMaj("Ra maj - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_SCALE, 0, 1)}, Pitchenga::shuffle, null),
//        RaMajHarm("Ra maj harm - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_HARM_SCALE, 0, 1)}, Pitchenga::shuffle, null),
//        ReMaj("Re maj - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_SCALE, 0, 2)}, Pitchenga::shuffle, null),
//        ReMajHarm("Re maj harm - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_HARM_SCALE, 0, 2)}, Pitchenga::shuffle, null),
//        MeMaj("Me maj - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_SCALE, 0, 3)}, Pitchenga::shuffle, null),
//        MeMajHarm("Me maj harm - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_HARM_SCALE, 0, 3)}, Pitchenga::shuffle, null),
//        MiMaj("Mi maj - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_SCALE, 0, 4)}, Pitchenga::shuffle, null),
//        MiMajHarm("Mi maj harm - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_HARM_SCALE, 0, 4)}, Pitchenga::shuffle, null),
//        FaMaj("Fa maj - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_SCALE, 0, 5)}, Pitchenga::shuffle, null),
//        FaMajHarm("Fa maj harm - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_HARM_SCALE, 0, 5)}, Pitchenga::shuffle, null),
//        FiMaj("Fi maj - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_SCALE, 0, 6)}, Pitchenga::shuffle, null),
//        FiMajHarm("Fi maj harm - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_HARM_SCALE, 0, 6)}, Pitchenga::shuffle, null),
//        SoMaj("So maj - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_SCALE, 0, 7)}, Pitchenga::shuffle, null),
//        SoMajHarm("So maj harm - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_HARM_SCALE, 0, 7)}, Pitchenga::shuffle, null),
//        LeMaj("Le maj - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_SCALE, 0, 8)}, Pitchenga::shuffle, null),
//        LeMajHarm("Le maj harm - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_HARM_SCALE, 0, 8)}, Pitchenga::shuffle, null),
//        LaMaj("La maj - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_SCALE, 0, 9)}, Pitchenga::shuffle, null),
//        LaMajHarm("La maj harm - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_HARM_SCALE, 0, 9)}, Pitchenga::shuffle, null),
//        SeMaj("Se maj - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_SCALE, 0, 10)}, Pitchenga::shuffle, null),
//        SeMajHarm("Se maj harm - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_HARM_SCALE, 0, 10)}, Pitchenga::shuffle, null),
//        SiMaj("Si maj - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_SCALE, 0, 11)}, Pitchenga::shuffle, null),
//        SiMajHarm("Si maj harm - main octaves",
//                new Pitch[][]{transposeScale(DO_MAJ_HARM_SCALE, 0, 11)}, Pitchenga::shuffle, null),
        ChromaticScaleUpDown("Chromatic scale Mi3-La5-Mi3",
                CHROMATIC_SCALE_MI3_LA5_MI3, Pitchenga::ordered, new Integer[]{3, 4, 5}),
        ChromaticScaleUpDownUp("Chromatic scale Mi3-Le5-Mi3 extended",
                CHROMATIC_SCALE_MI3_LA5_MI3_UP_DOWN_UP, Pitchenga::ordered, new Integer[]{3, 4, 5}),
        Step01Do4Do5Fi4La4("Step 1: Do4, Do5, Fi4, La4",
                new Pitch[][]{{Do4, Do5, Fi4, La4}}, Pitchenga::shuffle, new Integer[0]),
        Step02Me4("Step 2: Me4",
                new Pitch[][]{{Do4, Do5, Fi4, La4, Me4}}, Pitchenga::shuffle, new Integer[0]),
        Step03Ra4("Step 3: Ra4",
                new Pitch[][]{{Do4, Do5, Fi4, La4, Me4, Ra4}}, Pitchenga::shuffle, new Integer[0]),
        Step04So4("Step 4: So4",
                new Pitch[][]{{Do4, Do5, Fi4, Fi4, La4, Me4, Ra4, So4, So4}}, Pitchenga::shuffle, new Integer[0]),
        Step05Se4("Step 5: Se4",
                new Pitch[][]{{Do4, Do5, Fi4, La4, La4, Me4, Ra4, So4, Se4, Se4, Se4}}, Pitchenga::shuffle, new Integer[0]),
        Step06Mi4("Step 6: Mi4",
                new Pitch[][]{{Do4, Do5, Fi4, La4, Me4, Me4, Ra4, So4, Se4, Mi4, Mi4, Mi4}}, Pitchenga::shuffle, new Integer[0]),
        Step07Le4("Step 7: Le4",
                new Pitch[][]{{Do4, Do5, Fi4, La4, La4, La4, Me4, Ra4, So4, So4, So4, Se4, Mi4, Mi4, Le4, Le4, Le4, Le4}}, Pitchenga::shuffle, new Integer[0]),
        Step08Re4("Step 8: Re4",
                new Pitch[][]{{Do4, Do5, Fi4, La4, Me4, Me4, Me4, Ra4, Ra4, Ra4, So4, Se4, Mi4, Le4, Le4, Re4, Re4, Re4, Re4, Re4}}, Pitchenga::shuffle, new Integer[0]),
        Step09Si4("Step 9: Si4",
                new Pitch[][]{{Do4, Do5, Do5, Fi4, La4, Me4, Ra4, So4, Se4, Se4, Se4, Mi4, Le4, Re4, Re4, Si4, Si4, Si4, Si4, Si4}}, Pitchenga::shuffle, new Integer[0]),
        Step10Fa4("Step 10: Fa4",
                new Pitch[][]{{Do4, Do5, Fi4, Fi4, Fi4, La4, Me4, Ra4, So4, Se4, Mi4, Mi4, Mi4, Le4, Re4, Si4, Si4, Fa4, Fa4, Fa4, Fa4, Fa4, Fa4}}, Pitchenga::shuffle, new Integer[0]),
        Step11Do3Do4Fi3La3("Step 11: Do3, Do4, Fi3, La3",
                new Pitch[][]{{Do3, Do4, Fi3, La3}}, Pitchenga::shuffle, new Integer[0]),
        Step12Me3("Step 12: Me3",
                new Pitch[][]{{Do3, Do4, Fi3, La3, Me3}}, Pitchenga::shuffle, new Integer[0]),
        Step13Fa3("Step 13: Fa3",
                new Pitch[][]{{Do3, Do4, Fi3, La3, Me3, Fa3}}, Pitchenga::shuffle, new Integer[0]),
        Step14Si3("Step 14: Si3",
                new Pitch[][]{{Do3, Do4, Fi3, La3, Me3, Fa3, Si3, Si3}}, Pitchenga::shuffle, new Integer[0]),
        Step15Re3("Step 15: Re3",
                new Pitch[][]{{Do3, Do4, Fi3, La3, Me3, Me3, Fa3, Si3, Re3, Re3}}, Pitchenga::shuffle, new Integer[0]),
        Step16Le3("Step 16: Le3",
                new Pitch[][]{{Do3, Do4, Fi3, La3, La3, Me3, Fa3, Si3, Re3, Le3, Le3, Le3}}, Pitchenga::shuffle, new Integer[0]),
        Step17Mi3("Step 17: Mi3",
                new Pitch[][]{{Do3, Do4, Fi3, La3, Me3, Me3, Me3, Fa3, Fa3, Fa3, Si3, Re3, Le3, Mi3, Mi3, Mi3, Mi3}}, Pitchenga::shuffle, new Integer[0]),
        Step18Se3("Step 18: Se3",
                new Pitch[][]{{Do3, Do4, Fi3, La3, La3, La3, Me3, Fa3, Si3, Si3, Si3, Re3, Le3, Mi3, Se3, Se3, Se3, Se3, Se3}}, Pitchenga::shuffle, new Integer[0]),
        Step19So3("Step 19: So3",
                new Pitch[][]{{Do3, Do4, Fi3, Fi3, Fi3, La3, Me3, Fa3, Si3, Re3, Le3, Le3, Le3, Mi3, Se3, So3, So3, So3, So3, So3}}, Pitchenga::shuffle, new Integer[0]),
        Step20Ra3("Step 20: Ra3",
                new Pitch[][]{{Do3, Do4, Fi3, La3, Me3, Fa3, Si3, Re3, Le3, Mi3, Se3, So3, Ra3}}, Pitchenga::shuffle, new Integer[0]),
        Step21Octaves3And4Grouped("Step 21a: Octaves 3 and 4 grouped", new Pitch[][]{
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
        }, pitchenga -> pitchenga.shuffleGroupSeries(false), new Integer[0]),
        Step21Octaves3And4("Step 21b: Octaves 3 and 4 shuffled", new Pitch[][]{
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
        }, pitchenga -> pitchenga.shuffleGroupSeries(true), new Integer[0]),
        Step22Octave5("Step 22: Octave 5",
                new Pitch[][]{{Do5, Ra5, Re5, Me5, Mi5, Fa5, Fi5, So5, Le5, La5, Se5, Si5, Do6}}, Pitchenga::shuffle, new Integer[0]),
        Step23Octaves3And4And5("Step 23: Octaves 3, 4, 5", new Pitch[][]{
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do5, Ra5, Re5, Me5, Mi5, Fa5, Fi5, So5, Le5, La5, Se5, Si5, Do6},
        }, pitchenga -> pitchenga.shuffleGroupSeries(true), new Integer[0]),
        Step24Octave2("Step 24: Octave 2",
                new Pitch[][]{{Do2, Ra2, Re2, Me2, Mi2, Fa2, Fi2, So2, Le2, La2, Se2, Si2, Do3}}, Pitchenga::shuffle, new Integer[0]),
        Step25Octaves2And3And4And5("Step 23: Octaves 2, 3, 4, 5", new Pitch[][]{
                {Do2, Ra2, Re2, Me2, Mi2, Fa2, Fi2, So2, Le2, La2, Se2, Si2, Do3},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do5, Ra5, Re5, Me5, Mi5, Fa5, Fi5, So5, Le5, La5, Se5, Si5, Do6},
        }, pitchenga -> pitchenga.shuffleGroupSeries(true), new Integer[0]),
        Step26Octave2("Step 26: Octave 6",
                new Pitch[][]{{Do6, Ra6, Re6, Me6, Mi6, Fa6, Fi6, So6, Le6, La6, Se6, Si6, Do6}}, Pitchenga::shuffle, new Integer[0]),
        Step27Octaves2And3And4And5And6("Step 23: Octaves 2, 3, 4, 5, 6", new Pitch[][]{
                {Do2, Ra2, Re2, Me2, Mi2, Fa2, Fi2, So2, Le2, La2, Se2, Si2, Do3},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do5, Ra5, Re5, Me5, Mi5, Fa5, Fi5, So5, Le5, La5, Se5, Si5, Do6},
                {Do6, Ra6, Re6, Me6, Mi6, Fa6, Fi6, So6, Le6, La6, Se6, Si6, Do6},
        }, pitchenga -> pitchenga.shuffleGroupSeries(true), new Integer[0]),
        Step28Octave2("Step 28: Octave 1",
                new Pitch[][]{{Do1, Ra1, Re1, Me1, Mi1, Fa1, Fi1, So1, Le1, La1, Se1, Si1, Do2}}, Pitchenga::shuffle, new Integer[0]),
        Step29Octaves1And2And3And4And5And6("Step 20: Octaves 1, 2, 3, 4, 5, 6", new Pitch[][]{
                {Do1, Ra1, Re1, Me1, Mi1, Fa1, Fi1, So1, Le1, La1, Se1, Si1, Do2},
                {Do2, Ra2, Re2, Me2, Mi2, Fa2, Fi2, So2, Le2, La2, Se2, Si2, Do3},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do5, Ra5, Re5, Me5, Mi5, Fa5, Fi5, So5, Le5, La5, Se5, Si5, Do6},
                {Do6, Ra6, Re6, Me6, Mi6, Fa6, Fi6, So6, Le6, La6, Se6, Si6, Do6},
        }, pitchenga -> pitchenga.shuffleGroupSeries(true), new Integer[0]),
        Step30Octave7("Step 30: Octave 7",
                new Pitch[][]{{Do7, Ra7, Re7, Me7, Mi7, Fa7, Fi7, So7, Le7, La7, Se7, Si7, Do8}}, Pitchenga::shuffle, new Integer[0]),
        Step31Octaves1And2And3And4And5And6And7("Step 31: Octaves 1, 2, 3, 4, 5, 6, 7", new Pitch[][]{
                {Do1, Ra1, Re1, Me1, Mi1, Fa1, Fi1, So1, Le1, La1, Se1, Si1, Do2},
                {Do2, Ra2, Re2, Me2, Mi2, Fa2, Fi2, So2, Le2, La2, Se2, Si2, Do3},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4, Do5},
                {Do5, Ra5, Re5, Me5, Mi5, Fa5, Fi5, So5, Le5, La5, Se5, Si5, Do6},
                {Do6, Ra6, Re6, Me6, Mi6, Fa6, Fi6, So6, Le6, La6, Se6, Si6, Do6},
                {Do7, Ra7, Re7, Me7, Mi7, Fa7, Fi7, So7, Le7, La7, Se7, Si7, Do8},
        }, pitchenga -> pitchenga.shuffleGroupSeries(true), new Integer[0]),
        ;

        private final String name;
        private final Pitch[][] scale;
        private final Function<Pitchenga, List<Pitch>> riddle;
        private final Integer[] octaves;

        Riddler(String name, Pitch[][] scale, Function<Pitchenga, List<Pitch>> riddle, Integer[] octaves) {
            this.name = name;
            this.scale = scale;
            this.riddle = riddle;
            this.octaves = octaves;
        }

        public String toString() {
            return name;
        }
    }

    public enum Ringer {
        None("Ring nothing", pitch -> new Object[]{thirtyTwo}),
        Tune("Ring mnemonic tune", pitch -> transposeFugue(pitch, pitch.tone.getFugue().tune)),
        Tone("Ring tone", pitch -> new Object[]{pitch, eight, eight}),
        JustDo("Ring Do", pitch -> transposeFugue(pitch, new Object[]{Do.getFugue().pitch, eight, four})),
        JustRa("Ring Ra", pitch -> transposeFugue(pitch, new Object[]{Ra.getFugue().pitch, eight, four})),
        JustRe("Ring Re", pitch -> transposeFugue(pitch, new Object[]{Re.getFugue().pitch, eight, four})),
        JustMe("Ring Me", pitch -> transposeFugue(pitch, new Object[]{Me.getFugue().pitch, eight, four})),
        JustMi("Ring Mi", pitch -> transposeFugue(pitch, new Object[]{Mi.getFugue().pitch, eight, four})),
        JustFa("Ring Fa", pitch -> transposeFugue(pitch, new Object[]{Fa.getFugue().pitch, eight, four})),
        JustFi("Ring Fi", pitch -> transposeFugue(pitch, new Object[]{Fi.getFugue().pitch, eight, four})),
        JustSo("Ring So", pitch -> transposeFugue(pitch, new Object[]{So.getFugue().pitch, eight, four})),
        JustLe("Ring Le", pitch -> transposeFugue(pitch, new Object[]{Le.getFugue().pitch, eight, four})),
        JustLa("Ring La", pitch -> transposeFugue(pitch, new Object[]{La.getFugue().pitch, eight, four})),
        JustSe("Ring Se", pitch -> transposeFugue(pitch, new Object[]{Se.getFugue().pitch, eight, four})),
        JustSi("Ring Si", pitch -> transposeFugue(pitch, new Object[]{Si.getFugue().pitch, eight, four})),
        ToneAndDo("Ring tone and Do", pitch -> transposeFugue(pitch, pitch.tone.getFugue().intervals[Do.ordinal()])),
        ToneAndRa("Ring tone and Ra", pitch -> transposeFugue(pitch, pitch.tone.getFugue().intervals[Ra.ordinal()])),
        ToneAndRe("Ring tone and Re", pitch -> transposeFugue(pitch, pitch.tone.getFugue().intervals[Re.ordinal()])),
        ToneAndMe("Ring tone and Me", pitch -> transposeFugue(pitch, pitch.tone.getFugue().intervals[Me.ordinal()])),
        ToneAndMi("Ring tone and Mi", pitch -> transposeFugue(pitch, pitch.tone.getFugue().intervals[Mi.ordinal()])),
        ToneAndFa("Ring tone and Fa", pitch -> transposeFugue(pitch, pitch.tone.getFugue().intervals[Fa.ordinal()])),
        ToneAndFi("Ring tone and Fi", pitch -> transposeFugue(pitch, pitch.tone.getFugue().intervals[Fi.ordinal()])),
        ToneAndSo("Ring tone and So", pitch -> transposeFugue(pitch, pitch.tone.getFugue().intervals[So.ordinal()])),
        ToneAndLe("Ring tone and Le", pitch -> transposeFugue(pitch, pitch.tone.getFugue().intervals[Le.ordinal()])),
        ToneAndLa("Ring tone and La", pitch -> transposeFugue(pitch, pitch.tone.getFugue().intervals[La.ordinal()])),
        ToneAndSe("Ring tone and Se", pitch -> transposeFugue(pitch, pitch.tone.getFugue().intervals[Se.ordinal()])),
        ToneAndSi("Ring tone and Si", pitch -> transposeFugue(pitch, pitch.tone.getFugue().intervals[Si.ordinal()])),
        ;
        private final String name;
        private final Function<Pitch, Object[]> ring;

        Ringer(String name, Function<Pitch, Object[]> ring) {
            this.name = name;
            this.ring = ring;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Buzzer {
        Tune("Riddle mnemonic tune", Pitchenga::transposeTune),
        Tone("Riddle tone", pitch -> new Object[]{pitch, sixteen}),
        ShortToneAndLongPause("Riddle shorter tone with longer pause (for acoustic instruments)", pitch -> new Object[]{pitch, eight, four, sixteen}), //Otherwise the game plays with itself through the microphone by picking up the "tail". This could probably be improved with a shorter midi decay.
        ToneAndDo("Riddle tone and Do", pitch -> transposeFugue(pitch, new Object[]{pitch.tone.getFugue().pitch, Do.getFugue().pitch, sixteen, four})),
        ;
        private final String name;
        private final Function<Pitch, Object[]> buzz;

        Buzzer(String name, Function<Pitch, Object[]> buzz) {
            this.name = name;
            this.buzz = buzz;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Hinter {
        Always("Hint: immediately", 0),
        Series("Hint: series", 0),
        Delayed100("Hint: after 100 ms", 100),
        Delayed200("Hint: after 200 ms", 200),
        Delayed300("Hint: after 300 ms", 300),
        Delayed400("Hint: after 400 ms", 400),
        Delayed500("Hint: after 500 ms", 500),
        Delayed600("Hint: after 600 ms", 600),
        Delayed700("Hint: after 700 ms", 700),
        Delayed800("Hint: after 800 ms", 800),
        Delayed900("Hint: after 900 ms", 900),
        Delayed1000("Hint: after 1 second", 1000),
        Delayed2000("Hint: after 2 seconds", 2000),
        Delayed3000("Hint: after 3 seconds", 3000),
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

    public enum Pacer {
        Answer("Answer to continue", 0),
        Tempo20("Tempo 20", 20),
        Tempo25("Tempo 25", 25),
        Tempo30("Tempo 30", 30),
        Tempo35("Tempo 35", 35),
        Tempo40("Tempo 40", 40),
        Tempo45("Tempo 45", 45),
        Tempo50("Tempo 50", 50),
        Tempo55("Tempo 55", 55),
        Tempo60("Tempo 60", 60),
        Tempo65("Tempo 65", 65),
        Tempo70("Tempo 70", 70),
        Tempo75("Tempo 75", 75),
        Tempo80("Tempo 80", 80),
        Tempo85("Tempo 85", 85),
        Tempo90("Tempo 90", 90),
        Tempo95("Tempo 95", 95),
        Tempo100("Tempo 100", 100),
        Tempo105("Tempo 105", 105),
        Tempo110("Tempo 110", 110),
        Tempo115("Tempo 115", 115),
        Tempo120("Tempo 120", 120),
        Tempo125("Tempo 125", 125),
        Tempo130("Tempo 130", 130),
        Tempo135("Tempo 135", 135),
        Tempo140("Tempo 140", 140),
        Tempo145("Tempo 145", 145),
        Tempo150("Tempo 150", 150),
        Tempo155("Tempo 155", 155),
        Tempo160("Tempo 160", 160),
        Tempo165("Tempo 165", 165),
        Tempo170("Tempo 170", 170),
        Tempo175("Tempo 175", 175),
        Tempo180("Tempo 180", 180),
        ;

        private final String name;
        private final int bpm;

        Pacer(String name, int bpm) {
            this.name = name;
            this.bpm = bpm;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
