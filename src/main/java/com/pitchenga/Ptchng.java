package com.pitchenga;

import be.tarsos.dsp.pitch.PitchProcessor;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

@SuppressWarnings("unused")
public class Ptchng extends Setup {


    public Ptchng() {
        defaultPenaltyFactor = 3;
        defaultPenaltyFactor = 0;

        defaultOctaves = new Integer[]{2, 3, 4, 5, 6};

//        defaultPitchAlgo = PitchProcessor.PitchEstimationAlgorithm.MPM;
//        defaultPitchAlgo = PitchProcessor.PitchEstimationAlgorithm.DYNAMIC_WAVELET;
        defaultPitchAlgo = PitchProcessor.PitchEstimationAlgorithm.YIN;

//        riddleInstrument = Instrument.ELECTRIC_BASS_FINGER;
//        riddleInstrument = Instrument.TENOR_SAX;
//        keyboardInstrument = Instrument.ELECTRIC_GUITAR_CLEAN;
//        ringInstrument = Instrument.ACOUSTIC_GRAND_PIANO;

        series = 3;
        repeat = 3;

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
        defaultPacer = Pacer.Tempo120;
        defaultPacer = Pacer.Tempo100;
        defaultPacer = Pacer.Tempo110;
        defaultPacer = Pacer.Tempo75;

        defaultRiddler = Riddler.Step51BassOctave2;

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
        System.setProperty("com.pitchenga.setup.class", "com.pitchenga.Ptchng");

        SwingUtilities.invokeAndWait(() -> {
//            Pitchenga secondary = new Pitchenga(false, null);
//            System.setProperty("com.pitchenga.default.input", "NO_AUDIO_INPUT");
            System.setProperty("com.pitchenga.default.input", "Sonic Port VX");
//            System.setProperty("com.pitchenga.default.input", "STUDIO-CAPTURE");
//            System.setProperty("com.pitchenga.default.input", "HD Pro Webcam C920");
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