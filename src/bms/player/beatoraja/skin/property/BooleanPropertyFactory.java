package bms.player.beatoraja.skin.property;

import static bms.player.beatoraja.ClearType.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.ScoreData.SongTrophy;
import bms.player.beatoraja.ir.RankingData;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.JudgeManager;
import bms.player.beatoraja.play.GrooveGauge.Gauge;
import bms.player.beatoraja.result.AbstractResult;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.*;
import bms.player.beatoraja.skin.SkinProperty;
import bms.player.beatoraja.song.SongData;

public class BooleanPropertyFactory {

	private static final int ID_LENGTH = 65536;
	private static final BooleanProperty[] pcache = new BooleanProperty[ID_LENGTH];
	private static final BooleanProperty[] npcache = new BooleanProperty[ID_LENGTH];
	
	public static BooleanProperty getBooleanProperty(int optionid) {
		final int id = Math.abs(optionid);
		if(id >= ID_LENGTH) {
			return null;
		}
		if(optionid >= 0 && pcache[id] != null) {
			return pcache[id];
		}
		if(optionid < 0 && npcache[id] != null) {
			return npcache[id];
		}
		BooleanProperty result = null;
		for(BooleanType t : BooleanType.values()) {
			if(t.id == id) {
				result = t.property;
				break;
			}
		}
		
		if (id >= OPTION_COURSE_STAGE1 && id <= OPTION_COURSE_STAGE4) {
			int index = id - OPTION_COURSE_STAGE1;
			result = new DrawProperty(DrawProperty.TYPE_STATIC_WITHOUT_MUSICSELECT, (state) -> {
				final CourseData course = state.resource.getCourseData();
				final int courseIndex = state.resource.getCourseIndex();
				return course != null && index == courseIndex && index != course.getSong().length - 1;
			});
		} else if (id == OPTION_COURSE_STAGE_FINAL) {
			result = new DrawProperty(DrawProperty.TYPE_STATIC_WITHOUT_MUSICSELECT, (state) -> {
				final CourseData course = state.resource.getCourseData();
				final int courseIndex = state.resource.getCourseIndex();
				return course != null && courseIndex == course.getSong().length - 1;				
			});
		}

		if (result == null) {
			result = getBooleanProperty0(id);
		}

		if (result != null) {
			if(optionid < 0) {
				final BooleanProperty dc = result;
				result = new BooleanProperty() {
					@Override
					public boolean isStatic(MainState state) {
						return dc.isStatic(state);
					}

					public boolean get(MainState state) {
						return !dc.get(state);
					}
				};
				npcache[id] = result;
			} else {
				pcache[id] = result;				
			}
		}
		
		return result;
	}
	
