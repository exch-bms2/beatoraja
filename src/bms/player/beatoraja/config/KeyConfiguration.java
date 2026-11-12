package bms.player.beatoraja.config;

import bms.player.beatoraja.input.keyboard.KeyBoardInputProcesseor;

import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinType;

import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.GdxRuntimeException;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayModeConfig.*;
import bms.player.beatoraja.input.*;
import bms.player.beatoraja.input.controller.BMControllerInputProcessor;
import bms.player.beatoraja.input.keyboard.KeyBoardInputProcesseor.ControlKeys;
import bms.player.beatoraja.input.midi.MidiInputProcessor;

/**
 * キーコンフィグ画面
 *
 * @author exch
 */
public class KeyConfiguration extends MainState {

	// TODO スキンベースへ移行

	private BitmapFont titlefont;

	private static final String[] MODE = { "5 KEYS", "7 KEYS", "9 KEYS", "10 KEYS", "14 KEYS", "24 KEYS", "24 KEYS DOUBLE" };
	private static final Mode[] MODE_HINT = { Mode.BEAT_5K,Mode.BEAT_7K, Mode.POPN_9K, Mode.BEAT_10K, Mode.BEAT_14K, Mode.KEYBOARD_24K,
			Mode.KEYBOARD_24K_DOUBLE };

