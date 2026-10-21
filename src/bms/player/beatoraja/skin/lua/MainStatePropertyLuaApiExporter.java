package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.SkinPropertyMapper;
import bms.player.beatoraja.skin.property.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.IntFunction;

final class MainStatePropertyLuaApiExporter implements LuaApiExporter {

	private static final long TIMER_OFF_VALUE = Long.MIN_VALUE;
	private final MainState state;
	private final Map<String, BooleanProperty> booleanPropertiesByName = new HashMap<>();
	private final Map<String, IntegerProperty> integerPropertiesByName = new HashMap<>();
	private final Map<String, FloatProperty> floatPropertiesByName = new HashMap<>();
	private final Map<String, StringProperty> stringPropertiesByName = new HashMap<>();

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
		table.set("timer_is_on", new TimerIsOnFunction());
		table.set("timer_is_off", new TimerIsOffFunction());
		table.set("timer_elapsed", new TimerElapsedFunction(1));
		table.set("timer_elapsed_ms", new TimerElapsedFunction(1_000));
		table.set("timer_elapsed_seconds", new TimerElapsedSecondsFunction());
		table.set("time", new TimeFunction());
		table.set("set_timer", new SetTimerFunction());
		table.set("event_exec", new EventExecFunction());
		table.set("event_index", new EventIndexFunction());
		table.set("key_pressed", new KeyPressedFunction(false));
		table.set("key_just_pressed", new KeyPressedFunction(true));
		table.set("input_pressed", new OneArgFunction() {
			@Override public LuaValue call(LuaValue value) {
				BMSPlayerInputProcessor input = getInput();
				int index = inputIndex(value, BMSPlayerInputProcessor.KEYSTATE_SIZE - 1);
				return LuaBoolean.valueOf(input != null && index >= 0 && input.getKeyState(index));
			}
		});
		table.set("start_pressed", zeroArg(() -> LuaBoolean.valueOf(getInput() != null && getInput().startPressed())));
		table.set("select_pressed", zeroArg(() -> LuaBoolean.valueOf(getInput() != null && getInput().isSelectPressed())));
		table.set("mouse_pressed", new MousePressedFunction(false));
		table.set("mouse_just_pressed", new MousePressedFunction(true));
		table.set("mouse_x", zeroArg(() -> mousePosition(false)));
		table.set("mouse_y", zeroArg(() -> mousePosition(true)));
		table.set("numbers", new NumbersFunction());
		table.set("screen_width", zeroArg(() -> LuaInteger.valueOf(Gdx.graphics != null ? Gdx.graphics.getWidth() : 0)));
		table.set("screen_height", zeroArg(() -> LuaInteger.valueOf(Gdx.graphics != null ? Gdx.graphics.getHeight() : 0)));

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

	private static <T> T getProperty(LuaValue value, IntFunction<T> byId, Function<String, T> byName,
			Map<String, T> namedProperties) {
		if (value.isnumber()) {
			return byId.apply(value.toint());
		}
		if (value.isstring()) {
			String name = value.tojstring();
			if (!namedProperties.containsKey(name)) {
				namedProperties.put(name, byName.apply(name));
			}
			return namedProperties.get(name);
		}
		return null;
	}

	private BooleanProperty getBooleanProperty(LuaValue value) {
		return getProperty(value, BooleanPropertyFactory::getBooleanProperty,
				BooleanPropertyFactory::getBooleanProperty, booleanPropertiesByName);
	}

	private IntegerProperty getIntegerProperty(LuaValue value) {
		return getProperty(value, IntegerPropertyFactory::getIntegerProperty,
				IntegerPropertyFactory::getIntegerProperty, integerPropertiesByName);
	}

	private FloatProperty getFloatProperty(LuaValue value) {
		return getProperty(value, FloatPropertyFactory::getFloatProperty,
				FloatPropertyFactory::getFloatProperty, floatPropertiesByName);
	}

	private StringProperty getStringProperty(LuaValue value) {
		return getProperty(value, StringPropertyFactory::getStringProperty,
				StringPropertyFactory::getStringProperty, stringPropertiesByName);
	}

	private LuaValue getNumberValue(LuaValue value) {
		IntegerProperty property = getIntegerProperty(value);
		return LuaInteger.valueOf(property != null ? property.get(state) : 0);
	}

