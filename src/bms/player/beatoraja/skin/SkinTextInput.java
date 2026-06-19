package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener.FocusEvent;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;

class SkinTextInput {

	private MainState state;
	private SkinText target;
	private TextField textField;
	private TextField.TextFieldStyle style;
	private FreeTypeFontGenerator generator;
	private BitmapFont font;
	private int fontSize;
	private Texture cursorTexture;
	private Texture selectionTexture;
	private boolean committing;

	void focus(MainState state, SkinText target) {
		if (target.getWriter() == null) {
			return;
		}
		this.state = state;
		this.target = target;

		Stage stage = ensureStage(state);
		ensureTextField(state, stage, Math.max(1, Math.round(target.region.height)));
		updateBounds(target);
		style.fontColor = target.color;
		textField.setText(target.getCurrentText());
		textField.setVisible(true);
		stage.setKeyboardFocus(textField);
		textField.setCursorPosition(textField.getText().length());
		textField.getOnscreenKeyboard().show(true);
		state.main.getInputProcessor().getKeyBoardInputProcesseor().setTextInputMode(true);
	}

	void commit() {
		if (target == null || textField == null || committing) {
			return;
		}
		committing = true;
		try {
			target.getWriter().set(state, textField.getText());
			target.setText(textField.getText());
			textField.setVisible(false);
			textField.getOnscreenKeyboard().show(false);
			Stage stage = state != null ? state.getStage() : null;
			if (stage != null && stage.getKeyboardFocus() == textField) {
				stage.setKeyboardFocus(null);
			}
			if (state != null) {
				state.main.getInputProcessor().getKeyBoardInputProcesseor().setTextInputMode(false);
			}
			target = null;
		} finally {
			committing = false;
		}
	}

	void commitIfOutside(int x, int y) {
		if (target != null && !getBounds(target).contains(x, y)) {
			commit();
		}
	}

	boolean isFocused(SkinText text) {
		return target == text;
	}

	void dispose() {
		commit();
		if (textField != null) {
			textField.remove();
			textField = null;
		}
		if (font != null) {
			font.dispose();
			font = null;
		}
		if (generator != null) {
			generator.dispose();
			generator = null;
		}
		if (cursorTexture != null) {
			cursorTexture.dispose();
			cursorTexture = null;
		}
		if (selectionTexture != null) {
			selectionTexture.dispose();
			selectionTexture = null;
		}
	}

	private Stage ensureStage(MainState state) {
		Stage stage = state.getStage();
		if (stage == null) {
			var resolution = state.main.getConfig().getResolution();
			stage = new Stage(new FitViewport(resolution.width, resolution.height));
			state.setStage(stage);
		}
		Gdx.input.setInputProcessor(new InputMultiplexer(stage, state.main.getInputProcessor().getKeyBoardInputProcesseor()));
		return stage;
	}

	private void ensureTextField(MainState state, Stage stage, int size) {
		if (generator == null) {
			generator = new FreeTypeFontGenerator(Gdx.files.internal(state.main.getConfig().getSystemfontpath()));
		}
		if (font == null || fontSize != size) {
			if (font != null) {
				font.dispose();
			}
			FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
			parameter.size = size;
			parameter.incremental = true;
			font = generator.generateFont(parameter);
			fontSize = size;
		}
		if (style == null) {
			style = new TextField.TextFieldStyle();
			style.cursor = new TextureRegionDrawable(new TextureRegion(createTexture(2, Math.max(8, size), Color.WHITE, true)));
			style.selection = new TextureRegionDrawable(new TextureRegion(createTexture(2, Math.max(8, size), Color.WHITE, false)));
			style.messageFontColor = Color.GRAY;
		}
		style.font = font;
		style.messageFont = font;
		if (textField == null) {
			textField = new TextField("", style);
			textField.setFocusTraversal(false);
			textField.setTextFieldListener(new TextFieldListener() {
				@Override
				public void keyTyped(TextField textField, char key) {
					if (key == '\n' || key == '\r') {
						commit();
					}
				}
			});
			textField.addListener(new FocusListener() {
				@Override
				public void keyboardFocusChanged(FocusEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor, boolean focused) {
					if (!focused) {
						commit();
					}
				}
			});
			stage.addActor(textField);
		} else if (textField.getStage() != stage) {
			textField.remove();
			stage.addActor(textField);
		}
	}

	private Texture createTexture(int width, int height, Color color, boolean cursor) {
		Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
		pixmap.setColor(color);
		pixmap.fill();
		Texture texture = new Texture(pixmap);
		pixmap.dispose();
		if (cursor) {
			cursorTexture = texture;
		} else {
			selectionTexture = texture;
		}
		return texture;
	}

	private void updateBounds(SkinText text) {
		Rectangle bounds = getBounds(text);
		textField.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
	}

	private Rectangle getBounds(SkinText text) {
		return text.getInputBounds();
	}
}
