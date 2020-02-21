package com.pitchenga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.pitchenga.Duration.*;
import static com.pitchenga.Pitch.*;

public enum Fugue {

    Fi(Fi3, eight, sixteen, So3, sixteen, Do4, four),
    So(So3, eight, La3, sixteen, Si3, sixteen, Do4, four),
    Le(Le3, four, sixteen, So3, sixteen, Do4, four),
    La(La3, four, Si3, sixteen, Do4, four),
    Se(Se3, four, sixteen, Do4, four),
    Si(Si3, four, Do4, four),
    Do(Do4, eight, Do4, eight, Do4, four),
    Ra(Ra4, four, Do4, four),
    Re(Re4, four, Do4, four),
    Me(Me4, four, sixteen, Do4, four),
    Mi(Mi4, four, Re4, sixteen, Do4, four),
    Fa(Fa4, eight, Mi4, sixteen, Re4, sixteen, Do4, four),
    ;

    public final Pitch pitch;
    public final Object[] tune;

    Fugue(Pitch pitch, Object... coda) {
        this.pitch = pitch;
        List<Object> shortTune = new ArrayList<>(coda.length + 1);
        shortTune.add(pitch);
        shortTune.addAll(Arrays.asList(coda));
        this.tune = shortTune.toArray();
    }

}
