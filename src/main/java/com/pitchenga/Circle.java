package com.pitchenga;

import javax.swing.*;
import java.awt.*;

import static com.pitchenga.Tone.*;
import static com.pitchenga.Tone.So;

public class Circle extends JPanel {
    private static final Tone[] TONES = new Tone[]{Fi, Fa, Mi, Me, Re, Ra, Do, Si, Se, La, Le, So};
    //    private static final int side = 1105;
    private static final int SIDE = 400;
    private static final int DIAMETER = SIDE / 4;
    private static final int RADIUS = DIAMETER / 2;
    private static final int HALF_SIDE = SIDE / 2;
    private volatile Pitch pitch;
    private volatile Color color;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Circle");
        Image image = Toolkit.getDefaultToolkit().getImage(com.pitchenga.Circle.class.getResource("/pitchenga.png"));
        frame.setIconImage(image);

        Circle circle = new Circle();
        frame.add(circle);
        Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setLocation(screenSize.width / 2 - frame.getSize().width / 2, screenSize.height / 2 - frame.getSize().height / 2);
        frame.setVisible(true);
        frame.pack();
    }

    //fixme: Resizing
    public Circle() {
        super(null);
        int mySide = SIDE + DIAMETER + RADIUS;
        setSize(mySide, mySide);
        setPreferredSize(new Dimension(mySide, mySide));

        this.setBackground(Color.BLACK);
        Font font = new Font("Courier", Font.BOLD, 42);
        for (int i = 0; i < TONES.length; i++) {
            double phi = (i * Math.PI * 2) / TONES.length;
            int x = (int) (HALF_SIDE * Math.sin(phi) + HALF_SIDE - RADIUS / 2) + RADIUS;
            int y = (int) (HALF_SIDE * Math.cos(phi) + HALF_SIDE - RADIUS / 2) + RADIUS;
            Tone tone = TONES[i];

            JPanel circlet = new Circlet(tone);
//            JPanel circle = new JPanel();
            this.add(circlet);
            JLabel label = new JLabel();
            circlet.add(label);
            label.setText(" " + tone.name() + " ");
            label.setFont(font);
            label.setAlignmentY(Component.CENTER_ALIGNMENT);
            label.setBackground(Color.BLACK);
            label.setForeground(Color.WHITE);
            label.setBounds(x, y, RADIUS, RADIUS);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            circlet.setBounds(x, y, DIAMETER, DIAMETER);
        }
    }

    public void update(Pitch pitch, Color color, Color pitchinessColor) {
        this.pitch = pitch;
        this.color = color;
        if (pitchinessColor == null) {
            pitchinessColor = color;
        }
        if (pitchinessColor == null) {
            setBorder(BorderFactory.createLineBorder(Color.BLACK));
        } else {
            //fixme: Probably creating lots of garbage with all these borders
            setBorder(BorderFactory.createLineBorder(pitchinessColor, 5));
        }
        repaint();
    }


    private class Circlet extends JPanel {
        private final Tone tone;

        public Circlet(Tone tone) {
            this.tone = tone;
        }

        public void paint(Graphics graphics) {
            //fixme: Use JLayeredPane or something
//            super.paint(graphics);
            Pitch pitch = Circle.this.pitch;
            if (pitch == null || tone == pitch.tone) {
                Color color = Circle.this.color;
                if (color == null) {
                    color = tone.color;
                }
                graphics.setColor(color);
                graphics.fillOval(0, 0, DIAMETER, DIAMETER);
            }
        }

    }

//    private class MyPanel extends JPanel {
//        public MyPanel() {
//            super(null);
//        }
//
//        public void paint(Graphics g) {
//            super.paint(g);
//            if (!(g instanceof Graphics2D)) {
//                return;
//            }
//            Graphics2D graphics = (Graphics2D) g;
//            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//            Tone[] tones = new Tone[]{Fi, Fa, Mi, Me, Re, Ra, Do, Si, Se, La, Le, So};
//            for (int i = 0; i < tones.length; i++) {
//                double phi = (i * Math.PI * 2) / tones.length;
//                int x = (int) (halfSide * Math.sin(phi) + halfSide - radius / 2) + radius;
//                int y = (int) (halfSide * Math.cos(phi) + halfSide - radius / 2) + radius;
//                Tone tone = tones[i];
//                graphics.setColor(tone.getColor());
//                graphics.fillOval(x, y, diameter, diameter);
//                labels[i].repaint();
//            }
//
//        }
//
//    }

}
