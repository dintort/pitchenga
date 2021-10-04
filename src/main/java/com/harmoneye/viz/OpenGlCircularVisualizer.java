package com.harmoneye.viz;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.geom.Rectangle2D;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import com.pitchenga.Pitchenga;
import com.pitchenga.Tone;
import org.apache.commons.math3.util.FastMath;

import com.harmoneye.analysis.AnalyzedFrame;
import com.harmoneye.math.cqt.CqtContext;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.awt.TextRenderer;

// TODO: rewrite to use vertex buffers instead of immediate mode

public class OpenGlCircularVisualizer implements
        SwingVisualizer<AnalyzedFrame>, GLEventListener {

    //fixme: Un-hack
    public static volatile OpenGlCircularVisualizer INSTANCE;
    private static final float DEFAULT_LINE_WIDTH = 1f;
    private static final float WAIT_SCROBBLER_LINE_WIDTH = 1.5f;

    protected static final String[] HALFTONE_NAMES = {"do", "ra", "re", "me",
            "mi", "fa", "fi", "so", "le", "la", "se", "si"};

    private int pitchStep = 1;

    private double[] binVelocities;

    private ColorFunction colorFunction = new ColorFunction();
    private Component component;
    private int binsPerHalftone;
    private int halftoneCount;
    private double segmentCountInv;
    private double stepAngle;

    private TextRenderer renderer;
    public static volatile Tone toneOverride;
    public static volatile boolean locked;

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
        if (Pitchenga.isPlaying()) {
            return;
        }
//        if (Pitchenga.frozen) {
//            return;
//        }

        CqtContext ctx = pcProfile.getCtxContext();

        binsPerHalftone = ctx.getBinsPerHalftone();
        halftoneCount = ctx.getHalftonesPerOctave();

        double[] octaveBins = pcProfile.getOctaveBins();
        for (int i = 0; i < octaveBins.length; i++) {
            octaveBins[i] = octaveBins[i] * 1.05;
            if (octaveBins[i] > 1) {
                octaveBins[i] = 1;
            }
        }
        this.binVelocities = octaveBins;
        segmentCountInv = 1.0 / this.binVelocities.length;
        stepAngle = 2 * FastMath.PI * segmentCountInv;
    }

    @Override
    public void setPitchStep(int i) {
        this.pitchStep = i;
    }

    @Override
    public void display(GLAutoDrawable drawable) {
//        if (Pitchenga.frozen) {
//            return;
//        }
//        if (locked) {
//            return;
//        }
        Tone tone = toneOverride;
//        if (Pitchenga.isPlaying() && tone == null) {
//            return;
//        }

        GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        //match bin
        int biggestBinNumber = -1;
        if (Pitchenga.isPlaying()) {
            if (binVelocities != null) {
                binVelocities = new double[binVelocities.length];
                if (binVelocities.length > 0) {
                    if (tone != null) {
                        double binRatio = (int) (tone.ordinal() / ((double) Tone.values().length / (double) binVelocities.length));
                        //fixme: hack
                        binRatio = binRatio + 4;
//                        binRatio = binRatio + 7.5;
                        biggestBinNumber = (int) binRatio;
                        //                Arrays.fill(binVelocities, 0.1);
//                    binVelocities[addWithFlip(biggestBinNumber, -12)] = 0.3;
//                    binVelocities[addWithFlip(biggestBinNumber, -11)] = 0.35;
//                    binVelocities[addWithFlip(biggestBinNumber, -10)] = 0.4;
//                    binVelocities[addWithFlip(biggestBinNumber, -9)] = 0.45;
//                    binVelocities[addWithFlip(biggestBinNumber, -8)] = 0.5;
//                    binVelocities[addWithFlip(biggestBinNumber, -7)] = 0.55;
//            binVelocities[addWithFlip(biggestBinNumber, -6)] = 0.6;
//            binVelocities[addWithFlip(biggestBinNumber, -5)] = 0.65;
//                        binVelocities[addWithFlip(biggestBinNumber, -4)] = 0.7;
                        binVelocities[addWithFlip(biggestBinNumber, -3)] = 0.75;
                        binVelocities[addWithFlip(biggestBinNumber, -2)] = 0.8;
                        binVelocities[addWithFlip(biggestBinNumber, -1)] = 0.85;
                        binVelocities[biggestBinNumber] = 0.9;
                        binVelocities[addWithFlip(biggestBinNumber, 1)] = 0.85;
                        binVelocities[addWithFlip(biggestBinNumber, 2)] = 0.8;
                        binVelocities[addWithFlip(biggestBinNumber, 3)] = 0.75;
//                        binVelocities[addWithFlip(biggestBinNumber, 4)] = 0.7;
//            binVelocities[addWithFlip(biggestBinNumber, 5)] = 0.65;
//            binVelocities[addWithFlip(biggestBinNumber, 6)] = 0.6;
//                    binVelocities[addWithFlip(biggestBinNumber, 7)] = 0.55;
//                    binVelocities[addWithFlip(biggestBinNumber, 8)] = 0.5;
//                    binVelocities[addWithFlip(biggestBinNumber, 9)] = 0.45;
//                    binVelocities[addWithFlip(biggestBinNumber, 10)] = 0.4;
//                    binVelocities[addWithFlip(biggestBinNumber, 11)] = 0.35;
//                    binVelocities[addWithFlip(biggestBinNumber, 12)] = 0.3;
                    }
                }
            }
        } else {
            double biggestBinVelocity = 0;
            if (binVelocities != null) {
                for (int i = 0; i < binVelocities.length; i++) {
                    double value = binVelocities[i];
                    if (value > biggestBinVelocity) {
                        biggestBinVelocity = value;
                        biggestBinNumber = i;
                    }
                }
//            if (toneOverride != null) {
//                tone = toneOverride;
                double toneRatio = (double) biggestBinNumber / ((double) binVelocities.length / (double) Tone.values().length);
                tone = Tone.values()[(int) toneRatio];
//            } else {
//            }
            }
        }


        drawPitchClassFrame(gl);
        drawPitchClassBins(gl, biggestBinNumber, tone);
        drawHalftoneNames(drawable, tone);
        if (!isDataAvailable()) {
            drawWaitingAnimation(gl);
        }
    }

    private void drawWaitingAnimation(GL2 gl) {
        long millis = System.currentTimeMillis();
        long millisInSecond = millis % 1000;
        float phaseOffset = (float) (millisInSecond * 0.001);

        gl.glLineWidth(WAIT_SCROBBLER_LINE_WIDTH);
        double halfToneCountInv = 1.0 / HALFTONE_NAMES.length;
        double maxRadius = 0.09;
        double innerRadius = maxRadius * 0.25;
        double outerRadius = maxRadius * 0.75;
        gl.glBegin(GL.GL_LINES);
        for (int i = 0; i < HALFTONE_NAMES.length; i++) {
            double unitAngle = (i - 0.5) * halfToneCountInv;
            double angle = 2 * FastMath.PI * unitAngle;

            float velocity = (float) (0.25 + 0.5 * ((1 - unitAngle + phaseOffset) % 1.0));
            Color color = colorFunction.toColor(velocity, i);
            gl.glColor3ub((byte) color.getRed(),
                    (byte) color.getGreen(),
                    (byte) color.getBlue());

            double x = FastMath.sin(angle);
            double y = FastMath.cos(angle);
            gl.glVertex2d(innerRadius * x, innerRadius * y);
            gl.glVertex2d(outerRadius * x, outerRadius * y);
        }
        gl.glEnd();
        gl.glLineWidth(DEFAULT_LINE_WIDTH);
    }

    private void drawPitchClassFrame(GL2 gl) {
        gl.glClearColor(0f, 0f, 0f, 1f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

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
            double lineRadius = 0.71;
            double x = lineRadius * FastMath.sin(angle);
            double y = lineRadius * FastMath.cos(angle);
            gl.glVertex2d(x, y);
            gl.glVertex2d(0, 0);
        }
        gl.glEnd();
    }

    private void drawPitchClassBins(GL2 gl, int biggestBinNumber, Tone tone) {
        if (!isDataAvailable()) {
            return;
        }

//        double radius = 0.68;
//        double radius = 0.99;
        double radius = 0.8;

//        gl.glBegin(GL.GL_TRIANGLES);
        double angle = 0.5 * (1 - binsPerHalftone) * stepAngle;
        for (int i = 0; i < binVelocities.length; i++, angle += stepAngle) {
            int pitchClass = i / binsPerHalftone;
            int binInPitchClass = i % binsPerHalftone;
            int movedPitchClass = (pitchClass * pitchStep) % halftoneCount;
            int index = movedPitchClass * binsPerHalftone + binInPitchClass;

//            Pair<Color, Color> guessAndPitchinessColor = Pitchenga.getGuessAndPitchinessColor(0, biggestTone.getFugue().pitch, 1, biggestTone.color);
//            Color color = guessAndPitchinessColor.left;

            double toneRatio = i / ((double) binVelocities.length / (double) Tone.values().length);

            //fixme: hack
            toneRatio = toneRatio - 0.4;

//            double toneRatio = angle * (1.0 / (double) Tone.values().length);
            Color color = colorFunction.toColor(1, toneRatio);
            double velocity = binVelocities[index];
            double myVelocity = velocity;
            if (biggestBinNumber != i) {
                myVelocity = velocity * 0.9;
            }
//            Color color = colorFunction.toColor(myVelocity, i);

//            Color color = colorFunction.toColor((float) binVelocities[i], toneRatio);
//            if (
//            if (binVelocities[i] > 0.3) {
//                System.out.println("i=" + i + " ratio=" + toneRatio + " bins=" + binVelocities.length);
//            }

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

            // Outer dot
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
                if (biggestBinNumber == i) {
                    centerRadius = outerRadius * 0.99 - binVelocities[index] * 0.1;
                } else {
//                    centerRadius = outerRadius - 0.02;
                    centerRadius = outerRadius * 0.99 - binVelocities[index] * 0.05;
                }
            }

            double centerAngle = angle - 0.000000001 * stepAngle;
            double sinCenterAngle = FastMath.sin(centerAngle);
            double cosCenterAngle = FastMath.cos(centerAngle);
            gl.glVertex2d(centerRadius * sinCenterAngle, centerRadius * cosCenterAngle);
            gl.glVertex2d(outerRadius * sinStartAngle, outerRadius * cosStartAngle);
            gl.glVertex2d(outerRadius * sinEndAngle, outerRadius * cosEndAngle);
            gl.glEnd();
        }
