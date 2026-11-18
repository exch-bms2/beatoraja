package bms.player.beatoraja;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.input.controller.ControllerBackend;
import bms.player.beatoraja.skin.SkinType;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Json;

public final class PlaySideConfigTest {
    public static void main(String[] args) throws Exception {
        var json = new Json();
        var profile = json.fromJson(PlayerConfig.class, "{}");
        profile.validate();
        var modes = new Mode[]{Mode.BEAT_5K, Mode.BEAT_7K, Mode.KEYBOARD_24K};
        var skins = new SkinType[]{SkinType.PLAY_5KEYS, SkinType.PLAY_7KEYS, SkinType.PLAY_24KEYS};
        check(!profile.isIndependentPlaySides()
                && profile.getPlayInputConfig(Mode.BEAT_7K, PlaySide.P2) == profile.getMode7(), "Old config uses the existing input bank");
        for (var mode : modes) profile.getPlayConfig(mode).getKeyboardConfig().getKeyAssign()[0] = Keys.Q;
        profile.setIndependentPlaySides(true);
        for (int i = 0; i < modes.length; i++) {
            var left = profile.getPlayInputConfig(modes[i], PlaySide.P1);
            var right = profile.getPlayInputConfig(modes[i], PlaySide.P2);
            check(right != left && right.getKeyboardConfig().getKeyAssign()[0] == Keys.Q
                    && right.getPlayconfig() == null, "Initial input copy does not create a second PlayConfig");
            right.getKeyboardConfig().getKeyAssign()[0] = Keys.Z;
            check(left.getKeyboardConfig().getKeyAssign()[0] == Keys.Q, "Keyboard arrays are independent");
            var leftSkin = profile.getSkinConfig(skins[i], PlaySide.P1);
            var rightSkin = profile.getSkinConfig(skins[i], PlaySide.P2);
            check(leftSkin != rightSkin && leftSkin.getPath().equals(rightSkin.getPath()), "Same skin path has independent settings");
            rightSkin.getProperties().setNotesOffsetCenter(8);
            check(leftSkin.getProperties().getNotesOffsetCenter() == 0, "Left offset center remains unchanged");
        }
        var leftSkin = profile.getSkinConfig(SkinType.PLAY_7KEYS, PlaySide.P1);
        var rightSkin = profile.getSkinConfig(SkinType.PLAY_7KEYS, PlaySide.P2);
        profile.setSkinHistory(PlaySide.P1, new SkinConfig[]{leftSkin});
        profile.setSkinHistory(PlaySide.P2, new SkinConfig[]{rightSkin});
        profile.selectControllerBackend(ControllerBackend.SDL);
        profile.getPlayInputConfig(Mode.BEAT_7K, PlaySide.P2).getController()[0].setName("SDL right");
        profile.selectControllerBackend(ControllerBackend.JINPUT);
        check(!"SDL right".equals(profile.getPlayInputConfig(Mode.BEAT_7K, PlaySide.P2).getController()[0].getName()), "Backend banks remain separate");
        profile.getPlayInputConfig(Mode.BEAT_7K, PlaySide.P2).getController()[0].setName("JInput right");
        profile.setIndependentPlaySides(false);
        var restored = json.fromJson(PlayerConfig.class, json.toJson(profile));
        restored.validate();
        check(!restored.isIndependentPlaySides() && restored.getSkinConfig(SkinType.PLAY_7KEYS, PlaySide.P2)
                == restored.getSkinConfig(SkinType.PLAY_7KEYS, PlaySide.P1), "Disabled settings are not used after restart");
        restored.setIndependentPlaySides(true);
        for (int i = 0; i < modes.length; i++) {
            check(restored.getPlayInputConfig(modes[i], PlaySide.P2).getKeyboardConfig().getKeyAssign()[0] == Keys.Z,
                    "Right assignment survives a save/reload while disabled");
            check(restored.getSkinConfig(skins[i], PlaySide.P2).getProperties().getNotesOffsetCenter() == 8,
                    "Right skin options survive a save/reload");
        }
        check(restored.getSkinHistory(PlaySide.P1)[0].getProperties().getNotesOffsetCenter() == 0
                && restored.getSkinHistory(PlaySide.P2)[0].getProperties().getNotesOffsetCenter() == 8, "History is separate for the same path");
        check("JInput right".equals(restored.getPlayInputConfig(Mode.BEAT_7K, PlaySide.P2).getController()[0].getName()), "JInput restored");
        restored.selectControllerBackend(ControllerBackend.SDL_ASYNC);
        check("SDL right".equals(restored.getPlayInputConfig(Mode.BEAT_7K, PlaySide.P2).getController()[0].getName()), "SDL ASYNC shares right SDL bank");
        check(restored.getPlayInputConfig(Mode.BEAT_14K, PlaySide.P2) == restored.getMode14()
                && restored.getSkinConfig(SkinType.PLAY_14KEYS, PlaySide.P2) == restored.getSkin()[SkinType.PLAY_14KEYS.getId()]
                && restored.getSkinConfig(SkinType.PLAY_7KEYS_BATTLE, PlaySide.P2) == restored.getSkin()[SkinType.PLAY_7KEYS_BATTLE.getId()],
                "DP and BATTLE skin slots are unchanged");
        var resource = KeyboardQueueTestSupport.allocate(PlayerResource.class);
        var model = new BMSModel();
        model.setMode(Mode.BEAT_7K);
        KeyboardQueueTestSupport.set(resource, PlayerResource.class, "pconfig", restored);
        KeyboardQueueTestSupport.set(resource, PlayerResource.class, "model", model);
        resource.setPlaySide(PlaySide.P2);
        resource.setPlayMode(BMSPlayerMode.PLAY);
        check(resource.getPlaySide() == PlaySide.P2, "Single play uses the locked side");
        resource.setPlayMode(BMSPlayerMode.REPLAY_1);
        check(resource.getPlaySide() == PlaySide.P1, "Legacy replays use the original bank");
        resource.setPlayMode(BMSPlayerMode.PLAY);
        model.setMode(Mode.KEYBOARD_24K_DOUBLE);
        check(resource.getPlaySide() == PlaySide.P1, "DP does not use a SP side");
        System.out.println("PlaySideConfigTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
