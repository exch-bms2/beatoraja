package bms.player.beatoraja.playmode;

import java.util.Arrays;

import bms.model.Mode;
import bms.player.beatoraja.input.BMControllerInputProcessor.BMKeys;



/**
 * �궠�꺍�깉�꺆�꺖��?�꺖鼇�若싧츣獰?��뵪�궚��?�궧
 *
 * @author exch
 */
public class ControllerConfig extends IntConfiguration {

    private String name = "";

    /**
     * JKOC Hack (boolean) private variable
     */
    private boolean jkoc_hack = false;

    /**
     * �궋�깏�꺆�궛�궧�궚��?�긿�긽�굮�닶�뵪�걲�굥�걢(INFINITAS�궠�꺍�깉�꺆�꺖��?�겗�졃�릦true)
     */
    private boolean analogScratch = false;
    

    public ControllerConfig() {
        this(Mode.BEAT_7K, 0, true);
    }

    public ControllerConfig(Mode mode, int player, boolean enable) {
        this.setKeyAssign(mode, player, enable);
    }

    public ControllerConfig(int[] keys, int start, int select) {
    	setKeys(keys);
    	setStart(start);
    	setSelect(select);
    }

    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setKeyAssign(Mode mode, int player, boolean enable) {
        if(player == 0) {
            switch (mode) {
                case BEAT_5K:
                case BEAT_7K:
                case POPN_5K:
                case POPN_9K:
                default:
                    keys = new Integer[]{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
                            BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN };
                    break;
                case BEAT_10K:
                case BEAT_14K:
                    keys = new Integer[]{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
                            BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, -1,-1,-1,-1,-1,-1,-1,-1,-1 };
                    break;
                case KEYBOARD_24K:
                    keys = new Integer[]{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
                            BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN };
                    keys = Arrays.copyOf(keys, 26);
                    break;
                case KEYBOARD_24K_DOUBLE:
                    keys = new Integer[]{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
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
                    keys = new Integer[9];
                    Arrays.fill(keys, -1);
                    break;
                case BEAT_10K:
                case BEAT_14K:
                    keys = new Integer[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
                            BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN};
                    break;
                case KEYBOARD_24K:
                    keys = new Integer[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
                            BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN};
                    keys = Arrays.copyOf(keys, 26);
                    break;
                case KEYBOARD_24K_DOUBLE:
                    keys = new Integer[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
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
}
