package bms.player.beatoraja.input;

import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.PlayModeConfig.MouseScratchConfig;
import com.badlogic.gdx.Gdx;

import java.util.Arrays;

public class MouseScratchInput {
    private final BMSPlayerInputProcessor bmsPlayerInputProcessor;
    private final KeyBoardInputProcesseor keyboardInputProcessor;
    
    private int[] keys;
    private int[] control;

    private boolean[] mouseScratchState = new boolean[2];
    private boolean[] mouseScratchChanged = new boolean[2];
    private static final int MOUSESCRATCH_PLUS = 0;
    private static final int MOUSESCRATCH_MINUS = 1;

    MouseToAnalog mouseToAnalog = null;
    /**
     * マウス皿のアルゴリズム (null=アナログ皿を通用しない)
     */
    MouseScratchAlgorithm mouseScratchAlgorithm = null;
    /**
     * 最後に押されたマウス皿
     */
    private int lastMouseScratch = -1;
    /**
     * キーの最少入力感覚
     */
    private int duration;
    /**
     * マウス皿を利用すてる
     */
    private boolean mouseScratchEnabled = false;
    /**
     * スクラッチ停止閾値 (ms)
     */
    private int mouseScratchTimeThreshold = 150;
    /**
     * スクラッチ距離
     */
    private int mouseScratchDistance = 150;

    public MouseScratchInput(BMSPlayerInputProcessor bmsPlayerInputProcessor, KeyBoardInputProcesseor keyboardInputProcessor, KeyboardConfig config) {
        this.bmsPlayerInputProcessor = bmsPlayerInputProcessor;
        this.keyboardInputProcessor = keyboardInputProcessor;
        this.setConfig(config);
    }

    public void poll(final long presstime) {
        // MOUSEの更新
        if (mouseScratchEnabled) {
            mouseToAnalog.update();
            mouseScratchAlgorithm.update(presstime);

            for (int mouseInput=0; mouseInput<mouseScratchState.length; mouseInput++) {
                final boolean prev = mouseScratchState[mouseInput];
                if (mouseInput == MOUSESCRATCH_PLUS) {
                    mouseScratchState[mouseInput] = mouseScratchAlgorithm.getPositiveScratch();
                } else if (mouseInput == MOUSESCRATCH_MINUS) {
                    mouseScratchState[mouseInput] = mouseScratchAlgorithm.getNegativeScratch();
                } else {
                    mouseScratchState[mouseInput] = false;
                }
                if (prev != mouseScratchState[mouseInput]) {
                    mouseScratchChanged[mouseInput] = true;
                    if (!prev) this.lastMouseScratch = mouseInput;
                }
            }

            for (int i=0; i<keys.length; i++) {
                final int axis = keys[i];
                if (axis >= 0 && mouseScratchChanged[axis]) {
                    this.bmsPlayerInputProcessor.keyChanged(keyboardInputProcessor, presstime, i, mouseScratchState[axis]);
                    mouseScratchChanged[axis] = false;
                }
            }

            if (control[0] >= 0 && mouseScratchChanged[control[0]]) {
                this.bmsPlayerInputProcessor.startChanged(mouseScratchState[control[0]]);
                mouseScratchChanged[control[0]] = false;
            }

            if (control[1] >= 0 && mouseScratchChanged[control[1]]) {
                this.bmsPlayerInputProcessor.setSelectPressed(mouseScratchState[control[1]]);
                mouseScratchChanged[control[1]] = false;
            }

            for (int i=0; i<keys.length; i++) {
                if (keys[i] >= 0) {
                    this.bmsPlayerInputProcessor.setAnalogState(i, true, getMouseAnalogValue(keys[i]));
                }
            }
        }
    }

