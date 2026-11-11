package bms.player.beatoraja.launcher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Locale;

import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinProperty;
import bms.player.beatoraja.skin.SkinType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Regression tests for skin scanning through symbolic links. */
public final class SkinConfigurationViewTest {

	private SkinConfigurationViewTest() {
	}

	public static void main(String[] args) throws Exception {
		Path root = Files.createTempDirectory("skin-scan-test");
		Path outside = Files.createTempDirectory("skin-scan-outside-test");
		try {
			Path actualSkin = Files.createFile(root.resolve("actual.lr2skin"));
			Files.createFile(outside.resolve("outside.lr2skin"));
			try {
				Files.createSymbolicLink(root.resolve("outside"), outside);
				Files.createSymbolicLink(root.resolve("loop"), root);
			} catch (UnsupportedOperationException | IOException e) {
				return;
			}

			List<Path> paths = scan(root);
			assertEquals(List.of(actualSkin), paths);
		} finally {
			deleteTree(root);
			deleteTree(outside);
		}
	}

    /** Run on the real JavaFX application thread, not in the headless regression entry point. */
    public static Parent verifyNotesOffsetCenter() throws Exception {
        var loader = new FXMLLoader(SkinConfigurationView.class.getResource("SkinConfigurationView.fxml"),
                ResourceBundle.getBundle("resources.UIResources", Locale.ROOT));
        Parent root = loader.load();
        SkinConfigurationView view = loader.getController();
        var player = new PlayerConfig();
        var playerField = SkinConfigurationView.class.getDeclaredField("player");
        playerField.setAccessible(true); playerField.set(view, player);
        var header = new SkinHeader();
        header.setPath(Path.of("skin/test-notes.luaskin")); header.setSkinType(SkinType.PLAY_7KEYS);
        header.setCustomOffsets(new SkinHeader.CustomOffset[]{new SkinHeader.CustomOffset("Notes offset",
                SkinProperty.OFFSET_NOTES_1P, false, false, false, true, false, false)});
        Method create = SkinConfigurationView.class.getDeclaredMethod("create", SkinHeader.class, SkinConfig.Property.class);
        create.setAccessible(true);
        VBox panel = (VBox) create.invoke(view, header, new SkinConfig.Property());
        var scroll = (ScrollPane) loader.getNamespace().get("skinconfig"); scroll.setContent(panel);
        var row = (HBox) panel.getChildren().get(1);
        if (!((Label) ((HBox) panel.getChildren().get(0)).getChildren().get(0)).getText().equals("Notes offset")
                || !((Label) row.getChildren().get(0)).getText().equals("Notes Offset Center"))
            throw new AssertionError("Center selector must immediately follow Notes offset");
        @SuppressWarnings("unchecked")
        var combo = (ComboBox<Integer>) row.getChildren().get(1);
        if (!combo.getItems().equals(List.of(0, 2, 5, 8)) || combo.getValue() != 0)
            throw new AssertionError("Skin default plus Bottom/Center/Top choices");
        for (int value : new int[]{2, 5, 8, 0}) {
            combo.setValue(value);
            if (view.getProperty().getNotesOffsetCenter() != value)
                throw new AssertionError("Save selected note center");
        }
        combo.setValue(8); view.commitSkinType(); view.commitSkinHeader();
        if (player.getSkin()[SkinType.PLAY_7KEYS.getId()].getProperties().getNotesOffsetCenter() != 8
                || player.getSkinHistory()[player.getSkinHistory().length - 1].getProperties().getNotesOffsetCenter() != 8)
            throw new AssertionError("Active skin and skin history preserve selection");
        view.updateSkinHeader(header);
        row = (HBox) ((VBox) scroll.getContent()).getChildren().get(1);
        if (!((ComboBox<?>) row.getChildren().get(1)).getValue().equals(8))
            throw new AssertionError("Restore selection from skin history");
        var other = new SkinHeader(); other.setPath(Path.of("skin/test-select.json")); other.setSkinType(SkinType.MUSIC_SELECT);
        create.invoke(view, other, new SkinConfig.Property());
        if (view.getProperty().getNotesOffsetCenter() != 0) throw new AssertionError("No stale selection on other skins");
        scroll.setContent((VBox) create.invoke(view, header, player.getSkin()[SkinType.PLAY_7KEYS.getId()].getProperties()));
        var types = (ComboBox<?>) loader.getNamespace().get("skintypeSelector");
        if (types.getItems().stream().anyMatch(item -> item.toString().endsWith(" (2P)")))
            throw new AssertionError("2P entries must not be visible by default");
        player.setIndependentPlaySides(true);
        view.update(player);
        var rightItems = types.getItems().stream().map(Object::toString).filter(name -> name.endsWith(" (2P)")).toList();
        if (!rightItems.equals(List.of("7KEYS (2P)", "5KEYS (2P)", "24KEYS (2P)")))
            throw new AssertionError("Only supported SP skin slots are duplicated: " + rightItems);
        player.setIndependentPlaySides(false);
        view.update(player);
        scroll.setContent((VBox) create.invoke(view, header, player.getSkin()[SkinType.PLAY_7KEYS.getId()].getProperties()));
        System.out.println("SkinConfigurationView Notes Offset Center passed (real FXML, placement, choices, save/history/restore/reset)");
        return root;
    }

	private static List<Path> scan(Path root) throws Exception {
		SkinConfigurationView view = new SkinConfigurationView();
		Method scan = SkinConfigurationView.class.getDeclaredMethod("scan", Path.class, List.class);
		scan.setAccessible(true);
		List<Path> paths = new ArrayList<>();
		try {
			scan.invoke(view, root, paths);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception exception) {
				throw exception;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new RuntimeException(cause);
		}
		return paths;
	}

	private static void deleteTree(Path root) throws IOException {
		try (var paths = Files.walk(root)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}

	private static void assertEquals(Object expected, Object actual) {
		if (!expected.equals(actual)) {
			throw new AssertionError("Expected " + expected + " but was " + actual);
		}
	}
}
