package com.pitchenga;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static com.pitchenga.Pitch.*;

public enum Riddler {
    //    FuguesOrdered("All fugues ordered",
//            new Pitch[][][]{{{Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, Fi3, Fi3, So3, Le3, La3, Se3, Si3, Do4,}}}, Pitchenga::ordered, null, null, new int[0]),
    FuguesOrdered("All fugues ordered",
            new Pitch[][][]{{Arrays.stream(Fugue.values()).map(fugue -> fugue.pitch).toArray(Pitch[]::new)}}, Pitchenga::ordered, null, null, new int[0]),
    ChromaticOneOctave("Chromatic - octave 4",
            new Pitch[][][]{{Pitchenga.CHROMATIC_SCALE}}, Pitchenga::shuffle, new Integer[0], null, new int[0]),
    Chromatic("Chromatic - main octaves",
            new Pitch[][][]{{Pitchenga.CHROMATIC_SCALE}}, Pitchenga::shuffle, null, null, new int[0]),
    //    Step51("51", new Pitch[][][]{
//            multiply(new Pitch[]{Do2, Me2, Fi2, La2, Do3,}, 110),
//            multiply(Sets.octavesDo2ToDo7, 10),
//            {{Non}},},
//            pitchenga -> pitchenga.shuffleGroupSeries(false, true), new Integer[0], null,
//            new int[]{0, 1, 0}),
//    Step52("52", new Pitch[][][]{
//            multiply(new Pitch[]{Do2, Me2, Fa2, Fa2, Fa2, Fi2, Fi2, Fi2, La2, Si2, Si2, Si2, Si2, Do3,}, 60),
//            multiply(Sets.octavesDo2ToDo7, 9),
//            {{Non}},},
//            pitchenga -> pitchenga.shuffleGroupSeries(false, true), new Integer[0], null,
//            new int[]{0, 1, 0}),
    Step53("53", new Pitch[][][]{
            multiply(new Pitch[]{Do2, Ra2, Ra2, Ra2, Me2, Fa2, Fi2, So2, So2, So2, La2, Si2, Do3,}, 500),
            multiply(Sets.octavesDo2ToDo7, 500),
            {{Non}},},
            pitchenga -> pitchenga.shuffleGroupSeries(false, true), new Integer[0], null,
            new int[]{0, 1, 0}),
    ;

    @SuppressWarnings("SameParameterValue")
    private static Pitch[][] multiply(Pitch[] pitches, int count) {
        int times = count / pitches.length;
        Pitch[][] result = new Pitch[times][];
        Arrays.fill(result, pitches);
        return result;
    }

    @SuppressWarnings("SameParameterValue")
    private static Pitch[][] multiply(Pitch[][] rows, int count) {
        int size = 0;
        List<Pitch[]> result = new LinkedList<>();
        while (size <= count) {
            for (Pitch[] row : rows) {
                size += row.length;
            }
            Collections.addAll(result, rows);
        }
         return result.toArray(new Pitch[0][]);
    }

    private final String name;
    public final Pitch[][][] scale;
    public final Function<Pitchenga, List<Pitch>> riddle;
    public final Integer[] octaves;
    public final Hinter hinter;
    public final int[] instruments;

    Riddler(String name, Pitch[][][] scale, Function<Pitchenga, List<Pitch>> riddle, Integer[] octaves, Hinter hinter, int[] instruments) {
        this.name = name;
        this.scale = scale;
        this.riddle = riddle;
        this.octaves = octaves;
        this.hinter = hinter;
        this.instruments = instruments;
    }

    public String toString() {
        return name;
    }

    private static class Sets {
        private static final Pitch[][] octavesDo2ToDo7 = {
                {Do2, Ra2, Re2, Me2, Mi2, Fa2, Fi2, So2, Le2, La2, Se2, Si2,},
                {Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3,},
                {Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4, Se4, Si4,},
                {Do5, Ra5, Re5, Me5, Mi5, Fa5, Fi5, So5, Le5, La5, Se5, Si5,},
                {Do6, Ra6, Re6, Me6, Mi6, Fa6, Fi6, So6, Le6, La6, Se6, Si6, Do7,},
        };
    }
}