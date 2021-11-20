package com.harmoneye.app;

import com.harmoneye.analysis.AnalyzedFrame;
import com.harmoneye.analysis.MusicAnalyzer;
import com.harmoneye.viz.OpenGlCircularVisualizer;
import com.harmoneye.viz.OpenGlLinearVisualizer;
import com.harmoneye.viz.SwingVisualizer;
import com.harmoneye.viz.Visualizer;
import org.simplericity.macify.eawt.ApplicationEvent;
import org.simplericity.macify.eawt.ApplicationListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class AbstractHarmonEyeApp {

    private static final int TIME_PERIOD_MILLIS = 16;
//    private static final int TIME_PERIOD_MILLIS = 8;
    private static final String WINDOW_TITLE = "HarmonEye";

    protected static final float AUDIO_SAMPLE_RATE = 44100.0f;
    protected static final int AUDIO_BITS_PER_SAMPLE = 16;

    protected MusicAnalyzer soundAnalyzer;

    private final JFrame frame;

    private final CircleOfFifthsEnabledAction circleOfFifthsEnabledAction;
    private final PauseAction pauseAction;
    private JMenuItem pauseMenuItem;
    private final AccumulationEnabledAction accumulationEnabledAction;

    private final ApplicationListener appListener;

    private final SwingVisualizer<AnalyzedFrame> visualizer;
    private final AtomicBoolean initialized = new AtomicBoolean();

    private Timer updateTimer;

    public AbstractHarmonEyeApp(Visualizer<AnalyzedFrame> visualizer2) {
        visualizer = new com.harmoneye.viz.OpenGlCircularVisualizer();
//        visualizer = new com.harmoneye.viz.OpenGlLinearVisualizer();

        soundAnalyzer = new MusicAnalyzer(visualizer, AUDIO_SAMPLE_RATE, AUDIO_BITS_PER_SAMPLE, visualizer2);

        circleOfFifthsEnabledAction = new CircleOfFifthsEnabledAction("Circle of fifths", null, "", KeyEvent.VK_F);
        pauseAction = new PauseAction("Pause", null, "", KeyEvent.VK_P);
        accumulationEnabledAction = new AccumulationEnabledAction("Accumulate", null, "", KeyEvent.VK_A);

        frame = createFrame();

        appListener = new MyApplicationListener(frame);

        frame.setVisible(true);
    }

    private JFrame createFrame() {
        JFrame frame = new JFrame(WINDOW_TITLE);
        frame.add(visualizer.getComponent());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (visualizer instanceof OpenGlCircularVisualizer) {
            if (OpenGlCircularVisualizer.DRAW_SNOWFLAKE) {
                frame.setSize(1080, 1280);
            } else {
                frame.setSize(700, 710);
            }
        } else if (visualizer instanceof OpenGlLinearVisualizer) {
            frame.setSize(1900, 620);
        }
        frame.setLocationRelativeTo(null);
        frame.setLocation(0, 220);
        frame.setJMenuBar(createMenuBar());

        return frame;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        menuBar.add(createVisualizationMenu());
        menuBar.add(createWindowMenu());
        menuBar.add(createHelpMenu());
        menuBar.setVisible(false);
        return menuBar;
    }

    private JMenu createVisualizationMenu() {
        JMenu menu = new JMenu("Visualization");

        pauseMenuItem = new JMenuItem(pauseAction);
        pauseMenuItem.setAccelerator(KeyStroke.getKeyStroke(' '));
        menu.add(pauseMenuItem);

        JCheckBoxMenuItem circleOfFifthsEnabledMenuItem = new JCheckBoxMenuItem(circleOfFifthsEnabledAction);
        circleOfFifthsEnabledMenuItem.setAccelerator(KeyStroke.getKeyStroke('f'));
        menu.add(circleOfFifthsEnabledMenuItem);

        JCheckBoxMenuItem accumulationEnabledMenuItem = new JCheckBoxMenuItem(accumulationEnabledAction);
        accumulationEnabledMenuItem.setAccelerator(KeyStroke.getKeyStroke('a'));
        menu.add(accumulationEnabledMenuItem);
        return menu;
    }

    private JMenu createWindowMenu() {
        JMenu menu = new JMenu("Window");

        final JCheckBoxMenuItem alwaysOnTopMenuItem = new JCheckBoxMenuItem();
        alwaysOnTopMenuItem.setAction(new AbstractAction("Always on top") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setAlwaysOnTop(alwaysOnTopMenuItem.getState());
            }
        });
        alwaysOnTopMenuItem.setAccelerator(KeyStroke.getKeyStroke('t'));
        menu.add(alwaysOnTopMenuItem);
        return menu;
    }

    private JMenu createHelpMenu() {
        JMenu menu = new JMenu("Help");

        final JCheckBoxMenuItem helpMenuItem = new JCheckBoxMenuItem();
        helpMenuItem.setAction(new AbstractAction("Open website") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                WebHelper.openWebpage(WebHelper.HELP_URL);
            }
        });
        menu.add(helpMenuItem);
        return menu;
    }

    public void start() {
        if (!initialized.get()) {
            return;
        }

        updateTimer = new Timer("update timer");
        TimerTask updateTask = new TimerTask() {
            @Override
            public void run() {
                soundAnalyzer.updateSignal();
            }
        };
        updateTimer.scheduleAtFixedRate(updateTask, 200, TIME_PERIOD_MILLIS);
        pauseMenuItem.setText("Pause");
        frame.setTitle(WINDOW_TITLE);
    }

    public void stop() {
        if (!initialized.get()) {
            return;
        }

        updateTimer.cancel();
        updateTimer = null;
        pauseMenuItem.setText("Play");
        frame.setTitle("= " + WINDOW_TITLE + " =");
    }

    public void init() {
        soundAnalyzer.init();
        initialized.set(true);
    }

    private void toggle() {
        if (updateTimer != null) {
            stop();
        } else {
            start();
        }
    }

    public JFrame getFrame() {
        return frame;
    }

    private class CircleOfFifthsEnabledAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        boolean fifthsEnabled = false;

        public CircleOfFifthsEnabledAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        public void actionPerformed(ActionEvent e) {
            fifthsEnabled = !fifthsEnabled;
            visualizer.setPitchStep(fifthsEnabled ? 7 : 1);
            //visualizer.getPanel().repaint();
        }
    }

    public class PauseAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public PauseAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        public void actionPerformed(ActionEvent e) {
            toggle();
        }
    }

    public class AccumulationEnabledAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public AccumulationEnabledAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        public void actionPerformed(ActionEvent e) {
            soundAnalyzer.toggleAccumulatorEnabled();
        }
    }

    // Must be public!!
    public static class MyApplicationListener implements ApplicationListener {

        private final JFrame frame;

        public MyApplicationListener(JFrame frame) {
            this.frame = frame;
        }

        private void handle(ApplicationEvent event, String message) {
            JOptionPane.showMessageDialog(frame, message);
            event.setHandled(true);
        }

        public void handleAbout(ApplicationEvent event) {
            String message = prepareAboutMessage();
            JOptionPane.showMessageDialog(frame, message, "About HarmonEye", JOptionPane.INFORMATION_MESSAGE);
            event.setHandled(true);
        }

        private String prepareAboutMessage() {
            Package p = getClass().getPackage();
            String version = p.getImplementationVersion();
            if (version == null) {
                version = "";
            }

            return "HarmonEye\n" +
                    "Version: " + version + "\n\n" +
                    "A software that enables you to see what you hear.\n" +
                    "Crafted with love by Bohumír Zamecník since 2012.\n\n" + //Sorry for butchering the accented symbols, but it crashed on windows :(
                    "http://harmoneye.com/";
        }

        public void handleOpenApplication(ApplicationEvent event) {
            // Ok, we know our application started
            // Not much to do about that.
        }

        public void handleOpenFile(ApplicationEvent event) {
            handle(event, "openFileInEditor: " + event.getFilename());
        }

        public void handlePreferences(ApplicationEvent event) {
            // TODO
            handle(event, "For now there are no preferences.");
        }

        public void handlePrintFile(ApplicationEvent event) {
            handle(event, "Sorry, printing not implemented");
        }

        public void handleQuit(ApplicationEvent event) {
            //handle(event, "exitAction");
            System.exit(0);
        }

        public void handleReOpenApplication(ApplicationEvent event) {
            event.setHandled(true);
            frame.setVisible(true);
        }
    }

    public ApplicationListener getApplicationListener() {
        return appListener;
    }

    private static class WebHelper {

        // TODO: the help page!
        public static final String HELP_URL = "http://harmoneye.com/?utm_campaign=help&utm_medium=macosxapp";

        public static void openWebpage(String uri) {
            if (!Desktop.isDesktopSupported()) {
                return;
            }
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                return;
            }
            try {
                desktop.browse(new URI(uri));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}