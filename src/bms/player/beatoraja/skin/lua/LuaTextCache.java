package bms.player.beatoraja.skin.lua;

import java.util.IdentityHashMap;
import java.util.Map;
import bms.player.beatoraja.skin.property.StringProperty;
import org.luaj.vm2.LuaString;

/** One current UTF-8 result per property, scoped to one main_state.text API instance. */
final class LuaTextCache {
    private static final LuaString EMPTY = LuaString.valueOf("");
    private final Map<StringProperty, Entry> values = new IdentityHashMap<>();

    LuaString encode(StringProperty property, String text) {
        if (property == null) return EMPTY;
        String current = text != null ? text : "";
        Entry entry = values.get(property);
        if (entry == null) {
            entry = new Entry();
            values.put(property, entry);
        } else if (current.equals(entry.text)) {
            return entry.encoded;
        }
        // Replace rather than accumulate values when the song, ranking or input changes.
        entry.encoded = current.isEmpty() ? EMPTY : LuaString.valueOf(current);
        entry.text = current;
        return entry.encoded;
    }

    private static final class Entry {
        String text;
        LuaString encoded;
    }
}
