package bms.model.bmson;

public class Note {
	public int x; // as lane (BGM:0 1P 1-8 2P 11-18 ?)
	public int y; // as locate( 240BPM,1sec = 960 )
	public int l; // as length( 0:normal note 1- : long note)
	public boolean c; // as whether or not to play sound file at start.
}
