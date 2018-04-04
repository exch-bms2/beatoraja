package bms.player.beatoraja.skin.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.BMSPlayer;

public class SkinLuaAccessor {
	
	private final Globals globals;
	
	public SkinLuaAccessor (MainState state) {
        globals = JsePlatform.standardGlobals();
        globals.set("rate", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.getScoreDataProperty().getNowRate());
			}
        });
		globals.set("exscore", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.getScoreDataProperty().getNowEXScore());
			}
		});
		globals.set("rate_best", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.getScoreDataProperty().getNowBestScoreRate());
			}
		});
		globals.set("exscore_best", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.getScoreDataProperty().getBestScore());
			}
		});
		globals.set("rate_rival", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.getScoreDataProperty().getRivalScoreRate());
			}
		});
		globals.set("exscore_rival", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.getScoreDataProperty().getRivalScore());
			}
		});
        globals.set("judge", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				return LuaInteger.valueOf(state.getJudgeCount(value.toint(), true) + state.getJudgeCount(value.toint(), false));
			}
        });
        globals.set("gauge", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				if(state instanceof BMSPlayer) {
					BMSPlayer player = (BMSPlayer) state;
					return LuaDouble.valueOf(player.getGauge().getValue());
				}
				return LuaInteger.ZERO;
			}
        });
		globals.set("gauge_type", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				if(state instanceof BMSPlayer) {
					BMSPlayer player = (BMSPlayer) state;
					return LuaDouble.valueOf(player.getGauge().getType());
				}
				return LuaInteger.ZERO;
			}
		});
	}
	
	public LuaValue load(String script) {
		return globals.load("return " + script);
	}	
}
