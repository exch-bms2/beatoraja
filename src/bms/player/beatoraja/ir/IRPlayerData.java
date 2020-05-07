package bms.player.beatoraja.ir;

/**
 * IR用プレイヤーデータ
 *
 * @author exch
 */
public class IRPlayerData {

    /**
     * プレイヤーID
     */
    public final String id;
    /**
     * プレイヤー名
     */
    public final String name;
    /**
     * 段位
     */
    public final String rank;

    public IRPlayerData(String id, String name, String rank) {
        this.id = id;
        this.name = name;
        this.rank = rank;
    }
}
