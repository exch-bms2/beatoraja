package bms.player.beatoraja.ir;

import java.util.HashMap;
import java.util.Map;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.player.beatoraja.song.SongData;

/**
 * IR用譜面データ
 * 
 * @author exch
 */
public class IRChartData {

	/**
	 * 譜面のMD5
	 */
	public final String md5;
	/**
	 * 譜面のSHA256
	 */
	public final String sha256;
	/**
	 * 譜面タイトル
	 */
	public final String title;
	/**
	 * 譜面サブタイトル
	 */
	public final String subtitle;
	/**
	 * 譜面ジャンル
	 */
	public final String genre;
	/**
	 * 譜面アーティスト名
	 */
	public final String artist;
	/**
	 * 譜面サブアーティスト名
	 */
	public final String subartist;
	/**
	 * 楽曲のダウンロードURL
	 */
	public final String url;
	/**
	 * 差分譜面のダウンロードURL
	 */
	public final String appendurl;
	/**
	 * レベル表記
	 */
	public final int level;
	/**
	 * TOTAL値
	 */
	public final int total;
	/**
	 * モード
	 */
	public final Mode mode;
	/**
	 * LN TYPE(-1: 未指定, 0: LN, 1: CN, 2: HCN)
	 */
	public final int lntype;
	/**
	 * 判定幅(bmsonのjudgerank表記)
	 */
	public final int judge;
	/**
	 * 最小BPM
	 */
	public final int minbpm;
	/**
	 * 最大BPM
	 */
	public final int maxbpm;
	/**
	 * 総ノーツ数
	 */
	public final int notes;
	/**
	 * LN TYPE未定義ロングノーツが存在するかどうか
	 */
	public final boolean hasUndefinedLN;
	/**
	 * LNが存在するかどうか
	 */
	public final boolean hasLN;
	/**
	 * CNが存在するかどうか
	 */
	public final boolean hasCN;
	/**
	 * HCNが存在するかどうか
	 */
	public final boolean hasHCN;
	/**
	 * 地雷ノーツが存在するかどうか
	 */
	public final boolean hasMine;
	/**
	 * RANDOM定義が存在するかどうか
	 */
	public final boolean hasRandom;
	/**
	 * ストップシーケンスが存在するかどうか
	 */
	public final boolean hasStop;
	
	public final Map<String, String> values = new HashMap<String, String>();
	
	public IRChartData(SongData song) {
		this(song, song.getBMSModel() != null ? song.getBMSModel().getLntype() : 0);
	}
	
	public IRChartData(SongData song, int lntype) {
		this.title = song.getTitle();
		this.subtitle = song.getSubtitle();
		this.genre = song.getGenre();
		this.artist = song.getArtist();
		this.subartist = song.getSubartist();
		this.md5 = song.getMd5();
		this.sha256 = song.getSha256();
		this.url = song.getUrl();
		this.appendurl = song.getAppendurl();
		this.level = song.getLevel();
		
		final BMSModel model = song.getBMSModel();
		this.total = (int) (model != null ? model.getTotal() : 0);
		this.mode = model != null ? model.getMode() : null;
		this.judge = song.getJudge();
		this.minbpm = song.getMinbpm();
		this.maxbpm = song.getMaxbpm();
		this.notes = song.getNotes();
		this.hasUndefinedLN = song.hasUndefinedLongNote();
		this.hasLN = song.hasLongNote();
		this.hasCN = song.hasChargeNote();
		this.hasHCN = song.hasHellChargeNote();
		this.hasMine = song.hasMineNote();
		this.hasRandom = song.hasRandomSequence();
		this.hasStop = song.isBpmstop();
		this.lntype = lntype;

		if(model != null) {
			values.putAll(model.getValues());			
		}
	}
}
