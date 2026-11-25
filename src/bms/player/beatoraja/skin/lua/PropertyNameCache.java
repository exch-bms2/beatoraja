package bms.player.beatoraja.skin.lua;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

/** Caches name-to-property resolution, including missing properties, for one API exporter. */
final class PropertyNameCache<T> {
    private final Map<LuaString, Optional<T>> properties = new HashMap<>();

    T resolve(LuaValue value, Function<String, T> resolver) {
        // Nonstandard LuaValue subclasses retain their existing tojstring conversion.
        LuaString key = value instanceof LuaString string ? string : LuaString.valueOf(value.tojstring());
        Optional<T> cached = properties.get(key);
        if (cached == null) {
            cached = Optional.ofNullable(resolver.apply(value.tojstring()));
            // Own only the key bytes; a substring may refer to a much larger source buffer.
            properties.put(LuaString.valueOf(key.m_bytes, key.m_offset, key.m_length), cached);
        }
        return cached.orElse(null);
    }
}
