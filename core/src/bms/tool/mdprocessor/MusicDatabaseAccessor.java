package bms.tool.mdprocessor;

/**
 * 楽曲データベースのアクセスインターフェイス
 * 
 * @author exch
 */
public interface MusicDatabaseAccessor {
	/**
	 * SHA256で指定した楽曲のパスを取得する
	 * 
	 * @param md5 楽曲のmd5
	 * @return 楽曲パス
	 */
	public String[] getMusicPaths(String[] md5);
}
