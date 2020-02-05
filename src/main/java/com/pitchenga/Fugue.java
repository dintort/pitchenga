package com.pitchenga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.pitchenga.Interval.*;
import static com.pitchenga.Pitch.*;

public enum Fugue {

    Fi(Fi3, i8, i16, So3, i16, Do4, i4),
    So(So3, i8, La3, i16, Si3, i16, Do4, i4),
    Le(Le3, i4, i16, So3, i16, Do4, i4),
    La(La3, i4, Si3, i16, Do4, i4),
    Se(Se3, i4, i16, Do4, i4),
    Si(Si3, i4, Do4, i4),
    Do(Do4, i8, Do4, i8, Do4, i4),
    Ra(Ra4, i4, Do4, i4),
    Re(Re4, i4, Do4, i4),
    Me(Me4, i4, i16, Do4, i4),
    Mi(Mi4, i4, Re4, i16, Do4, i4),
    Fa(Fa4, i8, Mi4, i16, Re4, i16, Do4, i4),
    ;

    private final Pitch pitch;
    private final Object[] tune;

    Fugue(Pitch pitch, Object... coda) {
        this.pitch = pitch;
        List<Object> shortTune = new ArrayList<>(coda.length + 1);
        shortTune.add(pitch);
        shortTune.addAll(Arrays.asList(coda));
        this.tune = shortTune.toArray();
    }

    public Pitch getPitch() {
        return pitch;
    }

    public Object[] getTune() {
        return tune;
    }

}
