package bms.player.beatoraja.select.bar;

import java.util.ArrayDeque;
import java.util.Queue;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;

/**
 * Bar to resolve when selecting a song. 
 * TODO: ExecutableBar extends to make things happen
 */
public class ExecutableBar extends SelectableBar {

	/**
	 * index table size
	 */
	private final static int QueueLength = 1000;

	/**
	 * source songs
	 */
	private SongData[] songs = null;

	/**
	 * current state pointer
	 */
	private MainState state;

	/**
	 * Queue for get index to SongData array.
	 */
	private Queue<Integer> queue;

	/**
	 * currentSong
	 */
	private SongData currentSong = null;

	/**
	 * Constructor
	 * 
	 * @param songs SongData array that is the source of SongData returned according to the conditions.
	 * @param state MainState pointer.
	 */
	public ExecutableBar(SongData[] songs, MainState state) {
		super();
		this.songs = songs;
		this.state = state;
		createIndexQueue();
	}

	/**
	 * @return SongData
	 */
	public SongData getSongData() {
		return _getSongData();
	}

	/**
	 * Return random SongData
	 * 
	 * @return SongData extracted from this.songs
	 */
	private synchronized SongData _getSongData() {
		if (queue.size() == 0) {
			createIndexQueue();
		}

		if (state instanceof MusicSelector || currentSong == null) {
			int index = queue.remove();
			currentSong = songs[index];
		}

		return currentSong;
	}

	/**
	 * Create random index Queue
	 */
	private void createIndexQueue() {
		queue = new ArrayDeque<>();
		for (int i = 0; i < QueueLength - 1; i++) {
			int index = ((int) (Math.random() * (songs.length)));
			queue.add(index);
		}
	}

	@Override
	public String getTitle() {
		return "RANDOM SELECT";
	}

	@Override
	public String getArtist() {
		return "";
	}

	@Override
	public int getLamp(boolean isPlayer) {
		return 0;
	}

}
