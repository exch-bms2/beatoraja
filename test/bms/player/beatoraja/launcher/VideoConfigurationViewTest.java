package bms.player.beatoraja.launcher;

import java.util.Locale;
import java.util.ResourceBundle;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainLoader;
import bms.player.beatoraja.StartupMonitor;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

/** Run on the JavaFX thread as part of Lwjgl3LauncherProcessSmokeTest. */
public final class VideoConfigurationViewTest {
    public static Parent verify() throws Exception {
        Parent root = null;
        for (Locale locale : new Locale[]{Locale.ROOT, Locale.JAPAN}) {
            var loader = new FXMLLoader(VideoConfigurationView.class.getResource("VideoConfigurationView.fxml"),
                    ResourceBundle.getBundle("resources.UIResources", locale));
            root = loader.load();
            VideoConfigurationView view = loader.getController();
            var monitor = (ComboBox<?>) loader.getNamespace().get("startupMonitor");
            var resolution = (ComboBox<?>) loader.getNamespace().get("resolution");
            @SuppressWarnings("unchecked")
            var mode = (ComboBox<Config.DisplayMode>) loader.getNamespace().get("displayMode");
            var config = new Config(); view.update(config); view.commit(config);
            var workaround = (CheckBox) loader.getNamespace().get("nvidiaWaylandWorkaround");
            boolean linux = "Linux".equals(System.getProperty("os.name"));
            check(workaround.isVisible() == linux && workaround.isManaged() == linux, "Show workaround only on Linux");
            check(!workaround.isSelected(), "Workaround defaults to off");
            config.setNvidiaWaylandWorkaround(true);
            view.update(config);
            check(workaround.isSelected(), "Workaround displays saved opt-in");
            workaround.setSelected(false);
            view.commit(config);
            check(config.isNvidiaWaylandWorkaround() != linux, "Hidden workaround must not overwrite the saved value");
            if (linux) {
                workaround.setSelected(true);
                view.commit(config);
                check(config.isNvidiaWaylandWorkaround(), "Save the Linux workaround opt-in");
            }
            check(config.getStartupMonitor() == null && monitor.getSelectionModel().getSelectedIndex() == 0,
                    "Default automatic selection in " + locale);
            var screens = MainLoader.getLauncherScreenInfo();
            check(screens.monitors.length > 0, "Real GLFW child monitor enumeration");
            for (int i = 0; i < screens.monitors.length; i++) {
                monitor.getSelectionModel().select(i + 1); view.commit(config);
                check(StartupMonitor.find(config.getStartupMonitor(), screens.monitors) == i, "Save selected monitor");
                mode.setValue(Config.DisplayMode.FULLSCREEN);
                check(!resolution.getItems().isEmpty(), "Selected monitor fullscreen resolutions");
                mode.setValue(Config.DisplayMode.WINDOW);
            }
            var missing = new Config.MonitorSelection("beatoraja-test-disconnected-monitor", -50000, 0);
            config.setStartupMonitor(missing); view.update(config); view.commit(config);
            check(config.getStartupMonitor() == missing, "Opening and saving preserves disconnected preference");
            monitor.getSelectionModel().select(0); view.commit(config);
            check(config.getStartupMonitor() == null, "Explicit automatic resets preference");
        }
        System.out.println("VideoConfigurationViewTest passed (real FXML, English/Japanese, automatic/selected/disconnected persistence)");
        return root;
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
