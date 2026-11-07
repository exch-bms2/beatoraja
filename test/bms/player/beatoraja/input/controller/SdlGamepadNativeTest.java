package bms.player.beatoraja.input.controller;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.JudgeEventQueue;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLGamepad.*;
import static org.lwjgl.sdl.SDLJoystick.*;
import static org.lwjgl.sdl.SDLEvents.*;

/** Real SDL3 mapping and native events on the real input worker, without a window or physical device. */
public final class SdlGamepadNativeTest {
    public static void main(String[] args) throws Exception {
        SdlHotplugNativeTest.run(ControllerBackend.SDL_ASYNC);
        String hint = SDLHints.SDL_GetHint(SDLHints.SDL_HINT_AUTO_UPDATE_JOYSTICKS);
        check(SDL_InitSubSystem(SDL_INIT_GAMEPAD | SDL_INIT_EVENTS), "Outer SDL3 init");
        var oldInput = Gdx.input; var oldRunner = BMSPlayerInputProcessor.controllerPollRunner;
        var posted = new ConcurrentLinkedQueue<Runnable>();
        int id = 0; long joystick = 0;
        BMSPlayerInputProcessor input = null; SdlRawControllerManager manager = null;
        try (var stack = MemoryStack.stackPush()) {
            var descriptor = SDL_VirtualJoystickDesc.calloc(stack); SDL_INIT_INTERFACE(descriptor);
            descriptor.type((short) SDL_JOYSTICK_TYPE_GAMEPAD).naxes((short) 6).nbuttons((short) 21).nhats((short) 1)
                    .button_mask((1 << 21) - 1).axis_mask(63).vendor_id((short) 0x1234).product_id((short) 0x6789)
                    .name(stack.UTF8("beatoraja mapped virtual test"));
            id = SDL_AttachVirtualJoystick(descriptor); check(id != 0, "Create virtual Gamepad");
            joystick = SDL_OpenJoystick(id); check(joystick != 0, "Open virtual joystick");
            var guid = SDL_GetJoystickGUIDForID(id, SDL_GUID.calloc(stack));
            var guidText = stack.malloc(33); SDLGUID.SDL_GUIDToString(guid, guidText);
            String mapping = org.lwjgl.system.MemoryUtil.memASCII(org.lwjgl.system.MemoryUtil.memAddress(guidText)) + ",beatoraja mapped virtual test,"
                    + "a:b3,b:b1,x:b2,y:b0,back:b4,guide:b5,start:b6,leftstick:b7,rightstick:b8,"
                    + "leftshoulder:b9,rightshoulder:b10,dpup:h0.1,dpdown:h0.4,dpleft:h0.8,dpright:h0.2,"
                    + "misc1:b15,paddle1:b16,paddle2:b17,paddle3:b18,paddle4:b19,touchpad:b20,"
                    + "leftx:a2,lefty:a1,rightx:a0,righty:a3,lefttrigger:a4,righttrigger:a5,";
            check(SDL_AddGamepadMapping(mapping) >= 0, "Custom SDL3 mapping accepted: " + SDLError.SDL_GetError());
            check(SDL_SetJoystickVirtualAxis(joystick, 4, Short.MIN_VALUE)
                    && SDL_SetJoystickVirtualAxis(joystick, 5, Short.MIN_VALUE), "Released trigger baseline");
            manager = new SdlRawControllerManager(new SdlGamepadNative(), posted::add, ControllerBackend.SDL_ASYNC);
            var device = Arrays.stream(manager.getControllers().toArray(com.badlogic.gdx.controllers.Controller.class))
                    .filter(c -> c.getName().equals("beatoraja mapped virtual test")).findFirst().orElseThrow();
            check(device.getAxisCount() == 6 && device.getMapping().buttonDpadLeft == 13, "Legacy mapped namespace");
            var player = new PlayerConfig(); player.selectControllerBackend(ControllerBackend.SDL);
            var play = player.getMode7(); Arrays.fill(play.getKeyboardConfig().getKeyAssign(), -1);
            var settings = play.getController()[0]; settings.setName(device.getName()); settings.setDuration(0);
            Arrays.fill(settings.getKeyAssign(), -1); settings.getKeyAssign()[0] = 0; settings.getKeyAssign()[1] = 28;
            settings.getKeyAssign()[2] = 29; settings.setStart(6); settings.setSelect(4);
            var shared = play.getController();
            var config = new Config(); config.setControllerBackend(ControllerBackend.SDL_ASYNC);
            Gdx.input = (Input) Proxy.newProxyInstance(SdlGamepadNativeTest.class.getClassLoader(), new Class<?>[]{Input.class},
                    (p, m, a) -> m.getReturnType() == boolean.class ? false : m.getReturnType() == int.class ? 0 : null);
            BMSPlayerInputProcessor.controllerPollRunner = SdlRawAsyncControllerTest.runner(manager);
            input = KeyboardQueueTestSupport.input(config, player, manager.getControllers());
            check(player.getMode7().getController() == shared, "Actual BMS input selection shares existing SDL bindings");
            input.setPlayConfig(player.getMode7()); input.setStartTime(1); input.beginInputJudge(null, null, false);
            check(SDL_SetJoystickVirtualButton(joystick, 3, true), "Mapped physical button 3 press");
            check(SDL_SetJoystickVirtualHat(joystick, 0, (byte) SDL_HAT_LEFTUP), "Native diagonal hat");
            check(SDL_SetJoystickVirtualAxis(joystick, 2, (short) -1), "Mapped negative one axis value");
            check(SDL_SetJoystickVirtualAxis(joystick, 4, Short.MAX_VALUE), "Fully pressed left trigger");
            input.poll();
            check(input.getKeyState(0) && input.getKeyState(1) && input.getKeyState(2), "Mapped button/D-pad reach game without raw IDs");
            check(device.getButton(0) && !device.getButton(3) && device.getButton(11) && device.getButton(13)
                    && device.getButton(28) && device.getButton(29), "D-pad canonical IDs and aliases; physical remapping respected");
            check(device.getAxis(0) == -1 / 32767f && device.getAxis(4) == 1 && device.getAxis(5) == 0, "Legacy scale and 0..1 triggers");
            check(SDL_SetJoystickVirtualButton(joystick, 3, false) && SDL_SetJoystickVirtualHat(joystick, 0, (byte) 0), "Release mapped controls");
            input.poll(); var batch = new JudgeEventQueue.Batch(); input.getJudgeEventQueue().drain(batch);
            try (var event = SDL_Event.calloc()) {
                event.type(SDL_EVENT_GAMEPAD_BUTTON_DOWN); SDL_GamepadButtonEvent.nwhich(event.address(), id);
                SDL_GamepadButtonEvent.nbutton(event.address(), (byte) SDL_GAMEPAD_BUTTON_SOUTH); SDL_GamepadButtonEvent.ndown(event.address(), true);
                check(SDL_PushEvent(event), "Queue native mapped press");
                event.type(SDL_EVENT_GAMEPAD_BUTTON_UP); SDL_GamepadButtonEvent.ndown(event.address(), false);
                check(SDL_PushEvent(event), "Queue native mapped release");
                event.type(SDL_EVENT_GAMEPAD_BUTTON_DOWN); SDL_GamepadButtonEvent.nbutton(event.address(), (byte) SDL_GAMEPAD_BUTTON_MISC2);
                SDL_GamepadButtonEvent.ndown(event.address(), true); check(SDL_PushEvent(event), "Queue unsupported new SDL3 button");
            }
            input.poll(); input.getJudgeEventQueue().drain(batch);
            check(batch.size == 2 && batch.keys[0] == 0 && batch.pressed[0] && !batch.pressed[1] && batch.times[0] == batch.times[1],
                    "Native mapped short tap preserves poll time/FIFO and ignores extra SDL3 IDs");
            try (var unrelated = SDL_Event.calloc()) { unrelated.type(SDL_EVENT_USER); check(SDL_PushEvent(unrelated), "Queue unrelated event"); }
            input.startPolling();
            check(SDL_SetJoystickVirtualButton(joystick, 3, true), "No-render native press"); awaitKey(input, 0, true);
            check(SDL_SetJoystickVirtualButton(joystick, 3, false), "No-render native release"); awaitKey(input, 0, false);
            check(SDL_SetJoystickVirtualButton(joystick, 6, true) && SDL_SetJoystickVirtualButton(joystick, 4, true), "Native START/SELECT");
            awaitControls(input, true);
            check(SDL_SetJoystickVirtualButton(joystick, 6, false) && SDL_SetJoystickVirtualButton(joystick, 4, false), "Release START/SELECT");
            awaitControls(input, false);
            var queued = (QueuedController) device;
            KeyboardQueueTestSupport.windowActivityChanged(input, false);
            check(SDL_SetJoystickVirtualButton(joystick, 3, true), "Inactive native press");
            Thread.sleep(30); check(!input.getKeyState(0), "Inactive input suppressed");
            check(SDL_SetJoystickVirtualButton(joystick, 3, false), "Inactive native release");
            long baseline = queued.inputGeneration();
            KeyboardQueueTestSupport.windowActivityChanged(input, true);
            await(() -> queued.inputGeneration() >= baseline + 2, "Focus baseline established by worker");
            check(!input.getKeyState(0), "Inactive tap cannot survive focus boundary");
            check(SDL_SetJoystickVirtualButton(joystick, 3, true), "Fresh native press after focus"); awaitKey(input, 0, true);
            check(SDL_DetachVirtualJoystick(id), "Native virtual unplug"); id = 0;
            awaitKey(input, 0, false); check(!device.isConnected(), "Native detach releases held game state");
            input.dispose(); input = null; manager.dispose(); manager = null;
            Runnable postedTask; while ((postedTask = posted.poll()) != null) postedTask.run();
            check(Objects.equals(hint, SDLHints.SDL_GetHint(SDLHints.SDL_HINT_AUTO_UPDATE_JOYSTICKS)), "Hint restored after owner shutdown");
            try (var event = SDL_Event.calloc()) {
                check(nSDL_PeepEvents(event.address(), 1, SDL_GETEVENT, SDL_EVENT_USER, SDL_EVENT_USER) == 1, "Unrelated event is not consumed");
                check(nSDL_PeepEvents(0, 0, SDL_PEEKEVENT, SDL_EVENT_JOYSTICK_AXIS_MOTION, SDL_EVENT_JOYSTICK_UPDATE_COMPLETE) == 0,
                        "Underlying raw joystick events do not accumulate");
            }
        } finally {
            if (input != null) input.dispose(); if (manager != null) manager.dispose();
            Runnable task; while ((task = posted.poll()) != null) task.run();
            if (joystick != 0) SDL_CloseJoystick(joystick); if (id != 0) SDL_DetachVirtualJoystick(id);
            SDL_QuitSubSystem(SDL_INIT_GAMEPAD | SDL_INIT_EVENTS);
            Gdx.input = oldInput; BMSPlayerInputProcessor.controllerPollRunner = oldRunner;
        }
        System.out.println("SdlGamepadNativeTest passed (actual SDL3 mapped virtual device, SDL bank, legacy IDs/scale, short tap/native POV, no-render worker, START/SELECT, focus/unplug/cleanup; NO physical hardware)");
    }
    private static void awaitKey(BMSPlayerInputProcessor input, int key, boolean down) throws Exception {
        var batch = new JudgeEventQueue.Batch();
        await(() -> { input.getJudgeEventQueue().drain(batch); return input.getKeyState(key) == down; }, "Worker key=" + key + " down=" + down);
    }
    private static void awaitControls(BMSPlayerInputProcessor input, boolean down) throws Exception {
        var batch = new JudgeEventQueue.Batch();
        await(() -> { input.getJudgeEventQueue().drain(batch); return input.startPressed() == down && input.isSelectPressed() == down; }, "Worker START/SELECT=" + down);
    }
    private static void await(java.util.function.BooleanSupplier condition, String description) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean()) { check(System.nanoTime() < deadline, description); Thread.sleep(1); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
