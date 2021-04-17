package com.pitchenga;

import java.awt.event.KeyEvent;

import static com.pitchenga.Pitch.*;

public enum Button {
    //fixme: +Alternative full-keyboard diatonic layout with sharps via shift?
    //fixme: +Fret-less sliding up and down with modifier keys
    //fixme: +Toggleable A B C D E etc button labels
    //fixme: +Num-pad mapping

//    np01(Fi3, KeyEvent.VK_NUMPAD1, "1", false),
//    np02(So3, KeyEvent.VK_NUMPAD2, "2", false),
//    np03(Le3, KeyEvent.VK_NUMPAD3, "3", false),
//    np04(La3, KeyEvent.VK_NUMPAD4, "4", false),
//    np05(Se3, KeyEvent.VK_NUMPAD5, "5", false),
//    np06(Si3, KeyEvent.VK_NUMPAD6, "6", false),
//    np07(Do4, KeyEvent.VK_NUMPAD7, "7", false),
//    np08(Ra4, KeyEvent.VK_NUMPAD8, "8", false),
//    np09(Re4, KeyEvent.VK_NUMPAD9, "9", false),
//    np10(Me4, KeyEvent.VK_  "0", false),
//    np11(Mi4, KeyEvent.VK_MULTIPLY, "-", false),
//    np12(Fa4, KeyEvent.VK_ , "=", false),


    n00(Do4, KeyEvent.VK_BACK_QUOTE, 0,"`"),
    n01(Ra4, KeyEvent.VK_1, 0,"1"),
    n02(Re4, KeyEvent.VK_2, 0, "2"),
    n03(Me4, KeyEvent.VK_3, 0, "3"),
    n04(Mi4, KeyEvent.VK_4, 0, "4"),
    n05(Fa4, KeyEvent.VK_5, 0, "5"),
    n06(Fi4, KeyEvent.VK_6, 0, "6"),
    n07(So4, KeyEvent.VK_7, 0, "7"),
    n08(Le4, KeyEvent.VK_8, 0, "8"),
    n09(La4, KeyEvent.VK_9, 0, "9"),
    n10(Se4, KeyEvent.VK_0, 0, "0"),
    n11(Si4, KeyEvent.VK_MINUS, 0, "-"),
    n12(Do5, KeyEvent.VK_EQUALS, 0, "="),

//    do3(Do3, -1, 1, null, false),
//    ra3(Ra3, -2, 1, null, false),
//    re3(Re3, -3, 1, null, false),
//    me3(Me3, -4, 1, null, false),
//    mi3(Mi3, -5, 1, null, false),
//    fa3(Fa3, -6, 1, null, false),
//    fi3(Fi3, -7, 1,null, true),
//    so3(So3, -8, 1, null, true),
//    le3(Le3, -9, 1, null, true),
//    la3(La3, -10, 1, null, true),
//    se3(Se3, -11, 1, null, true),
//    si3(Si3, -12, 1, null, true),
//    do4(Do4, -13, 1, null, true),
//    ra4(Ra4, -14, 1, null, true),
//    re4(Re4, -15, 1, null, true),
//    me4(Me4, -16, 1, null, true),
//    mi4(Mi4, -17, 1,null, true),
//    fa4(Fa4, -18, 1, null, true),
//    m01(Fi3, -1, null, false),
//    m02(So3, -2, null, false),
//    m03(Le3, -3, null, false),
//    m04(La3, -4, null, false),
//    m05(Se3, -5, null, false),
//    m06(Si3, -6, null, false),
//    m07(null, -7, null, false),
    Q(Si3, KeyEvent.VK_Q, 1, null),
    A(Do4, KeyEvent.VK_A, 2, "a"),
    W(Ra4, KeyEvent.VK_W, 1, "w"),
    S(Re4, KeyEvent.VK_S, 2, "s"),
    E(Me4, KeyEvent.VK_E, 1, "e"),
    D(Mi4, KeyEvent.VK_D, 2, "d"),
    R(null, KeyEvent.VK_R, 1, null), //fixme: Micro-tonal "fleh" and "floh"
    F(Fa4, KeyEvent.VK_F, 2, "f"),
    T(Fi4, KeyEvent.VK_T, 1, "t"),
    G(So4, KeyEvent.VK_G, 2, "g"),
    Y(Le4, KeyEvent.VK_Y, 1, "y"),
    H(La4, KeyEvent.VK_H, 2, "h"),
    U(Se4, KeyEvent.VK_U, 1, "u"),
    J(Si4, KeyEvent.VK_J, 2, "j"),
    I(null, KeyEvent.VK_I, 1, null),
    K(Do5, KeyEvent.VK_K, 2, "k"),
    O(Ra5, KeyEvent.VK_O, 1, "o"),
    L(Re5, KeyEvent.VK_L, 2, "l"),
    P(Me5, KeyEvent.VK_P, 1, "p"),
    SEMICOLON(Mi5, KeyEvent.VK_SEMICOLON, 2, ";"),
    QUOTE(Fa5, KeyEvent.VK_QUOTE, 2, "\""),
    CLOSE_BRACKET(Fi5, KeyEvent.VK_CLOSE_BRACKET, 1, null),
//    OPEN_BRACKET(Fi4, KeyEvent.VK_OPEN_BRACKET, null, false),
//    OPEN_BRACKET(Fi5, KeyEvent.VK_CLOSE_BRACKET, 1, null),
    BACK_SLASH(So5, KeyEvent.VK_BACK_SLASH, 1, null),
    ;

    public final Pitch pitch;
    public final int keyEventCode;
    public final int row;
    public final String label;

    Button(Pitch pitch, int keyEventCode, int row, String label) {
        this.pitch = pitch;
        this.keyEventCode = keyEventCode;
        this.row = row;
        this.label = label;
    }

}