	private class OptionFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			BooleanProperty property = getBooleanProperty(luaValue);
			return LuaBoolean.valueOf(property != null && property.get(state));
		}
	}

	private class NumberFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			return getNumberValue(luaValue);
		}
	}

	private class FloatNumberFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			FloatProperty property = getFloatProperty(luaValue);
			return LuaDouble.valueOf(property != null ? property.get(state) : 0f);
		}
	}

	private class TextFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			StringProperty property = getStringProperty(luaValue);
			String text = property != null ? property.get(state) : null;
			return LuaString.valueOf(text != null ? text : "");
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

	private class TimerIsOnFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue value) {
			return LuaBoolean.valueOf(state.timer.isTimerOn(value.toint()));
		}
	}

	private class TimerIsOffFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue value) {
			return LuaBoolean.valueOf(!state.timer.isTimerOn(value.toint()));
		}
	}

	private class TimerElapsedFunction extends OneArgFunction {
		private final long divisor;

		private TimerElapsedFunction(long divisor) {
			this.divisor = divisor;
		}

		@Override
		public LuaValue call(LuaValue value) {
			long timer = state.timer.getMicroTimer(value.toint());
			return LuaNumber.valueOf(timer == TIMER_OFF_VALUE ? -1 : (state.timer.getNowMicroTime() - timer) / divisor);
		}
	}

	private class TimerElapsedSecondsFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue value) {
			long timer = state.timer.getMicroTimer(value.toint());
			return timer == TIMER_OFF_VALUE ? LuaInteger.valueOf(-1)
					: LuaDouble.valueOf((state.timer.getNowMicroTime() - timer) / 1_000_000.0);
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

	private class NumbersFunction extends VarArgFunction {
		@Override
		public Varargs invoke(Varargs arguments) {
			LuaValue[] values = new LuaValue[arguments.narg()];
			for (int index = 0; index < values.length; index++) {
				values[index] = getNumberValue(arguments.arg(index + 1));
			}
			return LuaValue.varargsOf(values);
		}
	}

	private BMSPlayerInputProcessor getInput() {
		return state != null && state.main != null ? state.main.getInputProcessor() : null;
	}

	private static int inputIndex(LuaValue value, int maximum) {
		if (!value.isnumber()) return -1;
		double number = value.todouble();
		return number >= 0 && number <= maximum && number == (int) number ? (int) number : -1;
	}

	private LuaValue mousePosition(boolean vertical) {
		if (Gdx.input == null || Gdx.graphics == null) return LuaInteger.ZERO;
		int screenSize = vertical ? Gdx.graphics.getHeight() : Gdx.graphics.getWidth();
		if (screenSize <= 0) return LuaInteger.ZERO;
		double position = vertical ? screenSize - Gdx.input.getY() : Gdx.input.getX();
		Skin skin = state != null ? state.getSkin() : null;
		if (skin != null) {
			double scale = vertical ? skin.getScaleY() : skin.getScaleX();
			if (scale > 0) position *= (vertical ? skin.getHeight() : skin.getWidth()) / scale / screenSize;
		}
		return LuaDouble.valueOf(position);
	}

	private class MousePressedFunction extends OneArgFunction {
		private final boolean justPressed;

		MousePressedFunction(boolean justPressed) {
			this.justPressed = justPressed;
		}

		@Override public LuaValue call(LuaValue value) {
			int button = inputIndex(value, Input.Buttons.FORWARD);
			if (button < 0) return LuaBoolean.FALSE;
			BMSPlayerInputProcessor input = getInput();
			return LuaBoolean.valueOf(justPressed ? input != null && input.isSkinMouseJustPressed(button)
					: Gdx.input != null && Gdx.input.isButtonPressed(button));
		}
	}

	private static class KeyPressedFunction extends OneArgFunction {
		private static final Map<String, Integer> KEY_CODES = createKeyCodes();
		private final boolean justPressed;

		KeyPressedFunction(boolean justPressed) {
			this.justPressed = justPressed;
		}

		private static Map<String, Integer> createKeyCodes() {
			Map<String, Integer> codes = new HashMap<>();
			for (int code = 0; code <= 255; code++) {
				String name = Input.Keys.toString(code);
				if (name != null) codes.put(name.toUpperCase(Locale.ROOT), code);
			}
			for (var field : Input.Keys.class.getFields()) {
				if (field.getType() == int.class) {
					try {
						codes.put(field.getName(), field.getInt(null));
					} catch (IllegalAccessException e) {
						throw new ExceptionInInitializerError(e);
					}
				}
			}
			return codes;
		}
		@Override
		public LuaValue call(LuaValue value) {
			if(Gdx.input == null) {
				return LuaBoolean.FALSE;
			}
			int keycode = value.isnumber() ? inputIndex(value, 255)
					: KEY_CODES.getOrDefault(value.tojstring().toUpperCase(Locale.ROOT), -1);
			return LuaBoolean.valueOf(keycode >= 0 && keycode <= 255
					&& (justPressed ? Gdx.input.isKeyJustPressed(keycode) : Gdx.input.isKeyPressed(keycode)));
		}
	}

	@FunctionalInterface
	private interface LuaValueSupplier {
		LuaValue get();
	}
}
