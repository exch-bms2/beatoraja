package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.SkinPropertyMapper;
import bms.player.beatoraja.skin.property.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

final class MainStatePropertyLuaApiExporter implements LuaApiExporter {

	private static final long TIMER_OFF_VALUE = Long.MIN_VALUE;
	private final MainState state;

	MainStatePropertyLuaApiExporter(MainState state) {
		this.state = state;
	}

	@Override
	public void export(LuaTable table) {
		table.set("option", new OptionFunction());
		table.set("number", new NumberFunction());
		table.set("float_number", new FloatNumberFunction());
		table.set("text", new TextFunction());
		table.set("offset", new OffsetFunction());
		table.set("timer", new TimerFunction());
		table.set("timer_off_value", TIMER_OFF_VALUE);
		table.set("time", new TimeFunction());
		table.set("set_timer", new SetTimerFunction());
		table.set("event_exec", new EventExecFunction());
		table.set("event_index", new EventIndexFunction());
		table.set("key_pressed", new KeyPressedFunction());

		table.set("rate", zeroArg(() -> LuaDouble.valueOf(state.getScoreDataProperty().getNowRate())));
		table.set("exscore", zeroArg(() -> LuaDouble.valueOf(state.getScoreDataProperty().getNowEXScore())));
		table.set("rate_best", zeroArg(() -> LuaDouble.valueOf(state.getScoreDataProperty().getNowBestScoreRate())));
		table.set("exscore_best", zeroArg(() -> LuaDouble.valueOf(state.getScoreDataProperty().getBestScore())));
		table.set("rate_rival", zeroArg(() -> LuaDouble.valueOf(state.getScoreDataProperty().getRivalScoreRate())));
		table.set("exscore_rival", zeroArg(() -> LuaDouble.valueOf(state.getScoreDataProperty().getRivalScore())));
		table.set("volume_sys", zeroArg(() -> LuaDouble.valueOf(state.main.getConfig().getAudioConfig().getSystemvolume())));
		table.set("volume_key", zeroArg(() -> LuaDouble.valueOf(state.main.getConfig().getAudioConfig().getKeyvolume())));
		table.set("volume_bg", zeroArg(() -> LuaDouble.valueOf(state.main.getConfig().getAudioConfig().getBgvolume())));
		table.set("gauge", zeroArg(this::getGauge));
		table.set("gauge_type", zeroArg(this::getGaugeType));

		table.set("set_volume_sys", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				state.main.getConfig().getAudioConfig().setSystemvolume(value.tofloat());
				return LuaBoolean.TRUE;
			}
		});
		table.set("set_volume_key", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				state.main.getConfig().getAudioConfig().setKeyvolume(value.tofloat());
				return LuaBoolean.TRUE;
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
				int judge = value.toint();
				return LuaInteger.valueOf(state.getJudgeCount(judge, true) + state.getJudgeCount(judge, false));
			}
		});
	}

	private static ZeroArgFunction zeroArg(LuaValueSupplier supplier) {
		return new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return supplier.get();
			}
		};
	}

	private LuaValue getGauge() {
		if(state instanceof BMSPlayer player) {
			return LuaDouble.valueOf(player.getGauge().getValue());
		}
		return LuaInteger.ZERO;
	}

	private LuaValue getGaugeType() {
		if(state instanceof BMSPlayer player) {
			return LuaDouble.valueOf(player.getGauge().getType());
		}
		return LuaInteger.ZERO;
	}

	private class OptionFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			BooleanProperty prop = BooleanPropertyFactory.getBooleanProperty(luaValue.toint());
			return LuaBoolean.valueOf(prop.get(state));
		}
	}

	private class NumberFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			IntegerProperty prop = IntegerPropertyFactory.getIntegerProperty(luaValue.toint());
			return LuaNumber.valueOf(prop.get(state));
		}
	}

	private class FloatNumberFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			FloatProperty prop = FloatPropertyFactory.getRateProperty(luaValue.toint());
			return LuaDouble.valueOf(prop.get(state));
		}
	}

	private class TextFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			StringProperty prop = StringPropertyFactory.getStringProperty(luaValue.toint());
			return LuaString.valueOf(prop.get(state));
		}
	}

	private class OffsetFunction extends OneArgFunction {
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

	private class TimerFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue value) {
			return LuaNumber.valueOf(state.timer.getMicroTimer(value.toint()));
		}
	}

	private class TimeFunction extends ZeroArgFunction {
		@Override
		public LuaValue call() {
			return LuaNumber.valueOf(state.timer.getNowMicroTime());
		}
	}

	private class SetTimerFunction extends TwoArgFunction {
		@Override
		public LuaValue call(LuaValue timerId, LuaValue timerValue) {
			int id = timerId.toint();
			if(!SkinPropertyMapper.isTimerWritableBySkin(id)) {
				throw new IllegalArgumentException("指定されたタイマーはスキンから変更できません");
			}
			state.timer.setMicroTimer(id, timerValue.tolong());
			return LuaBoolean.TRUE;
		}
	}

	private class EventExecFunction extends VarArgFunction {
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
			if(!SkinPropertyMapper.isEventRunnableBySkin(id)) {
				throw new IllegalArgumentException("指定されたイベントはスキンから実行できません");
			}
			return id;
		}
	}

	private class EventIndexFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			IntegerProperty prop = IntegerPropertyFactory.getImageIndexProperty(luaValue.toint());
			return LuaNumber.valueOf(prop.get(state));
		}
	}

	private static class KeyPressedFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue value) {
			if(Gdx.input == null) {
				return LuaBoolean.FALSE;
			}
			int keycode;
			try {
				keycode = value.isnumber() ? value.toint() : Input.Keys.valueOf(value.tojstring());
			} catch(IllegalArgumentException e) {
				return LuaBoolean.FALSE;
			}
			return LuaBoolean.valueOf(keycode >= 0 && Gdx.input.isKeyPressed(keycode));
		}
	}

	@FunctionalInterface
	private interface LuaValueSupplier {
		LuaValue get();
	}
}
