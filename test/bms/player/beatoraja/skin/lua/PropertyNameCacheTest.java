package bms.player.beatoraja.skin.lua;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;

public final class PropertyNameCacheTest {
    public static void main(String[] args) {
        PropertyNameCache<AtomicInteger> cache = new PropertyNameCache<>();
        AtomicInteger resolutions = new AtomicInteger();
        AtomicInteger property = new AtomicInteger(7);
        var resolver = (java.util.function.Function<String, AtomicInteger>) name -> {
            resolutions.incrementAndGet();
            return name.equals("日本語property") ? property : null;
        };
        LuaString key = LuaString.valueOf("日本語property");
        check(cache.resolve(key, resolver) == property, "first resolution");
        byte[] buffer = new byte[1024];
        byte[] bytes = "日本語property".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(bytes, 0, buffer, 17, bytes.length);
        LuaString distinct = LuaString.valueUsing(buffer, 17, bytes.length);
        check(key != distinct, "different string objects");
        check(cache.resolve(distinct, resolver) == property, "content equality and nonzero offset");
        property.set(42);
        check(cache.resolve(key, resolver).get() == 42, "cache property, not current value");
        check(resolutions.get() == 1, "resolve known name once");
        for (int i = 0; i < 10; i++) check(cache.resolve(LuaString.valueOf("missing"), resolver) == null, "missing property");
        check(resolutions.get() == 2, "negative cache");
        LuaTable api = new LuaTable();
        new MainStatePropertyLuaApiExporter(null).export(api);
        check(api.get("number").call(LuaString.valueOf("unknown-property")).toint() == 0, "number fallback");
        check(!api.get("option").call(LuaString.valueOf("unknown-property")).toboolean(), "option fallback");
        check(api.get("float_number").call(LuaString.valueOf("unknown-property")).todouble() == 0, "float fallback");
        check(api.get("text").call(LuaString.valueOf("unknown-property")).tojstring().isEmpty(), "text fallback");
        System.out.println("PropertyNameCacheTest passed");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
