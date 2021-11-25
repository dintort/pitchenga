package com.pitchenga;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static com.pitchenga.Pitch.*;
import static com.pitchenga.Pitchenga.transposeScale;

public enum Riddler {
    //    FuguesOrdered("All fugues ordered",
//            new Pitch[][][]{{{Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, Fi3, Fi3, So3, Le3, La3, Se3, Si3, Do4,}}}, Pitchenga::ordered, null, null, new int[0]),
    FuguesOrdered("All fugues ordered",
            new Pitch[][][]{{Arrays.stream(Fugue.values()).map(fugue -> fugue.pitch).toArray(Pitch[]::new)}}, Pitchenga::ordered, null, null, new int[0], new String[]{"Do", "Ra", "Re", "Me", "Mi", "Fa", "Fi", "So", "Le", "La", "Se", "Si"}),
    ChromaticOneOctave("Chromatic - octave 4",
            new Pitch[][][]{{Pitchenga.CHROMATIC_SCALE}}, Pitchenga::shuffle, new Integer[0], null, new int[0], new String[]{"Do", "Ra", "Re", "Me", "Mi", "Fa", "Fi", "So", "Le", "La", "Se", "Si"}),
    Chromatic("Chromatic - main octaves",
            new Pitch[][][]{{Pitchenga.CHROMATIC_SCALE}}, Pitchenga::shuffle, null, null, new int[0], new String[]{"Do", "Ra", "Re", "Me", "Mi", "Fa", "Fi", "So", "Le", "La", "Se", "Si"}),
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
//    Step53("53", new Pitch[][][]{
//            multiply(new Pitch[]{Do2, Ra2, Ra2, Ra2, Me2, Fa2, Fi2, So2, So2, So2, La2, Si2, Do3,}, 500),
//            multiply(Sets.octavesDo2ToDo7, 500),
//            {{Non}},},
//            pitchenga -> pitchenga.shuffleGroupSeries(false, true), new Integer[0], null,
//            new int[]{0, 1, 0}),
    Step54("54", new Pitch[][][]{
            multiply(new Pitch[]{Do2, Ra2, Me2, Mi2, Mi2, Mi2, Mi2, Fa2, Fi2, So2, La2, Se2, Se2, Se2, Se2, Si2, Do3,}, 50),

            multiply(Scale.octavesDo2ToDo7, 500),
            {{Non}},},
            pitchenga -> pitchenga.shuffleGroupSeries(false, true), new Integer[0], null,
            //fixme: Insturments
            new int[]{0, 0, 0}, new String[]{"Do", "Ra", "Re", "Me", "Mi", "Fa", "Fi", "So", "Le", "La", "Se", "Si"}),
    BassOct2MiSe(null, new Pitch[][][]{
            multiply(new Pitch[]{Do2, Ra2, Me2, Mi2, Mi2, Mi2, Mi2, Fa2, Fi2, So2, La2, Se2, Se2, Se2, Se2, Si2, Do3,}, 400),
            multiply(transposeScale(Scale.Do3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.So3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Re3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.La3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Mi3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Si3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Fi3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Ra3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Le3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Me3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Se3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Fa3Maj.getScale(), -1, 0), 50),
            multiply(Scale.octavesDo2ToDo7, 300),
            {{Non}},},
            pitchenga -> pitchenga.shuffleGroupSeries(false, true), new Integer[0], null,
            new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
            new String[]{"Mi/Se", "Do", "So", "Re", "La", "Mi", "Si", "Fi", "Ra", "Le", "Me", "Se", "Fa", "Piano", "End"}),
    //fixme multi-oct voice hints
    BassOct2ReLe(null, new Pitch[][][]{
            multiply(new Pitch[]{Do2, Ra2, Re2, Re2, Re2, Re2, Re2, Re2, Me2, Mi2, Fa2, Fi2, So2, Le2, Le2, Le2, Le2, Le2, Le2, La2, Se2, Si2, Do3,}, 400),
            multiply(transposeScale(Scale.Do3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.So3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Re3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.La3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Mi3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Si3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Fi3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Ra3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Le3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Me3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Se3Maj.getScale(), -1, 0), 50),
            multiply(transposeScale(Scale.Fa3Maj.getScale(), -1, 0), 50),
            Scale.DoMajWide,
            Scale.SoMajWide,
            Scale.ReMajWide,
            Scale.LaMajWide,
            Scale.MiMajWide,
            Scale.SiMajWide,
            Scale.FiMajWide,
            Scale.RaMajWide,
            Scale.LeMajWide,
            Scale.MeMajWide,
            Scale.SeMajWide,
            Scale.FaMajWide,
            {{Non}},},
            pitchenga -> pitchenga.shuffleGroupSeries(false, true), new Integer[0], null,
            new int[]{0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            new String[]{"Re/Le",
                    "Do", "So", "Re", "La", "Mi", "Si", "Fi", "Ra", "Le", "Me", "Se", "Fa",
                    "Do", "So", "Re", "La", "Mi", "Si", "Fi", "Ra", "Le", "Me", "Se", "Fa", "End"}),
    ;

    public final String[] messages;

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

    Riddler(String nameOverride, Pitch[][][] scale, Function<Pitchenga, List<Pitch>> riddle, Integer[] octaves, Hinter hinter, int[] instruments, String[] messages) {
        if (nameOverride == null) {
            nameOverride = name();
        }
        this.name = nameOverride;
        this.scale = scale;
        this.riddle = riddle;
        this.octaves = octaves;
        this.hinter = hinter;
        this.instruments = instruments;
        this.messages = messages;
    }

    public String toString() {
        return name;
    }

    private static class Sets {
    }
}