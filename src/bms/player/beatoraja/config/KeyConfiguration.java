package bms.player.beatoraja.config;

import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayConfig.KeyboardConfig;
import bms.player.beatoraja.PlayConfig.ControllerConfig;
import bms.player.beatoraja.PlayConfig.MidiConfig;
import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.input.*;

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

/**
 * キーコンフィグ画面
 * 
 * @author exch
 */
public class KeyConfiguration extends MainState {

	// TODO スキンベースへ移行

	private BitmapFont titlefont;

	private static final String[] MODE = { "7 KEYS", "9 KEYS", "14 KEYS", "24 KEYS" };

	private static final String[][] KEYS = {
			{ "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "6 KEY", "7 KEY", "F-SCR", "R-SCR", "START", "SELECT" },
			{ "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "6 KEY", "7 KEY", "8 KEY", "9 KEY", "START", "SELECT" },
			{ "1P-1 KEY", "1P-2 KEY", "1P-3 KEY", "1P-4 KEY", "1P-5 KEY", "1P-6 KEY", "1P-7 KEY", "1P-F-SCR",
					"1P-R-SCR", "2P-1 KEY", "2P-2 KEY", "2P-3 KEY", "2P-4 KEY", "2P-5 KEY", "2P-6 KEY", "2P-7 KEY",
					"2P-F-SCR", "2P-R-SCR", "START", "SELECT" },
			{ "C1", "C#1", "D1", "D#1", "E1", "F1", "F#1", "G1", "G#1", "A1", "A#1", "B1",
					"C2", "C#2", "D2", "D#2", "E2", "F2", "F#2", "G2", "G#2", "A2", "A#2", "B2",
					"WHEEL-UP", "WHEEL-DOWN", "START", "SELECT" } };
	private static final int[][] KEYSA = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -2 } };
	private static final int[][] BMKEYSA = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, -1, -2 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 100, 101, 102, 103, 104, 105, 106, 107, 108, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -2 } };
	private static final int[][] MIDIKEYSA = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, -1, -2 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -2 } };
	private static final int playerOffset = 100;

	private static final String[] SELECTKEY = {"2dx sp", "popn", "2dx dp"};

	private int cursorpos = 0;
	private int scrollpos = 0;
	private boolean keyinput = false;

	private int mode = 0;

	private ShapeRenderer shape;

	private BMSPlayerInputProcessor input;
	private KeyBoardInputProcesseor keyboard;
	private BMControllerInputProcessor[] controllers;
	private MidiInputProcessor midiinput;

	private PlayerConfig config;
	private PlayConfig pc;
	private KeyboardConfig keyboardConfig;
	private ControllerConfig[] controllerConfigs;
	private MidiConfig midiconfig;

	public KeyConfiguration(MainController main) {
		super(main);
		

	}

	public void create() {
		this.setSkin(new MusicDecideSkin(Resolution.SD,Resolution.HD));
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 20;
		titlefont = generator.generateFont(parameter);
		shape = new ShapeRenderer();

		input = getMainController().getInputProcessor();
		keyboard = input.getKeyBoardInputProcesseor();
		controllers = input.getBMInputProcessor();
		midiinput = input.getMidiInputProcessor();
		setMode(0);
	}

	public void render() {
		final MainController main = getMainController();
		final SpriteBatch sprite = main.getSpriteBatch();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		boolean[] cursor = input.getCursorState();
		long[] cursortime = input.getCursorTime();
		boolean[] number = input.getNumberState();
		if (cursor[2] && cursortime[2] != 0) {
			cursortime[2] = 0;
			setMode((mode + KEYS.length - 1) % KEYS.length);
		}
		if (cursor[3] && cursortime[3] != 0) {
			cursortime[3] = 0;
			setMode((mode + 1) % KEYS.length);
		}

		String[] keys = KEYS[mode];
		int[] keysa = KEYSA[mode];
		int[] bmkeysa = BMKEYSA[mode];
		int[] midikeysa = MIDIKEYSA[mode];

		if (cursor[0] && cursortime[0] != 0) {
			cursortime[0] = 0;
			cursorpos = (cursorpos + keys.length - 1) % keys.length;
		}
		if (cursor[1] && cursortime[1] != 0) {
			cursortime[1] = 0;
			cursorpos = (cursorpos + 1) % keys.length;
		}

		if (number[1] && input.getNumberTime()[1] != 0) {
			input.getNumberTime()[1] = 0;
			config.setMusicselectinput((config.getMusicselectinput() + 1) % 3);
		}
		if (number[2] && input.getNumberTime()[2] != 0) {
			input.getNumberTime()[2] = 0;
			if(controllers.length > 0) {
				int index = 0;
				for(;index < controllers.length;index++) {
					if(controllers[index].getController().getName().equals(pc.getController()[0].getName())) {
						break;
					}
				}
				pc.getController()[0].setName(controllers[(index + 1) % controllers.length].getController().getName());
				pc.setController(pc.getController());
			}
		}
		if (number[3] && input.getNumberTime()[3] != 0) {
			input.getNumberTime()[3] = 0;
			if(controllers.length > 0 && pc.getController().length > 1) {
				int index = 0;
				for(;index < controllers.length;index++) {
					if(controllers[index].getController().getName().equals(pc.getController()[1].getName())) {
						break;
					}
				}
				pc.getController()[1].setName(controllers[(index + 1) % controllers.length].getController().getName());
				pc.setController(pc.getController());
			}
		}

		if (input.getKeyBoardInputProcesseor().getLastPressedKey() == Keys.ENTER) {
			input.getKeyBoardInputProcesseor().setLastPressedKey(-1);
			for (BMControllerInputProcessor bmc : controllers) {
				bmc.setLastPressedButton(-1);
			}
			midiinput.clearLastPressedKey();
			keyinput = true;
		}

		if (keyinput && input.getKeyBoardInputProcesseor().getLastPressedKey() != -1) {
			setKeyboardKeyAssign(keysa[cursorpos]);
			// System.out.println(input.getKeyBoardInputProcesseor().getLastPressedKey());
			keyinput = false;
		}
		for (BMControllerInputProcessor bmc : controllers) {
			if (keyinput && bmc.getLastPressedButton() != -1) {
				setControllerKeyAssign(bmkeysa[cursorpos], bmc);
//				System.out.println(bmc.getLastPressedButton());
				keyinput = false;
				break;
			}
		}
		if (keyinput && midiinput.hasLastPressedKey()) {
			setMidiKeyAssign(midikeysa[cursorpos]);
			keyinput = false;
		}

		sprite.begin();
		titlefont.setColor(Color.CYAN);
		titlefont.draw(sprite, "<-- " + MODE[mode] + " -->", 80, 650);
		titlefont.setColor(Color.YELLOW);
		titlefont.draw(sprite, "Key Board", 180, 620);
		titlefont.draw(sprite, "Controller", 330, 620);
		titlefont.draw(sprite, "MIDI", 480, 620);
		titlefont.setColor(Color.ORANGE);
		titlefont.draw(sprite, "Music Select (press [1] to change) :   " + SELECTKEY[config.getMusicselectinput()], 600, 620);
		
		titlefont.draw(sprite, "Controller Device 1 (press [2] to change) :   " + pc.getController()[0].getName(), 600, 500);
		if(pc.getController().length > 1) {
			titlefont.draw(sprite, "Controller Device 2 (press [3] to change) :   " + pc.getController()[1].getName(), 600, 300);
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
				shape.rect(200, y, 80, 24);
				shape.rect(350, y, 80, 24);
				shape.rect(500, y, 80, 24);
				shape.end();
			}
			sprite.begin();
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, keys[i], 50, y + 22);
			titlefont.draw(sprite, Keys.toString(getKeyboardKeyAssign(keysa[i])), 202, y + 22);
			titlefont.draw(sprite, BMControllerInputProcessor.BMKeys.toString(getControllerKeyAssign(bmkeysa[i])), 352, y + 22);
			titlefont.draw(sprite, getMidiKeyAssign(midikeysa[i]).toString(), 502, y + 22);
			sprite.end();
		}

		if (input.isExitPressed()) {
			input.setExitPressed(false);
			main.changeState(MainController.STATE_SELECTMUSIC);
		}
	}

	private void setMode(int mode) {
		this.mode = mode;
		config = getMainController().getPlayerResource().getPlayerConfig();
		switch (mode) {
		case 0:
			pc = config.getMode7();
			break;
		case 1:
			pc = config.getMode9();
			break;
		case 2:
			pc = config.getMode14();
			break;
		case 3:
			pc = config.getMode24();
			break;
		default:
			pc = config.getMode7();
		}
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

	private void setKeyboardKeyAssign(int index) {
		if (index >= 0) {
			keyboardConfig.getKeyAssign()[index] = keyboard.getLastPressedKey();
		} else if (index == -1) {
			keyboardConfig.setStart(keyboard.getLastPressedKey());
		} else if (index == -2) {
			keyboardConfig.setSelect(keyboard.getLastPressedKey());
		}
	}

	private int getControllerKeyAssign(int index) {
		if (index >= 0) {
			return controllerConfigs[index / playerOffset].getKeyAssign()[index % playerOffset];
		} else if (index == -1) {
			return controllerConfigs[0].getStart();
		} else if (index == -2) {
			return controllerConfigs[0].getSelect();
		}
		return 0;
	}

	private void setControllerKeyAssign(int index, BMControllerInputProcessor bmc) {
		if (index >= 0 && bmc.getController().getName().equals(controllerConfigs[index / playerOffset].getName())) {
			controllerConfigs[index / playerOffset].getKeyAssign()[index % playerOffset] = bmc.getLastPressedButton();
		} else if (index == -1 && bmc.getController().getName().equals(controllerConfigs[0].getName())) {
			controllerConfigs[0].setStart(bmc.getLastPressedButton());
		} else if (index == -2 && bmc.getController().getName().equals(controllerConfigs[0].getName())) {
			controllerConfigs[0].setSelect(bmc.getLastPressedButton());
		}
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

	private void setMidiKeyAssign(int index) {
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
		for (int key : BMKEYSA[mode]) {
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
		for (int key : MIDIKEYSA[mode]) {
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
	public void dispose() {
		super.dispose();
		if(titlefont != null) {
			titlefont.dispose();
			titlefont = null;
		}
		if(shape != null) {
			shape.dispose();
			shape = null;
		}
	}
}
