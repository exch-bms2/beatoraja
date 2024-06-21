package bms.player.beatoraja.input;

import bms.player.beatoraja.PlayModeConfig.MidiConfig;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * MIDIデバイス入力処理用クラス
 *
 * @author excln
 */
public final class MidiInputProcessor extends BMSPlayerInputDevice implements AutoCloseable {

	static final int MaxKeys = 128;

	ArrayList<MidiDevice> devices = new ArrayList<>();
	MidiReceiver receiver = new MidiReceiver();

	// milliseconds
	long starttime = 0;

	int pitch = 0;

	boolean lastPressedKeyAvailable = false;
	MidiConfig.Input lastPressedKey = new MidiConfig.Input();

	// pitch value: -8192 ~ 8191
	final int pitchThreshold = 8192 / 32;

	// MIDI note number -> game key number
	// NOTE: この方法だと1つのMIDIキーに複数キー割り当てが不可能
	KeyHandler[] keyMap = new KeyHandler[MaxKeys];

	KeyHandler pitchBendUp, pitchBendDown;

	public MidiInputProcessor(BMSPlayerInputProcessor inputProcessor) {
		super(inputProcessor, Type.MIDI);
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
		devices.forEach(device -> {
			try {
				device.open();
				device.getTransmitter().setReceiver(receiver);
			} catch (MidiUnavailableException e) {
			}			
		});
	}

	public void close() {
		devices.forEach(MidiDevice::close);
	}

	public void setConfig(MidiConfig config) {
		clear();
		clearHandlers();

		MidiConfig.Input[] keys = config.getKeys();
		for (int i=0; i<keys.length; i++) {
			final int key = i;
			setHandler(keys[i], pressed -> {
				bmsPlayerInputProcessor.keyChanged(this, currentTime(), key, pressed);
				bmsPlayerInputProcessor.setAnalogState(key, false, 0);
			});
		}

		setHandler(config.getStart(), pressed -> {
			bmsPlayerInputProcessor.startChanged(pressed);
		});
		setHandler(config.getSelect(), pressed -> {
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
		Arrays.fill(keyMap, null);
		pitchBendUp = null;
		pitchBendDown = null;
	}

	void setHandler(MidiConfig.Input input, KeyHandler handler) {
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
		lastPressedKey.type = MidiConfig.Input.Type.NOTE;
		lastPressedKey.value = num;
		if (keyMap[num] != null) {
			keyMap[num].accept(true);
		}
	}

	void onPitchBendUp(boolean pressed) {
		if (pressed) {
			lastPressedKeyAvailable = true;
			lastPressedKey.type = MidiConfig.Input.Type.PITCH_BEND;
			lastPressedKey.value = 1;
		}
		if (pitchBendUp != null) {
			pitchBendUp.accept(pressed);
		}
	}

	void onPitchBendDown(boolean pressed) {
		if (pressed) {
			lastPressedKeyAvailable = true;
			lastPressedKey.type = MidiConfig.Input.Type.PITCH_BEND;
			lastPressedKey.value = -1;
		}
		if (pitchBendDown != null) {
			pitchBendDown.accept(pressed);
		}
	}

	long currentTime() {
		return System.nanoTime() / 1000 - starttime;
	}

	public boolean hasLastPressedKey() {
		return lastPressedKeyAvailable;
	}

	public MidiConfig.Input getLastPressedKey() {
		return lastPressedKeyAvailable ? new MidiConfig.Input(lastPressedKey) : null;
	}

	public void clearLastPressedKey() {
		lastPressedKeyAvailable = false;
	}

	private final class MidiReceiver implements Receiver {

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
	
	private interface KeyHandler {
		
		public void accept(boolean pressed);
	}
}
