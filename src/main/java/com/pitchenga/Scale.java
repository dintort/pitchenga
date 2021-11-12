package com.pitchenga;

import static com.pitchenga.Pitch.*;

public enum Scale {

    Do3Maj(true, true, new Pitch[]{Do3, Re3, Mi3, Fa3, So3, La3, Si3, Do3,}),
    Ra3Maj(true, false, new Pitch[]{Ra3, Me3, Fa3, Fi3, Le3, Se3, Do4, Ra3,}),
    Re3Maj(true, false, new Pitch[]{Re3, Mi3, Fi3, So3, La3, Si3, Ra3, Re3,}),
    Me3Maj(true, false, new Pitch[]{Me3, Fa3, So3, Le3, Se3, Do4, Re3, Me3,}),
    Mi3Maj(true, false, new Pitch[]{Mi3, Fi3, Le3, La3, Si3, Ra3, Me3, Mi3,}),
    Fa3Maj(true, true, new Pitch[]{Fa3, So3, La3, Se3, Do4, Re3, Mi3, Fa3,}),
    Fi3Maj(true, false, new Pitch[]{Fi3, Le3, Se3, Si3, Ra3, Me3, Fa3, Fi3,}),
    So3Maj(true, true, new Pitch[]{So3, La3, Si3, Do4, Re3, Mi3, Fi3, So3,}),
    Le3Maj(true, false, new Pitch[]{Le3, Se3, Do4, Ra3, Me3, Fa3, So3, Le3,}),
    La3Maj(true, false, new Pitch[]{La3, Si3, Ra3, Re3, Mi3, Fi3, Le3, La3,}),
    Se3Maj(true, false, new Pitch[]{Se3, Do4, Re3, Me3, Fa3, So3, La3, Se3,}),
    Si3Maj(true, false, new Pitch[]{Si3, Ra3, Me3, Mi3, Fi3, Le3, Se3, Si3,}),
    ;

    private final boolean major;
    private final boolean primary;
    private final Pitch[] scale;

    Scale(boolean major, boolean primary, Pitch[] scale) {
        this.major = major;
        this.primary = primary;
        this.scale = scale;
    }

    public Pitch[] getScale() {
        return scale;
    }

    public boolean isMajor() {
        return major;
    }

    public boolean isPrimary() {
        return primary;
    }
}