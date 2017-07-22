package bms.player.beatoraja.select;

import java.util.Comparator;

/**
 * バーのソートアルゴリズム
 *
 * @author exch
 */
public enum BarSorter implements Comparator<Bar> {

	/**
	 * 楽曲名ソート
	 */
	NAME_SORTER("SONG NAME") {
		@Override
		public int compare(Bar o1, Bar o2) {
			if (!(o1 instanceof SongBar) && !(o2 instanceof SongBar)) {
				return 0;
			}
			if (!(o1 instanceof SongBar)) {
				return 1;
			}
			if (!(o2 instanceof SongBar)) {
				return -1;
			}
			return o1.getTitle().compareTo(o2.getTitle());
		}
	},
	/**
	 * レベルソート
	 */
	LEVEL_SORTER("LEVEL") {
		@Override
		public int compare(Bar o1, Bar o2) {
			if (!(o1 instanceof SongBar && ((SongBar)o1).existsSong())
					&& !(o2 instanceof SongBar && ((SongBar)o2).existsSong())) {
				return 0;
			}
			if (!(o1 instanceof SongBar && ((SongBar)o1).existsSong())) {
				return 1;
			}
			if (!(o2 instanceof SongBar && ((SongBar)o2).existsSong())) {
				return -1;
			}
			return ((SongBar) o1).getSongData().getLevel() - ((SongBar) o2).getSongData().getLevel();
		}
	},
	/**
	 * クリアランプソート
	 */
	LAMP_SORTER("CLEAR LAMP") {
		@Override
		public int compare(Bar o1, Bar o2) {
			if ((!(o1 instanceof SongBar) && !(o2 instanceof SongBar))
					|| (o1.getScore() == null && o2.getScore() == null)) {
				return 0;
			}
			if (!(o1 instanceof SongBar) || o1.getScore() == null) {
				return 1;
			}
			if (!(o2 instanceof SongBar) || o2.getScore() == null) {
				return -1;
			}
			return o1.getScore().getClear() - o2.getScore().getClear();
		}
	},
	/**
	 * スコアレートソート
	 */
	SCORE_SORTER("SCORE RATE") {
		@Override
		public int compare(Bar o1, Bar o2) {
			if ((!(o1 instanceof SongBar) && !(o2 instanceof SongBar))
					|| (o1.getScore() == null && o2.getScore() == null)) {
				return 0;
			}
			if (!(o1 instanceof SongBar) || o1.getScore() == null || o1.getScore().getNotes() == 0) {
				return 1;
			}
			if (!(o2 instanceof SongBar) || o2.getScore() == null || o2.getScore().getNotes() == 0) {
				return -1;
			}
			return o1.getScore().getExscore() * 1000 / o1.getScore().getNotes()
					- o2.getScore().getExscore() * 1000 / o2.getScore().getNotes();
		}
	},
	/**
	 * ミスカウントソート
	 */
	MISSCOUNT_SORTER("MISS COUNT") {
		@Override
		public int compare(Bar o1, Bar o2) {
			if ((!(o1 instanceof SongBar) && !(o2 instanceof SongBar))
					|| (o1.getScore() == null && o2.getScore() == null)) {
				return 0;
			}
			if (!(o1 instanceof SongBar) || o1.getScore() == null) {
				return 1;
			}
			if (!(o2 instanceof SongBar) || o2.getScore() == null) {
				return -1;
			}
			return o1.getScore().getMinbp() - o2.getScore().getMinbp();
		}
	}
	;

	/**
	 * ソート名称
	 */
	public final String name;

	private BarSorter(String name) {
		this.name = name;
	}
}
