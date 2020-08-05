package com.pitchenga;

import java.awt.event.KeyEvent;

public enum Key {
    //fixme: +Alternative full-keyboard diatonic layout with sharps via shift?
    //fixme: +Fret-less sliding up and down with modifier keys
    //fixme: +Toggleable A B C D E etc button labels
    //fixme: +Num-pad mapping

//    np01(Pitch.Fi3, KeyEvent.VK_NUMPAD1, "1", false),
//    np02(Pitch.So3, KeyEvent.VK_NUMPAD2, "2", false),
//    np03(Pitch.Le3, KeyEvent.VK_NUMPAD3, "3", false),
//    np04(Pitch.La3, KeyEvent.VK_NUMPAD4, "4", false),
//    np05(Pitch.Se3, KeyEvent.VK_NUMPAD5, "5", false),
//    np06(Pitch.Si3, KeyEvent.VK_NUMPAD6, "6", false),
//    np07(Pitch.Do4, KeyEvent.VK_NUMPAD7, "7", false),
//    np08(Pitch.Ra4, KeyEvent.VK_NUMPAD8, "8", false),
//    np09(Pitch.Re4, KeyEvent.VK_NUMPAD9, "9", false),
//    np10(Pitch.Me4, KeyEvent.VK_  "0", false),
//    np11(Pitch.Mi4, KeyEvent.VK_MULTIPLY, "-", false),
//    np12(Pitch.Fa4, KeyEvent.VK_ , "=", false),


    n00(Pitch.Do4, KeyEvent.VK_BACK_QUOTE, 0,"`"),
    n01(Pitch.Ra4, KeyEvent.VK_1, 0,"1"),
    n02(Pitch.Re4, KeyEvent.VK_2, 0, "2"),
    n03(Pitch.Me4, KeyEvent.VK_3, 0, "3"),
    n04(Pitch.Mi4, KeyEvent.VK_4, 0, "4"),
    n05(Pitch.Fa4, KeyEvent.VK_5, 0, "5"),
    n06(Pitch.Fi4, KeyEvent.VK_6, 0, "6"),
    n07(Pitch.So4, KeyEvent.VK_7, 0, "7"),
    n08(Pitch.Le4, KeyEvent.VK_8, 0, "8"),
    n09(Pitch.La4, KeyEvent.VK_9, 0, "9"),
    n10(Pitch.Te4, KeyEvent.VK_0, 0, "0"),
    n11(Pitch.Ti4, KeyEvent.VK_MINUS, 0, "-"),
    n12(Pitch.Do5, KeyEvent.VK_EQUALS, 0, "="),

//    do3(Pitch.Do3, -1, 1, null, false),
//    ra3(Pitch.Ra3, -2, 1, null, false),
//    re3(Pitch.Re3, -3, 1, null, false),
//    me3(Pitch.Me3, -4, 1, null, false),
//    mi3(Pitch.Mi3, -5, 1, null, false),
//    fa3(Pitch.Fa3, -6, 1, null, false),
//    fi3(Pitch.Fi3, -7, 1,null, true),
//    so3(Pitch.So3, -8, 1, null, true),
//    le3(Pitch.Le3, -9, 1, null, true),
//    la3(Pitch.La3, -10, 1, null, true),
//    se3(Pitch.Se3, -11, 1, null, true),
//    si3(Pitch.Si3, -12, 1, null, true),
//    do4(Pitch.Do4, -13, 1, null, true),
//    ra4(Pitch.Ra4, -14, 1, null, true),
//    re4(Pitch.Re4, -15, 1, null, true),
//    me4(Pitch.Me4, -16, 1, null, true),
//    mi4(Pitch.Mi4, -17, 1,null, true),
//    fa4(Pitch.Fa4, -18, 1, null, true),
//    m01(Pitch.Fi3, -1, null, false),
//    m02(Pitch.So3, -2, null, false),
//    m03(Pitch.Le3, -3, null, false),
//    m04(Pitch.La3, -4, null, false),
//    m05(Pitch.Se3, -5, null, false),
//    m06(Pitch.Si3, -6, null, false),
//    m07(null, -7, null, false),
//    Q(null, KeyEvent.VK_Q, 1, null, false),
    A(Pitch.Do4, KeyEvent.VK_A, 2, "a"),
    W(Pitch.Ra4, KeyEvent.VK_W, 1, "w"),
    S(Pitch.Re4, KeyEvent.VK_S, 2, "s"),
    E(Pitch.Me4, KeyEvent.VK_E, 1, "e"),
    D(Pitch.Mi4, KeyEvent.VK_D, 2, "d"),
    R(null, KeyEvent.VK_R, 1, null), //fixme: Micro-tonal "fleh" and "floh"
    F(Pitch.Fa4, KeyEvent.VK_F, 2, "f"),
    T(Pitch.Fi4, KeyEvent.VK_T, 1, "t"),
    G(Pitch.So4, KeyEvent.VK_G, 2, "g"),
    Y(Pitch.Le4, KeyEvent.VK_Y, 1, "y"),
    H(Pitch.La4, KeyEvent.VK_H, 2, "h"),
    U(Pitch.Te4, KeyEvent.VK_U, 1, "u"),
    J(Pitch.Ti4, KeyEvent.VK_J, 2, "j"),
    I(null, KeyEvent.VK_I, 1, null),
    K(Pitch.Do5, KeyEvent.VK_K, 2, "k"),
    O(Pitch.Ra5, KeyEvent.VK_O, 1, "o"),
    L(Pitch.Re5, KeyEvent.VK_L, 2, "l"),
    P(Pitch.Me5, KeyEvent.VK_P, 1, "p"),
    SEMICOLON(Pitch.Mi5, KeyEvent.VK_SEMICOLON, 2, ";"),
    OPEN_BRACKET(null, KeyEvent.VK_OPEN_BRACKET, 1, null),
    QUOTE(Pitch.Fa5, KeyEvent.VK_QUOTE, 2, "\""),
//    OPEN_BRACKET(Pitch.Fi4, KeyEvent.VK_OPEN_BRACKET, null, false),
//    CLOSE_BRACKET(Pitch.Fi4, KeyEvent.VK_CLOSE_BRACKET, 2, null, false),
//    BACK_SLASH(Pitch.So4, KeyEvent.VK_BACK_SLASH, null, false),
    ;

    public final Pitch pitch;
    public final int keyEventCode;
    public final int row;
    public final String label;

    Key(Pitch pitch, int keyEventCode, int row, String label) {
        this.pitch = pitch;
        this.keyEventCode = keyEventCode;
        this.row = row;
        this.label = label;
    }

}
