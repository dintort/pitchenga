package com.pitchenga;

import javax.swing.*;
import java.awt.*;

public class Label extends JLabel {
    private final String text;

    public Label(String text) {
        super(text);
        this.text = text;
    }

    public void paintComponent(Graphics g) {
        Graphics2D graphics = (Graphics2D) g;
        graphics.setColor(getForeground());
        graphics.rotate(Math.toRadians(270.0));
        graphics.drawString(text, -15, 15);
    }
}