package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.*;
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
import bms.player.beatoraja.select.bar.RandomCourseBar;
import bms.player.beatoraja.song.SongData;

/**
 * StringPropertyのFactoryクラス
 * 
 * @author exch
 */
public class StringPropertyFactory {

	/**
	 * property IDに対応するStringPropertyを返す
	 * 
	 * @param id property ID
	 * @return 対応するStringProperty
	 */
	public static StringProperty getStringProperty(final int id) {
		for(StringType t : StringType.values()) {
			if(t.id == id) {
				return t.property;
			}
		}
		return null;
	}
	
	/**
	 * property nameに対応するStringPropertyを返す
	 * 
	 * @param name property name
	 * @return 対応するStringProperty
	 */
	public static StringProperty getStringProperty(final String name) {
		for(StringType t : StringType.values()) {
			if(t.name().equals(name)) {
				return t.property;
			}
		}
		return null;
	}
	
	public enum StringType {
		
		rival(1, (state) -> {
			if (state instanceof MusicSelector) {
				final PlayerInformation rival = ((MusicSelector) state).getRival();
				return rival != null ? rival.getName() : "";
			} else {
				final ScoreData rival = state.main.getPlayerResource().getRivalScoreData();
				return rival != null ? rival.getPlayer() : "";
			}
		}),
		player(2, (state) -> (state.main.getPlayerConfig().getName())),
		target(3, (state) -> {
			if (state instanceof MusicSelector) {
				return TargetProperty.getTargetName(state.main.getPlayerConfig().getTargetid());					
			} else {
				final ScoreData rival = state.main.getPlayerResource().getRivalScoreData();
				return rival != null ? rival.getPlayer() : "";
			}
		}),
		title(10, (state) -> {
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
		}),
		subtitle(11, (state) -> {
			final SongData song = state.main.getPlayerResource().getSongdata();
			return song != null ? song.getSubtitle() : "";
		}),
		fulltitle(12, (state) -> {
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
		}),
		genre(13, (state) -> {
			final SongData song = state.main.getPlayerResource().getSongdata();
			return song != null ? song.getGenre() : "";
		}),
		artist(14, (state) -> {
			final SongData song = state.main.getPlayerResource().getSongdata();
			return song != null ? song.getArtist() : "";
		}),
		subartist(15, (state) -> {
			final SongData song = state.main.getPlayerResource().getSongdata();
			return song != null ? song.getSubartist() : "";
		}),
		fullartist(16, (state) -> {
			final SongData song = state.main.getPlayerResource().getSongdata();
			return song != null ? song.getFullArtist() : "";
		}),
		skinname(50, (state) -> {
			if (state instanceof SkinConfiguration) {
				return ((SkinConfiguration)state).getSelectedSkinHeader() != null ? ((SkinConfiguration)state).getSelectedSkinHeader().getName() : "";
			}
			return "";
		}),
		skinauthor(51, (state) -> {
			if (state instanceof SkinConfiguration) {
				return ((SkinConfiguration)state).getSelectedSkinHeader() != null ? "" : "";
			}
			return "";
		}),
		skincategory1(100, createSkincategory(0)),
		skincategory2(101, createSkincategory(1)),
		skincategory3(102, createSkincategory(2)),
		skincategory4(103, createSkincategory(3)),
		skincategory5(104, createSkincategory(4)),
		skincategory6(105, createSkincategory(5)),
		skincategory7(106, createSkincategory(6)),
		skincategory8(107, createSkincategory(7)),
		skincategory9(108, createSkincategory(8)),
		skincategory10(109, createSkincategory(9)),
		skinitem1(110, createSkinitem(0)),
		skinitem2(111, createSkinitem(1)),
		skinitem3(112, createSkinitem(2)),
		skinitem4(113, createSkinitem(3)),
		skinitem5(114, createSkinitem(4)),
		skinitem6(115, createSkinitem(5)),
		skinitem7(116, createSkinitem(6)),
		skinitem8(117, createSkinitem(7)),
		skinitem9(118, createSkinitem(8)),
		skinitem10(119, createSkinitem(9)),
		rankingname1(120, createRankingname(0)),
		rankingname2(121, createRankingname(1)),
		rankingname3(122, createRankingname(2)),
		rankingname4(123, createRankingname(3)),
		rankingname5(124, createRankingname(4)),
		rankingname6(125, createRankingname(5)),
		rankingname7(126, createRankingname(6)),
		rankingname8(127, createRankingname(7)),
		rankingname9(128, createRankingname(8)),
		rankingname10(129, createRankingname(9)),
		coursetitle1(150, createCoursetitle(0)),
		coursetitle2(151, createCoursetitle(1)),
		coursetitle3(152, createCoursetitle(2)),
		coursetitle4(153, createCoursetitle(3)),
		coursetitle5(154, createCoursetitle(4)),
		coursetitle6(155, createCoursetitle(5)),
		coursetitle7(156, createCoursetitle(6)),
		coursetitle8(157, createCoursetitle(7)),
		coursetitle9(158, createCoursetitle(8)),
		coursetitle10(159, createCoursetitle(9)),
		targetnamep10(200, createTargetname(-10)),
		targetnamep9(201, createTargetname(-9)),
		targetnamep8(202, createTargetname(-8)),
		targetnamep7(203, createTargetname(-7)),
		targetnamep6(204, createTargetname(-6)),
		targetnamep5(205, createTargetname(-5)),
		targetnamep4(206, createTargetname(-4)),
		targetnamep3(207, createTargetname(-3)),
		targetnamep2(208, createTargetname(-2)),
		targetnamep1(209, createTargetname(-1)),
		targetnamen1(210, createTargetname(1)),
		targetnamen2(211, createTargetname(2)),
		targetnamen3(212, createTargetname(3)),
		targetnamen4(213, createTargetname(4)),
		targetnamen5(214, createTargetname(5)),
		targetnamen6(215, createTargetname(6)),
		targetnamen7(216, createTargetname(7)),
		targetnamen8(217, createTargetname(8)),
		targetnamen9(218, createTargetname(9)),
		targetnamen10(219, createTargetname(10)),

