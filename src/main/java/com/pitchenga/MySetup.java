package com.pitchenga;

import be.tarsos.dsp.pitch.PitchProcessor;

public class MySetup extends Setup {

    public MySetup() {
        defaultPenaltyFactor = 1;
        defaultPenaltyFactor = 0;

        defaultOctaves = new Integer[]{2, 3, 4, 5, 6};

        defaultPitchAlgo = PitchProcessor.PitchEstimationAlgorithm.MPM;

        defaultHinter = Pitchenga.Hinter.Delayed200;
        defaultHinter = Pitchenga.Hinter.Delayed100;
        defaultHinter = Pitchenga.Hinter.Delayed700;
        defaultHinter = Pitchenga.Hinter.Delayed1000;
        defaultHinter = Pitchenga.Hinter.Always;
        defaultHinter = Pitchenga.Hinter.Delayed2000;
        defaultHinter = Pitchenga.Hinter.Delayed500;

        defaultPacer = Pitchenga.Pacer.Tempo140;
        defaultPacer = Pitchenga.Pacer.Tempo30;
        defaultPacer = Pitchenga.Pacer.Tempo45;
        defaultPacer = Pitchenga.Pacer.Tempo60;
        defaultPacer = Pitchenga.Pacer.Tempo40;
        defaultPacer = Pitchenga.Pacer.Answer;
        defaultPacer = Pitchenga.Pacer.Tempo70;

        defaultRiddler = Pitchenga.Riddler.ChromaticWithDoubledDiatonic;
        defaultRiddler = Pitchenga.Riddler.ChromaticWithDoubledSharps;
        defaultRiddler = Pitchenga.Riddler.ChromaticScaleUpDown;
        defaultRiddler = Pitchenga.Riddler.DoMaj;
        defaultRiddler = Pitchenga.Riddler.Chromatic;
        defaultRiddler = Pitchenga.Riddler.ChromaticWithDoubledDiatonicOneOctave;
        defaultRiddler = Pitchenga.Riddler.ChromaticOneOctave;
        defaultRiddler = Pitchenga.Riddler.ChromaticWithDoubledSharpsOneOctave;
        defaultRiddler = Pitchenga.Riddler.Step03;

        defaultGuessRinger = Pitchenga.GuessRinger.ToneAndDo;
        defaultGuessRinger = Pitchenga.GuessRinger.JustDo;
        defaultGuessRinger = Pitchenga.GuessRinger.ToneAndDo;
        defaultGuessRinger = Pitchenga.GuessRinger.ToneAndLa;
        defaultGuessRinger = Pitchenga.GuessRinger.ToneAndRa;
        defaultGuessRinger = Pitchenga.GuessRinger.Tone;
        defaultGuessRinger = Pitchenga.GuessRinger.None;

        defaultRiddleRinger = Pitchenga.RiddleRinger.Tone;

        defaultAudioInput = NO_AUDIO_INPUT;
        defaultAudioInput = null;

        mainFrameVisible = false;
        mainFrameVisible = true;
    }

}
