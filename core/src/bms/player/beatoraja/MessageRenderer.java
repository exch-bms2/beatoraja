package bms.player.beatoraja;

import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

import bms.player.beatoraja.select.MusicSelector;

/**
 * メッセージ描画用クラス。
 *
 * @author exch
 */
public class MessageRenderer implements Disposable  {

	private FreeTypeFontGenerator generator;
	private final Array<Message> messages = new Array<Message>();

	public MessageRenderer() {
		try {
			generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));				
		} catch (GdxRuntimeException e) {
			Logger.getGlobal().severe("Message Font読み込み失敗");
		}
	}

	public void render(MainState state, SpriteBatch sprite, int x, int y) {
		for(int i = messages.size - 1, dy = 0;i >= 0;i--) {
			final Message message = messages.get(i);

			if(message.time < System.currentTimeMillis()) {
				message.dispose();
				messages.removeIndex(i);
			} else {
				message.draw(state, sprite, x, y - dy);
				dy+=24;
			}
		}
	}

	public Message addMessage(String text, Color color, int type) {
		return addMessage(text, 24 * 60 * 60 * 1000 , color, type);
	}

	public Message addMessage(String text, int time, Color color, int type) {
		Message message = new Message(text, time, color, type);
		if(generator != null) {
			Gdx.app.postRunnable(() -> {
				message.init(generator);
				messages.add(message);
			});
		}
		return message;
	}

	@Override
	public void dispose() {
		generator.dispose();
	}
	
	/**
	 * MessageRendererで描画されるメッセージ
	 *
	 * @author exch
	 */
	public static class Message implements Disposable {

		private BitmapFont font;
		private long time;
		private String text;
		private final Color color;
		private final int type;

		public Message(String text, long time, Color color, int type) {
			this.time = time + System.currentTimeMillis();
			this.text = text;
			this.color = color;
			this.type = type;
		}

		public void init(FreeTypeFontGenerator generator) {
			FreeTypeFontParameter parameter = new FreeTypeFontParameter();
			parameter.size = 24;
			parameter.characters += text;
			font = generator.generateFont(parameter);
			font.setColor(color);
		}

		public void setText(String text) {
			this.text = text;
		}

		public void stop() {
			time = -1;
		}

		public void draw(MainState state, SpriteBatch sprite, int x, int y) {
			if(type != 1 || state instanceof MusicSelector) {
				font.setColor(color.r, color.g, color.b, MathUtils.sinDeg((System.currentTimeMillis() % 1440) / 4.0f) * 0.3f + 0.7f);
				font.draw(sprite, text, x, y);
			}
		}

		@Override
		public void dispose() {
			font.dispose();
		}
	}
}
