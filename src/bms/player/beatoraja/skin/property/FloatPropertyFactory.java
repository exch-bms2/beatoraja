package bms.player.beatoraja.skin.property;

import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.player.beatoraja.BMSResource;
import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.PlayConfig;
import bms.player.beatoraja.config.SkinConfiguration;
import bms.player.beatoraja.ir.RankingData;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.GradeBar;
import bms.player.beatoraja.select.bar.SongBar;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.result.AbstractResult;

/**
 * FloatProperty/FloatWriterのFactoryクラス
 * 
 * @author exch
 */
public class FloatPropertyFactory {
	
	private static RateType[] RateTypeValues = RateType.values();
	private static FloatType[] FloatTypeValues = FloatType.values();

	private static final int PG = 0;
	private static final int GR = 1;
	private static final int GD = 2;
	private static final int BD = 3;
	private static final int PR = 4;

	/**
	 * RateType IDに対応するFloatPropertyを返す
	 * 
	 * @param id property ID
	 * @return 対応するFloatProperty
	 */
	public static FloatProperty getRateProperty(int optionid) {
		for(RateType t : RateTypeValues) {
			if(t.id == optionid) {
				return t.property;
			}
		}
		return null;
	}

	/**
	 * RateType名に対応するFloatPropertyを返す
	 * 
	 * @param name property name
	 * @return 対応するFloatProperty
	 */
	public static FloatProperty getRateProperty(String name) {
		for(RateType t : RateTypeValues) {
			if(t.name().equals(name)) {
				return t.property;
			}
		}
		return null;
	}

	/**
	 * property IDに対応するFloatWriterを返す
	 * 
	 * @param id property ID
	 * @return 対応するFloatWriter
	 */
	public static FloatWriter getRateWriter(int id) {
		for(RateType t : RateTypeValues) {
			if(t.id == id) {
				return t.writer;
			}
		}
		return null;
	}
	
	/**
	 * property nameに対応するFloatWriterを返す
	 * 
	 * @param name property name
	 * @return 対応するFloatWriter
	 */
	public static FloatWriter getRateWriter(String name) {
		for(RateType t : RateTypeValues) {
			if(t.name().equals(name)) {
				return t.writer;
			}
		}
		return null;
	}

	/**
	 * FloatType IDに対応するFloatPropertyを返す
	 * なければRateType IDに対応するFloatPropertyを返す
	 * 
	 * @param id property ID
	 * @return 対応するFloatProperty
	 */
	public static FloatProperty getFloatProperty(int optionid) {
		for(FloatType t : FloatTypeValues) {
			if(t.id == optionid) {
				return t.property;
			}
		}
		for(RateType r : RateTypeValues) {
			if(r.id == optionid) {
				return r.property;
			}
		}
		return null;
	}

	/**
	 * FloatType名に対応するFloatPropertyを返す
	 * なければRateTYpe名に対応するFloatPropertyを返す
	 * 
	 * @param name property name
	 * @return 対応するFloatProperty
	 */
	public static FloatProperty getFloatProperty(String name) {
		for(FloatType t : FloatTypeValues) {
			if(t.name().equals(name)) {
				return t.property;
			}
		}
		for(RateType r : RateTypeValues) {
			if(r.name().equals(name)) {
				return r.property;
			}
		}
		return null;
	}

	private static FloatProperty createMusicProgress() {
		return (state) -> {
			if (state instanceof BMSPlayer) {
				if (state.timer.isTimerOn(TIMER_PLAY)) {
					return Math.min((float) state.timer.getNowTime(TIMER_PLAY) / ((BMSPlayer) state).getPlaytime(),
							1);
				}
			}
			return 0;
		};
	}
	
	public enum RateType {
		
