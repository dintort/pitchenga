package com.pitchenga;

import java.awt.event.KeyEvent;

public enum Key {
    //fixme: +Diatonic keyboard layout with sharps via shift
    //fixme: +Micro-tones on unused keys?
    //fixme: +Fret-less sliding up and down on shift and control
    //fixme: +Toggleable A B C D E etc button labels
    //fixme: +Num-pad mapping

//    np01(Pitch.fI3, KeyEvent.VK_NUMPAD1, "1", false),
//    np02(Pitch.So3, KeyEvent.VK_NUMPAD2, "2", false),
//    np03(Pitch.lO3, KeyEvent.VK_NUMPAD3, "3", false),
//    np04(Pitch.La3, KeyEvent.VK_NUMPAD4, "4", false),
//    np05(Pitch.tU3, KeyEvent.VK_NUMPAD5, "5", false),
//    np06(Pitch.Ti3, KeyEvent.VK_NUMPAD6, "6", false),
//    np07(Pitch.Do4, KeyEvent.VK_NUMPAD7, "7", false),
//    np08(Pitch.rA4, KeyEvent.VK_NUMPAD8, "8", false),
//    np09(Pitch.Re4, KeyEvent.VK_NUMPAD9, "9", false),
//    np10(Pitch.mE4, KeyEvent.VK_  "0", false),
//    np11(Pitch.Mi4, KeyEvent.VK_MULTIPLY, "-", false),
//    np12(Pitch.Fa4, KeyEvent.VK_ , "=", false),


    n01(Pitch.fI3, KeyEvent.VK_1, "1", true),
    n02(Pitch.So3, KeyEvent.VK_2, "2", true),
    n03(Pitch.lO3, KeyEvent.VK_3, "3", true),
    n04(Pitch.La3, KeyEvent.VK_4, "4", true),
    n05(Pitch.tU3, KeyEvent.VK_5, "5", true),
    n06(Pitch.Ti3, KeyEvent.VK_6, "6", true),
    n07(Pitch.Do4, KeyEvent.VK_7, "7", true),
    n08(Pitch.rA4, KeyEvent.VK_8, "8", true),
    n09(Pitch.Re4, KeyEvent.VK_9, "9", true),
    n10(Pitch.mE4, KeyEvent.VK_0, "0", true),
    n11(Pitch.Mi4, KeyEvent.VK_MINUS, "-", true),
    n12(Pitch.Fa4, KeyEvent.VK_EQUALS, "=", true),

    m01(Pitch.fI3, -1, null, false),
    m02(Pitch.So3, -2, null, false),
    m03(Pitch.lO3, -3, null, false),
    m04(Pitch.La3, -4, null, false),
    m05(Pitch.tU3, -5, null, false),
    m06(Pitch.Ti3, -6, null, false),
    m07(null, -7, null, false),
    A(Pitch.Do3, KeyEvent.VK_A, "a", false),
    W(Pitch.rA3, KeyEvent.VK_W, "w", false),
    S(Pitch.Re3, KeyEvent.VK_S, "s", false),
    E(Pitch.mE3, KeyEvent.VK_E, "e", false),
    D(Pitch.Mi3, KeyEvent.VK_D, "d", false),
    R(null, KeyEvent.VK_R, null, false),
    F(Pitch.Fa3, KeyEvent.VK_F, "f", false),
    T(Pitch.fI3, KeyEvent.VK_T, "t", false),
    G(Pitch.So3, KeyEvent.VK_G, "g", false),
    Y(Pitch.lO3, KeyEvent.VK_Y, "y", false),
    H(Pitch.La3, KeyEvent.VK_H, "h", false),
    U(Pitch.tU3, KeyEvent.VK_U, "u", false),
    I(null, KeyEvent.VK_I, null, false),
    J(Pitch.Ti3, KeyEvent.VK_J, "j", false),
    K(Pitch.Do4, KeyEvent.VK_K, "k", false),
    O(Pitch.rA4, KeyEvent.VK_O, "o", false),
    L(Pitch.Re4, KeyEvent.VK_L, "l", false),
    P(Pitch.mE4, KeyEvent.VK_P, "p", false),
    SEMICOLON(Pitch.Mi4, KeyEvent.VK_SEMICOLON, ";", false),
    QUOTE(Pitch.Fa4, KeyEvent.VK_QUOTE, "\"", false),
    m999(null, -999, null, false),
    ;

    private final Pitch pitch;
    private final int keyEvent;
    private final String label;
    private final boolean chromaticPiano;

    Key(Pitch pitch, int keyEvent, String label, boolean chromaticPiano) {
        this.pitch = pitch;
        this.keyEvent = keyEvent;
        this.label = label;
        this.chromaticPiano = chromaticPiano;
    }


    public Pitch getPitch() {
        return pitch;
    }

    public int getKeyEventCode() {
        return keyEvent;
    }

    public String getLabel() {
        return label;
    }

    public boolean isChromaticPiano() {
        return chromaticPiano;
    }

}
