package bms.player.beatoraja.skin.lua;

import java.nio.file.Files;
import java.nio.file.Path;

import org.luaj.vm2.LuaError;

import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinType;

/** Regression coverage for legacy Lua modules used by existing skins. */
public final class SkinLuaCompatibilityTest {

	public static void main(String[] args) throws Exception {
		compatibilityFacadeProvidesRequiredModules();
		compatibilityFacadeAllowsInputClasses();
		compatibilityFacadeRejectsArbitraryJavaClasses();
		sandboxProvidesOnlySafeOsFunctions();
		sandboxStillRejectsOutsideFileAccess();
		sandboxedHeaderLoaderLoadsCompatibleSkin();
		System.out.println("SkinLuaCompatibilityTest passed");
	}

	private static void sandboxProvidesOnlySafeOsFunctions() throws Exception {
		// Given
		Path sandboxRoot = Files.createTempDirectory("lua-sandbox-os-");
		SkinLuaAccessor loader = new SkinLuaAccessor(false, sandboxRoot);

		// When
		String dateType = loader.exec("return type(os.date)").tojstring();

		// Then
		check("function".equals(dateType), "sandbox must provide safe date and time functions");
		check(loader.exec("return os.execute == nil and os.remove == nil and os.rename == nil "
				+ "and os.getenv == nil and os.exit == nil").toboolean(),
				"sandbox must not expose process, environment, or file mutation OS functions");
	}

	private static void sandboxedHeaderLoaderLoadsCompatibleSkin() throws Exception {
		// Given
		Path skinDirectory = Files.createTempDirectory("legacy-lua-skin-");
		Path skinFile = skinDirectory.resolve("legacy.luaskin");
		Files.writeString(skinFile, "local luajava = require('luajava')\n"
				+ "luajava.bindClass('com.badlogic.gdx.Gdx')\n"
				+ "return { type = 5, name = 'Legacy test skin', w = 1280, h = 720 }");

		// When
		SkinHeader header = LuaSkinLoader.sandboxed(skinFile).loadHeader(skinFile);

		// Then
		check(header != null, "compatible legacy skin header must load inside the sandbox");
		check(header.getSkinType() == SkinType.MUSIC_SELECT,
				"legacy skin header must preserve its screen type");
	}

	private static void compatibilityFacadeProvidesRequiredModules() {
		// Given
		SkinLuaAccessor loader = new SkinLuaAccessor(false);

		// When
		String luajavaType = loader.exec("return type(require('luajava'))").tojstring();
		String debugType = loader.exec("return type(require('debug'))").tojstring();

		// Then
		check("table".equals(luajavaType), "compatibility facade must provide the luajava module name");
		check("table".equals(debugType), "compatibility facade must provide the debug module name");
		check(loader.exec("return debug.sethook == nil").toboolean(),
				"compatibility debug module must not expose hooks");
	}

	private static void compatibilityFacadeAllowsInputClasses() {
		// Given
		SkinLuaAccessor loader = new SkinLuaAccessor(false);

		// When
		String result = loader.exec("local j = require('luajava'); "
				+ "return type(j.bindClass('com.badlogic.gdx.Gdx')) .. ':' .. "
				+ "type(j.bindClass('com.badlogic.gdx.Input')) .. ':' .. "
				+ "type(j.bindClass('com.badlogic.gdx.controllers.Controllers'))").tojstring();

		// Then
		check("table:table:table".equals(result), "input compatibility classes must be available as facades");
	}

	private static void compatibilityFacadeRejectsArbitraryJavaClasses() {
		// Given
		SkinLuaAccessor loader = new SkinLuaAccessor(false);

		// When / Then
		checkThrows(LuaError.class,
				() -> loader.exec("return require('luajava').bindClass('java.lang.Runtime')"),
				"compatibility facade must reject arbitrary Java classes");
	}

	private static void sandboxStillRejectsOutsideFileAccess() throws Exception {
		// Given
		Path sandboxRoot = Files.createTempDirectory("lua-sandbox-");
		SkinLuaAccessor loader = new SkinLuaAccessor(false, sandboxRoot);

		// When / Then
		checkThrows(LuaError.class, () -> loader.exec("assert(io.open('../escape.txt', 'w'))"),
				"sandbox must reject writes outside the skin directory");
		check(!Files.exists(sandboxRoot.getParent().resolve("escape.txt")),
				"sandbox escape file must not be created");
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private static void checkThrows(Class<? extends Throwable> expectedType, Runnable action, String message) {
		try {
			action.run();
		} catch (Throwable actual) {
			if (expectedType.isInstance(actual)) {
				return;
			}
			throw new AssertionError(message + ": unexpected " + actual.getClass().getSimpleName(), actual);
		}
		throw new AssertionError(message + ": no exception was thrown");
	}
}
