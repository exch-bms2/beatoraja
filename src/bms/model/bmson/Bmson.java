package bms.model.bmson;

public class Bmson {
	public BMSInfo info = new BMSInfo(); // as bmson informations.
	public BarLine[] lines; // as line locates.
	public EventNote[] bpmNotes; // change BPM. value is BPM.
	public EventNote[] stopNotes; // Stop flow. value is StopTime.
	public SoundChannel[] soundChannel; // as Note data.
	public BGA bga = new BGA(); // as BGA(movie) data.
}
