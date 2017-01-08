package bms.player.beatoraja.audio;

import java.nio.file.Path;

import bms.model.BMSModel;
import bms.model.Note;

import com.badlogic.gdx.utils.Disposable;

public interface AudioDriver extends Disposable {

	public void play(String path, boolean loop);
	
	public void stop(String path);	
	
	/**
	 * BMSの音源データを読み込む
	 * @param model
	 * @param filepath
	 */
	public void setModel(BMSModel model);

	public void abort();
	/**
	 * 音源の読み込み状況を返す
	 * @return 
	 */
	public float getProgress();

	/**
	 * 指定したNoteの音を鳴らす
	 */
	public void play(Note n, float volume);
	/**
	 * 指定したNoteの音を止める。nullの場合は再生されている音を全て止める
	 */
	public void stop(Note n);
}
