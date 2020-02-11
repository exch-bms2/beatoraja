package bms.player.beatoraja.launcher;

import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig;
import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import bms.player.beatoraja.PlayerConfig;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.util.converter.IntegerStringConverter;

import java.net.URL;
import java.util.Arrays;
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
    private TableColumn<ControllerConfigViewModel, String> nameCol;
    @FXML
    private TableColumn<ControllerConfigViewModel, Boolean> isAnalogCol;
    @FXML
    private TableColumn<ControllerConfigViewModel, Integer> analogThresholdCol;
    @FXML
    private TableColumn<ControllerConfigViewModel, Integer> analogModeCol;

    private PlayerConfig player;

    private PlayConfigurationView.PlayMode mode;
    
    public class ControllerConfigViewModel {
	private StringProperty nameProperty = new SimpleStringProperty();
	private BooleanProperty isAnalogScratchProperty = new SimpleBooleanProperty();
	private ObjectProperty<Integer> analogScratchThresholdProperty = new SimpleIntegerProperty().asObject();
	private ObjectProperty<Integer> analogScratchModeProperty = new SimpleIntegerProperty().asObject();
	
	private ControllerConfig config;
	
	public ControllerConfigViewModel(ControllerConfig config) {
	    this.config = config;
	    
	    this.nameProperty.set(config.getName());
	    this.isAnalogScratchProperty.set(config.isAnalogScratch());
	    this.analogScratchThresholdProperty.set(config.getAnalogScratchThreshold());
	    this.analogScratchModeProperty.set(config.getAnalogScratchMode());
	    
	}
	
	public String getName() {
	    return this.nameProperty.get();
	}
	public StringProperty getNameProperty() {
	    return nameProperty;
	}

	public boolean getIsAnalogScratch() {
	    return isAnalogScratchProperty.get();
	}
	public void setIsAnalogScratch(boolean isAnalogScratch) {
	    this.isAnalogScratchProperty.set(isAnalogScratch);
	}
	public BooleanProperty getIsAnalogScratchProperty() {
	    return isAnalogScratchProperty;
	}

	public int getAnalogScratchThreshold() {
	    return analogScratchThresholdProperty.get();
	}
	public void setAnalogScratchThreshold(Integer analogScratchThreshold) {
	    this.analogScratchThresholdProperty.set(analogScratchThreshold);
	}
	public ObjectProperty<Integer> getAnalogScratchThresholdProperty() {
	    return analogScratchThresholdProperty;
	}
	
	public int getAnalogScratchMode() {
	    return this.analogScratchModeProperty.get();
	}
	public void setAnalogScratchMode(int analogScratchMode) {
	    this.analogScratchModeProperty.set(analogScratchMode);
	}
	public ObjectProperty<Integer> getAnalogScratchModeProperty() {
	    return analogScratchModeProperty;
	}
	
	public ControllerConfig getConfig() {
	    return this.config;
	}
    }

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
	inputduration.getValueFactory().setValue(conf.getKeyboardConfig().getDuration());
	controller_tableView.setEditable(true);
	nameCol.setEditable(false);
	nameCol.setSortable(false);
	isAnalogCol.setSortable(false);
	analogThresholdCol.setSortable(false);
	analogModeCol.setSortable(false);

	nameCol.setCellValueFactory(col -> col.getValue().getNameProperty());
	isAnalogCol.setCellValueFactory(col -> col.getValue().getIsAnalogScratchProperty());
	analogThresholdCol.setCellValueFactory(col -> col.getValue().getAnalogScratchThresholdProperty());
	analogModeCol.setCellValueFactory(col -> col.getValue().getAnalogScratchModeProperty());

	nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
	isAnalogCol.setCellFactory(CheckBoxTableCell.forTableColumn(isAnalogCol));
	analogThresholdCol.setCellFactory(col -> new SpinnerCell(1, 100, 100, 1));
	analogModeCol.setCellFactory(ComboBoxTableCell.forTableColumn(new IntegerStringConverter() {
	    @Override
	    public Integer fromString(String arg0) {
		if (arg0 == "Ver. 2 (Newest)") {
		    return PlayModeConfig.ControllerConfig.ANALOG_SCRATCH_VER_2;
		} else {
		    return PlayModeConfig.ControllerConfig.ANALOG_SCRATCH_VER_1;
		}
	    }

	    @Override
	    public String toString(Integer arg0) {
		if (arg0 == PlayModeConfig.ControllerConfig.ANALOG_SCRATCH_VER_2) {
		    return "Ver. 2 (Newest)";
		} else {
		    return "Ver. 1 (~0.6.9)";
		}
	    }
	}, PlayModeConfig.ControllerConfig.ANALOG_SCRATCH_VER_2, PlayModeConfig.ControllerConfig.ANALOG_SCRATCH_VER_1));

	ObservableList<ControllerConfigViewModel> data = FXCollections
		.observableArrayList(Arrays.asList(conf.getController()).stream()
			.map(config -> new ControllerConfigViewModel(config)).collect(Collectors.toList()));

	controller_tableView.setItems(data);

	for (PlayModeConfig.ControllerConfig controller : conf.getController()) {
	    inputduration.getValueFactory().setValue(controller.getDuration());
	    jkoc_hack.setSelected(controller.getJKOC());
	}

    }
    
    private final class SpinnerCell extends TableCell<ControllerConfigViewModel, Integer> {
	private final NumericSpinner<Integer> spinner;

	private SpinnerCell(int min, int max, int initial, int step) {
	    spinner = new NumericSpinner<>();
	    spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial, step));
	    setEditable(true);
	}

	@Override
	public void startEdit() {
	    if (!isEmpty()) {
		super.startEdit();
		spinner.getValueFactory().setValue(getItem());

		setOnKeyPressed(event -> {
		    if (event.getCode() == KeyCode.ENTER) {
			Platform.runLater(() -> {
			    commitEdit(spinner.getValue());
			});
		    }
		});

		setText(null);
		setGraphic(spinner);
	    }
	}

	@Override
	public void cancelEdit() {
	    super.cancelEdit();

	    setText(getItem().toString());
	    setGraphic(null);
	}

	@Override
	public void updateItem(Integer item, boolean empty) {
	    super.updateItem(item, empty);

	    if (empty) {
		setText(null);
		setGraphic(null);
	    } else {
		if (isEditing()) {
		    setText(null);
		    setGraphic(spinner);
		} else {
		    setText(getItem().toString());
		    setGraphic(null);
		}
	    }
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
                controller.setAnalogScratch(vm.getIsAnalogScratchProperty().get());
                controller.setAnalogScratchThreshold(vm.getAnalogScratchThreshold());
                controller.setAnalogScratchMode(vm.getAnalogScratchMode());
            }
        }
    }
}
