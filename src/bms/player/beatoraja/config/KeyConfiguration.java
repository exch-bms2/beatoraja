package bms.player.beatoraja.config;

import java.io.File;
import java.io.IOException;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.input.BMControllerInputProcessor;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.skin.LR2DecideSkinLoader;

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

public class KeyConfiguration extends ApplicationAdapter {

	private MainController main;
	private PlayerResource resource;

	private BitmapFont titlefont;

	private static final String[] MODE = {"7 KEYS", "9 KEYS", "14 KEYS"};
	
	private static final String[][] KEYS = {{ "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "6 KEY", "7 KEY", "F-SCR",
			"R-SCR", "START", "SELECT" }, { "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "6 KEY", "7 KEY", "8 KEY",
				"9 KEY", "START", "SELECT"  },{ "1P-1 KEY", "1P-2 KEY", "1P-3 KEY", "1P-4 KEY", "1P-5 KEY", "1P-6 KEY",
					"1P-7 KEY", "1P-F-SCR", "1P-R-SCR", "2P-1 KEY", "2P-2 KEY", "2P-3 KEY", "2P-4 KEY", "2P-5 KEY", "2P-6 KEY",
					"2P-7 KEY", "2P-F-SCR", "2P-R-SCR" , "START", "SELECT" }};
	private static final int[][] KEYSA = {{ 0,1,2,3,4,5,6,7,8,18,19}, { 0,1,2,3,4,5,6,7,8,18,19},{ 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19}};
	private static final int[][] BMKEYSA = {{ 0,1,2,3,4,5,6,7,8,9,10}, { 0,1,2,3,4,5,6,7,8,9,10},{ 0,1,2,3,4,5,6,7,8,0,1,2,3,4,5,6,7,8,9,10}};

	private static final String[] KEYCODE = { "?", "META_SHIFT_ON", "META_ALT_ON", "HOME", "BACK", "CALL", "ENDCALL",
			"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "?", "UP", "DOWN", "LEFT", "RIGHT", "3", "4", "5",
			"6", "7", "8", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
			"S", "T", "U", "V", "W", "X", "Y", "Z", ",", ".", "L_ALT", "R_ALT", "L_SHIFT", "R_SHIFT", "TAB", "SPACE", "63",
			"64", "65", "ENTER", "BACKSPACE", "8", "-", "70", "1", "2", "\\", ";", "@", "/", "@", "NUMLK", "79", "80", "+",
			"2", "3", "4", "5", "6", "7", "8", "9", "90", "1", "PGUP", "PGDN", "4", "5", "6", "7", "8", "9", "100", "1", "2",
			"3", "4", "5", "6", "7", "8", "9", "110", "1", "DEL", "3", "4", "5", "6", "7", "8", "9", "120", "1", "2", "3",
			"4", "5", "6", "7", "8", "L_CTL", "R_CTL", "ESC", "END", "INSERT", "4", "5", "6", "7", "8", "9", "0", "1",
			"2", "3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "1", "2",
			"3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
	
	private static final String[] BMCODE = {"0","1","2","3","4","5","6","7","8","9","UP","DOWN","LEFT","RIGHT"};
	
	private int cursorpos = 0;
	private boolean keyinput = false;
	
	private int mode = 0;

	public KeyConfiguration(MainController main) {
		this.main = main;
	}

	private long time = 0;

	public void create(PlayerResource resource) {
		this.resource = resource;
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 20;
		titlefont = generator.generateFont(parameter);
		time = System.currentTimeMillis();
	}

	public void render() {
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();
		BMSPlayerInputProcessor input = main.getInputProcessor();
		Config config = resource.getConfig();
		BMControllerInputProcessor[] controllers = input.getBMInputProcessor();
		
		String[] keys = KEYS[mode];
		int[] keysa = KEYSA[mode];
		int[] bmkeysa = BMKEYSA[mode];
		int[] keyassign = (mode == 0 ? config.getKeyassign7() : (mode == 1 ? config.getKeyassign9() : config.getKeyassign14()));
		int[] bmkeyassign = config.getControllerasign();
		
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		boolean[] cursor = input.getCursorState();
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
		
		if(input.getKeyBoardInputProcesseor().getLastPressedKey() == Keys.ENTER) {
			input.getKeyBoardInputProcesseor().setLastPressedKey(-1);
			for(BMControllerInputProcessor controller : controllers) {
				controller.setLastPressedButton(-1);
			}
			keyinput = true;
		}

		if (keyinput && input.getKeyBoardInputProcesseor().getLastPressedKey() != -1) {
			keyassign[keysa[cursorpos]] = input.getKeyBoardInputProcesseor().getLastPressedKey();
//			System.out.println(input.getKeyBoardInputProcesseor().getLastPressedKey());
			keyinput = false;
		}
		for(BMControllerInputProcessor controller : controllers) {
			if (keyinput && controller.getLastPressedButton() != -1) {
				bmkeyassign[bmkeysa[cursorpos]] = controller.getLastPressedButton();
//				System.out.println(input.getKeyBoardInputProcesseor().getLastPressedKey());
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
			titlefont.draw(sprite, KEYCODE[keyassign[keysa[i]]], 202, 598 - i * 24);
			titlefont.draw(sprite, BMCODE[bmkeyassign[bmkeysa[i]]], 352, 598 - i * 24);
			sprite.end();
		}
		
		if(input.isExitPressed()) {
			input.setExitPressed(false);
			main.changeState(MainController.STATE_SELECTMUSIC);
		}
	}
}
