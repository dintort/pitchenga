package com.pitchenga;

import java.awt.*;

/**
 * Tone names are based on https://en.wikipedia.org/wiki/Solfege#Chromatic_variants and http://openmusictheory.com/chromaticSolfege.html
 * major: do di re ri mi fa fi so se la li si
 * minor: do di re me mi fa fi so le la se si
 * lowrd: do ra re me mi fa se so le la se si - this one, but F# instead of Gb
 * Colors are roughly based on https://www.nature.com/articles/s41598-017-18150-y/figures/2 but only using round numbers (circle.png).
 */
public enum Tone {

    Fi("F#", false, new Color(0, 127, 127)),
    So("G", true, new Color(0, 255, 255)),
    Le("Ab", false, new Color(0, 0, 127)),
    La("A", true, new Color(0, 0, 255)),
    Se("Bb", false, new Color(127, 0, 255)),
    Si("B", true, new Color(255, 0, 255)),
    Do("C", true, new Color(255, 0, 0)),
    Ra("Db", false, new Color(127, 0, 0)),
    Re("D", true, new Color(255, 127, 0)),
    Me("Eb", false, new Color(127, 127, 0)),
    Mi("E", true, new Color(255, 255, 0)),
    Fa("F", true, new Color(0, 255, 0));

    private final String note;
    private final boolean diatonic;
    private final Color color;
    private final String spacedName;
    private Fugue fugue;

    Tone(String note, boolean diatonic, Color color) {
        this.note = note;
        this.diatonic = diatonic;
        this.color = color;
        this.spacedName = " " + name().toLowerCase() + " ";
    }

    public Color getColor() {
        return color;
    }

    public boolean isDiatonic() {
        return diatonic;
    }

    public String getSpacedName() {
        return spacedName;
    }

    //fixme: Cache them i.e. move to a static map
    public Fugue getFugue() {
        Fugue fugue = this.fugue;
        if (fugue == null) {
            for (Fugue aFugue : Fugue.values()) {
                if (aFugue.getPitch().getTone().equals(this)) {
                    this.fugue = aFugue;
                    return aFugue;
                }
            }
            throw new IllegalArgumentException("Fugue not found for=" + this);
        } else {
            return fugue;
        }
    }

    public Key getKey() {
        Key[] keys = Key.values();
        for (int i = keys.length - 1; i >= 0; i--) {
            Key key = keys[i];
            if (key.getPitch() != null && key.getPitch().getTone() == this) {
                return key;
            }
        }
        throw new IllegalArgumentException("Key not found for=" + this);
    }

    public Object[] getTune() {
        return getFugue().getTune();
    }

    public Pitch getPitch() {
        return getFugue().getPitch();
    }

    public String getNote() {
        return note;
    }

}
