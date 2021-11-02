package com.harmoneye.viz;

import com.harmoneye.analysis.AnalyzedFrame;
import com.harmoneye.math.cqt.CqtContext;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.pitchenga.Pitchenga;
import com.pitchenga.Tone;
import org.apache.commons.math3.util.FastMath;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static com.pitchenga.Pitch.Do1;
import static com.pitchenga.Pitch.Do7;
import static java.awt.Color.BLACK;
import static java.awt.Color.white;

// TODO: rewrite to use vertex buffers instead of immediate mode

public class OpenGlCircularVisualizer implements
        SwingVisualizer<AnalyzedFrame>, GLEventListener {

    public static final boolean muteWhenPlaying = "true".equals(System.getProperty("muteWhenPlaying"));
    //        public static final boolean muteWhenPlaying = true;
    public static final int SLIDER_MIN = Pitchenga.convertPitchToSlider(Do1, Do1.frequency);
    public static final int SLIDER_MAX = Pitchenga.convertPitchToSlider(Do7, Do7.frequency);
    //fixme: Un-hack
    public static volatile OpenGlCircularVisualizer INSTANCE;
    private static final float DEFAULT_LINE_WIDTH = 1f;
    private static final float WAIT_SCROBBLER_LINE_WIDTH = 1.5f;

    protected static final String[] HALFTONE_NAMES = {"do", "ra", "re", "me",
            "mi", "fa", "fi", "so", "le", "la", "se", "si"};
    public static final Color MORE_DARK = new Color(42, 42, 42);
    private static final Color EVEN_MORE_DARK = new Color(31, 31, 31);
    public static volatile Tone toneOverrideTarsos;
    public static volatile double freqOverrideTarsos;
    public static int sliderOverrideTarsos;
    public static Color guessColorOverrideTarsos;
    public static Color pitchinessColorOverrideTarsos;

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
        if (muteWhenPlaying()) {
            return;
        }
        if (!Pitchenga.showSeriesHint && Pitchenga.isPlaying() && binVelocities != null) {
            double[] octaveBins = pcProfile.getOctaveBins();
            if (binVelocities == octaveBins) { //Shared array
                binVelocities = new double[octaveBins.length];
                System.arraycopy(octaveBins, 0, binVelocities, 0, octaveBins.length);
            }
            for (int i = 0; i < binVelocities.length; i++) {
                binVelocities[i] = binVelocities[i] * 0.75; //Smooth fadeout
            }
            return;
        }

        CqtContext cqtContext = pcProfile.getCqtContext();
        binsPerHalftone = cqtContext.getBinsPerHalftone();
        halftoneCount = cqtContext.getHalftonesPerOctave();
        binVelocities = pcProfile.getOctaveBins();
        if (binVelocities == null || binVelocities.length == 0) {
            return;
        }
        for (int i = 0; i < binVelocities.length; i++) {
            double binVelocity = binVelocities[i];
            if (binVelocity < 0.2) {
                binVelocity = binVelocity * 0.8;
            } else if (binVelocity < 0.3) {
                binVelocity = binVelocity * 0.9;
            } else if (binVelocity < 0.4) {
                binVelocity = binVelocity * 0.95;
            } else if (binVelocity > 0.5) {
                binVelocity = binVelocity * 1.02;
            }
//            binVelocity = binVelocity * 1.2;
            if (binVelocity > 1.1) {
                binVelocity = 1.1;
            }
            binVelocities[i] = binVelocity;
        }
        double segmentCountInv = 1.0 / binVelocities.length;
        stepAngle = 2 * FastMath.PI * segmentCountInv;
    }

    @Override
    public void setPitchStep(int i) {
        this.pitchStep = i;
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        int biggestBinNumber = getBiggestBinNumber();

        gl.glClearColor(0f, 0f, 0f, 1f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        drawPitchClassBins(gl, biggestBinNumber);
        drawPitchClassFrame(gl);
        Tone tone = getTone(biggestBinNumber);
        //fixme: Temporary using the tuner for the second DSP
//        if (tone != null) {
            drawTuner(gl);
//        }
        drawHalftoneNames(drawable, tone);
    }

    private Tone getTone(int biggestBinNumber) {
        if (binVelocities == null || binVelocities.length == 0 || biggestBinNumber < 0 || biggestBinNumber >= binVelocities.length ) {
            return null;
        }
        double biggestBinVelocity = binVelocities[biggestBinNumber];
        Tone tone = null;
        if (biggestBinVelocity > 0.3) {
            double toneRatio = (double) biggestBinNumber / ((double) binVelocities.length / (double) Tone.values().length);
            int toneNumber = (int) toneRatio;
            if (toneNumber >= 0 && toneNumber <= Tone.values().length + 1) {
                tone = Tone.values()[toneNumber];
            }
        }
        return tone;
    }

    private void drawTuner(GL2 gl) {
        if (binsPerHalftone == 0) {
            return;
        }
        if (Pitchenga.isPlaying()) {
            return;
        }

        int slider = sliderOverrideTarsos;
        slider = slider * 2;
        slider = slider % SLIDER_MAX;
        double halfToneCountInv = 1.0 / SLIDER_MIN;
        double angle = 2 * FastMath.PI * (slider * halfToneCountInv);
        int i = slider;

        Color color = guessColorOverrideTarsos;
        if (color == null) {
            color = BLACK;
        }

        double startAngle = angle - 0.5 * stepAngle;
        double sinStartAngle = FastMath.sin(startAngle);
        double cosStartAngle = FastMath.cos(startAngle);

        double endAngle = angle + 0.5 * stepAngle;
        double sinEndAngle = FastMath.sin(endAngle);
        double cosEndAngle = FastMath.cos(endAngle);

        double outerRadius = 0.95;
        gl.glBegin(GL.GL_TRIANGLES);
        gl.glColor3ub((byte) color.getRed(),
                (byte) color.getGreen(),
                (byte) color.getBlue());

        double centerRadius;
        centerRadius = outerRadius;
        outerRadius = outerRadius - 0.05;

        double centerAngle = angle - 0.000000001 * stepAngle;
        double sinCenterAngle = FastMath.sin(centerAngle);
        double cosCenterAngle = FastMath.cos(centerAngle);
        gl.glVertex2d(centerRadius * sinCenterAngle, centerRadius * cosCenterAngle);
        gl.glVertex2d(outerRadius * sinStartAngle, outerRadius * cosStartAngle);
        gl.glVertex2d(outerRadius * sinEndAngle, outerRadius * cosEndAngle);
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

    private boolean muteWhenPlaying() {
        return muteWhenPlaying && Pitchenga.isPlaying();
    }

    private void drawPitchClassFrame(GL2 gl) {

        Color color;
        double halfToneCountInv = 1.0 / HALFTONE_NAMES.length;
        gl.glBegin(GL.GL_LINES);
        for (int i = 0; i < HALFTONE_NAMES.length; i++) {
            Tone tone = Tone.values()[i];
            color = tone.color;
            gl.glColor3ub((byte) color.getRed(),
                    (byte) color.getGreen(),
                    (byte) color.getBlue());

            double angle = 2 * FastMath.PI * (i * halfToneCountInv);
            double sin = FastMath.sin(angle);
            double cos = FastMath.cos(angle);
            double lineRadius = 0.70;
            double x = lineRadius * sin;
            double y = lineRadius * cos;
            gl.glVertex2d(x, y);
            gl.glVertex2d(0, 0);

            lineRadius = 1;
            x = lineRadius * sin;
            y = lineRadius * cos;
            gl.glVertex2d(x, y);

            lineRadius = 0.95;
            x = lineRadius * sin;
            y = lineRadius * cos;
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

            double toneRatio = i / ((double) binVelocities.length / (double) Tone.values().length);
            //fixme: hack
            toneRatio = toneRatio - 0.4;

            double binVelocity = binVelocities[i];
            Color color = colorFunction.toColor(binVelocity, toneRatio);
//            Color color = colorFunction.toColor(1, toneRatio);
            double velocity = binVelocities[index];
            double myVelocity = velocity;
            if (biggestBinNumber != i) {
                myVelocity = velocity * 0.9;
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

            drawOuterDot(gl, biggestBinNumber, angle, i, index, color, sinStartAngle, cosStartAngle, sinEndAngle, cosEndAngle);
        }
    }

    private void drawOuterDot(GL2 gl, int biggestBinNumber, double angle, int i, int index, Color color, double sinStartAngle, double cosStartAngle, double sinEndAngle, double cosEndAngle) {
        double outerRadius = 0.98;
        gl.glBegin(GL.GL_TRIANGLES);
        gl.glColor3ub((byte) color.getRed(),
                (byte) color.getGreen(),
                (byte) color.getBlue());

        double centerRadius;
        if (Pitchenga.isPlaying()) {
            if (biggestBinNumber == i) {
                centerRadius = outerRadius - 0.03;
            } else {
                centerRadius = outerRadius - 0.01;
            }
        } else {
            centerRadius = outerRadius * 0.99 - binVelocities[index] * 0.02;
        }

        double centerAngle = angle - 0.000000001 * stepAngle;
        double sinCenterAngle = FastMath.sin(centerAngle);
        double cosCenterAngle = FastMath.cos(centerAngle);
        gl.glVertex2d(centerRadius * sinCenterAngle, centerRadius * cosCenterAngle);
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
            Rectangle2D bounds = renderer.getBounds(halftoneName);
            int offsetX = (int) (scaleFactor * 0.5f * bounds.getWidth());
            int offsetY = (int) (scaleFactor * 0.5f * bounds.getHeight());
//            double radius = 0.43;
            double radius = 0.42;
            int x = (int) (centerX + radius * size * FastMath.sin(angle) - offsetX);
            int y = (int) (centerY + radius * size * FastMath.cos(angle) - offsetY);
            Color color;
            if ((tone != null && tone.name().equalsIgnoreCase(HALFTONE_NAMES[i]))) {
                color = BLACK;
                renderer.setColor(color);
                //fixme: There must be an easier way to draw outline font
                int offset = 2;
                renderer.draw3D(halftoneName, x + offset, y - offset, 0, scaleFactor);
                renderer.draw3D(halftoneName, x - offset, y + offset, 0, scaleFactor);
                renderer.draw3D(halftoneName, x + offset, y + offset, 0, scaleFactor);
                renderer.draw3D(halftoneName, x - offset, y - offset, 0, scaleFactor);
                color = colorFunction.toColor(100, i);
            } else {
                Tone myTone = Pitchenga.TONE_BY_LOWERCASE_NAME.get(halftoneName);
                if (myTone != null && myTone.diatonic) {
                    color = MORE_DARK;
                } else {
                    color = EVEN_MORE_DARK;
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
        // Otherwise we can use the FPSAnimator instead of the plain Animator.
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
    public void reshape(GLAutoDrawable drawable, int x, int y, int width,
                        int height) {
        setConstantAspectRatio(drawable);
    }

    @Override
    public Component getComponent() {
        return component;
    }
}