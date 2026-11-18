package bms.player.beatoraja.play;

import bms.player.beatoraja.BMSPlayerMode.Mode;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.skin.SkinSource;
import bms.player.beatoraja.skin.SkinType;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Json;

import static bms.player.beatoraja.skin.SkinProperty.OFFSET_ALL;
import static bms.player.beatoraja.skin.SkinProperty.OFFSET_NOTES_1P;

public final class SkinAdjustmentOverlayTest {
    public static void main(String[] args) {
        for (Mode mode : Mode.values())
            check(SkinAdjustmentOverlay.allows(mode) == (mode == Mode.AUTOPLAY || mode == Mode.PRACTICE), "Mode gate");
        var skin = skin();
        try {
            var property = skin.getEditableNoteProperties();
            var note = new SkinOffset();
            var all = new SkinOffset();
            java.util.function.IntFunction<SkinOffset> offsets = id -> id == OFFSET_NOTES_1P ? note : all;
            var edit = new SkinAdjustmentOverlay.Session(skin, offsets);
            var controls = SkinOffsetSettings.edits(skin, offsets);
            check(controls.size() == 2 && !controls.get(0).allowed(0) && controls.get(0).allowed(3)
                    && controls.get(1).allowed(0), "Only declared axes are editable");
            edit.offsets.get(0).change(0, 10);
            edit.offsets.get(0).change(3, -20);
            edit.offsets.get(1).change(0, 8);
            edit.selectOption("layout", 11);
            edit.selectFile("notes", "blue.png");
            check(note.x == 0 && note.h == -20 && all.x == 8, "Only allowed live render values change");
            check(property.getOffset().length == 0, "Preview does not mutate saved values");
            edit.finish(false);
            check(note.h == 0 && all.x == 0, "Cancel restores both live offsets");
            check(property.getPendingSelection() == null, "Cancel does not queue a skin reload");
            edit = new SkinAdjustmentOverlay.Session(skin, offsets);
            edit.selectOption("layout", 11);
            edit.selectFile("notes", "blue.png");
            edit.offsets.get(0).change(3, 15);
            edit.offsets.get(1).change(2, 300);
            edit.finish(true);
            var json = new Json();
            var restored = json.fromJson(SkinConfig.Property.class, json.toJson(property));
            restored.validate();
            check(restored.getOffset()[0].name.equals("Notes offset") && restored.getOffset()[0].h == 15
                    && restored.getOffset()[1].name.equals("All offset(%)") && restored.getOffset()[1].w == 300
                    && restored.getNotesOffsetCenter() == 5 && !restored.hasLegacyNoteAdjustment(),
                    "Launcher offset names and values round-trip without a second adjustment");
            check(restored.getOption().length == 0 && restored.getFile().length == 0
                    && restored.getPendingSelection().options()[0].value == 11
                    && restored.getPendingSelection().files()[0].path.equals("blue.png"), "Pending choices round-trip separately");
            edit = new SkinAdjustmentOverlay.Session(skin, offsets);
            edit.selectFile("notes", "red.png");
            edit.finish(false);
            check(property.getPendingSelection().files()[0].path.equals("blue.png"), "Cancel preserves previous pending changes");
        } finally { skin.dispose(); }
        System.out.println("SkinAdjustmentOverlayTest passed");
    }

    public static void verifyScene2d() {
        var skin = skin();
        skin.header.setCustomOptions(new SkinHeader.CustomOption[]{
                new SkinHeader.CustomOption("Layout", new int[]{1, 2}, new String[]{"Old", "New"})});
        var live = new SkinOffset();
        var alternate = new SkinHeader();
        alternate.setPath(java.nio.file.Path.of("skin/other.json"));
        alternate.setName("Other");
        var overlay = new SkinAdjustmentOverlay(skin, id -> id == OFFSET_NOTES_1P ? live : new SkinOffset(),
                "font/VL-Gothic-Regular.ttf", true, () -> java.util.List.of(alternate));
        try {
            overlay.draw();
            var root = (Table) overlay.stage().getActors().first();
            var panel = (Table) root.getChildren().first();
            for (var child : panel.getChildren()) {
                if (!(child instanceof ScrollPane pane)) continue;
                boolean adjusted = false;
                for (var control : ((Table) pane.getActor()).getChildren()) {
                    if (control instanceof TextButton button) {
                        if (!adjusted && button.getText().toString().equals("+")) {
                            button.fire(new ChangeListener.ChangeEvent());
                            adjusted = true;
                        } else if (button.getText().toString().equals(">")) button.fire(new ChangeListener.ChangeEvent());
                    }
                }
            }
            check(live.h == 1 && live.x == 0, "Scene2D button updates the permitted live axis");
            for (var child : panel.getChildren()) {
                if (child instanceof TextButton button && button.getText().toString().equals(">")) {
                    button.fire(new ChangeListener.ChangeEvent());
                    check(overlay.previewSkin() == alternate, "Scene2D selects the alternate skin");
                    button.fire(new ChangeListener.ChangeEvent());
                    check(overlay.previewSkin() == null, "Scene2D returns to the current skin");
                    break;
                }
            }
            for (var child : panel.getChildren()) {
                if (child instanceof TextButton button) {
                    if (button.getText().toString().equals("Apply & Preview")) button.fire(new ChangeListener.ChangeEvent());
                    if (button.getText().toString().equals("Save")) button.fire(new ChangeListener.ChangeEvent());
                }
            }
            check(overlay.takePreviewRequest() && !overlay.takePreviewRequest()
                    && overlay.previewProperties().getOption()[0].value == 2
                    && skin.getEditableNoteProperties().getPendingSelection() == null,
                    "Preview submits the current draft once without changing the active settings");
            check(Boolean.TRUE.equals(overlay.saveRequested()), "Scene2D Save queues a commit");
            overlay.finish(true);
            check(skin.getEditableNoteProperties().getOffset()[0].h == 1, "Save commits the edited offset");
            check(skin.getEditableNoteProperties().getPendingSelection().options()[0].value == 2,
                    "Scene2D queues the selected option without reloading");
        } finally {
            overlay.dispose();
            skin.dispose();
        }
    }

    private static PlaySkin skin() {
        var header = new SkinHeader();
        header.setSourceResolution(Resolution.HD);
        header.setDestinationResolution(Resolution.HD);
        header.setSkinType(SkinType.PLAY_7KEYS);
        header.setCustomOffsets(new SkinHeader.CustomOffset[]{
                new SkinHeader.CustomOffset("Notes offset", OFFSET_NOTES_1P, false, false, false, true, false, false),
                new SkinHeader.CustomOffset("All offset(%)", OFFSET_ALL, true, true, true, true, false, false)});
        var skin = new PlaySkin(header);
        var property = new SkinConfig.Property();
        property.setNotesOffsetCenter(5);
        skin.setEditableNoteProperties(property);
        header.setSkinConfigProperty(property);
        for (var definition : header.getCustomOffsets()) skin.getOffset().put(definition.id, definition.getOffset());
        var note = new SkinNote(new SkinSource[0], new SkinSource[0][], new SkinSource[0]);
        note.setOffsetID(OFFSET_NOTES_1P);
        skin.add(note);
        return skin;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
