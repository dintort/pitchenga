package com.pitchenga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.pitchenga.Duration.*;
import static com.pitchenga.Pitch.*;

public enum Fugue {

    Do(Do4, eight, Do4, eight, Do4, four),
    Ra(Ra4, four, Do4, four),
    Re(Re4, four, Do4, four),
    Me(Me4, four, sixteen, Do4, four),
    Mi(Mi4, four, Re4, sixteen, Do4, four),
    Fa(Fa4, eight, Mi4, sixteen, Re4, sixteen, Do4, four),
    Fi(Fi4, eight, So4, sixteen, Do5, four),
    So(So4, eight, La4, sixteen, Si4, sixteen, Do5, four),
    Le(Le4, four, So4, sixteen, Do5, four),
    La(La4, four, Si4, sixteen, Do5, four),
    Se(Se4, four, sixteen, Do5, four),
    Si(Si4, four, Do5, four),
    DoDo(Do5, eight, Do5, eight, Do5, four),
    ;

    public final Pitch pitch;
    public final Object[] tune;
    public final Object[][] intervals ;

    Fugue(Pitch pitch, Object... coda) {
        this.pitch = pitch;
        List<Object> tune = new ArrayList<>(coda.length + 1);
        tune.add(pitch);
        tune.addAll(Arrays.asList(coda));
        this.tune = tune.toArray();
        Pitch theDo = (Pitch) coda[coda.length - 2];
        Tone[] tones = Tone.values();
        intervals = new Object[tones.length][];
        for (Tone tone : tones) {
            Object[] interval = new Object[4];
            this.intervals[tone.ordinal()] = interval;
            Pitch end = Pitchenga.transposePitch(theDo, 0, tone.ordinal());
            interval[0] = pitch;
            interval[1] = eight;
            interval[2] = end;
            interval[3] = eight;
        }
    }

}
