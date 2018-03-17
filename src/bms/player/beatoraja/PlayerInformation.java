package bms.player.beatoraja;

import lombok.Data;

/**
 * プレイヤーの情報
 * 
 * @author exch
 */
@Data
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
	
}
