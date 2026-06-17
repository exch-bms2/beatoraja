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
		IntegerProperty result = ValueType.getProperty(optionid);
		
		vcache[optionid] = result;
		return result;
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
		
		hispeed_lr2(NUMBER_HISPEED_LR2, createHispeedProperty(true, false)),
		notesdisplaytiming(12, (state) -> (state.main.getPlayerResource().getPlayerConfig().getJudgetiming())), 
		lanecover1(NUMBER_LANECOVER1, (state) -> {
			if (state instanceof BMSPlayer) {
				return (int) (((BMSPlayer) state).getLanerender().getLanecover() * 1000);
			}
			return Integer.MIN_VALUE;
		}),
		lift1(NUMBER_LIFT1, (state) -> {
			if (state instanceof BMSPlayer) {
				return (int) (((BMSPlayer) state).getLanerender().getLiftRegion() * 1000);
			}
			return Integer.MIN_VALUE;
		}),
		hidden1(NUMBER_HIDDEN1, (state) -> {
			if (state instanceof BMSPlayer) {
				return (int) (((BMSPlayer) state).getLanerender().getHiddenCover() * 1000);
			}
			return Integer.MIN_VALUE;
		}),
		lanecover2(NUMBER_LANECOVER2, (state) -> {
			if (state instanceof BMSPlayer) {
				LaneRenderer laneRenderer = ((BMSPlayer) state).getLanerender();
				return (int)((1.0 - laneRenderer.getLiftRegion()) * laneRenderer.getLanecover() * 1000);
			}
			return Integer.MIN_VALUE;
		}),

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

		score(NUMBER_SCORE, createScoreProperty()),
		maxscore(NUMBER_MAXSCORE, (state) -> (state.getScoreDataProperty().getScoreData() != null
				? state.getScoreDataProperty().getScoreData().getNotes() * 2
				: 0)),
		totalnotes(NUMBER_TOTALNOTES, createTotalNotesProperty()),
		maxcombo(NUMBER_MAXCOMBO, createMaxComboProperty()),
		misscount(NUMBER_MISSCOUNT, createMissCountProperty()),
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
		playlevel(NUMBER_PLAYLEVEL, createPlayLevelProperty()),
		folder_level_beginner(NUMBER_FOLDER_BEGINNER, createPlayLevelProperty()),
		folder_level_normal(NUMBER_FOLDER_NORMAL, createPlayLevelProperty()),
		folder_level_hyper(NUMBER_FOLDER_HYPER, createPlayLevelProperty()),
		folder_level_another(NUMBER_FOLDER_ANOTHER, createPlayLevelProperty()),
		folder_level_insane(NUMBER_FOLDER_INSANE, createPlayLevelProperty()),

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
		point(NUMBER_POINT, (state) -> (state.getScoreDataProperty().getNowScore())),
		score2(NUMBER_SCORE2, createScoreProperty()),
		score_rate(NUMBER_SCORE_RATE, (state) -> (state.getScoreDataProperty().getScoreData() != null
				? state.getScoreDataProperty().getNowRateInt()
				: Integer.MIN_VALUE)),
		score_rate_afterdot(NUMBER_SCORE_RATE_AFTERDOT, (state) -> (state.getScoreDataProperty().getScoreData() != null
				? state.getScoreDataProperty().getNowRateAfterDot()
				: Integer.MIN_VALUE)),
		maxcombo2(NUMBER_MAXCOMBO2, createMaxComboProperty()),
		totalnotes2(NUMBER_TOTALNOTES2, createTotalNotesProperty()),
		groovegauge(NUMBER_GROOVEGAUGE, createGrooveGaugeProperty()),
		groovegauge_afterdot(NUMBER_GROOVEGAUGE_AFTERDOT, createGrooveGaugeAfterDotProperty()),
		diff_exscore(NUMBER_DIFF_EXSCORE, createDiffRivalScoreProperty()),
		total_rate(NUMBER_TOTAL_RATE, createTotalRateProperty()),
		total_rate_afterdot(NUMBER_TOTAL_RATE_AFTERDOT, createTotalRateAfterDotProperty()),
		target_score(NUMBER_TARGET_SCORE, (state) -> (state.getScoreDataProperty().getRivalScore())),
		target_score_rate(NUMBER_TARGET_SCORE_RATE, (state) -> (state.getScoreDataProperty().getRivalRateInt())),
		target_score_rate_afterdot(NUMBER_TARGET_SCORE_RATE_AFTERDOT, (state) -> (state.getScoreDataProperty().getRivalRateAfterDot())),
		diff_exscore2(NUMBER_DIFF_EXSCORE2, createDiffRivalScoreProperty()),
		target_total_rate(NUMBER_TARGET_TOTAL_RATE, (state) -> (state.getScoreDataProperty().getRivalRateInt())),
		target_total_rate_afterdot(NUMBER_TARGET_TOTAL_RATE_AFTERDOT, (state) -> (state.getScoreDataProperty().getRivalRateAfterDot())),
		highscore(NUMBER_HIGHSCORE, createHighScoreProperty()),
		target_score2(NUMBER_TARGET_SCORE2, (state) -> (state.getScoreDataProperty().getRivalScore())),
		diff_highscore(NUMBER_DIFF_HIGHSCORE, createDiffHighScoreProperty()),
		diff_targetscore(NUMBER_DIFF_TARGETSCORE, createDiffRivalScoreProperty()),
		diff_nextrank(NUMBER_DIFF_NEXTRANK, (state) -> (state.getScoreDataProperty().getNextRank())),
		score_rate2(NUMBER_SCORE_RATE2, createTotalRateProperty()),
		score_rate_afterdot2(NUMBER_SCORE_RATE_AFTERDOT2, createTotalRateAfterDotProperty()),
		target_score_rate2(NUMBER_TARGET_SCORE_RATE2, (state) -> (state.getScoreDataProperty().getRivalRateInt())),
		target_score_rate_afterdot2(NUMBER_TARGET_SCORE_RATE_AFTERDOT2, (state) -> (state.getScoreDataProperty().getRivalRateAfterDot())),
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
		highscore2(NUMBER_HIGHSCORE2, createHighScoreProperty()),
		score3(NUMBER_SCORE3, createScoreProperty()),
		diff_highscore2(NUMBER_DIFF_HIGHSCORE2, createDiffHighScoreProperty()),
		target_maxcombo(NUMBER_TARGET_MAXCOMBO, createTargetMaxComboProperty()),
		maxcombo3(NUMBER_MAXCOMBO3, createMaxComboProperty()),
		diff_maxcombo(NUMBER_DIFF_MAXCOMBO, createDiffMaxComboProperty()),
		target_misscount(NUMBER_TARGET_MISSCOUNT, createTargetMissCountProperty()),
		misscount2(NUMBER_MISSCOUNT2, createMissCountProperty()),
		diff_misscount(NUMBER_DIFF_MISSCOUNT, createDiffMissCountProperty()),
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
		ir_totalplayer(NUMBER_IR_TOTALPLAYER, createIRTotalPlayerProperty()),
		ir_prevrank(182, (state) -> {
			if (state instanceof AbstractResult) {
				return ((AbstractResult) state).getState() != AbstractResult.STATE_OFFLINE
						? ((AbstractResult) state).getOldIRRank()
						: Integer.MIN_VALUE;
			}
			return Integer.MIN_VALUE;
		}),
		best_rate(NUMBER_BEST_RATE, (state) -> (state.getScoreDataProperty().getBestRateInt())),
		best_rate_afterdot(NUMBER_BEST_RATE_AFTERDOT, (state) -> (state.getScoreDataProperty().getBestRateAfterDot())),
		ir_totalplayer2(NUMBER_IR_TOTALPLAYER2, createIRTotalPlayerProperty()),
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

		lastplay_timestamp(243, createLastPlayTimestampProperty()),
		lastplay_year(244, createLastPlayDateProperty(Calendar.YEAR)),
		lastplay_month(245, createLastPlayDateProperty(Calendar.MONTH)),
		lastplay_day(246, createLastPlayDateProperty(Calendar.DATE)),
		lastplay_hour(247, createLastPlayDateProperty(Calendar.HOUR_OF_DAY)),
		lastplay_minute(248, createLastPlayDateProperty(Calendar.MINUTE)),
		lastplay_second(249, createLastPlayDateProperty(Calendar.SECOND)),
		rival_score(NUMBER_RIVAL_SCORE, (state) -> (state.getScoreDataProperty().getRivalScore())),
		folder_totalsongs(NUMBER_FOLDER_TOTALSONGS, new FolderTotalClearCountProperty(new int[]{0,1,2,3,4,5,6,7,8,9,10})),
		hispeed(NUMBER_HISPEED, createHispeedProperty(false, false)),
		hispeed_afterdot(NUMBER_HISPEED_AFTERDOT, createHispeedProperty(false, true)),
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
		clear(NUMBER_CLEAR, (state) -> {
			if (state instanceof AbstractResult) {
				final ScoreData score = ((AbstractResult) state).getNewScore();
				return score != null ? score.getClear() : Integer.MIN_VALUE;
			}
			return Integer.MIN_VALUE;
		}),
		target_clear(NUMBER_TARGET_CLEAR, (state) -> {
			if (state instanceof AbstractResult) {
				return ((AbstractResult) state).getOldScore().getClear();
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
		miss(NUMBER_MISS, (state) -> (state.getJudgeCount(5, true) + state.getJudgeCount(5, false))),
		early_miss(NUMBER_EARLY_MISS, (state) -> (state.getJudgeCount(5, true))),
		late_miss(NUMBER_LATE_MISS, (state) -> (state.getJudgeCount(5, false))),
		totalearly(NUMBER_TOTALEARLY, (state) -> {
			int ecount = 0;
			for (int i = 1; i < 6; i++) {
				ecount += state.getJudgeCount(i, true);
			}
			return ecount;
		}),
		totallate(NUMBER_TOTALLATE, (state) -> {
			int ecount = 0;
			for (int i = 1; i < 6; i++) {
				ecount += state.getJudgeCount(i, false);
			}
			return ecount;
		}),
		combobreak(NUMBER_COMBOBREAK, (state) -> (state.getJudgeCount(3, true) + state.getJudgeCount(3, false)
				+ state.getJudgeCount(4, true) + state.getJudgeCount(4, false))),
		poor_plus_miss(NUMBER_POOR_PLUS_MISS, (state) -> (state.getJudgeCount(4, true) + state.getJudgeCount(4, false)
				+ state.getJudgeCount(5, true) + state.getJudgeCount(5, false))),
		bad_plus_poor_plus_miss(NUMBER_BAD_PLUS_POOR_PLUS_MISS, (state) -> (state.getJudgeCount(3, true) + state.getJudgeCount(3, false)
				+ state.getJudgeCount(4, true) + state.getJudgeCount(4, false) + state.getJudgeCount(5, true)
				+ state.getJudgeCount(5, false))),

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

		private static IntegerProperty getProperty(int id) {
			if (id >= NUMBER_DURATION_LANECOVER_ON && id <= NUMBER_MAXBPM_DURATION_GREEN_LANECOVER_OFF) {
				return createDurationLanecoverProperty(id);
			}
			if (id >= NUMBER_PERFECT2 && id <= NUMBER_POOR2) {
				final int index = id - NUMBER_PERFECT2;
				return (state) -> (state.getScoreDataProperty().getScoreData() != null ?
						state.getScoreDataProperty().getScoreData().getJudgeCount(index) : Integer.MIN_VALUE);
			}
			if (id >= NUMBER_PERFECT_RATE && id <= NUMBER_POOR_RATE) {
				final int index = id - NUMBER_PERFECT_RATE;
				return (state) -> {
					final ScoreData score = state.getScoreDataProperty().getScoreData();
					return score != null && score.getNotes() > 0 ? score.getJudgeCount(index) * 100 / score.getNotes() : Integer.MIN_VALUE;
				};
			}
			if (id >= NUMBER_PERFECT && id <= NUMBER_POOR) {
				final int index = id - NUMBER_PERFECT;
				return (state) -> (state.getJudgeCount(index, true) + state.getJudgeCount(index, false));
			}
			if (id >= NUMBER_EARLY_PERFECT && id <= NUMBER_LATE_POOR) {
				final int index = (id - NUMBER_EARLY_PERFECT) / 2;
				final boolean early = (id - NUMBER_EARLY_PERFECT) % 2 == 0;
				return (state) -> (state.getJudgeCount(index, early));
			}
			if (id >= NUMBER_RIVAL_PERFECT && id <= NUMBER_RIVAL_POOR) {
				final int index = id - NUMBER_RIVAL_PERFECT;
				return (state) -> (state.getScoreDataProperty().getRivalScoreData() != null ?
						state.getScoreDataProperty().getRivalScoreData().getJudgeCount(index) : Integer.MIN_VALUE);
			}
			if (id >= NUMBER_RIVAL_PERFECT_RATE && id <= NUMBER_RIVAL_POOR_RATE) {
				final int index = id - NUMBER_RIVAL_PERFECT_RATE;
				return (state) -> {
					final ScoreData rival = state.getScoreDataProperty().getRivalScoreData();
					return rival != null && rival.getNotes() > 0 ? rival.getJudgeCount(index) * 100 / rival.getNotes() : Integer.MIN_VALUE;
				};
			}
			for(ValueType t : ValueType.values()) {
				if(t.id == id) {
					return t.property;
				}
			}
			return null;
		}

		private static IntegerProperty createDurationLanecoverProperty(int id) {
			final boolean green = (id - NUMBER_DURATION_LANECOVER_ON) % 2 == 1;
			final boolean cover = (id - NUMBER_DURATION_LANECOVER_ON) % 4 < 2;
			final int mode = (id - NUMBER_DURATION_LANECOVER_ON) / 4;
			return (state) -> {
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

		private static IntegerProperty createTotalNotesProperty() {
			return (state) -> {
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

		private static IntegerProperty createPlayLevelProperty() {
			return (state) -> (state.main.getPlayerResource().getSongdata() != null
					? state.main.getPlayerResource().getSongdata().getLevel()
					: Integer.MIN_VALUE);
		}

		private static IntegerProperty createHispeedProperty(boolean lr2, boolean afterdot) {
			return (state) -> {
				float hispeed;
				if (state instanceof BMSPlayer) {
					hispeed = ((BMSPlayer) state).getLanerender().getHispeed();
				} else if (state.main.getPlayerResource().getSongdata() != null) {
					SongData song = state.main.getPlayerResource().getSongdata();
					PlayConfig pc = state.main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode())
							.getPlayconfig();
					hispeed = pc.getHispeed();
				} else {
					return Integer.MIN_VALUE;
				}
				if (lr2) {
					return (int) (hispeed * 100);
				}
				return afterdot ? (int) (hispeed * 100) % 100 : (int) hispeed;
			};
		}

		private static IntegerProperty createScoreProperty() {
			return (state) -> {
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getNewScore();
					return score != null ? score.getExscore() : Integer.MIN_VALUE;
				}
				return state.getScoreDataProperty().getScoreData() != null
						? state.getScoreDataProperty().getNowEXScore()
						: Integer.MIN_VALUE;
			};
		}

		private static IntegerProperty createHighScoreProperty() {
			return (state) -> {
				if (state instanceof AbstractResult) {
					return ((AbstractResult) state).getOldScore().getExscore();
				}
				return state.getScoreDataProperty().getBestScore();
			};
		}

		private static IntegerProperty createMaxComboProperty() {
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
		}

		private static IntegerProperty createMissCountProperty() {
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
		}

		private static IntegerProperty createGrooveGaugeProperty() {
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
		}

		private static IntegerProperty createGrooveGaugeAfterDotProperty() {
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
		}

		private static IntegerProperty createTotalRateProperty() {
			return (state) -> (state.getScoreDataProperty().getScoreData() != null
					? state.getScoreDataProperty().getRateInt()
					: Integer.MIN_VALUE);
		}

		private static IntegerProperty createTotalRateAfterDotProperty() {
			return (state) -> (state.getScoreDataProperty().getScoreData() != null
					? state.getScoreDataProperty().getRateAfterDot()
					: Integer.MIN_VALUE);
		}

		private static IntegerProperty createDiffHighScoreProperty() {
			return (state) -> (state.getScoreDataProperty().getNowEXScore()
					- state.getScoreDataProperty().getNowBestScore());
		}

		private static IntegerProperty createDiffRivalScoreProperty() {
			return (state) -> (state.getScoreDataProperty().getNowEXScore()
					- state.getScoreDataProperty().getNowRivalScore());
		}

		private static IntegerProperty createTargetMissCountProperty() {
			return (state) -> {
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getOldScore();
					return score.getMinbp() != Integer.MAX_VALUE ? score.getMinbp() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		}

		private static IntegerProperty createDiffMissCountProperty() {
			return (state) -> {
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getOldScore();
					return score.getMinbp() != Integer.MAX_VALUE
							? ((AbstractResult) state).getNewScore().getMinbp() - score.getMinbp()
							: Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		}

		private static IntegerProperty createTargetMaxComboProperty() {
			return (state) -> {
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getOldScore();
					return score.getCombo() > 0 ? score.getCombo() : Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		}

		private static IntegerProperty createDiffMaxComboProperty() {
			return (state) -> {
				if (state instanceof AbstractResult) {
					final ScoreData score = ((AbstractResult) state).getOldScore();
					return score.getCombo() > 0 ? ((AbstractResult) state).getNewScore().getCombo() - score.getCombo()
							: Integer.MIN_VALUE;
				}
				return Integer.MIN_VALUE;
			};
		}

		private static IntegerProperty createIRTotalPlayerProperty() {
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

		private static IntegerProperty createLastPlayTimestampProperty() {
			return (state) -> {
				ScoreData score = getCurrentScoreData(state);
				long date = score != null ? score.getDate() : 0;
				return date > 0 && date <= Integer.MAX_VALUE ? (int) date : Integer.MIN_VALUE;
			};
		}

		private static IntegerProperty createLastPlayDateProperty(int field) {
			return (state) -> {
				ScoreData score = getCurrentScoreData(state);
				long date = score != null ? score.getDate() : 0;
				if (date <= 0) {
					return Integer.MIN_VALUE;
				}
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(date * 1000L);
				int value = calendar.get(field);
				return field == Calendar.MONTH ? value + 1 : value;
			};
		}

		private static ScoreData getCurrentScoreData(MainState state) {
			if (state instanceof MusicSelector selector) {
				Bar selected = selector.getBarManager().getSelected();
				return selected != null ? selected.getScore() : null;
			}
			return state.getScoreDataProperty().getScoreData();
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
				return state.main.getPlayerConfig().getModeFilter().getSkinNumber();
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
