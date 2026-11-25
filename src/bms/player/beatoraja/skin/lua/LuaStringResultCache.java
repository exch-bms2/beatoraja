package bms.player.beatoraja.skin.lua;

import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

/** Reuses only the last immutable LuaString's Java representation, after each Lua call. */
final class LuaStringResultCache {
    // Final fields keep the value/result pair consistent even if callers overlap.
    private Entry previous;

    String decode(LuaValue value) {
        if (value instanceof LuaString string) {
            Entry cached = previous;
            if (cached != null && cached.value == string) return cached.decoded;
            String result = string.tojstring();
            previous = new Entry(string, result);
            return result;
        }
        // Other LuaValues may implement stateful tojstring(); never memoize those.
        previous = null;
        return value.tojstring();
    }

    private static final class Entry {
        final LuaString value;
        final String decoded;
        Entry(LuaString value, String decoded) {
            this.value = value;
            this.decoded = decoded;
        }
    }
}
