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
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.pitchenga.Duration.*;
import static com.pitchenga.Pitch.*;

public class Pitchenga extends JFrame implements PitchDetectionHandler {

    private static final PrintStream log;
    private static final boolean debug = "true".equalsIgnoreCase(System.getProperty("com.pitchenga.debug"));
    private static final Pitch[] PITCHES = Pitch.values();
    private static final Tone[] TONES = Tone.values();
    private static final Fugue[] FUGUES = Fugue.values();
    private static final Button[] BUTTONS = Button.values();
    private static final Integer[] ALL_OCTAVES = Arrays.stream(PITCHES).map(pitch -> pitch.octave).filter(octave -> octave >= 0).distinct().toArray(Integer[]::new);
    //fixme: Move to Scale
    public static final Pitch[] CHROMATIC_SCALE = Arrays.stream(FUGUES).map(fugue -> fugue.pitch).toArray(Pitch[]::new);
    //    public static final Pitch[] DO_MAJ_SCALE = Arrays.stream(FUGUES).filter(fugue -> fugue.pitch.tone.diatonic).map(fugue -> fugue.pitch).toArray(Pitch[]::new);
    //    private static final Pitch[] DO_MAJ_HARM_SCALE = new Pitch[]{Do3, Re3, Mi3, Fa3, So3, Le3, Si3, Do4};
    //    private static final Pitch[] SHARPS_SCALE = Arrays.stream(TONES).filter(tone -> !tone.diatonic).map(tone -> tone.getFugue().pitch).toArray(Pitch[]::new);
    private static final Map<Integer, Button> BUTTON_BY_CODE = Arrays.stream(Button.values()).collect(Collectors.toMap(button -> button.keyEventCode, button -> button));
    public static final Font MONOSPACED = new Font(Font.MONOSPACED, Font.BOLD, 20);
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
    private final boolean nativeFullScreenAvailable = isNativeFullScreenAvailable();
    private final AtomicBoolean isInNativeFullScreen = new AtomicBoolean(false);

    private final AtomicReference<Tone> lastGuess = new AtomicReference<>(null);
    private volatile Pitch lastPitch;
    private volatile long lastGuessTimestampMs = System.currentTimeMillis();
    private final List<Pair<Pitch, Double>> guessQueuePitchAndRms = new ArrayList<>();
    private final Queue<Pitch> riddleQueue = new LinkedBlockingQueue<>();
    private final AtomicReference<Pitch> riddle = new AtomicReference<>(null);
    private final AtomicReference<Pitch> prevRiddle = new AtomicReference<>(null);
    private final AtomicReference<Pitch> prevPrevRiddle = new AtomicReference<>(null);
    private volatile long riddleTimestampMs = System.currentTimeMillis();
    private volatile long penaltyRiddleTimestampMs = System.currentTimeMillis();
    private volatile long lastPacerTimestampMs = 0;
    private volatile long lastBuzzTimestampMs;
    private volatile boolean frozen = false;
    private final AtomicInteger seriesCounter = new AtomicInteger(0);
    private final Map<Button, Integer> pressedButtonToMidi = new HashMap<>(); // To ignore OS's key repeating when holding, also used to remember the modified midi code to release
    private volatile boolean fall = false; // Control - octave down
    private volatile boolean lift = false; // Shift - octave up
    private volatile Integer octaveShift = 0;

