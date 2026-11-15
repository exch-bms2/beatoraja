package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.model.TimeLine;
import bms.player.beatoraja.*;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinCatalog;
import bms.player.beatoraja.skin.SkinImage;
import bms.player.beatoraja.skin.SkinLoader;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.skin.lua.SkinLuaAccessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static bms.player.beatoraja.input.KeyboardQueueTestSupport.allocate;
import static bms.player.beatoraja.input.KeyboardQueueTestSupport.set;
import static bms.player.beatoraja.skin.SkinProperty.OFFSET_NOTES_1P;
import static bms.player.beatoraja.skin.SkinProperty.OFFSET_ALL;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PLAY;

public final class PracticeSkinPreviewTest {
    public static void main(String[] args) throws Exception {
        var main = allocate(MainController.class);
        var live = new SkinOffset[MainController.offsetCount];
        java.util.Arrays.setAll(live, i -> new SkinOffset());
        set(main, MainController.class, "offset", live);
        var resource = allocate(PlayerResource.class);
        var profile = new PlayerConfig();
        set(resource, PlayerResource.class, "pconfig", profile);
        var timer = new TimerManager();
        var player = allocate(BMSPlayer.class);
        set(player, MainState.class, "main", main);
        set(player, MainState.class, "resource", resource);
        set(player, MainState.class, "timer", timer);
        var model = new BMSModel();
        model.setMode(Mode.BEAT_7K);
        model.setAllTimeLine(new TimeLine[0]);
        set(player, BMSPlayer.class, "model", model);
        set(player, BMSPlayer.class, "state", BMSPlayer.STATE_PRACTICE);
        var configured = new SkinConfig("skin/practice.luaskin");
        configured.validate();
        profile.getSkin()[SkinType.PLAY_7KEYS.getId()] = configured;
        var saved = configured.getProperties();
        saved.setOption(new SkinConfig.Option[]{option(1)});
        saved.setNotesOffsetCenter(5);
        saved.setPendingSelection(selection(3));
        var old = new ProbeSkin();
        old.setEditableNoteProperties(saved);
        old.getOffset().put(OFFSET_NOTES_1P, offset("Notes offset", 6));
        player.setSkin(old);
        var draft = saved.candidateFor(selection(2));
        draft.setOffset(new SkinConfig.Offset[]{offset("All offset(%)", 9)});

        resource.setPlayMode(BMSPlayerMode.AUTOPLAY);
        check(!player.applySkinPreview(draft, (type, cfg) -> { throw new AssertionError("Autoplay loaded"); }), "Autoplay disabled");
        resource.setPlayMode(BMSPlayerMode.PRACTICE);
        timer.setTimerOn(TIMER_PLAY);
        check(!player.applySkinPreview(draft, (type, cfg) -> { throw new AssertionError("Live play loaded"); }), "Active timer disables preview");
        timer.setTimerOff(TIMER_PLAY);
        set(player, BMSPlayer.class, "state", BMSPlayer.STATE_READY);
        check(!player.canPreviewSkin(), "Ready disables preview");
        set(player, BMSPlayer.class, "state", BMSPlayer.STATE_PRACTICE);
        check(!player.applySkinPreview(draft, (type, cfg) -> {
            var lua = new SkinLuaAccessor(false);
            try {
                lua.exportMainStateAccessor(player);
                check(lua.exec("""
                        local state = require('main_state')
                        return state.set_volume_sys(0) == false
                           and state.set_timer(0, 0) == false
                           and state.event_exec(0) == false
                        """).toboolean(), "Candidate Lua cannot mutate live state while loading");
            } finally { lua.dispose(); }
            return null;
        }), "Load failure");
        check(player.getSkin() == old && old.disposals == 0 && saved.getOption()[0].value == 1
                && saved.getPendingSelection().options()[0].value == 3, "Failed load preserves skin, active settings and prior pending edits");

        var broken = new ProbeSkin();
        broken.fail = true;
        broken.getOffset().put(OFFSET_ALL, draft.getOffset()[0]);
        live[OFFSET_ALL].h = 4;
        check(!player.applySkinPreview(draft, (type, cfg) -> broken), "Preparation failure");
        check(player.getSkin() == old && old.disposals == 0 && broken.disposals == 1
                && live[OFFSET_NOTES_1P].h == 6 && live[OFFSET_ALL].h == 4
                && saved.getOption()[0].value == 1 && saved.getOffset().length == 0, "Rollback restores offsets and releases only candidate");

        var good = new ProbeSkin();
        good.getOffset().put(OFFSET_ALL, draft.getOffset()[0]);
        check(player.applySkinPreview(draft, (type, cfg) -> {
            check(cfg.getPath().equals(configured.getPath()) && cfg.getProperties().getRequiredSelection().options()[0].value == 2,
                    "Preview uses the validated draft of the current skin");
            return good;
        }), "Successful preview");
        check(player.getSkin() == good && good.prepared && old.disposals == 1 && good.disposals == 0
                && live[OFFSET_NOTES_1P].h == 0 && live[OFFSET_ALL].h == 9
                && saved.getOption()[0].value == 2 && saved.getOffset()[0].h == 9
                && saved.getPendingSelection() == null && saved.getNotesOffsetCenter() == 5
                && good.getEditableNoteProperties() == saved, "Success publishes and persists one shared set of settings");
        var other = new SkinConfig("skin/other.json");
        other.validate();
        other.getProperties().setOption(new SkinConfig.Option[]{option(7)});
        other.getProperties().setNotesOffsetCenter(8);
        profile.setSkinHistory(new SkinConfig[]{other});
        var target = target(other.getPath(), SkinType.PLAY_7KEYS);
        check(!player.applySkinPreview(target(other.getPath(), SkinType.PLAY_14KEYS), null,
                (type, cfg) -> { throw new AssertionError("Incompatible skin reached loader"); }), "Wrong mode rejected");
        var rejected = new ProbeSkin();
        rejected.fail = true;
        check(!player.applySkinPreview(target, null, (type, cfg) -> rejected)
                && profile.getSkin()[SkinType.PLAY_7KEYS.getId()] == configured
                && profile.getSkinHistory()[0] == other && good.disposals == 0 && rejected.disposals == 1,
                "Failed switch preserves active skin and history");
        var otherLoaded = new ProbeSkin();
        check(player.applySkinPreview(target, null, (type, cfg) -> {
            check(cfg.getPath().equals(other.getPath()) && cfg.getProperties().getOption()[0].value == 7
                    && cfg.getProperties().getNotesOffsetCenter() == 8, "Target's saved values are loaded, not current draft");
            return otherLoaded;
        }), "Switch to compatible skin");
        check(player.getSkin() == otherLoaded && good.disposals == 1 && profile.getSkinHistory()[1] == configured
                && profile.getSkin()[SkinType.PLAY_7KEYS.getId()].getPath().equals(other.getPath()), "Active path and history updated only after success");
        var returned = new ProbeSkin();
        check(player.applySkinPreview(target(configured.getPath(), SkinType.PLAY_7KEYS), null, (type, cfg) -> {
            check(cfg.getProperties().getOption()[0].value == 2 && cfg.getProperties().getOffset()[0].h == 9,
                    "Switching back restores the previous skin's values");
            return returned;
        }) && otherLoaded.disposals == 1, "Switch back");
        player.setSkin(null);
        check(returned.disposals == 1, "Current skin is released normally on exit");
        verifyCatalog();
        System.out.println("PracticeSkinPreviewTest passed");
    }

