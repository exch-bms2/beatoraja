package bms.player.beatoraja.result;

import bms.model.Mode;

import static bms.player.beatoraja.result.ResultKeyProperty.ResultKey.*;

public enum ResultKeyProperty {

    BEAT_5K(new ResultKey[]{OK, OK, OK, OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, null, null}),
    BEAT_7K(new ResultKey[]{OK, OK, OK, OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, null, null}),
    BEAT_10K(new ResultKey[]{OK, OK, OK, OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, null, null, OK, OK, OK, OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, null, null}),
    BEAT_14K(new ResultKey[]{OK, OK, OK, OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, null, null, OK, OK, OK, OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, null, null}),
    POPN_9K(new ResultKey[]{OK, OK, OK, OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, OK, OK}),
    KEYBOARD_24K(new ResultKey[]{OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, OK, OK, OK, OK, OK, OK, OK, OK, OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, OK, OK, OK, OK, OK, OK, OK, OK, null, null}),
    KEYBOARD_24K_DOUBLE(new ResultKey[]{OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, OK, OK, OK, OK, OK, OK, OK, OK, OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, OK, OK, OK, OK, OK, OK, OK, OK, null, null,
            OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, OK, OK, OK, OK, OK, OK, OK, OK, OK, REPLAY_DIFFERENT, CHANGE_GRAPH, REPLAY_SAME, OK, OK, OK, OK, OK, OK, OK, OK, null, null}),
    ;

    private final ResultKey[] assign;

    private ResultKeyProperty(ResultKey[] keys) {
        this.assign = keys;
    }

    public ResultKey getAssign(int index) {
        if(index < 0 || index >= assign.length) {
            return null;
        }
        return assign[index];
    }

    public int getAssignLength() {
        return assign.length;
    }

    public enum ResultKey {
        OK,
        REPLAY_DIFFERENT,
        REPLAY_SAME,
        CHANGE_GRAPH;
    }

    public static ResultKeyProperty get(Mode mode) {
        try {
            return valueOf(mode.name());
        } catch (IllegalArgumentException e) {
            return BEAT_7K;
        }
    }
}
