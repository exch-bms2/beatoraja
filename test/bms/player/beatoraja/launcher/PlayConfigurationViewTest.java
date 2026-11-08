package bms.player.beatoraja.launcher;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.IRConfig;
import bms.player.beatoraja.PlayerConfig;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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
            if (!(loader.getNamespace().get("discordWebhookController") instanceof DiscordConfigurationView))
                throw new AssertionError("Discord Webhook settings missing from launcher");
            verifyDiscordWebhookSettings(locale);
            verifyReviewSyncSettings(locale);
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

    private static void verifyReviewSyncSettings(Locale locale) throws Exception {
        var loader = new FXMLLoader(IRConfigurationView.class.getResource("IRConfigurationView.fxml"),
                ResourceBundle.getBundle("resources.UIResources", locale));
        loader.load();
        IRConfigurationView view = loader.getController();
        var sync = (CheckBox) loader.getNamespace().get("syncReviewLevel");
        @SuppressWarnings("unchecked")
        var names = (ComboBox<String>) loader.getNamespace().get("irname");
        names.getItems().setAll("review-test-primary", "review-test-secondary");
        var player = new PlayerConfig();
        var primary = reviewAccount("review-test-primary");
        var secondary = reviewAccount("review-test-secondary");
        player.setIrconfig(new IRConfig[]{primary, secondary});
        view.update(player);
        if (sync.isSelected() || sync.isDisable() || sync.getText().contains("%"))
            throw new AssertionError("Primary review sync must be localized and opt-in");
        sync.setSelected(true);
        names.setValue(secondary.getIrname());
        view.updateIRConnection();
        if (!sync.isDisable() || sync.isSelected())
            throw new AssertionError("Review sync cannot be enabled on a secondary connection");
        view.commit();
        if (!player.getIrconfig()[0].isSyncReviewLevel() || player.getIrconfig()[1].isSyncReviewLevel())
            throw new AssertionError("Review sync is stored on the selected Primary connection");

        var other = new PlayerConfig();
        other.setIrconfig(new IRConfig[]{reviewAccount(secondary.getIrname())});
        view.update(other);
        names.setValue(primary.getIrname());
        view.updateIRConnection();
        if (sync.isSelected()) throw new AssertionError("Review opt-in cannot carry over from another player");
    }

    private static IRConfig reviewAccount(String name) {
        var config = new IRConfig();
        config.setIrname(name);
        config.setUserid("fixture");
        return config;
    }

    private static void verifyDiscordWebhookSettings(Locale locale) throws Exception {
        var loader = new FXMLLoader(DiscordConfigurationView.class.getResource("DiscordConfigurationView.fxml"),
                ResourceBundle.getBundle("resources.UIResources", locale));
        loader.load();
        DiscordConfigurationView view = loader.getController();
        var config = new Config();
        view.update(config);
        var fields = loader.getNamespace();
        var mode = (ComboBox<?>) fields.get("mode");
        var url = (PasswordField) fields.get("url");
        var destinations = (ListView<?>) fields.get("destinations");
        if (mode.getSelectionModel().getSelectedIndex() != 0 || mode.getItems().size() != 3)
            throw new AssertionError("Discord Webhook is opt-in and has three post formats");
        mode.getSelectionModel().select(2);
        ((TextField) fields.get("name")).setText("リザルト");
        url.setText("https://discord.com/api/webhooks/123/token");
        url.fireEvent(new ActionEvent());
        url.setText("https://discord.com/api/webhooks/456/token");
        view.commit(config);
        config.validate();
        view.update(config);
        if (mode.getSelectionModel().getSelectedIndex() != 2 || destinations.getItems().size() != 2
                || !config.getWebhookName().equals("リザルト") || !url.getText().isEmpty())
            throw new AssertionError("Discord Webhook settings did not round-trip: " + locale);
    }
}
