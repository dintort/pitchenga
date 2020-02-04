package com.pitchenga;

import java.awt.*;

/**
 * Note names are based on https://en.wikipedia.org/wiki/Solfege#Chromatic_variants and http://openmusictheory.com/chromaticSolfege.html
 * major: do di re ri mi fa fi so se la li si
 * minor: do di re me mi fa fi so le la se si
 * lowrd: do ra re me mi fa se so le la se si - this one, but F# instead of Gb
 * <p>
 * Colors are roughly based on https://www.nature.com/articles/s41598-017-18150-y/figures/2 but only using round numbers (chromesthesia.png).
 */
public enum Tone {

    //  a e   i u o
    // d r m f s l t
    // d a r e m f i s u l o t
    fI(false, new Color(0, 127, 127)),
    So(true, new Color(0, 255, 255)),
    lU(false, new Color(0, 0, 127)),
    La(true, new Color(0, 0, 255)),
    tO(false, new Color(127, 0, 255)),
    Ti(true, new Color(255, 0, 255)),
    Do(true, new Color(255, 0, 0)),
    rA(false, new Color(127, 0, 0)),
    Re(true, new Color(255, 127, 0)),
    mE(false, new Color(127, 127, 0)),
    Mi(true, new Color(255, 255, 0)),
    Fa(true, new Color(0, 255, 0));

    private final boolean diatonic;
    private final Color color;
    private final String spacedName;
    private Fugue fugue;

    Tone(boolean diatonic, Color color) {
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
                if (aFugue.name().equals(this.name())) {
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

}
