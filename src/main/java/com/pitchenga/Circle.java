package com.pitchenga;

import javax.swing.*;
import java.awt.*;

import static com.pitchenga.Tone.*;

public class Circle {

    public static void main(String[] args) {

        JFrame frame = new JFrame();
        Image image = Toolkit.getDefaultToolkit().getImage(Circle.class.getResource("/pitchenga.png"));
        frame.setIconImage(image);

        JPanel panel = new MyPanel();
        panel.setBackground(Color.WHITE);
        frame.add(panel);
        panel.setLayout(new BorderLayout());

        frame.pack();
        Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize((int) screenSize.getWidth(), (int) screenSize.getHeight());
        frame.setLocation(screenSize.width / 2 - frame.getSize().width / 2, screenSize.height / 2 - frame.getSize().height / 2);
        frame.setVisible(true);
    }


    private static class MyPanel extends JPanel {

        public void paint(Graphics g) {
            super.paint(g);
            int side = 1105;
            int diameter = side / 5;
            int radius = diameter / 2;
            int halfSide = side / 2;
            Tone[] tones = new Tone[]{fI, Fa, Mi, mE, Re, rA, Do, Ti, tO, La, lU, So};
            for (int i = 0; i < tones.length; i++) {
                double phi = (i * Math.PI * 2) / tones.length;
                int x = (int) (halfSide * Math.sin(phi) + halfSide - radius / 2) + radius;
                int y = (int) (halfSide * Math.cos(phi) + halfSide - radius / 2) + radius;
                g.setColor(tones[i].getColor());
                g.fillOval(x, y, diameter, diameter);
            }
        }

    }

}