	private static final String[][] KEYS = {
			{ "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "F-SCR", "R-SCR", "START", "SELECT" },
			{ "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "6 KEY", "7 KEY", "F-SCR", "R-SCR", "START", "SELECT" },
			{ "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "6 KEY", "7 KEY", "8 KEY", "9 KEY", "START", "SELECT" },
			{ "1P-1 KEY", "1P-2 KEY", "1P-3 KEY", "1P-4 KEY", "1P-5 KEY", "1P-F-SCR",
				"1P-R-SCR", "2P-1 KEY", "2P-2 KEY", "2P-3 KEY", "2P-4 KEY", "2P-5 KEY", 
				"2P-F-SCR", "2P-R-SCR", "START", "SELECT" },
			{ "1P-1 KEY", "1P-2 KEY", "1P-3 KEY", "1P-4 KEY", "1P-5 KEY", "1P-6 KEY", "1P-7 KEY", "1P-F-SCR",
					"1P-R-SCR", "2P-1 KEY", "2P-2 KEY", "2P-3 KEY", "2P-4 KEY", "2P-5 KEY", "2P-6 KEY", "2P-7 KEY",
					"2P-F-SCR", "2P-R-SCR", "START", "SELECT" },
			{ "C1", "C#1", "D1", "D#1", "E1", "F1", "F#1", "G1", "G#1", "A1", "A#1", "B1", "C2", "C#2", "D2", "D#2",
					"E2", "F2", "F#2", "G2", "G#2", "A2", "A#2", "B2", "WHEEL-UP", "WHEEL-DOWN", "START", "SELECT" },
			{ "1P-C1", "1P-C#1", "1P-D1", "1P-D#1", "1P-E1", "1P-F1", "1P-F#1", "1P-G1", "1P-G#1", "1P-A1", "1P-A#1",
					"1P-B1", "1P-C2", "1P-C#2", "1P-D2", "1P-D#2", "1P-E2", "1P-F2", "1P-F#2", "1P-G2", "1P-G#2",
					"1P-A2", "1P-A#2", "1P-B2", "1P-WHEEL-UP", "1P-WHEEL-DOWN", "2P-C1", "2P-C#1", "2P-D1", "2P-D#1",
					"2P-E1", "2P-F1", "2P-F#1", "2P-G1", "2P-G#1", "2P-A1", "2P-A#1", "2P-B1", "2P-C2", "2P-C#2",
					"2P-D2", "2P-D#2", "2P-E2", "2P-F2", "2P-F#2", "2P-G2", "2P-G#2", "2P-A2", "2P-A#2", "2P-B2",
					"2P-WHEEL-UP", "2P-WHEEL-DOWN", "START", "SELECT" } };;
	private static final int[][] KEYSA = { { 0, 1, 2, 3, 4, 5, 6, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28,
					29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1,
					-2 } };
	private static final int playerOffset = 100;

	private static final String[] SELECTKEY = { "7 KEYS", "9 KEYS", "14 KEYS" };

	private int cursorpos = 0;
	private int scrollpos = 0;
	private boolean keyinput = false;
	private int deviceSelectionSlot = -1;
	private final boolean assignmentDiagnostics = Boolean.getBoolean("beatoraja.inputDiagnostics");
	private String assignmentDevice = "";
	private String assignmentResult = "";

	private int mode = 0;
	private int modeSlot;
	private static final int[] RIGHT_MODES = {0, 1, 5};

	private ShapeRenderer shape;
	private FreeTypeFontGenerator fontGenerator;

	private BMSPlayerInputProcessor input;
	private KeyBoardInputProcesseor keyboard;
	private BMControllerInputProcessor[] controllers;
	private MidiInputProcessor midiinput;

	private PlayerConfig config;
	private PlayModeConfig pc;
	private KeyboardConfig keyboardConfig;
	private ControllerConfig[] controllerConfigs;
	private MidiConfig midiconfig;

	private boolean deletepressed = false;

	public KeyConfiguration(MainController main) {
		super(main, MainStateType.CONFIG);

	}

	public void create() {
		keyinput = false;
		deviceSelectionSlot = -1;
		assignmentDevice = "";
		assignmentResult = "";
		loadSkin(SkinType.KEY_CONFIG);
		if(getSkin() == null) {
			SkinHeader header = new SkinHeader();
			header.setSourceResolution(Resolution.HD);
			header.setDestinationResolution(main.getConfig().getResolution());
			this.setSkin(new KeyConfigurationSkin(header));
		}

		try {
			fontGenerator = new FreeTypeFontGenerator(
					Gdx.files.internal(main.getConfig().getSystemfontpath()));
			FreeTypeFontParameter parameter = new FreeTypeFontParameter();
			parameter.size = (int) (20 * getSkin().getScaleY());
			parameter.incremental = true;
			titlefont = fontGenerator.generateFont(parameter);
		} catch (GdxRuntimeException e) {
			if (fontGenerator != null) fontGenerator.dispose();
			fontGenerator = null;
			Logger.getGlobal().severe("Font読み込み失敗");
			titlefont = new BitmapFont();
		}

		shape = new ShapeRenderer();

		input = main.getInputProcessor();
		keyboard = input.getKeyBoardInputProcesseor();
		controllers = input.getBMInputProcessor();
		if (assignmentDiagnostics) Logger.getGlobal().info("KeyConfig controller enumeration: backend="
				+ main.getConfig().getControllerBackend() + ", count=" + controllers.length);
		for (BMControllerInputProcessor controller: controllers) {
			controller.setEnable(true);
			logController(controller);
		}
		midiinput = input.getMidiInputProcessor();
		setMode(0);
	}

	public void render() {
		var detectedControllers = input.getBMInputProcessor();
		if (controllers != detectedControllers) {
			for (var controller : detectedControllers) {
				controller.setEnable(true);
				boolean known = false;
				for (var previous : controllers) if (previous == controller) { known = true; break; }
				if (!known) {
					logController(controller);
					controller.setAssignmentTrace(keyinput);
					controller.setDeviceSelectionCapture(deviceSelectionSlot >= 0);
				}
			}
			controllers = detectedControllers;
		}
		final SpriteBatch sprite = main.getSpriteBatch();
		final float scaleX = (float) getSkin().getScaleX();
		final float scaleY = (float) getSkin().getScaleY();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		if (deviceSelectionSlot < 0 && input.isControlKeyPressed(ControlKeys.LEFT)) {
			setMode(Math.floorMod(modeSlot - 1, modeCount()));
		}
		if (deviceSelectionSlot < 0 && input.isControlKeyPressed(ControlKeys.RIGHT)) {
			setMode((modeSlot + 1) % modeCount());
		}

		String[] keys = KEYS[mode];
		int[] keysa = KEYSA[mode];

		if (deviceSelectionSlot >= 0) {
			if (input.isControlKeyPressed(ControlKeys.ESCAPE) || keyboard.getLastPressedKey() == Keys.ESCAPE) {
				finishDeviceSelection();
				assignmentResult = "Device selection cancelled";
			} else {
				for (var controller : controllers) {
					if (controller.takeDeviceSelectionButton() < 0) continue;
					selectController(controllerConfigs, deviceSelectionSlot, controller.getName());
					assignmentDevice = displayDeviceName(controller.getName());
					assignmentResult = "Selected as Controller " + (deviceSelectionSlot + 1);
					finishDeviceSelection();
					break;
				}
			}
		} else if (keyinput) {
			if (input.isControlKeyPressed(ControlKeys.ESCAPE) || keyboard.getLastPressedKey() == Keys.ESCAPE) {
				keyinput = false;
				assignmentResult = "Assignment cancelled";
			}
			if (keyinput && input.getKeyBoardInputProcesseor().getLastPressedKey() != -1) {
				setKeyboardKeyAssign(keysa[cursorpos]);
				// System.out.println(input.getKeyBoardInputProcesseor().getLastPressedKey());
				keyinput = false;
			}
			if (keyinput && input.getKeyBoardInputProcesseor().getMouseScratchInput().getLastMouseScratch() != -1) {
				setMouseScratchKeyAssign(keysa[cursorpos], input.getKeyBoardInputProcesseor());
				// System.out.println(input.getKeyBoardInputProcesseor().getLastMouseScratch());
				keyinput = false;
			}
			for (BMControllerInputProcessor bmc : controllers) {
				if (!keyinput) break;
				int button = bmc.takeLastPressedButton();
				if (button < 0) continue;
				int slot = assignController(controllerConfigs, keysa[cursorpos], bmc.getName(), button, this::resetKeyAssign);
				assignmentDevice = displayDeviceName(bmc.getName());
				assignmentResult = "#" + button + " " + BMControllerInputProcessor.BMKeys.toString(button)
						+ (slot >= 0 ? " - Assigned to Controller " + (slot + 1) : " - Device not selected");
				if (assignmentDiagnostics) Logger.getGlobal().info("KeyConfig controller assignment: device="
						+ bmc.getAssignmentTraceId() + ", name=" + assignmentDevice + ", logical=" + button
						+ ", target=" + keys[cursorpos] + ", slot=" + (slot + 1)
						+ ", result=" + (slot >= 0 ? "assigned (pending config save)" : "not selected; still waiting"));
				if (slot >= 0) keyinput = false;
			}
			if (keyinput && midiinput.hasLastPressedKey()) {
				setMidiKeyAssign(keysa[cursorpos]);
				keyinput = false;
			}
			if (input.isControlKeyPressed(ControlKeys.DEL)) {
				deletepressed = true;
			}
			if (!keyinput) stopAssignmentTrace();
		} else {
			if (input.isControlKeyPressed(ControlKeys.UP)) {
				cursorpos = (cursorpos + keys.length - 1) % keys.length;
			}
			if (input.isControlKeyPressed(ControlKeys.DOWN)) {
				cursorpos = (cursorpos + 1) % keys.length;
			}
			if (input.isControlKeyPressed(ControlKeys.NUM1)) {
				config.setMusicselectinput((config.getMusicselectinput() + 1) % 3);
			}
			// change contronnler device 1
			if (input.isControlKeyPressed(ControlKeys.NUM2)) {
				if (controllers.length > 0) {
					int index = 0;
					for (; index < controllers.length; index++) {
						if (controllers[index].getName().equals(pc.getController()[0].getName())) {
							break;
						}
					}
					pc.getController()[0]
							.setName(controllers[index == controllers.length ? 0 : (index + 1) % controllers.length].getName());
					pc.setController(pc.getController());
				}
			}
			// change contronnler device 2
			if (input.isControlKeyPressed(ControlKeys.NUM3)) {
				if (controllers.length > 0 && pc.getController().length > 1) {
					int index = 0;
					for (; index < controllers.length; index++) {
						if (controllers[index].getName().equals(pc.getController()[1].getName())) {
							break;
						}
					}
					pc.getController()[1]
							.setName(controllers[index == controllers.length ? 0 : (index + 1) % controllers.length].getName());
					pc.setController(pc.getController());
				}
			}

			if (input.isControlKeyPressed(ControlKeys.NUM7)) {
				keyboardConfig.setKeyAssign(MODE_HINT[mode], true);
				keyboardConfig.getMouseScratchConfig().setKeyAssign(MODE_HINT[mode]);
				for (int i = 0; i < controllerConfigs.length; i++) {
					controllerConfigs[i].setKeyAssign(MODE_HINT[mode], i, false);
				}
				midiconfig.setKeyAssign(MODE_HINT[mode], false);
			}
			if (input.isControlKeyPressed(ControlKeys.NUM8)) {
				keyboardConfig.setKeyAssign(MODE_HINT[mode], false);
				keyboardConfig.getMouseScratchConfig().setKeyAssign(MODE_HINT[mode]);
				for (int i = 0; i < controllerConfigs.length; i++) {
					controllerConfigs[i].setKeyAssign(MODE_HINT[mode], i, true);
				}
				midiconfig.setKeyAssign(MODE_HINT[mode], false);
			}
			if (input.isControlKeyPressed(ControlKeys.NUM9)) {
				keyboardConfig.setKeyAssign(MODE_HINT[mode], false);
				keyboardConfig.getMouseScratchConfig().setKeyAssign(MODE_HINT[mode]);
				for (int i = 0; i < controllerConfigs.length; i++) {
					controllerConfigs[i].setKeyAssign(MODE_HINT[mode], i, false);
				}
				midiconfig.setKeyAssign(MODE_HINT[mode], true);
			}

			if (input.isControlKeyPressed(ControlKeys.NUM4)) {
				startDeviceSelection(0);
			} else if (input.isControlKeyPressed(ControlKeys.NUM5) && controllerConfigs.length > 1) {
				startDeviceSelection(1);
			} else if (input.isControlKeyPressed(ControlKeys.ENTER)) {
				setKeyAssignMode(cursorpos);
			}

			if (input.isControlKeyPressed(ControlKeys.ESCAPE)) {
				boolean saved = main.saveConfigAndReportPlayerResult();
				if (assignmentDiagnostics) Logger.getGlobal().info("KeyConfig controller assignment: player config "
						+ (saved ? "saved" : "save failed"));
				main.changeState(MainStateType.MUSICSELECT);
			}

			if (input.isControlKeyPressed(ControlKeys.DEL)) {
				if(!deletepressed) deleteKeyAssign(keysa[cursorpos]);
				deletepressed = true;
			} else deletepressed = false;
		}

		sprite.begin();
		if(titlefont != null) {
			titlefont.setColor(Color.CYAN);
			titlefont.draw(sprite, "<-- " + MODE[mode] + (modeSlot >= KEYS.length ? " (2P)" : "") + " -->", 80 * scaleX, 650 * scaleY);
			titlefont.setColor(Color.YELLOW);
			titlefont.draw(sprite, "Keyboard", 180 * scaleX, 620 * scaleY);
			titlefont.draw(sprite, "Controller1", 330 * scaleX, 620 * scaleY);
			titlefont.draw(sprite, "MIDI", 630 * scaleX, 620 * scaleY);
			titlefont.setColor(Color.ORANGE);
			titlefont.draw(sprite, "Music Select Input (press [1] to change) :   ", 750 * scaleX, 620 * scaleY);
			titlefont.draw(sprite, SELECTKEY[config.getMusicselectinput()], 780 * scaleX, 590 * scaleY);

			titlefont.draw(sprite, "Controller 1 ([2] Next / [4] Select by press)", 750 * scaleX, 500 * scaleY);
			titlefont.draw(sprite, pc.getController()[0].getName(), 780 * scaleX, 470 * scaleY);
			if (pc.getController().length > 1) {
				titlefont.setColor(Color.YELLOW);
				titlefont.draw(sprite, "Controller2", 480 * scaleX, 620 * scaleY);
				titlefont.setColor(Color.ORANGE);
				titlefont.draw(sprite, "Controller 2 ([3] Next / [5] Select by press)", 750 * scaleX, 300 * scaleY);
				titlefont.draw(sprite, pc.getController()[1].getName(), 780 * scaleX, 270 * scaleY);
			}

			titlefont.draw(sprite, "[7] Restore to Default (Keyboard)", 750 * scaleX, 150 * scaleY);
			titlefont.draw(sprite, "[8] Restore to Default (Controller)", 750 * scaleX, 120 * scaleY);
			titlefont.draw(sprite, "[9] Restore to Default (MIDI)", 750 * scaleX, 90 * scaleY);			
			if (!assignmentResult.isEmpty()) {
				titlefont.setColor(keyinput ? Color.YELLOW : Color.WHITE);
				titlefont.draw(sprite, assignmentDevice, 750 * scaleX, 420 * scaleY, 0, assignmentDevice.length(),
						500 * scaleX, com.badlogic.gdx.utils.Align.left, false, "...");
				titlefont.draw(sprite, assignmentResult, 750 * scaleX, 390 * scaleY);
				if (keyinput) titlefont.draw(sprite, "Waiting for input (Esc to cancel)", 750 * scaleX, 360 * scaleY);
			}
		}

		sprite.end();
		if (cursorpos < scrollpos) {
			scrollpos = cursorpos;
		} else if (cursorpos - scrollpos > 24) {
			scrollpos = cursorpos - 24;
		}
		for (int i = scrollpos; i < keys.length; i++) {
			int y = 576 - (i - scrollpos) * 24;
			if (i == cursorpos) {
				shape.begin(ShapeType.Filled);
				shape.setColor(keyinput ? Color.RED : Color.BLUE);
				shape.rect(200 * scaleX, y * scaleY, 80 * scaleX, 24 * scaleY);
				shape.rect(350 * scaleX, y * scaleY, 80 * scaleX, 24 * scaleY);
				shape.rect(650 * scaleX, y * scaleY, 80 * scaleX, 24 * scaleY);
				shape.end();
			}
			sprite.begin();
			if(titlefont != null) {
				titlefont.setColor(Color.WHITE);
				titlefont.draw(sprite, keys[i], 50 * scaleX, (y + 22) * scaleY);
				titlefont.draw(sprite, getMouseScratchKeyString(keysa[i], getKeyboardKeyAssign(keysa[i]) != -1 ? 
					keyboardKeyName(getKeyboardKeyAssign(keysa[i])) : "----"), 202 * scaleX, (y + 22) * scaleY);
				titlefont.draw(sprite, getControllerKeyAssign(0, keysa[i]) != -1
						? BMControllerInputProcessor.BMKeys.toString(getControllerKeyAssign(0, keysa[i])) : "----",
						352 * scaleX, (y + 22) * scaleY);
				if (pc.getController().length > 1) {
					titlefont.draw(sprite, getControllerKeyAssign(1, keysa[i]) != -1 ?
							BMControllerInputProcessor.BMKeys.toString(getControllerKeyAssign(1, keysa[i])) : "----", 502 * scaleX,
							(y + 22) * scaleY);
				}
				titlefont.draw(sprite,
						getMidiKeyAssign(keysa[i]) != null ? getMidiKeyAssign(keysa[i]).toString() : "----",
						652 * scaleX, (y + 22) * scaleY);				
			}
			sprite.end();
		}
	}
	
	public void setKeyAssignMode(final int index) {
		if (deviceSelectionSlot >= 0) return;
		input.getKeyBoardInputProcesseor().setLastPressedKey(-1);
		input.getKeyBoardInputProcesseor().getMouseScratchInput().setLastMouseScratch(-1);
		for (BMControllerInputProcessor bmc : controllers) {
			bmc.setLastPressedButton(-1);
			bmc.setAssignmentTrace(true);
		}
		midiinput.clearLastPressedKey();
		assignmentDevice = "";
		assignmentResult = "";
		cursorpos = index;
		keyinput = true;
	}

	private void startDeviceSelection(int slot) {
		deviceSelectionSlot = slot;
		keyboard.setLastPressedKey(-1);
		assignmentDevice = "Controller " + (slot + 1);
		assignmentResult = "Press a controller button (Esc to cancel)";
		for (var controller : controllers) controller.setDeviceSelectionCapture(true);
	}

	private void finishDeviceSelection() {
		if (deviceSelectionSlot < 0) return;
		deviceSelectionSlot = -1;
		for (var controller : controllers) controller.setDeviceSelectionCapture(false);
	}

	static void selectController(ControllerConfig[] configs, int slot, String device) {
		configs[slot].setName(device);
	}
	/**
	 * キーインデックスに対応するキーの文字列を返す
	 * 
	 * @param index 
	 * @return
	 */
	public String getKeyAssign(final int index) {
		if(index < 0 || index >= KEYSA[mode].length) {
			return "!!!";
		}
		int keyindex = KEYSA[mode][index];
		
		
		final int kbinput = getKeyboardKeyAssign(keyindex);
		if(kbinput != -1) {
			return keyboardKeyName(getKeyboardKeyAssign(keyindex));
		}

		final String mouseinput = getMouseScratchKeyString(keyindex, null);
		if(mouseinput != null) {
			return mouseinput;
		}
		
		final int controllerinput = getControllerKeyAssign(0, keyindex);
		if(controllerinput != -1) {
			return BMControllerInputProcessor.BMKeys.toString(controllerinput);
		}
		if (pc.getController().length > 1) {
			final int controllerinput2 = getControllerKeyAssign(1, keyindex);
			if(controllerinput2 != -1) {
				return BMControllerInputProcessor.BMKeys.toString(controllerinput2);
			}
		}
		
		final PlayModeConfig.MidiConfig.Input midiinput = getMidiKeyAssign(keyindex);
		if(midiinput != null) {
			return midiinput.toString();
		}
		return "---";
	}

	private int modeCount() {
		return KEYS.length + (config.isIndependentPlaySides() ? RIGHT_MODES.length : 0);
	}

	private void setMode(int slot) {
		modeSlot = slot;
		boolean right = slot >= KEYS.length;
		int mode = this.mode = right ? RIGHT_MODES[slot - KEYS.length] : slot;
		config = main.getPlayerResource().getPlayerConfig();
		pc = config.getPlayInputConfig(MODE_HINT[mode], right ? bms.player.beatoraja.PlaySide.P2 : bms.player.beatoraja.PlaySide.P1);
		keyboardConfig = pc.getKeyboardConfig();
		controllerConfigs = pc.getController();
		midiconfig = pc.getMidiConfig();

		// 各configのキーサイズ等が足りない場合は補充する
		validateKeyboardLength();
		validateControllerLength();
		validateMidiLength();

		if (cursorpos >= KEYS[mode].length) {
			cursorpos = 0;
		}
	}

	private int getKeyboardKeyAssign(int index) {
		if (index >= 0) {
			return keyboardConfig.getKeyAssign()[index];
		} else if (index == -1) {
			return keyboardConfig.getStart();
		} else if (index == -2) {
			return keyboardConfig.getSelect();
		}
		return 0;
	}

	private static String keyboardKeyName(int key) {
		return Gdx.input instanceof PollingLwjgl3Input input ? input.keyName(key) : Keys.toString(key);
	}

	private void setKeyboardKeyAssign(int index) {
		if (keyboard.isReservedKey(keyboard.getLastPressedKey())) {
			return;
		}
		resetKeyAssign(index);
		if (index >= 0) {
			keyboardConfig.getKeyAssign()[index] = keyboard.getLastPressedKey();
		} else if (index == -1) {
			keyboardConfig.setStart(keyboard.getLastPressedKey());
		} else if (index == -2) {
			keyboardConfig.setSelect(keyboard.getLastPressedKey());
		}
	}

	private String getMouseScratchKeyString(int index, String defaultKeyString) {
		String keyString = null;
		if (index >= 0) {
			keyString = keyboardConfig.getMouseScratchConfig().getKeyString(index);
		} else if (index == -1) {
			keyString = keyboardConfig.getMouseScratchConfig().getStartString();
		} else if (index == -2) {
			keyString = keyboardConfig.getMouseScratchConfig().getSelectString();
		}
		if (keyString == null) {
			return defaultKeyString;
		} else {
			return keyString;
		}
	}

	private void setMouseScratchKeyAssign(int index, KeyBoardInputProcesseor kbp) {
		resetKeyAssign(index);
		int lastMouseScratch = kbp.getMouseScratchInput().getLastMouseScratch();
		if (index >= 0) {
			keyboardConfig.getMouseScratchConfig().getKeyAssign()[index] = lastMouseScratch;
		} else if (index == -1) {
			keyboardConfig.getMouseScratchConfig().setStart(lastMouseScratch);
		} else if (index == -2) {
			keyboardConfig.getMouseScratchConfig().setSelect(lastMouseScratch);
		}
	}

	private int getControllerKeyAssign(int device, int index) {
		if (index >= 0) {
			return controllerConfigs[device].getKeyAssign()[index];
		} else if (index == -1) {
			return controllerConfigs[device].getStart();
		} else if (index == -2) {
			return controllerConfigs[device].getSelect();
		}
		return 0;
	}

	static int assignController(ControllerConfig[] configs, int index, String device, int button,
			java.util.function.IntConsumer reset) {
		for (int slot = 0; slot < configs.length; slot++) {
			var config = configs[slot];
			if (!device.equals(config.getName())) continue;
			reset.accept(index);
			if (index >= 0) config.getKeyAssign()[index] = button;
			else if (index == -1) config.setStart(button);
			else if (index == -2) config.setSelect(button);
			return slot;
		}
		return -1;
	}

	private static String displayDeviceName(String name) {
		return name.replace('\r', ' ').replace('\n', ' ');
	}

	private void logController(BMControllerInputProcessor controller) {
		if (assignmentDiagnostics) Logger.getGlobal().info("KeyConfig controller enumerated: backend="
				+ main.getConfig().getControllerBackend() + ", device=" + controller.getAssignmentTraceId()
				+ ", name=" + displayDeviceName(controller.getName()));
	}

	private void stopAssignmentTrace() {
		if (controllers != null) for (var controller : controllers) controller.setAssignmentTrace(false);
	}

	private MidiConfig.Input getMidiKeyAssign(int index) {
		if (index >= 0) {
			return midiconfig.getKeyAssign(index);
		} else if (index == -1) {
			return midiconfig.getStart();
		} else if (index == -2) {
			return midiconfig.getSelect();
		}
		return new MidiConfig.Input();
	}

	private void resetKeyAssign(int index) {
		if (index >= 0) {
			keyboardConfig.getKeyAssign()[index] = -1;
			for (ControllerConfig cc : controllerConfigs) {
				cc.getKeyAssign()[index] = -1;
			}
			keyboardConfig.getMouseScratchConfig().getKeyAssign()[index] = -1;
			midiconfig.setKeyAssign(index, null);
		}
	}

	private void deleteKeyAssign(int index) {
		final int noAssign = -1;
		if (index >= 0) keyboardConfig.getKeyAssign()[index] = noAssign;
		if(index >= 0) {
			keyboardConfig.getMouseScratchConfig().getKeyAssign()[index] = noAssign;
			for (ControllerConfig cc : controllerConfigs) {
				cc.getKeyAssign()[index] = noAssign;
			}
			midiconfig.setKeyAssign(index, null);
		} else if (index == -1) {
			keyboardConfig.getMouseScratchConfig().setStart(noAssign);
			for (int i = 0; i < controllerConfigs.length; i++) {
				controllerConfigs[i].setStart(noAssign);
			}
			midiconfig.setStart(null);
		} else if (index == -2) {
			keyboardConfig.getMouseScratchConfig().setSelect(noAssign);
			for (int i = 0; i < controllerConfigs.length; i++) {
				controllerConfigs[i].setSelect(noAssign);
			}
			midiconfig.setSelect(null);
		}
	}

	private void setMidiKeyAssign(int index) {
		resetKeyAssign(index);
		if (index >= 0) {
			midiconfig.setKeyAssign(index, midiinput.getLastPressedKey());
		} else if (index == -1) {
			midiconfig.setStart(midiinput.getLastPressedKey());
		} else if (index == -2) {
			midiconfig.setSelect(midiinput.getLastPressedKey());
		}
	}

	private void validateKeyboardLength() {
		int maxKey = 0;
		for (int key : KEYSA[mode]) {
			if (key > maxKey) {
				maxKey = key;
			}
		}
		if (keyboardConfig.getKeyAssign().length <= maxKey) {
			int[] keys = new int[maxKey + 1];
			for (int i = 0; i < keyboardConfig.getKeyAssign().length; i++) {
				keys[i] = keyboardConfig.getKeyAssign()[i];
			}
			keyboardConfig.setKeyAssign(keys);
		}
	}

	private void validateControllerLength() {
		int maxPlayer = 0;
		int maxKey = 0;
		for (int key : KEYSA[mode]) {
			if (key / playerOffset > maxPlayer) {
				maxPlayer = key / playerOffset;
			}
			if (key % playerOffset > maxKey) {
				maxKey = key % playerOffset;
			}
		}
		if (controllerConfigs.length <= maxPlayer) {
			ControllerConfig[] configs = new ControllerConfig[maxPlayer + 1];
			for (int i = 0; i < configs.length; i++) {
				configs[i] = i < controllerConfigs.length ? controllerConfigs[i] : new ControllerConfig();
			}
			pc.setController(configs);
			controllerConfigs = configs;
		}
		for (ControllerConfig controllerConfig : controllerConfigs) {
			if (controllerConfig.getKeyAssign().length <= maxKey) {
				int[] keys = new int[maxKey + 1];
				for (int i = 0; i < controllerConfig.getKeyAssign().length; i++) {
					keys[i] = controllerConfig.getKeyAssign()[i];
				}
				controllerConfig.setKeyAssign(keys);
			}
		}
	}

	private void validateMidiLength() {
		int maxKey = 0;
		for (int key : KEYSA[mode]) {
			if (key > maxKey) {
				maxKey = key;
			}
		}
		if (midiconfig.getKeys().length <= maxKey) {
			MidiConfig.Input[] keys = new MidiConfig.Input[maxKey + 1];
			for (int i = 0; i < keys.length; i++) {
				keys[i] = i < midiconfig.getKeys().length ? midiconfig.getKeys()[i] : new MidiConfig.Input();
			}
			midiconfig.setKeys(keys);
		}
	}

	@Override
	public void shutdown() {
		releaseView();
	}

	@Override
	public void dispose() {
		releaseView();
		super.dispose();
	}

	private void releaseView() {
		finishDeviceSelection();
		stopAssignmentTrace();
		if (titlefont != null) {
			titlefont.dispose();
			titlefont = null;
		}
		if (fontGenerator != null) {
			fontGenerator.dispose();
			fontGenerator = null;
		}
		if (shape != null) {
			shape.dispose();
			shape = null;
		}
	}
}