    private final Display display;
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
    //    private final JToggleButton playButton = new JToggleButton();
    //fixme: Un-hack
    public static final JToggleButton playButton = new JToggleButton();
    private final JToggleButton[] keyButtons = new JToggleButton[Button.values().length];
    private final JLabel frequencyLabel = new JLabel("0000.00");
    private final JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL);
    private final JComponent pitchSliderPanel = new JPanel(new BorderLayout());
    private final JPanel bottomPanel;
    private volatile Dimension previousSize;
    private volatile Point previousLocation;

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
        try {
            Class<?> fullScreenUtilities = Class.forName("com.apple.eawt.FullScreenUtilities");
            fullScreenUtilities.getMethod("setWindowCanFullScreen", Window.class, Boolean.TYPE).invoke(null, this, true);
        } catch (Exception ignore) {
        }

        this.isPrimary = isPrimary;
        this.secondary = secondary;
        if (secondary != null) {
            secondary.setAutoRequestFocus(false);
        }
        this.display = new Display();
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
        asyncExecutor.execute(() -> {
            updateMixer();
            //fixme: Does not work the first time
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            updateMixer();
        });
    }

    private void guess(Pitch guess, boolean exact) {
        try {
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
                pace(pacer.bpm, getBuzzer().buzz.apply(riddle));
            }
            debug(String.format("Guess: [%s] %s [%.2fHz] : %s", riddle, guess, riddle.frequency, success));
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

        if (getPacer() == Pacer.Answer) {
            transcribe(riddle, false);
        }
        fugue(ringInstrument, getRinger().ring.apply(riddle), true);

        this.playQueue.clear();
        playExecutor.execute(() -> guess(null, false));
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
                this.riddle.set(riddle);
                this.riddleTimestampMs = System.currentTimeMillis();
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

    private void pace(int bpm, Object[] fugue) {
        long delay = 60_000 / bpm;
        long prevTimestamp = lastPacerTimestampMs;
        long elapsed = 0;
        if (prevTimestamp != 0) {
            elapsed = System.currentTimeMillis() - prevTimestamp;
        }
        if (elapsed < delay) {
            long diff = delay - elapsed;
            int fugueLength = Arrays.stream(fugue)
                    .mapToInt(item -> item instanceof Integer ? (int) item : 0)
                    .sum();
            diff = diff - fugueLength;
            if (diff > 0) {
                try {
                    Thread.sleep(diff);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        lastPacerTimestampMs = System.currentTimeMillis();
    }

    private boolean checkAnswer(Pitch riddle, Pitch guess, boolean exact) {
        if (riddle == Non) {
            return true;
        }
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
                    double rmsThreshold = 0.1;
                    if (rms > rmsThreshold && !isPlaying()) {
                        transcribe(guess, false);
                    }
                    double maxRms = rms;
                    //fixme: Test it
//                    if (guessQueuePitchAndRms.size() < 2) {
                    if (guessQueuePitchAndRms.size() < 1) {
                        guessQueuePitchAndRms.add(new Pair<>(guess, rms));
                    } else {
                        boolean same = true;
                        for (Pair<Pitch, Double> pitchAndRms : guessQueuePitchAndRms) {
                            if (!guess.equals(pitchAndRms.left)) {
                                same = false;
                                maxRms = Math.max(maxRms, pitchAndRms.right);
                                break;
                            }
                        }
                        if (same) {
                            if (maxRms > rmsThreshold) {
                                if (!isPlaying()) {
                                    transcribe(guess, false);
                                }
                                if (!frozen && getPacer() == Pacer.Answer) {
                                    playExecutor.execute(() -> guess(guess, false));
                                }
                            }
                        } else {
                            guessQueuePitchAndRms.clear();
                            guessQueuePitchAndRms.add(new Pair<>(guess, rms));
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
        boolean playing = isPlaying();
        if (debug && !playing && isPrimary) {
            debug(String.format(" %s | pitch=%.2fHz | probability=%.2f | rms=%.2f | diff=%.2f | pitchyDiff=%.2f | accuracy=%.2f | pitchiness=%.2f | guessRoundedColor=%s | pitchyColor=%s | guessColor=%s | borderColor=%s",
                    guess, frequency, probability, rms, diff, pitchyDiff, accuracy, pitchiness, info(toneColor), info(pitchy.tone.color), info(guessColor), info(pitchinessColor)));
        }
        if (!frozen) {
            SwingUtilities.invokeLater(() -> {
                updateSlider(guess, frequency, isKeyboard);
                frequencyLabel.setText(String.format("%07.2f", frequency));
                boolean answer = getPacer() == Pacer.Answer;
                if (!playing || answer) {
                    updatePianoButtons(guess.tone.getButton());
                    if (!isKeyboard || (!playing && !answer)) {
                        display.setTone(guess.tone, guessColor, pitchinessColor);
                    }
                    display.setFillColor(guessColor);
                    display.update();
                }
            });
        }
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
        Pitch[][][] scale = getRiddler().scale;
        //fixme: Duplicated Do will act weird when multiple octaves
        for (Pitch[][] rowRow : scale) {
            for (Pitch[] row : rowRow) {
                for (Pitch pitch : row) {
                    if (pitch != null && pitch != Non) {
                        JSpinner spinner = toneSpinners[pitch.getFugue().ordinal()];
                        int value = (int) spinner.getValue();
                        spinner.setValue(value + 1);
                    }
                }
            }
        }
        display.setScaleTones(getScaleTones());
        display.update();
    }

    private void scheduleHint(Pitch riddle, int seriesCount) {
        if (riddle == null || riddle == Non) {
            return;
        }
        long riddleTimestampMs = System.currentTimeMillis();
        this.riddleTimestampMs = riddleTimestampMs;
        Hinter hinter = getHinter();
        if (hinter == Hinter.Always) {
            return;
        } else if (hinter == Hinter.Series) {
            SwingUtilities.invokeLater(() -> {
                if (isShowSeriesHint(seriesCount)) {
                    showHint(riddle);
                } else {
                    display.setTones();
                    display.setFillColor(null);
                    display.update();
                }
            });
            return;
        } else if (hinter == Hinter.Never) {
            return;
        } else {
            SwingUtilities.invokeLater(() -> {
                display.setTones();
                if (getPacer() != Pacer.Answer) {
                    display.setFillColor(null);
                }
                display.update();
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
        debug("hint=" + riddle);
        if (riddle == Non) {
            return;
        }
        debug("tone=" + riddle.tone);
        display.setTones(riddle.tone);
        if (getPacer() != Pacer.Answer) {
            display.setFillColor(riddle.tone.color);
        }
        display.update();
        pitchSlider.setValue(convertPitchToSlider(riddle, riddle.frequency));
        frequencyLabel.setText(String.format("%07.2f", riddle.frequency));
    }

    private List<Pitch> deduplicate(Supplier<List<Pitch>> supplier) {
        //fixme: This can be done better, e.g. move the conflicting items forward or something
        List<Pitch> pitches;
        int attempts = 4096;
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
        pitches = new ArrayList<>(pitches);
        Pitch previous = null;
        for (Pitch pitch : pitches) {
            if (pitch.equals(previous)) {
                debug(pitches);
                return true;
            }
            previous = pitch;
        }

        if (setup.repeat > 0) {
            Pitch firstInSeries = null;
            for (int i = 0; i < pitches.size(); i++) {
                Pitch pitch = pitches.get(i);
                int mod = i % setup.series;
                if (mod == 0) {
                    firstInSeries = pitch;
                }
                if (mod == setup.series - 1) {
                    if (pitch.equals(firstInSeries)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<Pitch> shuffle() {
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

    public List<Pitch> shuffleGroupSeries(boolean shuffleMacroGroups, boolean shuffleGroups) {
        Pitch[][][] scales = getRiddler().scale;
        List<Pitch[][]> scalesList = Arrays.asList(scales);
        if (shuffleMacroGroups) {
            Collections.shuffle(scalesList);
        }
        List<Pitch> results = new LinkedList<>();
        for (Pitch[][] scale : scalesList) {
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
            //fixme: Refactor?
            results.addAll(result);
        }
        debug(results + " are the new riddles multiplied");
        return results;
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

    public List<Pitch> ordered() {
        return Arrays.stream(getRiddler().scale)
                .flatMap(Arrays::stream)
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
                    if (pitch != Non) {
                        midiChannel.noteOn(pitch.midi, 127);
                        if (flashColors) {
                            SwingUtilities.invokeLater(() -> {
                                updatePianoButton(pitch.tone.getButton(), true);
                                display.setTone(pitch.tone, pitch.tone.color, pitch.tone.color);
                                display.update();
                            });
                        }
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
                    display.text("\n");
                }
//                String text = "<span style=\"background-color: #FFFF00\">This text is highlighted in yellow.</span>";
//                text(text);
//                display.text("  ");
//                display.text(guess.tone.label);
                display.text(guess.tone.name().toLowerCase());
//                text(guess.getTone().name().toLowerCase(), Color.LIGHT_GRAY, guess.getTone().getColor());
                display.text("\n");
            }
        });
    }

    static {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Pitchenga"); //fixme: Does not work
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.xrender", "f");

        try {
            //fixme: Configurable log path
            File logDir = new File(System.getProperty("user.home") + "/dev/pitchenga/logs/");
            //noinspection ResultOfMethodCallIgnored
            logDir.mkdirs();
            File logFile = new File(logDir, "pitchenga.log");
            if (logFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                logFile.delete();
            }
            boolean newFile = logFile.createNewFile();
            if (!newFile) {
                throw new RuntimeException("Failed creating log file=" + logFile.getCanonicalPath());
            }
            log = new PrintStream(new FileOutputStream(logFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
        SwingUtilities.invokeAndWait(() -> new Pitchenga(true, null));
    }

    private void initGui() {
        initIcon();
        this.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        this.add(mainPanel);
        mainPanel.setBackground(Color.DARK_GRAY);
        mainPanel.setLayout(new BorderLayout());

        JPanel circlePanel = new JPanel();
        mainPanel.add(circlePanel, BorderLayout.CENTER);
        circlePanel.setBackground(Color.DARK_GRAY);
        circlePanel.setLayout(new BorderLayout());
        circlePanel.add(display, BorderLayout.CENTER);

        initPitchSlider();
        mainPanel.add(pitchSliderPanel, BorderLayout.WEST);

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
        sideSize();
        setVisible(setup.mainFrameVisible);

        if (!setup.mainFrameVisible) {
            JFrame frame = new JFrame("Test");
            frame.setVisible(true);
        }
    }

    private void sideSize() {
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
    }

    private void initPitchSlider() {
        pitchSliderPanel.setOpaque(false);

        JPanel frequencyPanel = new JPanel();
        pitchSliderPanel.add(frequencyPanel, BorderLayout.SOUTH);
        frequencyPanel.setOpaque(false);
        frequencyPanel.add(frequencyLabel);
        frequencyLabel.setFont(MONOSPACED);
        frequencyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        frequencyLabel.setForeground(Color.LIGHT_GRAY);

        pitchSliderPanel.add(pitchSlider, BorderLayout.CENTER);
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
                    label.setFont(MONOSPACED);
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
            if (!pressed && event.getKeyCode() == KeyEvent.VK_SPACE) {
                if (!playButton.hasFocus()) {
                    playButton.setSelected(!playButton.isSelected());
                    playButton.requestFocus();
                }
            }
            if (!pressed && event.getKeyCode() == KeyEvent.VK_ENTER) {
                handleEnterButton();
            }
            if (pressed && event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                playButton.setSelected(false);
                playButton.requestFocus();
                boolean set = isInNativeFullScreen.compareAndSet(true, false);
                if (set) {
                    toggleNativeFullScreen();
                }
                GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].setFullScreenWindow(null);
            }
            if (!pressed && event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                display.clearText();
            }
            if (!pressed && event.getKeyCode() == KeyEvent.VK_SLASH) {
                display.text("RST\n");
                updateMixer();
            }
            //fixme: Media keys not recognized :(
//            if (!pressed && event.getKeyCode() == 0) {
//                nextTempo(true);
//            }
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
                nextTempo(event.getKeyCode() != KeyEvent.VK_OPEN_BRACKET);
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

    private void nextTempo(boolean up) {
        int index = pacerCombo.getSelectedIndex();
        index = up ? index + 1 : index - 1;
        if (index >= 0 && index < pacerCombo.getItemCount()) {
            pacerCombo.setSelectedIndex(index);
            display.text(String.valueOf(riddleQueue.size()));
            display.text("\n");
            display.text(String.valueOf(getPacer().bpm));
            display.text("\n");
        }
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
                playExecutor.execute(() -> guess(pitch, true));
            }
        }
        Tone[] tones = pressedButtonToMidi.keySet().stream().map(k -> k.pitch.tone).toArray(Tone[]::new);
        display.setTones(tones);
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
                display.setScaleTones(scaleTones);
                stop();
            });

            colorPanel.add(Box.createVerticalStrut(5));

            JLabel toneLabel = new JLabel(tone.label);
            colorPanel.add(toneLabel);
            toneLabel.setFont(MONOSPACED);
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
            colorLabel.setFont(MONOSPACED);
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
        //fixme
        playButton.setEnabled(isPrimary);
//        playButton.setEnabled(true);
        playButton.addItemListener(event -> handlePlayButton());

        JButton resetButton = new JButton("Reset");
        panel.add(resetButton);
        resetButton.addActionListener(event -> {
            display.text("RST\n");
            updateMixer();
            playButton.requestFocus();
        });

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
        display.clear();
        if (secondary != null) {
            secondary.setVisible(!playing);
        }
        if (playing) {
            resetGame();
            playButton.setText("Stop");
            lastPacerTimestampMs = 0;
            playExecutor.execute(() -> guess(null, false));
//            if (!getPacer().equals(Pacer.Answer)) {
            //fixme: Hide only the control panel, but not the piano
            bottomPanel.setVisible(false);
            pitchSliderPanel.setVisible(false);
//            }
            if (setup.fullScreenWhenPlaying) {
                if (nativeFullScreenAvailable) {
                    boolean current = isInNativeFullScreen.get();
                    if (!current) {
                        boolean set = isInNativeFullScreen.compareAndSet(false, true);
                        if (set) {
                            toggleNativeFullScreen();
                        }
                    }
                } else {
                    this.previousSize = getSize();
                    this.previousLocation = getLocation();
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].setFullScreenWindow(this);
                }
            }
        } else {
            playButton.setText("Play");
            bottomPanel.setVisible(true);
            pitchSliderPanel.setVisible(true);
            if (setup.fullScreenWhenPlaying) {
                if (nativeFullScreenAvailable) {
                    boolean current = isInNativeFullScreen.get();
                    if (current) {
                        boolean set = isInNativeFullScreen.compareAndSet(true, false);
                        if (set) {
                            toggleNativeFullScreen();
                        }
                    }
                } else {
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].setFullScreenWindow(null);
                    if (previousSize != null && previousLocation != null) {
                        setSize(previousSize);
                        setLocation(previousLocation);
                        this.previousSize = getSize();
                        this.previousLocation = getLocation();
                    }
                }
            }
        }
        debug("running=" + playing);
    }

    private void handleEnterButton() {
        if (nativeFullScreenAvailable) {
            boolean current = isInNativeFullScreen.get();
            isInNativeFullScreen.compareAndSet(current, !current);
            bottomPanel.setVisible(!bottomPanel.isVisible());
            toggleNativeFullScreen();
        } else {
            GraphicsDevice screenDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
            if (screenDevice.getFullScreenWindow() == this) {
                bottomPanel.setVisible(true);
                screenDevice.setFullScreenWindow(null);
            } else {
                bottomPanel.setVisible(false);
                screenDevice.setFullScreenWindow(this);
            }
        }
    }

    private void stop() {
        playButton.setSelected(false);
        playButton.requestFocus();
    }

    private Hinter getHinter() {
        Hinter hinter = getRiddler().hinter;
        if (hinter != null) {
            return hinter;
        }
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

    public static Object[] transposeTune(Pitch pitch) {
        return transposeFugue(pitch, pitch.tone.getFugue().tune);
    }

    private static void debug(Object... messages) {
        if (!debug) {
            return;
        }
        if (messages == null || messages.length == 0) {
            String toPrint = Thread.currentThread().getName();
            System.out.println(toPrint);
            log.println(toPrint);
            log.flush();
        }
        if (messages != null) {
            for (Object message : messages) {
                String toPrint = Thread.currentThread().getName() + ": " + message;
                System.out.println(toPrint);
                log.println(toPrint);
                log.flush();
            }
        }
    }

    private void initIcon() {
        Image image = Toolkit.getDefaultToolkit().getImage(Display.class.getResource("/pitchenga.png"));
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

    //fixme: This works wrong when the window was put to full screen manually
    public void toggleNativeFullScreen() {
        try {
            Class<?> app = Class.forName("com.apple.eawt.Application");
            Object getApp = app.getMethod("getApplication").invoke(null);
            getApp.getClass().getMethod("requestToggleFullScreen", Window.class).invoke(getApp, this);
        } catch (Exception ignore) {
        }
    }

    public static boolean isNativeFullScreenAvailable() {
        try {
            Class.forName("com.apple.eawt.FullScreenUtilities");
            Class.forName("com.apple.eawt.Application");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;

    }

}
