package bms.player.beatoraja.config;

import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayConfig.ControllerConfig;
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

	private static final String[] MODE = { "7 KEYS", "9 KEYS", "14 KEYS" };

	private static final String[][] KEYS = {
			{ "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "6 KEY", "7 KEY", "F-SCR", "R-SCR", "START", "SELECT" },
			{ "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "6 KEY", "7 KEY", "8 KEY", "9 KEY", "START", "SELECT" },
			{ "1P-1 KEY", "1P-2 KEY", "1P-3 KEY", "1P-4 KEY", "1P-5 KEY", "1P-6 KEY", "1P-7 KEY", "1P-F-SCR",
					"1P-R-SCR", "2P-1 KEY", "2P-2 KEY", "2P-3 KEY", "2P-4 KEY", "2P-5 KEY", "2P-6 KEY", "2P-7 KEY",
					"2P-F-SCR", "2P-R-SCR", "START", "SELECT" } };
	private static final int[][] KEYSA = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 18, 19 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 18, 19 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 } };
	private static final int[][] BMKEYSA = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 20, 21, 22, 23, 24, 25, 26, 27, 28, 9, 10 } };
	private static final int[][] MIDIKEYSA = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 18, 19 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 18, 19 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 } };

	private static final String[] SELECTKEY = {"2dx sp", "popn", "2dx dp"};

	private int cursorpos = 0;
	private boolean keyinput = false;

	private int mode = 0;

	private ShapeRenderer shape;


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
	}

	public void render() {
		final MainController main = getMainController();
		final SpriteBatch sprite = main.getSpriteBatch();
		BMSPlayerInputProcessor input = main.getInputProcessor();
		PlayerConfig config = getMainController().getPlayerResource().getPlayerConfig();
		BMControllerInputProcessor[] controllers = input.getBMInputProcessor();
		MidiInputProcessor midiinput = input.getMidiInputProcessor();

		String[] keys = KEYS[mode];
		int[] keysa = KEYSA[mode];
		int[] bmkeysa = BMKEYSA[mode];
		int[] midikeysa = MIDIKEYSA[mode];
		final PlayConfig pc = (mode == 0 ? config.getMode7() : (mode == 1 ? config.getMode9() : config
				.getMode14()));
		int[] keyassign = pc.getKeyassign();
		ControllerConfig[] controller = pc.getController();
		PlayConfig.MidiConfig midiconfig = pc.getMidiConfig();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		boolean[] cursor = input.getCursorState();
		long[] cursortime = input.getCursorTime();
		boolean[] number = input.getNumberState();
		if (cursor[0] && cursortime[0] != 0) {
			cursortime[0] = 0;
			cursorpos = (cursorpos + keys.length - 1) % keys.length;
		}
		if (cursor[1] && cursortime[1] != 0) {
			cursortime[1] = 0;
			cursorpos = (cursorpos + 1) % keys.length;
		}
		if (cursor[2] && cursortime[2] != 0) {
			cursortime[2] = 0;
			mode = (mode + KEYS.length - 1) % KEYS.length;
		}
		if (cursor[3] && cursortime[3] != 0) {
			cursortime[3] = 0;
			mode = (mode + 1) % KEYS.length;
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
			keyassign[keysa[cursorpos]] = input.getKeyBoardInputProcesseor().getLastPressedKey();
			// System.out.println(input.getKeyBoardInputProcesseor().getLastPressedKey());
			keyinput = false;
		}
		for (BMControllerInputProcessor bmc : controllers) {
			if (keyinput && bmc.getLastPressedButton() != -1) {
				int index = bmkeysa[cursorpos];
				if(bmc.getController().getName().equals(controller[index / 20].getName())) {
					controller[index / 20].getAssign()[bmkeysa[cursorpos] % 20] = bmc.getLastPressedButton();					
				}
//				System.out.println(bmc.getLastPressedButton());
				keyinput = false;
				break;
			}
		}
		if (keyinput && midiinput.hasLastPressedKey()) {
			midiconfig.setAssignment(midikeysa[cursorpos], midiinput.getLastPressedKey());
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
		for (int i = 0; i < keys.length; i++) {
			if (i == cursorpos) {
				shape.begin(ShapeType.Filled);
				shape.setColor(keyinput ? Color.RED : Color.BLUE);
				shape.rect(200, 576 - i * 24, 80, 24);
				shape.rect(350, 576 - i * 24, 80, 24);
				shape.rect(500, 576 - i * 24, 80, 24);
				shape.end();
			}
			sprite.begin();
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, keys[i], 50, 598 - i * 24);
			titlefont.draw(sprite, Keys.toString(keyassign[keysa[i]]), 202, 598 - i * 24);
			
			int index = bmkeysa[i];
			
			titlefont.draw(sprite, BMControllerInputProcessor.BMKeys.toString(controller[index / 20].getAssign()[index % 20]), 352, 598 - i * 24);

			titlefont.draw(sprite, midiconfig.getAssignment(midikeysa[i]).toString(), 502, 598 - i * 24);
			sprite.end();
		}

		if (input.isExitPressed()) {
			input.setExitPressed(false);
			main.changeState(MainController.STATE_SELECTMUSIC);
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
