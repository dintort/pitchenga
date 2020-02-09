package com.pitchenga;

import javax.swing.*;
import java.awt.*;

import static com.pitchenga.Tone.*;

public class Circle extends JFrame {
    private static final Tone[] tones = new Tone[]{Fi, Fa, Mi, Me, Re, Ra, Do, Si, Se, La, Le, So};
    private static final int side = 1105;
    private static final int diameter = side / 5;
    private static final int radius = diameter / 2;
    private static final int halfSide = side / 2;

    public static void main(String[] args) {
        new com.pitchenga.Circle();
    }

    public Circle() {
        Image image = Toolkit.getDefaultToolkit().getImage(com.pitchenga.Circle.class.getResource("/pitchenga.png"));
        this.setIconImage(image);

        JPanel panel = new JPanel(null);
        this.add(panel);
//        panel.setBackground(Color.WHITE);
//        panel.setLayout(null);

//        frame.pack();
//        Font font = new Font("Courier", Font.BOLD, 100);
        Font font = new Font("Courier", Font.BOLD, 42);
        for (int i = 0; i < tones.length; i++) {
            double phi = (i * Math.PI * 2) / tones.length;
            int x = (int) (halfSide * Math.sin(phi) + halfSide - radius / 2) + radius;
            int y = (int) (halfSide * Math.cos(phi) + halfSide - radius / 2) + radius;
            Tone tone = tones[i];

            JPanel circle = new Circlet(tone);
//            JPanel circle = new JPanel();
            panel.add(circle);
            JLabel label = new JLabel();
            circle.add(label);
            label.setText(" " + tone.name() + " ");
            label.setFont(font);
            label.setAlignmentY(Component.CENTER_ALIGNMENT);
            label.setBackground(Color.BLACK);
            label.setForeground(Color.WHITE);
            label.setBounds(x, y, radius, radius);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            circle.setBounds(x, y, diameter, diameter);
        }

        Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        this.setSize((int) screenSize.getWidth() / 2, (int) screenSize.getHeight());
        this.setLocation(screenSize.width / 2 - this.getSize().width / 2, screenSize.height / 2 - this.getSize().height / 2);
        this.setVisible(true);
    }


    private static class Circlet extends JPanel {
        private final Tone tone;

        public Circlet(Tone tone) {
            this.tone = tone;
        }

        public void paint(Graphics graphics) {
            //fixme: Use JLayeredPane or something
//            super.paint(graphics);
            graphics.setColor(tone.color);
            graphics.fillOval(0, 0, diameter, diameter);
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
//                graphics.filLeval(x, y, diameter, diameter);
//                labels[i].repaint();
//            }
//
//        }
//
//    }

}
