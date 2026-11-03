package bms.player.beatoraja.input;

import java.lang.reflect.Field;
import java.util.Arrays;

import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.Resolution;

/** Test-only constructor bypass: never initializes MIDI, IR, user databases or audio devices. */
public final class KeyboardQueueTestSupport {
    private KeyboardQueueTestSupport() {}

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
        set(input, type, "skinMouseInput", new BMSPlayerInputProcessor.SkinMouseInput());
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
