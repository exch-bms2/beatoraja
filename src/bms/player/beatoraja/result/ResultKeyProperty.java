package bms.player.beatoraja.result;

import static bms.player.beatoraja.result.ResultKeyProperty.ResultKey.*;

public enum ResultKeyProperty {

    BEAT_5K(new ResultKey[]{OK, OK, OK, OK, REPLAY_DIFFERENT, OK, REPLAY_SAME, null, null}),
    BEAT_7K(new ResultKey[]{OK, OK, OK, OK, REPLAY_DIFFERENT, OK, REPLAY_SAME, null, null}),
    BEAT_10K(new ResultKey[]{OK, OK, OK, OK, REPLAY_DIFFERENT, OK, REPLAY_SAME, null, null, OK, OK, OK, OK, REPLAY_DIFFERENT, OK, REPLAY_SAME, null, null}),
    BEAT_14K(new ResultKey[]{OK, OK, OK, OK, REPLAY_DIFFERENT, OK, REPLAY_SAME, null, null, OK, OK, OK, OK, REPLAY_DIFFERENT, OK, REPLAY_SAME, null, null}),
    POPN_9K(new ResultKey[]{OK, OK, OK, OK, REPLAY_DIFFERENT, OK, REPLAY_SAME, OK, OK}),
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
        REPLAY_SAME;
    }
}
