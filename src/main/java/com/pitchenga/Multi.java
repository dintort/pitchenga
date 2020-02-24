package com.pitchenga;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class Multi {

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        System.setProperty("com.pitchenga.debug", "false");
        System.setProperty("com.pitchenga.setup.class", "com.pitchenga.MySetup");

        SwingUtilities.invokeAndWait(() -> new Pitchenga(false));

        System.setProperty("com.pitchenga.default.input", "pandora-agg");
        SwingUtilities.invokeAndWait(() -> {
            Pitchenga pitchenga = new Pitchenga(true);
            Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            int width = 1024;
            pitchenga.setSize(width, (int) screenSize.getHeight());
            pitchenga.setLocation(screenSize.width / 2 - pitchenga.getSize().width / 2, screenSize.height / 2 - pitchenga.getSize().height / 2);
        });
    }

}
