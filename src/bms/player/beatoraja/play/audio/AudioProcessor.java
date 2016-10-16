package bms.player.beatoraja.play.audio;

import bms.model.BMSModel;
import bms.model.Note;

public interface AudioProcessor {

	/**
	 * BMSの音源データを読み込む
	 * @param model
	 * @param filepath
	 */
	public void setModel(BMSModel model);

	/**
	 * 音源の読み込み状況を返す
	 * @return 
	 */
	public float getProgress();

	/**
	 * 指定したNoteの音を鳴らす
	 */
	public void play(Note n);
	/**
	 * 指定したNoteの音を止める。nullの場合は再生されている音を全て止める
	 */
	public void stop(Note n);

	/**
	 * リソースを開放する
	 */
	public void dispose();	
}
