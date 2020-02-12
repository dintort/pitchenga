package com.pitchenga;

import javax.swing.*;
import java.awt.*;

import static com.pitchenga.Tone.*;
import static com.pitchenga.Tone.So;

public class Circle extends JComponent {
    private static final Tone[] TONES = new Tone[]{Fi, Fa, Mi, Me, Re, Ra, Do, Si, Se, La, Le, So};
    private volatile Tone tone;
    private volatile Color color;
    private volatile Tone hint;

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

    public void update(Color toneColor) {
        this.color = toneColor;
        repaint();
    }

    public void update(Tone guess, Color toneColor) {
        this.tone = guess;
        this.color = toneColor;
        repaint();
    }

    public void update(Tone guess, Color toneColor, Tone hint) {
        this.tone = guess;
        this.color = toneColor;
        this.hint = hint;
        repaint();
    }

    public void paint(Graphics graphics) {
        super.paint(graphics);
        final Tone tone = Circle.this.tone;
        final Tone hint = Circle.this.hint;
        final Color color = Circle.this.color;

        Rectangle bounds = graphics.getClipBounds();
        Color fillColor = color != null ? color : Color.GRAY;
        graphics.setColor(fillColor);
        graphics.fillRect(0, 0, bounds.width, bounds.height);

        int fullSide = Math.min(bounds.height, bounds.width);
        int side = fullSide - fullSide / 4;
        int halfSide = side / 2;
        int diameter = (int) (side / 4.7);
        int radius = diameter / 2;
        int halfRadius = radius / 2;

        graphics.setColor(Color.BLACK);
        int gap = halfRadius / 2;
        int outerDiameter = fullSide - halfRadius - gap / 2;
        graphics.fillOval(gap, gap, outerDiameter, outerDiameter);

        for (int i = 0; i < TONES.length; i++) {
            Tone myTone = TONES[i];
            double phi = (i * Math.PI * 2) / TONES.length;
            int x = (int) (halfSide * Math.sin(phi) + halfSide - halfRadius) + radius;
            int y = (int) (halfSide * Math.cos(phi) + halfSide - halfRadius) + radius;

            if (tone == null
                    || myTone == tone
                    || (hint != null && myTone == hint)) {
                graphics.setColor(myTone.color);
                graphics.fillOval(x, y, diameter, diameter);
            }
        }
    }

}
