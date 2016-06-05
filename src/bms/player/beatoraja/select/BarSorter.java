package bms.player.beatoraja.select;

import java.util.Comparator;

/**
 * バーのソートアルゴリズム
 *
 * @author exch
 */
public interface BarSorter extends Comparator<Bar> {
    /**
     * 楽曲名ソート
     */
    public static final BarSorter NAME_SORTER = new NameSorter();
    /**
     * レベルソート
     */
    public static final BarSorter LEVEL_SORTER = new LevelSorter();
    /**
     * スコアレートソート
     */
    public static final BarSorter SCORE_SORTER = new ScoreSorter();
    /**
     * クリアランプソート
     */
    public static final BarSorter LAMP_SORTER = new LampSorter();
    /**
     * ミスカウントソート
     */
    public static final BarSorter MISSCOUNT_SORTER = new MissCountSorter();

    /**
     * ソート名を取得する
     *
     * @return ソートの名称
     */
    public abstract String getName();
}

abstract class AbstractBarSorter implements BarSorter {
    /**
     * ソート名称
     */
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

/**
 * 名前順ソート
 *
 * @author exch
 */
class NameSorter extends AbstractBarSorter {

    public NameSorter() {
        setName("SONG NAME");
    }

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
}

/**
 * レベル順ソート
 *
 * @author exch
 */
class LevelSorter extends AbstractBarSorter {

    public LevelSorter() {
        setName("LEVEL");
    }

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
        return ((SongBar) o1).getSongData().getLevel() - ((SongBar) o2).getSongData().getLevel();
    }
}

/**
 * スコアレート順ソート
 *
 * @author exch
 */
class ScoreSorter extends AbstractBarSorter {

    public ScoreSorter() {
        setName("SCORE RATE");
    }

    @Override
    public int compare(Bar o1, Bar o2) {
        if ((!(o1 instanceof SongBar) && !(o2 instanceof SongBar)) || (o1.getScore() == null && o2.getScore() == null)) {
            return 0;
        }
        if (!(o1 instanceof SongBar) || o1.getScore() == null || o1.getScore().getNotes() == 0) {
            return 1;
        }
        if (!(o2 instanceof SongBar) || o2.getScore() == null || o2.getScore().getNotes() == 0) {
            return -1;
        }
        return o1.getScore().getExscore() * 1000 / o1.getScore().getNotes() - o2.getScore().getExscore()
                * 1000 / o2.getScore().getNotes();
    }
}

/**
 * クリアランプ順ソート
 *
 * @author exch
 */
class LampSorter extends AbstractBarSorter {

    public LampSorter() {
        setName("CLEAR LAMP");
    }

    @Override
    public int compare(Bar o1, Bar o2) {
        if ((!(o1 instanceof SongBar) && !(o2 instanceof SongBar)) || (o1.getScore() == null && o2.getScore() == null)) {
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
}

/**
 * ミスカウント順ソート
 *
 * @author exch
 */
class MissCountSorter extends AbstractBarSorter {

    public MissCountSorter() {
        setName("MISS COUNT");
    }

    @Override
    public int compare(Bar o1, Bar o2) {
        if ((!(o1 instanceof SongBar) && !(o2 instanceof SongBar)) || (o1.getScore() == null && o2.getScore() == null)) {
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