    public void setConfig(KeyboardConfig config) {
        MouseScratchConfig msconfig = config.getMouseScratchConfig();
        this.keys = msconfig.getKeyAssign().clone();
        this.duration = config.getDuration();
        this.control = new int[] { msconfig.getStart(), msconfig.getSelect() };

        this.mouseScratchEnabled = msconfig.isMouseScratchEnabled();
        this.mouseScratchTimeThreshold = msconfig.getMouseScratchTimeThreshold();
        this.mouseScratchDistance = msconfig.getMouseScratchDistance();
        if (mouseScratchEnabled) {
            this.mouseToAnalog = new MouseToAnalog(mouseScratchDistance);
            switch (msconfig.getMouseScratchMode()) {
                case MouseScratchConfig.MOUSE_SCRATCH_VER_1:
                    this.mouseScratchAlgorithm = new MouseScratchAlgorithmVersion1(mouseScratchTimeThreshold, mouseToAnalog);
                    break;
                case MouseScratchConfig.MOUSE_SCRATCH_VER_2:
                    this.mouseScratchAlgorithm = new MouseScratchAlgorithmVersion2(mouseScratchTimeThreshold, mouseToAnalog);
                    break;
            }
        } else {
            this.mouseToAnalog = null;
            this.mouseScratchAlgorithm = null;
        }
    }

    public void clear() {
        //Arrays.fill(keytime, -duration);
        if (mouseScratchAlgorithm != null) {
            mouseScratchAlgorithm.reset();
        }
        lastMouseScratch = -1;
    }

    private float getMouseAnalogValue(int mouseInput) {
        float value = mouseToAnalog.getAnalogValue();
        return (mouseInput == MOUSESCRATCH_PLUS) ? value : -value;
    }

    public int getLastMouseScratch() {
        return lastMouseScratch;
    }

    public void setLastMouseScratch(int value) {
        this.lastMouseScratch = value;
    }

    public static final class MouseToAnalog {
        private final int scratchDistance;
        private final int tickLength;
        private final int domain;

        private int totalDistanceMoved;

        public static final int TICKS_FOR_SCRATCH = 2;

        public MouseToAnalog(int scratchDistance) {
            this.tickLength = Math.max(1, scratchDistance/TICKS_FOR_SCRATCH);
            this.scratchDistance = scratchDistance;
            this.domain = 256*tickLength;
        }

        public void update() {
            int distanceMoved = (Gdx.input.getX() - Gdx.graphics.getWidth() / 2) + (Gdx.input.getY() - Gdx.graphics.getHeight() / 2);
            Gdx.input.setCursorPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);

            totalDistanceMoved = (totalDistanceMoved+distanceMoved)%domain;
            totalDistanceMoved = (totalDistanceMoved < 0) ? (totalDistanceMoved+domain) : totalDistanceMoved;
        }

        public int getDistanceMoved() {
            return totalDistanceMoved;
        }

        public int getScratchDistance() {
            return scratchDistance;
        }

        public int computeDistanceDiff(int v1, int v2) {
            int v = v2 - v1;
            if (v >= domain/2) return v-domain;
            if (v < -domain/2) return v+domain;
            return v;
        }

