package bms.player.beatoraja.input;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;

/** Exercises the production no-desktop-position branch without claiming the host is a Wayland compositor. */
public final class WaylandInputTestSupport {
    private WaylandInputTestSupport() { }
    public static PollingLwjgl3Input input(Lwjgl3Window window) { return new PollingLwjgl3Input(window,null,false); }
}
