package bms.player.beatoraja.skin.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;

/**
 * Narrow compatibility facade for legacy skins that used {@code luajava} only
 * to inspect keyboard and controller input.
 */
final class LegacySkinLuaApi {

	private static final String LUAJAVA = "luajava";
	private static final String DEBUG = "debug";

	private LegacySkinLuaApi() {
	}

	static void install(Globals globals) {
		LuaTable luajava = new LuaTable();
		luajava.set("bindClass", new BindClassFunction());
		installModule(globals, LUAJAVA, luajava);

		LuaTable debug = new LuaTable();
		debug.set("getmetatable", globals.get("getmetatable"));
		installModule(globals, DEBUG, debug);
	}

	private static void installModule(Globals globals, String name, LuaTable module) {
		globals.set(name, module);
		globals.package_.setIsLoaded(name, module);
	}

	private static final class BindClassFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue className) {
			return switch (className.checkjstring()) {
				case "com.badlogic.gdx.Gdx" -> gdxFacade();
				case "com.badlogic.gdx.Input" -> inputClassFacade();
				case "com.badlogic.gdx.controllers.Controllers" -> controllersFacade();
				case "com.badlogic.gdx.controllers.Controller" -> new LuaTable();
				default -> throw new LuaError("Legacy Lua skin class access denied: " + className);
			};
		}
	}

	private static LuaTable gdxFacade() {
		LuaTable facade = new LuaTable();
		LuaTable input = new LuaTable();
		input.set("isKeyPressed", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue self, LuaValue keycode) {
				return LuaValue.valueOf(Gdx.input != null && Gdx.input.isKeyPressed(keycode.checkint()));
			}
		});
		facade.set("input", input);
		return facade;
	}

	private static LuaTable inputClassFacade() {
		LuaTable facade = new LuaTable();
		LuaTable keys = new LuaTable();
		LuaTable metatable = new LuaTable();
		metatable.set("__index", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue table, LuaValue key) {
				try {
					return LuaValue.valueOf(Input.Keys.valueOf(key.checkjstring()));
				} catch (IllegalArgumentException e) {
					return LuaValue.NIL;
				}
			}
		});
		keys.setmetatable(metatable);
		facade.set("Keys", keys);
		return facade;
	}

	private static LuaTable controllersFacade() {
		LuaTable facade = new LuaTable();
		facade.set("getControllers", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue self) {
				Array<Controller> controllers = Controllers.getControllers();
				LuaTable result = new LuaTable();
				result.set("size", controllers.size);
				result.set("first", new OneArgFunction() {
					@Override
					public LuaValue call(LuaValue ignored) {
						return controllers.size > 0 ? controllerFacade(controllers.first()) : LuaValue.NIL;
					}
				});
				return result;
			}
		});
		return facade;
	}

	private static LuaTable controllerFacade(Controller controller) {
		LuaTable facade = new LuaTable();
		facade.set("getButton", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue self, LuaValue button) {
				return LuaValue.valueOf(controller.getButton(button.checkint()));
			}
		});
		facade.set("getName", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue self) {
				return LuaValue.valueOf(controller.getName());
			}
		});
		return facade;
	}
}
