package bms.player.beatoraja.skin.lua;

import org.luaj.vm2.LuaNumber;

/** Avoid repeatedly boxing the immutable timer-off sentinel; active timestamps stay live. */
final class LuaTimerValue {
    private static final LuaNumber OFF = LuaNumber.valueOf(Long.MIN_VALUE);

    private LuaTimerValue() {}

    static LuaNumber valueOf(long microTime) {
        return microTime == Long.MIN_VALUE ? OFF : LuaNumber.valueOf(microTime);
    }
}
