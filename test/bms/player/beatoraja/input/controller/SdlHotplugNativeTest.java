package bms.player.beatoraja.input.controller;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.JudgeEventQueue;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;

import bms.player.beatoraja.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLJoystick.*;

/** Actual SDL3 late attach/unplug/replug, including workers running without render callbacks. */
public final class SdlHotplugNativeTest {
    static void run(ControllerBackend backend) throws Exception {
        boolean mapped = backend == ControllerBackend.SDL_ASYNC;
        boolean async = backend != ControllerBackend.SDL_RAW;
        int subsystems = SDL_INIT_JOYSTICK | SDL_INIT_EVENTS;
        check(SDL_InitSubSystem(subsystems), "Outer native hotplug init");
        var previousInput = Gdx.input;
        var previousRunner = BMSPlayerInputProcessor.controllerPollRunner;
        var posted = new ConcurrentLinkedQueue<Runnable>();
        SdlRawControllerManager manager = null;
        BMSPlayerInputProcessor input = null;
        long joystick = 0;
        int id = 0;
        try {
            String name = "beatoraja late hotplug " + backend;
            manager = new SdlRawControllerManager(mapped ? new SdlGamepadNative() : new SdlRawNative(async), posted::add, backend);
            var player = new PlayerConfig(); player.selectControllerBackend(backend);
            var play = player.getMode7(); Arrays.fill(play.getKeyboardConfig().getKeyAssign(), -1);
            var settings = play.getController()[0]; settings.setName(name);
            Arrays.fill(settings.getKeyAssign(), -1); settings.getKeyAssign()[0] = 0;
            settings.setStart(6); settings.setSelect(4); settings.setDuration(100);
            var config = new Config(); config.setControllerBackend(backend);
            Gdx.input = (Input) Proxy.newProxyInstance(SdlHotplugNativeTest.class.getClassLoader(), new Class<?>[]{Input.class},
                    (p, m, a) -> m.getReturnType() == boolean.class ? false : m.getReturnType() == int.class ? 0 : null);
            BMSPlayerInputProcessor.controllerPollRunner = SdlRawAsyncControllerTest.runner(manager);
            input = KeyboardQueueTestSupport.input(config, player, manager.getControllers());
            input.setPlayConfig(play); input.setStartTime(1); input.beginInputJudge(null, null, false);
            if (async) input.startPolling();
            final var activeManager = manager;
            final var activeInput = input;
            BMControllerInputProcessor original = null;
            int firstId = 0;
            for (int cycle = 0; cycle < 3; cycle++) {
                try (var stack = MemoryStack.stackPush()) {
                    var descriptor = SDL_VirtualJoystickDesc.calloc(stack); SDL_INIT_INTERFACE(descriptor);
                    descriptor.type((short) (mapped ? SDL_JOYSTICK_TYPE_GAMEPAD : SDL_JOYSTICK_TYPE_ARCADE_STICK))
                            .naxes((short) 6).nbuttons((short) 21).nhats((short) 1)
                            .button_mask((1 << 21) - 1).axis_mask(63)
                            .vendor_id((short) 0x1234).product_id((short) 0x7788).name(stack.UTF8(name));
                    id = SDL_AttachVirtualJoystick(descriptor);
                    check(id != 0, "Attach after input startup: " + SDLError.SDL_GetError());
                }
                if (cycle == 0) firstId = id;
                else check(id != firstId, "Reconnection changes SDL instance ID");
                joystick = SDL_OpenJoystick(id); check(joystick != 0, "Open test-owned virtual joystick");
                await(activeManager, activeInput, async, () -> Arrays.stream(activeInput.getBMInputProcessor())
                        .anyMatch(p -> p.getName().equals(name) && p.getController().isConnected()), "Late attachment reaches live BMS processor");
                var processor = Arrays.stream(input.getBMInputProcessor()).filter(p -> p.getName().equals(name)).findFirst().orElseThrow();
                if (original == null) original = processor;
                else check(processor == original, "Replug restores original binding slot and processor");
                check(Arrays.stream(input.getBMInputProcessor()).filter(p -> p.getName().startsWith(name)).count() == 1,
                        "Repeated reconnect does not accumulate duplicate device slots");
                var batch = new JudgeEventQueue.Batch(); input.getJudgeEventQueue().drain(batch);
                check(SDL_SetJoystickVirtualButton(joystick, 0, true)
                        && SDL_SetJoystickVirtualButton(joystick, 6, true)
                        && SDL_SetJoystickVirtualButton(joystick, 4, true), "Press saved game/START/SELECT buttons");
                await(activeManager, activeInput, async, () -> activeInput.getKeyState(0)
                        && activeInput.startPressed() && activeInput.isSelectPressed(), "Restored bindings work after attachment");
                input.getJudgeEventQueue().drain(batch);
                check(SDL_DetachVirtualJoystick(id), "Unplug while all controls are held"); id = 0;
                await(activeManager, activeInput, async, () -> !activeInput.getKeyState(0)
                        && !activeInput.startPressed() && !activeInput.isSelectPressed(), "Unplug releases all controls");
                input.getJudgeEventQueue().drain(batch);
                check(batch.size == 1 && batch.keys[0] == 0 && !batch.pressed[0], "Native unplug sends one release through judgment FIFO");
                SDL_CloseJoystick(joystick); joystick = 0;
            }
        } finally {
            if (input != null) input.dispose();
            if (manager != null) manager.dispose();
            Runnable task; while ((task = posted.poll()) != null) task.run();
            if (joystick != 0) SDL_CloseJoystick(joystick);
            if (id != 0) SDL_DetachVirtualJoystick(id);
            Gdx.input = previousInput; BMSPlayerInputProcessor.controllerPollRunner = previousRunner;
            SDL_QuitSubSystem(subsystems);
        }
        System.out.println("SdlHotplugNativeTest passed (" + backend + ", actual SDL3 late attach, three replug cycles, saved controls and judgment releases)");
    }

    private static void await(SdlRawControllerManager manager, BMSPlayerInputProcessor input, boolean async,
            BooleanSupplier condition, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        do {
            if (!async) { manager.poll(); input.poll(); }
            if (condition.getAsBoolean()) return;
            Thread.sleep(1);
        } while (System.nanoTime() < deadline);
        throw new AssertionError(message);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
