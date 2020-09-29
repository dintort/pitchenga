package com.pitchenga;

import be.tarsos.dsp.pitch.PitchProcessor;

public class MySetup extends Setup {

    public MySetup() {
        defaultPenaltyFactor = 3;
        defaultPenaltyFactor = 0;

        defaultOctaves = new Integer[]{2, 3, 4, 5, 6};

        defaultPitchAlgo = PitchProcessor.PitchEstimationAlgorithm.MPM;

//        buzzInstrument = Instrument.ELECTRIC_GUITAR_CLEAN;
//        keyboardInstrument = Instrument.ELECTRIC_GUITAR_CLEAN;
//        ringInstrument = Instrument.ACOUSTIC_GRAND_PIANO;

        series = 3;
        repeat = 3;

        defaultHinter = Pitchenga.Hinter.Delayed200;
        defaultHinter = Pitchenga.Hinter.Delayed100;
        defaultHinter = Pitchenga.Hinter.Delayed700;
        defaultHinter = Pitchenga.Hinter.Delayed500;
        defaultHinter = Pitchenga.Hinter.Always;
        defaultHinter = Pitchenga.Hinter.Delayed1000;
        defaultHinter = Pitchenga.Hinter.Delayed3000;
        defaultHinter = Pitchenga.Hinter.Series;

        defaultPacer = Pitchenga.Pacer.Tempo140;
        defaultPacer = Pitchenga.Pacer.Tempo45;
        defaultPacer = Pitchenga.Pacer.Answer;
        defaultPacer = Pitchenga.Pacer.Tempo30;
        defaultPacer = Pitchenga.Pacer.Tempo40;
        defaultPacer = Pitchenga.Pacer.Tempo50;
        defaultPacer = Pitchenga.Pacer.Tempo60;
        defaultPacer = Pitchenga.Pacer.Tempo80;
        defaultPacer = Pitchenga.Pacer.Tempo90;
        defaultPacer = Pitchenga.Pacer.Tempo100;

        defaultRiddler = Pitchenga.Riddler.ChromaticWithDoubledSharps;
        defaultRiddler = Pitchenga.Riddler.ChromaticWithDoubledDiatonic;
        defaultRiddler = Pitchenga.Riddler.ChromaticScaleUpDown;
        defaultRiddler = Pitchenga.Riddler.DoMaj;
        defaultRiddler = Pitchenga.Riddler.Chromatic;
        defaultRiddler = Pitchenga.Riddler.ChromaticWithDoubledSharpsOneOctave;
        defaultRiddler = Pitchenga.Riddler.ChromaticWithDoubledDiatonicOneOctave;
        defaultRiddler = Pitchenga.Riddler.ChromaticOneOctave;
        defaultRiddler = Pitchenga.Riddler.Step13Fa3;
        defaultRiddler = Pitchenga.Riddler.Step14Si3;
        defaultRiddler = Pitchenga.Riddler.Step10Fa4;

        defaultRinger = Pitchenga.Ringer.ToneAndDo;
        defaultRinger = Pitchenga.Ringer.JustDo;
        defaultRinger = Pitchenga.Ringer.ToneAndDo;
        defaultRinger = Pitchenga.Ringer.ToneAndLa;
        defaultRinger = Pitchenga.Ringer.JustRa;
        defaultRinger = Pitchenga.Ringer.ToneAndRa;
        defaultRinger = Pitchenga.Ringer.Tone;
        defaultRinger = Pitchenga.Ringer.None;

        defaultBuzzer = Pitchenga.Buzzer.Tone;

        defaultAudioInput = NO_AUDIO_INPUT;
        defaultAudioInput = null;

        mainFrameVisible = false;
        mainFrameVisible = true;
    }

}
