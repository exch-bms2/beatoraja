package bms.player.beatoraja.input;

import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayModeConfig.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLJoystick.*;
import static org.lwjgl.sdl.SDLEvents.*;

/** Actual bundled SDL3 and a virtual raw joystick; no physical hardware or user settings. */
public final class SdlRawNativeTest {
    public static void main(String[] args) throws Exception {
        boolean async = Arrays.asList(args).contains("--async");
        String previousAutoUpdate = SDLHints.SDL_GetHint(SDLHints.SDL_HINT_AUTO_UPDATE_JOYSTICKS);
        check(SDL_InitSubSystem(SDL_INIT_JOYSTICK | SDL_INIT_EVENTS), "SDL3 init: " + SDLError.SDL_GetError());
        int id = 0; long joystick = 0;
        var previousInput = Gdx.input; Runnable previousRunner = BMSPlayerInputProcessor.controllerPollRunner;
        SdlRawControllerManager manager = null; BMSPlayerInputProcessor input = null;
        try (var stack = MemoryStack.stackPush()) {
            var descriptor = SDL_VirtualJoystickDesc.calloc(stack);
            SDL_INIT_INTERFACE(descriptor);
            descriptor.type((short) SDL_JOYSTICK_TYPE_ARCADE_STICK).naxes((short) 4).nbuttons((short) 32).nhats((short) 1)
                    .vendor_id((short) 0x1234).product_id((short) 0x5678).name(stack.UTF8("beatoraja raw virtual test"));
            id = SDL_AttachVirtualJoystick(descriptor);
            check(id != 0, "Virtual joystick creation: " + SDLError.SDL_GetError());
            joystick = SDL_OpenJoystick(id); check(joystick != 0, "Open virtual joystick");
            var posted = new java.util.concurrent.ConcurrentLinkedQueue<Runnable>();
            var source = new SdlRawNative(async);
            manager = new SdlRawControllerManager(source, posted::add, async);
            if (async) check("0".equals(SDLHints.SDL_GetHint(SDLHints.SDL_HINT_AUTO_UPDATE_JOYSTICKS)), "Manual SDL3 updates in async mode");
            var device = Arrays.stream(manager.getControllers().toArray(com.badlogic.gdx.controllers.Controller.class))
                    .filter(c -> c.getName().equals("beatoraja raw virtual test")).findFirst().orElseThrow();
            check(device.getMaxButtonIndex() == 31 && device.getAxisCount() == 8, "Legacy raw namespace");
            var player = new PlayerConfig();
            var play = player.getMode7(); Arrays.fill(play.getKeyboardConfig().getKeyAssign(), -1);
            var binding = play.getController()[0]; binding.setName(device.getName()); binding.setDuration(0);
            Arrays.fill(binding.getKeyAssign(), -1); binding.getKeyAssign()[0] = 6; binding.getKeyAssign()[1] = 28;
            binding.setStart(8); binding.setSelect(9);
            var config = new Config(); config.setControllerBackend(async ? ControllerBackend.SDL_RAW_ASYNC : ControllerBackend.SDL_RAW);
            Gdx.input = (Input) Proxy.newProxyInstance(SdlRawNativeTest.class.getClassLoader(), new Class<?>[]{Input.class},
                    (p, m, a) -> m.getReturnType() == boolean.class ? false : m.getReturnType() == int.class ? 0 : null);
            var field = SdlRawControllerManager.class.getDeclaredField("pollRunner"); field.setAccessible(true);
            BMSPlayerInputProcessor.controllerPollRunner = (Runnable) field.get(manager);
            input = new BMSPlayerInputProcessor(config, player, manager.getControllers(), owner -> new MidiInputProcessor(owner, List.of()));
            input.setPlayConfig(player.getMode7()); input.setStartTime(1); input.beginInputJudge(null, null, false);
            check(player.getMode7().getController()[0].getKeyAssign()[0] == 6, "Same-name JInput settings activated automatically");
            check(SDL_SetJoystickVirtualButton(joystick, 6, true), "Set native button");
            check(SDL_SetJoystickVirtualHat(joystick, 0, (byte) SDL_HAT_LEFTUP), "Set native POV");
            check(SDL_SetJoystickVirtualAxis(joystick, 3, (short) -32768), "Set native fourth axis");
            poll(manager, input, async);
            check(device.getButton(6) && device.getButton(28) && device.getButton(29) && device.getAxis(3) == -1,
                    "Actual SDL raw button index, diagonal POV and axis normalization");
            check(input.getKeyState(0) && input.getKeyState(1), "Real input processor selected inherited raw bank");
            check(SDL_SetJoystickVirtualButton(joystick, 6, false), "Release button");
            check(SDL_SetJoystickVirtualHat(joystick, 0, (byte) 0), "Release POV");
            poll(manager, input, async);
            var batch = new JudgeEventQueue.Batch(); input.getJudgeEventQueue().drain(batch);
            check(!input.getKeyState(0) && !input.getKeyState(1), "Real SDL releases reach FIFO consumer");
            check(SDL_SetJoystickVirtualButton(joystick, 8, true) && SDL_SetJoystickVirtualButton(joystick, 9, true), "Press inherited START/SELECT");
            poll(manager, input, async);
            check(input.startPressed() && input.isSelectPressed(), "Inherited START/SELECT use raw button IDs");
            check(SDL_SetJoystickVirtualButton(joystick, 8, false) && SDL_SetJoystickVirtualButton(joystick, 9, false), "Release inherited START/SELECT");
            poll(manager, input, async);
            check(!input.startPressed() && !input.isSelectPressed(), "START/SELECT release is preserved");
            input.getJudgeEventQueue().drain(batch);
            // SDL timestamps are deliberately not the beatoraja clock. Both edges must survive one producer pump.
            try (var event = SDL_Event.calloc()) {
                event.type(SDL_EVENT_JOYSTICK_BUTTON_DOWN);
                SDL_JoyButtonEvent.nwhich(event.address(), id); SDL_JoyButtonEvent.nbutton(event.address(), (byte) 6);
                SDL_JoyButtonEvent.ndown(event.address(), true);
                check(SDL_PushEvent(event), "Native event press enqueue");
                event.type(SDL_EVENT_JOYSTICK_BUTTON_UP); SDL_JoyButtonEvent.ndown(event.address(), false);
                check(SDL_PushEvent(event), "Native event release enqueue");
            }
            poll(manager, input, async); input.getJudgeEventQueue().drain(batch);
            check(batch.size == 2 && batch.pressed[0] && !batch.pressed[1] && batch.times[0] == batch.times[1],
                    "Native SDL event FIFO captures short tap with existing poll timestamp");
            if (async) {
                // It must not consume events owned by another SDL subsystem.
                try (var unrelated = SDL_Event.calloc()) {
                    unrelated.type(SDL_EVENT_USER); check(SDL_PushEvent(unrelated), "Unrelated native event enqueue");
                }
            }
            input.startPolling();
            if (async) {
                check(SDL_SetJoystickVirtualButton(joystick, 6, true), "Async native press");
                awaitKey(input, true);
                check(SDL_SetJoystickVirtualButton(joystick, 6, false), "Async native release");
                awaitKey(input, false);
                // Focus changes and native polling are concurrent, with no render callbacks.
                input.windowActivityChanged(false);
                check(SDL_SetJoystickVirtualButton(joystick, 6, true), "Inactive native press");
                Thread.sleep(30); check(!input.getKeyState(0), "Inactive worker cannot forward game input");
                check(SDL_SetJoystickVirtualButton(joystick, 6, false), "Inactive native release");
                Thread.sleep(30); input.windowActivityChanged(true); Thread.sleep(30);
                check(!input.getKeyState(0), "Focus regain does not replay an old native tap");
                check(SDL_SetJoystickVirtualButton(joystick, 6, true), "Fresh press after regain"); awaitKey(input, true);
                check(SDL_SetJoystickVirtualButton(joystick, 6, false), "Fresh release after regain"); awaitKey(input, false);
            }
            input.dispose(); input = null;
            manager.dispose(); manager = null;
            Runnable task; while ((task = posted.poll()) != null) task.run();
            if (async) {
                check(java.util.Objects.equals(previousAutoUpdate, SDLHints.SDL_GetHint(SDLHints.SDL_HINT_AUTO_UPDATE_JOYSTICKS)), "Native shutdown restores SDL3 update hint");
                try (var unrelated = SDL_Event.calloc()) {
                    check(nSDL_PeepEvents(unrelated.address(), 1, SDL_GETEVENT, SDL_EVENT_USER, SDL_EVENT_USER) == 1,
                            "Async acquisition leaves non-joystick events untouched");
                }
            }
            check(SDL_JoystickConnected(joystick), "Raw backend only releases its own SDL subsystem reference");
        } finally {
            if (input != null) input.dispose();
            if (manager != null) manager.dispose();
            if (joystick != 0) SDL_CloseJoystick(joystick);
            if (id != 0) SDL_DetachVirtualJoystick(id);
            SDL_QuitSubSystem(SDL_INIT_JOYSTICK | SDL_INIT_EVENTS);
            Gdx.input = previousInput; BMSPlayerInputProcessor.controllerPollRunner = previousRunner;
        }
        System.out.println("SdlRawNativeTest passed (async=" + async + ", actual SDL3 virtual raw joystick, same-name import, buttons/POV/axis/native FIFO, poll thread and cleanup; NO physical hardware)");
        SdlHotplugNativeTest.run(async ? ControllerBackend.SDL_RAW_ASYNC : ControllerBackend.SDL_RAW);
    }
    private static void poll(SdlRawControllerManager manager, BMSPlayerInputProcessor input, boolean async) {
        if (!async) manager.poll();
        input.poll();
    }
    private static void awaitKey(BMSPlayerInputProcessor input, boolean down) throws Exception {
        var batch = new JudgeEventQueue.Batch();
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
        do {
            input.getJudgeEventQueue().drain(batch);
            if (input.getKeyState(0) == down) return;
            Thread.sleep(1);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("No-render native input worker did not deliver pressed=" + down);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
