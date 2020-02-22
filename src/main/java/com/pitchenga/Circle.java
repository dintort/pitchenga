package com.pitchenga;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import static com.pitchenga.Tone.*;

public class Circle extends JComponent {
    private static final Tone[] TONES = new Tone[]{Fi, Fa, Mi, Me, Re, Ra, Do, Si, Se, La, Le, So};
    private final Set<Tone> tones = EnumSet.noneOf(Tone.class);
    private final Set<Tone> scaleTones = EnumSet.noneOf(Tone.class);
    private volatile Tone tone;
    private volatile Color toneColor;
    private volatile Color pitchinessColor;

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

    public void clear() {
        setTones();
        setBackground(null);
    }

    public void paint(Graphics graphics) {
        Color background = getBackground();
        Tone tone = this.tone;
        Color toneColor = this.toneColor;
        Color pitchyColor = this.pitchinessColor;

        Rectangle bounds = graphics.getClipBounds();
        Color fillColor = background != null ? background : Color.GRAY;
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

        graphics.setColor(Color.BLACK);
        int gap = halfRadius / 2;
        int outerDiameter = fullSide - halfRadius - gap / 2;
        graphics.fillOval(gap + offset, gap, outerDiameter, outerDiameter);

        for (int i = 0; i < TONES.length; i++) {
            Tone myTone = TONES[i];
            double phi = (i * Math.PI * 2) / TONES.length;
            int x = (int) (halfSide * Math.sin(phi) + halfSide - halfRadius) + radius;
            int y = (int) (halfSide * Math.cos(phi) + halfSide - halfRadius) + radius;

            if (myTone == tone && toneColor != null && pitchyColor != null) {
                graphics.setColor(pitchyColor);
                graphics.fillOval(x + offset, y, diameter, diameter);
                graphics.setColor(toneColor);
                graphics.fillOval(x + offset + gap, y + gap, diameter - halfRadius, diameter - halfRadius);
            } else {
                graphics.setColor(myTone.color);
                graphics.fillOval(x + offset, y, diameter, diameter);
                int thickness = 0;
                if (!tones.contains(myTone)) {
                    thickness = gap;
                }
                if (!scaleTones.contains(myTone)) {
                    thickness = 1 + gap / 7;
                }
                if (thickness > 0) {
                    graphics.setColor(Color.BLACK);
                    graphics.fillOval(x + offset + thickness, y + thickness, diameter - thickness * 2, diameter - thickness * 2);
                }
            }
        }
        super.paint(graphics);
    }

}
