package com.pitchenga;

import java.awt.event.KeyEvent;

/**
 * Note names are based on https://en.wikipedia.org/wiki/Solfege#Chromatic_variants and http://openmusictheory.com/chromaticSolfege.html
 * major: do di re ri mi fa fi so se la li si
 * minor: do di re me mi fa fi so le la se si
 * lowrd: do ra re me mi fa se so le la se si - this one, but F# instead of Gb
 * <p>
 * Colors are roughly based on https://www.nature.com/articles/s41598-017-18150-y/figures/2 but only using round numbers (chromesthesia.png).
 */
public enum Key {

//        case KeyEvent.VK_Q:           handleKey(, pressed);
//        case KeyEvent.VK_A:           handleKey(Pitch.Do3, pressed);
//        case KeyEvent.VK_W:           handleKey(Pitch.Ra3, pressed);
//        case KeyEvent.VK_S:            handleKey(Pitch.Re3, pressed);
//        case KeyEvent.VK_E:            handleKey(Pitch.Me3, pressed);
//        case KeyEvent.VK_D:            handleKey(Pitch.Mi3, pressed);
//        case KeyEvent.VK_R:            handleKey(, pressed);
//        case KeyEvent.VK_F:            handleKey(Pitch.Fa3, pressed);
//        case KeyEvent.VK_T:            handleKey(Pitch.Fi3, pressed);
//        case KeyEvent.VK_G:            handleKey(Pitch.So3, pressed);
//        case KeyEvent.VK_Y:            handleKey(Pitch.Le3, pressed);
//        case KeyEvent.VK_H:            handleKey(Pitch.La3, pressed);
//        case KeyEvent.VK_U:            handleKey(Pitch.Se3, pressed);
//        case KeyEvent.VK_I:            handleKey(, pressed);
//        case KeyEvent.VK_J:            handleKey(Pitch.Si3, pressed);
//        case KeyEvent.VK_K:            handleKey(Pitch.Do4, pressed);
//        case KeyEvent.VK_O:            handleKey(Pitch.Ra4, pressed);
//        case KeyEvent.VK_L:            handleKey(Pitch.Re4, pressed);
//        case KeyEvent.VK_P:            handleKey(Pitch.Me4, pressed);
//        case KeyEvent.VK_SEMICOLON:    handleKey(Pitch.Mi4, pressed);
//        case KeyEvent.VK_QUOTE:        handleKey(Pitch.Fa4, pressed);


    //fixme: +Chromatic layout on 1234..90-= and z x c v rows
    //fixme: +Diatonic keyboard layout with sharps via shift
    //fixme: +Micro-tones on the top and the bottom rows
    //fixme: +Fret-less sliding up and down on shift and control
    //fixme: +Toggleable A B C D E etc button labels
    //fixme: +Num-pad mapping
    n01(Pitch.Fi3, KeyEvent.VK_1, "1", true),
    n02(Pitch.So3, KeyEvent.VK_2, "2", true),
    n03(Pitch.Le3, KeyEvent.VK_3, "3", true),
    n04(Pitch.La3, KeyEvent.VK_4, "4", true),
    n05(Pitch.Se3, KeyEvent.VK_5, "5", true),
    n06(Pitch.Si3, KeyEvent.VK_6, "6", true),
    n07(Pitch.Do4, KeyEvent.VK_7, "7", true),
    n08(Pitch.Ra4, KeyEvent.VK_8, "8", true),
    n09(Pitch.Re4, KeyEvent.VK_9, "9", true),
    n10(Pitch.Me4, KeyEvent.VK_0, "0", true),
    n11(Pitch.Mi4, KeyEvent.VK_MINUS, "-", true),
    n12(Pitch.Fa4, KeyEvent.VK_EQUALS, "=", true),

    m01(Pitch.Fi3, -1, null, false),
    m02(Pitch.So3, -2, null, false),
    m03(Pitch.Le3, -3, null, false),
    m04(Pitch.La3, -4, null, false),
    m05(Pitch.Se3, -5, null, false),
    m06(Pitch.Si3, -6, null, false),
    m07(null, -7, null, false),
    A(Pitch.Do3, KeyEvent.VK_A, "a", false),
    W(Pitch.Ra3, KeyEvent.VK_W, "w", false),
    S(Pitch.Re3, KeyEvent.VK_S, "s", false),
    E(Pitch.Me3, KeyEvent.VK_E, "e", false),
    D(Pitch.Mi3, KeyEvent.VK_D, "d", false),
    R(null, KeyEvent.VK_R, null, false),
    F(Pitch.Fa3, KeyEvent.VK_F, "f", false),
    T(Pitch.Fi3, KeyEvent.VK_T, "t", false),
    G(Pitch.So3, KeyEvent.VK_G, "g", false),
    Y(Pitch.Le3, KeyEvent.VK_Y, "y", false),
    H(Pitch.La3, KeyEvent.VK_H, "h", false),
    U(Pitch.Se3, KeyEvent.VK_U, "u", false),
    I(null, KeyEvent.VK_I, null, false),
    J(Pitch.Si3, KeyEvent.VK_J, "j", false),
    K(Pitch.Do4, KeyEvent.VK_K, "k", false),
    O(Pitch.Ra4, KeyEvent.VK_O, "o", false),
    L(Pitch.Re4, KeyEvent.VK_L, "l", false),
    P(Pitch.Me4, KeyEvent.VK_P, "p", false),
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
