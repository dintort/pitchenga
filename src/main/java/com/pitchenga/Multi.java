package com.pitchenga;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class Multi {

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.xrender","f");
        System.setProperty("com.pitchenga.debug", "true");
        System.setProperty("com.pitchenga.setup.class", "com.pitchenga.My");

        SwingUtilities.invokeAndWait(() -> {
//            Pitchenga secondary = new Pitchenga(false, null);
            System.setProperty("com.pitchenga.default.input", "NO_AUDIO_INPUT");
//            Pitchenga primary = new Pitchenga(true, secondary);
            Pitchenga primary = new Pitchenga(true, null);
//            secondary.requestFocus();
            Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            //noinspection SuspiciousNameCombination
            primary.setSize(screenSize.height, screenSize.height);
            primary.setLocation(screenSize.width / 2 - primary.getSize().width / 2, screenSize.height / 2 - primary.getSize().height / 2);
//            primary.setLocation(0, 0);
//            primary.setSize(screenSize.width, screenSize.height);
//            primary.setExtendedState(primary.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        });
    }

}
