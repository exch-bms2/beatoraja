package bms.player.beatoraja.launcher;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.portaudio.DeviceInfo;

import bms.player.beatoraja.AudioConfig;
import bms.player.beatoraja.AudioConfig.DriverType;
import bms.player.beatoraja.AudioConfig.FrequencyType;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.audio.PortAudioDriver;
import bms.player.beatoraja.launcher.PlayConfigurationView.OptionListCell;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;

public class AudioConfigurationView implements Initializable {

	@FXML
	private ComboBox<DriverType> audio;
	@FXML
	private ComboBox<String> audioname;
	@FXML
	private Spinner<Integer> audiobuffer;
	@FXML
	private Spinner<Integer> audiosim;
	@FXML
	private ComboBox<Integer> audiosamplerate;
	@FXML
	private Slider systemvolume;
	@FXML
	private Slider keyvolume;
	@FXML
	private Slider bgvolume;
	@FXML
	private ComboBox<FrequencyType> audioFreqOption;
	@FXML
	private ComboBox<FrequencyType> audioFastForward;
	@FXML
	private CheckBox loopResultSound;
	@FXML
	private CheckBox loopCourseResultSound;
	
	private AudioConfig config;

	public void initialize(URL arg0, ResourceBundle arg1) {
		audio.getItems().setAll(DriverType.OpenAL , DriverType.PortAudio);
		audiosamplerate.getItems().setAll(null, 44100, 48000);

		audioFreqOption.getItems().setAll(FrequencyType.UNPROCESSED , FrequencyType.FREQUENCY);
		audioFastForward.getItems().setAll(FrequencyType.UNPROCESSED , FrequencyType.FREQUENCY);
	}

	public void update(AudioConfig config) {
		this.config = config;
		
		audio.setValue(config.getDriver());
		audiobuffer.getValueFactory().setValue(config.getDeviceBufferSize());
		audiosim.getValueFactory().setValue(config.getDeviceSimultaneousSources());
		audiosamplerate.setValue(config.getSampleRate() > 0 ? config.getSampleRate() : null);
		audioFreqOption.setValue(config.getFreqOption());
		audioFastForward.setValue(config.getFastForward());
		systemvolume.setValue((double)config.getSystemvolume());
		keyvolume.setValue((double)config.getKeyvolume());
		bgvolume.setValue((double)config.getBgvolume());
		loopResultSound.setSelected(config.isLoopResultSound());
		loopCourseResultSound.setSelected(config.isLoopCourseResultSound());

		updateAudioDriver();
	}
	
	public void commit() {
		config.setDriver(audio.getValue());
		config.setDriverName(audioname.getValue());
		config.setDeviceBufferSize(audiobuffer.getValue());
		config.setDeviceSimultaneousSources(audiosim.getValue());
		config.setSampleRate(audiosamplerate.getValue() != null ? audiosamplerate.getValue() : 0);
		config.setFreqOption(audioFreqOption.getValue());
		config.setFastForward(audioFastForward.getValue());
		config.setSystemvolume((float) systemvolume.getValue());
		config.setKeyvolume((float) keyvolume.getValue());
		config.setBgvolume((float) bgvolume.getValue());
		config.setLoopResultSound(loopResultSound.isSelected());
		config.setLoopCourseResultSound(loopCourseResultSound.isSelected());
	}

    @FXML
	public void updateAudioDriver() {
		switch(audio.getValue()) {
		case OpenAL:
			audioname.setDisable(true);
			audioname.getItems().clear();
			audiobuffer.setDisable(false);
			audiosim.setDisable(false);
			break;
		case PortAudio:
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
				if(drivers.contains(config.getDriverName())) {
					audioname.setValue(config.getDriverName());
				} else {
					audioname.setValue(drivers.get(0));
				}
				audioname.setDisable(false);
				audiobuffer.setDisable(false);
				audiosim.setDisable(false);
//				PortAudio.terminate();
			} catch(Throwable e) {
				Logger.getGlobal().severe("PortAudioは選択できません : " + e.getMessage());
				audio.setValue(DriverType.OpenAL);
			}
			break;
		}
	}
}
