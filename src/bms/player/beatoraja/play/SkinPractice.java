package bms.player.beatoraja.play;

import bms.model.Mode;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.SkinNoteDistributionGraph;
import bms.player.beatoraja.skin.SkinObject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.logging.Logger;

/**
 * プラクティス項目表示用オブジェクト
 *
 * @author exch
 */
public final class SkinPractice extends SkinObject {
	private static final int MAX_VISIBLE_ITEMS = 16;

	private final int visibleItems;
	private final SkinNoteDistributionGraph[] graphs = {
			new SkinNoteDistributionGraph(SkinNoteDistributionGraph.TYPE_NORMAL, 500, 0, 0, 0, 0),
			new SkinNoteDistributionGraph(SkinNoteDistributionGraph.TYPE_JUDGE, 500, 0, 0, 0, 0),
			new SkinNoteDistributionGraph(SkinNoteDistributionGraph.TYPE_EARLYLATE, 500, 0, 0, 0, 0),
	};

	private BitmapFont legacyFont;
	private boolean legacyResourcesInitialized;
	private BMSPlayer player;
	private long time;

	public SkinPractice(int visibleItems) {
		this.visibleItems = Math.max(0, Math.min(visibleItems, MAX_VISIBLE_ITEMS));
	}

	@Override
	public void prepare(long time, MainState state) {
		super.prepare(time, state);
		if (state instanceof BMSPlayer player) {
			this.player = player;
			this.time = time;
			if (visibleItems > 0) {
				player.getPracticeConfiguration().setVisibleItemCount(visibleItems);
			} else {
				initializeLegacyResources(player.main.getConfig());
			}
		}
	}

	/**
	 * Prepares the legacy Practice UI to be drawn in a BGA object's area.
	 */
	void prepareFallback(long time, BMSPlayer player, Rectangle region) {
		this.player = player;
		this.time = time;
		this.region.set(region);
		this.draw = true;
		initializeLegacyResources(player.main.getConfig());
	}

	@Override
	public void draw(SkinObjectRenderer sprite) {
		if (visibleItems == 0 && player != null) {
			drawLegacy(sprite);
		}
	}

	private void initializeLegacyResources(Config config) {
		if (legacyResourcesInitialized) {
			return;
		}
		legacyResourcesInitialized = true;
		try {
			FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(config.getSystemfontpath()));
			FreeTypeFontParameter parameter = new FreeTypeFontParameter();
			parameter.size = 18;
			legacyFont = generator.generateFont(parameter);
			generator.dispose();
		} catch (GdxRuntimeException e) {
			Logger.getGlobal().warning("Practice Font読み込み失敗");
		}

		for (SkinNoteDistributionGraph graph : graphs) {
			graph.setDestination(0, 0, 0, 0, 0, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, new int[0]);
		}
	}

	private void drawLegacy(SkinObjectRenderer sprite) {
		PracticeConfiguration practice = player.getPracticeConfiguration();
		final Color unfocusedColor = practice.isHorizontalInputMode() ? Color.GRAY : Color.CYAN;
		final Color focusedColor = practice.isInputTurbo() ? Color.ORANGE : Color.YELLOW;
		final int ySpacing = 22;
		float x = region.x + region.width / 8;
		float y = region.y + region.height * 7 / 8;
		if (legacyFont != null) {
			for (int i = 0; i < PracticeConfiguration.elements.length; i++) {
				if (practice.isElementAvailable(i)) {
					sprite.draw(legacyFont, PracticeConfiguration.elements[i].text.apply(practice.getPracticeProperty()), x,
							y - ySpacing * i, practice.getCursorPosition() == i ? focusedColor : unfocusedColor);
				}
			}

			String helpLine1 = "";
			String helpLine2 = "";
			Mode mode = practice.getModel().getMode();
			if (mode == Mode.POPN_9K) {
				helpLine1 = "KEYS: 2/8=UP, 3/7=DOWN, 4=LEFT, 6=RIGHT,";
				helpLine2 = "5=TURBO";
			} else if (mode == Mode.KEYBOARD_24K || mode == Mode.KEYBOARD_24K_DOUBLE) {
				helpLine1 = "KEYS: F#1/A#1=UP, G1/A1=DOWN, F1=LEFT,";
				helpLine2 = "B1=RIGHT, D#1/G#1=TURBO";
			} else {
				helpLine1 = "KEYS: SCR=UP/DOWN, 2+SCR=LEFT/RIGHT, 4=TURBO";
			}
			if (player.resource.mediaLoadFinished()) {
				if (!helpLine2.isEmpty()) {
					helpLine2 += ". ";
				}
				helpLine2 += mode == Mode.KEYBOARD_24K || mode == Mode.KEYBOARD_24K_DOUBLE
						? "PRESS C1 TO PLAY" : "PRESS 1KEY TO PLAY";
			}
			sprite.draw(legacyFont, helpLine1, x, y - ySpacing * 12 - 12, Color.ORANGE);
			sprite.draw(legacyFont, helpLine2, x, y - ySpacing * 13 - 12, Color.ORANGE);

			String[] judge = {"PGREAT :", "GREAT  :", "GOOD   :", "BAD    :", "POOR   :", "KPOOR  :"};
			for (int i = 0; i < judge.length; i++) {
				sprite.draw(legacyFont,
						String.format("%s %d %d %d", judge[i], player.getJudgeCount(i, true) + player.getJudgeCount(i, false),
								player.getJudgeCount(i, true), player.getJudgeCount(i, false)),
						x + 250, y - (i * ySpacing), Color.WHITE);
			}
		}

		PracticeConfiguration.PracticeProperty property = practice.getPracticeProperty();
		graphs[property.graphtype].draw(sprite, time, player,
				new Rectangle(region.x, region.y, region.width, region.height / 4), property.starttime,
				property.endtime, property.freq / 100f);
	}

	@Override
	public void dispose() {
		if (legacyFont != null) {
			legacyFont.dispose();
			legacyFont = null;
		}
		for (SkinNoteDistributionGraph graph : graphs) {
			graph.dispose();
		}
		legacyResourcesInitialized = false;
	}
}
