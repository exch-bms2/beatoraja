package bms.player.beatoraja.select.bar;

/**
 * Created by exch on 2017/09/02.
 */
public abstract class DirectoryBar extends Bar {

    private int[] lamps = new int[11];
    private int[] rlamps = new int[11];
    private int[] ranks = new int[0];

    public int[] getLamps() {
        return lamps;
    }

    public void setLamps(int[] lamps) {
        this.lamps = lamps;
    }

    public int[] getRanks() {
        return ranks;
    }

    public void setRanks(int[] ranks) {
        this.ranks = ranks;
    }

    public int getLamp(boolean isPlayer) {
    	final int[] lamps = isPlayer ? this.lamps : rlamps;
        for (int i = 0; i < lamps.length; i++) {
            if (lamps[i] > 0) {
                return i;
            }
        }
        return 0;
    }

    public abstract Bar[] getChildren();

}
