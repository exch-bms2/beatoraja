package bms.player.beatoraja.select;

import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongInformation;

/**
 * 選曲時の譜面類似度フィルター。
 *
 * <p>ノーツ数プロファイルは最も距離が小さいものに譜面を分類する。条件型フィルターは
 * 指定された譜面要素の閾値で抽出する。ユーザー定義プロファイルを導入する際に要素を
 * 追加できるよう、判定はこの列挙型に集約する。</p>
 *
 * @author exch
 */
public enum DifficultyFilter {
	ALL("ALL", 0, false, song -> true),
	BEGINNER("BEGINNER", 1, new SimilarityFactor(ChartMetric.NOTES, 0, 250, 1)),
	NORMAL("NORMAL", 2, new SimilarityFactor(ChartMetric.NOTES, 500, 250, 1)),
	HYPER("HYPER", 3, new SimilarityFactor(ChartMetric.NOTES, 700, 250, 1)),
	ANOTHER("ANOTHER", 4, new SimilarityFactor(ChartMetric.NOTES, 1300, 250, 1)),
	INSANE("INSANE", 5, new SimilarityFactor(ChartMetric.NOTES, 2700, 250, 1)),
	SCRATCH_CHART("SCRATCH CHART", 6, true, DifficultyFilter::hasScratchRatio),
	LONG_NOTE_CHART("LONG NOTE CHART", 7, true, DifficultyFilter::hasLongNoteRatio),
	SPEED_CHANGE_CHART("SPEED CHANGE CHART", 8, false, DifficultyFilter::hasSpeedChange);

	private final String displayName;
	private final int skinNumber;
	private final SimilarityFactor[] factors;
	private final boolean requiresSongInformation;
	private final ChartCondition condition;

	DifficultyFilter(String displayName, int skinNumber, SimilarityFactor... factors) {
		this.displayName = displayName;
		this.skinNumber = skinNumber;
		this.factors = factors;
		this.requiresSongInformation = false;
		this.condition = null;
	}

	DifficultyFilter(String displayName, int skinNumber, boolean requiresSongInformation, ChartCondition condition) {
		this.displayName = displayName;
		this.skinNumber = skinNumber;
		this.factors = new SimilarityFactor[0];
		this.requiresSongInformation = requiresSongInformation;
		this.condition = condition;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getSkinNumber() {
		return skinNumber;
	}

	/**
	 * 指定された譜面が、このフィルターに一致するかを返す。
	 */
	public boolean matches(SongData song) {
		return condition != null ? condition.matches(song) : closestMatch(song) == this;
	}

	public boolean requiresSongInformation() {
		return requiresSongInformation;
	}

	public double getSimilarityDistance(SongData song) {
		double distance = 0;
		for (SimilarityFactor factor : factors) {
			distance += factor.distance(song);
		}
		return distance;
	}

	private static DifficultyFilter closestMatch(SongData song) {
		DifficultyFilter closest = BEGINNER;
		double closestDistance = closest.getSimilarityDistance(song);
		for (DifficultyFilter filter : values()) {
			if (filter.factors.length == 0 || filter == BEGINNER) {
				continue;
			}
			double distance = filter.getSimilarityDistance(song);
			// 各プロファイルの境界値では、従来の閾値判定と同様に上位側を選ぶ。
			if (distance <= closestDistance) {
				closest = filter;
				closestDistance = distance;
			}
		}
		return closest;
	}

	private static boolean hasScratchRatio(SongData song) {
		SongInformation information = song.getInformation();
		if (information == null) {
			return false;
		}
		int totalNotes = totalNotes(information);
		return totalNotes > 0 && (information.getS() + information.getLs()) * 8 >= totalNotes;
	}

	private static boolean hasLongNoteRatio(SongData song) {
		SongInformation information = song.getInformation();
		if (information == null) {
			return false;
		}
		int totalNotes = totalNotes(information);
		return totalNotes > 0 && (information.getLn() + information.getLs()) * 20 >= totalNotes;
	}

	private static int totalNotes(SongInformation information) {
		return information.getN() + information.getLn() + information.getS() + information.getLs();
	}

	private static boolean hasSpeedChange(SongData song) {
		return song.getMinbpm() != song.getMaxbpm()
				|| song.hasScrollChange()
				|| song.isBpmstop();
	}

	private enum ChartMetric {
		NOTES {
			@Override
			double value(SongData song) {
				return song.getNotes();
			}
		};

		abstract double value(SongData song);
	}

	private record SimilarityFactor(ChartMetric metric, double target, double scale, double weight) {
		private double distance(SongData song) {
			return Math.abs(metric.value(song) - target) / scale * weight;
		}
	}

	@FunctionalInterface
	private interface ChartCondition {
		boolean matches(SongData song);
	}
}
