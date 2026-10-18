package bms.player.beatoraja.skin.lua;

import java.awt.Desktop;
import java.net.URI;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Disposable;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

/** User-initiated browser navigation and skin-owned asynchronous HTTP requests. */
final class SkinRuntimeLuaApiExporter implements LuaApiExporter, Disposable {
    private final boolean enabled;
    private final SkinAsyncHttp http;
    private final BooleanSupplier userAction;
    private final Consumer<URI> browser;
    private long lastNavigation;
    private boolean disposed;

    SkinRuntimeLuaApiExporter(boolean enabled) {
        this(enabled, new SkinAsyncHttp(), () -> Gdx.input != null && (Gdx.input.justTouched()
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)),
                uri -> {
                    try {
                        if (!Desktop.isDesktopSupported()) throw new IllegalStateException("Browser unavailable");
                        Desktop.getDesktop().browse(uri);
                    } catch (Exception e) { throw new IllegalStateException("Browser launch failed", e); }
                });
    }

    SkinRuntimeLuaApiExporter(boolean enabled, SkinAsyncHttp http, BooleanSupplier userAction, Consumer<URI> browser) {
        this.enabled = enabled;
        this.http = http;
        this.userAction = userAction;
        this.browser = browser;
    }

    @Override public void export(LuaTable table) {
        table.set("open_url", new OneArgFunction() {
            @Override public LuaValue call(LuaValue url) {
                if (!enabled || disposed) return LuaValue.FALSE;
                try {
                    if (!userAction.getAsBoolean()) return LuaValue.FALSE;
                    URI uri = SkinAsyncHttp.validateUrl(url.checkjstring());
                    long now = System.nanoTime();
                    if (lastNavigation != 0 && now - lastNavigation < 1_000_000_000L) return LuaValue.FALSE;
                    lastNavigation = now;
                    browser.accept(uri);
                    return LuaValue.TRUE;
                } catch (RuntimeException e) { return LuaValue.FALSE; }
            }
        });
        table.set("http_get_async", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                if (!enabled || disposed) return failure("HTTP disabled");
                try { return LuaValue.valueOf(http.submit(args.checkjstring(1), args.optint(2, 1000),
                        SkinHttpBody.checkedLimit(args.optdouble(3, SkinHttpBody.DEFAULT_MAX_BYTES)))); }
                catch (RuntimeException e) { return failure(e.getMessage()); }
            }
        });
        table.set("http_poll", new OneArgFunction() {
            @Override public LuaValue call(LuaValue id) {
                SkinAsyncHttp.Result result = enabled && !disposed ? http.poll(id.checkint())
                        : SkinAsyncHttp.Result.failed("HTTP disabled");
                LuaTable response = new LuaTable();
                response.set("status", result.status());
                response.set("status_code", result.statusCode());
                response.set("error", result.error());
                try { response.set("lines", SkinHttpLuaApiExporter.toLuaLines(result.body())); }
                catch (java.io.IOException e) { throw new LuaError(e); }
                return response;
            }
        });
        table.set("http_cancel", new OneArgFunction() {
            @Override public LuaValue call(LuaValue id) {
                return LuaValue.valueOf(enabled && !disposed && http.cancel(id.checkint()));
            }
        });
    }

    private static Varargs failure(String message) {
        return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(message != null ? message : "HTTP request failed"));
    }

    @Override public void dispose() {
        disposed = true;
        http.dispose();
    }
}
