package bms.player.beatoraja.select.bar;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.ScoreData;
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
	 * bar title
	 */
	private final String title;
	
	/**
	 * source songs
	 */
	private final SongData[] songs;

	/**
	 * current state pointer
	 */
	private final MainState state;

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
	public ExecutableBar(SongData[] songs, MainState state, String title) {
		super();
		this.title = title;
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
		return this.title;
	}

	@Override
	public int getLamp(boolean isPlayer) {
		return 0;
	}

	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class RandomFolder {
		private String name;
		private Map<String, Object> filter;
		public String getName() {
			return "[RANDOM] " + name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Map<String, Object> getFilter() {
			return filter;
		}

		public void setFilter(Map<String, Object> filter) {
			this.filter = filter;
		}
		
        public boolean filter(ScoreData scoreData) {
            Set<String> filterKey = this.getFilter().keySet();
            for (String key : filterKey) {
                String getterMethodName = "get" + key.substring(0, 1).toUpperCase()
                        + key.substring(1);
                try {
                    if (getFilter().get(key) instanceof Integer value) {
                        if (scoreData == null) {
                            if (0 != value) {
                                return false;
                            }
                        } else {
                            Method getterMethod = ScoreData.class.getMethod(getterMethodName);
                            Object propertyValue = getterMethod.invoke(scoreData);
                            if (!propertyValue.equals(value)) {
                                return false;
                            }
                        }
                        return true;
                    }
                    Object valueArr[] = ((String)this.getFilter().get(key)).split("&&");
                    for (Object value : valueArr) {
                        String valueString = ((String)value).replaceAll("\\s",""); 
                        if (scoreData == null) {
                            if (!valueString.isEmpty() && !(valueString.charAt(0) == '<')) {
                                return false; 
                            }
                        } else {
                            Method getterMethod = ScoreData.class.getMethod(getterMethodName);
                            Object propertyValue = getterMethod.invoke(scoreData);
                            if (propertyValue instanceof Integer propertyValueInt) {
                                
                                if(valueString.startsWith(">=") || valueString.startsWith("=>")) {
                                    final int filterValueInt = Integer.parseInt(valueString.substring(2));
                                    if (propertyValueInt < filterValueInt) {
                                        return false;
                                    }
                                } else if(valueString.startsWith("<=") || valueString.startsWith("=<")) {
                                	final int filterValueInt = Integer.parseInt(valueString.substring(2));
                                    if (propertyValueInt > filterValueInt) {
                                        return false;
                                    }
                                } else if(valueString.startsWith("<>") || valueString.startsWith("><")  || valueString.startsWith("!=")) {
                                	final int filterValueInt = Integer.parseInt(valueString.substring(2));
                                    if (propertyValueInt == filterValueInt) {
                                        return false;
                                    }
                                } else if(valueString.startsWith(">")) {
                                	final int filterValueInt = Integer.parseInt(valueString.substring(1));
                                    if (propertyValueInt <= filterValueInt) {
                                        return false;
                                    }                                	
                                } else  if(valueString.startsWith("<")) {
                                	final int filterValueInt = Integer.parseInt(valueString.substring(1));
                                	if (propertyValueInt >= filterValueInt) {
                                        return false;
                                    }
                                } else if (!propertyValue.equals(value)) {
                                	return false;
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        }

	}
}
