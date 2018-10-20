package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.skin.SkinPropertyMapper;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

public class MainStateAccessor {

	private final MainState state;

	public MainStateAccessor(MainState state) {
		this.state = state;
	}

	public void export(LuaTable table) {
		table.set("timer", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				return LuaNumber.valueOf(state.main.getMicroTimer(value.toint()));
			}
		});
		table.set("timer_off_value", Long.MIN_VALUE);
		table.set("is_timer_on", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				return LuaNumber.valueOf(state.main.isTimerOn(value.toint()));
			}
		});
		table.set("now_timer", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				return LuaNumber.valueOf(state.main.getNowMicroTime(value.toint()));
			}
		});
		table.set("time", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaNumber.valueOf(state.main.getNowMicroTime());
			}
		});
		table.set("set_timer", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue timerId, LuaValue timerValue) {
				int id = timerId.toint();
				if (!SkinPropertyMapper.isTimerWritableBySkin(id))
					throw new IllegalArgumentException("指定されたタイマーはスキンから変更できません");
				state.main.setMicroTimer(id, timerValue.tolong());
				return LuaBoolean.TRUE;
			}
		});
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
				return LuaDouble.valueOf(state.main.getConfig().getSystemvolume());
			}
		});
		table.set("set_volume_sys", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				state.main.getConfig().setSystemvolume(value.tofloat());
				return LuaBoolean.TRUE;
			}
		});
		table.set("volume_key", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.main.getConfig().getKeyvolume());
			}
		});
		table.set("set_volume_key", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				state.main.getConfig().setKeyvolume(value.tofloat());
				return LuaBoolean.TRUE;
			}
		});
		table.set("volume_bg", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaDouble.valueOf(state.main.getConfig().getBgvolume());
			}
		});
		table.set("set_volume_bg", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				state.main.getConfig().setBgvolume(value.tofloat());
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
				if(state instanceof BMSPlayer) {
					BMSPlayer player = (BMSPlayer) state;
					return LuaDouble.valueOf(player.getGauge().getValue());
				}
				return LuaInteger.ZERO;
			}
		});
		table.set("gauge_type", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				if(state instanceof BMSPlayer) {
					BMSPlayer player = (BMSPlayer) state;
					return LuaDouble.valueOf(player.getGauge().getType());
				}
				return LuaInteger.ZERO;
			}
		});
		table.set("event_exec", new VarArgFunction() {
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
		});
	}
}
