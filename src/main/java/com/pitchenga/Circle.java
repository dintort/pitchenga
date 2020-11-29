package com.pitchenga;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
    private final JTextArea textArea = new JTextArea();
    private final JScrollPane textPane = new JScrollPane(textArea);

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
            label.setFont(Pitchenga.MONOSPACED);
            label.setForeground(Color.WHITE);
            label.setPreferredSize(new Dimension((int) label.getPreferredSize().getWidth(), (int) label.getPreferredSize().getWidth()));
            return label;
        }).toArray(JComponent[]::new);

        this.setLayout(new OverlayLayout(this));

        initTextPane();
        JPanel textLayer = new JPanel(new BorderLayout());
        textLayer.setOpaque(false);
        this.add(textLayer);
        textLayer.add(textPane, BorderLayout.EAST);

        TheCircle circle = new TheCircle();
        this.add(circle);

        initFontScaling();
    }

    private void initTextPane() {
        textPane.setBorder(null);
        textPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        textPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        textPane.setBackground(new Color(0, 0, 0, 0.0f));
        textPane.getViewport().setBackground(new Color(0, 0, 0, 0.0f));

        textArea.setFont(Pitchenga.MONOSPACED);
        textArea.setEditable(false);
        textArea.setForeground(Color.LIGHT_GRAY);
        textArea.setBackground(new Color(0, 0, 0, 0.0f));
        textArea.setBorder(null);
        //        text("<html>");
        for (int i = 0; i < 500; i++) { //There must be a better way
            text("        \n");
        }
    }

    private void initFontScaling() {
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                scaleFont();
            }
        });
        scaleFont();
    }

    private void scaleFont() {
        Dimension size = getSize();
        int min = Math.min(size.height, size.width);
        int fontSize = min / 35;
        Font font = Pitchenga.MONOSPACED.deriveFont((float) fontSize);
        setLabelsFont(font);
        repaint();
    }

    public void setLabelsFont(Font font) {
        for (JComponent label : labels) {
            label.setFont(font);
            Dimension size = new Dimension((int) label.getMinimumSize().getWidth(), (int) label.getMinimumSize().getWidth());
            label.setPreferredSize(size);
        }
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


    public void text(String message) {
//        text(message, null, null);
//    }
//
//    private void text(String message, Color foreground, Color background) {
        if (SwingUtilities.isEventDispatchThread()) {
            //fixme: Discard oldest when becomes too big
//            StyledDocument document = text.getStyledDocument();
//            SimpleAttributeSet attributes = new SimpleAttributeSet();
//            if (foreground != null) {
//                StyleConstants.setForeground(attributes, foreground);
//            }
//            if (background != null) {
//                StyleConstants.setBackground(attributes, background);
//            }
//            StyleConstants.setBold(attributes, false);
//            try {
//                document.insertString(document.getLength(), message, attributes);
//            } catch (BadLocationException e) {
//                e.printStackTrace();
//            }
            textArea.append(message);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }

    private class TheCircle extends JPanel {

        public TheCircle() {
            for (JComponent label : labels) {
                this.add(label);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Tone tone = Circle.this.tone;
            Color toneColor = Circle.this.toneColor;
            Color pitchyColor = Circle.this.pitchinessColor;
            Color fillColor = Circle.this.fillColor;
            if (fillColor == null) {
                fillColor = Color.DARK_GRAY;
            }

            Dimension bounds = this.getSize();
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
            int gap = (int) (halfRadius / 1.5);
            int outerDiameter = fullSide - gap * 2;

            graphics.setColor(Color.BLACK);
            graphics.fillOval(gap + offset, gap, outerDiameter, outerDiameter);

            for (int i = 0; i < TONES.length; i++) {
                Tone myTone = TONES[i];
                double phi = (i * Math.PI * 2) / TONES.length;
                int x = (int) Math.round(gap / 4.0 + halfSide * Math.sin(phi) + halfSide - halfRadius + radius);
                int y = (int) Math.round(gap / 4.0 + halfSide * Math.cos(phi) + halfSide - halfRadius + radius);

                if (myTone == tone && toneColor != null && pitchyColor != null) {
                    triangle(graphics, offset, gap, fullSide, halfSide, radius, halfRadius, i, toneColor, true);
                    graphics.setColor(pitchyColor);
                    graphics.fillOval(x + offset, y, diameter, diameter);
                    graphics.setColor(toneColor);
                    graphics.fillOval(x + offset + gap, y + gap, diameter - halfRadius, diameter - halfRadius);
                } else {
                    graphics.setColor(myTone.color);
                    graphics.fillOval(x + offset, y, diameter, diameter);

                    if (tones.contains(myTone)) {
                        triangle(graphics, offset, gap, fullSide, halfSide, radius, halfRadius, i, myTone.color, true);
                    } else {
                        triangle(graphics, offset, gap, fullSide, halfSide, radius, halfRadius, i, myTone.color, false);
                        int thickness;
                        if (scaleTones.contains(myTone)) {
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
                labelGraphics.setColor(Color.BLACK);
                labelGraphics.fillOval(0, 0, width, height);
                label.paint(labelGraphics);
            }
        }

        private void triangle(Graphics graphics, int offset, int gap, int fullSide, int halfSide, int radius, int halfRadius, int i, Color color, boolean fill) {
            double phi2 = ((i - 0.2) * Math.PI * 2) / TONES.length;
            int x2 = (int) Math.round(gap / 4.0 + halfSide * Math.sin(phi2) + halfSide + halfRadius + radius);
            int y2 = (int) Math.round(gap / 4.0 + halfSide * Math.cos(phi2) + halfSide + halfRadius + radius);
            double phi3 = ((i + 0.2) * Math.PI * 2) / TONES.length;
            int x3 = (int) Math.round(gap / 4.0 + halfSide * Math.sin(phi3) + halfSide + halfRadius + radius);
            int y3 = (int) Math.round(gap / 4.0 + halfSide * Math.cos(phi3) + halfSide + halfRadius + radius);
            graphics.setColor(color);
            int[] xPoints = {x2 + offset, x3 + offset, fullSide / 2 + offset};
            int[] yPoints = {y2, y3, fullSide / 2};
            if (fill) {
                graphics.fillPolygon(xPoints, yPoints, 3);
            } else {
                graphics.drawPolygon(xPoints, yPoints, 3);
            }
        }

        @Override
        protected void paintBorder(Graphics g) {
        }

        @Override
        protected void paintChildren(Graphics g) {
        }
    }
}
