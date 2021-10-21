package bms.player.beatoraja.play;


import java.nio.file.Path;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.audio.AudioDriver;

public class Metronome  {

	private boolean enabled;	// 練習モードか否か等にもよるので、configとは別管理。
	private final PracticeConfiguration config;
	private final RhythmTimerProcessor rhythm;
	private final AudioDriver audio;
	private int lastSections = 0;
	private int lastQuarterNote = 0;

	private final Path downbeatPath;
	private final Path upbeatPath;


	public Metronome(BMSPlayer main, boolean enabled) {
		this.rhythm = main.getRhythmTimerProcessor();
		this.audio = main.main.getAudioProcessor();
		this.config = main.getPracticeConfiguration();
		this.enabled = enabled;

		//TODO:複数入っていた時の挙動および一つも得られなかった時の挙動
		downbeatPath=main.main.getCurrentState().getSoundPaths("m-down.wav", MainState.SoundType.SOUND)[0];
		upbeatPath=main.main.getCurrentState().getSoundPaths("m-up.wav", MainState.SoundType.SOUND)[0];

	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	// rhythmのアップデート後に呼ぶ
	public void update() {
		if (enabled && rhythm != null) {
			float volume = config.getPracticeProperty().metronomevolume / 100f;
			if (rhythm.getSections() > lastSections) {
				audio.play(downbeatPath.toString(), volume, false);
			}else if (rhythm.getQuarterNote() > lastQuarterNote) {
				audio.play(upbeatPath.toString(), volume, false);
			}
			lastSections = rhythm.getSections();
			lastQuarterNote = rhythm.getQuarterNote();
		}
	}
}
