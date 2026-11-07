package bms.player.beatoraja.input;

import bms.player.beatoraja.input.mouse.SkinMouseInput;

import bms.player.beatoraja.input.keyboard.KeyBoardInputProcesseor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.input.controller.BMControllerInputProcessor;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.utils.Array;

/** Test-only constructor bypass: never initializes MIDI, IR, user databases or audio devices. */
public final class KeyboardQueueTestSupport {
    private KeyboardQueueTestSupport() {}

    public static BMSPlayerInputProcessor input(Config config, PlayerConfig player, Array<Controller> controllers) {
        return new BMSPlayerInputProcessor(config, player, controllers, owner -> new MidiInputProcessor(owner, List.of()));
    }

    public static void windowActivityChanged(BMSPlayerInputProcessor input, boolean active) {
        input.windowActivityChanged(active);
    }

    public static boolean matchesControllerName(String configured, String detected) {
        return BMSPlayerInputProcessor.matchesControllerName(configured, detected);
    }

    public static JudgeEventQueue judgeQueue(int capacity) {
        return new JudgeEventQueue(capacity);
    }

    public static KeyboardJudgeQueue keyboardJudgeQueue(int capacity) {
        return new KeyboardJudgeQueue(capacity);
    }

    public static InputDiagnostics inputDiagnostics() { return new InputDiagnostics(false); }

    public static PollingLwjgl3Input windowsKeyboardInput(com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window window) {
        return new PollingLwjgl3Input(window, null, true, true);
    }

    public static void includeJudgeKeys(JudgeEventQueue queue, int[] keys, boolean[] state) {
        queue.include(keys, state);
    }

    public static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafeClass.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    public static void set(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    public static BMSPlayerInputProcessor input(KeyboardConfig config) throws Exception {
        var input = allocate(BMSPlayerInputProcessor.class);
        Class<?> type = BMSPlayerInputProcessor.class;
        set(input, type, "enable", true);
        set(input, type, "keystate", new boolean[256]);
        long[] times = new long[256];
        Arrays.fill(times, Long.MIN_VALUE);
        set(input, type, "time", times);
        set(input, type, "keyboardJudgeQueue", new KeyboardJudgeQueue());
        set(input, type, "skinMouseInput", new SkinMouseInput());
        set(input, type, "keyboardReplayKeys", new int[0]);
        set(input, type, "isAnalog", new boolean[256]);
        set(input, type, "currentAnalogValue", new float[256]);
        Class<?> logger = Class.forName(type.getName() + "$KeyLogger");
        var constructor = logger.getDeclaredConstructor();
        constructor.setAccessible(true);
        set(input, type, "keylog", constructor.newInstance());
        set(input, type, "bminput", new BMControllerInputProcessor[0]);
        set(input, type, "midiinput", allocate(MidiInputProcessor.class));
        set(input, type, "kbinput", new KeyBoardInputProcesseor(input, config, Resolution.HD));
        set(input, type, "starttime", 1L);
        return input;
    }
}
