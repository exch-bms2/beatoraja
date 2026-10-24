package bms.player.beatoraja.input;

/** Compatibility name for callers of the original keyboard-only queue. */
public final class KeyboardJudgeQueue extends JudgeEventQueue {
    public KeyboardJudgeQueue() { super(); }
    KeyboardJudgeQueue(int capacity) { super(capacity); }
}
