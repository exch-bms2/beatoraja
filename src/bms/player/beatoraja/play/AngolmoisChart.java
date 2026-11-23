package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.MineNote;
import bms.model.NormalNote;
import bms.model.TimeLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Comparator;
import java.util.HashSet;

/** Converts the arranged BMS lanes back from cumulative section lengths to C's measure positions. */
final class AngolmoisChart {
    private AngolmoisChart() {}

    static AngolmoisEngine create(BMSModel model) throws IOException {
        if (model.getBase() != 36) throw new IllegalArgumentException("Angolmois requires base-36 BMS");
        var info = model.getChartInformation();
        if (info == null || info.source == null || info.source.location().toLowerCase(Locale.ROOT).endsWith(".bmson")) {
            throw new IllegalArgumentException("Angolmois requires a BMS source");
        }
        String text;
        try (var stream = info.source.openStream()) {
            // All metadata read here is ASCII, irrespective of the title/artist encoding.
            text = new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
        double[] lengths = new double[1000];
        Arrays.fill(lengths, 1);
        int rank = 2, randomIndex = 0;
        var active = new ArrayList<String>();
        var random = new ArrayDeque<Integer>();
        var skip = new ArrayDeque<Boolean>();
        for (String raw : text.split("\\R")) {
            String line = raw.strip().toUpperCase(Locale.ROOT);
            if (!line.startsWith("#")) continue;
            String[] command = line.split("\\s+", 2);
            if (command[0].equals("#RANDOM")) {
                if (info.selectedRandoms == null || randomIndex >= info.selectedRandoms.length) {
                    throw new IllegalArgumentException("Missing selected BMS random branch");
                }
                random.push(info.selectedRandoms[randomIndex++]);
            } else if (command[0].equals("#IF")) {
                skip.push(random.isEmpty() || random.peek() != Integer.parseInt(command[1]));
            } else if (command[0].equals("#ENDIF")) {
                if (!skip.isEmpty()) skip.pop();
            } else if (command[0].equals("#ENDRANDOM")) {
                if (!random.isEmpty()) random.pop();
            } else if (command[0].equals("#ELSE") || command[0].equals("#ELSEIF")
                    || command[0].equals("#SETRANDOM")) {
                // The shared decoder does not implement these branches. Do not pretend its notes match C's parser.
                throw new IllegalArgumentException("Unsupported BMS branch for Angolmois: " + command[0]);
            } else if (skip.isEmpty() || !skip.peek()) {
                active.add(line);
                if (command[0].equals("#RANK") && command.length == 2) rank = Integer.parseInt(command[1]);
                if (line.matches("#[0-9]{3}02:.*")) {
                    double rate = Double.parseDouble(line.substring(7).strip());
                    lengths[Integer.parseInt(line.substring(1, 4))] = rate > .001 ? rate : 1;
                }
            }
        }
        var boundaries = Arrays.stream(model.getAllTimeLines()).filter(TimeLine::getSectionLine).toArray(TimeLine[]::new);
        if (boundaries.length == 0) {
            throw new IllegalArgumentException("Missing BMS measure boundaries");
        }
        var objects = new ArrayList<AngolmoisEngine.ChartObject>();
        double[] bpms = new double[1296], stops = new double[1296];
        for (String line : active) {
            if (line.matches("#BPM[0-9A-Z]{2}\\s+.*")) bpms[Integer.parseInt(line.substring(4, 6), 36)] = Double.parseDouble(line.substring(6).strip());
            if (line.matches("#STOP[0-9A-Z]{2}\\s+.*")) stops[Integer.parseInt(line.substring(5, 7), 36)] = Integer.parseInt(line.substring(7).strip()) / 192.0;
            if (line.startsWith("#STP ")) throw new IllegalArgumentException("Shared decoder does not preserve STP");
        }
        boolean poorAtZero = false;
        var bpmPositions = new HashSet<Double>();
        var controls = active.stream().filter(line -> line.matches("#[0-9]{3}(03|04|06|07|08|09|0A):.*"))
                .sorted(Comparator.comparing(line -> line.substring(0, 7))).toList();
        for (String line : controls) {
            int measure = Integer.parseInt(line.substring(1, 4));
            String data = line.substring(7).strip().split("\\s+", 2)[0];
            for (int i = 0; i + 1 < data.length(); i += 2) {
                int id = Integer.parseInt(data.substring(i, i + 2), 36);
                if (id == 0) continue;
                int channel = Integer.parseInt(line.substring(4, 6), 36);
                double position = measure + (double) i / data.length();
                if (channel == 4 || channel == 6 || channel == 7 || channel == 10) {
                    int layer = channel == 4 ? 0 : channel == 6 ? 3 : channel == 7 ? 1 : 2;
                    objects.add(new AngolmoisEngine.ChartObject(position, AngolmoisEngine.BGA, layer, 0));
                    if (channel == 6 && position == 0) poorAtZero = true;
                    continue;
                }
                double value = channel == 3 ? Integer.parseInt(data.substring(i, i + 2), 16)
                        : channel == 8 ? bpms[id] : stops[id];
                if (value <= 0 || !Double.isFinite(value)) throw new IllegalArgumentException("Invalid Angolmois control");
                var control = new AngolmoisEngine.ChartObject(position,
                        channel == 9 ? AngolmoisEngine.STOP : AngolmoisEngine.BPM, channel == 8 ? 1 : 0, value);
                // C sanitization keeps the first BPM of the lowest type. Channel 03 sorts before 08.
                if (channel == 9 || bpmPositions.add(position)) objects.add(control);
            }
        }
        if (!poorAtZero) objects.add(new AngolmoisEngine.ChartObject(0, AngolmoisEngine.BGA, 3, 0));
        int measure = 0;
        for (var tl : model.getAllTimeLines()) {
            while (measure + 1 < boundaries.length && boundaries[measure + 1].getSection() <= tl.getSection()) measure++;
            double position = measure + (tl.getSection() - boundaries[measure].getSection()) / lengths[measure];
            if (!Double.isFinite(position) || tl.getBPM() <= 0 || !Double.isFinite(tl.getBPM())) {
                throw new IllegalArgumentException("Invalid Angolmois timing");
            }
            for (int lane = 0; lane < 8; lane++) {
                if (tl.getHiddenNote(lane) != null) objects.add(new AngolmoisEngine.ChartObject(position, lane, AngolmoisEngine.HIDDEN, 0));
                var note = tl.getNote(lane);
                if (note == null) continue;
                int type;
                double value = 0;
                if (note instanceof LongNote ln) {
                    if (ln.getPair() == null) throw new IllegalArgumentException("Angolmois requires paired LN");
                    type = ln.isEnd() ? AngolmoisEngine.END : AngolmoisEngine.HEAD;
                } else if (note instanceof MineNote mine) {
                    type = AngolmoisEngine.MINE;
                    value = mine.getDamage() == 1295 ? -1 : (int) (mine.getDamage() * 512 / 200);
                    if (value == 0 || value > 512) continue;
                } else if (note instanceof NormalNote) {
                    type = AngolmoisEngine.NOTE;
                } else throw new IllegalArgumentException("Unsupported Angolmois note");
                objects.add(new AngolmoisEngine.ChartObject(position, lane, type, value));
            }
            // These objects also participate in C's global miss cursor, even though they are never scored.
            for (var ignored : tl.getBackGroundNotes()) objects.add(new AngolmoisEngine.ChartObject(position, AngolmoisEngine.BGM, 0, 0));
        }
        return new AngolmoisEngine(objects.toArray(AngolmoisEngine.ChartObject[]::new), model.getBpm(), lengths, rank);
    }
}
