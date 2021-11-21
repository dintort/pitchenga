package com.harmoneye.viz;

import com.harmoneye.analysis.AnalyzedFrame;
import com.harmoneye.analysis.ExpSmoother;
import com.harmoneye.math.cqt.CqtContext;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.pitchenga.Fugue;
import com.pitchenga.Pitch;
import com.pitchenga.Pitchenga;
import com.pitchenga.Tone;
import org.apache.commons.math3.util.FastMath;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.pitchenga.Pitch.Do1;
import static com.pitchenga.Pitch.Do7;
import static com.pitchenga.Pitchenga.TARSOS;
import static java.awt.Color.BLACK;
import static java.awt.Color.white;

// TODO: rewrite to use vertex buffers instead of immediate mode
public class OpenGlCircularVisualizer implements SwingVisualizer<AnalyzedFrame>, GLEventListener {

    public static final boolean DRAW_SNOWFLAKE = false;
//    public static final boolean DRAW_SNOWFLAKE = true;
    public static final int SLIDER_MIN = Pitchenga.convertPitchToSlider(Do1, Do1.frequency);
    public static final int SLIDER_MAX = Pitchenga.convertPitchToSlider(Do7, Do7.frequency);
    //fixme: Un-hack
    public static volatile OpenGlCircularVisualizer INSTANCE;

    protected static final String[] HALFTONE_NAMES = Arrays.stream(Tone.values()).map(tone -> tone.name).toArray(String[]::new);
    public static final Color DARK = new Color(42, 42, 42);
    private static final Color MORE_DARK = new Color(31, 31, 31);
    private static final Color MORE_DARKER = new Color(21, 21, 21);
    public static volatile Tone toneOverrideTarsos;
    public static int sliderOverrideTarsos;
    public static Color guessColorOverrideTarsos;
    public static Color pitchinessColorOverrideTarsos;
    public static Set<String> scale = new HashSet<>();

    private int pitchStep = 1;

    private double[] binVelocities;

    private final ColorFunction colorFunction = new ColorFunction();
    private final Component component;
    private int binsPerHalftone;
    private int halftoneCount;
    private double stepAngle;

    private TextRenderer renderer;
    public static volatile Tone toneOverride;
    public static volatile String text;

    //recording
    public static final boolean RECORD_VIDEO = false;
    //    public static final boolean RECORD_VIDEO = true;
    public static volatile Pitch currentPitch;
    public static volatile Pitch previousPitch;
    private static volatile PrintStream recordVideoPrintStream;
    private static volatile ZipOutputStream recordVideoZipStream;

    //playback
    public static volatile Fugue playFugue;
    public static volatile Fugue playPreviousFugue;
    private static final AtomicInteger playFrameNumber = new AtomicInteger(-1);
    private static volatile ExpSmoother playSmoother = new ExpSmoother(CqtContext.binsPerHalftone, 0.2);

    public OpenGlCircularVisualizer() {
        INSTANCE = this;
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        GLCanvas canvas = new GLCanvas(caps);
        canvas.addGLEventListener(this);
        component = canvas;
        Animator animator = new Animator();
        animator.add(canvas);
        animator.start();
        // TODO: stop the animator if the computation is stopped
    }

    @Override
    public void update(AnalyzedFrame pcProfile) {
        if (pcProfile == null) {
            return;
        }
        CqtContext cqtContext = pcProfile.getCqtContext();
        binsPerHalftone = cqtContext.getBinsPerHalftone();
        halftoneCount = cqtContext.getHalftonesPerOctave();

        if (!RECORD_VIDEO && Pitchenga.isPlaying()) {
            return;
        }

        double[] octaveBins = pcProfile.getOctaveBins();
        if (octaveBins == null || octaveBins.length == 0) {
            return;
        }
        if (DRAW_SNOWFLAKE) {
            drawSnowflake();
        } else {
            if (binVelocities == null || binVelocities.length != octaveBins.length) {
                playSmoother = new ExpSmoother(octaveBins.length, 0.1);
            }
            binVelocities = octaveBins;
        }
        double segmentCountInv = 1.0 / binVelocities.length;
        stepAngle = 2 * FastMath.PI * segmentCountInv;

    }

