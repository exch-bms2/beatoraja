package bms.player.beatoraja.launcher;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainLoader;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.Resolution;
import com.badlogic.gdx.Graphics;
import com.github.eduramiba.webcamcapture.drivers.NativeDriver;
import com.github.sarxos.webcam.Webcam;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;

import java.awt.Dimension;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;

public class VideoConfigurationView implements Initializable {
    @FXML
	private ComboBox<Resolution> resolution;
	@FXML
	private ComboBox<Config.DisplayMode> displayMode;
	@FXML
	private ComboBox<String> bgaOp;
	@FXML
	private ComboBox<String> bgaExpand;

	@FXML
	private CheckBox vSync;

	@FXML
	private Spinner<Integer> maxFps;
	@FXML
	private Spinner<Integer> missLayerTime;

	@FXML
	private ComboBox<String> cameraEnabled;

	@FXML
	private ComboBox<String> cameraDevice;

	@FXML
	private ComboBox<String> cameraResolution;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
		updateResolutions();
		updateCameraInfo();

		displayMode.getItems().setAll(Config.DisplayMode.values());
    }

    public void update(Config config) {
		displayMode.setValue(config.getDisplaymode());
		resolution.setValue(config.getResolution());
		vSync.setSelected(config.isVsync());
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
		config.setVsync(vSync.isSelected());
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

		if (displayMode.getValue() == Config.DisplayMode.FULLSCREEN) {
			Graphics.DisplayMode[] displays = MainLoader.getAvailableDisplayMode();
			for(Resolution r : Resolution.values()) {
				for(Graphics.DisplayMode display : displays) {
					if(display.width == r.width && display.height == r.height) {
						resolution.getItems().add(r);
						break;
					}
				}
			}
		} else {
			Graphics.DisplayMode display = MainLoader.getDesktopDisplayMode();
			for(Resolution r : Resolution.values()) {
				if (r.width <= display.width && r.height <= display.height) {
					resolution.getItems().add(r);
				}
			}
		}
		resolution.setValue(resolution.getItems().contains(oldValue)
				? oldValue : resolution.getItems().get(resolution.getItems().size() - 1));
	}

	private void updateCameraInfo() {
		cameraEnabled.getSelectionModel().select(0);

		try {
			Webcam.setDriver(new NativeDriver());
			List<Webcam> cameras = Webcam.getWebcams(1000);
			cameraDevice.setItems(cameras.stream().map(Webcam::getName).collect(toCollection(FXCollections::observableArrayList)));

			cameraDevice.getSelectionModel().selectedIndexProperty().addListener(
					(observableValue, oldValue, newValue) -> {
				Dimension[] dims = cameras.get(newValue.intValue()).getViewSizes();
				cameraResolution.setItems(
					Arrays.stream(dims)
							.map(d -> String.format("%dx%d", d.width, d.height))
							.collect(toCollection(FXCollections::observableArrayList))
				);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
