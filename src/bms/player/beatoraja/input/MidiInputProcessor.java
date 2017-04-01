package bms.player.beatoraja.input;

import sun.security.util.BitArray;

import javax.sound.midi.*;
import java.util.ArrayList;

public class MidiInputProcessor implements AutoCloseable {

	public static class MidiState {

		public int MaxKeys = 128;

		BitArray keys = new BitArray(MaxKeys);

		public void noteOn(int number) {
			if (number >= 0 && number < MaxKeys) {
				keys.set(number, true);
			}
		}

		public void noteOff(int number) {
			if (number >= 0 && number < MaxKeys) {
				keys.set(number, false);
			}
		}
	}

	class MidiReceiver implements Receiver {

		public void send(MidiMessage message, long timeStamp) {
			if (message instanceof ShortMessage) {
				ShortMessage sm = (ShortMessage)message;
				switch (sm.getCommand()) {
					case ShortMessage.NOTE_OFF:
						state.noteOff(sm.getData1());
						break;
					case ShortMessage.NOTE_ON:
						if (sm.getData2() == 0) {
							state.noteOff(sm.getData1());
						} else {
							state.noteOn(sm.getData1());
						}
						break;
				}
			}
		}

		public void close(){
		}
	}

	ArrayList<MidiDevice> devices = new ArrayList<>();
	MidiReceiver receiver = new MidiReceiver();
	MidiState state = new MidiState();

	public MidiInputProcessor() {
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
	}

	public void close() {
		for (MidiDevice device : devices) {
			device.close();
		}
	}
}
