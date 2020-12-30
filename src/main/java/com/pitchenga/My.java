package com.pitchenga;

import be.tarsos.dsp.pitch.PitchProcessor;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

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
        defaultHinter = Hinter.Delayed3000;
        defaultHinter = Hinter.Delayed500;
        defaultHinter = Hinter.Delayed1000;
        defaultHinter = Hinter.Always;
        defaultHinter = Hinter.Delayed1000;
        defaultHinter = Hinter.Never;
        defaultHinter = Hinter.Series;
        //fixme: Delay hinter proportionally to the tempo

        defaultPacer = Pacer.Answer;
        defaultPacer = Pacer.Tempo140;
        defaultPacer = Pacer.Tempo45;
        defaultPacer = Pacer.Tempo30;
        defaultPacer = Pacer.Tempo40;
        defaultPacer = Pacer.Tempo60;
        defaultPacer = Pacer.Tempo80;
        defaultPacer = Pacer.Tempo90;
        defaultPacer = Pacer.Tempo50;
        defaultPacer = Pacer.Tempo100;

        defaultRiddler = Riddler.Step44Fa4;
        defaultRiddler = Riddler.ChromaticScaleUpDownUp;
        defaultRiddler = Riddler.ChromaticScaleDownUpDown;
        defaultRiddler = Riddler.Step33Octaves2And3Shuffled;
        defaultRiddler = Riddler.Step32Octaves2And3;

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
        fullScreenWhenPlaying = true;
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        System.setProperty("com.pitchenga.debug", "true");
        System.setProperty("com.pitchenga.setup.class", "com.pitchenga.My");

        SwingUtilities.invokeAndWait(() -> {
//            Pitchenga secondary = new Pitchenga(false, null);
//            System.setProperty("com.pitchenga.default.input", "NO_AUDIO_INPUT");
            System.setProperty("com.pitchenga.default.input", "Sonic Port VX");
            //fixme: Multiple JFrames collapse into tabs on mac
            //fixme: Pitchy circles are broken
//            Pitchenga primary = new Pitchenga(true, secondary);
            Pitchenga primary = new Pitchenga(true, null);

//            secondary.requestFocus();
//            Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
//            int side = Math.min(screenSize.height, screenSize.width);
//            primary.setSize(side, side);
//            primary.setLocation(screenSize.width / 2 - primary.getSize().width / 2, screenSize.height / 2 - primary.getSize().height / 2);
//            primary.setLocation(0, 0);
//            primary.setSize(screenSize.width, screenSize.height);
//            primary.setExtendedState(primary.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        });
    }
}
