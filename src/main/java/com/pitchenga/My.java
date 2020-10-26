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

        defaultHinter = Pitchenga.Hinter.Delayed200;
        defaultHinter = Pitchenga.Hinter.Delayed100;
        defaultHinter = Pitchenga.Hinter.Delayed700;
        defaultHinter = Pitchenga.Hinter.Always;
        defaultHinter = Pitchenga.Hinter.Delayed3000;
        defaultHinter = Pitchenga.Hinter.Delayed500;
        defaultHinter = Pitchenga.Hinter.Delayed1000;
        defaultHinter = Pitchenga.Hinter.Series;
        //fixme: Delay proportionally to the tempo

        defaultPacer = Pitchenga.Pacer.Tempo140;
        defaultPacer = Pitchenga.Pacer.Tempo45;
        defaultPacer = Pitchenga.Pacer.Answer;
        defaultPacer = Pitchenga.Pacer.Tempo30;
        defaultPacer = Pitchenga.Pacer.Tempo40;
        defaultPacer = Pitchenga.Pacer.Tempo60;
        defaultPacer = Pitchenga.Pacer.Tempo80;
        defaultPacer = Pitchenga.Pacer.Tempo90;
        defaultPacer = Pitchenga.Pacer.Tempo50;
        defaultPacer = Pitchenga.Pacer.Tempo100;

        defaultRiddler = Pitchenga.Riddler.Step22Octave5;
        defaultRiddler = Pitchenga.Riddler.Step21Octaves3And4;
        defaultRiddler = Pitchenga.Riddler.Step20Ra3;

        defaultRinger = Pitchenga.Ringer.ToneAndDo;
        defaultRinger = Pitchenga.Ringer.JustDo;
        defaultRinger = Pitchenga.Ringer.ToneAndDo;
        defaultRinger = Pitchenga.Ringer.ToneAndLa;
        defaultRinger = Pitchenga.Ringer.Tone;
        defaultRinger = Pitchenga.Ringer.None;

        defaultBuzzer = Pitchenga.Buzzer.Tone;

        defaultAudioInput = NO_AUDIO_INPUT;
        defaultAudioInput = null;

        mainFrameVisible = false;
        mainFrameVisible = true;
    }

}
