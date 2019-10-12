package bms.player.beatoraja.skin.property;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;
import java.util.Calendar;

import com.badlogic.gdx.Gdx;

import bms.model.BMSModel;
import bms.player.beatoraja.BMSResource;
import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.PlayConfig;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.config.SkinConfiguration;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.GrooveGauge;
import bms.player.beatoraja.play.JudgeManager;
import bms.player.beatoraja.play.LaneRenderer;
import bms.player.beatoraja.result.AbstractResult;
import bms.player.beatoraja.result.AbstractResult.TimingDistribution;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.DirectoryBar;
import bms.player.beatoraja.select.bar.SongBar;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.song.SongData;

public class IntegerPropertyFactory {

	public static IntegerProperty getIntegerProperty(int optionid) {
		IntegerProperty result = null;
		if (optionid == NUMBER_MINBPM) {
			result = (state) -> (state.main.getPlayerResource().getSongdata() != null
					? state.main.getPlayerResource().getSongdata().getMinbpm()
					: Integer.MIN_VALUE);
		}
		if (optionid == NUMBER_MAXBPM) {
			result = (state) -> (state.main.getPlayerResource().getSongdata() != null
					? state.main.getPlayerResource().getSongdata().getMaxbpm()
					: Integer.MIN_VALUE);
		}
		if (optionid == NUMBER_MAINBPM) {
			result = (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return (int) (song.getInformation().getMainbpm());
				}
				return Integer.MIN_VALUE;
			};
		}
		if (optionid >= NUMBER_DURATION_LANECOVER_ON && optionid <= NUMBER_MAXBPM_DURATION_GREEN_LANECOVER_OFF) {
			final boolean green = (optionid - NUMBER_DURATION_LANECOVER_ON) % 2 == 1;
			final boolean cover = (optionid - NUMBER_DURATION_LANECOVER_ON) % 4 < 2;
			final int mode = (optionid - NUMBER_DURATION_LANECOVER_ON) / 4;
			result = (state) -> {
				if (state instanceof BMSPlayer) {
					final LaneRenderer lanerender = ((BMSPlayer) state).getLanerender();
					double bpm = 0;
					switch (mode) {
					case 0:
						bpm = lanerender.getNowBPM();
						break;
					case 1:
						bpm = lanerender.getMainBPM();
						break;
					case 2:
						bpm = lanerender.getMinBPM();
						break;
					case 3:
						bpm = lanerender.getMaxBPM();
						break;
					}
					return (int) Math.round((240000 / bpm / lanerender.getHispeed())
							* (cover ? 1 - lanerender.getLanecover() : 1) * (green ? 1 : 0.6));
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
		if (optionid == NUMBER_SONGLENGTH_MINUTE) {
			result = (state) -> (state.main.getPlayerResource().getSongdata() != null
					? (state.main.getPlayerResource().getSongdata().getLength() / 60000) % 60
					: Integer.MIN_VALUE);
		}
		if (optionid == NUMBER_SONGLENGTH_SECOND) {
			result = (state) -> (state.main.getPlayerResource().getSongdata() != null
					? (state.main.getPlayerResource().getSongdata().getLength() / 1000) % 60
					: Integer.MIN_VALUE);
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

		if (optionid == NUMBER_LOADING_PROGRESS) {
			result = (state) -> {
				final BMSResource resource = state.main.getPlayerResource().getBMSResource();
				return (int) ((resource.isBGAOn()
						? (resource.getBGAProcessor().getProgress() + resource.getAudioDriver().getProgress()) / 2
						: resource.getAudioDriver().getProgress()) * 100);
			};
		}

		if (optionid >= VALUE_JUDGE_1P_DURATION && optionid <= VALUE_JUDGE_3P_DURATION) {
			final int player = optionid - VALUE_JUDGE_1P_DURATION;
			result = (state) -> {
				if (state instanceof BMSPlayer) {
					final JudgeManager judge = ((BMSPlayer) state).getJudgeManager();
					return (int) (judge.getRecentJudgeTiming().length > player ? judge.getRecentJudgeTiming()[player]
							: judge.getRecentJudgeTiming()[0]);
				}
				return 0;
			};
		}

		if (result == null) {
			result = getIntegerProperty0(optionid);
		}
		return result;
	}

	private static IntegerProperty getIntegerProperty0(int optionid) {
		switch (optionid) {
		case NUMBER_JUDGETIMING:
			return (state) -> (state.main.getPlayerResource().getPlayerConfig().getJudgetiming());
		case NUMBER_CURRENT_FPS:
			return (state) -> (Gdx.graphics.getFramesPerSecond());
		case NUMBER_TIME_YEAR:
			return (state) -> (state.main.getCurrnetTime().get(Calendar.YEAR));
		case NUMBER_TIME_MONTH:
			return (state) -> (state.main.getCurrnetTime().get(Calendar.MONTH) + 1);
		case NUMBER_TIME_DAY:
			return (state) -> (state.main.getCurrnetTime().get(Calendar.DATE));
		case NUMBER_TIME_HOUR:
			return (state) -> (state.main.getCurrnetTime().get(Calendar.HOUR_OF_DAY));
		case NUMBER_TIME_MINUTE:
			return (state) -> (state.main.getCurrnetTime().get(Calendar.MINUTE));
		case NUMBER_TIME_SECOND:
			return (state) -> (state.main.getCurrnetTime().get(Calendar.SECOND));

		case NUMBER_OPERATING_TIME_HOUR:
			return (state) -> ((int) (state.main.getPlayTime() / 3600000));
		case NUMBER_OPERATING_TIME_MINUTE:
			return (state) -> ((int) (state.main.getPlayTime() / 60000) % 60);
		case NUMBER_OPERATING_TIME_SECOND:
			return (state) -> ((int) (state.main.getPlayTime() / 1000) % 60);
		case NUMBER_TOTALPLAYTIME_HOUR:
			return (state) -> ((int) (state.main.getPlayerResource().getPlayerData().getPlaytime() / 3600));
		case NUMBER_TOTALPLAYTIME_MINUTE:
			return (state) -> ((int) (state.main.getPlayerResource().getPlayerData().getPlaytime() / 60) % 60);
		case NUMBER_TOTALPLAYTIME_SECOND:
			return (state) -> ((int) (state.main.getPlayerResource().getPlayerData().getPlaytime()) % 60);
		case NUMBER_TOTALPLAYCOUNT:
			return (state) -> ((int)state.main.getPlayerResource().getPlayerData().getPlaycount());
		case NUMBER_TOTALCLEARCOUNT:
			return (state) -> ((int)state.main.getPlayerResource().getPlayerData().getClear());
		case NUMBER_TOTALFAILCOUNT:
			return (state) -> ((int)state.main.getPlayerResource().getPlayerData().getPlaycount() - (int)state.main.getPlayerResource().getPlayerData().getClear());
		case NUMBER_TOTALPERFECT:
			return (state) -> ((int)state.main.getPlayerResource().getPlayerData().getEpg() + (int)state.main.getPlayerResource().getPlayerData().getLpg());
		case NUMBER_TOTALGREAT:
			return (state) -> ((int)state.main.getPlayerResource().getPlayerData().getEgr() + (int)state.main.getPlayerResource().getPlayerData().getLgr());
		case NUMBER_TOTALGOOD:
			return (state) -> ((int)state.main.getPlayerResource().getPlayerData().getEgd() + (int)state.main.getPlayerResource().getPlayerData().getLgd());
		case NUMBER_TOTALBAD:
			return (state) -> ((int)state.main.getPlayerResource().getPlayerData().getEbd() + (int)state.main.getPlayerResource().getPlayerData().getLbd());
		case NUMBER_TOTALPOOR:
			return (state) -> ((int)state.main.getPlayerResource().getPlayerData().getEpr() + (int)state.main.getPlayerResource().getPlayerData().getLpr());
		case NUMBER_TOTALPLAYNOTES:
			return (state) -> ((int)state.main.getPlayerResource().getPlayerData().getEpg() + (int)state.main.getPlayerResource().getPlayerData().getLpg() +
					(int)state.main.getPlayerResource().getPlayerData().getEgr() + (int)state.main.getPlayerResource().getPlayerData().getLgr() +
					(int)state.main.getPlayerResource().getPlayerData().getEgd() + (int)state.main.getPlayerResource().getPlayerData().getLgd() +
					(int)state.main.getPlayerResource().getPlayerData().getEbd() + (int)state.main.getPlayerResource().getPlayerData().getLbd());
		case NUMBER_PLAYCOUNT:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final IRScoreData score = ((MusicSelector)state).getBarRender().getSelected().getScore();
					return score != null ? score.getPlaycount() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_CLEARCOUNT:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final IRScoreData score = ((MusicSelector)state).getBarRender().getSelected().getScore();
					return score != null ? score.getClearcount() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_FAILCOUNT:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final IRScoreData score = ((MusicSelector)state).getBarRender().getSelected().getScore();
					return score != null ? score.getPlaycount() - score.getClearcount() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
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
					? state.getScoreDataProperty().getScoreData().getNotes()
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
		case NUMBER_DURATION:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector)state).getBarRender().getSelected();
					if (selected instanceof SongBar && ((SongBar) selected).existsSong()) {
						SongBar song = (SongBar) selected;
						PlayConfig pc = state.main.getPlayerConfig().getPlayConfig(song.getSongData().getMode()).getPlayconfig();
						return pc.getDuration();
					}
					return state.main.getPlayerConfig().getMode7().getPlayconfig().getDuration();
				} else if (state instanceof BMSPlayer) {
					return ((BMSPlayer) state).getLanerender().getCurrentDuration();
				} else if (state.main.getPlayerResource().getSongdata() != null) {
					SongData song = state.main.getPlayerResource().getSongdata();
					PlayConfig pc = state.main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode())
							.getPlayconfig();
					return pc.getDuration();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DURATION_GREEN:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector)state).getBarRender().getSelected();
					if (selected instanceof SongBar && ((SongBar) selected).existsSong()) {
						SongBar song = (SongBar) selected;
						PlayConfig pc = state.main.getPlayerConfig().getPlayConfig(song.getSongData().getMode()).getPlayconfig();
						return pc.getDuration() * 3 / 5;
					}
					return state.main.getPlayerConfig().getMode7().getPlayconfig().getDuration() * 3 / 5;
				} else if (state instanceof BMSPlayer) {
					return ((BMSPlayer) state).getLanerender().getCurrentDuration() * 3 / 5;
				} else if (state.main.getPlayerResource().getSongdata() != null) {
					SongData song = state.main.getPlayerResource().getSongdata();
					PlayConfig pc = state.main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode())
							.getPlayconfig();
					return pc.getDuration() * 3 / 5;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_TOTALNOTE_NORMAL:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return song.getInformation().getN();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_TOTALNOTE_LN:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return song.getInformation().getLn();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_TOTALNOTE_SCRATCH:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return song.getInformation().getS();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_TOTALNOTE_BSS:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return song.getInformation().getLs();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DENSITY_AVERAGE:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return (int) song.getInformation().getDensity();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DENSITY_AVERAGE_AFTERDOT:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return ((int) (song.getInformation().getDensity() * 100)) % 100;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DENSITY_END:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return (int) song.getInformation().getEnddensity();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DENSITY_END_AFTERDOT:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return ((int) (song.getInformation().getEnddensity() * 100)) % 100;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DENSITY_PEAK:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return (int) song.getInformation().getPeakdensity();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DENSITY_PEAK_AFTERDOT:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return ((int) (song.getInformation().getPeakdensity() * 100)) % 100;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_SONGGAUGE_TOTAL:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return (int) song.getInformation().getTotal();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_FOLDER_TOTALSONGS:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector)state).getBarRender().getSelected();
					if (selected instanceof DirectoryBar) {
						int[] lamps = ((DirectoryBar) selected).getLamps();
						int count = 0;
						for (int lamp : lamps) {
							count += lamp;
						}
						return count;
					}
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_PLAYTIME_MINUTE:
			return (state) -> ((int) (((int) (state.main.isTimerOn(TIMER_PLAY) ? state.main.getNowTime(TIMER_PLAY) : 0))
					/ 60000));
		case NUMBER_PLAYTIME_SECOND:
			return (state) -> ((((int) (state.main.isTimerOn(TIMER_PLAY) ? state.main.getNowTime(TIMER_PLAY) : 0))
					/ 1000) % 60);
		case NUMBER_TIMELEFT_MINUTE:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					return (int) (Math.max((((BMSPlayer) state).getPlaytime()
							- (int) (state.main.isTimerOn(TIMER_PLAY) ? state.main.getNowTime(TIMER_PLAY) : 0) + 1000),
							0) / 60000);
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_TIMELEFT_SECOND:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					return (Math.max((((BMSPlayer) state).getPlaytime()
							- (int) (state.main.isTimerOn(TIMER_PLAY) ? state.main.getNowTime(TIMER_PLAY) : 0) + 1000),
							0) / 1000) % 60;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_LANECOVER1:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					return (int) (((BMSPlayer) state).getLanerender().getLanecover() * 1000);
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
					return (int) state.main.getPlayerResource().getGauge()[gaugeType]
							.get(state.main.getPlayerResource().getGauge()[gaugeType].size - 1);
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_GROOVEGAUGE_AFTERDOT:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					final GrooveGauge gauge = ((BMSPlayer) state).getGauge();
					return (gauge.getType() == GrooveGauge.HARD || gauge.getType() == GrooveGauge.EXHARD
							|| gauge.getType() == GrooveGauge.HAZARD || gauge.getType() == GrooveGauge.CLASS
							|| gauge.getType() == GrooveGauge.EXCLASS || gauge.getType() == GrooveGauge.EXHARDCLASS)
							&& gauge.getValue() > 0 && gauge.getValue() < 0.1 ? 1
									: ((int) (gauge.getValue() * 10)) % 10;
				}
				if (state instanceof AbstractResult) {
					final int gaugeType = ((AbstractResult) state).getGaugeType();
					float value = state.main.getPlayerResource().getGauge()[gaugeType]
							.get(state.main.getPlayerResource().getGauge()[gaugeType].size - 1) * 10;
					if (value > 0 && value < 1)
						value = 1;
					return ((int) value) % 10;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_NOWBPM:
			return (state) -> {
				if (state instanceof BMSPlayer) {
					return (int) (((BMSPlayer) state).getLanerender().getNowBPM());
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_MAXCOMBO:
		case NUMBER_MAXCOMBO2:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final IRScoreData score = ((MusicSelector)state).getBarRender().getSelected().getScore();
					return score != null ? score.getCombo() : Integer.MIN_VALUE;
				}
				if (state instanceof BMSPlayer) {
					return ((BMSPlayer) state).getJudgeManager().getScoreData().getCombo();
				}
				if (state instanceof AbstractResult) {
					final IRScoreData score = ((AbstractResult) state).getNewScore();
					return score != null ? score.getCombo() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_CLEAR:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final IRScoreData score = ((AbstractResult) state).getNewScore();
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
					final IRScoreData score = ((AbstractResult) state).getNewScore();
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
					final IRScoreData score = ((AbstractResult) state).getOldScore();
					return score.getMinbp() != Integer.MAX_VALUE ? score.getMinbp() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DIFF_MISSCOUNT:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final IRScoreData score = ((AbstractResult) state).getOldScore();
					return score.getMinbp() != Integer.MAX_VALUE
							? ((AbstractResult) state).getNewScore().getMinbp() - score.getMinbp()
							: Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_TARGET_MAXCOMBO:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final IRScoreData score = ((AbstractResult) state).getOldScore();
					return score.getCombo() > 0 ? score.getCombo() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DIFF_MAXCOMBO:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final IRScoreData score = ((AbstractResult) state).getOldScore();
					return score.getCombo() > 0 ? ((AbstractResult) state).getNewScore().getCombo() - score.getCombo()
							: Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_MISSCOUNT:
		case NUMBER_MISSCOUNT2:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final IRScoreData score = ((MusicSelector)state).getBarRender().getSelected().getScore();
					return score != null ? score.getMinbp() : Integer.MIN_VALUE;
				}
				if (state instanceof AbstractResult) {
					final IRScoreData score = ((AbstractResult) state).getNewScore();
					return score != null ? score.getMinbp() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_IR_RANK:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final IRScoreData score = ((AbstractResult) state).getNewScore();
					return ((AbstractResult) state).getState() != AbstractResult.STATE_OFFLINE
							? ((AbstractResult) state).getIRRank()
							: Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_IR_PREVRANK:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final IRScoreData score = ((AbstractResult) state).getNewScore();
					return ((AbstractResult) state).getState() != AbstractResult.STATE_OFFLINE
							? ((AbstractResult) state).getOldIRRank()
							: Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_IR_TOTALPLAYER:
			return (state) -> {
				if (state instanceof AbstractResult) {
					final IRScoreData score = ((AbstractResult) state).getNewScore();
					return ((AbstractResult) state).getState() != AbstractResult.STATE_OFFLINE
							? ((AbstractResult) state).getIRTotalPlayer()
							: Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_AVERAGE_DURATION:
			return (state) -> {
				if (state instanceof AbstractResult) {
					return (int) ((AbstractResult) state).getAverageDuration();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_AVERAGE_DURATION_AFTERDOT:
			return (state) -> {
				if (state instanceof AbstractResult) {
					return (int) (((AbstractResult) state).getAverageDuration() * 100) % 100;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_AVERAGE_TIMING:
			return (state) -> {
				if (state instanceof AbstractResult) {
					return (int) ((AbstractResult) state).getTimingDistribution().getAverage();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_AVERAGE_TIMING_AFTERDOT:
			return (state) -> {
				if (state instanceof AbstractResult) {
					TimingDistribution timingDistribution = ((AbstractResult) state).getTimingDistribution();
					if (timingDistribution.getAverage() >= 0.0) {
						return (int) (timingDistribution.getAverage() * 100) % 100;
					} else {
						return (int) ( -1 * ((Math.abs(timingDistribution.getAverage()) * 100) % 100));
					}
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_STDDEV_TIMING:
			return (state) -> {
				if (state instanceof AbstractResult) {
					return (int) ((AbstractResult) state).getTimingDistribution().getStdDev();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_STDDEV_TIMING_AFTERDOT:
			return (state) -> {
				if (state instanceof AbstractResult) {
					return (int) (((AbstractResult) state).getTimingDistribution().getStdDev() * 100) % 100;
				}
				return Integer.MIN_VALUE;
			};
		}

		return null;
	}

	public static IntegerProperty getImageIndexProperty(int optionid) {
		IntegerProperty result = null;

		if (optionid == BUTTON_GAUGE_1P) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().getGauge());
		}
		if (optionid == BUTTON_RANDOM_1P) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().getRandom());
		}
		if (optionid == BUTTON_RANDOM_2P) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().getRandom2());
		}
		if (optionid == BUTTON_DPOPTION) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().getDoubleoption());
		}
		if (optionid == BUTTON_ASSIST_EXJUDGE) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().getJudgewindowrate() > 100 ? 1 : 0);
		}
		if (optionid == BUTTON_ASSIST_CONSTANT) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().isConstant() ? 1 : 0);
		}
		if (optionid == BUTTON_ASSIST_JUDGEAREA) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().isShowjudgearea() ? 1 : 0);
		}
		if (optionid == BUTTON_ASSIST_LEGACY) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().isLegacynote() ? 1 : 0);
		}
		if (optionid == BUTTON_ASSIST_MARKNOTE) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().isMarkprocessednote() ? 1 : 0);
		}
		if (optionid == BUTTON_ASSIST_BPMGUIDE) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().isBpmguide() ? 1 : 0);
		}
		if (optionid == BUTTON_ASSIST_NOMINE) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().isNomine() ? 1 : 0);
		}
		if (optionid == BUTTON_LNMODE) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().getLnmode());
		}
		if (optionid == BUTTON_TARGET) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().getTarget());
		}
		if (optionid == BUTTON_GAUGEAUTOSHIFT) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().getGaugeAutoShift());
		}
		if (optionid == BUTTON_BGA) {
			result = (state) -> (state.main.getPlayerResource().getConfig().getBga());
		}
		if (optionid == BUTTON_AUTOSAVEREPLAY_1) {
			result = (state) -> (state.main.getConfig().getAutoSaveReplay()[0]);
		}
		if (optionid == BUTTON_AUTOSAVEREPLAY_2) {
			result = (state) -> (state.main.getConfig().getAutoSaveReplay()[1]);
		}
		if (optionid == BUTTON_AUTOSAVEREPLAY_3) {
			result = (state) -> (state.main.getConfig().getAutoSaveReplay()[2]);
		}
		if (optionid == BUTTON_AUTOSAVEREPLAY_4) {
			result = (state) -> (state.main.getConfig().getAutoSaveReplay()[3]);
		}

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
			result = getImageIndexProperty0(optionid);
		}

		return result;
	}

	private static IntegerProperty getImageIndexProperty0(int optionid) {
		switch (optionid) {
		case BUTTON_HSFIX:
			return (state) -> {
				if (state.main.getPlayerResource().getSongdata() != null) {
					SongData song = state.main.getPlayerResource().getSongdata();
					PlayConfig pc = state.main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode())
							.getPlayconfig();
					return pc.getFixhispeed();
				} else if (state.main.getPlayerResource().getCourseData() != null) {
					PlayConfig pc = null;
					for (SongData song : state.main.getPlayerResource().getCourseData().getSong()) {
						if (song.getPath() == null) {
							pc = null;
							break;
						}
						PlayConfig pc2 = state.main.getPlayerConfig().getPlayConfig(song.getMode()).getPlayconfig();
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
			};
		case BUTTON_MODE:
			return (state) -> {
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
			};
		case BUTTON_SORT:
			return (state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSort()
					: Integer.MIN_VALUE);
		case NUMBER_CLEAR:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
					return selected.getScore() != null ? selected.getScore().getClear() : Integer.MIN_VALUE;
				} else if (state instanceof AbstractResult) {
					final IRScoreData score = ((AbstractResult) state).getNewScore();
					if (score != null) {
						return score.getClear();
					}
					return Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_TARGET_CLEAR:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
					return selected.getRivalScore() != null ? selected.getRivalScore().getClear() : Integer.MIN_VALUE;
				} else if (state instanceof AbstractResult) {
					return ((AbstractResult) state).getOldScore().getClear();
				}
				return Integer.MIN_VALUE;
			};
		}
		return null;
	}
}
