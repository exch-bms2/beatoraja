package bms.player.beatoraja.select;

import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.FolderBar;
import bms.player.beatoraja.select.bar.SongBar;

import java.util.Comparator;

/**
 * バーのソートアルゴリズム
 *
 * @author exch
 */
public enum BarSorter implements Comparator<Bar> {

	/**
	 * 楽曲/タイトル名ソート
	 */
	NAME_SORTER("TITLE") {
		@Override
		public int compare(Bar o1, Bar o2) {
			if (!(o1 instanceof SongBar || o1 instanceof FolderBar) && !(o2 instanceof SongBar || o2 instanceof FolderBar)) {
				return 0;
			}
			if (!(o1 instanceof SongBar || o1 instanceof FolderBar)) {
				return 1;
			}
			if (!(o2 instanceof SongBar || o2 instanceof FolderBar)) {
				return -1;
			}

			if((o1 instanceof SongBar && o2 instanceof SongBar)){
				//タイトルの比較値を変数で保持
				int title_compare;
				title_compare = ((SongBar)o1).getSongData().getTitle().compareToIgnoreCase(((SongBar)o2).getSongData().getTitle());

				if(title_compare == 0){ //タイトルが一致
					return (((SongBar)o1).getSongData().getDifficulty()-((SongBar)o2).getSongData().getDifficulty());
				}else{ //タイトルが不一致
					return title_compare;
				}
			}else{
				return o1.getTitle().compareToIgnoreCase(o2.getTitle()) ;
			}

		}
	},
	/**
	 * アーティスト名ソート
	 */
	ARTIST_SORTER("ARTIST") {
		@Override
		public int compare(Bar o1, Bar o2) {
			if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
				return NAME_SORTER.compare(o1, o2);
			}
			return o1.getArtist().compareToIgnoreCase(o2.getArtist());
		}
	},
	/**
	 * 楽曲のBPMソート
	 */
	BPM_SORTER("BPM") {
		@Override
		public int compare(Bar o1, Bar o2) {
			if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
				return NAME_SORTER.compare(o1, o2);
			}
			if (!((SongBar)o1).existsSong() && !((SongBar)o2).existsSong()) {
				return 0;
			}
			if (!((SongBar)o1).existsSong()) {
				return 1;
			}
			if (!((SongBar)o2).existsSong()) {
				return -1;
			}
			return ((SongBar) o1).getSongData().getMaxbpm() - ((SongBar) o2).getSongData().getMaxbpm();
		}
	},
	/**
	 * 楽曲の長さソート
	 */
	LENGTH_SORTER("LENGTH") {
		@Override
		public int compare(Bar o1, Bar o2) {
			if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
				return NAME_SORTER.compare(o1, o2);
			}
			if (!((SongBar)o1).existsSong() && !((SongBar)o2).existsSong()) {
				return 0;
			}
			if (!((SongBar)o1).existsSong()) {
				return 1;
			}
			if (!((SongBar)o2).existsSong()) {
				return -1;
			}
			return ((SongBar) o1).getSongData().getLength() - ((SongBar) o2).getSongData().getLength();


		}
	},
	/**
	 * レベルソート
	 */
	LEVEL_SORTER("LEVEL") {
		@Override
		public int compare(Bar o1, Bar o2) {
			if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
				return NAME_SORTER.compare(o1, o2);
			}
			if (!((SongBar)o1).existsSong() && !((SongBar)o2).existsSong()) {
				return 0;
			}
			if (!((SongBar)o1).existsSong()) {
				return 1;
			}
			if (!((SongBar)o2).existsSong()) {
				return -1;
			}

			//levelが同じ場合はDifficultyでソート
			int revelSort=((SongBar) o1).getSongData().getLevel() - ((SongBar) o2).getSongData().getLevel();
			if(revelSort==0){
				return ((SongBar)o1).getSongData().getDifficulty()-((SongBar)o2).getSongData().getDifficulty();
			}else{
				return revelSort;
			}
		}
	},
	/**
	 * クリアランプソート
	 */
	LAMP_SORTER("CLEAR LAMP") {
		@Override
		public int compare(Bar o1, Bar o2) {
			if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
				return NAME_SORTER.compare(o1, o2);
			}
			if (o1.getScore() == null && o2.getScore() == null) {
				return 0;
			}
			if (o1.getScore() == null) {
				return 1;
			}
			if (o2.getScore() == null) {
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
			if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
				return NAME_SORTER.compare(o1, o2);
			}
			final int n1 = o1.getScore() != null ? o1.getScore().getNotes() : 0;
			final int n2 = o2.getScore() != null ? o2.getScore().getNotes() : 0;
			if (n1 == 0 && n2 == 0) {
				return 0;
			}
			if (n1 == 0) {
				return 1;
			}
			if (n2 == 0) {
				return -1;
			}
			return o1.getScore().getExscore() * 1000 / n1 - o2.getScore().getExscore() * 1000 / n2;
		}
	},
	/**
	 * ミスカウントソート
	 */
	MISSCOUNT_SORTER("MISS COUNT") {
		@Override
		public int compare(Bar o1, Bar o2) {
			if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
				return NAME_SORTER.compare(o1, o2);
			}
			if (o1.getScore() == null && o2.getScore() == null) {
				return 0;
			}
			if (o1.getScore() == null) {
				return 1;
			}
			if (o2.getScore() == null) {
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
