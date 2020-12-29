package com.pitchenga;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.List;

import static com.pitchenga.Pitch.*;
import static com.pitchenga.Tone.*;

public class Display extends JPanel {
    private static final Tone[] TONES = new Tone[]{Fi, Fa, Mi, Me, Re, Ra, Do, Si, Se, La, Le, So};
    private final Set<Tone> tones = EnumSet.noneOf(Tone.class);
    private final Set<Tone> scaleTones = EnumSet.noneOf(Tone.class);
    private JComponent[] labels;
    private final JTextArea textArea = new JTextArea();
    private final JScrollPane textPane = new JScrollPane(textArea);

    private volatile Tone tone;
    private volatile Color toneColor;
    private volatile Color pitchinessColor;
    private volatile Color fillColor;
    private volatile Updatable displayPanel;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Display");
        Image image = Toolkit.getDefaultToolkit().getImage(Display.class.getResource("/pitchenga.png"));
        frame.setIconImage(image);

        Display display = new Display();
        int mySide = 700;
        display.setSize(mySide, mySide);
        display.setPreferredSize(new Dimension(mySide, mySide));

        frame.add(display);
        Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setLocation(screenSize.width / 2 - frame.getSize().width / 2, screenSize.height / 2 - frame.getSize().height / 2);
        frame.setVisible(true);
        frame.pack();
    }

    public Display() {
        super();

        //fixme
        labels = Arrays.stream(TONES).map(tone -> {
            JLabel label = new JLabel(tone.label);
            label.setFont(Pitchenga.MONOSPACED);
            label.setForeground(Color.WHITE);
            label.setPreferredSize(new Dimension((int) label.getPreferredSize().getWidth(), (int) label.getPreferredSize().getWidth()));
            return label;
        }).toArray(JComponent[]::new);

//        this.setLayout(new OverlayLayout(this));
        this.setLayout(new BorderLayout());

        initTextPane();
//        JPanel textLayer = new JPanel(new BorderLayout());
//        textLayer.setOpaque(false);
//        this.add(textLayer);
//        textLayer.add(textPane, BorderLayout.EAST);
        this.add(textPane, BorderLayout.EAST);

        //fixme: Make them switchable
//        Circle circle = new Circle();
//        this.add(circle);
//        this.displayPanel = circle;
        Frets frets = new Frets();
        this.add(frets);
        this.displayPanel = frets;
        initFontScaling();
    }

    private void initTextPane() {
        textPane.setBorder(null);
        textPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        textPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
//        textPane.setBackground(new Color(0, 0, 0, 0.0f));
        textPane.setBackground(Color.BLACK);
//        textPane.setBackground(null);
//        textPane.setOpaque(false);
//        textPane.getViewport().setBackground(new Color(0, 0, 0, 0.0f));
        textPane.getViewport().setBackground(Color.BLACK);
//        textPane.getViewport().setBackground(null);
//        textPane.getViewport().setOpaque(false);

        //fixme: Copy text to clipboard does not work on mac
//        textArea.setVisible(false);
        textArea.setFont(Pitchenga.MONOSPACED);
        textArea.setEditable(false);
        textArea.setForeground(Color.LIGHT_GRAY);
        textArea.setBackground(Color.BLACK);
//        textArea.setBackground(new Color(0, 0, 0, 0.0f));
//        textArea.setBackground(null);
//        textArea.setOpaque(false);
        textArea.setBorder(null);
        //        text("<html>");
        clearText();
    }

    public void clearText() {
        textArea.setText("");
        for (int i = 0; i < 500; i++) { //There must be a better way
//            text("        \n");
            text("   \n");
        }
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

    private void initFontScaling() {
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                scaleFontAndRepaint();
            }
        });
        scaleFontAndRepaint();
    }

    private void scaleFontAndRepaint() {
        Dimension size = getSize();
        int min = Math.min(size.height, size.width);
        int fontSize = min / 35;
        Font font = Pitchenga.MONOSPACED.deriveFont((float) fontSize);
        setLabelsFont(font);
        update();
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
    }

    public void setTones(Tone... tones) {
        this.tones.clear();
        this.tones.addAll(Arrays.asList(tones));
        this.tone = null;
        this.pitchinessColor = null;
    }

    public void setScaleTones(Collection<Tone> scaleTones) {
        this.scaleTones.clear();
        this.scaleTones.addAll(scaleTones);
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public void clear() {
        setTones();
        setFillColor(null);
    }

    public void update() {
//        displayPanel.update();
        repaint();
    }

    public static Pitch[][] FRETS = {
            {Mi5, Fa5, Fi5, So5, Le5, La5},
            {Si4, Do5, Ra5, Re5, Me5, Mi5},
            {So4, Le4, La4, Se4, Si4, Do5},
            {Re4, Me4, Mi4, Fa4, Fi4, So4},
            {La3, Se3, Si3, Do4, Ra4, Re4},
            {Mi3, Fa3, Fi3, So3, Le3, La3},
    };

    //fixme: Prettify
    public static Pitch[][] BASE_FRETS = {
            {null, null, null, null, null, null},
            {null, null, null, null, null, null},
            {null, Le4, La4, Se4, Si4, Do5},
            {null, Me4, Mi4, Fa4, Fi4, So4},
            {null, null, null, Do4, Ra4, Re4},
            {null, null, null, null, null, null},
    };

    private class Frets extends JPanel implements Updatable {

        private final JPanel[][] panels = new JPanel[FRETS.length][];

        public Frets() {
            super(new GridLayout(FRETS[0].length, FRETS.length, 4, 4));
            this.setBackground(Color.BLACK);

            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int borderThickness = getBorderThickness(panels[0][0]);
                    setLayout(new GridLayout(FRETS[0].length, FRETS.length, borderThickness, borderThickness));
                }
            });

            List<JComponent> labelsList = new LinkedList<>();
            for (int i = 0; i < FRETS.length; i++) {
                Pitch[] row = FRETS[i];
                panels[i] = new JPanel[row.length];
                for (int j = 0; j < row.length; j++) {
                    Pitch pitch = row[j];
                    Tone tone = pitch.tone;
                    JPanel panel = new JPanel();
                    panels[i][j] = panel;
                    this.add(panel);
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//                    panel.setLayout(new BorderLayout());
                    panel.setBackground(Color.BLACK);
                    panel.setBorder(BorderFactory.createLineBorder(tone.color, getBorderThickness(panel)));

//                    JPanel labelPanel = new JPanel();
//                    labelPanel.setBackground(null);
//                    panel.add(labelPanel, BorderLayout.CENTER);
//                    labelPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN));

                    panel.add(Box.createVerticalGlue());
                    JLabel toneLabel = new JLabel(tone.label);
                    labelsList.add(toneLabel);
                    panel.add(toneLabel, BorderLayout.CENTER);
//                    labelPanel.add(toneLabel, BorderLayout.CENTER);
                    toneLabel.setFont(Pitchenga.MONOSPACED);
                    toneLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    toneLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
                    toneLabel.setForeground(Color.WHITE);
                    toneLabel.setBackground(Color.BLACK);
                    toneLabel.setOpaque(true);
                    toneLabel.setHorizontalTextPosition(SwingConstants.CENTER);
                    panel.add(Box.createVerticalGlue());


                    //fixme: Handle clicks
//                colorPanel.addMouseListener(new MouseAdapter() {
//                    @Override
//                    public void mousePressed(MouseEvent e) {
//                        handleButton(theButton, true);
//                    }
//                    @Override
//                    public void mouseReleased(MouseEvent e) {
//                        handleButton(theButton, false);
//                    }
//                });
                }
                labels = labelsList.toArray(new JComponent[0]);
            }
        }

        private int getBorderThickness(JPanel panel) {
            int thickness = Math.min(panel.getWidth(), panel.getHeight()) / 8;
            if (thickness == 0) {
                thickness = 1;
            }
            return thickness;
        }

        @Override
        public void update() {
            this.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Tone tone = Display.this.tone;
            Color toneColor = Display.this.toneColor;
            Color pitchyColor = Display.this.pitchinessColor;
            Color fillColor = Display.this.fillColor;
            if (fillColor == null) {
                fillColor = Color.DARK_GRAY;
            }
            setBackground(fillColor);

            if (panels == null || panels[0] == null) {
                return;
            }

            int borderThickness = getBorderThickness(panels[0][0]);
            for (int i = 0; i < FRETS.length; i++) {
                Pitch[] row = FRETS[i];
                Pitch[] baseRow = BASE_FRETS[i];
                for (int j = 0; j < row.length; j++) {
                    Pitch pitch = row[j];
                    Pitch isBase = baseRow[j];
                    Tone myTone = pitch.tone;
                    JPanel panel = panels[i][j];
                    if (isBase != null || !Pitchenga.playButton.isSelected()) {
                        if (myTone == tone && toneColor != null && pitchyColor != null) {
                            panel.setBorder(BorderFactory.createLineBorder(pitchyColor, borderThickness * 2));
                            panel.setBackground(toneColor);
                        } else {
                            if (tones.contains(myTone)) {
                                panel.setBackground(myTone.color);
                            } else {
                                panel.setBorder(BorderFactory.createLineBorder(myTone.color, borderThickness));
                                panel.setBackground(Color.BLACK);
                            }
                        }
//                        panel.setOpaque(true);
                    } else {
//                        panel.setOpaque(false);
                        panel.setBackground(Color.BLACK);
                        panel.setBorder(null);
                    }
                }
            }
            super.paintComponent(g);
        }
    }

    private class Circle extends JPanel implements Updatable {

        public Circle() {
            for (JComponent label : labels) {
                this.add(label);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Tone tone = Display.this.tone;
            Color toneColor = Display.this.toneColor;
            Color pitchyColor = Display.this.pitchinessColor;
            Color fillColor = Display.this.fillColor;
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


        @Override
        public void update() {
            repaint();
        }
    }

    private static interface Updatable {
        public void update();
    }

}
