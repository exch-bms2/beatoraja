package bms.player.beatoraja.select;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;

public class KeyConfigurationRenderer {

	private ShapeRenderer shape;
	private SpriteBatch sprite;
	private BitmapFont titlefont;

	private Config config;
	
	private BMSPlayerInputProcessor input;

	private static final String[] KEY7 = { "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "6 KEY", "7 KEY", "F-SCR",
			"R-SCR" };
	private static final String[] KEY9 = { "1 KEY", "2 KEY", "3 KEY", "4 KEY", "5 KEY", "6 KEY", "7 KEY", "8 KEY",
			"9 KEY" };
	private static final String[] KEY14 = { "1P-1 KEY", "1P-2 KEY", "1P-3 KEY", "1P-4 KEY", "1P-5 KEY", "1P-6 KEY",
			"1P-7 KEY", "1P-F-SCR", "1P-R-SCR", "2P-1 KEY", "2P-2 KEY", "2P-3 KEY", "2P-4 KEY", "2P-5 KEY", "2P-6 KEY",
			"2P-7 KEY", "2P-F-SCR", "2P-R-SCR"};

	private static final String[] CONTROL = { "START", "SELECT" };

	private static final String[] KEYCODE = { "?", "?", "?", "?", "?", "?", "?", "0", "1", "2", "3", "4", "5", "6", "7",
			"8", "9", "?", "?", "UP", "DOWN", "LEFT", "RIGHT", "3", "4", "5", "6", "7", "8", "A", "B", "C", "D", "E",
			"F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
			",", ".", "7", "8", "L_SHIFT", "R_SHIFT", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3",
			"4", "5", "6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3", "4",
			"5", "6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6", "7", "8", "L_CTL", "R_CTL", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6",
			"7", "8", "9", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6", "7",
			"8", "9" };

	public KeyConfigurationRenderer(BMSPlayerInputProcessor input, ShapeRenderer shape, SpriteBatch sprite, BitmapFont titlefont, Config config) {
		this.input = input;
		this.sprite = sprite;
		this.shape = shape;
		this.titlefont = titlefont;
		this.config = config;
	}

	private int cursorpos = 0;
	private boolean keyinput = false;

	public void render(boolean[] cursor) {

		if (cursor[0]) {
			cursor[0] = false;
			cursorpos = (cursorpos + KEY7.length - 1) % KEY7.length;
			input.getKeyBoardInputProcesseor().setLastPressedKey(-1);
		}
		if (cursor[1]) {
			cursor[1] = false;
			cursorpos = (cursorpos + 1) % KEY7.length;
		}
		if(cursor[3]) {
			input.getKeyBoardInputProcesseor().setLastPressedKey(-1);
			keyinput = true;
		}
		
		if(keyinput && input.getKeyBoardInputProcesseor().getLastPressedKey() != -1) {
			config.getKeyassign7()[cursorpos] = input.getKeyBoardInputProcesseor().getLastPressedKey();
			keyinput =false;
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
	}

}
