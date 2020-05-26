package bms.player.beatoraja.skin.property;

import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerInformation;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.config.SkinConfiguration;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.ir.IRScoreData;
import bms.player.beatoraja.ir.RankingData;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.result.AbstractResult;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.DirectoryBar;
import bms.player.beatoraja.select.bar.GradeBar;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.song.SongData;

public class StringPropertyFactory {

	public static StringProperty getStringProperty(final int optionid) {
		StringProperty result = null;
		if (optionid >= STRING_COURSE1_TITLE && optionid <= STRING_COURSE10_TITLE) {
			result = new StringProperty() {
				private final int index = optionid - STRING_COURSE1_TITLE;

				@Override
				public String get(MainState state) {
					if (state instanceof MusicSelector) {
						final Bar bar = ((MusicSelector) state).getSelectedBar();
						if (bar instanceof GradeBar) {
							if (((GradeBar) bar).getSongDatas().length > index) {
								SongData song = ((GradeBar) bar).getSongDatas()[index];
								final String songname = song != null && song.getTitle() != null ? song.getTitle()
										: "----";
								return song != null && song.getPath() != null ? songname : "(no song) " + songname;
							}
						}
					} else {
						CourseData course = state.main.getPlayerResource().getCourseData();
						if (course != null && course.getSong().length > index && course.getSong()[index] != null) {
							return course.getSong()[index].getTitle();
						}
					}
					return "";
				}
			};
		}
		if (optionid >= STRING_RANKING1_NAME && optionid <= STRING_RANKING10_NAME) {
			final int index = optionid - STRING_RANKING1_NAME;
			result = (state) -> {
				RankingData irc = null;
				if (state instanceof MusicSelector) {
					irc = ((MusicSelector) state).getCurrentRankingData();
				}
				if (state instanceof AbstractResult) {
					irc = ((AbstractResult) state).getRankingData();
				}
				IRScoreData[] scores = irc != null ? irc.getScores() : null;
				if(scores != null && scores.length > index) {
					return scores[index].player.length() > 0 ? scores[index].player : "YOU";							
				}
				return "";
			};
		}
		if (SkinPropertyMapper.isSkinCustomizeCategory(optionid)) {
			final int index = SkinPropertyMapper.getSkinCustomizeCategoryIndex(optionid);
			result = (state) -> {
				if (state instanceof SkinConfiguration) {
					return ((SkinConfiguration)state).getCategoryName(index);
				}
				return "";
			};
		}
		if (SkinPropertyMapper.isSkinCustomizeItem(optionid)) {
			final int index = SkinPropertyMapper.getSkinCustomizeItemIndex(optionid);
			result = (state) -> {
				if (state instanceof SkinConfiguration) {
					return ((SkinConfiguration)state).getDisplayValue(index);
				}
				return "";
			};
		}

		if (result == null) {
			result = getStringProperty0(optionid);
		}

		return result;
	}

	private static StringProperty getStringProperty0(int optionid) {
		switch (optionid) {
		case STRING_RIVAL:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final PlayerInformation rival = ((MusicSelector) state).getRival();
					return rival != null ? rival.getName() : "";
				} else {
					return TargetProperty.getAllTargetProperties()[state.main.getPlayerResource().getPlayerConfig()
							.getTarget()].getName();
				}
			};
		case STRING_PLAYER:
			return (state) -> (state.main.getPlayerConfig().getName());
		case STRING_TITLE:
			return (state) -> {
				if (state instanceof MusicSelector) {
					if (((MusicSelector) state).getSelectedBar() instanceof DirectoryBar) {
						return ((MusicSelector) state).getSelectedBar().getTitle();
					}
				} else if ((state instanceof MusicDecide || state instanceof CourseResult)) {
					if(state.main.getPlayerResource().getCoursetitle() != null) {
						return state.main.getPlayerResource().getCoursetitle();						
					}
				}
				final SongData song = state.main.getPlayerResource().getSongdata();
				return song != null ? song.getTitle() : "";
			};
		case STRING_SUBTITLE:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				return song != null ? song.getSubtitle() : "";
			};
		case STRING_FULLTITLE:
			return (state) -> {
				if (state instanceof MusicSelector) {
					if (((MusicSelector) state).getSelectedBar() instanceof DirectoryBar) {
						return ((MusicSelector) state).getSelectedBar().getTitle();
					}
				} else if ((state instanceof MusicDecide || state instanceof CourseResult)) {
					if(state.main.getPlayerResource().getCoursetitle() != null) {
						return state.main.getPlayerResource().getCoursetitle();
					}
				}
				final SongData song = state.main.getPlayerResource().getSongdata();
				return song != null ? song.getFullTitle() : "";
			};
		case STRING_ARTIST:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				return song != null ? song.getArtist() : "";
			};
		case STRING_SUBARTIST:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				return song != null ? song.getSubartist() : "";
			};
		case STRING_FULLARTIST:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				return song != null ? song.getFullArtist() : "";
			};
		case STRING_GENRE:
			return (state) -> {
				final SongData song = state.main.getPlayerResource().getSongdata();
				return song != null ? song.getGenre() : "";
			};
		case STRING_TABLE_NAME:
			return (state) -> (state.main.getPlayerResource().getTablename());
		case STRING_TABLE_LEVEL:
			return (state) -> (state.main.getPlayerResource().getTablelevel());
		case STRING_TABLE_FULL:
			return (state) -> (state.main.getPlayerResource().getTableFullname());
		case STRING_DIRECTORY:
			return (state) -> {
				if (state instanceof MusicSelector) {
					return ((MusicSelector) state).getBarRender().getDirectoryString();
				}
				return "";
			};
		case STRING_SKIN_NAME:
			return (state) -> {
				if (state instanceof SkinConfiguration) {
					return ((SkinConfiguration)state).getSelectedSkinHeader() != null ? ((SkinConfiguration)state).getSelectedSkinHeader().getName() : "";
				}
				return "";
			};
		case STRING_SKIN_AUTHOR:
			return (state) -> {
				if (state instanceof SkinConfiguration) {
					return ((SkinConfiguration)state).getSelectedSkinHeader() != null ? "" : "";
				}
				return "";
			};

		}

		return null;
	}
}
