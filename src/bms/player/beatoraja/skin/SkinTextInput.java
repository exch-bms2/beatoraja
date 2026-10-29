package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.input.ImeBridge;
import bms.player.beatoraja.input.TextInputSession;
import bms.player.beatoraja.input.MacOsImeBridge;
import bms.player.beatoraja.input.WindowsImeBridge;

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
	private ImeBridge bridge;
	private TextInputSession session;
	private Runnable pendingFocus;
	private Runnable publishImeCaret;

	SkinTextInput() { this(System.getProperty("os.name", "").startsWith("Windows") ? WindowsImeBridge.create() : MacOsImeBridge.create()); }
	SkinTextInput(ImeBridge bridge) { this.bridge = java.util.Objects.requireNonNull(bridge); }

	void focus(MainState state, SkinText target) {
		if (target.getWriter() == null || committing) {
			return;
		}
		if (this.target == target && this.state == state && textField != null && textField.isVisible()
				&& state.getStage() != null && state.getStage().getKeyboardFocus() == textField) {
			updateBounds(target);
			return;
		}
		if (this.target != null) {
			pendingFocus = () -> focus(state, target);
			commit();
			return;
		}
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
		try {
			session = new TextInputSession(bridge, editorState());
		} catch (RuntimeException | Error e) {
			if (bridge instanceof MacOsImeBridge || bridge instanceof WindowsImeBridge) {
				java.util.logging.Logger.getGlobal().warning("Native IME attachment failed; using GLFW text input: " + e);
				bridge = ImeBridge.NONE;
				session = new TextInputSession(bridge, editorState());
			} else {
				this.target = null;
				textField.setVisible(false);
				stage.setKeyboardFocus(null);
				throw e;
			}
		}
		textField.getOnscreenKeyboard().show(textField);
		state.main.getInputProcessor().getKeyBoardInputProcesseor().setTextInputMode(true, session.usesIme());
		if (session.usesIme() && publishImeCaret != null) publishImeCaret.run();
	}

	void commit() {
		if (target == null || textField == null || committing) return;
		if (session != null) {
			try {
				syncEditor();
				session.requestFinish();
				session.drain();
				if (!session.isFinished()) return;
			} catch (RuntimeException | Error e) {
				pendingFocus = null;
				try { completeCommit(); } catch (RuntimeException | Error cleanup) { e.addSuppressed(cleanup); }
				throw e;
			}
		}
		completeCommit();
	}

	private void completeCommit() {
		if (target == null || textField == null || committing) {
			return;
		}
		committing = true;
		commitRequested = false;
		SkinText current = target;
		MainState owner = state;
		TextField field = textField;
		Runnable next = pendingFocus;
		pendingFocus = null;
		String value = session != null && session.usesIme() ? session.state().text() : field.getText();
		try {
			current.getWriter().set(owner, value);
			current.setText(value);
		} finally {
			target = null;
			try {
				try { if (session != null) session.close(); }
				finally {
					session = null;
					field.setVisible(false);
					field.getOnscreenKeyboard().close();
					Stage stage = owner != null ? owner.getStage() : null;
					if (stage != null && stage.getKeyboardFocus() == field) stage.setKeyboardFocus(null);
				}
			} finally {
				try {
					if (owner != null) owner.main.getInputProcessor().getKeyBoardInputProcesseor().setTextInputMode(false);
				} finally { committing = false; }
			}
		}
		if (next != null) next.run();
	}

	boolean isFinishing() { return session != null && session.isFinishing() && !session.isFinished(); }

	private TextInputSession.EditorState editorState() {
		String value = textField.getText();
		int cursor = textBoundary(value, textField.getCursorPosition());
		return new TextInputSession.EditorState(value,
				textField.getSelection().isEmpty() ? cursor : textBoundary(value, textField.getSelectionStart()), cursor);
	}

	private static int textBoundary(String text, int offset) {
		return offset > 0 && offset < text.length() && Character.isSurrogatePair(text.charAt(offset - 1), text.charAt(offset))
				? offset - 1 : offset;
	}

	private void syncEditor() {
		if (session != null && session.usesIme() && !session.isComposing() && !session.isFinishing()) session.updateEditor(editorState());
	}

	private void updateSession() {
		if (session == null || !session.usesIme() || target == null) return;
		syncEditor();
		bridge.poll(session.id());
		session.drain();
		if (session.usesIme()) {
			if (session.isFinished()) { completeCommit(); return; }
			String display = session.displayText();
			if (!textField.getText().equals(display)) textField.setText(display);
			var value = session.state();
			if (!session.isComposing() && value.anchor() != value.cursor()) textField.setSelection(value.anchor(), value.cursor());
			else textField.setCursorPosition(session.displayCursor());
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
		pendingFocus = null;
		try {
			// Screen disposal must not wait for a future native finish acknowledgement.
			if (session != null && session.usesIme()) {
				try { session.drain(); } finally { completeCommit(); }
			}
			else commit();
		}
		finally { disposeResources(); }
	}

	private void disposeResources() {
		if (textField != null) {
			textField.remove();
			textField = null;
			publishImeCaret = null;
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
				private final com.badlogic.gdx.math.Vector2 caretBottom = new com.badlogic.gdx.math.Vector2();
				private final com.badlogic.gdx.math.Vector2 caretTop = new com.badlogic.gdx.math.Vector2();
				private float caretX, caretY;
				{ publishImeCaret = this::publishCaret; }
				private void publishCaret() {
					if (session == null || !session.usesIme() || getStage() == null) return;
					calculateOffsets();
					BitmapFont font = getStyle().font;
					var background = getBackgroundDrawable();
					caretX = textOffset + glyphPositions.get(cursor) - glyphPositions.get(visibleTextStart)
							+ fontOffset + font.getData().cursorX + (background != null ? background.getLeftWidth() : 0);
					caretY = getTextY(font, background) - textHeight - font.getDescent();
					localToStageCoordinates(caretBottom.set(caretX, caretY));
					localToStageCoordinates(caretTop.set(caretX + 2, caretY + textHeight));
					getStage().stageToScreenCoordinates(caretBottom);
					getStage().stageToScreenCoordinates(caretTop);
					bridge.caret(session.id(), Math.min(caretBottom.x, caretTop.x), Math.min(caretBottom.y, caretTop.y),
							Math.abs(caretTop.x - caretBottom.x), Math.abs(caretTop.y - caretBottom.y),
							Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				}
				@Override public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float alpha) {
					super.draw(batch, alpha);
					if (session == null || !session.usesIme() || getStage() == null) return;
					publishCaret();
					if (session.isComposing()) {
						int from = Math.min(glyphPositions.size - 1, session.preeditStart());
						int to = Math.min(glyphPositions.size - 1, from + session.preedit().length());
						float left = Math.max(0, caretX + glyphPositions.get(from) - glyphPositions.get(cursor));
						float right = Math.min(getWidth(), caretX + glyphPositions.get(to) - glyphPositions.get(cursor));
						if (right > left) getStyle().cursor.draw(batch, getX() + left, getY() + caretY, right - left, 1);
					}
				}
				@Override public void act(float delta) {
					super.act(delta);
					updateSession();
					// Drain the complete input batch before submitting, including text delivered after Enter.
					if (commitRequested) commit();
				}
				@Override protected InputListener createInputListener() {
					return new TextFieldClickListener() {
						@Override public boolean keyDown(InputEvent event, int keycode) {
							if (session != null && session.usesIme()) return true;
							try { return super.keyDown(event, keycode); }
							catch (SkinTextClipboard.WriteFailure e) { return true; }
						}
						@Override public boolean keyTyped(InputEvent event, char character) {
							if (session != null && session.usesIme()) return true;
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
