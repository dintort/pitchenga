package com.pitchenga;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public class Player {

    private final Clip clip;

//    private static final int AUDIO_BITS_PER_SAMPLE = 16;
//    int sampleSizeBytes = AUDIO_BITS_PER_SAMPLE / 8;
//    private final int readBufferSizeInSamples = 1024;
//    private final int bufferSize = sampleSizeBytes * readBufferSizeInSamples;
//    private final byte[] data = new byte[bufferSize];
//    private final double[] amplitudes = new double[readBufferSizeInSamples];

    public Player(URL resource) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(resource);
            this.clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            float volume = 0.4f;
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(20f * (float) Math.log10(volume));
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

//        try {
//            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(resource);
//            int readBytesCount = audioInputStream.read(data, 0, bufferSize);
//            if (readBytesCount == -1) {
//                return;
//            }
//            ByteConverter.littleEndianBytesToDoubles(data, amplitudes);
//        } catch (UnsupportedAudioFileException | IOException e) {
//            e.printStackTrace();
//        }
    }

    public void play() {
        new Thread(() -> {
//            MusicAnalyzer.INSTANCE.consume(amplitudes, data);
            clip.setFramePosition(0);
            clip.start();
        }).start();
    }

    public void stop() {
//        new Thread(() -> {
        clip.stop();
//        }).start();
    }

}