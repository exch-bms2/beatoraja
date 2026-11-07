package bms.player.beatoraja.play;

import java.util.Arrays;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.play.SkinNote.SkinLane;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.skin.SkinObjectOffsetCenterTest.Draw;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import static bms.player.beatoraja.skin.SkinObjectOffsetCenterTest.near;

/** Captures the real SkinNote -> LaneRenderer draw path with inert player/GL fixtures. */
public final class NoteOffsetCenterTest {
    public static void main(String[] args) throws Exception {
        try (var gl = new MockTextureEnvironment()) {
            var texture = gl.createTexture();
            try {
                var f = new Fixture(new TextureRegion(texture));
                for (int center = 0; center <= 9; center++) {
                    for (int sign : new int[]{-1, 0, 1}) {
                        f.offset.w = 12 * sign; f.offset.h = 4 * sign;
                        for (int beat : new int[]{-1, 0, 4, 9, 100, 159, 160}) {
                            f.beat(beat);
                            f.chart(new NormalNote(0));
                            Draw normal = f.draw(center, 0, f.lane.noteImage);
                            verify(normal, center, f.offset, beat, false);
                            f.chart(new MineNote(0, 1));
                            verify(f.draw(center, 0, f.lane.mineImage), center, f.offset, beat, false);
                        }
                        f.beat(-1);
                        f.chart(null);
                        f.line.setHiddenNote(0, new NormalNote(0)); f.playerConfig.setShowhiddennote(true);
                        verify(f.draw(center, 0, f.lane.hiddenImage), center, f.offset, -1, center == 0);
                        f.playerConfig.setShowhiddennote(false);
                        for (int type : new int[]{LongNote.TYPE_LONGNOTE, LongNote.TYPE_CHARGENOTE, LongNote.TYPE_HELLCHARGENOTE}) {
                            for (boolean constant : new boolean[]{false, true}) {
                                for (int beat : new int[]{-1, 9}) {
                                    f.beat(beat); f.longChart(type);
                                    f.playConfig.setEnableConstant(constant); f.playConfig.setDuration(200);
                                    int image = type == LongNote.TYPE_HELLCHARGENOTE ? 5 : 1;
                                    verify(f.draw(center, 0, f.lane.longImage[image]), center, f.offset, beat, false);
                                    verify(f.draw(center, 100, f.lane.longImage[image]), center, f.offset, beat, false);
                                    check(f.batch.draws.stream().allMatch(draw -> Float.isFinite(draw.height()) && draw.height() >= 0),
                                            "LN body and endpoints remain valid when anchored below the lane floor");
                                }
                            }
                        }
                        f.playConfig.setEnableConstant(false);
                        f.beat(-1); f.chart(new NormalNote(0)); f.lane.dstnote2 = 0;
                        verify(f.draw(center, 0, f.lane.noteImage), center, f.offset, -1, center == 0);
                        f.lane.dstnote2 = Integer.MIN_VALUE;
                    }
                }
                // The old entry point and explicit zero must produce identical legacy rectangles.
                f.chart(new NormalNote(0)); f.beat(9); f.offset.w = 12; f.offset.h = 4;
                f.renderer.drawLane(f.sprite, 1000, new SkinLane[]{f.lane}, new SkinOffset[]{f.offset});
                Draw legacy = f.batch.draws.get(f.batch.draws.size() - 1);
                Draw zero = f.draw(0, 0, f.lane.noteImage);
                near(legacy.x(), zero.x()); near(legacy.y(), zero.y());
                near(legacy.width(), zero.width()); near(legacy.height(), zero.height());
            } finally { texture.dispose(); }
        }
        System.out.println("NoteOffsetCenterTest passed (real SkinNote forwarding, normal/mine/hidden, LN/CN/HCN, held/CONSTANT, beat expansion, PMS and legacy geometry)");
    }

    private static void verify(Draw draw, int center, SkinOffset offset, int beat, boolean ignoreOffset) {
        float w = ignoreOffset ? 40 : 40 + offset.w, h = ignoreOffset ? 8 : 8 + offset.h;
        float x = 100 + (ignoreOffset ? 0 : offset.x), y = 100 + (ignoreOffset ? 0 : offset.y);
        if (center != 0) {
            x -= offset.w * ((center - 1) % 3) * 0.5f;
            y -= offset.h * ((center - 1) / 3) * 0.5f;
        }
        if (beat >= 0 && beat <= 159) {
            float rate = beat < 9 ? beat / 9f : (150 - (beat - 9)) / 150f;
            float expandedW = w * (1 + rate), expandedH = h * (1 + rate);
            x -= (expandedW - (center == 0 ? 40 : w)) / 2;
            y -= (expandedH - (center == 0 ? 8 : h)) / 2;
            w = expandedW; h = expandedH;
        }
        near(draw.x(), x); near(draw.y(), y); near(draw.width(), w); near(draw.height(), h);
    }