    private void drawSnowflake() {
        double[] velocities = new double[108];
        for (int i = 0; i < velocities.length; i++) {
            double velocity;
            int ii = 5 + i;
            int mod = ii % 9;
            if (mod == 0) {
                velocity = 1.2;
            } else if (mod == 1 || mod == 8) {
                velocity = 0.87;
            } else if (mod == 2 || mod == 7) {
                velocity = 0.75;
            } else if (mod == 3 || mod == 6) {
                velocity = 0.65;
            } else {
                velocity = 0.3;
            }
            velocities[i] = velocity;
        }
        binVelocities = velocities;
    }

    private void fadeOut() {
        if (binVelocities == null) {
            return;
        }
        for (int i = 0; i < binVelocities.length; i++) {
            binVelocities[i] = binVelocities[i] * 0.8;
            binVelocities = playSmoother.smooth(binVelocities);
        }
    }

    @Override
    public void setPitchStep(int i) {
        this.pitchStep = i;
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0f, 0f, 0f, 1f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        playVideo();

        int biggestBinNumber = getBiggestBinNumber();
        drawPitchClassFrame(gl);
        drawPitchClassBins(gl, biggestBinNumber);
        Tone tone = getTone(biggestBinNumber);
        if (tone != null) {
            drawTuner(gl);
        }
        drawHalftoneNames(drawable, tone);
        recordVideo();
    }

    private void playVideo() {
        if (RECORD_VIDEO) {
            return;
        }
        if (!Pitchenga.isPlaying()) {
            return;
        }
        if (Pitchenga.showSeriesHint) {
            Fugue previous = playPreviousFugue;
            Fugue current = playFugue;
            if (current != null) {
                if (previous != current) {
                    playPreviousFugue = current;
                    playFrameNumber.set(16);
                }
                int frameNumber = playFrameNumber.incrementAndGet();
                double[][] video = current.pitch.video;
                if (video != null) {
                    if (frameNumber >= video.length) {
                        fadeOut();
                    } else {
                        binVelocities = video[frameNumber];
                        binVelocities = playSmoother.smooth(binVelocities);
                    }
                } else {
                    fadeOut();
                }
            } else {
                fadeOut();
            }
        } else {
            fadeOut();
        }
    }

