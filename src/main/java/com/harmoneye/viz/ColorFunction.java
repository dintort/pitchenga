package com.harmoneye.viz;

import com.pitchenga.Pair;
import com.pitchenga.Pitch;
import com.pitchenga.Pitchenga;
import com.pitchenga.Tone;

import java.awt.Color;

import static com.pitchenga.Tone.Do;

public class ColorFunction {

    public Color toColor(double velocity, double toneRatio) {
        int toneNumber = (int) toneRatio;
        double diff = toneRatio - toneNumber;

        Tone tone;
        if (toneNumber >= Tone.values().length) {
            tone = Do;
            System.out.println("toneNumber=" + toneNumber);
        } else {
            tone = Tone.values()[toneNumber];
        }
        Pitch guess = tone.getFugue().pitch;

        Pitch pitchy;
        if (diff < 0) {
            pitchy = Pitchenga.transposePitch(guess, 0, -1);
        } else {
            pitchy = Pitchenga.transposePitch(guess, 0, +1);
        }
//		double pitchyDiff = Math.abs(guess.frequency - pitchy.frequency);
        double pitchyDiff = Math.abs(toneRatio - pitchy.ordinal());
        double inaccuracy = Math.abs(diff) / pitchyDiff;
        inaccuracy = inaccuracy * 40;
        Color toneColor = guess.tone.color;
        Pair<Color, Color> guessAndPitchinessColor = Pitchenga.getGuessAndPitchinessColor(diff, pitchy, inaccuracy, toneColor);
        Color color = guessAndPitchinessColor.left;
//		Color color = toneColor;

//		float colorVelocity = velocity * 0.5f;
        double colorVelocity = 0.3 + velocity * 1.2;
        if (colorVelocity > 1) {
            colorVelocity = 1;
        }
        //fixme: restore
        color = Pitchenga.interpolateColor(colorVelocity, Color.black, color);
        return color;

////		double toneRatio = (double) biggest / ((double) values.length / (double) TONES.length);
//		double toneRatio = (double) value / ((double) 1 / (double) Tone.values().length);
//
//		int toneNumber = (int) toneRatio;
////            int toneNumber = (int) Math.round(toneRatio);
//		Tone tone;
//		if (toneNumber >= Tone.values().length) {
//			tone = Do;
//			System.out.println("toneNumber=" + toneNumber);
//		} else {
//			tone = Tone.values()[toneNumber];
//		}


//		int n = (int) (value / 12);
//		if (n > Tone.values().length) {
//			System.out.println("n=" + n);
//		}
//		Tone tone = Tone.values()[n];
//		Pitch pitchy;
//		if (diff < 0) {
//			pitchy = transposePitch(guess, 0, -1);
//		} else {
//			pitchy = transposePitch(guess, 0, +1);
//		}
//		double pitchyDiff = Math.abs(guess.frequency - pitchy.frequency);
//		double accuracy = Math.abs(diff) / pitchyDiff;
//		double pitchiness = accuracy * 20;


//		Pair<Color, Color> guessAndPitchinessColor = Pitchenga.getGuessAndPitchinessColor(0, tone.getFugue().pitch, 1, tone.color);
//		Color color = guessAndPitchinessColor.left;
//		System.out.println("tone=" + tone + " value=" + value + " ratio=" + toneRatio + " n=" + toneNumber);
//		return color;

//		float hue = (1.8f - value) % 1.0f;
        //float saturation = 0.75f * value + 0.25f * value * value;
//		float saturation = value > 0.5f ? value : 0.05f + value * value;
//		float brightness = 0.25f + 0.75f * value;
//		return Color.getHSBColor(hue, saturation, brightness);
    }

}