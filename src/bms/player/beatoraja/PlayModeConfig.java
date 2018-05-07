package bms.player.beatoraja;

import bms.model.Mode;
import bms.player.beatoraja.input.BMControllerInputProcessor.BMKeys;
import bms.player.beatoraja.playmode.*;

import java.util.Arrays;

import com.badlogic.gdx.Input.Keys;

/**
 * �깤�꺃�궎�궠�꺍�깢�궍�궛�귙깴�꺖�깋驪롢겓岳앮똻�걲�굥�겧�걤�ㅳ겓�겇�걚�겍�겘�걪�걾�굢�겓�졏榮띲걲�굥
 *
 * @author exch
 */
public class PlayModeConfig {

    public float hispeed = 1.0f;
    public int duration = 500;
    public float hispeedmargin = 0.25f;
    public float lanecover = 0.2f;
    public boolean enablelanecover = true;
    public float lift = 0.1f;
    public boolean enablelift = false;

    private PlayConfig playconfig = new PlayConfig();
    /**
     * �궘�꺖�깭�꺖�깋鼇�若�
     */
    private KeyboardConfig keyboard = new KeyboardConfig();
    /**
     * �궠�꺍�깉�꺆�꺖�꺀�꺖鼇�若�
     */
    private ControllerConfig[] controller = new ControllerConfig[] { new ControllerConfig() };
    /**
     * MIDI鼇�若�
     */
    private MidiConfig midi = new MidiConfig();

    public PlayModeConfig() {
        this(Mode.BEAT_7K);
    }

    public PlayModeConfig(Mode mode) {
        boolean midi = (mode == Mode.KEYBOARD_24K || mode == Mode.KEYBOARD_24K_DOUBLE);
        this.keyboard = new KeyboardConfig(mode, !midi);
        controller = new ControllerConfig[mode.player];
        for(int i = 0;i < controller.length;i++) {
            controller[i] = new ControllerConfig(mode, i, false);
        }
        this.midi = new MidiConfig(mode, midi);
    }

    public PlayModeConfig(KeyboardConfig keyboard, ControllerConfig[] controllers, MidiConfig midi) {
        this.keyboard = keyboard;
        this.controller = controllers.clone();
        this.midi = midi;
    }

    public KeyboardConfig getKeyboardConfig() {
        return keyboard;
    }

    public void setKeyboardConfig(KeyboardConfig keyboard) {
        this.keyboard = keyboard;
    }

    public ControllerConfig[] getController() {
        return controller;
    }

    public MidiConfig getMidiConfig() {
        return midi;
    }

    public void setController(ControllerConfig[] controllerassign) {
        this.controller = controllerassign;
    }

    public void validate(int keyLength) {
        if(playconfig == null) {
            playconfig = new PlayConfig();
        }
        // �깈�꺖�궭燁삭죱�뵪
        if(duration != 500) {
            playconfig.setDuration(duration);
            duration = 500;
        }
        if(!enablelanecover) {
            playconfig.setEnablelanecover(enablelanecover);
            enablelanecover = true;
        }
        if(enablelift) {
            playconfig.setEnablelift(enablelift);
            enablelift = false;
        }
        if(hispeed != 1.0f) {
            playconfig.setHispeed(hispeed);
            hispeed = 1.0f;
        }
        if(hispeedmargin != 0.25f) {
            playconfig.setHispeedMargin(hispeedmargin);
            hispeedmargin = 0.25f;
        }
        if(lanecover != 0.2f) {
            playconfig.setLanecover(lanecover);
            lanecover = 0.2f;
        }
        if(lift != 0.1f) {
            playconfig.setLift(lift);
            lift = 0.1f;
        }
        
        playconfig.validate();

        if (!keyboard.isKeyAssigned()) {
            keyboard.setKeys (new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                    Keys.CONTROL_LEFT});
        }
        if (keyboard.getKeyLength() != keyLength) {
            keyboard.setKeys (Arrays.copyOf(keyboard.getKeys(), keyLength));
        }

        int index = 0;
        for (ControllerConfig c : controller) {
            if (c.isKeyAssigned()) {
                c.setKeys(new int[] { BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8,
                        BMKeys.BUTTON_2, BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN });
            }
            if (c.getKeyLength() != keyLength) {
                int[] newkeys = new int[keyLength];
                Arrays.fill(newkeys, -1);
                for (int i = 0; i < c.getKeyLength() && index < newkeys.length; i++, index++) {
                    newkeys[index] = c.getKey(i);
                }
                c.setKeys(newkeys);
            }
        }

        if (!midi.isKeyAssigned()) {
            midi.setKeys( new MidiConfig().getKeys());
        }
        if (midi.getKeyLength() != keyLength) {
            midi.setKeys( Arrays.copyOf(midi.getKeys(), keyLength));
        }

        // KB, �궠�꺍�깉�꺆�꺖�꺀�꺖, Midi�겗�릢�깭�궭�꺍�겓�겇�걚�겍�럲餓뽫쉪�눇�릤�굮若잍뼺
        boolean[] exclusive = new boolean[keyboard.getKeyLength()];
        validate0(keyboard.getKeys(),  exclusive);
        for(int i = 0;i < controller.length;i++) {
            validate0(controller[i].getKeys(),  exclusive);
        }

        for(int i = 0;i < midi.getKeys().length;i++) {
            if(exclusive[i]) {
                midi.getKeys()[i] = null;
            }
        }
    }
    
    public int getControllerLength() {
    	return controller.length;
    }

    private void validate0(int[] keys, boolean[] exclusive) {
        for(int i = 0;i < exclusive.length;i++) {
            if(exclusive[i]) {
                keys[i] = -1;
            } else if(keys[i] != -1){
                exclusive[i] = true;
            }
        }
    }

    public PlayConfig getPlayconfig() {
        return playconfig;
    }

    public void setPlayconfig(PlayConfig playconfig) {
        this.playconfig = playconfig;
    }
}
