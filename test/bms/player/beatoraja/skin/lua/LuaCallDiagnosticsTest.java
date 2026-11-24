package bms.player.beatoraja.skin.lua;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

/** Opt-in Lua call diagnostics: disabled call sites are free, enabled visits produce reports. */
public final class LuaCallDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        testDisabledByDefault();
        testVisitReport(LuaCallDiagnostics.Mode.TIME);
        testVisitReport(LuaCallDiagnostics.Mode.ALLOC);
        testVisitReport(LuaCallDiagnostics.Mode.COUNT);
        System.out.println("LuaCallDiagnosticsTest passed");
    }

    private static void testDisabledByDefault() {
        check(System.getProperty("beatoraja.luaDiagnostics") == null, "test must run without the property");
        check(!LuaCallDiagnostics.isEnabled(), "disabled by default");
        check(LuaCallDiagnostics.site("integer", "1") == null, "no site when disabled");
        long token = LuaCallDiagnostics.begin(null);
        check(token < 0, "null site is not recorded");
        LuaCallDiagnostics.end(null, token);
    }

    private static void testVisitReport(LuaCallDiagnostics.Mode mode) throws Exception {
        Path dir = Files.createTempDirectory("luadiag");
        LuaCallDiagnostics d = new LuaCallDiagnostics(mode, 0, dir);
        Globals g = JsePlatform.standardGlobals();
        LuaFunction f = g.load("return 1 + 2").checkfunction();
        LuaCallDiagnostics.Site a = d.register("integer", "1 + 2");
        LuaCallDiagnostics.Site b = d.register("timer", LuaCallDiagnostics.describe(f));
        check(d.register("integer", "1 + 2") == a, "same kind and label share a site");

        d.frame("PLAY");           // owner + calibration; warmup 0 starts recording on this frame
        for (int frame = 0; frame < 10; frame++) {
            for (int i = 0; i < 3; i++) {
                long t = d.start();
                check(t >= 0, "recorded on owner thread");
                f.call();
                d.stop(a, t);
            }
            long outer = d.start();
            long inner = d.start();     // nested call counts for its site but not twice for the frame
            d.stop(b, inner);
            d.stop(b, outer);
            d.frame("PLAY");
        }
        Thread other = new Thread(() -> check(d.start() < 0, "other threads are not recorded"));
        other.start(); other.join();
        d.finishVisit();

        String text = Files.readString(dir.resolve(only(dir, ".txt")), StandardCharsets.UTF_8);
        check(text.contains("state=PLAY visit=1 mode=" + mode), "report header: " + text);
        check(text.contains("10 frames"), "frame count: " + text);
        check(text.contains("lua calls/frame: mean 4.0"), "top-level calls per frame: " + text);
        check(text.contains("off-render-thread calls: 1"), "off-thread count: " + text);
        String csv = Files.readString(dir.resolve(only(dir, ".csv")), StandardCharsets.UTF_8);
        check(csv.contains("\"integer\",30,"), "site calls: " + csv);
        check(csv.contains("\"timer\",20,"), "nested site calls: " + csv);
        check(csv.contains("function "), "closure label: " + csv);

        // A second state visit appends a new report.
        d.frame("RESULT");
        d.frame("RESULT");
        d.frame("RESULT");
        d.finishVisit();
        text = Files.readString(dir.resolve(only(dir, ".txt")), StandardCharsets.UTF_8);
        check(text.contains("state=RESULT visit=1"), "second visit: " + text);
    }

    private static String only(Path dir, String suffix) throws Exception {
        try (var s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(suffix)).findFirst().orElseThrow();
        }
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
