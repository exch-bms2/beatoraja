package bms.player.beatoraja;

import java.util.Arrays;

import bms.model.Mode;
import bms.player.beatoraja.input.BMControllerInputProcessor.BMKeys;



/**
 * �궠�꺍�깉�꺆�꺖�꺀�꺖鼇�若싧츣獰⑴뵪�궚�꺀�궧
 *
 * @author exch
 */
public class ControllerConfig {

    private String name = "";

    private int[] keys;

    private int start;

    private int select;

    /**
     * JKOC Hack (boolean) private variable
     */
    private boolean jkoc_hack = false;

    /**
     * �궋�깏�꺆�궛�궧�궚�꺀�긿�긽�굮�닶�뵪�걲�굥�걢(INFINITAS�궠�꺍�깉�꺆�꺖�꺀�겗�졃�릦true)
     */
    private boolean analogScratch = false;

    public ControllerConfig() {
        this(Mode.BEAT_7K, 0, true);
    }

    public ControllerConfig(Mode mode, int player, boolean enable) {
        this.setKeyAssign(mode, player, enable);
    }

    public ControllerConfig(int[] keys, int start, int select) {
        this.keys = keys;
        this.start = start;
        this.select = select;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public int getStart() {
        return start;
    }

    public int getSelect() {
        return select;
    }
    
    public boolean isKeyAssigned() {
    	return keys != null;
    }
    
    public void setKeys(int[] newKeys) {
    	keys = newKeys;
    }
    
    public void setKey(int i, int newKey) {
    	keys[i] = newKey;
    }
    
    public int[] getKeys() {
    	return keys;
    }
    
    public int getKey(int i) {
    	return keys[i];
    }
    
    public int getKeyLength() {
    	return keys.length;
    }

    public void setKeyAssign(Mode mode, int player, boolean enable) {
        if(player == 0) {
            switch (mode) {
                case BEAT_5K:
                case BEAT_7K:
                case POPN_5K:
                case POPN_9K:
                default:
                    keys = new int[]{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
                            BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN };
                    break;
                case BEAT_10K:
                case BEAT_14K:
                    keys = new int[]{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
                            BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, -1,-1,-1,-1,-1,-1,-1,-1,-1 };
                    break;
                case KEYBOARD_24K:
                    keys = new int[]{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
                            BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN };
                    keys = Arrays.copyOf(keys, 26);
                    break;
                case KEYBOARD_24K_DOUBLE:
                    keys = new int[]{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
                            BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN };
                    keys = Arrays.copyOf(keys, 52);
                    break;
            }
        } else {
            switch (mode) {
                case BEAT_5K:
                case BEAT_7K:
                case POPN_5K:
                case POPN_9K:
                default:
                    keys = new int[9];
                    Arrays.fill(keys, -1);
                    break;
                case BEAT_10K:
                case BEAT_14K:
                    keys = new int[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
                            BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN};
                    break;
                case KEYBOARD_24K:
                    keys = new int[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
                            BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN};
                    keys = Arrays.copyOf(keys, 26);
                    break;
                case KEYBOARD_24K_DOUBLE:
                    keys = new int[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
                            BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN};
                    keys = Arrays.copyOf(keys, 52);
                    break;
            }
        }
        if(!enable) {
            Arrays.fill(keys, -1);
        }
        start = BMKeys.BUTTON_9;
        select = BMKeys.BUTTON_10;
    }

    public void setKeyAssign(int[] keys) {
        this.keys = keys;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setSelect(int select) {
        this.select = select;
    }

    public boolean getJKOC()  {
        return jkoc_hack;
    }

    public void setJKOC(boolean jkoc)  {
        this.jkoc_hack = jkoc;
    }

    public boolean isAnalogScratch() {
        return analogScratch;
    }

    public void setAnalogScratch(boolean analogScratch) {
        this.analogScratch = analogScratch;
    }
}
