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
        defaultHinter = Pitchenga.Hinter.Delayed700;

        defaultPacer = Pitchenga.Pacer.Tempo140;
        defaultPacer = Pitchenga.Pacer.Tempo55;
        defaultPacer = Pitchenga.Pacer.Answer;
        defaultPacer = Pitchenga.Pacer.Tempo40;

        defaultRiddler = Pitchenga.Riddler.ChromaticWithDoubledDiatonic;
        defaultRiddler = Pitchenga.Riddler.ChromaticOneOctave;
        defaultRiddler = Pitchenga.Riddler.ChromaticWithDoubledSharps;
        defaultRiddler = Pitchenga.Riddler.ChromaticScaleUpDown;
        defaultRiddler = Pitchenga.Riddler.DoMaj;
        defaultRiddler = Pitchenga.Riddler.Chromatic;

        defaultGuessRinger = Pitchenga.GuessRinger.JustRa;
        defaultGuessRinger = Pitchenga.GuessRinger.JustRe;
        defaultGuessRinger = Pitchenga.GuessRinger.JustMe;
        defaultGuessRinger = Pitchenga.GuessRinger.JustMi;
        defaultGuessRinger = Pitchenga.GuessRinger.JustFa;
        defaultGuessRinger = Pitchenga.GuessRinger.JustFi;
        defaultGuessRinger = Pitchenga.GuessRinger.JustSo;
        defaultGuessRinger = Pitchenga.GuessRinger.JustLe;
        defaultGuessRinger = Pitchenga.GuessRinger.JustSe;
        defaultGuessRinger = Pitchenga.GuessRinger.JustSi;
        defaultGuessRinger = Pitchenga.GuessRinger.JustLa;
        defaultGuessRinger = Pitchenga.GuessRinger.ToneAndDo;
        defaultGuessRinger = Pitchenga.GuessRinger.JustDo;
        defaultGuessRinger = Pitchenga.GuessRinger.Tone;

        defaultRiddleRinger = Pitchenga.RiddleRinger.Tune;
        defaultRiddleRinger = Pitchenga.RiddleRinger.Tone;

        defaultAudioInput = NO_AUDIO_INPUT;
        defaultAudioInput = null;

        mainFrameVisible = true;
//        MAIN_FRAME_VISIBLE = false;
    }

}
