package bms.player.beatoraja.launcher;

import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig;
import bms.player.beatoraja.PlayerConfig;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class InputConfigurationView implements Initializable {

    // TODO 各デバイス毎の最小入力間隔設定

    @FXML
    private ComboBox<PlayConfigurationView.PlayMode> inputconfig;

    @FXML
    private Spinner<Integer> inputduration;
    @FXML
    private CheckBox jkoc_hack;
    @FXML
    private TableView<ControllerConfigViewModel> controller_tableView;
    @FXML
    private TableColumn<ControllerConfigViewModel, String> playsideCol;
    @FXML
    private TableColumn<ControllerConfigViewModel, String> nameCol;
    @FXML
    private TableColumn<ControllerConfigViewModel, Boolean> isAnalogCol;
    @FXML
    private TableColumn<ControllerConfigViewModel, Integer> analogThresholdCol;
    @FXML
    private TableColumn<ControllerConfigViewModel, Integer> analogModeCol;
    @FXML
    private CheckBox mouseScratch;
    @FXML
    private NumericSpinner<Integer> mouseScratchDuration;

    private PlayerConfig player;

    private PlayConfigurationView.PlayMode mode;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        inputconfig.getItems().setAll(PlayConfigurationView.PlayMode.values());
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
	List<ControllerConfigViewModel> listControllerConfigViewModel = Arrays.asList(conf.getController()).stream()
		.map(config -> new ControllerConfigViewModel(config)).collect(Collectors.toList());
	
	inputduration.getValueFactory().setValue(conf.getKeyboardConfig().getDuration());
	controller_tableView.setEditable(true);
	playsideCol.setEditable(false);
	nameCol.setEditable(false);
	playsideCol.setSortable(false);
	nameCol.setSortable(false);
	isAnalogCol.setSortable(false);
	analogThresholdCol.setSortable(false);
	analogModeCol.setSortable(false);

	// Display "1P" or "2P"
	playsideCol.setCellValueFactory(col -> new SimpleStringProperty(col != null && col.getValue() != null
		? Integer.toString(listControllerConfigViewModel.indexOf(col.getValue()) + 1) + "P"
		: ""));
	nameCol.setCellValueFactory(col -> col.getValue().getNameProperty());
	isAnalogCol.setCellValueFactory(col -> col.getValue().getIsAnalogScratchProperty());
	analogThresholdCol.setCellValueFactory(col -> col.getValue().getAnalogScratchThresholdProperty());
	analogModeCol.setCellValueFactory(col -> col.getValue().getAnalogScratchModeProperty());

	nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
	isAnalogCol.setCellFactory(CheckBoxTableCell.forTableColumn(isAnalogCol));
	analogThresholdCol.setCellFactory(col -> new SpinnerCell(1, 100, 100, 1));
	analogModeCol.setCellFactory(ComboBoxTableCell.forTableColumn(new IntegerStringConverter() {
	    private String v2String = "Ver. 2 (Newest)";
	    private String v1String = "Ver. 1 (~0.6.9)";
	    
	    @Override
	    public Integer fromString(String arg0) {
		if (arg0 == v2String) {
		    return PlayModeConfig.ControllerConfig.ANALOG_SCRATCH_VER_2;
		} else {
		    return PlayModeConfig.ControllerConfig.ANALOG_SCRATCH_VER_1;
		}
	    }

	    @Override
	    public String toString(Integer arg0) {
		if (arg0 == PlayModeConfig.ControllerConfig.ANALOG_SCRATCH_VER_2) {
		    return v2String;
		} else {
		    return v1String;
		}
	    }
	}, PlayModeConfig.ControllerConfig.ANALOG_SCRATCH_VER_2, PlayModeConfig.ControllerConfig.ANALOG_SCRATCH_VER_1));

	ObservableList<ControllerConfigViewModel> data = FXCollections
		.observableArrayList(listControllerConfigViewModel);

	controller_tableView.setItems(data);

	for (PlayModeConfig.ControllerConfig controller : conf.getController()) {
	    inputduration.getValueFactory().setValue(controller.getDuration());
	    jkoc_hack.setSelected(controller.getJKOC());
      mouseScratch.setSelected(controller.isMouseScratch());
      mouseScratchDuration.getValueFactory().setValue(controller.getMouseScratchDuration());
	}

    }
    
    public void commitMode() {
        if (mode != null) {
            PlayModeConfig conf = player.getPlayConfig(Mode.valueOf(mode.name()));
            conf.getKeyboardConfig().setDuration(inputduration.getValue());
            
            for(ControllerConfigViewModel vm : this.controller_tableView.getItems()) {
        	PlayModeConfig.ControllerConfig controller = vm.getConfig();
        	controller.setDuration(inputduration.getValue());
                controller.setJKOC(jkoc_hack.isSelected());
                controller.setMouseScratch(mouseScratch.isSelected());
                controller.setMouseScratchDuration(mouseScratchDuration.getValue());
                controller.setAnalogScratch(vm.getIsAnalogScratchProperty().get());
                controller.setAnalogScratchThreshold(vm.getAnalogScratchThreshold());
                controller.setAnalogScratchMode(vm.getAnalogScratchMode());
            }
        }
    }
}
