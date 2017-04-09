package bms.player.beatoraja.input;

import bms.player.beatoraja.PlayConfig.MidiConfig;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class MidiInputProcessor implements AutoCloseable {

	static final int MaxKeys = 128;

	BMSPlayerInputProcessor bmsPlayerInputProcessor;
	ArrayList<MidiDevice> devices = new ArrayList<>();
	MidiReceiver receiver = new MidiReceiver();

	// milliseconds
	long starttime = 0;

	// MIDI note number -> game key number
	// NOTE: この方法だと1つのMIDIキーに複数キー割り当てが不可能
	Consumer<Boolean>[] keyMap = new Consumer[MaxKeys];

	public MidiInputProcessor(BMSPlayerInputProcessor inputProcessor) {
		this.bmsPlayerInputProcessor = inputProcessor;
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for (MidiDevice.Info info : infos) {
			try {
				MidiDevice device = MidiSystem.getMidiDevice(info);
				devices.add(device);
			} catch (MidiUnavailableException e) {
				Logger.getGlobal().warning("Cannot get MIDI device `" + info.getName() + "`: " + e.getMessage());
			}
		}

		for (int i=0; i<MaxKeys; i++) {
			keyMap[i] = null;
		}
	}

	public void open() {
		for (MidiDevice device : devices) {
			try {
				device.open();
				device.getTransmitter().setReceiver(receiver);
			} catch (MidiUnavailableException e) {
				Logger.getGlobal().warning("Cannot open or receive events from MIDI device `" + device.getDeviceInfo().getName() + "`: " + e.getMessage());
			}
		}
	}

	public void close() {
		for (MidiDevice device : devices) {
			device.close();
		}
	}

	public void setConfig(MidiConfig config) {
		for (int i=0; i<MaxKeys; i++) {
			keyMap[i] = null;
		}

		MidiConfig.Assign[] keys = config.getKeys();
		for (int i=0; i<keys.length; i++) {
			final int key = i;
			setHandler(keys[i], (Boolean pressed) -> {
				bmsPlayerInputProcessor.keyChanged(0, currentTime(), key, pressed);
			});
		}

		setHandler(config.getStart(), (Boolean pressed) -> {
			bmsPlayerInputProcessor.startChanged(pressed);
		});
		setHandler(config.getSelect(), (Boolean pressed) -> {
			bmsPlayerInputProcessor.setSelectPressed(pressed);
		});
	}

	public void setStartTime(long starttime) {
		this.starttime = starttime;
	}

	void setHandler(MidiConfig.Assign control, Consumer<Boolean> handler) {
		if (control == null)
			return;
		switch (control.type) {
			case NOTE:
				if (control.value >= 0 && control.value < MaxKeys) {
					keyMap[control.value] = handler;
				}
				break;
			case PITCH_BEND:
				break;
			case CONTROL_CHANGE:
				break;
		}
	}

	void noteOff(int num) {
		if (keyMap[num] != null) {
			keyMap[num].accept(false);
		}
	}

	void noteOn(int num) {
		if (keyMap[num] != null) {
			keyMap[num].accept(true);
		}
	}

	long currentTime() {
		return System.nanoTime() / 1000000 - starttime;
	}

	class MidiReceiver implements Receiver {

		public void send(MidiMessage message, long timeStamp) {
			if (message instanceof ShortMessage) {
				ShortMessage sm = (ShortMessage)message;
				switch (sm.getCommand()) {
					case ShortMessage.NOTE_OFF:
						noteOff(sm.getData1());
						break;
					case ShortMessage.NOTE_ON:
						if (sm.getData2() == 0) {
							noteOff(sm.getData1());
						} else {
							noteOn(sm.getData1());
						}
						break;
				}
			}
		}

		public void close(){
		}
	}
}
