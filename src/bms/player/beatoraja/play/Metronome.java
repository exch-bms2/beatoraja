package bms.player.beatoraja.play;

import bms.player.beatoraja.audio.AudioDriver;

public class Metronome {

	boolean enabled = true;
	RhythmTimerProcessor rhythm;
	AudioDriver audio;
	int lastSections = 0;
	int lastQuarterNote = 0;

	public Metronome(BMSPlayer main) {
		this.rhythm = main.getRhythmTimerProcessor();
		this.audio = main.main.getAudioProcessor();
	}

	// rhythmのアップデート後に呼ぶ
	public void update() {
		if (enabled && rhythm != null) {
			if (rhythm.getSections() > lastSections) {
				audio.play("defaultsound/metronome/downbeat.wav", 0.3f, false);
			}else if (rhythm.getQuarterNote() > lastQuarterNote) {
				audio.play("defaultsound/metronome/upbeat.wav", 0.3f, false);
			}
			lastSections = rhythm.getSections();
			lastQuarterNote = rhythm.getQuarterNote();
		}
	}
}
