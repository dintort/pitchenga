package com.pitchenga;

import static com.pitchenga.Interval.*;
import static com.pitchenga.Pitch.*;

public enum Fugue {

    Fi(Fi4, Fi3, i8, So3, i16, Do4, i4),
    So(So3, So3, i8, La3, i16, Si3, i16, Do4, i4),
    Le(Le3, Le3, i8, So3, i16, Do4, i4),
    La(La3, La3, i8, Si3, i16, Do4, i4),
    Se(Se3, Se3, i8, Do4, i4),
    Si(Si3, Si3, i8, Do4, i4),
    Do(Do4, Do4, i8, Do4, i8, Do4, i4),
    Ra(Ra4, Ra4, i8, Do4, i4),
    Re(Re4, Re4, i8, Do4, i4),
    Me(Me4, Me4, i8, Do4, i4),
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
