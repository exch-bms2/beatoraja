package bms.player.beatoraja.skin;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Calendar;

import com.badlogic.gdx.Gdx;

import bms.player.beatoraja.BMSResource;
import bms.player.beatoraja.PlayConfig;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.JudgeManager;
import bms.player.beatoraja.play.LaneRenderer;
import bms.player.beatoraja.skin.SkinObject.IntegerProperty;
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
			result = (state) -> (state.main.getPlayerResource().getSongdata() != null
					? state.main.getPlayerResource().getSongdata().getMainbpm()
					: Integer.MIN_VALUE);
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
			result = (state) -> (state.main.getPlayerResource().getSongdata() != null
					? state.main.getPlayerResource().getSongdata().getNotes()
					: Integer.MIN_VALUE);
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
		case NUMBER_SCORE:
		case NUMBER_SCORE2:
		case NUMBER_SCORE3:
			return (state) -> (state.getScoreDataProperty().getScoreData() != null
					? state.getScoreDataProperty().getNowEXScore()
					: Integer.MIN_VALUE);
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
		case NUMBER_HIGHSCORE:
			return (state) -> (state.getScoreDataProperty().getBestScore());
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
				if (state.main.getPlayerResource().getSongdata() != null) {
					SongData song = state.main.getPlayerResource().getSongdata();
					PlayConfig pc = state.main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode())
							.getPlayconfig();
					return (int) (pc.getHispeed() * 100);
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_HISPEED:
			return (state) -> {
				if (state.main.getPlayerResource().getSongdata() != null) {
					SongData song = state.main.getPlayerResource().getSongdata();
					PlayConfig pc = state.main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode())
							.getPlayconfig();
					return (int) pc.getHispeed();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_HISPEED_AFTERDOT:
			return (state) -> {
				if (state.main.getPlayerResource().getSongdata() != null) {
					SongData song = state.main.getPlayerResource().getSongdata();
					PlayConfig pc = state.main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode())
							.getPlayconfig();
					return (int) (pc.getHispeed() * 100) % 100;
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DURATION:
			return (state) -> {
				if (state.main.getPlayerResource().getSongdata() != null) {
					SongData song = state.main.getPlayerResource().getSongdata();
					PlayConfig pc = state.main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode())
							.getPlayconfig();
					return pc.getDuration();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DURATION_GREEN:
			return (state) -> {
				if (state.main.getPlayerResource().getSongdata() != null) {
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
		case NUMBER_DENSITY_ENDPEAK:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return (int) song.getInformation().getDensity();
				}
				return Integer.MIN_VALUE;
			};
		case NUMBER_DENSITY_ENDPEAK_AFTERDOT:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return ((int) (song.getInformation().getDensity() * 100)) % 100;
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
		}

		return null;
	}
}
