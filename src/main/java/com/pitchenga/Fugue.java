package com.pitchenga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.pitchenga.Interval.*;
import static com.pitchenga.Pitch.*;

public enum Fugue {

    Fi(Fi3, eit, sxt, So3, sxt, Do4, frt),
    So(So3, eit, La3, sxt, Si3, sxt, Do4, frt),
    Le(Le3, frt, sxt, So3, sxt, Do4, frt),
    La(La3, frt, Si3, sxt, Do4, frt),
    Se(Se3, frt, sxt, Do4, frt),
    Si(Si3, frt, Do4, frt),
    Do(Do4, eit, Do4, eit, Do4, frt),
    Ra(Ra4, frt, Do4, frt),
    Re(Re4, frt, Do4, frt),
    Me(Me4, frt, sxt, Do4, frt),
    Mi(Mi4, frt, Re4, sxt, Do4, frt),
    Fa(Fa4, eit, Mi4, sxt, Re4, sxt, Do4, frt),
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
