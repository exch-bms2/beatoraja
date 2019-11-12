package bms.player.beatoraja.launcher;

import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig;
import bms.player.beatoraja.PlayerConfig;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;

import java.net.URL;
import java.util.ResourceBundle;

public class InputConfigurationView implements Initializable {

    // TODO 各デバイス毎の最小入力間隔設定

    @FXML
    private ComboBox<PlayConfigurationView.PlayMode> inputconfig;

    @FXML
    private Spinner<Integer> inputduration;
    @FXML
    private CheckBox jkoc_hack;
    @FXML
    private CheckBox analogScratch;
    @FXML
    private ComboBox<Integer> analogScratchMode;
    @FXML
    private NumericSpinner<Integer> analogScratchThreshold;

    private PlayerConfig player;

    private PlayConfigurationView.PlayMode mode;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        inputconfig.getItems().setAll(PlayConfigurationView.PlayMode.values());
        PlayConfigurationView.initComboBox(analogScratchMode, new String[] { "Ver. 2 (Newest)", "Ver. 1 (~0.6.9)" });
    }

    @FXML
    public void changeMode() {
        commitMode();
        updateMode(inputconfig.getValue());
    }

    public void update(PlayerConfig player) {
        commitMode();
        this.player = player;
        updateMode(PlayConfigurationView.PlayMode.BEAT_7K);
        inputconfig.setValue(PlayConfigurationView.PlayMode.BEAT_7K);
    }

    public void commit() {
        commitMode();
    }

    public void updateMode(PlayConfigurationView.PlayMode mode) {
        this.mode = mode;
        PlayModeConfig conf = player.getPlayConfig(Mode.valueOf(mode.name()));
        inputduration.getValueFactory().setValue(conf.getKeyboardConfig().getDuration());
        for(PlayModeConfig.ControllerConfig controller : conf.getController()) {
            inputduration.getValueFactory().setValue(controller.getDuration());
            jkoc_hack.setSelected(controller.getJKOC());
            analogScratch.setSelected(controller.isAnalogScratch());
            analogScratchMode.getSelectionModel().select(controller.getAnalogScratchMode());
            analogScratchThreshold.getValueFactory().setValue(controller.getAnalogScratchThreshold());
        }

    }

    public void commitMode() {
        if (mode != null) {
            PlayModeConfig conf = player.getPlayConfig(Mode.valueOf(mode.name()));
            conf.getKeyboardConfig().setDuration(inputduration.getValue());
            for(PlayModeConfig.ControllerConfig controller : conf.getController()) {
                controller.setDuration(inputduration.getValue());
                controller.setJKOC(jkoc_hack.isSelected());
                controller.setAnalogScratch(analogScratch.isSelected());
                controller.setAnalogScratchThreshold(analogScratchThreshold.getValue());
                controller.setAnalogScratchMode(analogScratchMode.getValue());
            }
        }
    }
}
