package bms.player.beatoraja.select;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.input.KeyBoardInputProcesseor.ControlKeys;
import bms.player.beatoraja.select.bar.SearchWordBar;

import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * 楽曲検索用テキストフィールド
 *
 * @author exch
 */
public class SearchTextField extends Stage {
	
	// TOTO ユーザー定義のBitmapFontも使えるようにしたい
	
	/**
	 * フォント生成用クラス
	 */
	private FreeTypeFontGenerator generator;
	/**
	 * フォント
	 */
	private BitmapFont searchfont;

	private TextField search;
	/**
	 * 画面クリック感知用Actor
	 */
	private Group screen;

	public SearchTextField(MusicSelector selector, Resolution resolution) {
		super(new FitViewport(resolution.width, resolution.height));

		final Rectangle r = ((MusicSelectSkin) selector.getSkin()).getSearchTextRegion();

		try {
			generator = new FreeTypeFontGenerator(Gdx.files.internal(selector.main.getConfig().getSystemfontpath()));
			FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
			parameter.size = (int) r.height;
			parameter.incremental = true;
			searchfont = generator.generateFont(parameter);

			final TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle(); // background
			textFieldStyle.font = searchfont;
			textFieldStyle.fontColor = Color.WHITE;
			
			Pixmap cursorp = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
			cursorp.setColor(Color.toIntBits(255, 255, 255, 255));
			cursorp.fill();
			textFieldStyle.cursor = new TextureRegionDrawable(new TextureRegion(new Texture(cursorp)));
			cursorp.dispose();

			Pixmap selectionp = new Pixmap(2, 8, Pixmap.Format.RGBA8888);
			selectionp.setColor(Color.toIntBits(255, 255, 255, 255));
			selectionp.fill();
			textFieldStyle.selection = new TextureRegionDrawable(new TextureRegion(new Texture(selectionp)));
			selectionp.dispose();
			
			textFieldStyle.messageFont = searchfont;
			textFieldStyle.messageFontColor = Color.GRAY;

			search = new TextField("", textFieldStyle);
			search.setMessageText("search song");
			search.setTextFieldListener(new TextFieldListener() {

				public void keyTyped(TextField textField, char key) {
					if (key == '\n' || key == 13) {
						if (textField.getText().length() > 0) {
							SearchWordBar swb = new SearchWordBar(selector, textField.getText());
							int count = swb.getChildren().length;
							if (count > 0) {
								selector.getBarManager().addSearch(swb);
								selector.getBarManager().updateBar(null);
								selector.getBarManager().setSelected(swb);
								textField.setText("");
								textField.setMessageText(count + " song(s) found");
								textFieldStyle.messageFontColor = Color.valueOf("00c0c0");
							} else {
								textField.setText("");
								textField.setMessageText("no song found");
								textFieldStyle.messageFontColor = Color.DARK_GRAY;
								selector.main.getInputProcessor().isControlKeyPressed(ControlKeys.ENTER);
							}
						}
						
						textField.getOnscreenKeyboard().show(false);
						setKeyboardFocus(null);
					}
					if (!searchfont.getData().hasGlyph(key)) {
						FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
						parameter.size = (int) r.height;
						parameter.characters += textField.getText() + key;
						BitmapFont newsearchfont = generator.generateFont(parameter);
						textFieldStyle.font = newsearchfont;
						textFieldStyle.messageFont = newsearchfont;
						searchfont.dispose();
						searchfont = newsearchfont;
						textField.appendText(String.valueOf(key));
					}

				}

			});
			search.setBounds(r.x, r.y, r.width, r.height);
			search.setMaxLength(50);
			search.setFocusTraversal(false);

			search.setVisible(true);
			search.addListener((e) -> {
				if (e.isHandled()) {
					selector.main.getInputProcessor().getKeyBoardInputProcesseor()
							.setTextInputMode(getKeyboardFocus() != null);
				}
				return false;
			});			

			screen = new Group();
			screen.setBounds(0, 0, resolution.width, resolution.height);
			screen.addListener(new ClickListener() {
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					if (getKeyboardFocus() != null && !r.contains(x, y)) {
						unfocus(selector);
					}
					return false;
				}
			});
			screen.addActor(search);
			addActor(screen);
		} catch (GdxRuntimeException e) {
			Logger.getGlobal().warning("Search Text読み込み失敗");
		}
	}

	public void unfocus(MusicSelector selector) {
		if(search != null) {
			search.setText("");
			search.setMessageText("search song");
			search.getStyle().messageFontColor = Color.GRAY;
			search.getOnscreenKeyboard().show(false);			
		}
		setKeyboardFocus(null);
		selector.main.getInputProcessor().getKeyBoardInputProcesseor().setTextInputMode(false);
	}

	public void dispose() {
//		super.dispose();
		if(generator != null) {
			generator.dispose();
			generator = null;
		}
		if(searchfont != null) {
			searchfont.dispose();
			searchfont = null;
		}
	}

	public Rectangle getSearchBounds() {
		return search != null ? new Rectangle(search.getX(), search.getY(), search.getWidth(), search.getHeight()) : null;
	}
}
