package bms.player.beatoraja.ir;

public class IRAccount {

    /**
     * プレイヤーID
     */
    public final String id;
    /**
     * パスワード
     */
    public final String password;
    /**
     * プレイヤー名
     */
    public final String name;

    public IRAccount(String id, String password, String name) {
        this.id = id;
        this.password = password;
        this.name = name;
    }

}
