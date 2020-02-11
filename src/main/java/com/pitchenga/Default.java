package com.pitchenga;

import be.tarsos.dsp.pitch.PitchProcessor;

import javax.sound.sampled.Mixer;

public class Default {
    public static final int DEFAULT_PENALTY_FACTOR = 3;
    public static final Integer[] DEFAULT_OCTAVES = new Integer[]{2, 3, 4, 5, 6};
    public static final PitchProcessor.PitchEstimationAlgorithm DEFAULT_PITCH_ALGO = PitchProcessor.PitchEstimationAlgorithm.MPM; //fixme: Neither of them work for bass guitar :(
    public static final Pitchenga.Pacer DEFAULT_PACER = Pitchenga.Pacer.Answer;
//    public static final Pitchenga.Pacer DEFAULT_PACER = Pitchenga.Pacer.Tempo140;
    public static final Pitchenga.Hinter DEFAULT_HINTER = Pitchenga.Hinter.OneSec;

    public static final Pitchenga.Riddler DEFAULT_RIDDLER = Pitchenga.Riddler.Diatonic;
//    public static final Pitchenga.Riddler DEFAULT_RIDDLER = Pitchenga.Riddler.Chromatic;

    public static final Pitchenga.GuessRinger DEFAULT_GUESS_RINGER = Pitchenga.GuessRinger.Tune;
//    public static final Pitchenga.GuessRinger DEFAULT_GUESS_RINGER = Pitchenga.GuessRinger.Tone;
//    public static final Pitchenga.GuessRinger DEFAULT_GUESS_RINGER = Pitchenga.GuessRinger.None;

    public static final Pitchenga.RiddleRinger DEFAULT_RIDDLE_RINGER = Pitchenga.RiddleRinger.Tune;
//    public static final Pitchenga.RiddleRinger DEFAULT_RIDDLE_RINGER = Pitchenga.RiddleRinger.Tone;

    public static final Mixer.Info NO_AUDIO_INPUT = new Mixer.Info("No audio input", "", "", "1") {
    };
    public static final Mixer.Info DEFAULT_AUDIO_INPUT = NO_AUDIO_INPUT; //fixme: Audio input disabled by default, because of the bug where the game plays with itself through the microphone which is confusing.
//    public static final Mixer.Info DEFAULT_AUDIO_INPUT = null;

    public static final boolean CIRCLE_FRAME_VISIBLE = true;
//    public static final boolean CIRCLE_FRAME_VISIBLE = false;

    public static final boolean MAIN_FRAME_VISIBLE = true;
//    public static final boolean MAIN_FRAME_VISIBLE = false;
}
