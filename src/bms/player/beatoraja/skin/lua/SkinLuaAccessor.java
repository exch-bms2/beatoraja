package bms.player.beatoraja.skin.lua;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.logging.Logger;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinProperty;
import bms.player.beatoraja.skin.SkinHeader.CustomOffset;
import bms.player.beatoraja.skin.SkinHeader.CustomOption;
import bms.player.beatoraja.skin.property.*;
import bms.player.beatoraja.SkinConfig;

/**
 * Luaスキンからデータを参照するためのクラス
 * 
 * @author excln
 */
public class SkinLuaAccessor {
	
	private final Globals globals;

	// 各機能のエクスポート先を globals にするかどうか
	private final boolean isGlobal;

	// isGlobal == false のとき、エクスポートするモジュール名
	private static final String MAIN_STATE = "main_state";
	private static final String TIMER_UTIL = "timer_util";
	private static final String EVENT_UTIL = "event_util";

	public SkinLuaAccessor(boolean isGlobal) {
		globals = JsePlatform.standardGlobals();
		this.isGlobal = isGlobal;

		if (!isGlobal) {
			// ヘッダ読み込み時に require("main_state") だけでエラーになると面倒なので、空のテーブルを入れておく
			globals.package_.setIsLoaded(MAIN_STATE, new LuaTable());
			globals.package_.setIsLoaded(TIMER_UTIL, new LuaTable());
			globals.package_.setIsLoaded(EVENT_UTIL, new LuaTable());
		}
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

	/**
	 * MainState にアクセスするための機能をエクスポートする。
	 * isGlobal == true のとき、グローバル変数としてそのまま追加
	 * それ以外のとき、モジュール "main_state" にエクスポート
	 * (Lua からは main_state = require("main_state") などとすることで利用可能)
	 * @param state MainState
	 */
	public void exportMainStateAccessor(MainState state) {
		MainStateAccessor accessor = new MainStateAccessor(state);
		if (isGlobal) {
			accessor.export(globals);
		} else {
			LuaTable mainStateTable = new LuaTable();
			accessor.export(mainStateTable);
			globals.package_.setIsLoaded(MAIN_STATE, mainStateTable);
		}
	}

	/**
	 * その他のユーティリティーをエクスポートする。
	 * ロードがそれほど重くなく、JSONスキンから使う可能性もあることが前提
	 * @param state MainState
	 */
	public void exportUtilities(MainState state) {
		TimerUtility timerUtil = new TimerUtility(state);
		EventUtility eventUtil = new EventUtility(state);
		if (isGlobal) {
			timerUtil.export(globals);
			eventUtil.export(globals);
		} else {
			LuaTable timerUtilTable = new LuaTable();
			timerUtil.export(timerUtilTable);
			globals.package_.setIsLoaded(TIMER_UTIL, timerUtilTable);
			LuaTable eventUtilTable = new LuaTable();
			eventUtil.export(eventUtilTable);
			globals.package_.setIsLoaded(EVENT_UTIL, eventUtilTable);
		}
	}

	/**
	 * スキン設定をエクスポートする。
	 * isGlobal にかかわらず、グローバル変数 skin_config にデータがセットされた状態にする。
	 * Lua スキンは skin_config が nil のときヘッダのみ読み込めるようにする。
	 * 
	 * @param header スキンヘッダデータ
	 * @param property Property (スキン設定データ)
	 * @param filePathGetter スキン設定を元にファイルパスを解決する関数
	 */
	public void exportSkinProperty(SkinHeader header, SkinConfig.Property property, Function<String, String> filePathGetter) {
		LuaTable table = new LuaTable();
		exportSkinPropertyToTable(header, property, filePathGetter, table);
		globals.set("skin_config", table);
	}

	private void exportSkinPropertyToTable(SkinHeader header, SkinConfig.Property property, Function<String, String> filePathGetter, LuaTable table) {
		LuaTable file_path = new LuaTable();
		for (SkinConfig.FilePath file : property.getFile()) {
			file_path.set(file.name, file.path);
		}
		table.set("file_path", file_path);
		table.set("get_path", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				return LuaString.valueOf(filePathGetter.apply(value.tojstring()));
			}
		});

		LuaTable options = new LuaTable();
		LuaTable enabled_options = new LuaTable();
		
		for (CustomOption option : header.getCustomOptions()) {
			int opvalue = option.getSelectedOption();
			options.set(option.name, opvalue);
			enabled_options.insert(enabled_options.length() + 1, LuaInteger.valueOf(opvalue));				
		}

		table.set("option", options);
		table.set("enabled_options", enabled_options);

		LuaTable offsets = new LuaTable();
		
		for(CustomOffset offset : header.getCustomOffsets()) {
			SkinConfig.Offset ofs = null;
			for (SkinConfig.Offset of : property.getOffset()) {
				if(offset.name.equals(of.name)) {
					ofs = of;
					break;
				}
			}
			if(ofs == null) {
				ofs = new SkinConfig.Offset();
				ofs.name = offset.name;
			}
			LuaTable offsetTable = new LuaTable();
			offsetTable.set("x", ofs.x);
			offsetTable.set("y", ofs.y);
			offsetTable.set("w", ofs.w);
			offsetTable.set("h", ofs.h);
			offsetTable.set("r", ofs.r);
			offsetTable.set("a", ofs.a);
			offsets.set(ofs.name, offsetTable);			
		}
		table.set("offset", offsets);
	}
}