        public float getAnalogValue() {
            return (float)(totalDistanceMoved%256)/128 - 1;
        }
    }

    private static abstract class MouseScratchAlgorithm {
        protected long lastpresstime;

        public abstract boolean getPositiveScratch();
        public abstract boolean getNegativeScratch();
        public abstract void update(final long presstime);

        public void reset() {
            lastpresstime = -1;
        }

        protected long getTimeDiff(final long presstime) {
            if (lastpresstime < 0) {
                lastpresstime = presstime;
                return 0;
            }
            long dtime = presstime - lastpresstime;
            lastpresstime = presstime;
            return dtime;
        }
    }

    public static final class MouseScratchAlgorithmVersion1 extends MouseScratchAlgorithm {
        private final MouseToAnalog mouseToAnalog;
        private final int scratchDuration;

        private int prevPosition;
        private int remainingTime;

        private int currentScratch;

        public boolean getPositiveScratch() {
            return currentScratch > 0;
        }

        public boolean getNegativeScratch() {
            return currentScratch < 0;
        }

        public MouseScratchAlgorithmVersion1(int scratchDuration, MouseToAnalog mouseToAnalog) {
            this.scratchDuration = scratchDuration;
            this.mouseToAnalog = mouseToAnalog;
            this.prevPosition = mouseToAnalog.getDistanceMoved();
            reset();
        }

        public void update(final long presstime) {
            long dtime = getTimeDiff(presstime);

            int currPosition = mouseToAnalog.getDistanceMoved();
            int dTicks = mouseToAnalog.computeDistanceDiff(prevPosition, currPosition);
            prevPosition = currPosition;

            if (dTicks > 0) {
                remainingTime = scratchDuration;
                currentScratch = 1;
            } else if (dTicks < 0) {
                remainingTime = scratchDuration;
                currentScratch = -1;
            } else if (remainingTime > 0) {
                remainingTime -= dtime;
            } else {
                currentScratch = 0;
            }
        }
    }

    public static final class MouseScratchAlgorithmVersion2 extends MouseScratchAlgorithm {

        private final MouseToAnalog mouseToAnalog;
        private final int scratchDuration;
        private final int scratchDistance;
        private final int scratchReverseDistance;

        private int currentScratch = 0;

        private int prevPosition;

        private int positiveNoMovementTime = 0;
        private int negativeNoMovementTime = 0;

        private int positiveDistance = 0;
        private int negativeDistance = 0;

        public boolean getPositiveScratch() {
            return currentScratch > 0;
        }

        public boolean getNegativeScratch() {
            return currentScratch < 0;
        }

        public MouseScratchAlgorithmVersion2(int scratchDuration, MouseToAnalog mouseToAnalog) {
            this.scratchDuration = scratchDuration;
            this.mouseToAnalog = mouseToAnalog;
            this.scratchDistance = mouseToAnalog.getScratchDistance();
            this.scratchReverseDistance = this.scratchDistance/3;
            this.prevPosition = mouseToAnalog.getDistanceMoved();
            reset();
        }

        public void update(final long presstime) {
            long dtime = getTimeDiff(presstime);
            int currPosition = mouseToAnalog.getDistanceMoved();
            int distanceDiff = mouseToAnalog.computeDistanceDiff(prevPosition, currPosition);
            prevPosition = currPosition;
            if (positiveDistance == 0) {
                positiveNoMovementTime = 0;
            }
            if (negativeDistance == 0) {
                negativeNoMovementTime = 0;
            }
            positiveDistance = Math.max(0, positiveDistance + distanceDiff);
            negativeDistance = Math.max(0, negativeDistance - distanceDiff);
            positiveNoMovementTime += dtime;
            negativeNoMovementTime += dtime;

            if (positiveDistance > 0) {
                if (currentScratch == -1) {
                    if (positiveDistance >= scratchReverseDistance) {
                        currentScratch = 0;
                        negativeDistance = 0;
                        negativeNoMovementTime = 0;
                    }
                }
                if (positiveDistance > scratchDistance) {
                    currentScratch = 1;
                    positiveNoMovementTime = 0;
                    positiveDistance = scratchDistance;
                }
            } 
            if (negativeDistance > 0) {
                if (currentScratch == 1) {
                    if (negativeDistance >= scratchReverseDistance) {
                        currentScratch = 0;
                        positiveDistance = 0;
                        positiveNoMovementTime = 0;
                    }
                }
                if (negativeDistance > scratchDistance) {
                    currentScratch = -1;
                    negativeNoMovementTime = 0;
                    negativeDistance = scratchDistance;
                }
            }
            if (positiveNoMovementTime >= scratchDuration) {
                positiveDistance = 0;
                if (currentScratch == 1) {
                    currentScratch = 0;
                }
            }   
            if (negativeNoMovementTime >= scratchDuration) {
                negativeDistance = 0;
                if (currentScratch == -1) {
                    currentScratch = 0;
                }
            }
        }
    }
}