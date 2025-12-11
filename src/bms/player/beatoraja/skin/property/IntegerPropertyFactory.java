package bms.player.beatoraja.skin.property;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;
import java.util.Calendar;

import bms.model.Mode;
import bms.player.beatoraja.pattern.Random;
import bms.player.beatoraja.result.MusicResult;
import com.badlogic.gdx.Gdx;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.config.SkinConfiguration;
import bms.player.beatoraja.ir.IRScoreData;
import bms.player.beatoraja.ir.RankingData;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.result.AbstractResult;
import bms.player.beatoraja.result.AbstractResult.TimingDistribution;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.DirectoryBar;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.song.SongData;

public class IntegerPropertyFactory {

	private static final int ID_LENGTH = 65536;
	private static final IntegerProperty[] vcache = new IntegerProperty[ID_LENGTH];
	private static final IntegerProperty[] icache = new IntegerProperty[ID_LENGTH];
	
	public static IntegerProperty getIntegerProperty(int optionid) {
		if(optionid < 0 || optionid >= ID_LENGTH) {
			return null;
		}
		if(vcache[optionid] != null) {
			return vcache[optionid];
		}
		IntegerProperty result = null;
		if (optionid >= NUMBER_DURATION_LANECOVER_ON && optionid <= NUMBER_MAXBPM_DURATION_GREEN_LANECOVER_OFF) {
			final boolean green = (optionid - NUMBER_DURATION_LANECOVER_ON) % 2 == 1;
			final boolean cover = (optionid - NUMBER_DURATION_LANECOVER_ON) % 4 < 2;
			final int mode = (optionid - NUMBER_DURATION_LANECOVER_ON) / 4;
			result = (state) -> {
				if (state instanceof BMSPlayer) {
					final LaneRenderer lanerender = ((BMSPlayer) state).getLanerender();
					double bpm = switch (mode) {
						case 0 -> lanerender.getNowBPM();
						case 1 -> lanerender.getMainBPM();
						case 2 -> lanerender.getMinBPM();
						case 3 -> lanerender.getMaxBPM();
						default -> 0;
					};
					return (int) Math.round((240000 / bpm / lanerender.getHispeed())
							* (cover ? 1 - lanerender.getLanecover() : 1) * (green ? 0.6 : 1));
				}
				return 0;
			};
		}

		if (optionid == NUMBER_TOTALNOTES || optionid == NUMBER_TOTALNOTES2) {
			result = (state) -> {
				if(state instanceof CourseResult) {
					int notes = 0;
					for (BMSModel model : state.main.getPlayerResource().getCourseBMSModels()) {
						notes += model.getTotalNotes();
					}
					return notes;
				}
				return state.main.getPlayerResource().getSongdata() != null
						? state.main.getPlayerResource().getSongdata().getNotes()
						: state.main.getPlayerResource().getCourseData() != null
							? Arrays.asList(state.main.getPlayerResource().getCourseData().getSong()).stream()
								.mapToInt(sd -> sd.getNotes()).sum()
							: Integer.MIN_VALUE;
			};
		}

		if (optionid >= NUMBER_PERFECT2 && optionid <= NUMBER_POOR2) {
			final int index = optionid - NUMBER_PERFECT2;
			result = (state) -> (state.getScoreDataProperty().getScoreData() != null ? 
					state.getScoreDataProperty().getScoreData().getJudgeCount(index) : Integer.MIN_VALUE);
		}
		if (optionid >= NUMBER_PERFECT_RATE && optionid <= NUMBER_POOR_RATE) {
			final int index = optionid - NUMBER_PERFECT_RATE;
			result = (state) -> {
				final ScoreData score = state.getScoreDataProperty().getScoreData();
				return score != null && score.getNotes() > 0 ? score.getJudgeCount(index) * 100 / score.getNotes() : Integer.MIN_VALUE;
						};
		}
		if (optionid >= NUMBER_PERFECT && optionid <= NUMBER_POOR) {
			final int index = optionid - NUMBER_PERFECT;
			result = (state) -> (state.getJudgeCount(index, true) + state.getJudgeCount(index, false));
		}
		if (optionid >= NUMBER_EARLY_PERFECT && optionid <= NUMBER_LATE_POOR) {
			final int index = (optionid - NUMBER_EARLY_PERFECT) / 2;
			final boolean early = (optionid - NUMBER_EARLY_PERFECT) % 2 == 0;
			result = (state) -> (state.getJudgeCount(index, early));
		}
		if (optionid >= NUMBER_RIVAL_PERFECT && optionid <= NUMBER_RIVAL_POOR) {
			final int index = optionid - NUMBER_RIVAL_PERFECT;
			result = (state) -> (state.getScoreDataProperty().getRivalScoreData() != null ? 
					state.getScoreDataProperty().getRivalScoreData().getJudgeCount(index) : Integer.MIN_VALUE);
		}
		if (optionid >= NUMBER_RIVAL_PERFECT_RATE && optionid <= NUMBER_RIVAL_POOR_RATE) {
			final int index = optionid - NUMBER_RIVAL_PERFECT_RATE;
			result = (state) -> {
				final ScoreData rival = state.getScoreDataProperty().getRivalScoreData();
				return rival != null && rival.getNotes() > 0 ? rival.getJudgeCount(index) * 100 / rival.getNotes() : Integer.MIN_VALUE;
						};
		}

		if(result == null) {
			for(ValueType t : ValueType.values()) {
				if(t.id == optionid) {
					result = t.property;
					break;
				}
			}
		}
		
		if (result == null) {
			result = getIntegerProperty0(optionid);
		}
		
		vcache[optionid] = result;
		return result;
	}
	
