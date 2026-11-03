package bms.player.beatoraja;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;

import bms.player.beatoraja.input.KeyCommand;

/** Legacy settings must remain readable without retaining retired credentials. */
public final class TwitterIntegrationRemovalTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("beatoraja-retired-settings-");
        try {
            for (String filename : new String[] {"config_player.json", "config.json"}) {
                Path directory = Files.createDirectory(root.resolve(filename.equals("config.json") ? "legacy" : "current"));
                Files.writeString(directory.resolve(filename), """
                        {
                          "name": "legacy-player",
                          "twitterConsumerKey": "obsolete-key",
                          "twitterConsumerSecret": "obsolete-secret",
                          "twitterAccessToken": "obsolete-token",
                          "twitterAccessTokenSecret": "obsolete-token-secret"
                        }
                        """);
                PlayerConfig player = PlayerConfig.readPlayerConfig(root.toString(), directory.getFileName().toString());
                check(player != null && "legacy-player".equals(player.getName()), "Legacy config was not loaded");
                check(!Files.exists(directory.resolve("config_backup.json")), "Legacy config was rejected");
                PlayerConfig.write(root.toString(), player);
                String saved = Files.readString(directory.resolve("config_player.json"));
                check(!saved.toLowerCase(Locale.ROOT).contains("twitter"), "Retired credentials were saved");
                check("legacy-player".equals(PlayerConfig.readPlayerConfig(root.toString(), player.getId()).getName()),
                        "Config round trip failed");
            }
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        String resource = "bms/player/beatoraja/launcher/PlayConfigurationView.fxml";
        try (var in = TwitterIntegrationRemovalTest.class.getClassLoader().getResourceAsStream(resource)) {
            check(in != null, "Missing launcher FXML");
            var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
            check(!document.getDocumentElement().getTextContent().toLowerCase(Locale.ROOT).contains("twitter"),
                    "Retired launcher text");
        }
        try (var in = TwitterIntegrationRemovalTest.class.getClassLoader().getResourceAsStream(resource)) {
            String fxml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            check(!fxml.toLowerCase(Locale.ROOT).contains("twitter") && !fxml.contains("#startPINAuth"),
                    "Retired authentication UI or event handler");
        }
        check(KeyCommand.valueOf("SAVE_SCREENSHOT") != null, "Screenshot command must remain");
        for (KeyCommand command : KeyCommand.values()) {
            check(!command.name().contains("TWITTER"), "Retired posting command");
        }
        System.out.println("TwitterIntegrationRemovalTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
