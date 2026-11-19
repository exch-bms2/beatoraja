package bms.player.beatoraja.launcher;

import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlaySide;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class InputConfigurationView implements Initializable {

    // TODO 各デバイス毎の最小入力間隔設定

    @FXML
    private ComboBox<InputSlot> inputconfig;

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
    private NumericSpinner<Integer> mouseScratchTimeThreshold;
    @FXML
    private NumericSpinner<Integer> mouseScratchDistance;
    @FXML
    private ComboBox<Integer> mouseScratchMode;

    private PlayerConfig player;

    private PlayConfigurationView.PlayMode mode;
    private PlaySide side = PlaySide.P1;
    private boolean updating;

    private record InputSlot(PlayConfigurationView.PlayMode mode, PlaySide side) {
        @Override public String toString() { return mode + (side == PlaySide.P2 ? " (2P)" : ""); }
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshSlots();
        PlayConfigurationView.initComboBox(mouseScratchMode, new String[] { "Ver. 2 (Newest)", "Ver. 1 (~0.8.3)" });
    }

    @FXML
    public void changeMode() {
        if (updating || player == null || inputconfig.getValue() == null) return;
        commitMode();
        var slot = inputconfig.getValue();
        updateMode(slot.mode(), slot.side());
    }

    public void update(PlayerConfig player) {
        commitMode();
        this.player = player;
        mode = PlayConfigurationView.PlayMode.BEAT_7K;
        side = PlaySide.P1;
        refreshSlots();
    }

    public void commit() {
        commitMode();
    }

    void refreshMode() {
        if (mode != null) updateMode(mode, side);
    }

    void refreshSlots() {
        updating = true;
        try {
            var slots = new ArrayList<InputSlot>();
            for (var value : PlayConfigurationView.PlayMode.values()) {
                slots.add(new InputSlot(value, PlaySide.P1));
                if (player != null && player.isIndependentPlaySides() && PlaySide.supports(Mode.valueOf(value.name())))
                    slots.add(new InputSlot(value, PlaySide.P2));
            }
            if (player == null || !player.isIndependentPlaySides()) side = PlaySide.P1;
            inputconfig.getItems().setAll(slots);
            if (mode != null) inputconfig.setValue(new InputSlot(mode, side));
        } finally { updating = false; }
        if (player != null) refreshMode();
    }

    public void updateMode(PlayConfigurationView.PlayMode mode) {
        updateMode(mode, PlaySide.P1);
    }

    private void updateMode(PlayConfigurationView.PlayMode mode, PlaySide side) {
	this.mode = mode;
        this.side = side;
	PlayModeConfig conf = player.getPlayInputConfig(Mode.valueOf(mode.name()), side);
	List<ControllerConfigViewModel> listControllerConfigViewModel = Arrays.asList(conf.getController()).stream()
		.map(config -> new ControllerConfigViewModel(config)).collect(Collectors.toList());
	
	inputduration.getValueFactory().setValue(conf.getKeyboardConfig().getDuration());
    mouseScratch.setSelected(conf.getKeyboardConfig().getMouseScratchConfig().isMouseScratchEnabled());
    mouseScratchTimeThreshold.getValueFactory().setValue(conf.getKeyboardConfig().getMouseScratchConfig().getMouseScratchTimeThreshold());
    mouseScratchDistance.getValueFactory().setValue(conf.getKeyboardConfig().getMouseScratchConfig().getMouseScratchDistance());
    mouseScratchMode.getSelectionModel().select(conf.getKeyboardConfig().getMouseScratchConfig().getMouseScratchMode());

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
		? (side == PlaySide.P2 ? "2P" : Integer.toString(listControllerConfigViewModel.indexOf(col.getValue()) + 1) + "P")
		: ""));
	nameCol.setCellValueFactory(col -> col.getValue().getNameProperty());
	isAnalogCol.setCellValueFactory(col -> col.getValue().getIsAnalogScratchProperty());
	analogThresholdCol.setCellValueFactory(col -> col.getValue().getAnalogScratchThresholdProperty());
	analogModeCol.setCellValueFactory(col -> col.getValue().getAnalogScratchModeProperty());

	nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
	isAnalogCol.setCellFactory(CheckBoxTableCell.forTableColumn(isAnalogCol));
	analogThresholdCol.setCellFactory(col -> new SpinnerCell(1, 1000, 100, 1));
	analogModeCol.setCellFactory(ComboBoxTableCell.forTableColumn(new IntegerStringConverter() {
	    private String v2String = "Ver. 2 (Newest)";
	    private String v1String = "Ver. 1 (~0.6.9)";
	    
	    @Override
	    public Integer fromString(String arg0) {
		if (Objects.equals(arg0, v2String)) {
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
	}

    }
    
    public void commitMode() {
        if (player != null && mode != null) {
            PlayModeConfig conf = player.getPlayInputConfig(Mode.valueOf(mode.name()), side);
            conf.getKeyboardConfig().setDuration(inputduration.getValue());
            conf.getKeyboardConfig().getMouseScratchConfig().setMouseScratchEnabled(mouseScratch.isSelected());
            conf.getKeyboardConfig().getMouseScratchConfig().setMouseScratchTimeThreshold(mouseScratchTimeThreshold.getValue());
            conf.getKeyboardConfig().getMouseScratchConfig().setMouseScratchDistance(mouseScratchDistance.getValue());
            conf.getKeyboardConfig().getMouseScratchConfig().setMouseScratchMode(mouseScratchMode.getValue());
            
            for(ControllerConfigViewModel vm : this.controller_tableView.getItems()) {
        	PlayModeConfig.ControllerConfig controller = vm.getConfig();
        	controller.setDuration(inputduration.getValue());
                controller.setJKOC(jkoc_hack.isSelected());
                controller.setAnalogScratch(vm.getIsAnalogScratchProperty().get());
                controller.setAnalogScratchThreshold(vm.getAnalogScratchThreshold());
                controller.setAnalogScratchMode(vm.getAnalogScratchMode());
            }
        }
    }
}