		directory(1000, (state) -> {
			if (state instanceof MusicSelector) {
				return ((MusicSelector) state).getBarRender().getDirectoryString();
			}
			return "";
		}),
		tablename(1001, (state) -> (state.main.getPlayerResource().getTablename())),
		tablelevel(1002, (state) -> (state.main.getPlayerResource().getTablelevel())),
		tablefull(1003, (state) -> (state.main.getPlayerResource().getTableFullname())),
		version(1010, (state) -> (state.main.getVersion())),
		irname(1020, (state) -> {
			final IRConfig[] irconfig = state.main.getPlayerResource().getPlayerConfig().getIrconfig();
			if (irconfig.length > 0) {
				return irconfig[0].getIrname();
			}
			return "";
		}),
		;
		
		/**
		 * property ID
		 */
		private final int id;
		/**
		 * StringProperty
		 */
		private final StringProperty property;
		
		private StringType(int id, StringProperty property) {
			this.id = id;
			this.property = property;
		}
		
		private static StringProperty createSkincategory(final int index) {
			return (state) -> {
				if (state instanceof SkinConfiguration) {
					return ((SkinConfiguration)state).getCategoryName(index);
				}
				return "";
			};
		}
		
		private static StringProperty createSkinitem(final int index) {
			return (state) -> {
				if (state instanceof SkinConfiguration) {
					return ((SkinConfiguration)state).getDisplayValue(index);
				}
				return "";
			};
		}
		
		private static StringProperty createRankingname(final int index) {
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
				return score != null ? (score.player.length() > 0 ? score.player : "YOU") : "";
			};

		}
		
		private static StringProperty createTargetname(final int index) {
			return (state) -> {
				String[] targets = TargetProperty.getTargets();
				int id = -1;
				for(int i = 0;i < targets.length;i++) {
					if(targets[i].equals(state.main.getPlayerConfig().getTargetid())) {
						id = i;
						break;
					}
				}
				int offset = index >= 0 ? index : (targets.length + index);
				return id >= 0 ? TargetProperty.getTargetName(targets[(id + offset) % targets.length]) : "";
			};
		}
		
		private static StringProperty createCoursetitle(final int index) {
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar bar = ((MusicSelector) state).getSelectedBar();
					if (bar instanceof GradeBar) {
						if (((GradeBar) bar).getSongDatas().length > index) {
							SongData song = ((GradeBar) bar).getSongDatas()[index];
							final String songname = song != null && song.getTitle() != null ? song.getTitle()
									: "----";
							return song != null && song.getPath() != null ? songname : "(no song) " + songname;
						}
					} else if (bar instanceof RandomCourseBar) {
						if (((RandomCourseBar) bar).getCourseData().getStage().length > index) {
							RandomStageData stage = ((RandomCourseBar) bar).getCourseData().getStage()[index];
							final String stagename = stage != null && stage.getTitle() != null ? stage.getTitle()
									: "----";
							return stage != null ? stagename : "(no song) " + stagename;
						}
					}
				} else {
					CourseData course = state.main.getPlayerResource().getCourseData();
					if (course != null && course.getSong().length > index && course.getSong()[index] != null) {
						return course.getSong()[index].getTitle();
					}
				}
				return "";
			};
		}
	}
}
