package bms.player.beatoraja.play;

import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinHeader.CustomOffset;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;

import static bms.player.beatoraja.skin.SkinProperty.OFFSET_ALL;
import static bms.player.beatoraja.skin.SkinProperty.OFFSET_NOTES_1P;

/** The two existing play-skin offsets, edited through the live values used by rendering. */
public final class SkinOffsetSettings {
    private SkinOffsetSettings() { }

    static List<Edit> edits(PlaySkin skin, IntFunction<SkinOffset> liveOffsets) {
        var edits = new ArrayList<Edit>();
        for (int id : new int[]{OFFSET_NOTES_1P, OFFSET_ALL}) {
            var definition = definition(skin, id);
            var stored = skin.getOffset().get(id);
            if (definition == null || stored == null) continue;
            if (id == OFFSET_NOTES_1P && Arrays.stream(skin.getAllSkinObjects())
                    .noneMatch(object -> object instanceof SkinNote && uses(object, id))) continue;
            if (id == OFFSET_ALL && !Skin.supportsAllOffset(skin.header.getSkinType())) continue;
            var live = liveOffsets.apply(id);
            if (live != null) edits.add(new Edit(definition, stored, live));
        }
        return edits;
    }

    private static CustomOffset definition(PlaySkin skin, int id) {
        CustomOffset match = null;
        for (var offset : skin.header.getCustomOffsets()) {
            if (offset.id != id) continue;
            if (match != null) return null;
            match = offset;
        }
        return match;
    }

    private static boolean uses(SkinObject object, int id) {
        return object != null && Arrays.stream(object.getOffsetID()).anyMatch(offset -> offset == id);
    }

    private static boolean allowed(CustomOffset offset, int axis) {
        return switch (axis) { case 0 -> offset.x; case 1 -> offset.y; case 2 -> offset.w; case 3 -> offset.h; default -> false; };
    }

    private static void store(PlaySkin skin, SkinConfig.Property property, int id, SkinConfig.Offset value) {
        store(property, value);
        skin.getOffset().put(id, value);
    }

    private static void store(SkinConfig.Property property, SkinConfig.Offset value) {
        var offsets = new ArrayList<SkinConfig.Offset>();
        for (var offset : property.getOffset()) if (!value.name.equals(offset.name)) offsets.add(offset);
        offsets.add(value);
        property.setOffset(offsets.toArray(SkinConfig.Offset[]::new));
    }

    /** Only migrate when the old note-only transform is equivalent to the declared offset. */
    public static boolean migrateLegacy(PlaySkin skin, SkinConfig.Property property, String path) {
        if (!property.hasLegacyNoteAdjustment()) return false;
        // Lua/JSON skins may also use the offset in load-time expressions.
        if (!path.toLowerCase(Locale.ROOT).endsWith(".lr2skin")) return false;
        var definition = definition(skin, OFFSET_NOTES_1P);
        var stored = skin.getOffset().get(OFFSET_NOTES_1P);
        if (definition == null || stored == null) return false;
        int[] delta = property.getNoteAdjustment();
        int[] merged = values(stored);
        for (int i = 0; i < delta.length; i++) {
            if (delta[i] == 0) continue;
            long sum = (long) merged[i] + delta[i];
            if (!allowed(definition, i) || sum < -9999 || sum > 9999) return false;
            merged[i] = (int) sum;
        }
        boolean hasNotes = false;
        for (var object : skin.getAllSkinObjects()) {
            if (object instanceof SkinNote note) {
                hasNotes = true;
                // Other live offsets may change the dimensions, so the old clamp cannot be proven redundant.
                if (!uses(note, OFFSET_NOTES_1P) || note.getOffsetID().length != 1
                        || note.getOffsetCenter() < 1 || note.getOffsetCenter() > 9
                        || !note.canMigrateSize(delta[2], delta[3], merged[2], merged[3])) return false;
            } else if (uses(object, OFFSET_NOTES_1P)) return false;
            else if (object instanceof SkinJudge judge) {
                for (int i = 0; i < 7; i++)
                    if (uses(judge.getJudge(i), OFFSET_NOTES_1P) || uses(judge.getJudgeCount(i), OFFSET_NOTES_1P)) return false;
            }
        }
        if (!hasNotes) return false;
        store(skin, property, OFFSET_NOTES_1P, copy(definition.name, stored, merged));
        property.setNoteAdjustment(null);
        return true;
    }

    private static int[] values(SkinConfig.Offset offset) {
        return new int[]{offset.x, offset.y, offset.w, offset.h};
    }

    private static SkinConfig.Offset copy(String name, SkinConfig.Offset original, int[] values) {
        var result = new SkinConfig.Offset();
        result.name = name;
        result.x = values[0]; result.y = values[1]; result.w = values[2]; result.h = values[3];
        result.r = original.r; result.a = original.a;
        return result;
    }

    static final class Edit {
        final CustomOffset definition;
        final int[] values;
        private final SkinConfig.Offset stored;
        private final SkinOffset live;
        private final float[] original;

        Edit(CustomOffset definition, SkinConfig.Offset stored, SkinOffset live) {
            this.definition = definition;
            this.stored = stored;
            this.live = live;
            values = values(stored);
            original = new float[]{live.x, live.y, live.w, live.h};
        }

        boolean allowed(int axis) { return SkinOffsetSettings.allowed(definition, axis); }

        void change(int axis, int delta) {
            if (!allowed(axis)) return;
            values[axis] = (int) Math.max(-9999, Math.min(9999, (long) values[axis] + delta));
            switch (axis) {
                case 0 -> live.x = values[axis];
                case 1 -> live.y = values[axis];
                case 2 -> live.w = values[axis];
                case 3 -> live.h = values[axis];
            }
        }

        void finish(PlaySkin skin, SkinConfig.Property property, boolean save) {
            if (save) {
                if (!Arrays.equals(values, values(stored)))
                    store(skin, property, definition.id, copy(definition.name, stored, values));
            } else {
                live.x = original[0]; live.y = original[1]; live.w = original[2]; live.h = original[3];
            }
        }

        void copyTo(SkinConfig.Property property) {
            if (!Arrays.equals(values, values(stored))) store(property, copy(definition.name, stored, values));
        }
    }
}
