package bms.player.beatoraja.skin.lua;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.logging.Logger;

import bms.player.beatoraja.skin.SkinPropertyMapper;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.skin.event.*;
import bms.player.beatoraja.skin.property.*;
import bms.player.beatoraja.SkinConfig;

public class SkinLuaAccessor {
	
	private final Globals globals;

	public SkinLuaAccessor() {
		globals = JsePlatform.standardGlobals();
	}

	public SkinLuaAccessor (MainState state) {
        globals = JsePlatform.standardGlobals();
		globals.set("timer", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				return LuaNumber.valueOf(state.main.getMicroTimer(value.toint()));
			}
		});
		globals.set("timer_off_value", Long.MIN_VALUE);
		globals.set("is_timer_on", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				return LuaNumber.valueOf(state.main.isTimerOn(value.toint()));
			}
		});
		globals.set("now_timer", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				return LuaNumber.valueOf(state.main.getNowMicroTime(value.toint()));
			}
		});
		globals.set("time", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaNumber.valueOf(state.main.getNowMicroTime());
			}
		});
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
		globals.set("set_volume_sys", new OneArgFunction() {
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
		globals.set("set_volume_key", new OneArgFunction() {
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
		globals.set("set_volume_bg", new OneArgFunction() {
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
		globals.set("event_exec", new VarArgFunction() {
			@Override
			public LuaValue call(LuaValue luaValue) {
				state.executeEvent(getId(luaValue));
				return LuaValue.NIL;
			}
			@Override
			public LuaValue call(LuaValue luaValue, LuaValue arg1) {
				state.executeEvent(getId(luaValue), arg1.toint());
				return LuaValue.NIL;
			}
			@Override
			public LuaValue call(LuaValue luaValue, LuaValue arg1, LuaValue arg2) {
				state.executeEvent(getId(luaValue), arg1.toint(), arg2.toint());
				return LuaValue.NIL;
			}
			private int getId(LuaValue luaValue) {
				int id = luaValue.toint();
				if (!SkinPropertyMapper.isEventRunnableBySkin(id))
					throw new IllegalArgumentException("指定されたイベントはスキンから実行できません");
				return id;
			}
		});
	}

	public BooleanProperty loadBooleanProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			return loadBooleanProperty(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public BooleanProperty loadBooleanProperty(LuaFunction function) {
		return new BooleanProperty() {
			@Override
			public boolean isStatic(MainState state) {
				return false;
			}

			@Override
			public boolean get(MainState state) {
				try {
					return function.call().toboolean();
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外 : " + e.getMessage());
					return false;
				}
			}
		};
	}

	public IntegerProperty loadIntegerProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			return loadIntegerProperty(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public IntegerProperty loadIntegerProperty(LuaFunction function) {
		return new IntegerProperty() {
			@Override
			public int get(MainState state) {
				try{
					return function.call().toint();
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外 : " + e.getMessage());
					return 0;
				}
			}
		};
	}

	public FloatProperty loadFloatProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			return loadFloatProperty(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public FloatProperty loadFloatProperty(LuaFunction function) {
		return new FloatProperty() {
			@Override
			public float get(MainState state) {
				try{
					return function.call().tofloat();
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外 : " + e.getMessage());
					return 0f;
				}
			}
		};
	}

	public StringProperty loadStringProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			return loadStringProperty(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public StringProperty loadStringProperty(LuaFunction function) {
		return new StringProperty() {
			@Override
			public String get(MainState state) {
				try {
					return function.call().tojstring();
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外：" + e.getMessage());
					return "";
				}
			}
		};
	}

	/**
	 * Creates a timer property from Lua code.
	 * If {@code script} returns a function, the returned function is regarded as a timer function
	 * which will be called every frame or more frequently.
	 * Otherwise, {@code script} itself is regarded as a timer function.
	 * <p>NOTE: The former case is useful to synthesize a stateful custom timer in a JSON skin.</p>
	 * <p>NOTE: A timer function returns (i) start time in microseconds if on, or (ii) Long.MIN_VALUE if off.</p>
	 * @param script Lua script producing a function (producing a number) or a number
	 * @return new timer property
	 */
	public TimerProperty loadTimerProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			final LuaValue trialCallResult = lv.call();
			if (trialCallResult.isfunction()) {
				// タイマー関数を返す場合
				return loadTimerProperty(trialCallResult.checkfunction());
			} else {
				// 数値を返す場合
				return loadTimerProperty(lv.checkfunction());
			}
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	/**
	 * Creates a timer property from Lua function.
	 * The given function is always regarded as a timer function which will be called every frame.
	 * @param timerFunction Lua function producing a number
	 * @return new timer property
	 */
	public TimerProperty loadTimerProperty(LuaFunction timerFunction) {
		return new TimerProperty() {
			@Override
			public long getMicro(MainState state) {
				try {
					return timerFunction.call().tolong();
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外：" + e.getMessage());
					return Long.MIN_VALUE;
				}
			}
		};
	}

	public Event loadEvent(String script) {
		try {
			final LuaValue lv = globals.load(script);
			return loadEvent(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public Event loadEvent(LuaFunction function) {
		switch (function.narg()) {
		case 0:
			return EventFactory.createZeroArgEvent(state -> {
				try{
					function.call();
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外 : " + e.getMessage());
				}
			});
		case 1:
			return EventFactory.createOneArgEvent((state, arg1) -> {
				try{
					function.call(LuaNumber.valueOf(arg1));
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外 : " + e.getMessage());
				}
			});
		case 2:
			return EventFactory.createTwoArgEvent((state, arg1, arg2) -> {
				try{
					function.call(LuaNumber.valueOf(arg1), LuaNumber.valueOf(arg2));
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外 : " + e.getMessage());
				}
			});
		default:
			return null;
		}
	}

	public FloatWriter loadFloatWriter(String script) {
		try {
			final LuaValue lv = globals.load(script);
			return loadFloatWriter(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public FloatWriter loadFloatWriter(LuaFunction function) {
		return new FloatWriter() {
			@Override
			public void set(MainState state, float value) {
				try{
					function.call(LuaDouble.valueOf(value));
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外：" + e.getMessage());
				}
			}
		};
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
