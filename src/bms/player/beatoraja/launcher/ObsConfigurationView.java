package bms.player.beatoraja.launcher;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState.MainStateType;
import bms.player.beatoraja.external.obs.ObsControlCommand;
import bms.player.beatoraja.external.obs.ObsController;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;

public final class ObsConfigurationView implements Initializable {
    @FXML private CheckBox enabled;
    @FXML private TextField host;
    @FXML private Spinner<Integer> port;
    @FXML private PasswordField password;
    @FXML private Spinner<Integer> stopDelay;
    @FXML private ComboBox<String> recordingMode;
    @FXML private Label status;
    @FXML private ComboBox<String> timing, action, scene, source;
    @FXML private TableView<ObsControlCommand> commands;
    @FXML private TableColumn<ObsControlCommand, String> timingColumn, actionColumn, sceneColumn, sourceColumn;
    private ResourceBundle resources;
    private Map<String, List<String>> catalog = Map.of();
    private CompletableFuture<ObsController.ConnectionInfo> probe;

    @Override public void initialize(URL url, ResourceBundle resources) {
        this.resources = resources;
        recordingMode.getItems().setAll(text("OBS_KEEP_ALL"), text("OBS_KEEP_SCREENSHOT"), text("OBS_KEEP_REPLAY"));
        timing.getItems().add("MAIN_START");
        for (var state : MainStateType.values()) timing.getItems().addAll(state.name() + "_START", state.name() + "_END");
        timing.getItems().add("MAIN_END");
        timing.setValue("PLAY_START");
        action.getItems().setAll(ObsControlCommand.ACTIONS);
        action.setConverter(new StringConverter<>() {
            public String toString(String id) { return id == null ? "" : text("OBS_ACTION_" + id); }
            public String fromString(String label) { return label; }
        });
        action.setValue("SetScene");
        timingColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getTiming()));
        actionColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(text("OBS_ACTION_" + row.getValue().getAction())));
        sceneColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getTargetDisplay()));
        sourceColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getTargetSource()));
        scene.valueProperty().addListener((observable, old, value) -> refreshSources(value));
        action.valueProperty().addListener((observable, old, value) -> updateInputs());
        updateInputs();
    }
    public void update(Config config) {
        closeProbe();
        enabled.setSelected(config.isUseObsWs()); host.setText(config.getObsWsHost());
        port.getValueFactory().setValue(config.getObsWsPort()); password.setText(config.getObsWsPass());
        stopDelay.getValueFactory().setValue(config.getObsWsRecStopWait());
        recordingMode.getSelectionModel().select(config.getObsWsRecMode());
        commands.getItems().setAll(config.getObsCommands());
        status.setText("");
    }
    public void commit(Config config) {
        closeProbe();
        connectionSettings(config);
        config.setUseObsWs(enabled.isSelected());
        config.setObsWsRecStopWait(spinnerValue(stopDelay));
        config.setObsWsRecMode(recordingMode.getSelectionModel().getSelectedIndex());
        config.setObsCommands(new ArrayList<>(commands.getItems()));
    }
    private void connectionSettings(Config config) {
        config.setObsWsHost(host.getText().strip()); config.setObsWsPort(spinnerValue(port));
        config.setObsWsPass(password.getText());
    }
    private static int spinnerValue(Spinner<Integer> spinner) {
        try { spinner.getValueFactory().setValue(Integer.parseInt(spinner.getEditor().getText().strip())); }
        catch (NumberFormatException invalid) { }
        return spinner.getValue();
    }
    @FXML private void connect() {
        closeProbe();
        var settings = new Config(); connectionSettings(settings); settings.validate();
        status.setText(text("OBS_CONNECTING"));
        var request = ObsController.inspect(settings); probe = request;
        request.whenComplete((info, error) -> Platform.runLater(() -> {
            if (probe != request) return;
            probe = null;
            if (error != null) { status.setText(text("OBS_CONNECT_FAILED")); return; }
            status.setText(text("OBS_CONNECTED") + " " + info.version());
            catalog = info.scenes();
            String current = scene.getEditor().getText();
            scene.getItems().setAll(catalog.keySet()); scene.setValue(current);
            refreshSources(current);
        }));
    }
    public void closeProbe() {
        var request = probe; probe = null;
        if (request != null) request.cancel(false);
    }
    private void refreshSources(String name) {
        String current = source.getEditor().getText();
        source.getItems().setAll(name == null ? List.of() : catalog.getOrDefault(name, List.of()));
        source.setValue(current);
    }
    private void updateInputs() {
        boolean visibility = "ShowSource".equals(action.getValue()) || "HideSource".equals(action.getValue());
        scene.setDisable(!visibility && !"SetScene".equals(action.getValue())); source.setDisable(!visibility);
    }
    @FXML private void addCommand() {
        var command = new ObsControlCommand(timing.getValue(), action.getValue(), scene.getEditor().getText().strip(),
                source.isDisabled() ? "" : source.getEditor().getText().strip());
        if (command.isValid() && commands.getItems().size() < 128) commands.getItems().add(command);
    }
    @FXML private void removeCommand() {
        commands.getItems().remove(commands.getSelectionModel().getSelectedItem());
    }
    private String text(String key) { return resources.getString(key); }
}
