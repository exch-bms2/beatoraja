package bms.player.beatoraja.skin.property;

import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.player.beatoraja.BMSResource;
import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.PlayConfig;
import bms.player.beatoraja.config.SkinConfiguration;
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

	/**
	 * property IDに対応するFloatPropertyを返す
	 * 
	 * @param id property ID
	 * @return 対応するFloatProperty
	 */
	public static FloatProperty getFloatProperty(int optionid) {
		for(FloatType t : FloatType.values()) {
			if(t.id == optionid) {
				return t.property;
			}
		}
		return null;
	}

	/**
	 * property nameに対応するFloatPropertyを返す
	 * 
	 * @param name property name
	 * @return 対応するFloatProperty
	 */
	public static FloatProperty getFloatProperty(String name) {
		for(FloatType t : FloatType.values()) {
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
	public static FloatWriter getFloatWriter(int id) {
		for(FloatType t : FloatType.values()) {
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
	public static FloatWriter getFloatWriter(String name) {
		for(FloatType t : FloatType.values()) {
			if(t.name().equals(name)) {
				return t.writer;
			}
		}
		return null;
	}

	private static final FloatProperty PROPERTY_MUSIC_PROGRESS = (state) -> {
		if (state instanceof BMSPlayer) {
			if (state.main.isTimerOn(TIMER_PLAY)) {
				return Math.min((float) state.main.getNowTime(TIMER_PLAY) / ((BMSPlayer) state).getPlaytime(),
						1);
			}
		}
		return 0;
	};
	
	public enum FloatType {
		
		musicselect_position(1, 
				(state) -> (state instanceof MusicSelector ? ((MusicSelector) state).getBarRender().getSelectedPosition() : 0), 
				(state, value) -> {
					if(state instanceof MusicSelector) {
						final MusicSelector select = (MusicSelector) state;
						select.selectedBarMoved();
						select.getBarRender().setSelectedPosition(value);
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
		music_progress(6, PROPERTY_MUSIC_PROGRESS),
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
				(state) -> (state.main.getConfig().getAudioConfig().getSystemvolume()),
				(state, value) -> {
					state.main.getConfig().getAudioConfig().setSystemvolume(value);					
				}),
		keyvolume(18,
				(state) -> (state.main.getConfig().getAudioConfig().getKeyvolume()),
				(state, value) -> {
					state.main.getConfig().getAudioConfig().setKeyvolume(value);					
				}),
		bgmvolume(19,
				(state) -> (state.main.getConfig().getAudioConfig().getBgvolume()),
				(state, value) -> {
					state.main.getConfig().getAudioConfig().setBgvolume(value);					
				}),
		music_progress_bar(101, PROPERTY_MUSIC_PROGRESS),
		load_progress(102, (state) -> {
			final BMSResource resource = state.main.getPlayerResource().getBMSResource();
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
		rate_pgreat(140, (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
				if (selected instanceof SongBar) {
					ScoreData score = selected.getScore();
					return score != null
							? ((float) (score.getEpg() + score.getLpg()))
									/ ((SongBar) selected).getSongData().getNotes()
							: 0;
				}
				if (selected instanceof GradeBar) {
					ScoreData score = selected.getScore();
					if (score == null) return 0;
					int notes = 0;
					for (SongData songData : ((GradeBar) selected).getSongDatas()) {
						notes += songData.getNotes();
					}
					return ((float) (score.getEpg() + score.getLpg())) / notes;
				}
			}
			return 0;
		}),
		rate_great(141, (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
				if (selected instanceof SongBar) {
					ScoreData score = selected.getScore();
					return score != null
							? ((float) (score.getEgr() + score.getLgr()))
									/ ((SongBar) selected).getSongData().getNotes()
							: 0;
				}
				if (selected instanceof GradeBar) {
					ScoreData score = selected.getScore();
					if (score == null) return 0;
					int notes = 0;
					for (SongData songData : ((GradeBar) selected).getSongDatas()) {
						notes += songData.getNotes();
					}
					return ((float) (score.getEgr() + score.getLgr())) / notes;
				}
			}
			return 0;
		}),
		rate_good(142, (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
				if (selected instanceof SongBar) {
					ScoreData score = selected.getScore();
					return score != null
							? ((float) (score.getEgd() + score.getLgd()))
									/ ((SongBar) selected).getSongData().getNotes()
							: 0;
				}
				if (selected instanceof GradeBar) {
					ScoreData score = selected.getScore();
					if (score == null) return 0;
					int notes = 0;
					for (SongData songData : ((GradeBar) selected).getSongDatas()) {
						notes += songData.getNotes();
					}
					return ((float) (score.getEgd() + score.getLgd())) / notes;
				}
			}
			return 0;
		}),
		rate_bad(143, (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
				if (selected instanceof SongBar) {
					ScoreData score = selected.getScore();
					return score != null
							? ((float) (score.getEbd() + score.getLbd()))
									/ ((SongBar) selected).getSongData().getNotes()
							: 0;
				}
				if (selected instanceof GradeBar) {
					ScoreData score = selected.getScore();
					if (score == null) return 0;
					int notes = 0;
					for (SongData songData : ((GradeBar) selected).getSongDatas()) {
						notes += songData.getNotes();
					}
					return ((float) (score.getEbd() + score.getLbd())) / notes;
				}
			}
			return 0;
		}),
		rate_poor(144, (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
				if (selected instanceof SongBar) {
					ScoreData score = selected.getScore();
					return score != null
							? ((float) (score.getEpr() + score.getLpr()))
									/ ((SongBar) selected).getSongData().getNotes()
							: 0;
				}
				if (selected instanceof GradeBar) {
					ScoreData score = selected.getScore();
					if (score == null) return 0;
					int notes = 0;
					for (SongData songData : ((GradeBar) selected).getSongDatas()) {
						notes += songData.getNotes();
					}
					return ((float) (score.getEpr() + score.getLpr())) / notes;
				}
			}
			return 0;
		}),
		rate_maxcombo(145, (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
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
				final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
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

		private FloatType(int id, FloatProperty property) {
			this(id, property, null);
		}

		private FloatType(int id, FloatProperty property, FloatWriter writer) {
			this.id = id;
			this.property = property;
			this.writer = writer;
		}
	}


	private static FloatProperty getLevelRate(final int difficulty) {
		return (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
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
						return (float) sd.getLevel() / maxLevel;
					}
				}
			}
			return 0;
		};
	}

}
