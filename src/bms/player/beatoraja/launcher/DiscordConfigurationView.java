package bms.player.beatoraja.launcher;

import java.net.URI;
import java.util.ResourceBundle;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.external.discord.DiscordWebhook;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public final class DiscordConfigurationView {
    @FXML private ResourceBundle resources;
    @FXML private ComboBox<String> mode;
    @FXML private TextField name;
    @FXML private TextField avatar;
    @FXML private PasswordField url;
    @FXML private ListView<String> destinations;
    @FXML private Label status;

    @FXML private void initialize() {
        mode.getItems().setAll(resources.getString("DISCORD_WEBHOOK_OFF"),
                resources.getString("DISCORD_WEBHOOK_IMAGE"), resources.getString("DISCORD_WEBHOOK_CARD"));
        destinations.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    return;
                }
                URI endpoint = URI.create(value);
                String id = endpoint.getPath().split("/")[3];
                setText(endpoint.getQuery() == null ? id : id + " (" + endpoint.getQuery() + ")");
            }
        });
    }

    public void update(Config config) {
        mode.getSelectionModel().select(config.getWebhookOption());
        name.setText(config.getWebhookName());
        avatar.setText(config.getWebhookAvatar());
        destinations.getItems().setAll(config.getWebhookUrl());
        url.clear();
        status.setText("");
    }

    public void commit(Config config) {
        if (!url.getText().isBlank()) addUrl();
        config.setWebhookOption(mode.getSelectionModel().getSelectedIndex());
        config.setWebhookName(name.getText().strip());
        config.setWebhookAvatar(avatar.getText().strip());
        config.setWebhookUrl(destinations.getItems().toArray(String[]::new));
    }

    @FXML private void addUrl() {
        String endpoint = DiscordWebhook.normalizeUrl(url.getText());
        if (endpoint == null) {
            status.setText(resources.getString("DISCORD_WEBHOOK_INVALID_URL"));
            return;
        }
        if (!destinations.getItems().contains(endpoint)) {
            if (destinations.getItems().size() >= DiscordWebhook.MAX_DESTINATIONS) {
                status.setText(resources.getString("DISCORD_WEBHOOK_URL_LIMIT"));
                return;
            }
            destinations.getItems().add(endpoint);
        }
        destinations.getSelectionModel().select(endpoint);
        status.setText("");
        url.clear();
    }

    @FXML private void removeUrl() {
        int index = destinations.getSelectionModel().getSelectedIndex();
        if (index >= 0) destinations.getItems().remove(index);
    }
}
