package bms.player.lunaticrave2;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import bms.player.lunaticrave2.ir.*;

/**
 * LR2IRのスコアリストリーダー
 * 
 * @author ununique
 */
public class IRScoreListReader {

	/**
	 * XML形式のスコアリスト取得先
	 */
	private static final String PLAYER_XML_URL = "http://www.dream-pro.info/~lavalse/LR2IR/2/getplayerxml.cgi?id=";

	/**
	 * constructor
	 */
	public IRScoreListReader() {

	}

	/**
	 * IRサーバが返すスコアリストXMLからスコアデータを取得
	 * 
	 * @param scorelistUrl
	 *            スコアリスト取得先URL
	 * @return スコア一覧
	 */
	public final List<IRScoreData> getScores(final String irid) {
		List<IRScoreData> scores;
		scores = null;
		try {
			Lr2Ir ir = this.getIRScoreXMLData(irid);
			scores = new ArrayList<IRScoreData>();
			for (Score s : ir.getScorelist().getScore()) {
				scores.add(new IRScoreData(s.getHash(), "", s.getClear()
						.intValue(), s.getNotes().intValue(), s.getCombo()
						.intValue(), s.getPg().intValue(),
						s.getGr().intValue(), s.getGd().intValue(), s.getBd()
								.intValue(), s.getPr().intValue(), s.getMinbp()
								.intValue(), s.getOption().intValue(), s
								.getLastupdate().intValue()));
			}
		} catch (Exception e) {
			e.printStackTrace();
			Logger.getGlobal().severe(
					"playlog更新中の例外: " + e.getClass().getName() + " - "
							+ e.getMessage());
		}
		return scores;
	}

	/**
	 * LR2IRのxml形式のスコアリストを取得し、オブジェクトに変換して返す
	 * 
	 * @param irid
	 *            取得するプレイヤーのLR2ID
	 * @return スコアリストのオブジェクトモデル
	 * @throws IOException
	 * @throws JAXBException
	 */
	private Lr2Ir getIRScoreXMLData(String irid) throws IOException,
			JAXBException {
		URL url = new URL(PLAYER_XML_URL + irid);
		Object content = url.getContent();
		if (content instanceof InputStream) {
			LineIterator li = IOUtils.lineIterator((InputStream) content,
					"MS932");
			StringBuilder sb = new StringBuilder();
			// そのままではXMLパーサにかけられないので細工する
			while (li.hasNext()) {
				String line = li.nextLine();
				if (line.startsWith("#")) {
					line = line.substring(1);
				}
				if (line.contains("<scorelist>")) {
					sb.append("<lr2ir>");
				}
				sb.append(line);
			}
			sb.append("</lr2ir>");

			JAXBContext jc = JAXBContext.newInstance("bms.player.lunaticrave2.ir");
			Unmarshaller u = jc.createUnmarshaller();
			return (Lr2Ir) u.unmarshal(new ByteArrayInputStream(sb.toString()
					.getBytes()));
		}
		throw new IOException("InputStream取得失敗 : "
				+ content.getClass().getName());
	}
}
