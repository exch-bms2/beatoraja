package bms.player.beatoraja.select;

import bms.player.beatoraja.Resolution;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * 楽曲検索用テキストフィールド
 *
 * @author exch
 */
public class SearchTextField extends Stage {
	/**
	 * フォント生成用クラス
	 */
	private FreeTypeFontGenerator generator;
	/**
	 * フォント
	 */
	private BitmapFont searchfont;

	private TextField search;

	public SearchTextField(MusicSelector selector, Resolution resolution) {
		super(new FitViewport(resolution.width, resolution.height));

		final Rectangle r = ((MusicSelectSkin) selector.getSkin()).getSearchTextRegion();

		generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = (int) r.height;
		searchfont = generator.generateFont(parameter);

		final TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle(searchfont, // BitmapFont
				Color.WHITE, // font color
				new TextureRegionDrawable(new TextureRegion(new Texture("skin/default/system.png"), 0, 8, 8, 8)), // cusor
				new TextureRegionDrawable(new TextureRegion(new Texture("skin/default/system.png"), 0, 8, 2, 8)), // selectoin
				new TextureRegionDrawable(new TextureRegion(new Texture("skin/default/system.png"), 0, 8, 1, 8))); // background
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
							selector.getBarRender().addSearch(swb);
							selector.getBarRender().updateBar(null);
							textField.setText("");
							textField.setMessageText(count + " song(s) found");
							textFieldStyle.messageFontColor = Color.valueOf("00c0c0");
						} else {
							textField.setText("");
							textField.setMessageText("no song found");
							textFieldStyle.messageFontColor = Color.DARK_GRAY;
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
		addActor(search);

		search.setVisible(true);
		search.addListener(new EventListener() {
			@Override
			public boolean handle(Event e) {
				if (e.isHandled()) {
					selector.getMainController().getInputProcessor().getKeyBoardInputProcesseor()
							.setEnable(getKeyboardFocus() == null);
				}
				return false;
			}
		});
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
}
