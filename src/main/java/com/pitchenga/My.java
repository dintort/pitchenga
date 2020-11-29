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
//        repeat = 3;
        repeat = 2;
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

        defaultRiddler = Riddler.ChromaticOneOctave;
        defaultRiddler = Riddler.Step18Se3;
        defaultRiddler = Riddler.Step17Mii3;
        defaultRiddler = Riddler.Step19So3;
        defaultRiddler = Riddler.Step21Octave3;
        defaultRiddler = Riddler.Step25Si2;
        defaultRiddler = Riddler.Step26Re2;
        defaultRiddler = Riddler.Step27Le2;

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
