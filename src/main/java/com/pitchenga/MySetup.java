package com.pitchenga;

import be.tarsos.dsp.pitch.PitchProcessor;

public class MySetup extends Setup {

    public MySetup() {
        defaultPenaltyFactor = 1;
        defaultPenaltyFactor = 0;

        defaultOctaves = new Integer[]{2, 3, 4, 5, 6};

        defaultPitchAlgo = PitchProcessor.PitchEstimationAlgorithm.MPM;

        defaultHinter = Pitchenga.Hinter.Always;
        defaultHinter = Pitchenga.Hinter.Delayed1000;
        defaultHinter = Pitchenga.Hinter.Delayed500;

        defaultPacer = Pitchenga.Pacer.Answer;
        defaultPacer = Pitchenga.Pacer.Tempo140;
        defaultPacer = Pitchenga.Pacer.Tempo50;

        defaultRiddler = Pitchenga.Riddler.ChromaticWithDoubledDiatonic;
        defaultRiddler = Pitchenga.Riddler.ChromaticOneOctave;
        defaultRiddler = Pitchenga.Riddler.ChromaticWithDoubledSharps;
        defaultRiddler = Pitchenga.Riddler.ChromaticScaleUpDown;
        defaultRiddler = Pitchenga.Riddler.Diatonic;
        defaultRiddler = Pitchenga.Riddler.Chromatic;

        defaultGuessRinger = Pitchenga.GuessRinger.None;
        defaultGuessRinger = Pitchenga.GuessRinger.Tune;
        defaultGuessRinger = Pitchenga.GuessRinger.Tone;
        defaultGuessRinger = Pitchenga.GuessRinger.ToneAndDo;

        defaultRiddleRinger = Pitchenga.RiddleRinger.Tune;
        defaultRiddleRinger = Pitchenga.RiddleRinger.Tone;

        defaultAudioInput = NO_AUDIO_INPUT;
        defaultAudioInput = null;

        mainFrameVisible = true;
//        MAIN_FRAME_VISIBLE = false;
    }

}
