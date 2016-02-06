package bms.table;

import java.io.Serializable;
import java.util.Map;

import bms.player.lunaticrave2.SongData;

/**
 * 難易度表の要素
 * 
 * @author exch
 */
public class DifficultyTableElement extends BMSTableElement implements
		Serializable {

	/**
	 * 譜面の状態
	 */
	private int state = 0;
	/**
	 * 譜面の状態:新規追加
	 */
	public static final int STATE_NEW = 1;
	/**
	 * 譜面の状態:難易度更新
	 */
	public static final int STATE_UPDATE = 2;
	/**
	 * 譜面の状態:投票中
	 */
	public static final int STATE_VOTE = 3;
	/**
	 * 譜面の状態:おすすめ
	 */
	public static final int STATE_RECOMMEND = 4;
	/**
	 * 譜面の状態:削除
	 */
	public static final int STATE_DELETE = 5;
	/**
	 * 譜面の状態:復活
	 */
	public static final int STATE_REVIVE = 6;

	/**
	 * 譜面評価
	 */
	private int eval = 0;
	/**
	 * レベル表記
	 */
	private String level = "";
	/**
	 * 差分作者名
	 */
	private String diffname = "";
	/**
	 * コメント
	 */
	private String comment = "";
	/**
	 * 譜面情報
	 */
	private String info = "";
	
	private String proposer = "";

	public DifficultyTableElement() {
	}

	public DifficultyTableElement(String did, String title, int bmsid,
			String url1, String url2, String comment, String hash) {
		this.setDifficultyID(did);
		this.setTitle(title);
		this.setBMSID(bmsid);
		this.setURL1(url1);
		this.setURL2(url2);
		this.setComment1(comment);
		this.setHash(hash);
	}
	
	public DifficultyTableElement(SongData song) {
		this.setDifficultyID(String.valueOf(song.getLevel()));
		this.setTitle(song.getTitle());;
		this.setHash(song.getHash());
	}

	public int getState() {
		return state;
	}

	public void setState(int id) {
		state = id;
	}

	public String getDifficultyID() {
		return level;
	}

	public void setDifficultyID(String did) {
		level = did;
	}

	public int getEvaluation() {
		return eval;
	}

	public void setEvaluation(int eval) {
		this.eval = eval;
	}

	public String getURL1sub() {
		return (String) getValues().get("url_pack");
	}

	public void setURL1sub(String url1sub) {
		getValues().put("url_pack", url1sub);
	}

	public String getURL1subname() {
		return (String) getValues().get("name_pack");
	}

	public void setURL1subname(String url1subname) {
		getValues().put("name_pack", url1subname);
	}

	public String getURL2() {
		return (String) getValues().get("url_diff");
	}

	public void setURL2(String url2) {
		getValues().put("url_diff", url2);
	}

	public String getURL2name() {
		return diffname;
	}

	public void setURL2name(String url2name) {
		diffname = url2name;
	}

	public String getComment1() {
		return comment;
	}

	public void setComment1(String comment1) {
		comment = comment1;
	}

	public String getComment2() {
		return info;
	}

	public void setComment2(String comment2) {
		info = comment2;
	}

	public String getProposer() {
		return proposer;
	}

	public void setProposer(String proposer) {
		this.proposer = proposer;
	}
	
	public int getBMSID() {
		int result = 0;
		try {
			result = Integer.parseInt(String.valueOf(getValues().get(
					"lr2_bmsid")));
		} catch (NumberFormatException e) {

		}
		return result;
	}

	public void setBMSID(int bmsid) {
		getValues().put("lr2_bmsid", bmsid);
	}

	@Override
	public void setValues(Map<String, Object> values) {
		super.setValues(values);
		int statevalue = 0;
		try {
			statevalue = Integer.parseInt(String.valueOf(values.get("state")));
		} catch (NumberFormatException e) {

		}
		state = statevalue;

		int evalvalue = 0;
		try {
			evalvalue = Integer.parseInt(String.valueOf(values.get("eval")));
		} catch (NumberFormatException e) {

		}
		eval = evalvalue;
		level= (String) values.get("level");
		diffname = (String) values.get("name_diff");
		comment = (String) values.get("comment");
		info = (String) values.get("tag");
		proposer = (String) values.get("proposer");
	}

	@Override
	public Map<String, Object> getValues() {
		Map<String, Object> result = super.getValues();
		result.put("level", getDifficultyID());
		result.put("eval", getEvaluation());
		result.put("state", getState());
		result.put("name_diff", getURL2name());
		result.put("comment", getComment1());
		result.put("tag", getComment2());
		if(getProposer() != null && getProposer().length() > 0) {
			result.put("proposer", getProposer());			
		} else {
			result.remove("proposer");
		}
		return result;
	}

	// public String makeTable(String d) {
	// String s = "[" + id + ",\"" + d + did + "\",\n";
	// s += "\"" + this.getTitle() + "\",\n";
	// s += "\"" + bmsid + "\",\n";
	// if (this.getURL1() != null && this.getURL1().length() > 0) {
	// s += "\"<a href='" + this.getURL1() + "'>" + this.getURL1name()
	// + "</a>";
	// } else {
	// s += "\"" + this.getURL1name();
	// }
	// if (url1sub != null && url1sub.length() > 0) {
	// s += "<br />(<a href='" + url1sub + "'>" + url1subname
	// + "</a>)\",\n";
	// } else {
	// s += "\",\n";
	// }
	//
	// if (url2 != null && url2.length() > 0) {
	// s += "\"<a href='" + url2 + "'>" + url2name + "</a>\",\n";
	// } else {
	// s += "\"" + url2name + "\",\n";
	// }
	// s += "\"" + comment1 + "\",\n";
	// s += "],\n";
	//
	// return s;
	// }

}
