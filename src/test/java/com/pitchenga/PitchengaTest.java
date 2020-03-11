package com.pitchenga;


import static com.pitchenga.Pitch.*;

public class PitchengaTest {

    public static void main(String[] args) {
        testSeriesHint();
        testTranspose();
        System.out.println("Green");
    }

    private static void testTranspose() {
        if (!Do0.equals(Pitchenga.transposePitch(Do0, 0, 0))) throw new RuntimeException();

        if (!Si0.equals(Pitchenga.transposePitch(Do0, 0, -1))) throw new RuntimeException();
        if (!Se0.equals(Pitchenga.transposePitch(Do0, 0, -2))) throw new RuntimeException();
        if (!Do8.equals(Pitchenga.transposePitch(Si8, 0, 1))) throw new RuntimeException();
        if (!Do8.equals(Pitchenga.transposePitch(Si8, 0, 13))) throw new RuntimeException();

        if (!Do0.equals(Pitchenga.transposePitch(Do0, -3, 0))) throw new RuntimeException();
        if (!Si8.equals(Pitchenga.transposePitch(Si8, 42, 0))) throw new RuntimeException();
        if (!Ra4.equals(Pitchenga.transposePitch(Do1, 3, 1))) throw new RuntimeException();
    }

    private static void testSeriesHint() {
        if (Pitchenga.isShowSeriesHint(0)) throw new RuntimeException();
        if (Pitchenga.isShowSeriesHint(1)) throw new RuntimeException();
        if (Pitchenga.isShowSeriesHint(2)) throw new RuntimeException();
        if (!Pitchenga.isShowSeriesHint(3)) throw new RuntimeException();
        if (!Pitchenga.isShowSeriesHint(4)) throw new RuntimeException();
        if (!Pitchenga.isShowSeriesHint(5)) throw new RuntimeException();
        if (!Pitchenga.isShowSeriesHint(6)) throw new RuntimeException();
        if (!Pitchenga.isShowSeriesHint(7)) throw new RuntimeException();
        if (!Pitchenga.isShowSeriesHint(8)) throw new RuntimeException();
        if (Pitchenga.isShowSeriesHint(9)) throw new RuntimeException();
        if (Pitchenga.isShowSeriesHint(10)) throw new RuntimeException();
        if (Pitchenga.isShowSeriesHint(11)) throw new RuntimeException();
        if (!Pitchenga.isShowSeriesHint(12)) throw new RuntimeException();
        if (!Pitchenga.isShowSeriesHint(13)) throw new RuntimeException();
        if (!Pitchenga.isShowSeriesHint(14)) throw new RuntimeException();
        if (!Pitchenga.isShowSeriesHint(15)) throw new RuntimeException();
        if (!Pitchenga.isShowSeriesHint(16)) throw new RuntimeException();
        if (!Pitchenga.isShowSeriesHint(17)) throw new RuntimeException();
    }

}