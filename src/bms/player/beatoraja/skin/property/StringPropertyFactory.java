package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.*;
import bms.player.beatoraja.MainController.IRStatus;
import bms.player.beatoraja.config.KeyConfiguration;
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
import com.badlogic.gdx.utils.IntMap;

import java.util.*;

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
		StringType type = StringType.get(id);
		return type != null ? type.property : null;
	}
	
	/**
	 * property nameに対応するStringPropertyを返す
	 * 
	 * @param name property name
	 * @return 対応するStringProperty
	 */
	public static StringProperty getStringProperty(final String name) {
		for(StringType t : StringType.VALUES) {
			if(t.name().equals(name)) {
				return t.property;
			}
		}
		return null;
	}
	
	public enum StringType {
		
		rival(1, (state) -> {
			if (state instanceof MusicSelector selector) {
				final PlayerInformation rival = selector.getRival();
				return rival != null ? rival.getName() : "";
			} else {
				final ScoreData rival = state.resource.getTargetScoreData();
				return rival != null ? rival.getPlayer() : "";
			}
		}),
		player(2, (state) -> (state.resource.getPlayerConfig().getName())),
		target(3, (state) -> {
			if (state instanceof MusicSelector) {
				return TargetProperty.getTargetName(state.resource.getPlayerConfig().getTargetid());					
			} else {
				final ScoreData target = state.resource.getTargetScoreData();
				return target != null ? target.getPlayer() : "";
			}
		}),
		title(10, (state) -> {
			if (state instanceof MusicSelector selector && selector.getSelectedBar() instanceof DirectoryBar) {
				return selector.getSelectedBar().getTitle();
			} else if ((state instanceof MusicDecide || state instanceof CourseResult) && state.resource.getCoursetitle() != null) {
				return state.resource.getCoursetitle();						
			}
			final SongData song = state.resource.getSongdata();
			return song != null ? song.getTitle() : "";
		}),
		subtitle(11, (state) -> {
			final SongData song = state.resource.getSongdata();
			return song != null ? song.getSubtitle() : "";
		}),
		fulltitle(12, (state) -> {
			if (state instanceof MusicSelector selector && selector.getSelectedBar() instanceof DirectoryBar) {
				return selector.getSelectedBar().getTitle();
			} else if ((state instanceof MusicDecide || state instanceof CourseResult) && state.resource.getCoursetitle() != null) {
				return state.resource.getCoursetitle();
			}
			final SongData song = state.resource.getSongdata();
			return song != null ? song.getFullTitle() : "";
		}),
		genre(13, (state) -> {
			final SongData song = state.resource.getSongdata();
			return song != null ? song.getGenre() : "";
		}),
		artist(14, (state) -> {
			final SongData song = state.resource.getSongdata();
			return song != null ? song.getArtist() : "";
		}),
		subartist(15, (state) -> {
			final SongData song = state.resource.getSongdata();
			return song != null ? song.getSubartist() : "";
		}),
		fullartist(16, (state) -> {
			final SongData song = state.resource.getSongdata();
			return song != null ? song.getFullArtist() : "";
		}),
		key1(40, createKeyname(0)),
		key2(41, createKeyname(1)),
		key3(42, createKeyname(2)),
		key4(43, createKeyname(3)),
		key5(44, createKeyname(4)),
		key6(45, createKeyname(5)),
		key7(46, createKeyname(6)),
		key8(47, createKeyname(7)),
		key9(48, createKeyname(8)),
		key10(49, createKeyname(9)),
		sort(61, (state) -> state.resource.getPlayerConfig().getSortid()),

		chartreplication(86, (state) -> state.resource.getPlayerConfig().getChartReplicationMode()),

		key11(240, createKeyname(10)),
		key12(241, createKeyname(11)),
		key13(242, createKeyname(12)),
		key14(243, createKeyname(13)),
		key15(244, createKeyname(14)),
		key16(245, createKeyname(15)),
		key17(246, createKeyname(16)),
		key18(247, createKeyname(17)),
		key19(248, createKeyname(18)),
		key20(249, createKeyname(19)),
		key21(250, createKeyname(20)),
		key22(251, createKeyname(21)),
		key23(252, createKeyname(22)),
		key24(253, createKeyname(23)),
		key25(254, createKeyname(24)),
		key26(255, createKeyname(25)),
		key27(256, createKeyname(26)),
		key28(257, createKeyname(27)),
		key29(258, createKeyname(28)),
		key30(259, createKeyname(29)),
		key31(260, createKeyname(30)),
		key32(261, createKeyname(31)),
		key33(262, createKeyname(32)),
		key34(263, createKeyname(33)),
		key35(264, createKeyname(34)),
		key36(265, createKeyname(35)),
		key37(266, createKeyname(36)),
		key38(267, createKeyname(37)),
		key39(268, createKeyname(38)),
		key40(269, createKeyname(39)),
		key41(270, createKeyname(40)),
		key42(271, createKeyname(41)),
		key43(272, createKeyname(42)),
		key44(273, createKeyname(43)),
		key45(274, createKeyname(44)),
		key46(275, createKeyname(45)),
		key47(276, createKeyname(46)),
		key48(277, createKeyname(47)),
		key49(278, createKeyname(48)),
		key50(279, createKeyname(49)),
		key51(280, createKeyname(50)),
		key52(281, createKeyname(51)),
		key53(282, createKeyname(52)),
		key54(283, createKeyname(53)),

		skinname(50, (state) -> {
			if (state instanceof SkinConfiguration skinconfig) {
				return skinconfig.getSelectedSkinHeader() != null ? skinconfig.getSelectedSkinHeader().getName() : "";
			} else if(state.getSkin() != null && state.getSkin().header != null) {
				return state.getSkin().header.getName();
			}
			return "";
		}),
		skinauthor(51, (state) -> {
			if (state instanceof SkinConfiguration skinconfig) {
				return skinconfig.getSelectedSkinHeader() != null ? skinconfig.getSelectedSkinHeader().getAuthor() : "";
			} else if(state.getSkin() != null && state.getSkin().header != null) {
				return state.getSkin().header.getAuthor();
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

		directory(1000, (state) -> ((state instanceof MusicSelector selector) ? selector.getBarManager().getDirectoryString() : "")),
		tablename(1001, (state) -> (state.resource.getTablename())),
		tablelevel(1002, (state) -> (state.resource.getTablelevel())),
		tablefull(1003, (state) -> (state.resource.getTableFullname())),
		version(1010, (state) -> (state.main.getVersion())),
		irname(1020, (state) -> {
			final IRConfig[] irconfig = state.resource.getPlayerConfig().getIrconfig();
			if (irconfig.length > 0) {
				return irconfig[0].getIrname();
			}
			return "";
		}),
		irUserName(1021, (state) -> {
			final IRStatus[] ir = state.main.getIRStatus();
			if (ir.length > 0) {
				return ir[0].player.name;
			}
			return "";
		}),
		songhashmd5(1030, (state) -> {
			final SongData song = state.resource.getSongdata();
			return song != null ? song.getMd5() : "";
		}),
		songhashsha256(1031, (state) -> {
			final SongData song = state.resource.getSongdata();
			return song != null ? song.getSha256() : "";
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

		public static final List<StringType> VALUES = Collections.unmodifiableList(Arrays.asList(StringType.values()));

		private static final IntMap<StringType> ID_MAP;

		static {
			ID_MAP = new IntMap<>(VALUES.size());
			for (StringType type : VALUES) {
				ID_MAP.put(type.id, type);
			}
		}

		public static StringType get(int id) {
			return ID_MAP.get(id);
		}
		
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
				if (state instanceof MusicSelector selector) {
					irc =selector.getCurrentRankingData();
					rankingOffset = selector.getRankingOffset();
				}
				if (state instanceof AbstractResult result) {
					irc = result.getRankingData();
					rankingOffset = result.getRankingOffset();
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
					if(targets[i].equals(state.resource.getPlayerConfig().getTargetid())) {
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
				if (state instanceof MusicSelector selector) {
					final Bar bar = selector.getSelectedBar();
					if (bar instanceof GradeBar coursebar) {
						if (coursebar.getSongDatas().length > index) {
							SongData song = coursebar.getSongDatas()[index];
							final String songname = song != null && song.getTitle() != null ? song.getTitle()
									: "----";
							return song != null && song.getPath() != null ? songname : "(no song) " + songname;
						}
					} else if (bar instanceof RandomCourseBar randomcoursebar) {
						if (randomcoursebar.getCourseData().getStage().length > index) {
							RandomStageData stage = randomcoursebar.getCourseData().getStage()[index];
							final String stagename = stage != null && stage.getTitle() != null ? stage.getTitle()
									: "----";
							return stage != null ? stagename : "(no song) " + stagename;
						}
					}
				} else {
					CourseData course = state.resource.getCourseData();
					if (course != null && course.getSong().length > index && course.getSong()[index] != null) {
						return course.getSong()[index].getTitle();
					}
				}
				return "";
			};
		}
		
		private static StringProperty createKeyname(final int index) {
			return (state) -> {
				if (state instanceof KeyConfiguration keyconfig) {
					return keyconfig.getKeyAssign(index);
				}
				return "";
			};
		}		
	}
}
