package bms.player.beatoraja.skin;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.play.PlaySkin;
import bms.player.beatoraja.play.SkinNote;
import com.badlogic.gdx.utils.Json;

/** Config persistence/validation and the shared JSON/Lua/LR2 post-load override. */
public final class NotesOffsetCenterConfigTest {
    public static void main(String[] args) {
        var json = new Json();
        for (int value : new int[]{Integer.MIN_VALUE, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, Integer.MAX_VALUE}) {
            int expected = value == 2 || value == 5 || value == 8 ? value : 0;
            var property = json.fromJson(SkinConfig.Property.class, "{notesOffsetCenter:" + value + "}");
            property.validate();
            check(property.getNotesOffsetCenter() == expected, "Validate raw configuration: " + value);
            var copy = json.fromJson(SkinConfig.Property.class, json.toJson(property));
            check(copy.getNotesOffsetCenter() == expected, "JSON persistence: " + value);
            property.setNotesOffsetCenter(value);
            check(property.getNotesOffsetCenter() == expected, "Setter validation");
        }
        check(json.fromJson(SkinConfig.Property.class, "{}").getNotesOffsetCenter() == 0, "Old config keeps skin default");
        var header = new SkinHeader();
        header.setSourceResolution(Resolution.HD); header.setDestinationResolution(Resolution.HD);
        var skin = new PlaySkin(header);
        var note = new SkinNote(new SkinSource[0], new SkinSource[0][], new SkinSource[0]);
        var image = new SkinImage(new SkinSourceImage[0], 0);
        skin.add(note); skin.add(image);
        try {
            note.setOffsetCenter(7); image.setOffsetCenter(9);
            var property = new SkinConfig.Property();
            SkinLoader.applyNotesOffsetCenter(skin, property);
            check(note.getOffsetCenter() == 7, "Unspecified config retains existing note anchor");
            for (int center : new int[]{2, 5, 8}) {
                property.setNotesOffsetCenter(center);
                SkinLoader.applyNotesOffsetCenter(skin, property);
                check(note.getOffsetCenter() == center, "Common loader override, including LR2");
                check(image.getOffsetCenter() == 9, "Only notes are overridden");
            }
            SkinLoader.applyNotesOffsetCenter(null, property);
        } finally { skin.dispose(); }
        System.out.println("NotesOffsetCenterConfigTest passed (old config, validation, persistence, shared/LR2 override, unrelated objects)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
