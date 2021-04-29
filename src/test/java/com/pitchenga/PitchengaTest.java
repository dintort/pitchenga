package com.pitchenga;


import static com.pitchenga.Pitch.*;

public class PitchengaTest {

    public static void main(String[] args) {
        testTranspose();
        System.out.println("Green");
    }

    private static void testTranspose() {
        if (!Do0.equals(Pitchenga.transposePitch(Do0, 0, 0))) throw new RuntimeException();

        if (!Ti0.equals(Pitchenga.transposePitch(Do0, 0, -1))) throw new RuntimeException();
        if (!Te0.equals(Pitchenga.transposePitch(Do0, 0, -2))) throw new RuntimeException();
        if (!Do8.equals(Pitchenga.transposePitch(Ti8, 0, 1))) throw new RuntimeException();
        if (!Do8.equals(Pitchenga.transposePitch(Ti8, 0, 13))) throw new RuntimeException();

        if (!Do0.equals(Pitchenga.transposePitch(Do0, -3, 0))) throw new RuntimeException();
        if (!Ti8.equals(Pitchenga.transposePitch(Ti8, 42, 0))) throw new RuntimeException();
        if (!Ra4.equals(Pitchenga.transposePitch(Do1, 3, 1))) throw new RuntimeException();
    }


}