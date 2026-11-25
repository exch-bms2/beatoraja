package bms.player.beatoraja.skin.lua;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.ZeroArgFunction;
import bms.player.beatoraja.skin.property.StringProperty;

/** Cached decoding must never suppress Lua calls, failures or non-string conversions. */
public final class LuaStringResultCacheTest {
    public static void main(String[] args) throws Exception {
        LuaStringResultCache cache = new LuaStringResultCache();
        LuaString text = LuaString.valueOf("日本語 / 🎵 / " + "long text ".repeat(20));
        String first = cache.decode(text);
        check(first.equals(text.tojstring()), "UTF-8 decoding");
        check(first == cache.decode(text), "same LuaString reuses decoded String");
        LuaString second = LuaString.valueOf("different " + "long text ".repeat(20));
        check(cache.decode(second).equals(second.tojstring()), "changed LuaString");
        for (LuaValue value : new LuaValue[]{LuaValue.NIL, LuaValue.TRUE, LuaInteger.valueOf(1000),
                LuaDouble.valueOf(1.25), LuaString.valueOf(new byte[]{(byte)0xff,0,(byte)0xc0}), text})
            check(cache.decode(value).equals(value.tojstring()), "same conversion as before");
        AtomicInteger conversions = new AtomicInteger();
        LuaValue custom = new LuaTable() { @Override public String tojstring() { return "value" + conversions.incrementAndGet(); } };
        check(cache.decode(custom).equals("value1") && cache.decode(custom).equals("value2"), "custom values are not cached");
        SkinLuaAccessor lua = new SkinLuaAccessor(true);
        try {
            AtomicInteger calls = new AtomicInteger();
            AtomicReference<LuaValue> result = new AtomicReference<>(text);
            StringProperty property = lua.loadStringProperty(new ZeroArgFunction() {
                @Override public LuaValue call() { calls.incrementAndGet(); return result.get(); }
            });
            String returned = property.get(null);
            check(returned == property.get(null) && calls.get() == 2, "Lua function still called every time");
            result.set(second); check(property.get(null).equals(second.tojstring()), "live Lua result");
            result.set(custom); check(!property.get(null).equals(property.get(null)), "dynamic custom conversion");
            AtomicInteger attempts = new AtomicInteger();
            StringProperty failing = lua.loadStringProperty(new ZeroArgFunction() {
                @Override public LuaValue call() {
                    if (attempts.incrementAndGet() == 2) throw new LuaError("expected test failure");
                    return text;
                }
            });
            check(failing.get(null).equals(first), "before failure");
            check(failing.get(null).isEmpty(), "failure keeps empty-string fallback");
            check(failing.get(null).equals(first) && attempts.get() == 3, "recovery still calls Lua");
        } finally { lua.dispose(); }
        concurrentDecoding();
        System.out.println("LuaStringResultCacheTest passed");
    }

    private static void concurrentDecoding() throws Exception {
        LuaStringResultCache shared = new LuaStringResultCache();
        var executor = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            var tasks = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 4; i++) {
                String expected = "thread" + i + " repeated text ".repeat(20);
                LuaString value = LuaString.valueOf(expected);
                tasks.add(executor.submit(() -> {
                    for (int n = 0; n < 10000; n++) check(shared.decode(value).equals(expected), "concurrent decode");
                }));
            }
            for (var task : tasks) task.get();
        } finally { executor.shutdownNow(); }
    }

    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
