package com.pitchenga;

import static com.pitchenga.Interval.*;
import static com.pitchenga.Pitch.*;

public enum Fugue {

    Fi(fI4, fI3, i8, So3, i16, Do4, i4),
    So(So3, So3, i8, La3, i16, Ti3, i16, Do4, i4),
    lO(lO3, lO3, i8, So3, i16, Do4, i4),
    La(La3, La3, i8, Ti3, i16, Do4, i4),
    tU(tU3, tU3, i8, Do4, i4),
    Ti(Ti3, Ti3, i8, Do4, i4),
    Do(Do4, Do4, i8, Do4, i8, Do4, i4),
    rA(rA4, rA4, i8, Do4, i4),
    Re(Re4, Re4, i8, Do4, i4),
    mE(mE4, mE4, i8, Do4, i4),
    Mi(Mi4, Mi4, i8, Re4, i16, Do4, i4),
    Fa(Fa4, Fa4, i8, Mi4, i16, Re4, i16, Do4, i4),
    ;

    private final Pitch pitch;
    private final Object[] tune;

    Fugue(Pitch pitch, Object... tune) {
        this.pitch = pitch;
        this.tune = tune;
    }

    public Pitch getPitch() {
        return pitch;
    }

    public Object[] getTune() {
        return tune;
    }

}
