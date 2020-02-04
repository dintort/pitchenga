package com.pitchenga;

import java.awt.*;

/**
 * Tone names are roughly based on https://en.wikipedia.org/wiki/Solfege#Chromatic_variants and http://openmusictheory.com/chromaticSolfege.html
 * but modified so that each tone can be represented by a unique letter: "darem-fisolut", where diatonic tones are consonants and sharps are vowels:
 *  a e   i o u
 * d r m f s l t
 * Colors are roughly based on https://www.nature.com/articles/s41598-017-18150-y/figures/2 but only using round numbers (circle.png).
 */
public enum Tone {

    fI("i", "F#", false, new Color(0, 127, 127)),
    So("s", "G", true, new Color(0, 255, 255)),
    lO("o", "Ab", false, new Color(0, 0, 127)),
    La("l", "A", true, new Color(0, 0, 255)),
    tU("u", "Bb", false, new Color(127, 0, 255)),
    Ti("t", "B", true, new Color(255, 0, 255)),
    Do("d", "C", true, new Color(255, 0, 0)),
    rA("a", "Db", false, new Color(127, 0, 0)),
    Re("r", "D", true, new Color(255, 127, 0)),
    mE("e", "Eb", false, new Color(127, 127, 0)),
    Mi("m", "E", true, new Color(255, 255, 0)),
    Fa("f", "F", true, new Color(0, 255, 0));

    private final String letter;
    private final String western;
    private final boolean diatonic;
    private final Color color;
    private final String spacedName;
    private Fugue fugue;

    Tone(String letter, String western, boolean diatonic, Color color) {
        this.letter = letter;
        this.western = western;
        this.diatonic = diatonic;
        this.color = color;
        this.spacedName = " " + name() + " ";
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

    public String getWestern() {
        return western;
    }

    public String getLetter() {
        return letter;
    }
}