    @SuppressWarnings("unused")
    private void recordVideo() {
        if (!RECORD_VIDEO) {
            return;
        }
        Pitch previous = previousPitch;
        Pitch current = currentPitch;
        try {
            if (current != null) {
                if (previous != current) {
                    previousPitch = current;
                    if (recordVideoPrintStream != null) {
                        recordVideoPrintStream.flush();
                        recordVideoZipStream.closeEntry();
                        recordVideoPrintStream.close();
                    }
                    File logDir = new File(System.getProperty("user.home") + "/dev/pitchenga/src/main/resources/video/");
                    //noinspection ResultOfMethodCallIgnored
                    logDir.mkdirs();
                    //fixme: rename the wav files the same way
                    File logFile = new File(logDir, current.number + ".zip");
                    if (logFile.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        logFile.delete();
                    }
                    boolean newFile = logFile.createNewFile();
                    if (!newFile) {
                        throw new RuntimeException("Failed creating log file=" + logFile.getCanonicalPath());
                    }
                    recordVideoZipStream = new ZipOutputStream(new FileOutputStream(logFile));
                    recordVideoZipStream.putNextEntry(new ZipEntry(current.number + ".txt"));
                    recordVideoPrintStream = new PrintStream(recordVideoZipStream);
                }
                if (binVelocities != null && binVelocities.length > 0) {
                    for (double binVelocity : binVelocities) {
                        recordVideoPrintStream.print(binVelocity);
                        recordVideoPrintStream.print(" ");
                    }
                    recordVideoPrintStream.println();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Tone getTone(int biggestBinNumber) {
        if (binVelocities == null || binVelocities.length == 0 || biggestBinNumber < 0 || biggestBinNumber >= binVelocities.length) {
            return null;
        }
        double biggestBinVelocity = binVelocities[biggestBinNumber];
        Tone tone = null;
        if (biggestBinVelocity > 0.5) {
            double toneRatio = (double) biggestBinNumber / ((double) binVelocities.length / (double) Tone.values().length);
            int toneNumber = (int) toneRatio;
            if (toneNumber >= 0 && toneNumber <= Tone.values().length + 1) {
                tone = Tone.values()[toneNumber];
            }
        }
        return tone;
    }

    private void drawTuner(GL2 gl) {
        if (!TARSOS || DRAW_SNOWFLAKE) {
            return;
        }
        if (binsPerHalftone == 0) {
            return;
        }
        if (Pitchenga.isPlaying() /* && !Pitchenga.showSeriesHint*/) {
            return;
        }

        int slider = sliderOverrideTarsos;
        slider = slider * 2;
        slider = slider % SLIDER_MAX;
        double halfToneCountInv = 1.0 / SLIDER_MIN;
        double angle = 2 * FastMath.PI * (slider * halfToneCountInv);

        Color color = guessColorOverrideTarsos;
        if (color == null) {
            color = BLACK;
        }

        double startAngle = angle - 0.4 * stepAngle;
        double sinStartAngle = FastMath.sin(startAngle);
        double cosStartAngle = FastMath.cos(startAngle);

        double endAngle = angle + 0.4 * stepAngle;
        double sinEndAngle = FastMath.sin(endAngle);
        double cosEndAngle = FastMath.cos(endAngle);

        double outerRadius = 0.98;
        double outerOuterRadius = outerRadius + 0.02;
        gl.glBegin(GL.GL_TRIANGLES);
        gl.glColor3ub((byte) color.getRed(),
                (byte) color.getGreen(),
                (byte) color.getBlue());

        double innerRadius;
        innerRadius = outerRadius;
        outerRadius = outerRadius - 0.04;

        double centerAngle = angle - 0.000000001 * stepAngle;
        double sinCenterAngle = FastMath.sin(centerAngle);
        double cosCenterAngle = FastMath.cos(centerAngle);
        gl.glVertex2d(outerRadius * sinCenterAngle, outerRadius * cosCenterAngle);
        gl.glVertex2d(innerRadius * sinStartAngle, innerRadius * cosStartAngle);
        gl.glVertex2d(innerRadius * sinEndAngle, innerRadius * cosEndAngle);
        gl.glEnd();
        //fixme: draw a rectangle instead of two triangles
        gl.glBegin(GL.GL_TRIANGLES);
        gl.glVertex2d(outerOuterRadius * sinCenterAngle, outerOuterRadius * cosCenterAngle);
        gl.glVertex2d(innerRadius * sinStartAngle, innerRadius * cosStartAngle);
        gl.glVertex2d(innerRadius * sinEndAngle, innerRadius * cosEndAngle);
        gl.glEnd();
    }

    private int getBiggestBinNumber() {
        double biggestBinVelocity = 0;
        int biggestBinNumber = -1;
        if (binVelocities != null) {
            for (int i = 0; i < binVelocities.length; i++) {
                double velocity = binVelocities[i];
                if (velocity > biggestBinVelocity) {
                    biggestBinVelocity = velocity;
                    biggestBinNumber = i;
                }
            }
        }
        return biggestBinNumber;
    }

    private void drawPitchClassFrame(GL2 gl) {
        Color color;
        double halfToneCountInv = 1.0 / HALFTONE_NAMES.length;
        gl.glLineWidth(2f);
        gl.glBegin(GL.GL_LINES);
        for (int i = 0; i < HALFTONE_NAMES.length; i++) {
            Tone tone = Tone.values()[i];
            color = tone.color;
            if (Pitchenga.isPlaying() && !scale.isEmpty() && !scale.contains(tone.name)) {
                color = MORE_DARKER;
            }
            gl.glColor3ub((byte) color.getRed(),
                    (byte) color.getGreen(),
                    (byte) color.getBlue());

            double angle = 2 * FastMath.PI * (i * halfToneCountInv);
            double radius = 0.70;
            double sin = FastMath.sin(angle);
            double cos = FastMath.cos(angle);
            double x = radius * sin;
            double y = radius * cos;
            gl.glVertex2d(x, y);
            gl.glVertex2d(0, 0);

            radius = 1;
            x = radius * sin;
            y = radius * cos;
            gl.glVertex2d(x, y);
            radius = 0.935;
            x = radius * sin;
            y = radius * cos;
            gl.glVertex2d(x, y);
        }
        gl.glEnd();
    }

    private void drawPitchClassBins(GL2 gl, int biggestBinNumber) {
        if (binVelocities == null || binVelocities.length == 0) {
            return;
        }

        double radius = 0.8;
        double angle = 0.5 * (1 - binsPerHalftone) * stepAngle;
        for (int i = 0; i < binVelocities.length; i++, angle += stepAngle) {
            int pitchClass = i / binsPerHalftone;
            int binInPitchClass = i % binsPerHalftone;
            int movedPitchClass = (pitchClass * pitchStep) % halftoneCount;
            int index = movedPitchClass * binsPerHalftone + binInPitchClass;

            int ii = i - 4;
            double toneRatio = ii / ((double) binVelocities.length / (double) Tone.values().length);

            double binVelocity = binVelocities[i];
            Color color = colorFunction.toColor(binVelocity, toneRatio);
            double velocity = binVelocities[index];
            double myVelocity = velocity;
            if (biggestBinNumber == i) {
                myVelocity = velocity * 1.3;
            } else if (velocity < 0.2) {
                myVelocity = velocity * 0.8;
            } else if (velocity < 0.3) {
                myVelocity = velocity * 0.9;
            } else if (velocity < 0.4) {
                myVelocity = velocity * 0.95;
            } else if (velocity > 0.5) {
                myVelocity = velocity * 1.05;
            }
            if (myVelocity > 1.15) {
                myVelocity = 1.15;
            }


            double startRadius = radius * myVelocity;
            double startAngle = angle - 0.5 * stepAngle;
            double sinStartAngle = FastMath.sin(startAngle);
            double cosStartAngle = FastMath.cos(startAngle);

            double endRadius = radius * myVelocity;
            double endAngle = angle + 0.5 * stepAngle;
            double sinEndAngle = FastMath.sin(endAngle);
            double cosEndAngle = FastMath.cos(endAngle);

            gl.glBegin(GL.GL_TRIANGLES);
            gl.glColor3ub((byte) color.getRed(),
                    (byte) color.getGreen(),
                    (byte) color.getBlue());
            gl.glVertex2d(0, 0);
            gl.glVertex2d(startRadius * sinStartAngle, startRadius * cosStartAngle);
            gl.glVertex2d(endRadius * sinEndAngle, endRadius * cosEndAngle);
            gl.glEnd();

            drawOuterDot(gl, angle, index, color, sinStartAngle, cosStartAngle, sinEndAngle, cosEndAngle);
        }
    }

    private void drawOuterDot(GL2 gl, double angle, int index, Color color, double sinStartAngle, double cosStartAngle, double sinEndAngle, double cosEndAngle) {
        gl.glBegin(GL.GL_TRIANGLES);
        gl.glColor3ub((byte) color.getRed(),
                (byte) color.getGreen(),
                (byte) color.getBlue());

        double outerRadius = 0.98;
        double innerRadius = outerRadius * 0.99 - binVelocities[index] * 0.01;
        double centerAngle = angle - 0.000000001 * stepAngle;
        double sinCenterAngle = FastMath.sin(centerAngle);
        double cosCenterAngle = FastMath.cos(centerAngle);
        gl.glVertex2d(innerRadius * sinCenterAngle, innerRadius * cosCenterAngle);
        gl.glVertex2d(outerRadius * sinStartAngle, outerRadius * cosStartAngle);
        gl.glVertex2d(outerRadius * sinEndAngle, outerRadius * cosEndAngle);
        gl.glEnd();
    }

    private void drawHalftoneNames(GLAutoDrawable drawable, Tone tone) {
        if (binVelocities == null || binVelocities.length == 0) {
            return;
        }

        int width = drawable.getSurfaceWidth();
        int height = drawable.getSurfaceHeight();

        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double size = 0.97 * FastMath.min(width, height);
        double angleStep = 2 * FastMath.PI / HALFTONE_NAMES.length;
        double angle = 0;
        float scaleFactor = (float) (0.0015f * size);

        if (Pitchenga.isPlaying()) {
            tone = toneOverride;
        }
        renderer.beginRendering(width, height);
        for (int i = 0; i < HALFTONE_NAMES.length; i++, angle += angleStep) {
            int index = (i * pitchStep) % HALFTONE_NAMES.length;
            String halftoneName = HALFTONE_NAMES[index];
            if (Pitchenga.isPlaying() && !scale.isEmpty() && !scale.contains(halftoneName)) {
                continue;
            }
            Rectangle2D bounds = renderer.getBounds(halftoneName);
            int offsetX = (int) (scaleFactor * 0.5f * bounds.getWidth());
            int offsetY = (int) (scaleFactor * 0.5f * bounds.getHeight());
//            double radius = 0.43;
            double radius = 0.42;
            int x = (int) (centerX + radius * size * FastMath.sin(angle) - offsetX);
            int y = (int) (centerY + radius * size * FastMath.cos(angle) - offsetY);
            Color color;
            if (DRAW_SNOWFLAKE || (tone != null && tone.name().equalsIgnoreCase(HALFTONE_NAMES[i]))) {
                color = BLACK;
                renderer.setColor(color);
                //fixme: There must be an easier way to draw outline font
                int offset = 3;
                renderer.draw3D(halftoneName, x + offset, y - offset, 0, scaleFactor);
                renderer.draw3D(halftoneName, x - offset, y + offset, 0, scaleFactor);
                renderer.draw3D(halftoneName, x + offset, y + offset, 0, scaleFactor);
                renderer.draw3D(halftoneName, x - offset, y - offset, 0, scaleFactor);
                color = colorFunction.toColor(100, i);
            } else {
                Tone myTone = Pitchenga.TONE_BY_LOWERCASE_NAME.get(halftoneName);
                if (myTone != null && myTone.diatonic) {
                    color = DARK;
                } else {
                    color = MORE_DARK;
                }
            }
            renderer.setColor(color);
            renderer.draw3D(halftoneName, x, y, 0, scaleFactor);
        }
        if (text != null) {
            renderer.setColor(white);
            renderer.draw3D(text, 0, 0, 0, scaleFactor);
        }
        renderer.endRendering();
    }

    private void setConstantAspectRatio(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        double w = drawable.getSurfaceWidth();
        double h = drawable.getSurfaceHeight();
        if (w > h) {
            double aspectRatio = w / h;
            gl.glOrtho(-aspectRatio, aspectRatio, -1, 1, -1, 1);
        } else {
            double aspectRatio = h / w;
            gl.glOrtho(-1, 1, -aspectRatio, aspectRatio, -1, 1);
        }

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // TODO Auto-generated method stub
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        // Synchronize the FPS with the refresh rate of the display (v-sync).
        // Otherwise, we can use the FPSAnimator instead of the plain Animator.
        GL gl = drawable.getGL();
        gl.setSwapInterval(1);

        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_DONT_CARE);
        gl.glLineWidth(0.5f);

        gl.glClearColor(0.25f, 0.25f, 0.25f, 1f);

        renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 50), true,
                true, null, true);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        setConstantAspectRatio(drawable);
    }

    @Override
    public Component getComponent() {
        return component;
    }

}