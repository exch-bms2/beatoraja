package bms.player.beatoraja.play;

//import bms.player.beatoraja.MainState;
import bms.player.beatoraja.audio.AudioDriver;

public class Metronome  {

	private boolean enabled;	// 練習モードか否か等にもよるので、configとは別管理。
	private final PracticeConfiguration config;
	private final RhythmTimerProcessor rhythm;
	private final AudioDriver audio;
	private int lastSections = 0;
	private int lastQuarterNote = 0;

	private String downbeatPath="defaultsound/m-down.wav";//ハードコード
	private String upbeatPath="defaultsound/m-up.wav";

	public static final int SOUND_M_DOWN = 0;
	public static final int SOUND_M_UP = 1;

	public Metronome(BMSPlayer main, boolean enabled) {
		this.rhythm = main.getRhythmTimerProcessor();
		this.audio = main.main.getAudioProcessor();
		this.config = main.getPracticeConfiguration();
		this.enabled = enabled;





	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	// rhythmのアップデート後に呼ぶ
	public void update() {
		if (enabled && rhythm != null) {
			float volume = config.getPracticeProperty().metronomevolume / 100f;
			if (rhythm.getSections() > lastSections) {
				audio.play(downbeatPath, volume, false);
			}else if (rhythm.getQuarterNote() > lastQuarterNote) {
				audio.play(upbeatPath, volume, false);
			}
			lastSections = rhythm.getSections();
			lastQuarterNote = rhythm.getQuarterNote();
		}
	}
}
