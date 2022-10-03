package bms.player.beatoraja.audio;

import java.io.*;
import org.jflac.metadata.StreamInfo;
import org.jflac.PCMProcessor;
import org.jflac.util.ByteData;

public class FlacProcessor implements PCMProcessor {
	private OutputStream output;
	/**
	* 初期化
	*
	* @param out 出力先のストリーム
	*/
	public FlacProcessor(OutputStream out){
		output = out;
	}
	/**
	* メタ情報の処理
	*
	* @param StreamInfoブロック
	*/
	public void processStreamInfo(StreamInfo info) {}

	/**
	* PCMデータの処理
	*
	* @param 複号されたデータ
	*/
	public void processPCM(ByteData pcm) {
	try {
			output.write(pcm.getData(), 0, pcm.getLen());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}