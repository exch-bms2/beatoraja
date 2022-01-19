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

    private boolean[] mouseScratchState = new boolean[4];
    private boolean[] mouseScratchChanged = new boolean[4];
    private static final int MOUSESCRATCH_RIGHT = 0;
    private static final int MOUSESCRATCH_LEFT = 1;
    private static final int MOUSESCRATCH_UP = 2;
    private static final int MOUSESCRATCH_DOWN = 3;

    private MouseToAnalog mouseToAnalog = null;
    /**
     * マウス皿のアルゴリズム (null=アナログ皿を通用しない)
     */
    private final MouseScratchAlgorithm[] mouseScratchAlgorithm = new MouseScratchAlgorithm[2];
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

    public void poll(final long microtime) {
		final long presstime = microtime / 1000;
        // MOUSEの更新
        if (mouseScratchEnabled) {
            mouseToAnalog.update();
            for (int i=0; i<mouseScratchAlgorithm.length; i++) {
                mouseScratchAlgorithm[i].update(presstime);
            }

            for (int mouseInput=0; mouseInput<mouseScratchState.length; mouseInput++) {
                final boolean prev = mouseScratchState[mouseInput];
                mouseScratchState[mouseInput] = mouseScratchAlgorithm[mouseInput/2].isScratchActive(mouseInput%2 == 0);
                if (prev != mouseScratchState[mouseInput]) {
                    mouseScratchChanged[mouseInput] = true;
                    if (!prev) this.lastMouseScratch = mouseInput;
                }
            }

            for (int i=0; i<keys.length; i++) {
                final int axis = keys[i];
                if (axis >= 0 && mouseScratchChanged[axis]) {
                    this.bmsPlayerInputProcessor.keyChanged(keyboardInputProcessor, microtime, i, mouseScratchState[axis]);
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
            for (int i=0; i<mouseScratchAlgorithm.length; i++) {
                boolean xAxis = (i == 0);
                switch (msconfig.getMouseScratchMode()) {
                    case MouseScratchConfig.MOUSE_SCRATCH_VER_1:
                        this.mouseScratchAlgorithm[i] = new MouseScratchAlgorithmVersion1(mouseScratchTimeThreshold, mouseToAnalog, xAxis);
                        break;
                    case MouseScratchConfig.MOUSE_SCRATCH_VER_2:
                        this.mouseScratchAlgorithm[i] = new MouseScratchAlgorithmVersion2(mouseScratchTimeThreshold, mouseToAnalog, xAxis);
                        break;
                }
            }
        } else {
            this.mouseToAnalog = null;
            for (int i=0; i<mouseScratchAlgorithm.length; i++) {
                this.mouseScratchAlgorithm[i] = null;
            }
        }
    }

    public void clear() {
        //Arrays.fill(keytime, -duration);
        for (int i=0; i<mouseScratchAlgorithm.length; i++) {
            if (mouseScratchAlgorithm[i] != null) {
                mouseScratchAlgorithm[i].reset();
            }
        }
        lastMouseScratch = -1;
    }

    private float getMouseAnalogValue(int mouseInput) {
        final boolean plus = mouseInput%2 == 0;
        final boolean xAxis = mouseInput < 2;
        final float value = mouseToAnalog.getAnalogValue(xAxis);
        return plus ? value : -value;
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

        private int totalXDistanceMoved;
        private int totalYDistanceMoved;

        public static final int TICKS_FOR_SCRATCH = 2;

        public MouseToAnalog(int scratchDistance) {
            this.tickLength = Math.max(1, scratchDistance/TICKS_FOR_SCRATCH);
            this.scratchDistance = scratchDistance;
            this.domain = 256*tickLength;
        }

        public void update() {
            int xDistanceMoved = Gdx.input.getX() - Gdx.graphics.getWidth() / 2;
            int yDistanceMoved = Gdx.input.getY() - Gdx.graphics.getHeight() / 2;
            Gdx.input.setCursorPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);

            totalXDistanceMoved = ((totalXDistanceMoved+xDistanceMoved)%domain + domain)%domain;
            totalYDistanceMoved = ((totalYDistanceMoved+yDistanceMoved)%domain + domain)%domain;
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

        public int getDistanceMoved(boolean xAxis) {
            return xAxis ? totalXDistanceMoved : totalYDistanceMoved;
        }

        public float getAnalogValue(boolean xAxis) {
            return (float)(getDistanceMoved(xAxis)%256)/128 - 1;
        }
    }

    private static abstract class MouseScratchAlgorithm {
        protected long lastpresstime;

        public abstract boolean isScratchActive(boolean positive);
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
        private final boolean xAxis;

        private int prevPosition;
        private int remainingTime;

        private int currentScratch;

        public boolean isScratchActive(boolean positive) {
            return positive ? currentScratch > 0 : currentScratch < 0;
        }

        public MouseScratchAlgorithmVersion1(int scratchDuration, MouseToAnalog mouseToAnalog, boolean xAxis) {
            this.xAxis = xAxis;
            this.scratchDuration = scratchDuration;
            this.mouseToAnalog = mouseToAnalog;
            this.prevPosition = mouseToAnalog.getDistanceMoved(xAxis);
            reset();
        }

        public void update(final long presstime) {
            long dtime = getTimeDiff(presstime);

            int currPosition = mouseToAnalog.getDistanceMoved(xAxis);
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
        private final boolean xAxis;

        private int currentScratch = 0;

        private int prevPosition;

        private int positiveNoMovementTime = 0;
        private int negativeNoMovementTime = 0;

        private int positiveDistance = 0;
        private int negativeDistance = 0;

        public boolean isScratchActive(boolean positive) {
            return positive ? currentScratch > 0 : currentScratch < 0;
        }

        public MouseScratchAlgorithmVersion2(int scratchDuration, MouseToAnalog mouseToAnalog, boolean xAxis) {
            this.xAxis = xAxis;
            this.scratchDuration = scratchDuration;
            this.mouseToAnalog = mouseToAnalog;
            this.scratchDistance = mouseToAnalog.getScratchDistance();
            this.scratchReverseDistance = this.scratchDistance/3;
            this.prevPosition = mouseToAnalog.getDistanceMoved(xAxis);
            reset();
        }

        public void update(final long presstime) {
            long dtime = getTimeDiff(presstime);
            int currPosition = mouseToAnalog.getDistanceMoved(xAxis);
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