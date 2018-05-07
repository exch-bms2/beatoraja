package bms.player.beatoraja.input;

import javax.sound.midi.*;

import bms.player.beatoraja.playmode.*;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class MidiInputProcessor extends BMSPlayerInputDevice implements AutoCloseable {

	static final int MaxKeys = 128;

	BMSPlayerInputProcessor bmsPlayerInputProcessor;
	ArrayList<MidiDevice> devices = new ArrayList<>();
	MidiReceiver receiver = new MidiReceiver();

	// milliseconds
	long starttime = 0;

	int pitch = 0;

	boolean lastPressedKeyAvailable = false;
	Input lastPressedKey = new Input();

	// pitch value: -8192 ~ 8191
	final int pitchThreshold = 8192 / 32;

	// MIDI note number -> game key number
	// NOTE: �걪�겗�뼶力뺛걽�겏1�겇�겗MIDI�궘�꺖�겓筽뉑빊�궘�꺖�돯�굤壤볝겍�걣訝띶룾�꺗
	Consumer<Boolean>[] keyMap = new Consumer[MaxKeys];

	Consumer<Boolean> pitchBendUp, pitchBendDown;

	public MidiInputProcessor(BMSPlayerInputProcessor inputProcessor) {
		super(Type.MIDI);
		this.bmsPlayerInputProcessor = inputProcessor;
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for (MidiDevice.Info info : infos) {
			try {
				MidiDevice device = MidiSystem.getMidiDevice(info);
				devices.add(device);
			} catch (MidiUnavailableException e) {
			}
		}

		clearHandlers();
	}

	public void open() {
		for (MidiDevice device : devices) {
			try {
				device.open();
				device.getTransmitter().setReceiver(receiver);
			} catch (MidiUnavailableException e) {
			}
		}
	}

	public void close() {
		for (MidiDevice device : devices) {
			device.close();
		}
	}

	public void setConfig(MidiConfig config) {
		clear();
		clearHandlers();

		Input[] keys = config.getKeys();
		for (int i=0; i<keys.length; i++) {
			final int key = i;
			setHandler(keys[i], (Boolean pressed) -> {
				bmsPlayerInputProcessor.keyChanged(this, currentTime(), key, pressed);
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

	public void clear() {
		lastPressedKeyAvailable = false;
	}

	public void clearHandlers() {
		for (int i=0; i<MaxKeys; i++) {
			keyMap[i] = null;
		}
		pitchBendUp = null;
		pitchBendDown = null;
	}

	void setHandler(Input input, Consumer<Boolean> handler) {
		if (input == null)
			return;
		switch (input.type) {
			case NOTE:
				if (input.value >= 0 && input.value < MaxKeys && keyMap[input.value] == null) {
					keyMap[input.value] = handler;
				}
				break;
			case PITCH_BEND:
				if (input.value > 0 && pitchBendUp == null) {
					pitchBendUp = handler;
				} else if (input.value < 0 && pitchBendDown == null) {
					pitchBendDown = handler;
				}
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
		lastPressedKeyAvailable = true;
		lastPressedKey.type = Input.Type.NOTE;
		lastPressedKey.value = num;
		if (keyMap[num] != null) {
			keyMap[num].accept(true);
		}
	}

	void onPitchBendUp(boolean pressed) {
		if (pressed) {
			lastPressedKeyAvailable = true;
			lastPressedKey.type = Input.Type.PITCH_BEND;
			lastPressedKey.value = 1;
		}
		if (pitchBendUp != null) {
			pitchBendUp.accept(pressed);
		}
	}

	void onPitchBendDown(boolean pressed) {
		if (pressed) {
			lastPressedKeyAvailable = true;
			lastPressedKey.type = Input.Type.PITCH_BEND;
			lastPressedKey.value = -1;
		}
		if (pitchBendDown != null) {
			pitchBendDown.accept(pressed);
		}
	}

	long currentTime() {
		return System.nanoTime() / 1000000 - starttime;
	}

	public boolean hasLastPressedKey() {
		return lastPressedKeyAvailable;
	}

	public Input getLastPressedKey() {
		return lastPressedKeyAvailable ? new Input(lastPressedKey) : null;
	}

	public void clearLastPressedKey() {
		lastPressedKeyAvailable = false;
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
					case ShortMessage.PITCH_BEND: {
						int newPitch = (int)(short)((sm.getData1() & 0x7f) | ((sm.getData2() & 0x7f) << 7)) - 0x2000;
						if (newPitch > pitchThreshold) {
							if (pitch < -pitchThreshold) {
								onPitchBendDown(false);
							}
							if (pitch <= pitchThreshold) {
								onPitchBendUp(true);
							}
						} else if (newPitch < -pitchThreshold) {
							if (pitch > pitchThreshold) {
								onPitchBendUp(false);
							}
							if (pitch >= -pitchThreshold) {
								onPitchBendDown(true);
							}
						} else {
							if (pitch > pitchThreshold) {
								onPitchBendUp(false);
							}
							if (pitch < -pitchThreshold) {
								onPitchBendDown(false);
							}
						}
						pitch = newPitch;
						break;
					}
				}
			}
		}

		public void close(){
		}
	}
}
