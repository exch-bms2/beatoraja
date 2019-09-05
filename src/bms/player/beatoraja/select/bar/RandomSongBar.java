package bms.player.beatoraja.select.bar;

import java.util.ArrayDeque;
import java.util.Queue;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;

public class RandomSongBar extends SelectableBar {

	private final static int QueueLength = 1000;
	private SongData[] songs = null;
	private MainState state;
	private Queue<Integer> queue;
	private SongData currentSong = null;
	
	public RandomSongBar(SongData[] songs, MainState state) {
		super();
		this.songs = songs;
		this.state = state;
		createIndexQueue();
	}
	
	public SongData getSongData() {
		return _getSongData();
    }
	
	private synchronized SongData _getSongData() {
		if(queue.size() == 0) {
			createIndexQueue();
		}
		
		if(state instanceof MusicSelector || currentSong == null) {
			int index = queue.remove();
			currentSong = songs[index];
		}
		
		return currentSong;
	}
	
	private void createIndexQueue() {
		queue = new ArrayDeque<>();
		for(int i = 0; i < QueueLength - 1; i++) {
			int index = ((int)(Math.random()* (songs.length)));
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