    private static SkinHeader target(String path, SkinType type) {
        var header = new SkinHeader();
        header.setPath(Path.of(path));
        header.setSkinType(type);
        return header;
    }

    private static void verifyCatalog() throws Exception {
        var dir = Files.createTempDirectory("practice-skin-catalog-");
        try {
            var valid = Files.writeString(dir.resolve("play.json"), "{type:0,name:Seven}");
            Files.writeString(dir.resolve("select.json"), "{type:5,name:Select}");
            Files.writeString(dir.resolve("broken.json"), "{");
            var choices = SkinCatalog.discover(new Config(), dir, SkinType.PLAY_7KEYS);
            check(choices.size() == 1 && choices.get(0).getPath().equals(valid), "Catalog excludes incompatible and corrupt skins");
        } finally {
            try (var paths = Files.walk(dir)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static SkinConfig.Offset offset(String name, int value) {
        var offset = new SkinConfig.Offset();
        offset.name = name;
        offset.h = value;
        return offset;
    }

    private static SkinConfig.Selection selection(int value) {
        return new SkinConfig.Selection(new SkinConfig.Option[]{option(value)}, new SkinConfig.FilePath[0]);
    }

    private static SkinConfig.Option option(int value) {
        var option = new SkinConfig.Option();
        option.name = "layout";
        option.value = value;
        return option;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void verifyTextures() {
        try {
            var dir = Files.createTempDirectory("practice-preview-gl-");
            try {
                var pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
                try { PixmapIO.writePNG(new FileHandle(dir.resolve("selected.png").toFile()), pixmap); }
                finally { pixmap.dispose(); }
                var path = Files.writeString(dir.resolve("play.json"), """
                        {"type":0,"name":"preview","w":1280,"h":720,
                         "filepath":[{"name":"image","path":"*.png"}],
                         "source":[{"id":"source","path":"*.png"}],
                         "image":[{"id":"image","src":"source","x":0,"y":0,"w":2,"h":2}],
                         "destination":[{"id":"image","dst":[{"time":0,"x":0,"y":0,"w":2,"h":2}]}]}
                        """);
                var resource = allocate(PlayerResource.class);
                set(resource, PlayerResource.class, "config", new Config());
                var state = allocate(BMSPlayer.class);
                set(state, MainState.class, "resource", resource);
                var file = new SkinConfig.FilePath();
                file.name = "image";
                file.path = "selected.png";
                var draft = new SkinConfig.Property().candidateFor(new SkinConfig.Selection(
                        new SkinConfig.Option[0], new SkinConfig.FilePath[]{file}));
                var candidate = new SkinConfig(path.toString());
                candidate.setProperties(draft);
                var cache = SkinLoader.getResource();
                int cacheSize = cache.size();
                for (int i = 0; i < 4; i++) {
                    var skin = SkinLoader.loadPracticePreview(state, SkinType.PLAY_7KEYS, candidate);
                    check(skin instanceof PlaySkin, "Actual preview skin loads");
                    var image = (SkinImage) skin.getAllSkinObjects()[0];
                    var texture = image.getImage(0, state).getTexture();
                    check(texture.getTextureObjectHandle() != 0 && cache.size() == cacheSize,
                            "Texture uploaded without modifying the shared pixmap cache");
                    skin.dispose();
                    check(texture.getTextureObjectHandle() == 0, "Preview texture released");
                }
            } finally {
                try (var paths = Files.walk(dir)) {
                    for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
                }
            }
        } catch (Exception failure) {
            throw new AssertionError("Practice preview texture check failed", failure);
        }
    }

    private static final class ProbeSkin extends PlaySkin {
        int disposals;
        boolean fail, prepared;
        ProbeSkin() { super(header()); }
        private static SkinHeader header() {
            var header = new SkinHeader();
            header.setSourceResolution(Resolution.HD);
            header.setDestinationResolution(Resolution.HD);
            header.setSkinType(SkinType.PLAY_7KEYS);
            return header;
        }
        @Override public void prepare(MainState state) {
            check(state.getSkin() == this, "Candidate is visible to properties during preparation");
            if (fail) throw new IllegalStateException("test preparation failure");
            prepared = true;
        }
        @Override public void dispose() { disposals++; }
    }
}
