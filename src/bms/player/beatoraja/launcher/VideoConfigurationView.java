package bms.player.beatoraja.launcher;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainLoader;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.DesktopLauncher;
import bms.player.beatoraja.StartupMonitor;
import com.badlogic.gdx.Graphics;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;

import java.net.URL;
import java.util.ResourceBundle;

public class VideoConfigurationView implements Initializable {
	@FXML
	private ComboBox<Resolution> resolution;
	@FXML
	private ComboBox<Config.DisplayMode> displayMode;
	@FXML
	private ComboBox<MonitorChoice> startupMonitor;
	@FXML
	private ComboBox<String> bgaOp;
	@FXML
	private ComboBox<String> bgaExpand;

	@FXML
	private CheckBox vSync;
	@FXML
	private CheckBox notePositionSmoothing;

	@FXML
	private Spinner<Integer> maxFps;
	@FXML
	private Spinner<Integer> missLayerTime;
	private DesktopLauncher.ScreenInfo screens;
	private ResourceBundle resources;

	private record MonitorChoice(String label, Config.MonitorSelection selection, StartupMonitor.Info info) {
		@Override public String toString() { return label; }
	}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
		this.resources = resources;
		screens = MainLoader.getLauncherScreenInfo();
		populateMonitors(null);
		displayMode.getItems().setAll(Config.DisplayMode.values());
		displayMode.valueProperty().addListener((observable, oldValue, value) -> updateResolutions());
		startupMonitor.valueProperty().addListener((observable, oldValue, value) -> updateResolutions());
		updateResolutions();
    }

	private void populateMonitors(Config.MonitorSelection selected) {
		startupMonitor.getItems().clear();
		var automatic = new MonitorChoice(resources.getString("MONITOR_AUTO"), null, null);
		startupMonitor.getItems().add(automatic);
		MonitorChoice choice = automatic;
		int match = StartupMonitor.find(selected, screens.monitors);
		for (int i = 0; i < screens.monitors.length; i++) {
			var info = screens.monitors[i];
			var item = new MonitorChoice((i + 1) + ": " + info.name + " (" + info.desktop.width + "x"
					+ info.desktop.height + (info.primary ? ", " + resources.getString("MONITOR_PRIMARY") : "") + ")",
					info.selection(), info);
			startupMonitor.getItems().add(item);
			if (i == match) choice = item;
		}
		if (selected != null && match < 0) {
			choice = new MonitorChoice(selected.name + " (" + resources.getString("MONITOR_DISCONNECTED") + ")", selected, null);
			startupMonitor.getItems().add(choice);
		}
		startupMonitor.setValue(choice);
	}

    public void update(Config config) {
		populateMonitors(config.getStartupMonitor());
		displayMode.setValue(config.getDisplaymode());
		resolution.setValue(config.getResolution());
		vSync.setSelected(config.isVsync());
		notePositionSmoothing.setSelected(config.isNotePositionSmoothing());
		bgaOp.getSelectionModel().select(config.getBga());
		bgaExpand.getSelectionModel().select(config.getBgaExpand());
		maxFps.getValueFactory().setValue(config.getMaxFramePerSecond());
	}

	public void updatePlayer(PlayerConfig player) {
		missLayerTime.getValueFactory().setValue(player.getMisslayerDuration());
	}

	public void commit(Config config) {
		config.setResolution(resolution.getValue());
		config.setDisplaymode(displayMode.getValue());
		var monitor = startupMonitor.getValue();
		config.setStartupMonitor(monitor != null ? monitor.selection() : null);
		config.setVsync(vSync.isSelected());
		config.setNotePositionSmoothing(notePositionSmoothing.isSelected());
		config.setBga(bgaOp.getSelectionModel().getSelectedIndex());
		config.setBgaExpand(bgaExpand.getSelectionModel().getSelectedIndex());
		config.setMaxFramePerSecond(maxFps.getValue());
	}

	public void commitPlayer(PlayerConfig player) {
		player.setMisslayerDuration(missLayerTime.getValue());
	}

	@FXML
	public void updateResolutions() {
		Resolution oldValue = resolution.getValue();
		resolution.getItems().clear();
		var choice = startupMonitor.getValue();
		var monitor = choice != null ? choice.info() : null;
		if (monitor == null) {
			int primary = StartupMonitor.resolve(null, screens.monitors);
			if (primary >= 0) monitor = screens.monitors[primary];
		}
		startupMonitor.setTooltip(!screens.windowPositionSupported && displayMode.getValue() != Config.DisplayMode.FULLSCREEN
				? new Tooltip(resources.getString("MONITOR_WAYLAND")) : null);

		if (displayMode.getValue() == Config.DisplayMode.FULLSCREEN) {
			var available = monitor != null ? monitor.available : screens.available;
			Graphics.DisplayMode[] displays = java.util.Arrays.stream(available)
					.map(DesktopLauncher.ModeData::toDisplayMode).toArray(Graphics.DisplayMode[]::new);
			for(Resolution r : Resolution.values()) {
				for(Graphics.DisplayMode display : displays) {
					if(display.width == r.width && display.height == r.height) {
						resolution.getItems().add(r);
						break;
					}
				}
			}
		} else {
			Graphics.DisplayMode display = (monitor != null ? monitor.desktop : screens.desktop).toDisplayMode();
			for(Resolution r : Resolution.values()) {
				if (r.width <= display.width && r.height <= display.height) {
					resolution.getItems().add(r);
				}
			}
		}
		if (resolution.getItems().isEmpty()) resolution.getItems().add(oldValue != null ? oldValue : Resolution.HD);
		resolution.setValue(resolution.getItems().contains(oldValue)
				? oldValue : resolution.getItems().get(resolution.getItems().size() - 1));
	}
}
