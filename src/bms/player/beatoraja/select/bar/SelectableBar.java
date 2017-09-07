package bms.player.beatoraja.select.bar;

/**
 * Created by exch on 2017/09/02.
 */
public abstract class SelectableBar extends Bar {

    /**
     * リプレイデータが存在するか
     */
    private boolean[] existsReplay = new boolean[0];

    public boolean existsReplayData() {
        for (boolean b : existsReplay) {
            if (b) {
                return b;
            }
        }
        return false;
    }

    public boolean[] getExistsReplayData() {
        return existsReplay;
    }

    public void setExistsReplayData(boolean[] existsReplay) {
        this.existsReplay = existsReplay;
    }

}
