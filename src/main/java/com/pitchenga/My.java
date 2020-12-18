package com.pitchenga;

import be.tarsos.dsp.pitch.PitchProcessor;

@SuppressWarnings("unused")
public class My extends Setup {

    public My() {
        defaultPenaltyFactor = 3;
        defaultPenaltyFactor = 0;

        defaultOctaves = new Integer[]{2, 3, 4, 5, 6};

        defaultPitchAlgo = PitchProcessor.PitchEstimationAlgorithm.MPM;

//        buzzInstrument = Instrument.ELECTRIC_GUITAR_CLEAN;
//        keyboardInstrument = Instrument.ELECTRIC_GUITAR_CLEAN;
//        ringInstrument = Instrument.ACOUSTIC_GRAND_PIANO;

        series = 3;
        repeat = 3;
//        series = 1;
//        repeat = 1;

        defaultHinter = Hinter.Delayed200;
        defaultHinter = Hinter.Delayed100;
        defaultHinter = Hinter.Delayed700;
        defaultHinter = Hinter.Always;
        defaultHinter = Hinter.Delayed3000;
        defaultHinter = Hinter.Delayed500;
        defaultHinter = Hinter.Delayed1000;
        defaultHinter = Hinter.Series;
        //fixme: Delay proportionally to the tempo

        defaultPacer = Pacer.Tempo140;
        defaultPacer = Pacer.Tempo45;
        defaultPacer = Pacer.Answer;
        defaultPacer = Pacer.Tempo30;
        defaultPacer = Pacer.Tempo40;
        defaultPacer = Pacer.Tempo60;
        defaultPacer = Pacer.Tempo80;
        defaultPacer = Pacer.Tempo90;
        defaultPacer = Pacer.Tempo50;
        defaultPacer = Pacer.Tempo100;

        defaultRiddler = Riddler.Step33Octaves2And3RaRe;
        defaultRiddler = Riddler.Warmup;
        defaultRiddler = Riddler.Step34Octaves2And3MeMi;
        defaultRiddler = Riddler.Step35Octaves2And3FaFi;
        defaultRiddler = Riddler.Step36Octaves2And3SoLe;
        defaultRiddler = Riddler.Step37Octaves2And3LaSe;
        defaultRiddler = Riddler.Step38Octaves2And3SiDo;
        defaultRiddler = Riddler.Step39Octaves2And3SeSiDo;
        defaultRiddler = Riddler.Step40Octaves2And3SoLeLao;
        defaultRiddler = Riddler.Step43Octave4;
        defaultRiddler = Riddler.Step44Octaves2And3And4SeSiDo;
        defaultRiddler = Riddler.Step45Octaves2And3And4SoLeLa;

        defaultRinger = Ringer.ToneAndDo;
        defaultRinger = Ringer.JustDo;
        defaultRinger = Ringer.ToneAndDo;
        defaultRinger = Ringer.ToneAndLa;
        defaultRinger = Ringer.Tone;
        defaultRinger = Ringer.None;

        defaultBuzzer = Buzzer.Tone;

        defaultAudioInput = NO_AUDIO_INPUT;
        defaultAudioInput = null;

        mainFrameVisible = false;
        mainFrameVisible = true;
    }

}
