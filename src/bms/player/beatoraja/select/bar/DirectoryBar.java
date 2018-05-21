package bms.player.beatoraja.select.bar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import bms.model.Mode;
import bms.player.beatoraja.ScoreDatabaseAccessor.ScoreDataCollector;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;

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
	private int[] lamps = new int[11];
	/**
	 * ライバルのクリアランプ数
	 */
	private int[] rlamps = new int[11];
	/**
	 * プレイヤーのランク数
	 */
	private int[] ranks = new int[28];

	public DirectoryBar(MusicSelector selector) {
		this.selector = selector;
	}

	public int[] getLamps() {
		return lamps;
	}

	public void setLamps(int[] lamps) {
		this.lamps = lamps;
	}

	public int[] getRivalLamps() {
		return rlamps;
	}

	public void setRivalLamps(int[] lamps) {
		this.rlamps = lamps;
	}

	public int[] getRanks() {
		return ranks;
	}

	public void setRanks(int[] ranks) {
		this.ranks = ranks;
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
		List<Bar> l = new ArrayList<Bar>();
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
		return l.toArray(new Bar[l.size()]);
	}

	public void updateFolderStatus() {

	}

	protected void updateFolderStatus(SongData[] songs) {
		clear();
		final ScoreDataCollector collector = (song, score) -> {
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

		selector.getScoreDataCache().readScoreDatas(collector, songs,
				selector.main.getPlayerResource().getPlayerConfig().getLnmode());
	}
}
