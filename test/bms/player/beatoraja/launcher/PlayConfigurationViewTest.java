package bms.player.beatoraja.launcher;

import bms.player.beatoraja.PlayerConfig;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

/** Real FXML injection and per-mode commits, run on the launcher smoke test's JavaFX thread. */
public final class PlayConfigurationViewTest {
    public static void verify() throws Exception {
        for (Locale locale : new Locale[]{Locale.ROOT, Locale.JAPAN}) {
            var view = new PlayConfigurationView() {
                @Override public void initialize(URL location, ResourceBundle resources) {
                    // This fixture only edits play settings, without loading players or checking versions online.
                }
            };
            var loader = new FXMLLoader(PlayConfigurationView.class.getResource("PlayConfigurationView.fxml"),
                    ResourceBundle.getBundle("resources.UIResources", locale));
            loader.setControllerFactory(type -> {
                if (type == PlayConfigurationView.class) return view;
                try { return type.getDeclaredConstructor().newInstance(); }
                catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            });
            loader.load();
            var player = new PlayerConfig();
            var field = PlayConfigurationView.class.getDeclaredField("player");
            field.setAccessible(true); field.set(view, player);
            var option = (CheckBox) loader.getNamespace().get("analogScratchHispeed");
            if (option == null || option.getText().contains("%") || option.getTooltip().getText().isBlank())
                throw new AssertionError("Localized checkbox and tooltip missing: " + locale);
            @SuppressWarnings("unchecked")
            var modes = (ComboBox<Object>) loader.getNamespace().get("playconfig");
            Object[] values = Class.forName(PlayConfigurationView.class.getName() + "$PlayMode").getEnumConstants();
            modes.getItems().setAll(values);
            Object seven = null, fourteen = null;
            for (Object value : values) {
                if (((Enum<?>) value).name().equals("BEAT_7K")) seven = value;
                if (((Enum<?>) value).name().equals("BEAT_14K")) fourteen = value;
            }
            if (seven == null || fourteen == null) throw new AssertionError("Missing play modes");
            modes.setValue(seven);
            modes.fireEvent(new ActionEvent());
            if (option.isSelected()) throw new AssertionError("Default must retain cover controls");
            option.setSelected(true); modes.setValue(fourteen);
            modes.fireEvent(new ActionEvent());
            if (!player.getMode7().getPlayconfig().isAnalogScratchHispeed() || option.isSelected())
                throw new AssertionError("Mode change must save 7K and restore the 14K setting");
            modes.setValue(seven);
            modes.fireEvent(new ActionEvent());
            if (!option.isSelected()) throw new AssertionError("Saved selection was not restored");
            option.setSelected(false); view.updatePlayConfig();
            if (player.getMode7().getPlayconfig().isAnalogScratchHispeed())
                throw new AssertionError("Explicitly selecting existing cover controls must persist");
        }
        System.out.println("PlayConfigurationViewTest passed (real FXML, English/Japanese, mode-specific checkbox save/restore)");
    }
}
