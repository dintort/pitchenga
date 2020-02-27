package com.pitchenga;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class Multi {

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        System.setProperty("com.pitchenga.debug", "true");
        System.setProperty("com.pitchenga.setup.class", "com.pitchenga.MySetup");

        SwingUtilities.invokeAndWait(() -> {
            Pitchenga secondary = new Pitchenga(false, null);
            System.setProperty("com.pitchenga.default.input", "PANDORA PX5D");
            Pitchenga primary = new Pitchenga(true, secondary);
            Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            primary.setLocation(screenSize.width / 2 - primary.getSize().width / 2, screenSize.height / 2 - primary.getSize().height / 2);
        });
    }

}
