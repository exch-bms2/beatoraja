package bms.player.beatoraja.skin.lua;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.logging.Logger;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.skin.SkinObject.*;
import bms.player.beatoraja.skin.property.*;
import bms.player.beatoraja.SkinConfig;

public class SkinLuaAccessor {
	
	private final Globals globals;

	public SkinLuaAccessor() {
		globals = JsePlatform.standardGlobals();
	}

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
		globals.set("volume_sys", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.main.getConfig().getSystemvolume());
			}
		});
		globals.set("volume_sys", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				state.main.getConfig().setSystemvolume(value.tofloat());
				return LuaBoolean.TRUE;
			}
		});
		globals.set("volume_key", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.main.getConfig().getKeyvolume());
			}
		});
		globals.set("volume_key", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				state.main.getConfig().setKeyvolume(value.tofloat());
				return LuaBoolean.TRUE;
			}
		});
		globals.set("volume_bg", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.main.getConfig().getBgvolume());
			}
		});
		globals.set("volume_bg", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				state.main.getConfig().setBgvolume(value.tofloat());
				return LuaBoolean.TRUE;
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
	
	public BooleanProperty loadBooleanProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			return new BooleanProperty() {
				@Override
				public boolean isStatic(MainState state) {
					return false;
				}

				@Override
				public boolean get(MainState state) {
					return lv.call().toboolean();
				}
			};
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}
	
	public IntegerProperty loadIntegerProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			return new IntegerProperty() {
				@Override
				public int get(MainState state) {
					return lv.call().toint();
				}
			};			
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}
	
	public FloatProperty loadFloatProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			return new FloatProperty() {
				@Override
				public float get(MainState state) {
					return lv.call().tofloat();
				}
			};			
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}
	
	public Event loadEvent(String script) {
		try {
			final LuaValue lv = globals.load(script);
			return new Event() {
				@Override
				public void exec(MainState state) {
					lv.call();
				}
			};			
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public FloatWriter loadFloatWriter(String script) {
		try {
			final LuaValue lv = globals.load(script);
			return new FloatWriter() {

				@Override
				public void set(MainState state, float value) {
					lv.call(LuaDouble.valueOf(value));
				}
				
			};			
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public LuaValue exec(String script) {
		return globals.load(script).call();
	}

	public LuaValue execFile(Path path) {
		return globals.loadfile(path.toString()).call();
	}

	public void setDirectory(Path path) {
		LuaTable pkg = globals.get("package").checktable();
		pkg.set("path", pkg.get("path").tojstring() + ";" + path.toString() + "/?.lua");
	}

	public void setSkinProperty(SkinConfig.Property property, Function<String, String> filePathGetter) {
		LuaTable skin_config = new LuaTable();

		LuaTable file_path = new LuaTable();
		for (SkinConfig.FilePath file : property.getFile()) {
			file_path.set(file.name, file.path);
		}
		skin_config.set("file_path", file_path);
		skin_config.set("get_path", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				return LuaString.valueOf(filePathGetter.apply(value.tojstring()));
			}
		});

		LuaTable options = new LuaTable();
		LuaTable enabled_options = new LuaTable();
		for (SkinConfig.Option op : property.getOption()){
			options.set(op.name, op.value);
			enabled_options.insert(enabled_options.length() + 1, LuaInteger.valueOf(op.value));
		}
		skin_config.set("option", options);
		skin_config.set("enabled_options", enabled_options);

		LuaTable offsets = new LuaTable();
		for (SkinConfig.Offset ofs : property.getOffset()) {
			LuaTable table = new LuaTable();
			table.set("x", ofs.x);
			table.set("y", ofs.y);
			table.set("w", ofs.w);
			table.set("h", ofs.h);
			table.set("r", ofs.r);
			table.set("a", ofs.a);
			offsets.set(ofs.name, table);
		}
		skin_config.set("offset", offsets);

		globals.set("skin_config", skin_config);
	}
}
