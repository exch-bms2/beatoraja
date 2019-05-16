package bms.player.beatoraja.launcher;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.portaudio.DeviceInfo;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.audio.PortAudioDriver;
import bms.player.beatoraja.launcher.PlayConfigurationView.OptionListCell;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;

public class AudioConfigurationView implements Initializable {

	@FXML
	private ComboBox<Integer> audio;
	@FXML
	private ComboBox<String> audioname;
	@FXML
	private Spinner<Integer> audiobuffer;
	@FXML
	private Spinner<Integer> audiosim;
	@FXML
	private Slider systemvolume;
	@FXML
	private Slider keyvolume;
	@FXML
	private Slider bgvolume;
	@FXML
	private ComboBox<Integer> audioFreqOption;
	@FXML
	private ComboBox<Integer> audioFastForward;
	
	private Config config;

	private void initComboBox(ComboBox<Integer> combo, final String[] values) {
		combo.setCellFactory((param) -> new OptionListCell(values));
		combo.setButtonCell(new OptionListCell(values));
		for (int i = 0; i < values.length; i++) {
			combo.getItems().add(i);
		}
	}

	public void initialize(URL arg0, ResourceBundle arg1) {
		initComboBox(audio, new String[] { "OpenAL (LibGDX Sound)", "OpenAL (LibGDX AudioDevice)", "PortAudio"});
		audio.getItems().setAll(0, 2);

		String[] audioPlaySpeedControls = new String[] { "UNPROCESSED", "FREQUENCY" };
		initComboBox(audioFreqOption, audioPlaySpeedControls);
		initComboBox(audioFastForward, audioPlaySpeedControls);

	}

	public void update(Config config) {
		this.config = config;
		
		audio.setValue(config.getAudioDriver());
		audiobuffer.getValueFactory().setValue(config.getAudioDeviceBufferSize());
		audiosim.getValueFactory().setValue(config.getAudioDeviceSimultaneousSources());
		audioFreqOption.setValue(config.getAudioFreqOption());
		audioFastForward.setValue(config.getAudioFastForward());
		systemvolume.setValue((double)config.getSystemvolume());
		keyvolume.setValue((double)config.getKeyvolume());
		bgvolume.setValue((double)config.getBgvolume());

		updateAudioDriver();
	}
	
	public void commit() {
		config.setAudioDriver(audio.getValue());
		config.setAudioDriverName(audioname.getValue());
		config.setAudioDeviceBufferSize(audiobuffer.getValue());
		config.setAudioDeviceSimultaneousSources(audiosim.getValue());
		config.setAudioFreqOption(audioFreqOption.getValue());
		config.setAudioFastForward(audioFastForward.getValue());
		config.setSystemvolume((float) systemvolume.getValue());
		config.setKeyvolume((float) keyvolume.getValue());
		config.setBgvolume((float) bgvolume.getValue());
	}

    @FXML
	public void updateAudioDriver() {
		switch(audio.getValue()) {
		case Config.AUDIODRIVER_SOUND:
			audioname.setDisable(true);
			audioname.getItems().clear();
			audiobuffer.setDisable(false);
			audiosim.setDisable(false);
			break;
		case Config.AUDIODRIVER_PORTAUDIO:
			try {
				DeviceInfo[] devices = PortAudioDriver.getDevices();
				List<String> drivers = new ArrayList<String>(devices.length);
				for(int i = 0;i < devices.length;i++) {
					drivers.add(devices[i].name);
				}
				if(drivers.size() == 0) {
					throw new RuntimeException("ドライバが見つかりません");
				}
				audioname.getItems().setAll(drivers);
				if(drivers.contains(config.getAudioDriverName())) {
					audioname.setValue(config.getAudioDriverName());
				} else {
					audioname.setValue(drivers.get(0));
				}
				audioname.setDisable(false);
				audiobuffer.setDisable(false);
				audiosim.setDisable(false);
//				PortAudio.terminate();
			} catch(Throwable e) {
				Logger.getGlobal().severe("PortAudioは選択できません : " + e.getMessage());
				audio.setValue(Config.AUDIODRIVER_SOUND);
			}
			break;
		}
	}
}
