package bms.player.beatoraja.skin.property;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.ReplayData;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.result.MusicResult;

import static bms.player.beatoraja.input.KeyboardQueueTestSupport.allocate;
import static bms.player.beatoraja.input.KeyboardQueueTestSupport.set;

public final class AssignedLanePropertyTest {
    public static void main(String[] args) throws Exception {
        var model = new BMSModel();
        model.setMode(Mode.BEAT_14K);
        var resource = allocate(PlayerResource.class);
        set(resource, PlayerResource.class, "model", model);
        var main = allocate(MainController.class);
        set(main, MainController.class, "resource", resource);
        var result = new MusicResult(main);

        var live = new ReplayData();
        live.randomoption = 2; // RANDOM
        live.randomoption2 = 8; // RANDOM EX
        live.laneShufflePattern = new int[][]{
                {2, 0, 1, 3, 4, 5, 6, 7},
                {15, 8, 9, 10, 11, 12, 13, 14}
        };
        var player = allocate(BMSPlayer.class);
        set(player, BMSPlayer.class, "model", model);
        set(player, BMSPlayer.class, "playinfo", live);
        set(player, MainState.class, "resource", resource);

        var saved = new ReplayData();
        saved.randomoption = 2;
        saved.randomoption2 = 2;
        saved.laneShufflePattern = new int[][]{
                {5, 0, 1, 2, 3, 4, 6, 7},
                {10, 8, 9, 11, 12, 13, 14, 15}
        };
        resource.setReplayData(saved);

        var first = IntegerPropertyFactory.getImageIndexProperty(450);
        var second = IntegerPropertyFactory.getImageIndexProperty(460);
        check(first != null && first == IntegerPropertyFactory.getImageIndexProperty("pattern_1p_1")
                && second != null && second == IntegerPropertyFactory.getImageIndexProperty("pattern_2p_1"),
                "IDs and names resolve to the same image index properties");
        check(first.get(player) == 3 && second.get(player) == 8, "Play properties use the current chart's lane assignments");
        check(first.get(result) == 6 && second.get(result) == 3, "Results use their existing replay data");

        var scratch1P = IntegerPropertyFactory.getImageIndexProperty("pattern_1p_SCR");
        var scratch2P = IntegerPropertyFactory.getImageIndexProperty("pattern_2p_SCR");
        check(scratch1P.get(player) == 0 && scratch2P.get(player) == 7 && scratch2P.get(result) == 0,
                "Scratch assignments are available for RANDOM EX only");
        model.setMode(Mode.BEAT_7K);
        check(first.get(player) == 3 && second.get(player) == 0, "SP exposes only the first side");
        live.randomoption = 1; // MIRROR
        check(first.get(player) == 0, "Options without displayable shuffle data return zero");
        live.randomoption = 2;
        live.laneShufflePattern = null;
        check(first.get(player) == 0, "Play without a stored lane mapping returns zero");
        saved.laneShufflePattern = new int[][]{{}};
        check(first.get(result) == 0, "A missing entry in a replay mapping returns zero");
        System.out.println("AssignedLanePropertyTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
