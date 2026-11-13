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
import bms.player.beatoraja.skin.SkinFontSource;
import bms.player.beatoraja.skin.SystemFontFactory;

/**
 * メッセージ描画用クラス。
 *
 * @author exch
 */
public class MessageRenderer implements Disposable {

	private final Array<Message> messages = new Array<Message>();
	/**
	 * メッセージのフォントが借りる供給元。描画側が一つだけ持ち、メッセージごとにフォントファイルを読み直さない。
	 * The font source message fonts borrow; the renderer owns the only one, so a message never re-reads the font file.
	 */
	private SkinFontSource source;
	private boolean disposed;

	public MessageRenderer(String fontpath) {
		try {
			source = new SkinFontSource(fontpath, new String[0]);
			if (!source.isAvailable()) {
				source.dispose();
				source = null;
			}
		} catch (GdxRuntimeException e) {
			source = null;
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
		if (source != null) {
			Gdx.app.postRunnable(() -> {
				// 予約した処理が解放後に走っても、解放済みの供給元でフォントを作らない。
				// A runnable that runs after dispose never builds a font on the released source.
				if (disposed) {
					return;
				}
				message.init(source);
				messages.add(message);
			});
		}
		return message;
	}

	@Override
	public void dispose() {
		disposed = true;
		RuntimeException failure = null;
		// 一つの解放に失敗しても残りのメッセージと供給元を解放し、失敗は最後にまとめて投げる。
		// A failed release never skips the remaining messages or the source; failures are thrown together at the end.
		for (Message message : messages) {
			try {
				message.dispose();
			} catch (RuntimeException cause) {
				if (failure == null) failure = cause;
				else failure.addSuppressed(cause);
			}
		}
		messages.clear();
		if (source != null) {
			try {
				source.dispose();
			} catch (RuntimeException cause) {
				if (failure == null) failure = cause;
				else failure.addSuppressed(cause);
			}
			source = null;
		}
		if (failure != null) {
			throw failure;
		}
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

		/** 描画側の供給元を借りてフォントを作る。フォントとページはこのメッセージが持つ。 / Builds the font on the renderer's source; the font and its pages belong to this message. */
		public void init(SkinFontSource source) {
			FreeTypeFontParameter parameter = new FreeTypeFontParameter();
			parameter.size = 24;
			parameter.characters = text != null ? text : "";
			font = SystemFontFactory.createFont(source, parameter);
			if (font != null) {
				font.setColor(color);
			}
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
			if (font != null && (type != 1 || state instanceof MusicSelector)) {
				font.setColor(color.r, color.g, color.b, MathUtils.sinDeg((System.currentTimeMillis() % 1440) / 4.0f) * 0.3f + 0.7f);
				font.draw(sprite, text, x, y);
			}
		}

		@Override
		public void dispose() {
			if (font != null) {
				font.dispose();
				font = null;
			}
		}
	}
}
