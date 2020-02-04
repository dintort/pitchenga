package com.pitchenga;

import static com.pitchenga.Tone.*;

public enum Pitch {
    Do0(Do, 0, 10, 16.35f),
    rA0(rA, 0, 12, 17.32f),
    Re0(Re, 0, 13, 18.35f),
    mE0(mE, 0, 14, 19.45f),
    Mi0(Mi, 0, 16, 20.6f),
    Fa0(Fa, 0, 17, 21.83f),
    fI0(fI, 0, 18, 23.12f),
    So0(So, 0, 19, 24.5f),
    lU0(lU, 0, 20, 25.96f),
    La0(La, 0, 21, 27.5f),
    tO0(tO, 0, 22, 29.14f),
    Ti0(Ti, 0, 23, 30.87f),
    Do1(Do, 1, 24, 32.7f),
    rA1(rA, 1, 25, 34.65f),
    Re1(Re, 1, 26, 36.71f),
    mE1(mE, 1, 27, 38.89f),
    Mi1(Mi, 1, 28, 41.2f),
    Fa1(Fa, 1, 29, 43.65f),
    fI1(fI, 1, 30, 46.25f),
    So1(So, 1, 31, 49f),
    lU1(lU, 1, 33, 51.91f),
    La1(La, 1, 32, 55f),
    tO1(tO, 1, 34, 58.27f),
    Ti1(Ti, 1, 35, 61.74f),
    Do2(Do, 2, 36, 65.41f),
    rA2(rA, 2, 37, 69.3f),
    Re2(Re, 2, 38, 73.42f),
    mE2(mE, 2, 39, 77.78f),
    Mi2(Mi, 2, 40, 82.41f),
    Fa2(Fa, 2, 41, 87.31f),
    fI2(fI, 2, 42, 92.5f),
    So2(So, 2, 43, 98f),
    lU2(lU, 2, 44, 103.83f),
    La2(La, 2, 45, 110f),
    tO2(tO, 2, 46, 116.54f),
    Ti2(Ti, 2, 47, 123.47f),
    Do3(Do, 3, 48, 130.81f),
    rA3(rA, 3, 49, 138.59f),
    Re3(Re, 3, 50, 146.83f),
    mE3(mE, 3, 51, 155.56f),
    Mi3(Mi, 3, 52, 164.81f),
    Fa3(Fa, 3, 53, 174.61f),
    fI3(fI, 3, 54, 185f),
    So3(So, 3, 55, 196f),
    lU3(lU, 3, 56, 207.65f),
    La3(La, 3, 57, 220f),
    tO3(tO, 3, 58, 233.08f),
    Ti3(Ti, 3, 59, 246.94f),
    Do4(Do, 4, 60, 261.63f),
    rA4(rA, 4, 61, 277.18f),
    Re4(Re, 4, 62, 293.66f),
    mE4(mE, 4, 63, 311.13f),
    Mi4(Mi, 4, 64, 329.63f),
    Fa4(Fa, 4, 65, 349.23f),
    fI4(fI, 4, 66, 369.99f),
    So4(So, 4, 67, 392f),
    lU4(lU, 4, 68, 415.3f),
    La4(La, 4, 69, 440f),
    tO4(tO, 4, 70, 466.16f),
    Ti4(Ti, 4, 71, 493.88f),
    Do5(Do, 5, 72, 523.25f),
    rA5(rA, 5, 73, 554.37f),
    Re5(Re, 5, 74, 587.33f),
    mE5(mE, 5, 75, 622.25f),
    Mi5(Mi, 5, 76, 659.25f),
    Fa5(Fa, 5, 77, 698.46f),
    fI5(fI, 5, 78, 739.99f),
    So5(So, 5, 79, 783.99f),
    lU5(lU, 5, 80, 830.61f),
    La5(La, 5, 81, 880f),
    tO5(tO, 5, 82, 932.33f),
    Ti5(Ti, 5, 83, 987.77f),
    Do6(Do, 6, 84, 1046.5f),
    rA6(rA, 6, 85, 1108.73f),
    Re6(Re, 6, 86, 1174.66f),
    mE6(mE, 6, 87, 1244.51f),
    Mi6(Mi, 6, 88, 1318.51f),
    Fa6(Fa, 6, 89, 1396.91f),
    fI6(fI, 6, 90, 1479.98f),
    So6(So, 6, 91, 1567.98f),
    lU6(lU, 6, 92, 1661.22f),
    La6(La, 6, 93, 1760f),
    tO6(tO, 6, 94, 1864.66f),
    Ti6(Ti, 6, 95, 1975.53f),
    Do7(Do, 7, 96, 2093f),
    rA7(rA, 7, 97, 2217.46f),
    Re7(Re, 7, 98, 2349.32f),
    mE7(mE, 7, 99, 2489.02f),
    Mi7(Mi, 7, 100, 2637.02f),
    Fa7(Fa, 7, 101, 2793.83f),
    fI7(fI, 7, 102, 2959.96f),
    So7(So, 7, 103, 3135.96f),
    lU7(lU, 7, 104, 3322.44f),
    La7(La, 7, 105, 3520f),
    tO7(tO, 7, 106, 3729.31f),
    Ti7(Ti, 7, 107, 3951.07f),
    Do8(Do, 8, 108, 4186.01f),
    rA8(rA, 8, 109, 4434.92f),
    Re8(Re, 8, 110, 4698.63f),
    mE8(mE, 8, 111, 4978.03f),
    Mi8(Mi, 8, 112, 5274.04f),
    Fa8(Fa, 8, 113, 5587.65f),
    fI8(fI, 8, 114, 5919.91f),
    So8(So, 8, 115, 6271.93f),
    lU8(lU, 8, 116, 6644.88f),
    La8(La, 8, 117, 7040f),
    tO8(tO, 8, 118, 7458.62f),
    Ti8(Ti, 8, 119, 7902.13f),
    ;

    private final Tone tone;
    private final int midi;
    private final float frequency;
    private final int octave;

    Pitch(Tone tone, int octave, int midi, float frequency) {
        this.tone = tone;
        this.octave = octave;
        this.midi = midi;
        this.frequency = frequency;
    }

    public Tone getTone() {
        return tone;
    }

    public int getOctave() {
        return octave;
    }

    public float getFrequency() {
        return frequency;
    }

    public int getMidi() {
        return midi;
    }
}
