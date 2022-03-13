package com.pitchenga;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static com.pitchenga.Pitch.*;
import static com.pitchenga.Pitchenga.CHROMATIC_SCALE;
import static com.pitchenga.Pitchenga.transposeScale;

public enum Riddler {
    //    FuguesOrdered("All fugues ordered",
//            new Pitch[][][]{{{Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4,π}}}, Pitchenga::ordered, null, null, new int[0]),
    Rec("Rec", new Pitch[][][]{{{Mi2, Fa2, Fi2, So2, Le2, La2, Se2, Si2, Do3, Ra3, Re3, Me3, Mi3, Fa3, Fi3, So3, Le3, La3, Se3, Si3, Do4, Do4, Ra4, Re4, Me4, Mi4, Fa4, Fi4, So4, Le4, La4,}}},
            Pitchenga::ordered, null, null, new int[0], new String[]{"Do", "Ra", "Re", "Me", "Mi", "Fa", "Fi", "So", "Le", "La", "Se", "Si"}),
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
            multiply(50, new Pitch[]{Do2, Ra2, Me2, Mi2, Mi2, Mi2, Mi2, Fa2, Fi2, So2, La2, Se2, Se2, Se2, Se2, Si2, Do3,}),
            multiply(500, Scale.octavesDo2ToDo7),
            {{Non}},},
            pitchenga -> pitchenga.shuffleGroupSeries(false, true), new Integer[0], null,
            //fixme: Insturments
            new int[]{0, 0, 0}, new String[]{"Do", "Ra", "Re", "Me", "Mi", "Fa", "Fi", "So", "Le", "La", "Se", "Si"}),
    BassOct2MiSe(null, new Pitch[][][]{
            multiply(400, new Pitch[]{Do2, Ra2, Me2, Mi2, Mi2, Mi2, Mi2, Fa2, Fi2, So2, La2, Se2, Se2, Se2, Se2, Si2, Do3,}),
            multiply(50, transposeScale(Scale.Do3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.So3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Re3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.La3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Mi3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Si3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Fi3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Ra3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Le3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Me3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Se3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Fa3Maj.getScale(), -1, 0)),
            multiply(300, Scale.octavesDo2ToDo7),
            {{Non}},},
            pitchenga -> pitchenga.shuffleGroupSeries(false, true), new Integer[0], null,
            new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
            new String[]{"Mi/Se", "Do", "So", "Re", "La", "Mi", "Si", "Fi", "Ra", "Le", "Me", "Se", "Fa", "Piano", "End"}),
    //fixme multi-oct voice hints
    BassOct2ReLe(null, new Pitch[][][]{
            multiply(400, new Pitch[]{Do2, Ra2, Re2, Re2, Re2, Re2, Re2, Re2, Me2, Mi2, Fa2, Fi2, So2, Le2, Le2, Le2, Le2, Le2, Le2, La2, Se2, Si2, Do3,}),
            multiply(50, transposeScale(Scale.Do3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.So3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Re3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.La3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Mi3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Si3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Fi3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Ra3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Le3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Me3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Se3Maj.getScale(), -1, 0)),
            multiply(50, transposeScale(Scale.Fa3Maj.getScale(), -1, 0)),
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
    BassAll(null, new Pitch[][][]{
            merge(70, transposeScale(Scale.Do3Maj.getScale(), 0, 0), new Pitch[]{Si3, Si3, Si3}),
            merge(30, transposeScale(Scale.Do3Maj.getScale(), -1, 0)),
            Scale.DoMajWide,
            merge(70, transposeScale(Scale.So3Maj.getScale(), 0, 0), new Pitch[]{Fi3, Fi3, Fi3}),
            merge(30, transposeScale(Scale.So3Maj.getScale(), -1, 0)),
            Scale.SoMajWide,
            merge(70, transposeScale(Scale.Re3Maj.getScale(), 0, 0), new Pitch[]{Ra3, Ra3, Ra3}),
            merge(30, transposeScale(Scale.Re3Maj.getScale(), -1, 0)),
            Scale.ReMajWide,
            merge(70, transposeScale(Scale.La3Maj.getScale(), 0, 0), new Pitch[]{Le3, Le3, Le3}),
            merge(30, transposeScale(Scale.La3Maj.getScale(), -1, 0)),
            Scale.LaMajWide,
            merge(70, transposeScale(Scale.Mi3Maj.getScale(), 0, 0), new Pitch[]{Me3, Me3, Me3}),
            merge(30, transposeScale(Scale.Mi3Maj.getScale(), -1, 0)),
            Scale.MiMajWide,
            merge(70, transposeScale(Scale.Si3Maj.getScale(), 0, 0), new Pitch[]{Se3, Se3, Se3}),
            merge(30, transposeScale(Scale.Si3Maj.getScale(), -1, 0)),
            Scale.SiMajWide,
            merge(70, transposeScale(Scale.Fi3Maj.getScale(), 0, 0), new Pitch[]{Fa3, Fa3, Fa3}),
            merge(30, transposeScale(Scale.Fi3Maj.getScale(), -1, 0)),
            Scale.FiMajWide,
            merge(70, transposeScale(Scale.Ra3Maj.getScale(), 0, 0), new Pitch[]{Do3, Do3, Do3}),
            merge(30, transposeScale(Scale.Ra3Maj.getScale(), -1, 0)),
            Scale.RaMajWide,
            merge(70, transposeScale(Scale.Le3Maj.getScale(), 0, 0), new Pitch[]{So3, So3, So3}),
            merge(30, transposeScale(Scale.Le3Maj.getScale(), -1, 0)),
            Scale.LeMajWide,
            merge(70, transposeScale(Scale.Me3Maj.getScale(), 0, 0), new Pitch[]{Re3, Re3, Re3}),
            merge(30, transposeScale(Scale.Me3Maj.getScale(), -1, 0)),
            Scale.MeMajWide,
            merge(70, transposeScale(Scale.Se3Maj.getScale(), 0, 0), new Pitch[]{La3, La3, La3}),
            merge(30, transposeScale(Scale.Se3Maj.getScale(), -1, 0)),
            Scale.SeMajWide,
            merge(70, transposeScale(Scale.Fa3Maj.getScale(), 0, 0), new Pitch[]{Mi3, Mi3, Mi3}),
            merge(30, transposeScale(Scale.Fa3Maj.getScale(), -1, 0)),
            Scale.FaMajWide,
            {{Non}},},
            pitchenga -> pitchenga.shuffleGroupSeries(false, true), new Integer[0], null,
            //fixme: Use macro groups and remove duplication
            new int[]{0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1},
            new String[]{"Do", "Do", "Do", "So", "So", "So", "Re", "Re", "Re", "La", "La", "La", "Mi", "Mi", "Mi", "Si", "Si", "Si", "Fi", "Fi", "Fi", "Ra", "Ra", "Ra", "Le", "Le", "Le", "Me", "Me", "Me", "Se", "Se", "Se", "Fa", "Fa", "Fa", "End",}),
    BassScales(null, new Pitch[][][]{
            multiply(500, new Pitch[]{Do1, Me1, Me1, Fi1, Fi1, La1, La1, Do2,}),
//            multiply(500, new Pitch[]{Do1, Me1, Fi1, La1, Do2,}),
            multiply(50, transposeScale(Scale.Do3Maj.getScale(), -1, 0)),
            Scale.DoMajWide,
            multiply(50, transposeScale(Scale.So3Maj.getScale(), -1, 0)),
            Scale.SoMajWide,
            multiply(50, transposeScale(Scale.Re3Maj.getScale(), -1, 0)),
            Scale.ReMajWide,
            multiply(50, transposeScale(Scale.La3Maj.getScale(), -1, 0)),
            Scale.LaMajWide,
            multiply(50, transposeScale(Scale.Mi3Maj.getScale(), -1, 0)),
            Scale.MiMajWide,
            multiply(50, transposeScale(Scale.Si3Maj.getScale(), -1, 0)),
            Scale.SiMajWide,
            multiply(50, transposeScale(Scale.Fi3Maj.getScale(), -1, 0)),
            Scale.FiMajWide,
            multiply(50, transposeScale(Scale.Ra3Maj.getScale(), -1, 0)),
            Scale.RaMajWide,
            multiply(50, transposeScale(Scale.Le3Maj.getScale(), -1, 0)),
            Scale.LeMajWide,
            multiply(50, transposeScale(Scale.Me3Maj.getScale(), -1, 0)),
            Scale.MeMajWide,
            multiply(50, transposeScale(Scale.Se3Maj.getScale(), -1, 0)),
            Scale.SeMajWide,
            multiply(50, transposeScale(Scale.Fa3Maj.getScale(), -1, 0)),
            Scale.FaMajWide,
            {{Non}},},
            pitchenga -> pitchenga.shuffleGroupSeries(false, true), new Integer[0], null,
            new int[]{0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0,},
            new String[]{"Bass", "Do", "Do", "So", "So", "Re", "Re", "La", "La", "Mi", "Mi", "Si", "Si", "Fi", "Fi", "Ra", "Ra", "Le", "Le", "Me", "Me", "Se", "Se", "Fa", "Fa", "End",}),
    Bass(null, new Pitch[][][]{
            multiply(600, new Pitch[]{Do1, Me1, Me1, Fi1, Fi1, La1, La1, Do2,}),
            multiply(300, transposeScale(CHROMATIC_SCALE, -1, 0)),
            multiply(300,Scale.octavesDo2ToDo7),
            {{Non}},},
            pitchenga -> pitchenga.shuffleGroupSeries(false, true), new Integer[0], null,
            new int[]{0, 0, 1, 0,},
            new String[]{"Bass octave 1", "Bass octave 2", "Piano", "End",}),
    ;

    public final String[] messages;

    @SuppressWarnings("SameParameterValue")
    private static Pitch[][] multiply(int count, Pitch[] pitches) {
        int times = count / pitches.length;
        Pitch[][] result = new Pitch[times][];
        Arrays.fill(result, pitches);
        return result;
    }

    @SuppressWarnings("SameParameterValue")
    private static Pitch[][] multiply(int count, Pitch[][] rows) {
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

    @SuppressWarnings("SameParameterValue")
    private static Pitch[][] merge(int count, Pitch[]... rows) {
        List<Pitch> merged = new LinkedList<>();
        for (Pitch[] row : rows) {
            merged.addAll(Arrays.asList(row));
        }
        return multiply(count, merged.toArray(new Pitch[0]));
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

}