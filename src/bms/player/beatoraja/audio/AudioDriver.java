package bms.player.beatoraja.audio;

import bms.model.BMSModel;
import bms.model.Note;

import com.badlogic.gdx.utils.Disposable;

/**
 * 各種音源再生インターフェイス
 * 
 * @author exch
 */
public interface AudioDriver extends Disposable {

	/**
	 * 指定したパスの音源を鳴らす
	 * 
	 * @param path
	 *            音源のファイルパス
	 * @param loop
	 *            ループ再生するかどうか
	 */
	public void play(String path, float volume, boolean loop);
	
	public void setVolume(String path, float volume);
	
	/**
	 * 指定したパスの音源がなっている場合は止める
	 * 
	 * @param path
	 *            音源のファイルパス
	 */
	public void stop(String path);

	/**
	 * 指定したパスの音源を開放する
	 * 
	 * @param path
	 *            音源のファイルパス
	 */
	public void dispose(String path);

	/**
	 * BMSの音源データを読み込む
	 * 
	 * @param model
	 *            BMSモデル
	 */
	public void setModel(BMSModel model);

	/**
	 * BMSの音源データ読み込みを中止する
	 */
	public void abort();

	/**
	 * 音源の読み込み状況を返す
	 * 
	 * @return 音源の読み込み状況(0.0 - 1.0)
	 */
	public float getProgress();

	/**
	 * 指定したNoteの音を鳴らす
	 * 
	 * @param n
	 *            Note
	 * @param volume
	 *            ボリューム(0.0 - 1.0)
	 * @param pitch
	 *            ピッチ変化(-12 - 12)
	 */
	public void play(Note n, float volume, int pitch);

	/**
	 * 指定したNoteの音を止める。nullの場合は再生されている音を全て止める
	 * 
	 * @param n
	 *            Note
	 */
	public void stop(Note n);

	/**
	 * 全体のピッチを変更する。可能な場合は再生中の音のピッチも変更する
	 *
	 * @param pitch ピッチ(0.5 - 2.0)
	 */
	public void setGlobalPitch(float pitch);

	/**
	 * 全体のピッチを取得する
	 * @return ピッチ(0.5 - 2.0)
	 */
	public float getGlobalPitch();

	public void disposeOld();
}
