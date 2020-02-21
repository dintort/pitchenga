package com.pitchenga;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static com.pitchenga.Tone.*;

public class Circle extends JComponent {
    private static final Tone[] TONES = new Tone[]{Fi, Fa, Mi, Me, Re, Ra, Do, Si, Se, La, Le, So};
    private final Set<Tone> tones = EnumSet.noneOf(Tone.class);
    private volatile boolean isBlank = true;

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

    public void setTones(Tone... tones) {
        this.tones.clear();
        this.tones.addAll(Arrays.asList(tones));
        isBlank = false;
        repaint();
    }

    public void clear() {
        this.tones.clear();
        isBlank = true;
        setBackground(null);
    }

    public void paint(Graphics graphics) {
        final Color background = getBackground();

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

            graphics.setColor(myTone.color);
            if (tones.contains(myTone) || isBlank) {
                graphics.fillOval(x + offset, y, diameter, diameter);
            } else {
                graphics.drawOval(x + offset, y, diameter, diameter);
            }
        }
        super.paint(graphics);
    }

}
