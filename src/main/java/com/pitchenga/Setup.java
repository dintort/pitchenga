package com.pitchenga;

import be.tarsos.dsp.pitch.PitchProcessor;

import javax.sound.sampled.Mixer;
import java.lang.reflect.InvocationTargetException;

public class Setup {
    public static final Mixer.Info NO_AUDIO_INPUT = new Mixer.Info("No audio input", "", "", "1") {
    };

    public volatile Mixer.Info defaultAudioInput = NO_AUDIO_INPUT;
    public volatile int defaultPenaltyFactor = 0;
    public volatile Integer[] defaultOctaves = new Integer[]{2, 3, 4, 5, 6};
    public volatile PitchProcessor.PitchEstimationAlgorithm defaultPitchAlgo = PitchProcessor.PitchEstimationAlgorithm.MPM;
    public volatile Hinter defaultHinter = Hinter.Series;
    public volatile Riddler defaultRiddler = Riddler.Chromatic;
    public volatile Buzzer defaultBuzzer = Buzzer.Tone;
    public volatile Ringer defaultRinger = Ringer.None;
    public volatile Pacer defaultPacer = Pacer.Tempo90;
    public volatile boolean mainFrameVisible = true;
    public volatile int buzzInstrument = Instrument.ACOUSTIC_GRAND_PIANO;
    public volatile int keyboardInstrument = Instrument.ACOUSTIC_GRAND_PIANO;
    public volatile int ringInstrument = Instrument.ELECTRIC_GUITAR_CLEAN;
    public volatile int series = 3;
    public volatile int repeat = 3;
    public volatile boolean fullScreenWhenPlaying = false;

    public static Setup create() {
        String className = System.getProperty("com.pitchenga.setup.class");
        if (className == null) {
            return new Setup();
        }
        try {
            Class<?> clazz = Class.forName(className);
            Object setup = clazz.getDeclaredConstructor().newInstance();
            return (Setup) setup;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
