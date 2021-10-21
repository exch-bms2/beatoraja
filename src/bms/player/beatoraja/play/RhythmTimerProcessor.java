package bms.player.beatoraja.play;

import static bms.player.beatoraja.skin.SkinProperty.TIMER_PLAY;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_RHYTHM;

import com.badlogic.gdx.utils.LongArray;

import bms.model.BMSModel;
import bms.model.TimeLine;

import java.util.Arrays;

public class RhythmTimerProcessor {

	private long[] sectiontimes;
	private int sections = 0;
	private long rhythmtimer;
	//4分のタイミングの時間 PMSのリズムに合わせたノート拡大用
	private long[] quarterNoteTimes;
	private int quarterNote = 0;
	private long nowQuarterNoteTime = 0;

	public RhythmTimerProcessor(BMSModel model, boolean useQuarterNoteTime) {

		LongArray sectiontimes = new LongArray();
		LongArray quarterNoteTimes = new LongArray();
		TimeLine[] timelines = model.getAllTimeLines();
		for (int i = 0; i < timelines.length; i++) {
			if(timelines[i].getSectionLine()) {
				sectiontimes.add(timelines[i].getMicroTime());

				if(useQuarterNoteTime) {
					quarterNoteTimes.add(timelines[i].getMicroTime());
					double sectionLineSection = timelines[i].getSection();
					double nextSectionLineSection = timelines[i].getSection() - sectionLineSection;
					boolean last = false;
					for(int j = i + 1; j < timelines.length; j++) {
						if(timelines[j].getSectionLine()) {
							nextSectionLineSection = timelines[j].getSection() - sectionLineSection;
							break;
						} else if(j == timelines.length - 1) {
							nextSectionLineSection = timelines[j].getSection() - sectionLineSection;
							last = true;
						}
					}
					for(double j = 0.25; j <= nextSectionLineSection; j += 0.25) {
						if((!last && j != nextSectionLineSection) || last) {
							int prevIndex;
							for(prevIndex = i; timelines[prevIndex].getSection() - sectionLineSection < j; prevIndex++) {}
							prevIndex--;
							quarterNoteTimes.add((long) (timelines[prevIndex].getMicroTime() + timelines[prevIndex].getMicroStop() + (j+sectionLineSection-timelines[prevIndex].getSection()) * 240000000 / timelines[prevIndex].getBPM()));
						}
					}					
				}
			}
		}
		this.sectiontimes = sectiontimes.toArray();
		this.quarterNoteTimes = quarterNoteTimes.toArray();

	}
	
	public void update(BMSPlayer player, long deltatime, double nowbpm, int freq) {
		final long now = player.main.getNowTime();
		final long micronow = player.main.getNowMicroTime();

		rhythmtimer += deltatime * (100 - nowbpm * player.getPlaySpeed() / 60) / 100;
		player.main.setMicroTimer(TIMER_RHYTHM, rhythmtimer);

		if(sections < sectiontimes.length && (sectiontimes[sections] * 100 / freq) <= player.main.getNowMicroTime(TIMER_PLAY)) {
			sections++;;
			player.main.setTimerOn(TIMER_RHYTHM);
			rhythmtimer = micronow;
		}
		if(quarterNoteTimes.length > 0) {
			if(quarterNote < quarterNoteTimes.length && (quarterNoteTimes[quarterNote] * 100 / freq) <= player.main.getNowMicroTime(TIMER_PLAY)) {
				quarterNote++;
				nowQuarterNoteTime = now;
			} else if(quarterNote == quarterNoteTimes.length && ((nowQuarterNoteTime + 60000 / nowbpm) * 100 / freq) <= now)  {
				nowQuarterNoteTime = now;
			}
		}
	}

	// いいメソッド名が思いつかない。実装も暫定。
	public void setAtStart(BMSPlayer player, int freq) {
		final long now = player.main.getNowTime();
		final long micronow = player.main.getNowMicroTime();

		// これでいいのか？要検証(特にTimerまわり)
		rhythmtimer = micronow;
		player.main.setMicroTimer(TIMER_RHYTHM, rhythmtimer);
		nowQuarterNoteTime = now;

		sections = Arrays.binarySearch(sectiontimes, player.main.getNowMicroTime(TIMER_PLAY) * freq / 100) + 1;
		if (sections <= 0) sections *= -1;

		if (quarterNoteTimes.length != 0) {
			quarterNote = Arrays.binarySearch(quarterNoteTimes, player.main.getNowMicroTime(TIMER_PLAY) * freq / 100) + 1;
			if (quarterNote <= 0) quarterNote *= -1;
		}

	}

	public int getSections() {
		return sections;
	}

	public int getQuarterNote() {
		return quarterNote;
	}

	public long getNowQuarterNoteTime() {
		return nowQuarterNoteTime;
	}
}
