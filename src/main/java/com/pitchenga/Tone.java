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

    Do("C", true, new Color(255, 0, 0), Color.WHITE),
    Ra("Db", false, new Color(127, 42, 0), Color.WHITE),
    Re("D", true, new Color(255, 127, 0), Color.WHITE),
    Me("Eb", false, new Color(171, 127, 0), Color.WHITE),
    Mi("E", true, new Color(255, 255, 0), Color.WHITE),
    Fa("F", true, new Color(0, 255, 0), Color.WHITE),
    Fi("F#", false, new Color(0, 127, 127), Color.WHITE),
    So("G", true, new Color(0, 255, 255), Color.WHITE),
    Le("Ab", false, new Color(0, 63, 127), Color.WHITE),
    La("A", true, new Color(0, 0, 255), Color.WHITE),
    Se("Bb", false, new Color(127, 0, 255), Color.WHITE),
    Si("B", true, new Color(255, 0, 255), Color.WHITE),
    ;

    public final String note;
    public final boolean diatonic;
    public final Color color;
    public final Color fontColor;
    public final String label;
    private volatile Button button;
    private volatile Fugue fugue;

    Tone(String note, boolean diatonic, Color color, Color fontColor) {
        this.note = note;
        this.diatonic = diatonic;
        this.color = color;
        this.fontColor = fontColor;
        this.label = " " + name().toLowerCase() + " ";
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

    public Button getButton() {
        if (this.button == null) {
            Button[] buttons = Button.values();
            for (int i = buttons.length - 1; i >= 0; i--) {
                Button aButton = buttons[i];
                if (aButton.pitch != null && aButton.pitch.tone == this) {
                    this.button = aButton;
                    return aButton;
                }
            }
        } else {
            return this.button;
        }
        throw new IllegalArgumentException("Button not found for=" + this);
    }

}