package bms.player.beatoraja.skin.lua;

import org.luaj.vm2.Lua;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;

/**
 * Conservative check that a skin function does not store outside its own registers.
 * <p>
 * Draw conditions are usually pure, but some skins update counters or flags in them
 * (e.g. {@code delayTime = delayTime + 1}). Those must keep running every frame, so only
 * functions without upvalue, global or table stores may be skipped. Functions they call
 * are not inspected.
 */
final class LuaSideEffects {
	private LuaSideEffects() {}

	static boolean isStoreFree(LuaValue function) {
		if (!(function instanceof LuaClosure closure)) return false;
		for (int i : closure.p.code) {
			switch (Lua.GET_OPCODE(i)) {
			case Lua.OP_SETUPVAL, Lua.OP_SETTABUP, Lua.OP_SETTABLE, Lua.OP_SETLIST:
				return false;
			default:
			}
		}
		return true;
	}
}
