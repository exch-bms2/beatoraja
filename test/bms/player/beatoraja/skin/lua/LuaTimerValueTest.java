package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.TimerManager;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;

/** Timer boxing must preserve numeric conversion, off comparisons and live state transitions. */
public final class LuaTimerValueTest {
    public static void main(String[] args) throws Exception {
        for (long value : new long[]{Long.MIN_VALUE, Long.MIN_VALUE + 1, -1000, -256, -1, 0, 255, 256,
                1234567890L, (1L << 53) + 1, Long.MAX_VALUE}) {
            LuaNumber expected = LuaNumber.valueOf(value), actual = LuaTimerValue.valueOf(value);
            check(expected.getClass() == actual.getClass(), "number class " + value);
            check(expected.raweq(actual), "number equality " + value);
            check(expected.tolong() == actual.tolong(), "long conversion " + value);
            check(expected.tojstring().equals(actual.tojstring()), "string conversion " + value);
        }
        check(LuaTimerValue.valueOf(Long.MIN_VALUE) == LuaTimerValue.valueOf(Long.MIN_VALUE), "shared off value");
        State state = KeyboardQueueTestSupport.allocate(State.class);
        Clock clock = new Clock();
        KeyboardQueueTestSupport.set(state, MainState.class, "timer", clock);
        LuaTable api = new LuaTable(); new MainStatePropertyLuaApiExporter(state).export(api);
        LuaTable util = new LuaTable(); new TimerUtility(state).export(util);
        LuaValue timer = api.get("timer"), id = LuaInteger.valueOf(1);
        LuaValue timerFunction = util.get("timer_function").call(id);
        clock.setTimerOff(1);
        LuaValue off = timer.call(id);
        check(off == timer.call(id) && off == timerFunction.call(), "shared off across APIs");
        check(off.raweq(api.get("timer_off_value")), "off sentinel equality");
        clock.setMicroTimer(1, 1234567890L);
        check(timer.call(id).tolong() == 1234567890L && timerFunction.call().tolong() == 1234567890L, "live timestamp");
        clock.setTimerOff(1); check(timer.call(id) == off, "off again");
        Globals g = JsePlatform.standardGlobals();
        Varargs functions = g.load("local on = false return function() return on end, function(v) on = v end").invoke();
        LuaValue observed = util.get("timer_observe_boolean").call(functions.arg1());
        check(observed.call() == off, "observer starts off");
        functions.arg(2).call(LuaValue.TRUE); clock.now = 987654321L;
        check(observed.call().tolong() == clock.now, "observer turns on");
        clock.now++; check(observed.call().tolong() == 987654321L, "observer preserves start");
        functions.arg(2).call(LuaValue.FALSE); check(observed.call() == off, "observer turns off");
        LuaValue passive = util.get("new_passive_timer").call();
        check(passive.get("timer").call() == off, "passive starts off");
        passive.get("turn_on").call(); check(passive.get("timer").call().tolong() == clock.now, "passive on");
        passive.get("turn_off").call(); check(passive.get("timer").call() == off, "passive off");
        System.out.println("LuaTimerValueTest passed");
    }
    private static final class Clock extends TimerManager {
        long now;
        @Override public long getNowMicroTime() { return now; }
    }
    private static final class State extends MainState {
        State() { super(null, null); }
        @Override public void create() {}
        @Override public void render() {}
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
