package com.pitchenga;

import java.awt.*;

/**
 * Tone names are based on https://en.wikipedia.org/wiki/Solfege#Chromatic_variants and http://openmusictheory.com/chromaticSolfege.html
 * major: do di re ri mi fa fi so se la li si
 * minor: do di re me mi fa fi so le la se si
 * lowrd: do ra re me mi fa se so le la se si - this one, but F# instead of Gb
 * Colors are roughly based on Isaac Newton's color wheel and https://www.nature.com/articles/s41598-017-18150-y/figures/2 but only using round numbers (circle.png).
 */
public enum Tone {

    Do("C", true, new Color(255, 0, 0), Color.DARK_GRAY),
    Ra("Db", false, new Color(127, 0, 0), Color.DARK_GRAY),
    Re("D", true, new Color(255, 127, 0), Color.DARK_GRAY),
    Me("Eb", false, new Color(171, 127, 0), Color.DARK_GRAY),
    Mi("E", true, new Color(255, 255, 0), Color.DARK_GRAY),
    Fa("F", true, new Color(0, 255, 0), Color.DARK_GRAY),
    Fi("F#", false, new Color(0, 127, 127), Color.WHITE),
    So("G", true, new Color(0, 255, 255), Color.DARK_GRAY),
    Le("Ab", false, new Color(0, 0, 127), Color.DARK_GRAY),
    La("A", true, new Color(0, 0, 255), Color.DARK_GRAY),
    Te("Bb", false, new Color(127, 0, 255), Color.DARK_GRAY),
    Ti("B", true, new Color(255, 0, 255), Color.DARK_GRAY),
    ;

    public final String note;
    public final boolean diatonic;
    public final Color color;
    public final Color fontColor;
    public final String label;
    public volatile Fugue fugue;

    Tone(String note, boolean diatonic, Color color, Color fontColor) {
        this.note = note;
        this.diatonic = diatonic;
        this.color = color;
        this.fontColor = fontColor;
        this.label = " " + name() + " ";
    }

    public Fugue getFugue() {
        if (this.fugue == null) {
            for (Fugue aFugue : Fugue.values()) {
                if (aFugue.pitch.tone.equals(this)) {
                    this.fugue = aFugue;
                    return aFugue;
                }
            }
            throw new IllegalArgumentException("Fugue not found for=" + this);
        } else {
            return this.fugue;
        }
    }

    public Key getKey() {
        //fixme: Cache them same way as fugues
        Key[] keys = Key.values();
        for (int i = keys.length - 1; i >= 0; i--) {
            Key key = keys[i];
            if (key.pitch != null && key.pitch.tone == this) {
                return key;
            }
        }
        throw new IllegalArgumentException("Key not found for=" + this);
    }

}
