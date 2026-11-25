package bms.player.beatoraja.skin.lua;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import bms.player.beatoraja.skin.property.StringProperty;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;

/** Text caching must reuse encoding while still evaluating every property and its updates. */
public final class LuaTextCacheTest {
    public static void main(String[] args) {
        LuaTextCache cache = new LuaTextCache();
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> current = new AtomicReference<>("日本語タイトル / 🎵 / " + "long text ".repeat(20));
        StringProperty property = state -> { calls.incrementAndGet(); return current.get(); };
        LuaString first = cache.encode(property, property.get(null));
        check(first.tojstring().equals(current.get()), "UTF-8 content");
        check(first == cache.encode(property, property.get(null)), "same result");
        current.set(new String(current.get()));
        check(first == cache.encode(property, property.get(null)), "equal content in distinct Java string");
        check(calls.get() == 3, "getter still runs every time");
        current.set("changed");
        LuaString changed = cache.encode(property, property.get(null));
        check(changed.tojstring().equals("changed") && first.tojstring().startsWith("日本語"), "change without mutating old result");
        current.set(null); check(cache.encode(property, property.get(null)).tojstring().isEmpty(), "null becomes empty");
        current.set(""); check(cache.encode(property, property.get(null)).tojstring().isEmpty(), "empty value");
        StringProperty other = state -> "other";
        check(cache.encode(other, other.get(null)).tojstring().equals("other"), "independent property");
        current.set("back"); check(cache.encode(property, property.get(null)).tojstring().equals("back"), "restore after empty");
        for (String value : new String[]{"ASCII", "日本語", "a\0b", "\uD800", "\uDC00", "\uD83C\uDFB5"}) {
            check(cache.encode(property, value).raweq(LuaString.valueOf(value)), "existing encoding semantics");
        }
        check(cache.encode(null, null).tojstring().isEmpty(), "unknown property");
        LuaTable api = new LuaTable(); new MainStatePropertyLuaApiExporter(null).export(api);
        check(api.get("text").call(LuaString.valueOf("unknown-property")).tojstring().isEmpty(), "API fallback");
        // Identity is important: custom property implementations may compare equal.
        StringProperty a = new EqualProperty(), b = new EqualProperty();
        check(cache.encode(a, "a").tojstring().equals("a"), "identity a");
        check(cache.encode(b, "b").tojstring().equals("b"), "identity b");
        check(cache.encode(a, "a").tojstring().equals("a"), "equal properties remain distinct");
        System.out.println("LuaTextCacheTest passed");
    }
    private static final class EqualProperty implements StringProperty {
        public String get(bms.player.beatoraja.MainState state) { return ""; }
        @Override public boolean equals(Object o) { return o instanceof EqualProperty; }
        @Override public int hashCode() { return 1; }
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