	private static BooleanProperty getBooleanProperty0(int optionid) {
		switch (optionid) {
		case OPTION_TABLE_SONG:
			return new DrawProperty(DrawProperty.TYPE_STATIC_WITHOUT_MUSICSELECT,
					(state) -> (state.resource.getTablename().length() != 0));
		case OPTION_RANDOMSELECTBAR:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedBar() instanceof ExecutableBar : false));
		case OPTION_RANDOMCOURSEBAR:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedBar() instanceof RandomCourseBar : false));
		case OPTION_PLAYABLEBAR:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							Bar selected = ((MusicSelector) state).getSelectedBar();
							return ((selected instanceof SongBar) && ((SongBar)selected).getSongData().getPath() != null) ||
									((selected instanceof GradeBar) && ((GradeBar)selected).existsAllSongs()) ||
									((selected instanceof RandomCourseBar) && ((RandomCourseBar)selected).existsAllSongs()) ||
									(selected instanceof ExecutableBar);
						}
						return false;
					});
		case OPTION_NOT_COMPARE_RIVAL:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getRival() == null : false));
		case OPTION_COMPARE_RIVAL:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getRival() != null : false));
		case OPTION_SELECT_BAR_NOT_PLAYED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							Bar current = ((MusicSelector) state).getSelectedBar();
							ScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
							return (current instanceof SongBar || current instanceof GradeBar)
									&& (score == null || (score != null && score.getClear() == NoPlay.id));
						}
						return false;
					});
		case OPTION_MODE_COURSE:
			return new DrawProperty(DrawProperty.TYPE_STATIC_WITHOUT_MUSICSELECT,
					(state) -> (state.main.getPlayerResource().getCourseData() != null));
		case OPTION_DISABLE_SAVE_SCORE:
			// TODO select, decide時の実装
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> (!state.main.getPlayerResource().isUpdateScore()));
		case OPTION_ENABLE_SAVE_SCORE:
			// TODO select, decide時の実装
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> (state.main.getPlayerResource().isUpdateScore()));
		case OPTION_NO_SAVE_CLEAR:
			// TODO 未実装
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> (state.main.getPlayerResource().isUpdateScore()));

		}
		
		return null;
	}
	
	private static DrawProperty createCourseDataConstraintProperty(CourseData.CourseDataConstraint constraint) {
		return new DrawProperty(DrawProperty.TYPE_NO_STATIC,(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(constraint) : false));
	}

	private static class DrawProperty extends DrawConditionProperty {

		public final BProperty bool;

		public DrawProperty(BProperty type, BProperty bool) {
			super(type);
			this.bool = bool;
		}

		@Override
		public boolean get(MainState state) {
			return bool.get(state);
		}
	}

	private interface BProperty {
		public boolean get(MainState state);
	}

	private static abstract class DrawConditionProperty implements BooleanProperty {

		public final BProperty type;

		public static final BProperty TYPE_NO_STATIC = (state) -> (false);
		public static final BProperty TYPE_STATIC_WITHOUT_MUSICSELECT = (state) -> !(state instanceof MusicSelector);
		public static final BProperty TYPE_STATIC_ON_RESULT = (state) -> (state instanceof MusicResult) || (state instanceof CourseResult);
		public static final BProperty TYPE_STATIC_ALL = (state) -> (true);

		public DrawConditionProperty(BProperty type) {
			this.type = type;
		}

		@Override
		public boolean isStatic(MainState state) {
			return type.get(state);
		}

	}
	
	private static class NowJudgeDrawCondition extends DrawProperty {

		public NowJudgeDrawCondition(final int player, final int type) {
			super(TYPE_NO_STATIC, (state) -> {
				if (state instanceof BMSPlayer) {
					JudgeManager judge = ((BMSPlayer) state).getJudgeManager();
					if (type == 0) {
						return judge.getNowJudge(player) == 1;
					} else if (type == 1) {
						return judge.getNowJudge(player) > 1 && judge.getRecentJudgeTiming(player) > 0;
					} else {
						return judge.getNowJudge(player) > 1 && judge.getRecentJudgeTiming(player) < 0;
					}
				}
				return false;				
			});
		}
	}

	private static class SongDataBooleanProperty extends DrawConditionProperty {

		public final SProperty bool;

		public SongDataBooleanProperty(SProperty bool) {
			super(TYPE_STATIC_WITHOUT_MUSICSELECT);
			this.bool = bool;
		}

		@Override
		public boolean get(MainState state) {
			final SongData model = state.resource.getSongdata();
			return model != null && bool.isTrue(model);
		}

	}

	private interface SProperty {
		public boolean isTrue(SongData model);
	}

	private static class SelectedBarClearDrawCondition extends DrawProperty {

		public SelectedBarClearDrawCondition(final ClearType type) {
			super(TYPE_NO_STATIC, (state) -> {
				if(state instanceof MusicSelector) {
					ScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
					return score != null ? score.getClear() == type.id : false; 
				}
				return false;				
			});
		}
	}

	private static class NowRankDrawCondition extends DrawConditionProperty {

		private final int low;
		private final int high;

		public NowRankDrawCondition(int rank) {
			super(TYPE_STATIC_ON_RESULT);
			final int[] values = { 0, 6, 9, 12, 15, 18, 21, 24, 28 };
			low = values[rank];
			high = values[rank + 1];
		}

		@Override
		public boolean get(MainState state) {
			final ScoreDataProperty score = state.getScoreDataProperty();
			return score.qualifyNowRank(low) && (high > 27 ? true : !score.qualifyNowRank(high));
		}

	}

	private static class BestRankDrawCondition extends DrawConditionProperty {

		private final int low;
		private final int high;

		public BestRankDrawCondition(int rank) {
			super(TYPE_STATIC_ON_RESULT);
			final int[] values = { 0, 6, 9, 12, 15, 18, 21, 24, 28 };
			low = values[rank];
			high = values[rank + 1];
		}

		@Override
		public boolean get(MainState state) {
			final ScoreDataProperty score = state.getScoreDataProperty();
			return score.qualifyBestRank(low) && (high > 27 ? true : !score.qualifyBestRank(high));
		}

	}

	private static class GaugeDrawCondition extends DrawConditionProperty {

		private final float low;
		private final float high;

		public GaugeDrawCondition(int range) {
			super(TYPE_STATIC_ON_RESULT);
			low = (range) * 0.1f;
			high = (range + 1) * 0.1f;
		}

		@Override
		public boolean get(MainState state) {
			if (state instanceof BMSPlayer) {
				final Gauge gauge = ((BMSPlayer) state).getGauge().getGauge();
				return gauge.getValue() >= low * gauge.getProperty().max
						&& gauge.getValue() < high * gauge.getProperty().max;
			}
			return false;
		}

	}

	private static class TrophyDrawCondition extends DrawProperty {

		public TrophyDrawCondition(final SongTrophy trophy) {
			super(TYPE_STATIC_WITHOUT_MUSICSELECT, (state) -> {
				final ScoreData score = state.getScoreDataProperty().getScoreData();
				return score != null && score.getTrophy() != null && score.getTrophy().indexOf(trophy.character) >= 0;				
			});
		}
	}

	private static DrawProperty createReplayProperty(final int index, final int type) {
		return new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> {
			if (state instanceof MusicSelector) {
				final Bar current = ((MusicSelector) state).getSelectedBar();
				return (current instanceof SelectableBar)
						&& index >= 0 && index < MusicSelector.REPLAY
						&& (type == 0 ? ((SelectableBar) current).existsReplay(index)
								: !((SelectableBar) current).existsReplay(index));
			} else if (state instanceof AbstractResult) {
				return ((AbstractResult) state).getReplayStatus(index) == (type == 0 ? AbstractResult.ReplayStatus.EXIST
						: (type == 1 ? AbstractResult.ReplayStatus.NOT_EXIST : AbstractResult.ReplayStatus.SAVED));
			}
			return false;				
		});
	}
	
	public enum BooleanType {
		
		bgaoff(40, new DrawProperty(DrawProperty.TYPE_STATIC_WITHOUT_MUSICSELECT, (state) -> (!state.resource.getBMSResource().isBGAOn()))),
		bgaon(41, new DrawProperty(DrawProperty.TYPE_STATIC_WITHOUT_MUSICSELECT, (state) -> (state.resource.getBMSResource().isBGAOn()))),
		gauge_groove(42, new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> {
			int type = Integer.MIN_VALUE;
			if (state instanceof BMSPlayer) {
				type = ((BMSPlayer) state).getGauge().getType();
			} else if (state instanceof AbstractResult) {
				type = ((AbstractResult) state).getGaugeType();
			}
			if (type != Integer.MIN_VALUE)
				return type <= 2;
			return false;
		})),
		gauge_hard(43, new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> {
			int type = Integer.MIN_VALUE;
			if (state instanceof BMSPlayer) {
				type = ((BMSPlayer) state).getGauge().getType();
			} else if (state instanceof AbstractResult) {
				type = ((AbstractResult) state).getGaugeType();
			}
			if (type != Integer.MIN_VALUE)
				return type >= 3;
			return false;			
		})),
		autoplay_on(OPTION_AUTOPLAYON, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof BMSPlayer) ? state.resource.getPlayMode().mode == BMSPlayerMode.Mode.AUTOPLAY : false))),
		autoplay_off(OPTION_AUTOPLAYOFF, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof BMSPlayer) ? state.resource.getPlayMode().mode != BMSPlayerMode.Mode.AUTOPLAY : false))),
		replay_off(OPTION_REPLAY_OFF, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof BMSPlayer) ? state.resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY || ((BMSPlayer) state).main.getPlayerResource().getPlayMode().mode == BMSPlayerMode.Mode.PRACTICE : false))),
		replay_playing(OPTION_REPLAY_PLAYING, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof BMSPlayer) ? state.resource.getPlayMode().mode == BMSPlayerMode.Mode.REPLAY : false))),
		state_practice(OPTION_STATE_PRACTICE, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getState() == BMSPlayer.STATE_PRACTICE : false))),
		now_loading(OPTION_NOW_LOADING, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getState() == BMSPlayer.STATE_PRELOAD : false))),
		loaded(OPTION_LOADED, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getState() != BMSPlayer.STATE_PRELOAD : false))),

		song_no_text(OPTION_NO_TEXT, new SongDataBooleanProperty(model -> !model.hasDocument())),
		song_text(OPTION_TEXT, new SongDataBooleanProperty(model -> model.hasDocument())),
		chart_no_ln(OPTION_NO_LN, new SongDataBooleanProperty(model -> !model.hasAnyLongNote())),
		chart_ln(OPTION_LN, new SongDataBooleanProperty(model -> model.hasAnyLongNote())),
		song_no_bga(OPTION_NO_BGA, new SongDataBooleanProperty(model -> !model.hasBGA())),
		song_bga(OPTION_BGA, new SongDataBooleanProperty(model -> model.hasBGA())),
		chart_no_randomsequence(OPTION_NO_RANDOMSEQUENCE, new SongDataBooleanProperty(model -> !model.hasRandomSequence())),
		chart_randomsequence(OPTION_RANDOMSEQUENCE, new SongDataBooleanProperty(model -> model.hasRandomSequence())),
		chart_no_bpmchange(OPTION_NO_BPMCHANGE, new SongDataBooleanProperty(model -> model.getMinbpm() == model.getMaxbpm())),
		chart_bpmchange(OPTION_BPMCHANGE, new SongDataBooleanProperty(model -> model.getMinbpm() < model.getMaxbpm())),
		chart_bpmstop(OPTION_BPMSTOP, new SongDataBooleanProperty(model -> model.isBpmstop())),
		chart_difficulty_0(OPTION_DIFFICULTY0, new SongDataBooleanProperty(model -> model.getDifficulty() <= 0 || model.getDifficulty() > 5)),
		chart_difficulty_1(OPTION_DIFFICULTY1, new SongDataBooleanProperty(model -> model.getDifficulty() == 1)),
		chart_difficulty_2(OPTION_DIFFICULTY2, new SongDataBooleanProperty(model -> model.getDifficulty() == 2)),
		chart_difficulty_3(OPTION_DIFFICULTY3, new SongDataBooleanProperty(model -> model.getDifficulty() == 3)),
		chart_difficulty_4(OPTION_DIFFICULTY4, new SongDataBooleanProperty(model -> model.getDifficulty() == 4)),
		chart_difficulty_5(OPTION_DIFFICULTY5, new SongDataBooleanProperty(model -> model.getDifficulty() == 5)),
		chart_judge_veryhard(OPTION_JUDGE_VERYHARD, new SongDataBooleanProperty(model -> model.getJudge() == 0 || (model.getJudge() >= 10 && model.getJudge() < 35))),
		chart_judge_hard(OPTION_JUDGE_HARD, new SongDataBooleanProperty(model -> model.getJudge() == 1 || (model.getJudge() >= 35 && model.getJudge() < 60))),
		chart_judge_normal(OPTION_JUDGE_NORMAL, new SongDataBooleanProperty(model -> model.getJudge() == 2 || (model.getJudge() >= 60 && model.getJudge() < 85))),
		chart_judge_easy(OPTION_JUDGE_EASY, new SongDataBooleanProperty(model -> model.getJudge() == 3 || (model.getJudge() >= 85 && model.getJudge() < 110))),
		chart_judge_veryeasy(OPTION_JUDGE_VERYEASY, new SongDataBooleanProperty(model -> model.getJudge() == 4 || model.getJudge() >= 110)),
		
		chart_7key(160, new SongDataBooleanProperty((model) -> (model.getMode() == Mode.BEAT_7K.id))),
		chart_5key(161, new SongDataBooleanProperty((model) -> (model.getMode() == Mode.BEAT_5K.id))),
		chart_14key(162, new SongDataBooleanProperty((model) -> (model.getMode() == Mode.BEAT_14K.id))),
		chart_10key(163, new SongDataBooleanProperty((model) -> (model.getMode() == Mode.BEAT_10K.id))),
		chart_9key(164, new SongDataBooleanProperty((model) -> (model.getMode() == Mode.POPN_9K.id))),
		
		select_bar_failed(OPTION_SELECT_BAR_FAILED, new SelectedBarClearDrawCondition(Failed)),
		select_bar_assist_easy(OPTION_SELECT_BAR_ASSIST_EASY_CLEARED, new SelectedBarClearDrawCondition(AssistEasy)),
		select_bar_light_assist_easy(OPTION_SELECT_BAR_LIGHT_ASSIST_EASY_CLEARED, new SelectedBarClearDrawCondition(LightAssistEasy)),
		select_bar_easy(OPTION_SELECT_BAR_EASY_CLEARED, new SelectedBarClearDrawCondition(Easy)),
		select_bar_normal(OPTION_SELECT_BAR_NORMAL_CLEARED, new SelectedBarClearDrawCondition(Normal)),
		select_bar_hard(OPTION_SELECT_BAR_HARD_CLEARED, new SelectedBarClearDrawCondition(Hard)),
		select_bar_exhard(OPTION_SELECT_BAR_EXHARD_CLEARED, new SelectedBarClearDrawCondition(ExHard)),
		select_bar_fullcombo(OPTION_SELECT_BAR_FULL_COMBO_CLEARED, new SelectedBarClearDrawCondition(FullCombo)),
		select_bar_perfect(OPTION_SELECT_BAR_PERFECT_CLEARED, new SelectedBarClearDrawCondition(Perfect)),
		select_bar_max(OPTION_SELECT_BAR_MAX_CLEARED, new SelectedBarClearDrawCondition(Max)),

		replaydata_exist_1(OPTION_REPLAYDATA, createReplayProperty(0, 0)),
		replaydata_exist_2(OPTION_REPLAYDATA2, createReplayProperty(1, 0)),
		replaydata_exist_3(OPTION_REPLAYDATA3, createReplayProperty(2, 0)),
		replaydata_exist_4(OPTION_REPLAYDATA4, createReplayProperty(3, 0)),
		replaydata_no_1(OPTION_NO_REPLAYDATA, createReplayProperty(0, 1)),
		replaydata_no_2(OPTION_NO_REPLAYDATA2, createReplayProperty(1, 1)),
		replaydata_no_3(OPTION_NO_REPLAYDATA3, createReplayProperty(2, 1)),
		replaydata_no_4(OPTION_NO_REPLAYDATA4, createReplayProperty(3, 1)),
		replaydata_saved_1(OPTION_REPLAYDATA_SAVED, createReplayProperty(0, 2)),
		replaydata_saved_2(OPTION_REPLAYDATA2_SAVED, createReplayProperty(1, 2)),
		replaydata_saved_3(OPTION_REPLAYDATA3_SAVED, createReplayProperty(2, 2)),
		replaydata_saved_4(OPTION_REPLAYDATA4_SAVED, createReplayProperty(3, 2)),
		
		select_replaydata_1(OPTION_SELECT_REPLAYDATA, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedReplay() == 0 : false))),
		select_replaydata_2(OPTION_SELECT_REPLAYDATA2, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedReplay() == 1 : false))),
		select_replaydata_3(OPTION_SELECT_REPLAYDATA3, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedReplay() == 2 : false))),
		select_replaydata_4(OPTION_SELECT_REPLAYDATA4, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedReplay() == 3 : false))),


		select_panel1(OPTION_PANEL1, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getPanelState() == 1 : false))),
		select_panel2(OPTION_PANEL2, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getPanelState() == 2 : false))),
		select_panel3(OPTION_PANEL3, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getPanelState() == 3 : false))),
		select_somgbar(OPTION_SONGBAR, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedBar() instanceof SongBar : false))),
		select_folderbar(OPTION_FOLDERBAR, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedBar() instanceof DirectoryBar : false))),
		select_coursebar(OPTION_GRADEBAR, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedBar() instanceof GradeBar : false))),

		course_class(OPTION_GRADEBAR_CLASS,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.CLASS)),
		course_mirror(OPTION_GRADEBAR_MIRROR,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.MIRROR)),
		course_random(OPTION_GRADEBAR_RANDOM,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.RANDOM)),
		course_nospeed(OPTION_GRADEBAR_NOSPEED,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.NO_SPEED)),
		course_nogood(OPTION_GRADEBAR_NOGOOD,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.NO_GOOD)),
		course_nogreat(OPTION_GRADEBAR_NOGREAT,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.NO_GREAT)),
		course_gauge_lr2(OPTION_GRADEBAR_GAUGE_LR2,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.GAUGE_LR2)),
		course_gauge_5keys(OPTION_GRADEBAR_GAUGE_5KEYS,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.GAUGE_5KEYS)),
		course_gauge_7keys(OPTION_GRADEBAR_GAUGE_7KEYS,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.GAUGE_7KEYS)),
		course_gauge_9keys(OPTION_GRADEBAR_GAUGE_9KEYS,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.GAUGE_9KEYS)),
		course_gauge_24keys(OPTION_GRADEBAR_GAUGE_24KEYS, createCourseDataConstraintProperty(CourseData.CourseDataConstraint.GAUGE_24KEYS)),
		course_ln(OPTION_GRADEBAR_LN,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.LN)),
		course_cn(OPTION_GRADEBAR_CN,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.CN)),
		course_hcn(OPTION_GRADEBAR_HCN,createCourseDataConstraintProperty(CourseData.CourseDataConstraint.HCN)),

		stagefile(OPTION_STAGEFILE, new DrawProperty(DrawProperty.TYPE_STATIC_WITHOUT_MUSICSELECT, 
				(state) -> (state.main.getPlayerResource().getBMSResource().getStagefile() != null))),
		no_stagefile(OPTION_NO_STAGEFILE, new DrawProperty(DrawProperty.TYPE_STATIC_WITHOUT_MUSICSELECT,
				(state) -> (state.main.getPlayerResource().getBMSResource().getStagefile() == null))),
		backbmp(OPTION_BACKBMP, new DrawProperty(DrawProperty.TYPE_STATIC_WITHOUT_MUSICSELECT,
				(state) -> (state.main.getPlayerResource().getBMSResource().getBackbmp() != null))),
		no_backbmp(OPTION_NO_BACKBMP, new DrawProperty(DrawProperty.TYPE_STATIC_WITHOUT_MUSICSELECT,
				(state) -> (state.main.getPlayerResource().getBMSResource().getBackbmp() == null))),
		banner(OPTION_BANNER, new DrawProperty(DrawProperty.TYPE_STATIC_WITHOUT_MUSICSELECT,
				(state) -> (state.main.getPlayerResource().getBMSResource().getBanner() != null))),
		no_banner(OPTION_NO_BANNER, new DrawProperty(DrawProperty.TYPE_STATIC_WITHOUT_MUSICSELECT,
				(state) -> (state.main.getPlayerResource().getBMSResource().getBanner() == null))),

		judge_1p_perfect(OPTION_1P_PERFECT, new NowJudgeDrawCondition(0, 0)),
		judge_1p_early(OPTION_1P_EARLY, new NowJudgeDrawCondition(0, 1)),
		judge_1p_late(OPTION_1P_LATE, new NowJudgeDrawCondition(0, 2)),
		judge_2p_perfect(OPTION_2P_PERFECT, new NowJudgeDrawCondition(1, 0)),
		judge_2p_early(OPTION_2P_EARLY, new NowJudgeDrawCondition(1, 1)),
		judge_2p_late(OPTION_2P_LATE, new NowJudgeDrawCondition(1, 2)),
		judge_3p_perfect(OPTION_3P_PERFECT, new NowJudgeDrawCondition(2, 0)),
		judge_3p_early(OPTION_3P_EARLY, new NowJudgeDrawCondition(2, 1)),
		judge_3p_late(OPTION_3P_LATE, new NowJudgeDrawCondition(2, 2)),
		
		judge_perfect_exist(OPTION_PERFECT_EXIST, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT,
				(state) -> (state.getJudgeCount(0, true) + state.getJudgeCount(0, false) > 0))),
		judge_great_exist(OPTION_GREAT_EXIST, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT,
				(state) -> (state.getJudgeCount(1, true) + state.getJudgeCount(1, false) > 0))),
		judge_good_exist(OPTION_GOOD_EXIST, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT,
				(state) -> (state.getJudgeCount(2, true) + state.getJudgeCount(2, false) > 0))),
		judge_bad_exist(OPTION_BAD_EXIST, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT,
				(state) -> (state.getJudgeCount(3, true) + state.getJudgeCount(3, false) > 0))),
		judge_poor_exist(OPTION_POOR_EXIST, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT,
				(state) -> (state.getJudgeCount(4, true) + state.getJudgeCount(4, false) > 0))),
		judge_miss_exist(OPTION_MISS_EXIST, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT,
				(state) -> (state.getJudgeCount(5, true) + state.getJudgeCount(5, false) > 0))),
		
		lanecover1_changing(OPTION_LANECOVER1_CHANGING, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> (state.main.getInputProcessor().startPressed() || state.main.getInputProcessor().isSelectPressed()))),
		lanecover1_on(OPTION_LANECOVER1_ON, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getLanerender().getPlayConfig().isEnablelanecover() : false))),
		lift1_on(OPTION_LIFT1_ON, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getLanerender().getPlayConfig().isEnablelift() : false))),
		hidden1_on(OPTION_HIDDEN1_ON, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getLanerender().getPlayConfig().isEnablehidden() : false))),
		border_or_more_1p(OPTION_1P_BORDER_OR_MORE, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getGauge().getGauge().isQualified() : false))),

		gauge_1p_0_10(OPTION_1P_0_9, new GaugeDrawCondition(0)),
		gauge_1p_10_20(OPTION_1P_10_19, new GaugeDrawCondition(1)),
		gauge_1p_20_30(OPTION_1P_20_29, new GaugeDrawCondition(2)),
		gauge_1p_30_40(OPTION_1P_30_39, new GaugeDrawCondition(3)),
		gauge_1p_40_50(OPTION_1P_40_49, new GaugeDrawCondition(4)),
		gauge_1p_50_60(OPTION_1P_50_59, new GaugeDrawCondition(5)),
		gauge_1p_60_70(OPTION_1P_60_69, new GaugeDrawCondition(6)),
		gauge_1p_70_80(OPTION_1P_70_79, new GaugeDrawCondition(7)),
		gauge_1p_80_90(OPTION_1P_80_89, new GaugeDrawCondition(8)),
		gauge_1p_90_100(OPTION_1P_90_99, new GaugeDrawCondition(9)),
		gauge_1p_100(OPTION_1P_100, new GaugeDrawCondition(10)),
		rank_1p_aaa(OPTION_1P_AAA, new NowRankDrawCondition(7)),
		rank_1p_aa(OPTION_1P_AA, new NowRankDrawCondition(6)),
		rank_1p_a(OPTION_1P_A, new NowRankDrawCondition(5)),
		rank_1p_b(OPTION_1P_B, new NowRankDrawCondition(4)),
		rank_1p_c(OPTION_1P_C, new NowRankDrawCondition(3)),
		rank_1p_d(OPTION_1P_D, new NowRankDrawCondition(2)),
		rank_1p_e(OPTION_1P_E, new NowRankDrawCondition(1)),
		rank_1p_f(OPTION_1P_F, new NowRankDrawCondition(0)),
		rank_result_1p_aaa(OPTION_RESULT_AAA_1P, new NowRankDrawCondition(7)),
		rank_result_1p_aa(OPTION_RESULT_AA_1P, new NowRankDrawCondition(6)),
		rank_result_1p_a(OPTION_RESULT_A_1P, new NowRankDrawCondition(5)),
		rank_result_1p_b(OPTION_RESULT_B_1P, new NowRankDrawCondition(4)),
		rank_result_1p_c(OPTION_RESULT_C_1P, new NowRankDrawCondition(3)),
		rank_result_1p_d(OPTION_RESULT_D_1P, new NowRankDrawCondition(2)),
		rank_result_1p_e(OPTION_RESULT_E_1P, new NowRankDrawCondition(1)),
		rank_result_1p_f(OPTION_RESULT_F_1P, new NowRankDrawCondition(0)),
		rank_now_1p_aaa(OPTION_NOW_AAA_1P, new NowRankDrawCondition(7)),
		rank_now_1p_aa(OPTION_NOW_AA_1P, new NowRankDrawCondition(6)),
		rank_now_1p_a(OPTION_NOW_A_1P, new NowRankDrawCondition(5)),
		rank_now_1p_b(OPTION_NOW_B_1P, new NowRankDrawCondition(4)),
		rank_now_1p_c(OPTION_NOW_C_1P, new NowRankDrawCondition(3)),
		rank_now_1p_d(OPTION_NOW_D_1P, new NowRankDrawCondition(2)),
		rank_now_1p_e(OPTION_NOW_E_1P, new NowRankDrawCondition(1)),
		rank_now_1p_f(OPTION_NOW_F_1P, new NowRankDrawCondition(0)),
		rank_best_1p_aaa(OPTION_BEST_AAA_1P, new BestRankDrawCondition(7)),
		rank_best_1p_aa(OPTION_BEST_AA_1P, new BestRankDrawCondition(6)),
		rank_best_1p_a(OPTION_BEST_A_1P, new BestRankDrawCondition(5)),
		rank_best_1p_b(OPTION_BEST_B_1P, new BestRankDrawCondition(4)),
		rank_best_1p_c(OPTION_BEST_C_1P, new BestRankDrawCondition(3)),
		rank_best_1p_d(OPTION_BEST_D_1P, new BestRankDrawCondition(2)),
		rank_best_1p_e(OPTION_BEST_E_1P, new BestRankDrawCondition(1)),
		rank_best_1p_f(OPTION_BEST_F_1P, new BestRankDrawCondition(0)),
		rank_aaa(OPTION_AAA, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT, (state) -> (state.getScoreDataProperty().qualifyRank(24)))),
		rank_aa(OPTION_AA, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT, (state) -> (state.getScoreDataProperty().qualifyRank(21)))),
		rank_a(OPTION_A, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT, (state) -> (state.getScoreDataProperty().qualifyRank(18)))),
		rank_b(OPTION_B, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT, (state) -> (state.getScoreDataProperty().qualifyRank(15)))),
		rank_c(OPTION_C, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT, (state) -> (state.getScoreDataProperty().qualifyRank(12)))),
		rank_d(OPTION_D, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT, (state) -> (state.getScoreDataProperty().qualifyRank(9)))),
		rank_e(OPTION_E, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT, (state) -> (state.getScoreDataProperty().qualifyRank(6)))),
		rank_f(OPTION_F, new DrawProperty(DrawProperty.TYPE_STATIC_ON_RESULT, (state) -> (state.getScoreDataProperty().qualifyRank(0)))),
		update_score(OPTION_UPDATE_SCORE, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof AbstractResult) ? ((AbstractResult) state).getNewScore().getExscore() > ((AbstractResult) state).getOldScore().getExscore() : false))),
		draw_score(OPTION_DRAW_SCORE, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof AbstractResult) ? ((AbstractResult) state).getNewScore().getExscore() == ((AbstractResult) state).getOldScore().getExscore() : false))),
		update_maxcombo(OPTION_UPDATE_MAXCOMBO, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof AbstractResult) ? ((AbstractResult) state).getNewScore().getCombo() > ((AbstractResult) state).getOldScore().getCombo() : false))),
		draw_maxcombo(OPTION_DRAW_MAXCOMBO, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof AbstractResult) ? ((AbstractResult) state).getNewScore().getCombo() == ((AbstractResult) state).getOldScore().getCombo() : false))),
		update_misscount(OPTION_UPDATE_MISSCOUNT, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof AbstractResult) ? ((AbstractResult) state).getNewScore().getMinbp() < ((AbstractResult) state).getOldScore().getMinbp() : false))),
		draw_misscount(OPTION_DRAW_MISSCOUNT, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof AbstractResult) ? ((AbstractResult) state).getNewScore().getMinbp() == ((AbstractResult) state).getOldScore().getMinbp() : false))),
		update_scorerank(OPTION_UPDATE_SCORERANK, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> (state.getScoreDataProperty().getNowRate() > state.getScoreDataProperty().getBestScoreRate()))),
		draw_scorerank(OPTION_DRAW_SCORERANK, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> (state.getScoreDataProperty().getNowRate() == state.getScoreDataProperty().getBestScoreRate()))),
		update_target(OPTION_UPDATE_TARGET, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof AbstractResult) ? state.main.getPlayerResource().getScoreData().getExscore() > state.getScoreDataProperty().getRivalScore() : false))),
		draw_target(OPTION_DRAW_TARGET, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state) -> ((state instanceof AbstractResult) ? state.main.getPlayerResource().getScoreData().getExscore() == state.getScoreDataProperty().getRivalScore() : false))),

		result_clear(OPTION_RESULT_CLEAR,new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> {
			final ScoreData score = state.main.getPlayerResource().getScoreData();
			final ScoreData cscore = state.main.getPlayerResource().getCourseScoreData();
			return score.getClear() != Failed.id && (cscore == null || cscore.getClear() != Failed.id);
		})),
		result_fail(OPTION_RESULT_FAIL,new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> {
			final ScoreData score = state.main.getPlayerResource().getScoreData();
			final ScoreData cscore = state.main.getPlayerResource().getCourseScoreData();
			return score.getClear() == Failed.id || (cscore != null && cscore.getClear() == Failed.id);
		})),
		result_1pwin(OPTION_1PWIN,new DrawProperty(DrawProperty.TYPE_NO_STATIC, 
				(state) -> (state.getScoreDataProperty().getNowEXScore() > state.getScoreDataProperty().getRivalScore()))),
		result_2pwin(OPTION_2PWIN,new DrawProperty(DrawProperty.TYPE_NO_STATIC, 
				(state) -> (state.getScoreDataProperty().getNowEXScore() < state.getScoreDataProperty().getRivalScore()))),
		result_draw(OPTION_DRAW,new DrawProperty(DrawProperty.TYPE_NO_STATIC, 
				(state) -> (state.getScoreDataProperty().getNowEXScore() == state.getScoreDataProperty().getRivalScore()))),

		ir_offline(OPTION_OFFLINE, new DrawProperty(DrawProperty.TYPE_STATIC_ALL, (state) -> (state.main.getIRStatus().length == 0))),
		ir_online(OPTION_ONLINE, new DrawProperty(DrawProperty.TYPE_STATIC_ALL, (state) -> (state.main.getIRStatus().length > 0))),
		ir_no_player(OPTION_IR_NOPLAYER, new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> {
			if(state instanceof MusicSelector) {
				final RankingData irc = ((MusicSelector)state).getCurrentRankingData();
				return irc != null && irc.getState() == RankingData.FINISH && irc.getTotalPlayer() == 0;
			}
			return false;
		})),
		ir_failed(OPTION_IR_FAILED,new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> {
			if(state instanceof MusicSelector) {
				final RankingData irc = ((MusicSelector)state).getCurrentRankingData();
				return irc != null && irc.getState() == RankingData.FAIL;
			}
			return false;
		})),		
		ir_busy(OPTION_IR_BUSY, new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> {
			if(state instanceof MusicSelector) {
				final RankingData irc = ((MusicSelector)state).getCurrentRankingData();
				return irc != null && irc.getState() == RankingData.FAIL;
			}
			return false;
		})),
		ir_waiting(OPTION_IR_WAITING, new DrawProperty(DrawProperty.TYPE_NO_STATIC,	
				(state) -> ((state instanceof MusicSelector) ? ((MusicSelector)state).getCurrentRankingData() == null : false))),
		
		chart_24key(1160, new SongDataBooleanProperty((model) -> (model.getMode() == Mode.KEYBOARD_24K.id))),
		chart_48key(1161, new SongDataBooleanProperty((model) -> (model.getMode() == Mode.KEYBOARD_24K_DOUBLE.id))),
		gauge_ex(1046, new DrawConditionProperty(DrawConditionProperty.TYPE_NO_STATIC) {
			@Override
			public boolean get(MainState state) {
				int type = Integer.MIN_VALUE;
				if (state instanceof BMSPlayer) {
					type = ((BMSPlayer) state).getGauge().getType();
				} else if (state instanceof AbstractResult) {
					type = ((AbstractResult) state).getGaugeType();
				}
				if (type != Integer.MIN_VALUE)
					return type == 0 || type == 1 || type == 4 || type == 5 || type == 7 || type == 8;
				return false;
			}
		}),
		
		trophy_gauge_easy(OPTION_CLEAR_EASY, new TrophyDrawCondition(SongTrophy.EASY)),
		trophy_gauge_normal(OPTION_CLEAR_GROOVE, new TrophyDrawCondition(SongTrophy.GROOVE)),
		trophy_gauge_hard(OPTION_CLEAR_HARD, new TrophyDrawCondition(SongTrophy.HARD)),
		trophy_gauge_exhard(OPTION_CLEAR_EXHARD, new TrophyDrawCondition(SongTrophy.EXHARD)),
		trophy_option_normal(OPTION_CLEAR_NORMAL, new TrophyDrawCondition(SongTrophy.NORMAL)),
		trophy_option_mirror(OPTION_CLEAR_MIRROR, new TrophyDrawCondition(SongTrophy.MIRROR)),
		trophy_option_random(OPTION_CLEAR_RANDOM, new TrophyDrawCondition(SongTrophy.RANDOM)),
		trophy_option_rrandom(OPTION_CLEAR_RRANDOM, new TrophyDrawCondition(SongTrophy.R_RANDOM)),
		trophy_option_srandom(OPTION_CLEAR_SRANDOM, new TrophyDrawCondition(SongTrophy.S_RANDOM)),
		trophy_option_spiral(OPTION_CLEAR_SPIRAL, new TrophyDrawCondition(SongTrophy.SPIRAL)),
		trophy_option_hrandom(OPTION_CLEAR_HRANDOM, new TrophyDrawCondition(SongTrophy.H_RANDOM)),
		trophy_option_allscr(OPTION_CLEAR_ALLSCR, new TrophyDrawCondition(SongTrophy.ALL_SCR)),
		trophy_option_exrandom(OPTION_CLEAR_EXRANDOM, new TrophyDrawCondition(SongTrophy.EX_RANDOM)),
		trophy_option_exsrandom(OPTION_CLEAR_EXSRANDOM, new TrophyDrawCondition(SongTrophy.EX_S_RANDOM)),

		constant(OPTION_CONSTANT, new DrawProperty(DrawProperty.TYPE_NO_STATIC,
				(state -> {
					if (state instanceof MusicSelector selector) {
						final PlayConfig playConfig = selector.getSelectedBarPlayConfig();
						if (playConfig != null) {
							return playConfig.isEnableConstant();
						}
					} else if (state instanceof BMSPlayer player) {
						return player.resource.getPlayerConfig().getPlayConfig(player.getMode()).getPlayconfig().isEnableConstant();
					}
					return false;
				})
		)),

		;
		/**
		 * property ID
		 */
		private final int id;
		/**
		 * StringProperty
		 */
		private final BooleanProperty property;
		
		private BooleanType(int id, BooleanProperty property) {
			this.id = id;
			this.property = property;
		}
	}
}
