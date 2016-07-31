package bms.player.beatoraja.config;

import bms.player.beatoraja.*;
import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.input.*;

import com.badlogic.gdx.ApplicationAdapter;
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

import java.util.logging.Logger;

/**
 * キーコンフィグ画面
 * 
 * @author exch
 */
public class KeyConfiguration extends MainState {

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
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 } };

	public static final int KEY_PLAY = 1;
	public static final int KEY_AUTO = 2;
	public static final int KEY_REPLAY = 3;
	public static final int KEY_UP = 4;
	public static final int KEY_DOWN = 5;
	public static final int KEY_FOLDER_OPEN = 6;
	public static final int KEY_FOLDER_CLOSE = 7;

	private static final String[] SELECTKEY = {"2dx", "popn"};

	public static final int[][][] keyassign = {{{KEY_PLAY, KEY_FOLDER_OPEN}, {KEY_FOLDER_CLOSE}, {KEY_FOLDER_OPEN}, {KEY_FOLDER_CLOSE}
			, {KEY_FOLDER_OPEN, KEY_AUTO}, {KEY_FOLDER_CLOSE},{KEY_FOLDER_OPEN, KEY_REPLAY}, {KEY_UP}, {KEY_DOWN}},
			{{KEY_AUTO}, {}, {KEY_FOLDER_CLOSE}, {KEY_DOWN}
					, {KEY_PLAY}, {KEY_UP},{KEY_FOLDER_OPEN}, {}, {KEY_REPLAY}}};

	/**
	 * 専コンのキーコードに対応したテキスト
	 */
	private static final String[] BMCODE = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "UP", "DOWN", "LEFT",
			"RIGHT" };

	private int cursorpos = 0;
	private boolean keyinput = false;

	private int mode = 0;

	public KeyConfiguration(MainController main) {
		super(main);
		
		this.setSkin(new MusicDecideSkin());

	}

	public void create() {
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 20;
		titlefont = generator.generateFont(parameter);
	}

	public void render() {
		final MainController main = getMainController();
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();
		BMSPlayerInputProcessor input = main.getInputProcessor();
		Config config = getMainController().getPlayerResource().getConfig();
		BMControllerInputProcessor[] controllers = input.getBMInputProcessor();

		String[] keys = KEYS[mode];
		int[] keysa = KEYSA[mode];
		int[] bmkeysa = BMKEYSA[mode];
		PlayConfig pc = (mode == 0 ? config.getMode7() : (mode == 1 ? config.getMode9() : config
				.getMode14()));
		int[] keyassign = pc.getKeyassign();
		int[] bmkeyassign = pc.getControllerassign();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		boolean[] cursor = input.getCursorState();
		boolean[] number = input.getNumberState();
		if (cursor[0]) {
			cursor[0] = false;
			cursorpos = (cursorpos + keys.length - 1) % keys.length;
		}
		if (cursor[1]) {
			cursor[1] = false;
			cursorpos = (cursorpos + 1) % keys.length;
		}
		if (cursor[2]) {
			cursor[2] = false;
			mode = (mode + KEYS.length - 1) % KEYS.length;
		}
		if (cursor[3]) {
			cursor[3] = false;
			mode = (mode + 1) % KEYS.length;
		}

		if (number[1]) {
			number[1] = false;
			config.setMusicselectinput((config.getMusicselectinput() + 1) % 2);
		}

		if (input.getKeyBoardInputProcesseor().getLastPressedKey() == Keys.ENTER) {
			input.getKeyBoardInputProcesseor().setLastPressedKey(-1);
			for (BMControllerInputProcessor controller : controllers) {
				controller.setLastPressedButton(-1);
			}
			keyinput = true;
		}

		if (keyinput && input.getKeyBoardInputProcesseor().getLastPressedKey() != -1) {
			keyassign[keysa[cursorpos]] = input.getKeyBoardInputProcesseor().getLastPressedKey();
			// System.out.println(input.getKeyBoardInputProcesseor().getLastPressedKey());
			keyinput = false;
		}
		for (BMControllerInputProcessor controller : controllers) {
			if (keyinput && controller.getLastPressedButton() != -1) {
				bmkeyassign[bmkeysa[cursorpos]] = controller.getLastPressedButton();
				// System.out.println(input.getKeyBoardInputProcesseor().getLastPressedKey());
				keyinput = false;
				break;
			}
		}

		sprite.begin();
		titlefont.setColor(Color.CYAN);
		titlefont.draw(sprite, "<-- " + MODE[mode] + " -->", 80, 650);
		titlefont.setColor(Color.YELLOW);
		titlefont.draw(sprite, "Key Board", 180, 620);
		titlefont.draw(sprite, "Controller", 330, 620);
		titlefont.setColor(Color.ORANGE);
		titlefont.draw(sprite, "Music Select (press [1] to change) :   " + SELECTKEY[config.getMusicselectinput()], 600, 620);
		sprite.end();
		for (int i = 0; i < keys.length; i++) {
			if (i == cursorpos) {
				shape.begin(ShapeType.Filled);
				shape.setColor(keyinput ? Color.RED : Color.BLUE);
				shape.rect(200, 576 - i * 24, 80, 24);
				shape.rect(350, 576 - i * 24, 80, 24);
				shape.end();
			}
			sprite.begin();
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, keys[i], 50, 598 - i * 24);
			titlefont.draw(sprite, Keys.toString(keyassign[keysa[i]]), 202, 598 - i * 24);
			titlefont.draw(sprite, BMCODE[bmkeyassign[bmkeysa[i]]], 352, 598 - i * 24);
			sprite.end();
		}

		if (input.isExitPressed()) {
			input.setExitPressed(false);
			main.changeState(MainController.STATE_SELECTMUSIC);
		}
	}

	@Override
	public void dispose() {
		if(titlefont != null) {
			titlefont.dispose();
			titlefont = null;
		}
		if(getSkin() != null) {
			getSkin().dispose();
			setSkin(null);
		}
	}
}
