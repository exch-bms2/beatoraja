package bms.player.beatoraja.select.bar;

import java.util.Arrays;
import bms.model.Mode;
import bms.player.beatoraja.ScoreDatabaseAccessor.ScoreDataCollector;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.utils.Array;

/**
 * ディレクトリの抽象バー。
 *
 * @author exch
 */
public abstract class DirectoryBar extends Bar {

	protected final MusicSelector selector;
	/**
	 * プレイヤーのクリアランプ数
	 */
	private final int[] lamps = new int[11];
	/**
	 * ライバルのクリアランプ数
	 */
	private final int[] rlamps = new int[11];
	/**
	 * プレイヤーのランク数
	 */
	private final int[] ranks = new int[28];
	/**
	 * 不可視譜面を表示するかどうか
	 */
	private final boolean showInvisibleChart;
	/**
	 * （Settingの内容で）ソートできるどうか
	 */
	private boolean isSortable = true;

	public DirectoryBar(MusicSelector selector) {
		this(selector, false);
	}

	public DirectoryBar(MusicSelector selector, boolean showInvisibleChart) {
		this.selector = selector;
		this.showInvisibleChart = showInvisibleChart;
	}

	public int[] getLamps() {
		return lamps;
	}

	public int[] getRivalLamps() {
		return rlamps;
	}

	public int[] getRanks() {
		return ranks;
	}

	public int getLamp(boolean isPlayer) {
		final int[] lamps = isPlayer ? this.lamps : rlamps;
		for (int i = 0; i < lamps.length; i++) {
			if (lamps[i] > 0) {
				return i;
			}
		}
		return 0;
	}

	public boolean isShowInvisibleChart() {
		return showInvisibleChart;
	}
	
	/**
	 * フォルダをソートできるどうか
	 */
	public boolean isSortable() {
        return isSortable;
    }
	
	public void setSortable(boolean val) {
        isSortable = val;
    }
	
	public void clear() {
		Arrays.fill(lamps, 0);
		Arrays.fill(rlamps, 0);
		Arrays.fill(ranks, 0);
	}

	/**
	 * ディレクトリ内のバーを返す
	 *
	 * @return ディレクトリ内のバー
	 */
	public abstract Bar[] getChildren();

	public Bar[] getChildren(Mode mode, boolean containsSameFolder) {
		Array<Bar> l = new Array<Bar>();
		for (Bar b : getChildren()) {
			if (!(mode != null && b instanceof SongBar && ((SongBar) b).getSongData().getMode() != 0
					&& ((SongBar) b).getSongData().getMode() != mode.id)) {
				boolean addBar = true;
				if (!containsSameFolder) {
					for (Bar bar : l) {
						if (b instanceof SongBar && bar instanceof SongBar
								&& ((SongBar) b).getSongData().getFolder() != null && ((SongBar) b).getSongData()
										.getFolder().equals(((SongBar) bar).getSongData().getFolder())) {
							addBar = false;
							break;
						}
					}
				}
				if (addBar) {
					l.add(b);
				}
			}
		}
		return l.toArray(Bar.class);
	}

	public void updateFolderStatus() {

	}

	protected void updateFolderStatus(SongData[] songs) {
		clear();
		final Mode mode = selector.main.getPlayerConfig().getMode();
		final ScoreDataCollector collector = (song, score) -> {
			if(song.getPath() == null || (mode != null && song.getMode() != 0 && song.getMode() != mode.id)) {
				return;
			}

			if(score != null) {
				lamps[score.getClear()]++;

				if (score.getNotes() != 0) {
					int rank = score.getExscore() * 27 / (score.getNotes() * 2);
					ranks[rank < 28 ? rank : 27]++;
				} else {
					ranks[0]++;
				}
			} else {
				lamps[0]++;
				ranks[0]++;
			}
		};

		selector.getScoreDataCache().readScoreDatas(collector, songs, selector.main.getPlayerConfig().getLnmode());
	}
}
