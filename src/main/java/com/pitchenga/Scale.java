package com.pitchenga;

public enum Scale {

    DoMaj(true, true, new Pitch[]{}),
    DoMin(false, false, new Pitch[]{}),
    RaMaj(true, false, new Pitch[]{}),
    RaMin(false, false, new Pitch[]{}),
    ReMaj(true, false, new Pitch[]{}),
    ReMin(false, true, new Pitch[]{}),
    MeMaj(true, false, new Pitch[]{}),
    MeMin(false, false, new Pitch[]{}),
    MiMaj(true, false, new Pitch[]{}),
    MiMin(false, true, new Pitch[]{}),
    FaMaj(true, true, new Pitch[]{}),
    FaMin(false, false, new Pitch[]{}),
    FiMaj(true, false, new Pitch[]{}),
    FiMin(false, false, new Pitch[]{}),
    SoMaj(true, true, new Pitch[]{}),
    SoMin(false, false, new Pitch[]{}),
    LeMaj(true, false, new Pitch[]{}),
    LeMin(false, false, new Pitch[]{}),
    LaMaj(true, false, new Pitch[]{}),
    LaMin(false, true, new Pitch[]{}),
    SeMaj(true, false, new Pitch[]{}),
    SeMin(false, false, new Pitch[]{}),
    SiMaj(true, false, new Pitch[]{}),
    SiMin(false, true, new Pitch[]{}),
    ;


    private final boolean major;
    private final boolean primary;
    private final Pitch[] scale;

    Scale(boolean major, boolean primary, Pitch[] scale) {
        this.major = major;
        this.primary = primary;
        this.scale = scale;
    }

}