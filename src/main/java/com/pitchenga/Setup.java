package com.pitchenga;

import be.tarsos.dsp.pitch.PitchProcessor;

import javax.sound.sampled.Mixer;

public class Setup {
    public static final Mixer.Info NO_AUDIO_INPUT = new Mixer.Info("No audio input", "", "", "1") {
    };

    public volatile Mixer.Info defaultAudioInput = NO_AUDIO_INPUT;
    public volatile int defaultPenaltyFactor = 0;
    public volatile Integer[] defaultOctaves = new Integer[]{2, 3, 4, 5, 6};
    public volatile PitchProcessor.PitchEstimationAlgorithm defaultPitchAlgo = PitchProcessor.PitchEstimationAlgorithm.MPM;
    public volatile Pitchenga.Hinter defaultHinter = Pitchenga.Hinter.Always;
    public volatile Pitchenga.Riddler defaultRiddler = Pitchenga.Riddler.DoMaj;
    public volatile Pitchenga.Ringer defaultRinger = Pitchenga.Ringer.Tune;
    public volatile Pitchenga.Buzzer defaultBuzzer = Pitchenga.Buzzer.Tune;
    public volatile Pitchenga.Pacer defaultPacer = Pitchenga.Pacer.Answer;
    public volatile boolean mainFrameVisible = true;

    public static Setup create() {
        String className = System.getProperty("com.pitchenga.setup.class");
        if (className == null) {
            className = Setup.class.getName();
        }
        try {
            Class<?> clazz = Class.forName(className);
            Object setup = clazz.newInstance();
            return (Setup) setup;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
