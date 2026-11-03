package bms.player.beatoraja.skin.lua;

import java.lang.reflect.Proxy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

/** Input API checks without a native window or controller. */
public final class MainStateInputLuaApiTest {

    public static void main(String[] args) {
        Input oldInput = Gdx.input;
        Graphics oldGraphics = Gdx.graphics;
        try {
            Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class},
                    (proxy, method, values) -> switch (method.getName()) {
                        case "isKeyPressed", "isKeyJustPressed" -> (int) values[0] == Input.Keys.LEFT;
                        case "isButtonPressed" -> (int) values[0] == Input.Buttons.RIGHT;
                        case "getX" -> 120;
                        case "getY" -> 80;
                        default -> throw new AssertionError("Unexpected input method: " + method.getName());
                    });
            Gdx.graphics = (Graphics) Proxy.newProxyInstance(Graphics.class.getClassLoader(), new Class<?>[]{Graphics.class},
                    (proxy, method, values) -> switch (method.getName()) {
                        case "getWidth" -> 800;
                        case "getHeight" -> 600;
                        default -> throw new AssertionError("Unexpected graphics method: " + method.getName());
                    });
            LuaTable api = new LuaTable();
            new MainStatePropertyLuaApiExporter(null).export(api);
            check(api.get("key_pressed").call(LuaValue.valueOf("LEFT")).toboolean(), "Existing key name API");
            check(api.get("key_pressed").call(LuaValue.valueOf("Left")).toboolean(), "Display key name");
            check(api.get("key_just_pressed").call(LuaValue.valueOf("LEFT")).toboolean(), "Key name edge");
            check(api.get("key_just_pressed").call(LuaValue.valueOf(Input.Keys.LEFT)).toboolean(), "Repeated numeric edge");
            check(!api.get("key_just_pressed").call(LuaValue.valueOf("UNKNOWN_KEY")).toboolean(), "Unknown key");
            check(!api.get("key_pressed").call(LuaValue.valueOf(256)).toboolean(), "Out-of-range key");
            check(api.get("mouse_pressed").call(LuaValue.valueOf(1)).toboolean(), "Right mouse button");
            check(!api.get("mouse_pressed").call(LuaValue.valueOf(-1)).toboolean(), "Invalid mouse button");
            check(!api.get("mouse_pressed").call(LuaValue.valueOf(1.5)).toboolean(), "Fractional mouse button");
            check(!api.get("key_pressed").call(LuaValue.valueOf(Double.NaN)).toboolean(), "Non-finite key");
            check(!api.get("mouse_pressed").call(LuaValue.valueOf("LEFT")).toboolean(), "Button IDs are numeric");
            check(!api.get("input_pressed").call(LuaValue.valueOf(0)).toboolean(), "No logical input context");
            check(!api.get("start_pressed").call().toboolean(), "No START context");
            check(!api.get("select_pressed").call().toboolean(), "No SELECT context");
            check(!api.get("mouse_just_pressed").call(LuaValue.valueOf(0)).toboolean(), "No mouse edge context");
            check(api.get("mouse_x").call().todouble() == 120, "Window X fallback");
            check(api.get("mouse_y").call().todouble() == 520, "Bottom-left Y fallback");
            Gdx.input = null;
            Gdx.graphics = null;
            check(!api.get("key_just_pressed").call(LuaValue.valueOf(21)).toboolean(), "Missing keyboard");
            check(!api.get("mouse_pressed").call(LuaValue.valueOf(0)).toboolean(), "Missing mouse");
            check(api.get("mouse_x").call().todouble() == 0 && api.get("mouse_y").call().todouble() == 0, "Missing coordinates");
        } finally {
            Gdx.input = oldInput;
            Gdx.graphics = oldGraphics;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
