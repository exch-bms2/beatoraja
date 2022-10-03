package bms.player.beatoraja.play.bga;

import com.badlogic.gdx.graphics.Texture;


public interface MovieProcessor {

	/**
	 * 動画のフレームを取得する
	 * @return 動画のフレーム
	 */
	public abstract Texture getFrame(long time);

	/**
	 * 動画の再生を開始する
	 * @param loop ループ再生する場合はtrue
	 */
	public abstract void play(long time, boolean loop);

	/**
	 * 動作の再生を停止する
	 */
	public abstract void stop();

	/**
	 * リソースを解放する
 	 */
	public abstract void dispose();	
}
