package bms.player.beatoraja.external.obs;

import bms.player.beatoraja.MainState.MainStateType;
import java.util.List;

/** A state transition and one OBS action. Field names also accept rian's command configuration. */
public final class ObsControlCommand {
    public static final List<String> ACTIONS = List.of("SetScene", "ShowSource", "HideSource", "StartRecord", "StopRecord");
    private String timing = "PLAY_START";
    private String action = "SetScene";
    private String targetScene = "";
    private String targetSource = "";
    private String transitionScene = "";

    public ObsControlCommand() { }
    public ObsControlCommand(String timing, String action, String scene, String source) {
        this.timing = timing; this.action = action; targetScene = scene; targetSource = source; transitionScene = scene;
    }
    public String getTiming() { return timing; }
    public String getAction() { return action; }
    public String getTargetScene() { return targetScene; }
    public String getTargetSource() { return targetSource; }
    public String getTransitionScene() { return transitionScene; }
    public String getTargetDisplay() { return "SetScene".equals(action) ? transitionScene : targetScene; }
    public boolean isValid() {
        if (!validText(timing) || !validText(action) || !validText(targetScene)
                || !validText(targetSource) || !validText(transitionScene) || !ACTIONS.contains(action)) return false;
        boolean validTiming = timing.equals("MAIN_START") || timing.equals("MAIN_END");
        for (var state : MainStateType.values())
            validTiming |= timing.equals(state.name() + "_START") || timing.equals(state.name() + "_END");
        return validTiming && (!action.equals("SetScene") || !transitionScene.isBlank())
                && (!(action.equals("ShowSource") || action.equals("HideSource")) || (!targetScene.isBlank() && !targetSource.isBlank()));
    }
    private static boolean validText(String value) { return value != null && value.length() <= 1024; }
}
