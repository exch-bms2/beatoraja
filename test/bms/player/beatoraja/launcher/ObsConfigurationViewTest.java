package bms.player.beatoraja.launcher;

import bms.player.beatoraja.Config;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;

/** Runs on the JavaFX launcher smoke test thread. */
public final class ObsConfigurationViewTest {
    public static void verify() throws Exception {
        for (Locale locale : new Locale[]{Locale.ROOT, Locale.JAPAN}) {
            var loader = new FXMLLoader(ObsConfigurationView.class.getResource("ObsConfigurationView.fxml"),
                    ResourceBundle.getBundle("resources.UIResources", locale));
            loader.load();
            ObsConfigurationView view = loader.getController();
            var config = new Config(); view.update(config);
            var fields = loader.getNamespace();
            var enabled = (CheckBox) fields.get("enabled");
            var port = (Spinner<?>) fields.get("port");
            var stop = (Spinner<?>) fields.get("stopDelay");
            var recording = (ComboBox<?>) fields.get("recordingMode");
            check(!enabled.isSelected() && port.getValue().equals(4455) && recording.getSelectionModel().getSelectedIndex() == 0,
                    "Defaults disabled and keep all: " + locale);
            enabled.setSelected(true); port.getEditor().setText("4460"); stop.getEditor().setText("1500");
            ((PasswordField) fields.get("password")).setText("fixture-password");
            recording.getSelectionModel().select(2);
            ((ComboBox<?>) fields.get("scene")).getEditor().setText("Gameplay");
            var add = ObsConfigurationView.class.getDeclaredMethod("addCommand"); add.setAccessible(true); add.invoke(view);
            view.commit(config); config.validate();
            check(config.isUseObsWs() && config.getObsWsPort() == 4460 && config.getObsWsRecStopWait() == 1500
                    && config.getObsWsRecMode() == 2 && config.getObsWsPass().equals("fixture-password"), "Saved OBS controls");
            check(config.getObsCommands().size() == 1 && config.getObsCommands().get(0).getTransitionScene().equals("Gameplay"),
                    "Manual scene entry saves without OBS connected");
            view.update(config);
            check(enabled.isSelected() && ((TableView<?>) fields.get("commands")).getItems().size() == 1, "Reload saved command");
        }
        System.out.println("ObsConfigurationViewTest passed (English/Japanese FXML, disabled defaults, editable values and command persistence)");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