//        gl.glEnd();
//
//        gl.glBegin(GL.GL_TRIANGLES);
//
//        gl.glEnd();
    }

    private int addWithFlip(int biggestBinNumber, int i) {
        int result = biggestBinNumber + i;
        result = result % binVelocities.length;
        if (result < 0) {
//            //fixme: magic number
            result = 0;
//            result = 108  + result;
//            result = 0;
        }
//        if (result >= 107) {
//            result = 107;
//            result = 107 - result;
//        }
        return result;
    }

    private boolean isDataAvailable() {
        return binVelocities != null;
    }

    private void drawHalftoneNames(GLAutoDrawable drawable, Tone tone) {
        int width = drawable.getSurfaceWidth();
        int height = drawable.getSurfaceHeight();

        double centerX = width / 2;
        double centerY = height / 2;
        double size = 0.99 * FastMath.min(width, height);
        double angleStep = 2 * FastMath.PI / HALFTONE_NAMES.length;
        double angle = 0;
        float scaleFactor = (float) (0.0015f * size);


//        if (Pitchenga.isPlaying() && toneOverride != null) {
//            tone = toneOverride;
//        }
        if (binVelocities != null && binVelocities.length > 0) {
            renderer.beginRendering(width, height);
            for (int i = 0; i < HALFTONE_NAMES.length; i++, angle += angleStep) {
                int index = (i * pitchStep) % HALFTONE_NAMES.length;
                float velocity = getMaxBinValue(index);
                float myVelocity;
                myVelocity = velocity * 0.001f;
                if (tone != null && tone.name().equalsIgnoreCase(HALFTONE_NAMES[i])) {
                    myVelocity = 1f;
                }
                Color color = colorFunction.toColor(myVelocity, i);
                renderer.setColor(color);
                String str = HALFTONE_NAMES[index];
                Rectangle2D bounds = renderer.getBounds(str);
                int offsetX = (int) (scaleFactor * 0.5f * bounds.getWidth());
                int offsetY = (int) (scaleFactor * 0.5f * bounds.getHeight());
//            double radius = 0.43;
                double radius = 0.42;
                int x = (int) (centerX + radius * size * FastMath.sin(angle) - offsetX);
                int y = (int) (centerY + radius * size * FastMath.cos(angle) - offsetY);
                renderer.draw3D(str, x, y, 0, scaleFactor);
            }
            renderer.endRendering();
        }
    }

    private float getMaxBinValue(int halftoneIndex) {
        float max = 0;
        int baseIndex = binsPerHalftone * halftoneIndex;
        for (int i = 0; i < binsPerHalftone; i++) {
            float value = (float) binVelocities[baseIndex + i];
            max = FastMath.max(max, value);
        }
        return max;
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

    double getAverageValue(int upperIndex) {
        int lowerIndex = (upperIndex + binVelocities.length + 1) % binVelocities.length;
        upperIndex = upperIndex % binVelocities.length;
        return 0.5 * (binVelocities[lowerIndex] + binVelocities[upperIndex]);
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