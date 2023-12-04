package bms.player.beatoraja.launcher;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.portaudio.DeviceInfo;

import bms.player.beatoraja.AudioConfig;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.TableData;
import bms.player.beatoraja.AudioConfig.DriverType;
import bms.player.beatoraja.AudioConfig.FrequencyType;
import bms.player.beatoraja.audio.PortAudioDriver;
import bms.player.beatoraja.song.SongDatabaseAccessor;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;

public class StreamEditorView implements Initializable {

    @FXML
    private CheckBox enableRequest;
    @FXML
    private CheckBox notifyRequest;
    @FXML
    private Spinner<Integer> maxRequestCount;
    
    private PlayerConfig player;

    public void initialize(URL arg0, ResourceBundle arg1) {
    }

    public void update(PlayerConfig player) {
        this.player = player;
        if(this.player == null) {
            return;
        }
        enableRequest.setSelected(this.player.getRequestEnable());
        notifyRequest.setSelected(this.player.getRequestNotify());
        maxRequestCount.getValueFactory().setValue(this.player.getMaxRequestCount());
    }

    public void commit() {
        if(this.player == null) {
            return;
        }
        player.setRequestEnable(enableRequest.isSelected());
        player.setRequestNotify(notifyRequest.isSelected());
        player.setMaxRequestCount(maxRequestCount.getValue());
    }
}
