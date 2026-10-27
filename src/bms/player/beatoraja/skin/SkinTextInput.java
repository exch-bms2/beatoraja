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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener.FocusEvent;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.StretchViewport;

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
	private boolean commitRequested;

	void focus(MainState state, SkinText target) {
		if (target.getWriter() == null || committing) {
			return;
		}
		if (this.target == target && this.state == state && textField != null && textField.isVisible()
				&& state.getStage() != null && state.getStage().getKeyboardFocus() == textField) {
			updateBounds(target);
			return;
		}
		commit();
		this.state = state;
		this.target = target;
		commitRequested = false;

		Stage stage = ensureStage(state);
		ensureTextField(state, stage, Math.max(1, Math.round(target.region.height)));
		updateBounds(target);
		style.fontColor = target.color;
		textField.setText(target.getCurrentText());
		textField.setVisible(true);
		stage.setKeyboardFocus(textField);
		textField.setCursorPosition(textField.getText().length());
		textField.getOnscreenKeyboard().show(textField);
		state.main.getInputProcessor().getKeyBoardInputProcesseor().setTextInputMode(true);
	}

	void commit() {
		if (target == null || textField == null || committing) {
			return;
		}
		committing = true;
		commitRequested = false;
		SkinText current = target;
		MainState owner = state;
		TextField field = textField;
		try {
			current.getWriter().set(owner, field.getText());
			current.setText(field.getText());
		} finally {
			target = null;
			try {
				field.setVisible(false);
				field.getOnscreenKeyboard().close();
				Stage stage = owner != null ? owner.getStage() : null;
				if (stage != null && stage.getKeyboardFocus() == field) stage.setKeyboardFocus(null);
			} finally {
				try {
					if (owner != null) owner.main.getInputProcessor().getKeyBoardInputProcesseor().setTextInputMode(false);
				} finally { committing = false; }
			}
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
		try { commit(); }
		finally { disposeResources(); }
	}

	private void disposeResources() {
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
			stage = new Stage(new StretchViewport(resolution.width, resolution.height));
			state.setStage(stage);
		}
		state.updateStageViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.input.setInputProcessor(new InputMultiplexer(state.main.getInputProcessor().getSkinMouseInput(), stage,
				state.main.getInputProcessor().getKeyBoardInputProcesseor()));
		return stage;
	}

	private void ensureTextField(MainState state, Stage stage, int size) {
		if (generator == null) {
			generator = new FreeTypeFontGenerator(Gdx.files.internal(state.main.getConfig().getSystemfontpath()));
		}
		if (font == null || fontSize != size) {
			FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
			parameter.size = size;
			parameter.incremental = true;
			BitmapFont replacement = generator.generateFont(parameter);
			if (font != null) font.dispose();
			font = replacement;
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
			textField = new TextField("", style) {
				@Override public void act(float delta) {
					super.act(delta);
					// Drain the complete input batch before submitting, including text delivered after Enter.
					if (commitRequested) commit();
				}
				@Override protected InputListener createInputListener() {
					return new TextFieldClickListener() {
						@Override public boolean keyDown(InputEvent event, int keycode) {
							try { return super.keyDown(event, keycode); }
							catch (SkinTextClipboard.WriteFailure e) { return true; }
						}
						@Override public boolean keyTyped(InputEvent event, char character) {
							int position = getCursorPosition();
							String value = getText();
							if (getSelection().isEmpty()) {
								if (character == '\b' && position >= 2
										&& Character.isSurrogatePair(value.charAt(position - 2), value.charAt(position - 1)))
									setSelection(position - 2, position);
								else if (character == 127 && position + 1 < value.length()
										&& Character.isSurrogatePair(value.charAt(position), value.charAt(position + 1)))
									setSelection(position, position + 2);
							}
							return super.keyTyped(event, character);
						}
					};
				}
			};
			textField.setOnlyFontChars(false);
			textField.setClipboard(new SkinTextClipboard(() -> Gdx.app.getClipboard()));
			textField.setFocusTraversal(false);
			textField.setTextFieldListener(new TextFieldListener() {
				@Override
				public void keyTyped(TextField textField, char key) {
					if (key == '\n' || key == '\r') {
						commitRequested = true;
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
		textField.setStyle(style);
	}

	private Texture createTexture(int width, int height, Color color, boolean cursor) {
		Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
		pixmap.setColor(color);
		pixmap.fill();
		Texture texture;
		try { texture = new Texture(pixmap); }
		finally { pixmap.dispose(); }
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