	private static IntegerProperty getIntegerProperty0(int optionid) {
		switch (optionid) {

		case NUMBER_MISS:
			return (state) -> (state.getJudgeCount(5, true) + state.getJudgeCount(5, false));
		case NUMBER_EARLY_MISS:
			return (state) -> (state.getJudgeCount(5, true));
		case NUMBER_LATE_MISS:
			return (state) -> (state.getJudgeCount(5, false));
		case NUMBER_POOR_PLUS_MISS:
			return (state) -> (state.getJudgeCount(4, true) + state.getJudgeCount(4, false)
					+ state.getJudgeCount(5, true) + state.getJudgeCount(5, false));
		case NUMBER_BAD_PLUS_POOR_PLUS_MISS:
			return (state) -> (state.getJudgeCount(3, true) + state.getJudgeCount(3, false)
					+ state.getJudgeCount(4, true) + state.getJudgeCount(4, false) + state.getJudgeCount(5, true)
					+ state.getJudgeCount(5, false));
		case NUMBER_PLAYLEVEL:
		case NUMBER_FOLDER_BEGINNER:
		case NUMBER_FOLDER_NORMAL:
		case NUMBER_FOLDER_HYPER:
		case NUMBER_FOLDER_ANOTHER:
		case NUMBER_FOLDER_INSANE:
			return (state) -> (state.main.getPlayerResource().getSongdata() != null
					? state.main.getPlayerResource().getSongdata().getLevel()
					: Integer.MIN_VALUE);
		case NUMBER_POINT:
			return (state) -> (state.getScoreDataProperty().getNowScore());
		case NUMBER_MAXSCORE:
			return (state) -> (state.getScoreDataProperty().getScoreData() != null
					? state.getScoreDataProperty().getScoreData().getNotes() * 2
					: 0);
		case NUMBER_DIFF_NEXTRANK:
			return (state) -> (state.getScoreDataProperty().getNextRank());
		case NUMBER_SCORE_RATE:
			return (state) -> (state.getScoreDataProperty().getScoreData() != null
					? state.getScoreDataProperty().getNowRateInt()
					: Integer.MIN_VALUE);
		case NUMBER_SCORE_RATE_AFTERDOT:
			return (state) -> (state.getScoreDataProperty().getScoreData() != null
					? state.getScoreDataProperty().getNowRateAfterDot()
					: Integer.MIN_VALUE);
		case NUMBER_TOTAL_RATE:
		case NUMBER_SCORE_RATE2:
			return (state) -> (state.getScoreDataProperty().getScoreData() != null
					? state.getScoreDataProperty().getRateInt()
					: Integer.MIN_VALUE);
		case NUMBER_TOTAL_RATE_AFTERDOT:
		case NUMBER_SCORE_RATE_AFTERDOT2:
			return (state) -> (state.getScoreDataProperty().getScoreData() != null
					? state.getScoreDataProperty().getRateAfterDot()
					: Integer.MIN_VALUE);
		case NUMBER_BEST_RATE:
			return (state) -> (state.getScoreDataProperty().getBestRateInt());
		case NUMBER_BEST_RATE_AFTERDOT:
			return (state) -> (state.getScoreDataProperty().getBestRateAfterDot());
		case NUMBER_TARGET_SCORE:
		case NUMBER_TARGET_SCORE2:
		case NUMBER_RIVAL_SCORE:
			return (state) -> (state.getScoreDataProperty().getRivalScore());
		case NUMBER_TARGET_SCORE_RATE:
		case NUMBER_TARGET_TOTAL_RATE:
		case NUMBER_TARGET_SCORE_RATE2:
			return (state) -> (state.getScoreDataProperty().getRivalRateInt());
		case NUMBER_TARGET_SCORE_RATE_AFTERDOT:
		case NUMBER_TARGET_TOTAL_RATE_AFTERDOT:
		case NUMBER_TARGET_SCORE_RATE_AFTERDOT2:
			return (state) -> (state.getScoreDataProperty().getRivalRateAfterDot());
		case NUMBER_DIFF_HIGHSCORE:
		case NUMBER_DIFF_HIGHSCORE2:
			return (state) -> (state.getScoreDataProperty().getNowEXScore()
					- state.getScoreDataProperty().getNowBestScore());
		case NUMBER_DIFF_EXSCORE:
		case NUMBER_DIFF_EXSCORE2:
		case NUMBER_DIFF_TARGETSCORE:
			return (state) -> (state.getScoreDataProperty().getNowEXScore()
					- state.getScoreDataProperty().getNowRivalScore());
		case NUMBER_TOTALEARLY:
			return (state) -> {
				int ecount = 0;
				for (int i = 1; i < 6; i++) {
					ecount += state.getJudgeCount(i, true);
				}
				return ecount;
			};
		case NUMBER_TOTALLATE:
			return (state) -> {
				int ecount = 0;
				for (int i = 1; i < 6; i++) {
					ecount += state.getJudgeCount(i, false);
				}
				return ecount;
			};
		case NUMBER_COMBOBREAK:
			return (state) -> (state.getJudgeCount(3, true) + state.getJudgeCount(3, false)
					+ state.getJudgeCount(4, true) + state.getJudgeCount(4, false));
		case NUMBER_HISPEED_LR2:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					return (int) (((BMSPlayer) state).getLanerender().getHispeed() * 100);
				} else if (state.main.getPlayerResource().getSongdata() != null) {
					SongData song = state.main.getPlayerResource().getSongdata();
					PlayConfig pc = state.main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode())
							.getPlayconfig();
					return (int) (pc.getHispeed() * 100);
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_HISPEED:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					return (int) (((BMSPlayer) state).getLanerender().getHispeed());
				} else if (state.main.getPlayerResource().getSongdata() != null) {
					SongData song = state.main.getPlayerResource().getSongdata();
					PlayConfig pc = state.main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode())
							.getPlayconfig();
					return (int) pc.getHispeed();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_HISPEED_AFTERDOT:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					return (int) ((((BMSPlayer) state).getLanerender().getHispeed() * 100) % 100);
				} else if (state.main.getPlayerResource().getSongdata() != null) {
					SongData song = state.main.getPlayerResource().getSongdata();
					PlayConfig pc = state.main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode())
							.getPlayconfig();
					return (int) (pc.getHispeed() * 100) % 100;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_FOLDER_TOTALSONGS:
			return new FolderTotalClearCountProperty(new int[]{0,1,2,3,4,5,6,7,8,9,10});
		case NUMBER_LANECOVER1:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					return (int) (((BMSPlayer) state).getLanerender().getLanecover() * 1000);
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_LANECOVER2:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					LaneRenderer laneRenderer = ((BMSPlayer) state).getLanerender();
					int laneCover = (int)((1.0 - laneRenderer.getLiftRegion()) * laneRenderer.getLanecover() * 1000);
					return laneCover;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_LIFT1:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					return (int) (((BMSPlayer) state).getLanerender().getLiftRegion() * 1000);
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_HIDDEN1:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					return (int) (((BMSPlayer) state).getLanerender().getHiddenCover() * 1000);
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_GROOVEGAUGE:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					return (int) (((BMSPlayer) state).getGauge().getValue());
				}
				if (state instanceof AbstractResult) {
					final int gaugeType = ((AbstractResult) state).getGaugeType();
					return (int) state.resource.getGauge()[gaugeType]
							.get(state.resource.getGauge()[gaugeType].size - 1);
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_GROOVEGAUGE_AFTERDOT:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					final GrooveGauge gauge = ((BMSPlayer) state).getGauge();
					return gauge.getValue() > 0 && gauge.getValue() < 0.1 ? 1 : ((int) (gauge.getValue() * 10)) % 10;
				}
				if (state instanceof AbstractResult) {
					final int gaugeType = ((AbstractResult) state).getGaugeType();
					float value = state.resource.getGauge()[gaugeType]
							.get(state.resource.getGauge()[gaugeType].size - 1) * 10;
					if (value > 0 && value < 1)
						value = 1;
					return ((int) value) % 10;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_MAXCOMBO:
		case NUMBER_MAXCOMBO2:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final ScoreData score = ((MusicSelector)state).getBarManager().getSelected().getScore();
					return score != null ? score.getCombo() : Integer.MIN_VALUE;
				}
				if (state instanceof BMSPlayer) {
					return ((BMSPlayer) state).getJudgeManager().getScoreData().getCombo();
				}
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getNewScore();
					return score != null ? score.getCombo() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_CLEAR:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getNewScore();
					return score != null ? score.getClear() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_TARGET_CLEAR:
			return (state) -> {
				if (state instanceof AbstractResult) {
					return ((AbstractResult) state).getOldScore().getClear();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_SCORE:
		case NUMBER_SCORE2:
		case NUMBER_SCORE3:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getNewScore();
					return score != null ? score.getExscore() : Integer.MIN_VALUE;
				}
				return state.getScoreDataProperty().getScoreData() != null
						? state.getScoreDataProperty().getNowEXScore()
						: Integer.MIN_VALUE;
			};
		case NUMBER_HIGHSCORE:
		case NUMBER_HIGHSCORE2:
			return (state) -> {
				if (state instanceof AbstractResult) {
					return ((AbstractResult) state).getOldScore().getExscore();
				}
				return state.getScoreDataProperty().getBestScore();
			};
		case NUMBER_TARGET_MISSCOUNT:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getOldScore();
					return score.getMinbp() != Integer.MAX_VALUE ? score.getMinbp() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DIFF_MISSCOUNT:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getOldScore();
					return score.getMinbp() != Integer.MAX_VALUE
							? ((AbstractResult) state).getNewScore().getMinbp() - score.getMinbp()
							: Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_TARGET_MAXCOMBO:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getOldScore();
					return score.getCombo() > 0 ? score.getCombo() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DIFF_MAXCOMBO:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getOldScore();
					return score.getCombo() > 0 ? ((AbstractResult) state).getNewScore().getCombo() - score.getCombo()
							: Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_MISSCOUNT:
		case NUMBER_MISSCOUNT2:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final ScoreData score = ((MusicSelector)state).getBarManager().getSelected().getScore();
					return score != null ? score.getMinbp() : Integer.MIN_VALUE;
				}
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getNewScore();
					return score != null ? score.getMinbp() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_IR_TOTALPLAYER:
		case NUMBER_IR_TOTALPLAYER2:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final RankingData irc = ((MusicSelector) state).getCurrentRankingData();
					return irc != null && irc.getState() == RankingData.FINISH ? irc.getTotalPlayer() : Integer.MIN_VALUE;
				}
				if (state instanceof AbstractResult) {
					return ((AbstractResult) state).getState() != AbstractResult.STATE_OFFLINE
							? ((AbstractResult) state).getIRTotalPlayer()
							: Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};			
		}
		
		return null;
	}
	
	public static IntegerProperty getIntegerProperty(String name) {
		for(ValueType t : ValueType.values()) {
			if(t.name().equals(name)) {
				return t.property;
			}
		}
		return null;
	}
	
	public enum ValueType {
		
		notesdisplaytiming(12, (state) -> (state.main.getPlayerResource().getPlayerConfig().getJudgetiming())), 

		playtime_total_hour(17, (state) -> ((int) (state.main.getPlayerResource().getPlayerData().getPlaytime() / 3600))),
		playtime_total_minute(18, (state) -> ((int) (state.main.getPlayerResource().getPlayerData().getPlaytime() / 60) % 60)),
		playtime_totla_saecond(19, (state) -> ((int) (state.main.getPlayerResource().getPlayerData().getPlaytime()) % 60)),

		current_fps(20, (state) -> (Gdx.graphics.getFramesPerSecond())),
		currenttime_year(21, (state) -> (state.main.getCurrnetTime().get(Calendar.YEAR))),
		currenttime_month(22, (state) -> (state.main.getCurrnetTime().get(Calendar.MONTH) + 1)),
		currenttime_day(23, (state) -> (state.main.getCurrnetTime().get(Calendar.DATE))),
		currenttime_hour(24, (state) -> (state.main.getCurrnetTime().get(Calendar.HOUR_OF_DAY))),
		currenttime_minute(25, (state) -> (state.main.getCurrnetTime().get(Calendar.MINUTE))),
		currenttime_saecond(26, (state) -> (state.main.getCurrnetTime().get(Calendar.SECOND))),
		boottime_hour(27, (state) -> ((int) (state.main.getPlayTime() / 3600000))),
		boottime_minute(28, (state) -> ((int) (state.main.getPlayTime() / 60000) % 60)),
		boottime_second(29, (state) -> ((int) (state.main.getPlayTime() / 1000) % 60)),
		
		player_playcount(30, (state) -> ((int)state.resource.getPlayerData().getPlaycount())),
		player_clearcount(31, (state) -> ((int)state.resource.getPlayerData().getClear())),		
		player_failcount(32, (state) -> ((int)state.resource.getPlayerData().getPlaycount() - (int)state.main.getPlayerResource().getPlayerData().getClear())),
		player_perfect(33, (state) -> ((int)state.resource.getPlayerData().getJudgeCount(0))),
		player_great(34, (state) -> ((int)state.resource.getPlayerData().getJudgeCount(1))),
		player_good(35, (state) -> ((int)state.resource.getPlayerData().getJudgeCount(2))),
		player_bad(36, (state) -> ((int)state.resource.getPlayerData().getJudgeCount(3))),
		player_poor(37, (state) -> ((int)state.resource.getPlayerData().getJudgeCount(4))),
		player_notes(333, (state) -> {
			final PlayerData pd = state.resource.getPlayerData();
			return (int) (pd.getJudgeCount(0) + pd.getJudgeCount(1) + pd.getJudgeCount(2) + pd.getJudgeCount(3));
		}),

		volume_system(57, (state) -> ((int)(state.resource.getConfig().getAudioConfig().getSystemvolume() * 100))),
		volume_key(58, (state) -> ((int)(state.resource.getConfig().getAudioConfig().getKeyvolume() * 100))),
		volume_background(59, (state) -> ((int)(state.resource.getConfig().getAudioConfig().getBgvolume() * 100))),

		playcount(77, (state) -> {
			if (state instanceof MusicSelector) {
				final ScoreData score = ((MusicSelector)state).getBarManager().getSelected().getScore();
				return score != null ? score.getPlaycount() : Integer.MIN_VALUE;
			}
			return Integer.MIN_VALUE;
		}),
		clearcount(78, (state) -> {
			if (state instanceof MusicSelector) {
				final ScoreData score = ((MusicSelector)state).getBarManager().getSelected().getScore();
				return score != null ? score.getClearcount() : Integer.MIN_VALUE;
			}
			return Integer.MIN_VALUE;
		}),
		failcount(79, (state) -> {
			if (state instanceof MusicSelector) {
				final ScoreData score = ((MusicSelector)state).getBarManager().getSelected().getScore();
				return score != null ? score.getPlaycount() - score.getClearcount() : Integer.MIN_VALUE;
			}
			return Integer.MIN_VALUE;
		}),

		maxbpm(90, (state) -> (state.resource.getSongdata() != null
				? state.resource.getSongdata().getMaxbpm()
				: Integer.MIN_VALUE)),
		minbpm(91, (state) -> (state.resource.getSongdata() != null
				? state.resource.getSongdata().getMinbpm()
				: Integer.MIN_VALUE)),
		mainbpm(92, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return (int) (song.getInformation().getMainbpm());
			}
			return Integer.MIN_VALUE;
		}),
		nowbpm(160, (state) -> (state instanceof BMSPlayer player ? (int)player.getLanerender().getNowBPM() : Integer.MIN_VALUE)),
		playtime_minute(161, (state) -> ((int) (((int) (state.timer.isTimerOn(TIMER_PLAY) ? state.timer.getNowTime(TIMER_PLAY) : 0)) / 60000))),
		playtime_second(162, (state) -> ((((int) (state.timer.isTimerOn(TIMER_PLAY) ? state.timer.getNowTime(TIMER_PLAY) : 0)) / 1000) % 60)),
		timeleft_minute(163, (state) -> {
			if (state instanceof BMSPlayer) {
				return (int) (Math.max((((BMSPlayer) state).getPlaytime()
						- (int) (state.timer.isTimerOn(TIMER_PLAY) ? state.timer.getNowTime(TIMER_PLAY) : 0) + 1000),
						0) / 60000);
			}
			return Integer.MIN_VALUE;
		}),
		timeleft_second(164, (state) -> {
			if (state instanceof BMSPlayer) {
				return (Math.max((((BMSPlayer) state).getPlaytime()
						- (int) (state.timer.isTimerOn(TIMER_PLAY) ? state.timer.getNowTime(TIMER_PLAY) : 0) + 1000),
						0) / 1000) % 60;
			}
			return Integer.MIN_VALUE;
		}),

		loading_progress(165, (state) -> {
			final BMSResource resource = state.resource.getBMSResource();
			return (int) ((resource.isBGAOn()
					? (resource.getBGAProcessor().getProgress() + resource.getAudioDriver().getProgress()) / 2
					: resource.getAudioDriver().getProgress()) * 100);
		}),
		ir_rank(179, (state) -> {
			if (state instanceof MusicSelector) {
				final RankingData irc = ((MusicSelector) state).getCurrentRankingData();
				return irc != null && irc.getState() == RankingData.FINISH ? irc.getRank() : Integer.MIN_VALUE;
			}
			if (state instanceof AbstractResult) {
				return ((AbstractResult) state).getState() != AbstractResult.STATE_OFFLINE
						? ((AbstractResult) state).getIRRank()
						: Integer.MIN_VALUE;
			}
			return Integer.MIN_VALUE;
		}),
		ir_prevrank(182, (state) -> {
			if (state instanceof AbstractResult) {
				return ((AbstractResult) state).getState() != AbstractResult.STATE_OFFLINE
						? ((AbstractResult) state).getOldIRRank()
						: Integer.MIN_VALUE;
			}
			return Integer.MIN_VALUE;
		}),
		ir_player_noplay(202, createIRClearCountProperty(0)),
		ir_player_failed(210, createIRClearCountProperty(1)),
		ir_player_assist(204, createIRClearCountProperty(2)),
		ir_player_lightassist(206, createIRClearCountProperty(3)),
		ir_player_easy(212, createIRClearCountProperty(4)),
		ir_player_normal(214, createIRClearCountProperty(5)),
		ir_player_hard(216, createIRClearCountProperty(6)),
		ir_player_exhard(208, createIRClearCountProperty(7)),
		ir_player_fullcombo(218, createIRClearCountProperty(8)),
		ir_player_perfect(222, createIRClearCountProperty(9)),
		ir_player_max(224, createIRClearCountProperty(10)),
		
		ir_update_waiting(220, (state) -> {
			if (state instanceof MusicSelector) {
				final long dtime = ((MusicSelector) state).getCurrentRankingDuration();
				if(dtime == -1) {
					return Integer.MIN_VALUE;						
				}
				final long time = state.timer.getTimer(TIMER_SONGBAR_CHANGE) + dtime  - state.timer.getNowTime();
				return (int) (time > 0 ? time / 1000 + 1 : Integer.MIN_VALUE);
			}
			return Integer.MIN_VALUE;
		}),
		ir_totalclear(226, createIRTotalClearCountProperty(new int[]{2,3,4,5,6,7,8,9,10})),
		ir_totalclearrate(227, createIRTotalClearRateProperty(new int[]{2,3,4,5,6,7,8,9,10}, false)),
		ir_totalclearrate_afterdot(241, createIRTotalClearRateProperty(new int[]{2,3,4,5,6,7,8,9,10}, true)),
		ir_totalfullcombo(228,createIRTotalClearCountProperty(new int[]{8,9,10})),
		ir_totalfullcomborate(229,createIRTotalClearRateProperty(new int[]{8,9,10}, false)),
		ir_totalfullcomborate_afterdot(242,createIRTotalClearRateProperty(new int[]{8,9,10}, true)),		

		ir_player_noplay_rate(203, createIRClearRateProperty(0, false)),
		ir_player_noplay_rate_afterdot(230, createIRClearRateProperty(0, true)),
		ir_player_failed_rate(211, createIRClearRateProperty(1, false)),
		ir_player_failed_rate_afterdot(234, createIRClearRateProperty(1, true)),
		ir_player_assist_rate(205, createIRClearRateProperty(2, false)),
		ir_player_assist_rate_afterdot(231, createIRClearRateProperty(2, true)),
		ir_player_lightassist_rate(207, createIRClearRateProperty(3, false)),
		ir_player_lightassist_rate_afterdot(232, createIRClearRateProperty(3, true)),
		ir_player_easy_rate(213, createIRClearRateProperty(4, false)),
		ir_player_easy_rate_afterdot(235, createIRClearRateProperty(4, true)),
		ir_player_normal_rate(215, createIRClearRateProperty(5, false)),
		ir_player_normal_rate_afterdot(236, createIRClearRateProperty(5, true)),
		ir_player_hard_rate(217, createIRClearRateProperty(6, false)),
		ir_player_hard_rate_afterdot(237, createIRClearRateProperty(6, true)),
		ir_player_exhard_rate(209, createIRClearRateProperty(7, false)),
		ir_player_exhard_rate_afterdot(233, createIRClearRateProperty(7, true)),
		ir_player_fullcombo_rate(219, createIRClearRateProperty(8, false)),
		ir_player_fullcombo_rate_afterdot(238, createIRClearRateProperty(8, true)),
		ir_player_perfect_rate(223, createIRClearRateProperty(9, false)),
		ir_player_perfect_rate_afterdot(239, createIRClearRateProperty(9, true)),
		ir_player_max_rate(225, createIRClearRateProperty(10, false)),
		ir_player_max_rate_afterdot(240, createIRClearRateProperty(10, true)),

		duration(312, (state) -> {
			if (state instanceof MusicSelector selector) {
				final PlayConfig pc = selector.getSelectedBarPlayConfig();
				return pc != null ? pc.getDuration() : Integer.MIN_VALUE;
			} else if (state instanceof BMSPlayer player) {
				return player.getLanerender().getCurrentDuration();
			} else if (state.resource.getSongdata() != null) {
				SongData song = state.resource.getSongdata();
				PlayConfig pc = state.resource.getPlayerConfig().getPlayConfig(song.getMode()).getPlayconfig();
				return pc.getDuration();
			}
			return Integer.MIN_VALUE;
		}),
		duration_green(313, (state) -> {
			if (state instanceof MusicSelector selector) {
				final PlayConfig pc = selector.getSelectedBarPlayConfig();
				return pc != null ? pc.getDuration() * 3 / 5 : Integer.MIN_VALUE;
			} else if (state instanceof BMSPlayer player) {
				return player.getLanerender().getCurrentDuration() * 3 / 5;
			} else if (state.main.getPlayerResource().getSongdata() != null) {
				SongData song = state.resource.getSongdata();
				PlayConfig pc = state.resource.getPlayerConfig().getPlayConfig(song.getMode()).getPlayconfig();
				return pc.getDuration() * 3 / 5;
			}
			return Integer.MIN_VALUE;
		}),

		folder_noplay(320, createFolderClearCountProperty(0)),
		folder_failed(321, createFolderClearCountProperty(1)),
		folder_assist(322, createFolderClearCountProperty(2)),
		folder_lightassist(323, createFolderClearCountProperty(3)),
		folder_easy(324, createFolderClearCountProperty(4)),
		folder_normal(325, createFolderClearCountProperty(5)),
		folder_hard(326, createFolderClearCountProperty(6)),
		folder_exhard(327, createFolderClearCountProperty(7)),
		folder_fullcombo(328, createFolderClearCountProperty(8)),
		folder_prefect(329, createFolderClearCountProperty(9)),
		folder_max(330, createFolderClearCountProperty(10)),
	
		chart_totalnote_n(350, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return song.getInformation().getN();
			}
			return Integer.MIN_VALUE;
		}),
		chart_totalnote_ln(351, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return song.getInformation().getLn();
			}
			return Integer.MIN_VALUE;
		}),
		chart_totalnote_s(352, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return song.getInformation().getS();
			}
			return Integer.MIN_VALUE;
		}),
		chart_totalnote_ls(353, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return song.getInformation().getLs();
			}
			return Integer.MIN_VALUE;
		}),
		chart_averagedensity(364, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return (int) song.getInformation().getDensity();
			}
			return Integer.MIN_VALUE;
		}),
		chart_averagedensity_afterdot(365, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return ((int) (song.getInformation().getDensity() * 100)) % 100;
			}
			return Integer.MIN_VALUE;
		}),
		chart_enddensity(362, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return (int) song.getInformation().getEnddensity();
			}
			return Integer.MIN_VALUE;
		}),
		chart_enddensity_peak(363, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return ((int) (song.getInformation().getEnddensity() * 100)) % 100;
			}
			return Integer.MIN_VALUE;
		}),
		chart_peakdensity(360, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return (int) song.getInformation().getPeakdensity();
			}
			return Integer.MIN_VALUE;
		}),
		chart_peakdensity_afterdot(361, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return ((int) (song.getInformation().getPeakdensity() * 100)) % 100;
			}
			return Integer.MIN_VALUE;
		}),
		chart_totalgauge(368, (state) -> {
			final SongData song = state.resource.getSongdata();
			if (song != null && song.getInformation() != null) {
				return (int) song.getInformation().getTotal();
			}
			return Integer.MIN_VALUE;
		}), 

		duration_average(372, (state) -> {
			if (state instanceof AbstractResult) {
				return (int) (((AbstractResult) state).getAverageDuration() / 1000);
			}
			return Integer.MIN_VALUE;
		}),
		duration_average_afterdot(373, (state) -> {
			if (state instanceof AbstractResult) {
				return (int) ((((AbstractResult) state).getAverageDuration() / 10) % 100);
			}
			return Integer.MIN_VALUE;
		}),
		timing_average(374, state -> (state instanceof AbstractResult result ? (int) result.getTimingDistribution().getAverage() : Integer.MIN_VALUE)),
		timing_average_afterdot(375, (state) -> {
			if (state instanceof AbstractResult) {
				TimingDistribution timingDistribution = ((AbstractResult) state).getTimingDistribution();
				if (timingDistribution.getAverage() >= 0.0) {
					return (int) (timingDistribution.getAverage() * 100) % 100;
				} else {
					return (int) ( -1 * ((Math.abs(timingDistribution.getAverage()) * 100) % 100));
				}
			}
			return Integer.MIN_VALUE;
		}),
		timing_stddev(376, (state) -> (state instanceof AbstractResult result ? (int) result.getTimingDistribution().getStdDev() :  Integer.MIN_VALUE)),
		timing_atddev_afterdot(377, (state) -> {
			if (state instanceof AbstractResult) {
				return (int) (((AbstractResult) state).getTimingDistribution().getStdDev() * 100) % 100;
			}
			return Integer.MIN_VALUE;
		}),

		ranking_exscore1(380, createRankingexscore(0)),
		ranking_exscore2(381, createRankingexscore(1)),
		ranking_exscore3(382, createRankingexscore(2)),
		ranking_exscore4(383, createRankingexscore(3)),
		ranking_exscore5(384, createRankingexscore(4)),
		ranking_exscore6(385, createRankingexscore(5)),
		ranking_exscore7(386, createRankingexscore(6)),
		ranking_exscore8(387, createRankingexscore(7)),
		ranking_exscore9(388, createRankingexscore(8)),
		ranking_exscore10(389, createRankingexscore(9)),

		ranking_index1(390, createRankingindex(0)),
		ranking_index2(391, createRankingindex(1)),
		ranking_index3(392, createRankingindex(2)),
		ranking_index4(393, createRankingindex(3)),
		ranking_index5(394, createRankingindex(4)),
		ranking_index6(395, createRankingindex(5)),
		ranking_index7(396, createRankingindex(6)),
		ranking_index8(397, createRankingindex(7)),
		ranking_index9(398, createRankingindex(8)),
		ranking_index10(399, createRankingindex(9)),

		judgerank(400, (state) -> (state.resource.getSongdata() != null
				? state.resource.getSongdata().getJudge() : Integer.MIN_VALUE)),

		judge_duration1(525, createJudgeduration(0)),
		judge_duration2(526, createJudgeduration(1)),
		judge_duration3(527, createJudgeduration(2)),

		chartlength_minute(1163, (state) -> (state.resource.getSongdata() != null
			? (state.resource.getSongdata().getLength() / 60000) % 60
					: Integer.MIN_VALUE)),
		chartlength_second(1164, (state) -> (state.resource.getSongdata() != null
			? (state.resource.getSongdata().getLength() / 1000) % 60
					: Integer.MIN_VALUE)),

		;
		
		/**
		 * property ID
		 */
		private final int id;
		/**
		 * StringProperty
		 */
		private final IntegerProperty property;
		
		private ValueType(int id, IntegerProperty property) {
			this.id = id;
			this.property = property;
		}

		private static IntegerProperty createFolderClearCountProperty(final int clearType) {
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector)state).getBarManager().getSelected();
					if (selected instanceof DirectoryBar) {
						return ((DirectoryBar) selected).getLamps()[clearType];
					}
				}
				return Integer.MIN_VALUE;
			};
		}

		private static IntegerProperty createIRClearCountProperty(final int clearType) {
			return (state) -> {
				RankingData irc = null;
				if (state instanceof MusicSelector) {
					irc = ((MusicSelector) state).getCurrentRankingData();
				} else if(state instanceof AbstractResult) {
					irc = ((AbstractResult) state).getRankingData();
				}
				return irc != null && irc.getState() == RankingData.FINISH ? irc.getClearCount(clearType) : Integer.MIN_VALUE;
			};
		}
		
		private static IntegerProperty createIRClearRateProperty(int clearType, boolean afterdot) {
			return (state) -> {
				RankingData irc = null;
				if (state instanceof MusicSelector) {
					irc = ((MusicSelector) state).getCurrentRankingData();
				} else if(state instanceof AbstractResult) {
					irc = ((AbstractResult) state).getRankingData();
				}
				return irc != null && irc.getState() == RankingData.FINISH && irc.getTotalPlayer() > 0 ?
						(afterdot ? (irc.getClearCount(clearType) * 1000 / irc.getTotalPlayer()) % 10 : irc.getClearCount(clearType) * 100 / irc.getTotalPlayer()) : Integer.MIN_VALUE;
			};
		}
		
		private static IntegerProperty createIRTotalClearCountProperty(int[] clearType) {
			return (state) -> {
				RankingData irc = null;
				if (state instanceof MusicSelector) {
					irc = ((MusicSelector) state).getCurrentRankingData();
				} else if(state instanceof AbstractResult) {
					irc = ((AbstractResult) state).getRankingData();
				}

				if(irc != null && irc.getState() == RankingData.FINISH) {
					int count = 0;
					for(int c : clearType) {
						count += irc.getClearCount(c);
					}
					return count;
				}
				return Integer.MIN_VALUE;
			};
		}

		private static IntegerProperty createIRTotalClearRateProperty (int[] clearType, boolean afterdot) {
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
					return (afterdot ? (count * 1000 / irc.getTotalPlayer()) % 10 : count * 100 / irc.getTotalPlayer());
				}
				return Integer.MIN_VALUE;
			};
		}
		
		private static IntegerProperty createRankingexscore(int index) {
			return (state) -> {
				RankingData irc = null;
				int rankingOffset = 0;
				if (state instanceof MusicSelector) {
					irc = ((MusicSelector) state).getCurrentRankingData();
					rankingOffset = ((MusicSelector) state).getRankingOffset();
				}
				if (state instanceof AbstractResult) {
					irc = ((AbstractResult) state).getRankingData();
					rankingOffset = ((AbstractResult) state).getRankingOffset();
				}
				IRScoreData score = irc != null ? irc.getScore(index + rankingOffset) : null;
				return score != null ? score.getExscore() : Integer.MIN_VALUE;
			};
		}
		
		private static IntegerProperty createRankingindex(int index) {
			return (state) -> {
				RankingData irc = null;
				int rankingOffset = 0;
				if (state instanceof MusicSelector) {
					irc = ((MusicSelector) state).getCurrentRankingData();
					rankingOffset = ((MusicSelector) state).getRankingOffset();
				}
				if (state instanceof AbstractResult) {
					irc = ((AbstractResult) state).getRankingData();
					rankingOffset = ((AbstractResult) state).getRankingOffset();
				}
				return irc != null ? irc.getScoreRanking(index + rankingOffset) : Integer.MIN_VALUE;
			};
		}
		
		private static IntegerProperty createJudgeduration(int player) {
			return (state) -> {
				if (state instanceof BMSPlayer) {
					final JudgeManager judge = ((BMSPlayer) state).getJudgeManager();
					return (int) judge.getRecentJudgeTiming(player);
				}
				return 0;
			};
		}
	}

	public static IntegerProperty getImageIndexProperty(int optionid) {
		if(optionid < 0 || optionid >= icache.length) {
			return null;
		}
		if(icache[optionid] != null) {
			return icache[optionid];
		}
		IntegerProperty result = null;

		if ((optionid >= VALUE_JUDGE_1P_SCRATCH && optionid <= VALUE_JUDGE_2P_KEY9)
				|| (optionid >= VALUE_JUDGE_1P_KEY10 && optionid <= VALUE_JUDGE_2P_KEY99)) {
			result = (state) -> {
				if (state instanceof BMSPlayer) {
					return ((BMSPlayer) state).getJudgeManager().getJudge(optionid);
				}
				return 0;
			};
		}

		if (SkinPropertyMapper.isSkinSelectTypeId(optionid)) {
			final SkinType t = SkinPropertyMapper.getSkinSelectType(optionid);
			result = (state) -> ((state instanceof SkinConfiguration)
					? (((SkinConfiguration) state).getSkinType() == t ? 1 : 0)
					: Integer.MIN_VALUE);
		}
		
		if (result == null) {
			for(IndexType t : IndexType.values()) {
				if(t.id == optionid) {
					result = t.property;
					break;
				}
			}
		}

		icache[optionid] = result;
		return result;
	}
	
	public static IntegerProperty getImageIndexProperty(String name) {
		for(IndexType t : IndexType.values()) {
			if(t.name().equals(name)) {
				return t.property;
			}
		}
		return null;
	}

	public enum IndexType {
		
		showjudgearea(303, (state) -> (state.resource.getPlayerConfig().isShowjudgearea() ? 1 : 0)),
		markprocessednote(305, (state) -> (state.resource.getPlayerConfig().isMarkprocessednote() ? 1 : 0)),
		bpmguide(306, (state) -> (state.resource.getPlayerConfig().isBpmguide() ? 1 : 0)),

		customjudge(301, (state) -> (state.resource.getPlayerConfig().isCustomJudge() ? 1 : 0)),
		lnmode(308, (state) -> {
			if (state instanceof BMSPlayer || state instanceof MusicResult) {
				SongData model = state.resource.getSongdata();
				if (model.hasAnyLongNote() && !model.hasUndefinedLongNote()) { // #LNMODE defined
					if (model.hasLongNote()) {
						return 0;
					} else if (model.hasChargeNote()) {
						return 1;
					} else {
						return 2;
					}
				}
			}
			return state.resource.getPlayerConfig().getLnmode();
		}),
		notesdisplaytimingautoadjust(75, (state) -> (state.resource.getPlayerConfig().isNotesDisplayTimingAutoAdjust() ? 1 : 0)),
		gaugeautoshift(78, (state) -> (state.resource.getPlayerConfig().getGaugeAutoShift())),
		bottomshiftablegauge(341, (state) -> (state.resource.getPlayerConfig().getBottomShiftableGauge())),
		bga(72, (state) -> (state.resource.getConfig().getBga())),
		
		mode(11, (state) -> {
			if (state instanceof MusicSelector) {
				int mode = 0;
				for (; mode < MusicSelector.MODE.length; mode++) {
					if (MusicSelector.MODE[mode] == state.main.getPlayerConfig().getMode()) {
						break;
					}
				}
				final int[] mode_lr2 = { 0, 2, 4, 5, 1, 3 };
				return mode < mode_lr2.length ? mode_lr2[mode] : mode;
			}
			return Integer.MIN_VALUE;
		}),
		sort(12, (state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSort() : Integer.MIN_VALUE)),
		gaugetype_1p(40, (state) -> {
			if(state instanceof BMSPlayer) {
				return ((BMSPlayer)state).getGauge().getType();
			} else if(state instanceof AbstractResult) {
				return ((AbstractResult) state).getGaugeType();
			}
			return state.resource.getPlayerConfig().getGauge();
		}),
		option_1p(42, (state) -> {
			if(state instanceof BMSPlayer) {
				return ((BMSPlayer)state).getOptionInformation().randomoption;
			} else if(state instanceof AbstractResult) {
				return state.resource.getReplayData().randomoption;
			}
			return state.resource.getPlayerConfig().getRandom();
		}),
		option_2p(43, (state) -> {
			if(state instanceof BMSPlayer) {
				return ((BMSPlayer)state).getOptionInformation().randomoption2;
			} else if(state instanceof AbstractResult) {
				return state.resource.getReplayData().randomoption2;
			}
			return state.resource.getPlayerConfig().getRandom2();
		}),
		option_dp(54, (state) -> {
			if(state instanceof BMSPlayer) {
				return ((BMSPlayer)state).getOptionInformation().doubleoption;
			} else if(state instanceof AbstractResult) {
				return state.resource.getReplayData().doubleoption;
			}
			return state.resource.getPlayerConfig().getDoubleoption();
		}),

		hsfix(55, (state) -> {
			if (state.resource.getSongdata() != null) {
				SongData song = state.resource.getSongdata();
				PlayConfig pc = state.resource.getPlayerConfig().getPlayConfig(song.getMode())
						.getPlayconfig();
				return pc.getFixhispeed();
			} else if (state.resource.getCourseData() != null) {
				PlayConfig pc = null;
				for (SongData song : state.resource.getCourseData().getSong()) {
					if (song.getPath() == null) {
						pc = null;
						break;
					}
					PlayConfig pc2 = state.resource.getPlayerConfig().getPlayConfig(song.getMode()).getPlayconfig();
					if (pc == null) {
						pc = pc2;
					}
					if (pc != pc2) {
						pc = null;
						break;
					}
				}
				if (pc != null) {
					return pc.getFixhispeed();
				}
			}
			return Integer.MIN_VALUE;
		}),
		
		option_target1_1p(61, (state) -> {
			final ScoreData rival = (state instanceof BMSPlayer || state instanceof AbstractResult) 
					? state.main.getPlayerResource().getTargetScoreData() : state.getScoreDataProperty().getRivalScoreData();
			if(rival != null && rival.getOption() >= 0) {
				return rival.getOption() % 10;
			}
			return Integer.MIN_VALUE;
		}),
		option_target1_2p(62, (state) -> {
			final ScoreData rival = (state instanceof BMSPlayer || state instanceof AbstractResult) 
					? state.main.getPlayerResource().getTargetScoreData() : state.getScoreDataProperty().getRivalScoreData();
			if(rival != null && rival.getOption() >= 0) {
				return (rival.getOption() / 10) % 10;
			}
			return Integer.MIN_VALUE;
		}),
		option_target1_dp(63, (state) -> {
			final ScoreData rival = (state instanceof BMSPlayer || state instanceof AbstractResult) 
					? state.main.getPlayerResource().getTargetScoreData() : state.getScoreDataProperty().getRivalScoreData();
			if(rival != null && rival.getOption() >= 0) {
				return (rival.getOption() / 100) % 10;
			}
			return Integer.MIN_VALUE;
		}),
		// TODO 64-69はtarget2, target3に割り当てたい

		hispeedautoadjust(342, (state) -> {
			PlayConfig pc = null;
			if(state instanceof MusicSelector) {
				pc = ((MusicSelector)state).getSelectedBarPlayConfig();
			} else {
				pc = state.resource.getPlayerConfig().getPlayConfig(state.resource.getPlayerConfig().getMode()).getPlayconfig();
			}
			if (pc != null) {
				return pc.isEnableHispeedAutoAdjust() ? 1 : 0;
			}
			return Integer.MIN_VALUE;
		}),

		favorite_song(89, (state) -> {
			final SongData sd = state.resource.getSongdata();
			if(sd != null) {
				int type = 1;
				if((sd.getFavorite() & (SongData.FAVORITE_SONG | SongData.INVISIBLE_SONG)) == 0) {
					type = 0;
				} else if((sd.getFavorite() & SongData.INVISIBLE_SONG) != 0) {
					type = 2;
				}
				return type;
			}
			return Integer.MIN_VALUE;
		}),
		favorite_chart(90, (state) -> {
			final SongData sd = state.resource.getSongdata();
			if(sd != null) {
				int type = 1;
				if((sd.getFavorite() & (SongData.FAVORITE_CHART | SongData.INVISIBLE_CHART)) == 0) {
					type = 0;
				} else if((sd.getFavorite() & SongData.INVISIBLE_CHART) != 0) {
					type = 2;
				}
				return type;
			}
			return Integer.MIN_VALUE;
		}),

		autosave_replay1(321, (state) -> (state.resource.getPlayerConfig().getAutoSaveReplay()[0])),
		autosave_replay2(322, (state) -> (state.resource.getPlayerConfig().getAutoSaveReplay()[1])),
		autosave_replay3(323, (state) -> (state.resource.getPlayerConfig().getAutoSaveReplay()[2])),
		autosave_replay4(324, (state) -> (state.resource.getPlayerConfig().getAutoSaveReplay()[3])),

		lanecover(330, (state) -> {
			PlayConfig pc = null;
			if(state instanceof MusicSelector) {
				pc = ((MusicSelector)state).getSelectedBarPlayConfig();
			} else {
				pc = state.resource.getPlayerConfig().getPlayConfig(state.resource.getPlayerConfig().getMode()).getPlayconfig();
			}
			if (pc != null) {
				return pc.isEnablelanecover() ? 1 : 0;
			}
			return Integer.MIN_VALUE;
		}),
		lift(331, (state) -> {
			PlayConfig pc = null;
			if(state instanceof MusicSelector) {
				pc = ((MusicSelector)state).getSelectedBarPlayConfig();
			} else {
				pc = state.resource.getPlayerConfig().getPlayConfig(state.resource.getPlayerConfig().getMode()).getPlayconfig();
			}
			if (pc != null) {
				return pc.isEnablelift() ? 1 : 0;
			}
			return Integer.MIN_VALUE;
		}),
		hidden(332, (state) -> {
			PlayConfig pc = null;
			if(state instanceof MusicSelector) {
				pc = ((MusicSelector)state).getSelectedBarPlayConfig();
			} else {
				pc = state.resource.getPlayerConfig().getPlayConfig(state.resource.getPlayerConfig().getMode()).getPlayconfig();
			}
			if (pc != null) {
				return pc.isEnablehidden() ? 1 : 0;
			}
			return Integer.MIN_VALUE;
		}),

		judgealgorithm(340, (state) -> {
			PlayConfig pc = null;
			if(state instanceof MusicSelector) {
				pc = ((MusicSelector)state).getSelectedBarPlayConfig();
			} else {
				pc = state.resource.getPlayerConfig().getPlayConfig(state.resource.getPlayerConfig().getMode()).getPlayconfig();
			}
			if (pc != null) {
				final String[] algorithms = {JudgeAlgorithm.Combo.name(), JudgeAlgorithm.Duration.name(), JudgeAlgorithm.Lowest.name()};
				final String jt = pc.getJudgetype();
				for (int i = 0; i < algorithms.length; i++) {
					if (jt.equals(algorithms[i])) {
						return i;
					}
				}
			}
			return Integer.MIN_VALUE;
		}),
		guidese(343, (state) -> (state.resource.getPlayerConfig().isGuideSE() ? 1 : 0)),

		extranotedepth(350, (state) -> (state.resource.getPlayerConfig().getExtranoteDepth())),
		minemode(351, (state) -> (state.resource.getPlayerConfig().getMineMode())),
		scrollmode(352, (state) -> (state.resource.getPlayerConfig().getScrollMode())),
		longnotemode(353, (state) -> (state.resource.getPlayerConfig().getLongnoteMode())),

		seventonine_pattern(360, (state) -> (state.resource.getPlayerConfig().getSevenToNinePattern())),
		seventonine_type(361, (state) -> (state.resource.getPlayerConfig().getSevenToNineType())),

		cleartype(370, (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarManager().getSelected();
				return selected.getScore() != null ? selected.getScore().getClear() : Integer.MIN_VALUE;
			} else if (state instanceof AbstractResult) {
				final ScoreData score = ((AbstractResult) state).getNewScore();
				if (score != null) {
					return score.getClear();
				}
				return Integer.MIN_VALUE;
			}
			return Integer.MIN_VALUE;
		}),
		cleartype_target(371, (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarManager().getSelected();
				return selected.getRivalScore() != null ? selected.getRivalScore().getClear() : Integer.MIN_VALUE;
			} else if (state instanceof AbstractResult) {
				return ((AbstractResult) state).getOldScore().getClear();
			}
			return Integer.MIN_VALUE;
		}),
		playertype_ranking1(380, createRankingPlayertypeProperty(0)),
		playertype_ranking2(381, createRankingPlayertypeProperty(1)),
		playertype_ranking3(382, createRankingPlayertypeProperty(2)),
		playertype_ranking4(383, createRankingPlayertypeProperty(3)),
		playertype_ranking5(384, createRankingPlayertypeProperty(4)),
		playertype_ranking6(385, createRankingPlayertypeProperty(5)),
		playertype_ranking7(386, createRankingPlayertypeProperty(6)),
		playertype_ranking8(387, createRankingPlayertypeProperty(7)),
		playertype_ranking9(388, createRankingPlayertypeProperty(8)),
		playertype_ranking10(389, createRankingPlayertypeProperty(9)),
		cleartype_ranking1(390, createRankinCleartypeProperty(0)),
		cleartype_ranking2(391, createRankinCleartypeProperty(1)),
		cleartype_ranking3(392, createRankinCleartypeProperty(2)),
		cleartype_ranking4(393, createRankinCleartypeProperty(3)),
		cleartype_ranking5(394, createRankinCleartypeProperty(4)),
		cleartype_ranking6(395, createRankinCleartypeProperty(5)),
		cleartype_ranking7(396, createRankinCleartypeProperty(6)),
		cleartype_ranking8(397, createRankinCleartypeProperty(7)),
		cleartype_ranking9(398, createRankinCleartypeProperty(8)),
		cleartype_ranking10(399, createRankinCleartypeProperty(9)),

		constant(400, (state) -> {
			if (state instanceof MusicSelector selector) {
				final PlayConfig playConfig = selector.getSelectedBarPlayConfig();
				if (playConfig != null) {
					return playConfig.isEnableConstant() ? 1 : 0;
				}
			} else if (state instanceof BMSPlayer player) {
				return player.getLanerender().getPlayConfig().isEnableConstant() ? 1 : 0;
			}
			return -1;
		}),

		pattern_1p_1(450, getAssignedLane(0, false)),
		pattern_1p_2(451, getAssignedLane(1, false)),
		pattern_1p_3(452, getAssignedLane(2, false)),
		pattern_1p_4(453, getAssignedLane(3, false)),
		pattern_1p_5(454, getAssignedLane(4, false)),
		pattern_1p_6(455, getAssignedLane(5, false)),
		pattern_1p_7(456, getAssignedLane(6, false)),
		pattern_1p_8(457, getAssignedLane(7, false)),
		pattern_1p_9(458, getAssignedLane(8, false)),
		pattern_1p_SCR(459, getAssignedLane(-1, false)),
		pattern_2p_1(460, getAssignedLane(0, true)),
		pattern_2p_2(461, getAssignedLane(1, true)),
		pattern_2p_3(462, getAssignedLane(2, true)),
		pattern_2p_4(463, getAssignedLane(3, true)),
		pattern_2p_5(464, getAssignedLane(4, true)),
		pattern_2p_6(465, getAssignedLane(5, true)),
		pattern_2p_7(466, getAssignedLane(6, true)),
		pattern_2p_SCR(469, getAssignedLane(-1, true)),

		
		// 旧仕様
		assist_constant(BUTTON_ASSIST_CONSTANT, (state) -> (state.resource.getPlayerConfig().getScrollMode() == 1 ? 1 : 0)),
		assist_legacy(BUTTON_ASSIST_LEGACY, (state) -> (state.resource.getPlayerConfig().getLongnoteMode() == 1 ? 1 : 0)),
		assist_nomine(BUTTON_ASSIST_NOMINE, (state) -> (state.resource.getPlayerConfig().getMineMode() == 1 ? 1 : 0)),
		
		;
		/**
		 * property ID
		 */
		private final int id;
		/**
		 * StringProperty
		 */
		private final IntegerProperty property;
		
		private IndexType(int id, IntegerProperty property) {
			this.id = id;
			this.property = property;
		}

		private static IntegerProperty createRankingPlayertypeProperty(int index) {
			return (state) -> {
				RankingData irc = null;
				int rankingOffset = 0;
				if (state instanceof MusicSelector) {
					irc = ((MusicSelector) state).getCurrentRankingData();
					rankingOffset = ((MusicSelector) state).getRankingOffset();
				}
				if (state instanceof AbstractResult) {
					irc = ((AbstractResult) state).getRankingData();
					rankingOffset = ((AbstractResult) state).getRankingOffset();
				}
				return irc != null ? irc.getPlayerType(index + rankingOffset) : Integer.MIN_VALUE;
			};
		}

		private static IntegerProperty createRankinCleartypeProperty(int index) {
			return (state) -> {
				RankingData irc = null;
				int rankingOffset = 0;
				if (state instanceof MusicSelector) {
					irc = ((MusicSelector) state).getCurrentRankingData();
					rankingOffset = ((MusicSelector) state).getRankingOffset();
				}
				if (state instanceof AbstractResult) {
					irc = ((AbstractResult) state).getRankingData();
					rankingOffset = ((AbstractResult) state).getRankingOffset();
				}
				IRScoreData score = irc != null ? irc.getScore(index + rankingOffset) : null;
				return score != null ? score.clear.id : Integer.MIN_VALUE;
			};
		}

		/**
		 * ランダムオプションで割り当てられたレーンを返す
		 */
		private static IntegerProperty getAssignedLane(int key, boolean is2PSide){
			return (state) -> {
				if (!(state instanceof MusicResult)){
					return 0;
				}

				ReplayData rd = state.resource.getReplayData();
				Mode mode = state.resource.getBMSModel().getMode();
				Random type = Random.getRandom(is2PSide? rd.randomoption2: rd.randomoption, mode);

				switch (type){
					case RANDOM:
					case ROTATE:
					case CROSS:
					case RANDOM_EX:
						break;
					default:
						return 0;
				}

				if(rd.laneShufflePattern == null){ // patternModifyLogで再現されたリプレイの場合が該当
					return 0;
				}

				if(mode.player == 1 && is2PSide){
					return 0;
				}
				int keyNum = mode.key / mode.player;

				int index;
				if(key >= keyNum || (mode.scratchKey.length != 0 && key == mode.scratchKey[0])) {
					return 0;
				} else if(key == -1){ // scratch
					if (mode.scratchKey.length == 0 || type != Random.RANDOM_EX){ // no scratch
						return 0;
					}
					index = mode.scratchKey[0];
				} else {
					index = key;
				}

				int[] pattern = rd.laneShufflePattern[is2PSide? 1 : 0];
				if (pattern == null){
					return 0;
				}
				return pattern[index] + 1 - (is2PSide? keyNum: 0);
			};
		}
	}

	private static class FolderTotalClearCountProperty implements IntegerProperty {

		private final int[] clearType;
		
		public FolderTotalClearCountProperty(int[] clearType) {
			this.clearType = clearType;
		}
		@Override
		public int get(MainState state) {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector)state).getBarManager().getSelected();
				if (selected instanceof DirectoryBar) {
					int[] lamps = ((DirectoryBar) selected).getLamps();
					int count = 0;
					for (int clear : clearType) {
						count += lamps[clear];
					}
					return count;
				}
			}
			return Integer.MIN_VALUE;
		}		
	}	
}
