package bms.player.beatoraja.play.bga;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public interface MovieProcessor {

	public abstract void create(String filepath);

	/**
	 * 動画のフレームを取得する
	 * @return 動画のフレーム
	 */
	public abstract Texture getFrame();

	/**
	 * 動画の再生を開始する
	 * @param loop ループ再生する場合はtrue
	 */
	public abstract void play(boolean loop);

	/**
	 * 動作の再生を停止する
	 */
	public abstract void stop();

	/**
	 * リソースを解放する
 	 */
	public abstract void dispose();
}
