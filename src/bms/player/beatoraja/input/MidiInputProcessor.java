package bms.player.beatoraja.input;

import bms.player.beatoraja.PlayConfig.MidiConfig;

import javax.sound.midi.*;
import java.util.ArrayList;

public class MidiInputProcessor implements AutoCloseable {

	class MidiReceiver implements Receiver {

		public void send(MidiMessage message, long timeStamp) {
			if (message instanceof ShortMessage) {
				ShortMessage sm = (ShortMessage)message;
				switch (sm.getCommand()) {
					case ShortMessage.NOTE_OFF:
						noteOff(sm.getData1(), timeStamp);
						break;
					case ShortMessage.NOTE_ON:
						if (sm.getData2() == 0) {
							noteOff(sm.getData1(), timeStamp);
						} else {
							noteOn(sm.getData1(), timeStamp);
						}
						break;
				}
			}
		}

		public void close(){
		}
	}

	BMSPlayerInputProcessor bmsPlayerInputProcessor;
	ArrayList<MidiDevice> devices = new ArrayList<>();
	MidiReceiver receiver = new MidiReceiver();

	// milliseconds
	long opentime = 0;
	long timeDiff = 0;

	// MIDI note number -> game key number
	int[] keyMap = new int[128];

	public MidiInputProcessor(BMSPlayerInputProcessor inputProcessor) {
		this.bmsPlayerInputProcessor = inputProcessor;
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for (MidiDevice.Info info : infos) {
			try {
				MidiDevice device = MidiSystem.getMidiDevice(info);
				devices.add(device);
			} catch (MidiUnavailableException e) {
				e.printStackTrace();
			}
		}
	}

	public void open() {
		for (MidiDevice device : devices) {
			try {
				device.open();
				device.getTransmitter().setReceiver(receiver);
			} catch (MidiUnavailableException e) {
				e.printStackTrace();
			}
		}
		opentime = System.nanoTime() / 1000000;
	}

	public void close() {
		for (MidiDevice device : devices) {
			device.close();
		}
	}

	public void setConfig(MidiConfig config) {
		MidiConfig.Assign[] assigns = config.getAssigns();
		for (int i=0; i<assigns.length; i++) {
			switch (assigns[i].type) {
				case NOTE:
					if (assigns[i].value >= 0 && assigns[i].value < 128) {
						keyMap[assigns[i].value] = i;
					}
					break;
				case PITCH:
					// TODO: implement
					break;
				case CONTROL_CHANGE:
					// TODO: implement
					break;
			}
		}
	}

	public void setStartTime(long starttime) {
		if (opentime != 0) {
			timeDiff = starttime - opentime;
		}
	}

	void noteOff(int num, long timeStamp) {
		long time = timeStamp/1000 - timeDiff;
		bmsPlayerInputProcessor.keyChanged(0, time, keyMap[num], false);
	}

	void noteOn(int num, long timeStamp) {
		long time = timeStamp/1000 - timeDiff;
		//time = (System.nanoTime() / 1000 - starttime) / 1000;
		bmsPlayerInputProcessor.keyChanged(0, time, keyMap[num], true);
	}
}