    private static final class Fixture {
        final PlayerConfig playerConfig = new PlayerConfig();
        final PlayConfig playConfig = new PlayConfig();
        final SkinOffset offset = new SkinOffset();
        final SkinLane lane;
        final SkinNote note = new SkinNote(new SkinSource[0], new SkinSource[0][], new SkinSource[0]);
        final LaneRenderer renderer;
        final Player player;
        final PlaySkin skin;
        final BMSModel model = new BMSModel();
        final TimerManager timer = new TimerManager();
        final SkinObjectOffsetCenterTest.RecordingBatch batch = SkinObjectOffsetCenterTest.recordingBatch();
        final Skin.SkinObjectRenderer sprite = SkinObjectOffsetCenterTest.renderer(batch);
        TimeLine line;
        Fixture(TextureRegion image) throws Exception {
            playerConfig.setJudgetiming(0); playerConfig.setShowpastnote(false);
            playConfig.setHispeed(1); playConfig.setEnablelift(false); playConfig.setEnablelanecover(false);
            var header = new SkinHeader(); header.setSourceResolution(Resolution.HD); header.setDestinationResolution(Resolution.HD);
            skin = new PlaySkin(header); skin.setLaneGroupRegion(new Rectangle[0]);
            lane = KeyboardQueueTestSupport.allocate(SkinLane.class);
            lane.region = new Rectangle(100, 100, 40, 600); lane.scale = 8; lane.dstnote2 = Integer.MIN_VALUE;
            lane.noteImage = image; lane.mineImage = new TextureRegion(image); lane.hiddenImage = new TextureRegion(image);
            lane.processedImage = new TextureRegion(image); lane.longImage = new TextureRegion[10];
            Arrays.setAll(lane.longImage, i -> new TextureRegion(image));
            var controller = KeyboardQueueTestSupport.allocate(MainController.class);
            var offsets = new SkinOffset[SkinProperty.OFFSET_MAX + 1]; Arrays.setAll(offsets, i -> new SkinOffset());
            set(controller, MainController.class, "offset", offsets);
            player = KeyboardQueueTestSupport.allocate(Player.class);
            set(player, MainState.class, "main", controller); set(player, MainState.class, "timer", timer);
            set(player, MainState.class, "skin", skin);
            player.judge = KeyboardQueueTestSupport.allocate(JudgeManager.class);
            var stateClass = Class.forName(JudgeManager.class.getName() + "$LaneState");
            var states = java.lang.reflect.Array.newInstance(stateClass, 1);
            java.lang.reflect.Array.set(states, 0, KeyboardQueueTestSupport.allocate(stateClass));
            set(player.judge, JudgeManager.class, "states", states);
            set(player.judge, JudgeManager.class, "window", new JudgeProperty.JudgeWindow(JudgeProperty.SEVENKEYS, 100, new int[]{100,100,100}));
            renderer = KeyboardQueueTestSupport.allocate(LaneRenderer.class);
            set(renderer, LaneRenderer.class, "main", player); set(renderer, LaneRenderer.class, "skin", skin);
            set(renderer, LaneRenderer.class, "config", playerConfig); set(renderer, LaneRenderer.class, "playconfig", playConfig);
            set(renderer, LaneRenderer.class, "model", model);
            model.setMode(Mode.BEAT_7K); model.setBpm(120);
            set(note, SkinNote.class, "lanes", new SkinLane[]{lane}); set(note, SkinNote.class, "renderer", renderer);
            set(note, SkinObject.class, "off", new SkinOffset[]{offset});
        }
        void beat(int elapsed) throws Exception {
            skin.setNoteExpansionRate(elapsed < 0 ? new int[]{100,100} : new int[]{200,200});
            set(timer, TimerManager.class, "nowmicrotime", Math.max(0, elapsed) * 1000L);
        }
        void chart(Note value) throws Exception {
            line = timeline(0, 1_000_000); line.setNote(0, value);
            set(renderer, LaneRenderer.class, "timelines", new TimeLine[]{line}); set(renderer, LaneRenderer.class, "pos", 0);
        }
        void longChart(int type) throws Exception {
            var start = new LongNote(0); var end = new LongNote(0);
            start.setType(type); end.setType(type);
            chart(start); var last = timeline(0.25, 1_500_000); last.setNote(0, end); start.setPair(end);
            set(renderer, LaneRenderer.class, "timelines", new TimeLine[]{line, last});
        }
        Draw draw(int center, long time, TextureRegion image) throws Exception {
            batch.draws.clear(); note.setOffsetCenter(center); set(note, SkinNote.class, "time", 1000 + time);
            set(renderer, LaneRenderer.class, "pos", 0); note.draw(sprite);
            return batch.draws.stream().filter(draw -> draw.image() == image).reduce((a, b) -> b)
                    .orElseThrow(() -> new AssertionError("Expected note draw, center=" + center));
        }
        private static TimeLine timeline(double section, long microtime) {
            var line = new TimeLine(section, microtime, Mode.BEAT_7K.key); line.setBPM(120); line.setScroll(1);
            return line;
        }
    }
    private static final class Player extends BMSPlayer {
        JudgeManager judge;
        private Player() { super(null, null); }
        @Override public int getState() { return STATE_PLAY; }
        @Override public long getNowQuarterNoteTime() { return 0; }
        @Override public JudgeManager getJudgeManager() { return judge; }
    }
    private static void set(Object value, Class<?> owner, String field, Object data) throws Exception {
        KeyboardQueueTestSupport.set(value, owner, field, data);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
