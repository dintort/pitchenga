package com.pitchenga;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public class Player {

    private final Clip clip;

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
    }

    public void play() {
        new Thread(() -> {
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