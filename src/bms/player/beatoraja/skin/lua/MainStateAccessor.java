package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.SkinPropertyMapper;
import bms.player.beatoraja.skin.property.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

import com.badlogic.gdx.math.MathUtils;

/**
 * 実行時にスキンからMainStateの数値などにアクセスできる関数を提供する
 */
public class MainStateAccessor {

	private final MainState state;

	public MainStateAccessor(MainState state) {
		this.state = state;
	}

	public void export(LuaTable table) {
		// 汎用関数(ID指定での取得・設定など)
		table.set("option", this.new option());
		table.set("number", this.new number());
		table.set("float_number", this.new float_number());
		table.set("text", this.new text());
		table.set("offset", this.new offset());
		table.set("timer", this.new timer());
		table.set("timer_off_value", MainStateAccessor.timer_off_value);
		table.set("time", this.new time());
		table.set("set_timer", this.new set_timer());
		table.set("event_exec", this.new event_exec());
		table.set("event_index", this.new event_index());

		// 具体的な数値の取得・設定など
		table.set("rate", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.getScoreDataProperty().getNowRate());
			}
		});
		table.set("exscore", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.getScoreDataProperty().getNowEXScore());
			}
		});
		table.set("rate_best", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.getScoreDataProperty().getNowBestScoreRate());
			}
		});
		table.set("exscore_best", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.getScoreDataProperty().getBestScore());
			}
		});
		table.set("rate_rival", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.getScoreDataProperty().getRivalScoreRate());
			}
		});
		table.set("exscore_rival", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.getScoreDataProperty().getRivalScore());
			}
		});
		table.set("volume_sys", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.main.getConfig().getAudioConfig().getSystemvolume());
			}
		});
		table.set("set_volume_sys", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				state.main.getConfig().getAudioConfig().setSystemvolume(value.tofloat());
				return LuaBoolean.TRUE;
			}
		});
		table.set("volume_key", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.main.getConfig().getAudioConfig().getKeyvolume());
			}
		});
		table.set("set_volume_key", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				state.main.getConfig().getAudioConfig().setKeyvolume(value.tofloat());
				return LuaBoolean.TRUE;
			}
		});
		table.set("volume_bg", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.main.getConfig().getAudioConfig().getBgvolume());
			}
		});
		table.set("set_volume_bg", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				state.main.getConfig().getAudioConfig().setBgvolume(value.tofloat());
				return LuaBoolean.TRUE;
			}
		});
		table.set("judge", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				return LuaInteger.valueOf(state.getJudgeCount(value.toint(), true) + state.getJudgeCount(value.toint(), false));
			}
		});
		table.set("gauge", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				if (state instanceof BMSPlayer) {
					BMSPlayer player = (BMSPlayer) state;
					return LuaDouble.valueOf(player.getGauge().getValue());
				}
				return LuaInteger.ZERO;
			}
		});
		table.set("gauge_type", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				if (state instanceof BMSPlayer) {
					BMSPlayer player = (BMSPlayer) state;
					return LuaDouble.valueOf(player.getGauge().getType());
				}
				return LuaInteger.ZERO;
			}
		});
		table.set("audio_play", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue path, LuaValue volume) {
				float vol = volume.tofloat();
				vol = vol <= 0 ? 1 : MathUtils.clamp(vol, 0.0f, 2.0f);
				state.main.getAudioProcessor().play(path.tojstring(), state.main.getConfig().getAudioConfig().getSystemvolume() * vol, false);
				return LuaBoolean.TRUE;
			}
		});
		table.set("audio_loop", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue path, LuaValue volume) {
				float vol = volume.tofloat();
				vol = vol <= 0 ? 1 : MathUtils.clamp(vol, 0.0f, 2.0f);
				state.main.getAudioProcessor().play(path.tojstring(), state.main.getConfig().getAudioConfig().getSystemvolume() * vol, true);
				return LuaBoolean.TRUE;
			}
		});
		table.set("audio_stop", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				state.main.getAudioProcessor().stop(path.tojstring());
				return LuaBoolean.TRUE;
			}
		});
	}

	/**
	 * ID指定で真理値(OPTION_*)を取得する関数
	 * NOTE: 呼び出しの度にBooleanPropertyを生成しており効率が悪いため非推奨
	 */
	private class option extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			BooleanProperty prop = BooleanPropertyFactory.getBooleanProperty(luaValue.toint());
			return LuaBoolean.valueOf(prop.get(state));
		}
	}

	/**
	 * ID指定で整数値(NUMBER_*)を取得する関数
	 * NOTE: 呼び出しの度にIntegerPropertyを生成しており効率が悪いため非推奨
	 */
	private class number extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			IntegerProperty prop = IntegerPropertyFactory.getIntegerProperty(luaValue.toint());
			return LuaNumber.valueOf(prop.get(state));
		}
	}

	/**
	 * ID指定で小数値(SLIDER_* | BARGRAPH_*)を取得する関数
	 * NOTE: 呼び出しの度にFloatPropertyを生成しており効率が悪いため非推奨
	 */
	private class float_number extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			FloatProperty prop = FloatPropertyFactory.getRateProperty(luaValue.toint());
			return LuaDouble.valueOf(prop.get(state));
		}
	}

	/**
	 * ID指定で文字列(STRING_*)を取得する関数
	 * NOTE: 呼び出しの度にStringPropertyを生成しており効率が悪いため非推奨
	 */
	private class text extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			StringProperty prop = StringPropertyFactory.getStringProperty(luaValue.toint());
			return LuaString.valueOf(prop.get(state));
		}
	}

	/**
	 * ID指定でオフセット(OFFSET_*)を取得する関数
	 */
	private class offset extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue value) {
			SkinObject.SkinOffset offset = state.getOffsetValue(value.toint());
			LuaTable offsetTable = new LuaTable();
			offsetTable.set("x", offset.x);
			offsetTable.set("y", offset.y);
			offsetTable.set("w", offset.w);
			offsetTable.set("h", offset.h);
			offsetTable.set("r", offset.r);
			offsetTable.set("a", offset.a);
			return offsetTable;
		}
	}

	/**
	 * ID指定でタイマー(TIMER_* またはカスタムタイマー)の値を取得する関数
	 * return: ONになった時刻 (micro sec) | timer_off_value (OFFのとき)
	 */
	private class timer extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue value) {
			return LuaNumber.valueOf(state.timer.getMicroTimer(value.toint()));
		}
	}

	/**
	 * タイマーがOFFの状態を表す定数
	 */
	private static final Long timer_off_value = Long.MIN_VALUE;

	/**
	 * 現在時刻を取得する関数
	 * return: 時刻 (micro sec)
	 */
	private class time extends ZeroArgFunction {
		@Override
		public LuaValue call() {
			return LuaNumber.valueOf(state.timer.getNowMicroTime());
		}
	}

	/**
	 * ID指定でタイマーの値を設定する関数
	 * ゲームプレイに影響するタイマーは設定不可
	 * param timerValue: ONになった時刻 (micro sec) | timer_off_value (OFFにする場合)
	 */
	private class set_timer extends TwoArgFunction {
		@Override
		public LuaValue call(LuaValue timerId, LuaValue timerValue) {
			int id = timerId.toint();
			if (!SkinPropertyMapper.isTimerWritableBySkin(id))
				throw new IllegalArgumentException("指定されたタイマーはスキンから変更できません");
			state.timer.setMicroTimer(id, timerValue.tolong());
			return LuaBoolean.TRUE;
		}
	}

	/**
	 * ID指定でイベント(BUTTON_* またはカスタムイベント)を実行する関数
	 * ゲームプレイに影響するイベントは実行不可
	 */
	private class event_exec extends VarArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			state.executeEvent(getId(luaValue));
			return LuaBoolean.TRUE;
		}

		@Override
		public LuaValue call(LuaValue luaValue, LuaValue arg1) {
			state.executeEvent(getId(luaValue), arg1.toint());
			return LuaBoolean.TRUE;
		}

		@Override
		public LuaValue call(LuaValue luaValue, LuaValue arg1, LuaValue arg2) {
			state.executeEvent(getId(luaValue), arg1.toint(), arg2.toint());
			return LuaBoolean.TRUE;
		}

		private int getId(LuaValue luaValue) {
			int id = luaValue.toint();
			if (!SkinPropertyMapper.isEventRunnableBySkin(id))
				throw new IllegalArgumentException("指定されたイベントはスキンから実行できません");
			return id;
		}
	}

	/**
	 * ID指定でイベント(BUTTON_*)のインデックスを取得する関数
	 * NOTE: 呼び出しの度にIntegerPropertyを生成しており効率が悪いため非推奨
	 */
	private class event_index extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			IntegerProperty prop = IntegerPropertyFactory.getImageIndexProperty(luaValue.toint());
			return LuaNumber.valueOf(prop.get(state));
		}
	}

}
