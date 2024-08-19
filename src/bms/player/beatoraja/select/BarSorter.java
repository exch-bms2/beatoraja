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
public enum BarSorter {

	/**
	 * 楽曲/タイトル名ソート
	 */
	TITLE((o1, o2) -> {
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
	}),
	/**
	 * アーティスト名ソート
	 */
	ARTIST((o1, o2) -> {
		if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
			return TITLE.sorter.compare(o1, o2);
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
		return ((SongBar)o1).getSongData().getArtist().compareToIgnoreCase(((SongBar)o2).getSongData().getArtist());
	}),
	/**
	 * 楽曲のBPMソート
	 */
	BPM((o1, o2) -> {
		if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
			return TITLE.sorter.compare(o1, o2);
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
	}),
	/**
	 * 楽曲の長さソート
	 */
	LENGTH((o1, o2) -> {
		if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
			return TITLE.sorter.compare(o1, o2);
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
	}),
	/**
	 * レベルソート
	 */
	LEVEL((o1, o2) -> {
		if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
			return TITLE.sorter.compare(o1, o2);
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
		final int levelSort = ((SongBar) o1).getSongData().getLevel() - ((SongBar) o2).getSongData().getLevel();
		if(levelSort == 0){
			return ((SongBar)o1).getSongData().getDifficulty() - ((SongBar)o2).getSongData().getDifficulty();
		}else{
			return levelSort;
		}
	}),
	/**
	 * クリアランプソート
	 */
	CLEAR((o1, o2) -> {
		if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
			return TITLE.sorter.compare(o1, o2);
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
	}),
	/**
	 * スコアレートソート
	 */
	SCORE((o1, o2) -> {
		if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
			return TITLE.sorter.compare(o1, o2);
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
		return Float.compare(
				(float) o1.getScore().getExscore() / n1,
				(float) o2.getScore().getExscore() / n2
		);
	}),
	/**
	 * ミスカウントソート
	 */
	MISSCOUNT((o1, o2) -> {
		if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
			return TITLE.sorter.compare(o1, o2);
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
	}),
	/**
	 * Durationソート
	 */
	DURATION((o1, o2) -> {
		if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
			return TITLE.sorter.compare(o1, o2);
		}
		
		final boolean existsDuration1 = (o1.getScore() != null && o1.getScore().getAvgjudge() != Long.MAX_VALUE);
		final boolean existsDuration2 = (o2.getScore() != null && o2.getScore().getAvgjudge() != Long.MAX_VALUE);
		if (!existsDuration1 && !existsDuration2) {
			return 0;
		}
		if (!existsDuration1) {
			return 1;
		}
		if (!existsDuration2) {
			return -1;
		}
		return (int) (o1.getScore().getAvgjudge() - o2.getScore().getAvgjudge());
	}),
	/**
	 * 最終更新日時ソート
	 */
	LASTUPDATE((o1, o2) -> {
		if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
			return TITLE.sorter.compare(o1, o2);
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
		return (int) (o1.getScore().getDate() - o2.getScore().getDate());
	}),
	RIVALCOMPARE_CLEAR((o1, o2) -> {
		if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
			return TITLE.sorter.compare(o1, o2);
		}
		if ((o1.getScore() == null || o1.getRivalScore() == null) && (o2.getScore() == null || o2.getRivalScore() == null)) {
			return 0;
		}
		if (o1.getScore() == null || o1.getRivalScore() == null) {
			return 1;
		}
		if (o2.getScore() == null || o2.getRivalScore() == null) {
			return -1;
		}
		return (o1.getScore().getClear() - o1.getRivalScore().getClear()) - (o2.getScore().getClear() - o2.getRivalScore().getClear());
	}),
	RIVALCOMPARE_SCORE((o1, o2) -> {
		if (!(o1 instanceof SongBar) || !(o2 instanceof SongBar)) {
			return TITLE.sorter.compare(o1, o2);
		}
		final int n1 = o1.getScore() != null ? o1.getScore().getNotes() : 0;
		final int n2 = o2.getScore() != null ? o2.getScore().getNotes() : 0;
		final int r1 = o1.getRivalScore() != null ? o1.getRivalScore().getNotes() : 0;
		final int r2 = o2.getRivalScore() != null ? o2.getRivalScore().getNotes() : 0;
		if ((n1 == 0 || r1 == 0) && (n2 == 0 || r2 == 0)) {
			return 0;
		}
		if (n1 == 0 || r1 == 0) {
			return 1;
		}
		if (n2 == 0 || r2 == 0) {
			return -1;
		}
		return Float.compare(
				(float) o1.getScore().getExscore() / n1 - (float) o1.getRivalScore().getExscore() / r1,
				(float) o2.getScore().getExscore() / n2 - (float) o2.getRivalScore().getExscore() / r2
		);
	}),
	;
	
	public static final BarSorter[] defaultSorter = {TITLE, ARTIST, BPM, LENGTH, LEVEL, CLEAR, SCORE, MISSCOUNT};

	public static final BarSorter[] allSorter = BarSorter.values();

	/**
	 * ソート名称
	 */
	public final Comparator<Bar> sorter;

	private BarSorter(Comparator<Bar> sorter) {
		this.sorter = sorter;
	}
}
