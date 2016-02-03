package bms.player.beatoraja.audio;

import bms.model.BMSModel;

public interface AudioProcessor {

	/**
	 * BMSの音源データを読み込む
	 * @param model
	 * @param filepath
	 */
	public void setModel(BMSModel model, String filepath);

	/**
	 * 音源の読み込み状況を返す
	 * @return 
	 */
	public float getProgress();

	/**
	 * 音を鳴らす
	 * @param id
	 */
	public void play(int id);

	/**
	 * リソースを開放する
	 */
	public void dispose();
	
}
