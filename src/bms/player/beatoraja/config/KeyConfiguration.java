package bms.player.beatoraja.config;

import java.io.File;
import java.io.IOException;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.skin.LR2DecideSkinLoader;
import bms.player.beatoraja.skin.SkinObject;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

public class KeyConfiguration extends ApplicationAdapter {

	private MainController main;
	private PlayerResource resource;

	private BitmapFont titlefont;

	private MusicDecideSkin skin;

	private static final String[] KEY7 = { "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "6 KEY", "7 KEY", "F-SCR",
			"R-SCR" };
	private static final String[] KEY9 = { "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "6 KEY", "7 KEY", "8 KEY",
			"9 KEY" };
	private static final String[] KEY14 = { "1P-1 KEY", "1P-2 KEY", "1P-3 KEY", "1P-4 KEY", "1P-5 KEY", "1P-6 KEY",
			"1P-7 KEY", "1P-F-SCR", "1P-R-SCR", "2P-1 KEY", "2P-2 KEY", "2P-3 KEY", "2P-4 KEY", "2P-5 KEY", "2P-6 KEY",
			"2P-7 KEY", "2P-F-SCR", "2P-R-SCR" };

	private static final String[] CONTROL = { "START", "SELECT" };

	private static final String[] KEYCODE = { "?", "META_SHIFT_ON", "META_ALT_ON", "HOME", "BACK", "CALL", "ENDCALL",
			"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "?", "?", "UP", "DOWN", "LEFT", "RIGHT", "3", "4", "5",
			"6", "7", "8", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
			"S", "T", "U", "V", "W", "X", "Y", "Z", ",", ".", "7", "8", "L_SHIFT", "R_SHIFT", "TAB", "SPACE", "63",
			"64", "65", "ENTER", "BACKSPACE", "8", "-", "70", "1", "2", "\\", ";", "@", "/", "7", "8", "9", "80", "+",
			"2", "3", "4", "5", "6", "7", "8", "9", "90", "1", "2", "3", "4", "5", "6", "7", "8", "9", "100", "1", "2",
			"3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3",
			"4", "5", "6", "7", "8", "L_CTL", "R_CTL", "ESC", "INSERT", "3", "4", "5", "6", "7", "8", "9", "0", "1",
			"2", "3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "1", "2",
			"3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };

	private int cursorpos = 0;
	private boolean keyinput = false;

	public KeyConfiguration(MainController main) {
		this.main = main;
	}

	private long time = 0;

	public void create(PlayerResource resource) {
		this.resource = resource;
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		titlefont = generator.generateFont(parameter);
		time = System.currentTimeMillis();

		if (new File("skin/decide.wav").exists()) {
			Gdx.audio.newSound(Gdx.files.internal("skin/decide.wav")).play();
		}

		if (resource.getConfig().getLr2decideskin() != null) {
			LR2DecideSkinLoader loader = new LR2DecideSkinLoader();
			try {
				skin = loader.loadMusicDecideSkin(new File(resource.getConfig().getLr2decideskin()), resource
						.getConfig().getLr2decideskinoption());
			} catch (IOException e) {
				e.printStackTrace();
				skin = new MusicDecideSkin();
			}
		} else {
			skin = new MusicDecideSkin();
		}
	}

	public void render() {
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();
		BMSPlayerInputProcessor input = main.getInputProcessor();
		Config config = resource.getConfig();
		
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		boolean[] cursor = input.getCursorState();
		if (cursor[0]) {
			cursor[0] = false;
			cursorpos = (cursorpos + KEY7.length - 1) % KEY7.length;
			input.getKeyBoardInputProcesseor().setLastPressedKey(-1);
		}
		if (cursor[1]) {
			cursor[1] = false;
			cursorpos = (cursorpos + 1) % KEY7.length;
		}
		if (cursor[3]) {
			input.getKeyBoardInputProcesseor().setLastPressedKey(-1);
			keyinput = true;
		}

		if (keyinput && input.getKeyBoardInputProcesseor().getLastPressedKey() != -1) {
			config.getKeyassign7()[cursorpos] = input.getKeyBoardInputProcesseor().getLastPressedKey();
			keyinput = false;
		}

		shape.begin(ShapeType.Filled);
		shape.setColor(Color.BLACK);
		shape.rect(100, 200, 400, 400);
		shape.end();
		shape.begin(ShapeType.Line);
		shape.setColor(Color.YELLOW);
		shape.rect(100, 200, 400, 400);
		shape.end();
		for (int i = 0; i < KEY7.length; i++) {
			if (i == cursorpos) {
				shape.begin(ShapeType.Filled);
				shape.setColor(keyinput ? Color.RED : Color.BLUE);
				shape.rect(180, 570 - i * 30, 60, 30);
				shape.end();
			}
			shape.begin(ShapeType.Line);
			shape.setColor(Color.YELLOW);
			shape.rect(100, 570 - i * 30, 70, 30);
			shape.rect(180, 570 - i * 30, 60, 30);
			shape.end();
			sprite.begin();
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, KEY7[i], 102, 598 - i * 30);
			titlefont.draw(sprite, KEYCODE[config.getKeyassign7()[i]], 182, 598 - i * 30);
			sprite.end();
		}
		
		if(input.isExitPressed()) {
			input.setExitPressed(false);
			main.changeState(MainController.STATE_SELECTMUSIC);
		}
	}
}
