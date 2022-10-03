package bms.player.beatoraja;

/**
 * プレイヤーの情報
 * 
 * @author exch
 */
public class PlayerInformation {

	/**
	 * プレイヤーID
	 */
	private String id;
	/**
	 * プレイヤー名
	 */
	private String name;
	/**
	 * 段位
	 */
	private String rank;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getRank() {
		return rank;
	}
	
	public void setRank(String rank) {
		this.rank = rank;
	}
}
