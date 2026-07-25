package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.*;
import bms.player.beatoraja.MainController.IRStatus;
import bms.player.beatoraja.select.RandomStageData;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.config.SkinConfiguration;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.ir.IRScoreData;
import bms.player.beatoraja.ir.RankingData;
import bms.player.beatoraja.play.BMSPlayer;
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
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

import static bms.player.beatoraja.skin.SkinProperty.*;

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
		StringProperty property = StringPropertyPattern.get(id);
		if (property != null) {
			return property;
		}
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
		StringProperty property = StringPropertyPattern.get(name);
		if (property != null) {
			return property;
		}
		StringType type = StringType.get(name);
		return type != null ? type.property : null;
	}

	public static StringWriter getStringWriter(final int id) {
		StringType type = StringType.get(id);
		return type != null ? type.writer : null;
	}

	public static StringWriter getStringWriter(final String name) {
		StringType type = StringType.get(name);
		return type != null ? type.writer : null;
	}

	/**
	 * Groups numbered String properties by their ID range and skin-facing name pattern.
	 */
	private enum StringPropertyPattern {
		KEY_1_TO_10(40, 10, StringType::createKeyname, new NamePattern("key", "")),
		KEY_11_TO_54(240, 44, index -> StringType.createKeyname(index + 10), new NamePattern("key", "", value -> value - 11)),
		SKIN_CATEGORY(STRING_SKIN_CUSTOMIZE_CATEGORY1, 10, StringType::createSkincategory, new NamePattern("skincategory", "")),
		SKIN_ITEM(STRING_SKIN_CUSTOMIZE_ITEM1, 10, StringType::createSkinitem, new NamePattern("skinitem", "")),
		RANKING_NAME(STRING_RANKING1_NAME, 10, StringType::createRankingname, new NamePattern("rankingname", "")),
		COURSE_TITLE(STRING_COURSE1_TITLE, 10, StringType::createCoursetitle, new NamePattern("coursetitle", "")),
		TARGET_NAME_PREVIOUS(200, 10, index -> StringType.createTargetname(index - 10),
				new NamePattern("targetnamep", "", value -> 10 - value)),
		TARGET_NAME_NEXT(210, 10, index -> StringType.createTargetname(index + 1), new NamePattern("targetnamen", "")),
		PRACTICE_ITEM(STRING_PRACTICE_ITEM1, 16,
				index -> state -> state instanceof BMSPlayer player
						? player.getPracticeConfiguration().getVisibleItemText(index) : "",
				new NamePattern("practice_item", "")),
		PRACTICE_ITEM_LABEL(STRING_PRACTICE_ITEM_LABEL1, 16,
				index -> state -> state instanceof BMSPlayer player
						? player.getPracticeConfiguration().getVisibleItemLabel(index) : "",
				new NamePattern("practice_item", "_label"), new NamePattern("practice_item_label", "")),
		PRACTICE_ITEM_VALUE(STRING_PRACTICE_ITEM_VALUE1, 16,
				index -> state -> state instanceof BMSPlayer player
						? player.getPracticeConfiguration().getVisibleItemValue(index) : "",
				new NamePattern("practice_item", "_value"), new NamePattern("practice_item_value", ""));

		private final int firstId;
		private final StringProperty[] properties;
		private final NamePattern[] namePatterns;

		StringPropertyPattern(int firstId, int count, IntFunction<StringProperty> propertyFactory, NamePattern... namePatterns) {
			this.firstId = firstId;
			this.properties = new StringProperty[count];
			for (int index = 0; index < count; index++) {
				properties[index] = propertyFactory.apply(index);
			}
			this.namePatterns = namePatterns;
		}

		private StringProperty getById(int id) {
			int index = id - firstId;
			return index >= 0 && index < properties.length ? properties[index] : null;
		}

		private StringProperty getByName(String name) {
			for (NamePattern namePattern : namePatterns) {
				int index = namePattern.getIndex(name);
				if (index >= 0 && index < properties.length) {
					return properties[index];
				}
			}
			return null;
		}

		private static StringProperty get(int id) {
			for (StringPropertyPattern pattern : values()) {
				StringProperty property = pattern.getById(id);
				if (property != null) {
					return property;
				}
			}
			return null;
		}

		private static StringProperty get(String name) {
			for (StringPropertyPattern pattern : values()) {
				StringProperty property = pattern.getByName(name);
				if (property != null) {
					return property;
				}
			}
			return null;
		}

		private record NamePattern(String prefix, String suffix, IntUnaryOperator indexMapper) {
			private NamePattern(String prefix, String suffix) {
				this(prefix, suffix, value -> value - 1);
			}

			private int getIndex(String name) {
				if (name == null || !name.startsWith(prefix) || !name.endsWith(suffix)) {
					return -1;
				}
				int end = name.length() - suffix.length();
				if (end <= prefix.length()) {
					return -1;
				}
				try {
					return indexMapper.applyAsInt(Integer.parseInt(name.substring(prefix.length(), end)));
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		}
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
		searchword(30, (state) -> "", (state, value) -> {
			if (state instanceof MusicSelector selector) {
				selector.search(value);
			}
		}),
		mode(60, (state) -> state.resource.getPlayerConfig().getModeFilter().getDisplayName()),

		sort(61, (state) -> state.resource.getPlayerConfig().getSortid()),
		difficulty(62, (state) -> state.resource.getPlayerConfig().getDifficultyFilter().getDisplayName()),

		chartreplication(86, (state) -> state.resource.getPlayerConfig().getChartReplicationMode()),

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
		/**
		 * StringWriter
		 */
		private final StringWriter writer;

		public static final List<StringType> VALUES = Collections.unmodifiableList(Arrays.asList(StringType.values()));

		private static final IntMap<StringType> ID_MAP;
		private static final Map<String, StringType> NAME_MAP;

		static {
			ID_MAP = new IntMap<>(VALUES.size());
			NAME_MAP = new HashMap<>(VALUES.size());
			for (StringType type : VALUES) {
				ID_MAP.put(type.id, type);
				NAME_MAP.put(type.name(), type);
			}
		}

		public static StringType get(int id) {
			return ID_MAP.get(id);
		}

		public static StringType get(String name) {
			return NAME_MAP.get(name);
		}
		
		private StringType(int id, StringProperty property) {
			this(id, property, null);
		}

		private StringType(int id, StringProperty property, StringWriter writer) {
			this.id = id;
			this.property = property;
			this.writer = writer;
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
				return score != null ? score.player : "";
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