		musicselect_position(1, 
				(state) -> (state instanceof MusicSelector ? ((MusicSelector) state).getBarManager().getSelectedPosition() : 0), 
				(state, value) -> {
					if(state instanceof MusicSelector) {
						final MusicSelector select = (MusicSelector) state;
						select.selectedBarMoved();
						select.getBarManager().setSelectedPosition(value);
					}
				}),
		lanecover(4, (state) -> {
			if (state instanceof BMSPlayer) {
				final PlayConfig pc = ((BMSPlayer) state).getLanerender().getPlayConfig();
				if (pc.isEnablelanecover()) {
					float lane = pc.getLanecover();
					if (pc.isEnablelift()) {
						lane = lane * (1 - pc.getLift());
					}
					return lane;
				}
			}
			return 0;
		}),
		lanecover2(5, (state) -> {
			if (state instanceof BMSPlayer) {
				final PlayConfig pc = ((BMSPlayer) state).getLanerender().getPlayConfig();
				if (pc.isEnablelanecover()) {
					float lane = pc.getLanecover();
					if (pc.isEnablelift()) {
						lane = lane * (1 - pc.getLift());
					}
					return lane;
				}
			}
			return 0;
		}),
		music_progress(6, createMusicProgress()),
		skinselect_position(7,
				(state) -> ((state instanceof SkinConfiguration) ? ((SkinConfiguration) state).getSkinSelectPosition() : 0),
				(state, value) -> {
					if(state instanceof SkinConfiguration) {
						((SkinConfiguration) state).setSkinSelectPosition(value);
					}
				}),
		ranking_position(8,
				(state) -> {
					if(state instanceof MusicSelector) {
						return ((MusicSelector) state).getRankingPosition();
					}
					if(state instanceof AbstractResult) {
						return ((AbstractResult) state).getRankingPosition();
					}
					return 0;
				},
				(state, value) -> {
					if(state instanceof MusicSelector) {
						((MusicSelector) state).setRankingPosition(value);
					}
					if(state instanceof AbstractResult) {
						((AbstractResult) state).setRankingPosition(value);
					}
				}),
		mastervolume(17,
				(state) -> (state.resource.getConfig().getAudioConfig().getSystemvolume()),
				(state, value) -> {
					state.resource.getConfig().getAudioConfig().setSystemvolume(value);					
				}),
		keyvolume(18,
				(state) -> (state.resource.getConfig().getAudioConfig().getKeyvolume()),
				(state, value) -> {
					state.resource.getConfig().getAudioConfig().setKeyvolume(value);					
				}),
		bgmvolume(19,
				(state) -> (state.resource.getConfig().getAudioConfig().getBgvolume()),
				(state, value) -> {
					state.resource.getConfig().getAudioConfig().setBgvolume(value);					
				}),
		music_progress_bar(101, createMusicProgress()),
		load_progress(102, (state) -> {
			final BMSResource resource = state.resource.getBMSResource();
			return resource.isBGAOn()
					? (resource.getBGAProcessor().getProgress() + resource.getAudioDriver().getProgress()) / 2
					: resource.getAudioDriver().getProgress();
		}),
		level(103, getLevelRate(-1)),
		level_beginner(105, getLevelRate(1)),
		level_normal(106, getLevelRate(2)),
		level_hyper(107, getLevelRate(3)),
		level_another(108, getLevelRate(4)),
		level_insane(109, getLevelRate(5)),
		scorerate(110, (state) -> (state.getScoreDataProperty().getRate())),
		scorerate_final(111, (state) -> (state.getScoreDataProperty().getNowRate())),
		bestscorerate_now(112, (state) -> (state.getScoreDataProperty().getNowBestScoreRate())),
		bestscorerate(113, (state) -> (state.getScoreDataProperty().getBestScoreRate())),
		targetscorerate_now(114, (state) -> (state.getScoreDataProperty().getNowRivalScoreRate())),
		targetscorerate(115, (state) -> (state.getScoreDataProperty().getRivalScoreRate())),
		rate_pgreat(140, createJudgeRate(0)),
		rate_great(141, createJudgeRate(1)),
		rate_good(142, createJudgeRate(2)),
		rate_bad(143, createJudgeRate(3)),
		rate_poor(144, createJudgeRate(4)),
		rate_maxcombo(145, (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarManager().getSelected();
				if (selected instanceof SongBar) {
					ScoreData score = selected.getScore();
					return score != null
							? ((float) score.getCombo()) / ((SongBar) selected).getSongData().getNotes()
							: 0;
				}
				if (selected instanceof GradeBar) {
					ScoreData score = selected.getScore();
					if (score == null) return 0;
					int notes = 0;
					for (SongData songData : ((GradeBar) selected).getSongDatas()) {
						notes += songData.getNotes();
					}
					return ((float) score.getCombo()) / notes;
				}
			}
			return 0;
		}),
		rate_exscore(147, (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarManager().getSelected();
				if (selected instanceof SongBar) {
					ScoreData score = selected.getScore();
					return score != null
							? ((float) score.getExscore()) / ((SongBar) selected).getSongData().getNotes() / 2
							: 0;
				}
				if (selected instanceof GradeBar) {
					ScoreData score = selected.getScore();
					if (score == null) return 0;
					int notes = 0;
					for (SongData songData : ((GradeBar) selected).getSongDatas()) {
						notes += songData.getNotes();
					}
					return ((float) score.getExscore()) / notes;
				}
			}
			return 0;
		}),
		;
		
		private final int id;
		private final FloatProperty property;
		private final FloatWriter writer;

		private RateType(int id, FloatProperty property) {
			this(id, property, null);
		}

		private RateType(int id, FloatProperty property, FloatWriter writer) {
			this.id = id;
			this.property = property;
			this.writer = writer;
		}
	}
	
	public enum FloatType {

		score_rate(1102, (state) -> {
			if (state.getScoreDataProperty().getScoreData() != null) {
				return state.getScoreDataProperty().getNowRate();
			} else {
				return Float.MIN_VALUE;
			}
		}),
		total_rate(1115, (state) -> {
			if (state.getScoreDataProperty().getScoreData() != null) {
				return state.getScoreDataProperty().getRate();
			} else {
				return Float.MIN_VALUE;
			}
		}),
		score_rate2(155, FloatType.total_rate.property),

		duration_average(372, (state) ->{
			if (state instanceof AbstractResult) {
				return ((AbstractResult) state).getAverageDuration() / 1000.0f;
			}
			return Float.MIN_VALUE;
		}),
		timing_average(374, (state) ->{
			if (state instanceof AbstractResult) {
				return ((AbstractResult) state).getTimingDistribution().getArrayCenter() / 1000.0f;
			}
			return Float.MIN_VALUE;
		}),
		timign_stddev(376, (state) ->{
			if (state instanceof AbstractResult) {
				return ((AbstractResult) state).getTimingDistribution().getStdDev();
			}
			return Float.MIN_VALUE;
		}),
		perfect_rate(85, (state) -> {
			final var score = state.getScoreDataProperty().getScoreData();
			if (score != null && score.getNotes() > 0) {
				return 1.0f *  score.getJudgeCount(PG) / score.getNotes();
			}
			return Float.MIN_VALUE;
		}),
		great_rate(86, (state) -> {
			final var score = state.getScoreDataProperty().getScoreData();
			if (score != null && score.getNotes() > 0) {
				return 1.0f *  score.getJudgeCount(GR) / score.getNotes();
			}
			return Float.MIN_VALUE;
		}),
		good_rate(87, (state) -> {
			final var score = state.getScoreDataProperty().getScoreData();
			if (score != null && score.getNotes() > 0) {
				return 1.0f *  score.getJudgeCount(GD) / score.getNotes();
			}
			return Float.MIN_VALUE;
		}),
		bad_rate(88, (state) -> {
			final var score = state.getScoreDataProperty().getScoreData();
			if (score != null && score.getNotes() > 0) {
				return 1.0f *  score.getJudgeCount(BD) / score.getNotes();
			}
			return Float.MIN_VALUE;
		}),
		poor_rate(89, (state) -> {
			final var score = state.getScoreDataProperty().getScoreData();
			if (score != null && score.getNotes() > 0) {
				return 1.0f *  score.getJudgeCount(PR) / score.getNotes();
			}
			return Float.MIN_VALUE;
		}),
		rival_perfect_rate(285, (state) -> {
			final var score = state.getScoreDataProperty().getRivalScoreData();
			if (score != null && score.getNotes() > 0) {
				return 1.0f *  score.getJudgeCount(PG) / score.getNotes();
			}
			return Float.MIN_VALUE;
		}),
		rival_great_rate(286, (state) -> {
			final var score = state.getScoreDataProperty().getRivalScoreData();
			if (score != null && score.getNotes() > 0) {
				return 1.0f *  score.getJudgeCount(GR) / score.getNotes();
			}
			return Float.MIN_VALUE;
		}),
		rival_good_rate(287, (state) -> {
			final var score = state.getScoreDataProperty().getRivalScoreData();
			if (score != null && score.getNotes() > 0) {
				return 1.0f *  score.getJudgeCount(GD) / score.getNotes();
			}
			return Float.MIN_VALUE;
		}),
		rival_bad_rate(288, (state) -> {
			final var score = state.getScoreDataProperty().getRivalScoreData();
			if (score != null && score.getNotes() > 0) {
				return 1.0f *  score.getJudgeCount(BD) / score.getNotes();
			}
			return Float.MIN_VALUE;
		}),
		rival_poor_rate(289, (state) -> {
			final var score = state.getScoreDataProperty().getRivalScoreData();
			if (score != null && score.getNotes() > 0) {
				return 1.0f *  score.getJudgeCount(PR) / score.getNotes();
			}
			return Float.MIN_VALUE;
		}),
		best_rate(183, state -> state.getScoreDataProperty().getBestScoreRate()),
		rival_rate(122, state -> state.getScoreDataProperty().getRivalScoreRate()),
		target_rate(135, FloatType.rival_rate.property),
		target_rate2(157, FloatType.rival_rate.property),

		hispeed(310, (state) -> {
			if (state instanceof BMSPlayer) {
				return ((BMSPlayer) state).getLanerender().getHispeed();
			} else if (state.main.getPlayerResource().getSongdata() != null) {
				var song = state.main.getPlayerResource().getSongdata();
				var pc = state.main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode())
						.getPlayconfig();
				return pc.getHispeed();
			}
			return Float.MIN_VALUE;
		}),
		groovegauge_1p(1107, (state) -> {
			if (state instanceof BMSPlayer) {
				return ((BMSPlayer) state).getGauge().getValue();
			}
			if (state instanceof AbstractResult) {
				final int gaugeType = ((AbstractResult) state).getGaugeType();
				return state.resource.getGauge()[gaugeType].get(state.resource.getGauge()[gaugeType].size - 1);
			}
			return Float.MIN_VALUE;
		}),
		chart_averagedensity(367, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return (float) song.getInformation().getDensity();
			}
			return Float.MIN_VALUE;
		}),
		chart_enddensity(362, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return (float) song.getInformation().getEnddensity();
			}
			return Float.MIN_VALUE;
		}),
		chart_peakdensity(360, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return (float) song.getInformation().getPeakdensity();
			}
			return Float.MIN_VALUE;
		}),
		chart_totalgauge(368, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return (float) song.getInformation().getTotal();
			}
			return Float.MIN_VALUE;
		}),
		loading_progress(165, (state) -> {
			final BMSResource resource = state.resource.getBMSResource();
			return resource.isBGAOn()
					? (resource.getBGAProcessor().getProgress() + resource.getAudioDriver().getProgress()) / 2
					: resource.getAudioDriver().getProgress();
		}),
		ir_totalclearrate(227, createIRTotalClearRateProperty(new int[]{2,3,4,5,6,7,8,9,10})),
		ir_totalfullcomborate(229,createIRTotalClearRateProperty(new int[]{8,9,10})),
		ir_player_noplay_rate(203, createIRClearRateProperty(0)),
		ir_player_failed_rate(211, createIRClearRateProperty(1)),
		ir_player_assist_rate(205, createIRClearRateProperty(2)),
		ir_player_lightassist_rate(207, createIRClearRateProperty(3)),
		ir_player_easy_rate(213, createIRClearRateProperty(4)),
		ir_player_normal_rate(215, createIRClearRateProperty(5)),
		ir_player_hard_rate(217, createIRClearRateProperty(6)),
		ir_player_exhard_rate(209, createIRClearRateProperty(7)),
		ir_player_fullcombo_rate(219, createIRClearRateProperty(8)),
		ir_player_perfect_rate(223, createIRClearRateProperty(9)),
		ir_player_max_rate(225, createIRClearRateProperty(10));

		private final int id;
		private final FloatProperty property;

		private FloatType(int id, FloatProperty property) {
			this.id = id;
			this.property = property;
		}

	}

	private static FloatProperty createIRClearRateProperty(int clearType) {
		return (state) -> {
			RankingData irc = null;
			if (state instanceof MusicSelector) {
				irc = ((MusicSelector) state).getCurrentRankingData();
			} else if(state instanceof AbstractResult) {
				irc = ((AbstractResult) state).getRankingData();
			}
			if (irc != null && irc.getState() == RankingData.FINISH && irc.getTotalPlayer() > 0) {
				return 1.0f * irc.getClearCount(clearType) / irc.getTotalPlayer();
			} else {
				return Float.MIN_VALUE;
			}
		};
	}

	private static FloatProperty createIRTotalClearRateProperty (int[] clearType) {
		return (state) -> {
			RankingData irc = null;
			if (state instanceof MusicSelector) {
				irc = ((MusicSelector) state).getCurrentRankingData();
			} else if(state instanceof AbstractResult) {
				irc = ((AbstractResult) state).getRankingData();
			}
			if(irc != null && irc.getState() == RankingData.FINISH && irc.getTotalPlayer() > 0) {
				int count = 0;
				for(int c : clearType) {
					count += irc.getClearCount(c);
				}
				return 1.0f * count / irc.getTotalPlayer();
			}
			return Float.MIN_VALUE;
		};
	}

	private static FloatProperty createJudgeRate(final int judge) {
		return (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarManager().getSelected();
				if (selected instanceof SongBar) {
					ScoreData score = selected.getScore();
					return score != null ? ((float) (score.getJudgeCount(judge))) / ((SongBar) selected).getSongData().getNotes() : 0;
				}
				if (selected instanceof GradeBar) {
					ScoreData score = selected.getScore();
					if (score == null) return 0;
					int notes = 0;
					for (SongData songData : ((GradeBar) selected).getSongDatas()) {
						notes += songData.getNotes();
					}
					return ((float) score.getJudgeCount(judge)) / notes;
				}
			}
			return 0;
		};
	}

	private static FloatProperty getLevelRate(final int difficulty) {
		return (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarManager().getSelected();
				if (selected instanceof SongBar && ((SongBar) selected).getSongData() != null) {
					SongData sd = ((SongBar) selected).getSongData();
					if (difficulty >= 0 && sd.getDifficulty() != difficulty) {
						return 0;
					}
					int maxLevel = 0;
					switch (sd.getMode()) {
					case 5:
					case 10:
						maxLevel = 9;
					case 7:
					case 14:
						maxLevel = 12;
					case 9:
						maxLevel = 50;
					case 25:
					case 50:
						maxLevel = 10;
					}
					if (maxLevel > 0) {
						return 1.0f * sd.getLevel() / maxLevel;
					}
				}
			}
			return 0;
		};
	}

}
