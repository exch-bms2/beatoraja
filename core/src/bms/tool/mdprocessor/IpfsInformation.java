package bms.tool.mdprocessor;

import java.util.List;

/**
 * IPFS情報を取得するインターフェイス
 * 
 * @author exch
 *
 */
public interface IpfsInformation {
	
	/**
	 * 楽曲のipfsパスを取得する
	 * 
	 * @return 楽曲のipfsパス
	 */
	public String getIpfs();
	
	/**
	 * 楽曲差分のipfsパスを取得する
	 * 
	 * @return 楽曲差分のipfsパス
	 */
	public String getAppendIpfs();
	
	/**
	 * 楽曲タイトルを取得する
	 * 
	 * @return 楽曲タイトル
	 */
	public String getTitle();
	
	/**
	 * 楽曲アーティスト名を取得する
	 * 
	 * @return 楽曲アーティスト名
	 */
	public String getArtist();
	
	/**
	 * 楽曲差分の場合、同梱譜面のmd5を取得する
	 * 
	 * @return 同梱譜面のmd5
	 */
	public List<String> getOrg_md5();

}
