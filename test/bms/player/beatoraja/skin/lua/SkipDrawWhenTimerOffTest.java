package bms.player.beatoraja.skin.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.TimerManager;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.BooleanProperty;
import bms.player.beatoraja.skin.property.TimerProperty;

/**
 * Default-on skipping of draw conditions while an object's timer is definitely off:
 * the visible result must equal evaluating them, and stateful or impure code must still run.
 */
public final class SkipDrawWhenTimerOffTest {
    public static void main(String[] args) throws Exception {
        // On by default; the test must run without an opt-out.
        check(System.getProperty("beatoraja.skin.skipDrawWhenTimerOff") == null, "test must run without the property");
        State state = KeyboardQueueTestSupport.allocate(State.class);
        KeyboardQueueTestSupport.set(state, MainState.class, "timer", new TimerManager());

        skinObjectSkipsOnlyWhenSafe(state);
        observeTimerProbe(state);
        sideEffectCheck();
        System.out.println("SkipDrawWhenTimerOffTest passed");
    }

    private static void skinObjectSkipsOnlyWhenSafe(State state) {
        for (boolean timerOff : new boolean[] {true, false}) {
            for (boolean probeable : new boolean[] {true, false}) {
                for (boolean pure : new boolean[] {true, false}) {
                    for (boolean condition : new boolean[] {true, false}) {
                        Counting draw = new Counting(condition, pure);
                        Probe object = new Probe();
                        object.setDestination(0, 0, 0, 10, 10, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0,
                                new FakeTimer(timerOff, probeable), new int[0]);
                        object.setDrawCondition(new BooleanProperty[] {draw});
                        object.prepare(0, state);
                        String c = "timerOff=" + timerOff + " probeable=" + probeable + " pure=" + pure + " condition=" + condition;
                        boolean expectedVisible = condition && !timerOff;
                        check(object.draw == expectedVisible, "visible " + c);
                        boolean mayskip = timerOff && probeable && pure;
                        check(draw.calls == (mayskip ? 0 : 1), "condition calls " + draw.calls + " " + c);
                    }
                }
            }
        }
    }

    private static void observeTimerProbe(State state) {
        SkinLuaAccessor lua = new SkinLuaAccessor(true);
        try {
            Globals g = JsePlatform.standardGlobals();
            LuaTable util = new LuaTable();
            new TimerUtility(state).export(util);
            g.set("timer_util", util);
            g.set("open", LuaValue.FALSE);
            LuaFunction observer = g.load("return timer_util.timer_observe_boolean(function() return open end)").call().checkfunction();
            TimerProperty timer = lua.loadTimerProperty(observer);

            check(timer.isDefinitelyOff(state), "off and observed false");
            check(timer.getMicro(state) == Long.MIN_VALUE, "still off after evaluation");
            g.set("open", LuaValue.TRUE);
            check(!timer.isDefinitelyOff(state), "observed true: not definitely off");
            check(timer.getMicro(state) != Long.MIN_VALUE, "turns on when evaluated");
            g.set("open", LuaValue.FALSE);
            check(!timer.isDefinitelyOff(state), "on: evaluating would turn it off, so not skippable");
            check(timer.getMicro(state) == Long.MIN_VALUE, "turns off when evaluated");
            check(timer.isDefinitelyOff(state), "off again");

            LuaFunction impureObserver = g.load(
                    "return timer_util.timer_observe_boolean(function() count = (count or 0) + 1 return false end)").call().checkfunction();
            TimerProperty impure = lua.loadTimerProperty(impureObserver);
            check(!impure.isDefinitelyOff(state), "observed function with side effects is never probed");
            check(g.get("count").isnil(), "probe did not call it");

            LuaFunction passive = g.load("return timer_util.new_passive_timer()").call().get("timer").checkfunction();
            check(lua.loadTimerProperty(passive).isDefinitelyOff(state), "passive timer starts off");

            LuaFunction plain = g.load("return function() return -9223372036854775807 - 1 end").call().checkfunction();
            check(!lua.loadTimerProperty(plain).isDefinitelyOff(state), "arbitrary Lua timers are not probed");
        } finally {
            lua.dispose();
        }
    }

    private static void sideEffectCheck() {
        Globals g = JsePlatform.standardGlobals();
        SkinLuaAccessor lua = new SkinLuaAccessor(true);
        try {
            String[] pure = {
                    "return function() return a and b end",
                    "local u = 1 return function() local x = u + 1 return x > 1 end",
                    "return function() return math.floor(1.5) == 1 end"};
            String[] impure = {
                    "return function() counter = (counter or 0) + 1 return true end",
                    "local u = 0 return function() u = u + 1 return true end",
                    "local t = {} return function() t.x = 1 return true end"};
            for (String s : pure)
                check(lua.loadBooleanProperty(g.load(s).call().checkfunction()).hasNoSideEffects(), "pure: " + s);
            for (String s : impure)
                check(!lua.loadBooleanProperty(g.load(s).call().checkfunction()).hasNoSideEffects(), "impure: " + s);
        } finally {
            lua.dispose();
        }
    }

    private static final class Counting implements BooleanProperty {
        final boolean value, pure;
        int calls;
        Counting(boolean value, boolean pure) { this.value = value; this.pure = pure; }
        @Override public boolean isStatic(MainState state) { return false; }
        @Override public boolean get(MainState state) { calls++; return value; }
        @Override public boolean hasNoSideEffects() { return pure; }
    }

    private static final class FakeTimer implements TimerProperty {
        final boolean off, probeable;
        FakeTimer(boolean off, boolean probeable) { this.off = off; this.probeable = probeable; }
        @Override public long getMicro(MainState state) { return off ? Long.MIN_VALUE : 0; }
        @Override public boolean isDefinitelyOff(MainState state) { return probeable && off; }
    }

    private static final class Probe extends SkinObject {
        @Override public void draw(SkinObjectRenderer renderer) { }
        @Override public void dispose() { }
    }

    private static final class State extends MainState {
        private State() { super(null, null); }
        @Override public void create() { }
        @Override public void render() { }
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
