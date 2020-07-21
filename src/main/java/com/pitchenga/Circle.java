package com.pitchenga;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import static com.pitchenga.Tone.*;

public class Circle extends JPanel {
    private static final Tone[] TONES = new Tone[]{Fi, Fa, Mi, Me, Re, Ra, Do, Si, Se, La, Le, So};
    private final Set<Tone> tones = EnumSet.noneOf(Tone.class);
    private final Set<Tone> scaleTones = EnumSet.noneOf(Tone.class);
    private final JComponent[] labels;

    private volatile Tone tone;
    private volatile Color toneColor;
    private volatile Color pitchinessColor;
    private volatile Color fillColor;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Circle");
        Image image = Toolkit.getDefaultToolkit().getImage(com.pitchenga.Circle.class.getResource("/pitchenga.png"));
        frame.setIconImage(image);

        Circle circle = new Circle();
        int mySide = 700;
        circle.setSize(mySide, mySide);
        circle.setPreferredSize(new Dimension(mySide, mySide));

        frame.add(circle);
        Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setLocation(screenSize.width / 2 - frame.getSize().width / 2, screenSize.height / 2 - frame.getSize().height / 2);
        frame.setVisible(true);
        frame.pack();
    }

    public Circle() {
        super();
        labels = Arrays.stream(TONES).map(tone -> {
            JLabel label = new JLabel(tone.label);
            label.setFont(Pitchenga.COURIER);
            label.setOpaque(true);
            label.setBackground(Color.BLACK);
            label.setForeground(Color.WHITE);
            this.add(label);
            return label;
        }).toArray(JComponent[]::new);
    }

    public void setTone(Tone tone, Color toneColor, Color pitchinessColor) {
        this.tones.clear();
        this.tones.add(tone);
        this.tone = tone;
        this.toneColor = toneColor;
        this.pitchinessColor = pitchinessColor;
        repaint();
    }

    public void setTones(Tone... tones) {
        this.tones.clear();
        this.tones.addAll(Arrays.asList(tones));
        this.tone = null;
        this.pitchinessColor = null;
        repaint();
    }

    public void setScaleTones(Collection<Tone> scaleTones) {
        this.scaleTones.clear();
        this.scaleTones.addAll(scaleTones);
        repaint();
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
        repaint();
    }

    public void clear() {
        setTones();
        setFillColor(null);
    }

    @Override
    public void paint(Graphics graphics) {
        Tone tone = this.tone;
        Color toneColor = this.toneColor;
        Color pitchyColor = this.pitchinessColor;
        Color fillColor = this.fillColor;
        if (fillColor == null) {
            fillColor = Color.DARK_GRAY;
        }

        Rectangle bounds = graphics.getClipBounds();
        graphics.setColor(fillColor);
        graphics.fillRect(0, 0, bounds.width, bounds.height);

        int offset;
        int fullSide;
        if (bounds.height < bounds.width) {
            fullSide = bounds.height;
            offset = (bounds.width - bounds.height) / 2;
        } else {
            fullSide = bounds.width;
            offset = 0;
        }
        int side = fullSide - fullSide / 4;
        int halfSide = side / 2;
        int diameter = (int) (side / 4.7);
        int radius = diameter / 2;
        int halfRadius = radius / 2;
        int gap = halfRadius / 2;
        int outerDiameter = fullSide - halfRadius - gap / 2;

        graphics.setColor(Color.BLACK);
        graphics.fillOval(gap + offset, gap, outerDiameter, outerDiameter);

        for (int i = 0; i < TONES.length; i++) {
            Tone myTone = TONES[i];
            double phi = (i * Math.PI * 2) / TONES.length;
            int x = (int) Math.round(halfSide * Math.sin(phi) + halfSide - halfRadius + radius);
            int y = (int) Math.round(halfSide * Math.cos(phi) + halfSide - halfRadius + radius);

            if (myTone == tone && toneColor != null && pitchyColor != null) {
                triangle(graphics, offset, fullSide, halfSide, radius, halfRadius, i, toneColor, true);
                graphics.setColor(pitchyColor);
                graphics.fillOval(x + offset, y, diameter, diameter);
                graphics.setColor(toneColor);
                graphics.fillOval(x + offset + gap, y + gap, diameter - halfRadius, diameter - halfRadius);
            } else {
                graphics.setColor(myTone.color);
                graphics.fillOval(x + offset, y, diameter, diameter);

                if (tones.contains(myTone)) {
                    triangle(graphics, offset, fullSide, halfSide, radius, halfRadius, i, myTone.color, true);
                } else {
                    int thickness;
                    if (scaleTones.contains(myTone)) {
                        triangle(graphics, offset, fullSide, halfSide, radius, halfRadius, i, myTone.color, false);
                        thickness = 1 + gap / 2;
                    } else {
                        thickness = 1 + gap / 12;
                    }
                    graphics.setColor(Color.BLACK);
                    graphics.fillOval(offset + x + thickness, y + thickness, diameter - thickness * 2, diameter - thickness * 2);
                }
            }

            JComponent label = labels[i];
            int width = label.getWidth();
            int height = label.getHeight();
            Graphics labelGraphics = graphics.create(offset + x + radius - width / 2, y + radius - height / 2, width, height);
            label.paint(labelGraphics);
        }
    }

    private void triangle(Graphics graphics, int offset, int fullSide, int halfSide, int radius, int halfRadius, int i, Color color, boolean fill) {
        double phi2 = ((i - 0.2) * Math.PI * 2) / TONES.length;
        int x2 = (int) Math.round(halfSide * Math.sin(phi2) + halfSide + halfRadius + radius);
        int y2 = (int) Math.round(halfSide * Math.cos(phi2) + halfSide + halfRadius + radius);
        double phi3 = ((i + 0.2) * Math.PI * 2) / TONES.length;
        int x3 = (int) Math.round(halfSide * Math.sin(phi3) + halfSide + halfRadius + radius);
        int y3 = (int) Math.round(halfSide * Math.cos(phi3) + halfSide + halfRadius + radius);
        graphics.setColor(color);
        int[] xPoints = {x2 + offset, x3 + offset, fullSide / 2 + offset};
        int[] yPoints = {y2, y3, fullSide / 2};
        if (fill) {
            graphics.fillPolygon(xPoints, yPoints, 3);
        } else {
            graphics.drawPolygon(xPoints, yPoints, 3);
        }
    }

}
