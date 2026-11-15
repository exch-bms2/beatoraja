package bms.player.beatoraja.launcher;

import bms.player.beatoraja.LocalTableFile;
import java.nio.file.Files;
import java.sql.DriverManager;
import bms.player.beatoraja.TableData;
import bms.player.beatoraja.song.SongData;
import javafx.animation.PauseTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TableView;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableRow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import java.util.List;
import java.util.function.BooleanSupplier;
import javafx.util.Duration;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class TableEditorWindowTest {
    public static void main(String[] args) {
        Application.launch(Probe.class, args);
    }

    public static void verify() throws Exception {
        var directory = Files.createTempDirectory("table-editor-window");
        var path = directory.resolve(LocalTableFile.FILE_NAME);
        var database = directory.resolve("songdata.db");
        String content = "{\"name\":\"New Table\"}";
        Stage editor = null;
        try {
            Files.writeString(path, content);
            try (var db = DriverManager.getConnection("jdbc:sqlite:" + database);
                    var statement = db.createStatement()) {
                statement.execute("CREATE TABLE song (title TEXT, subtitle TEXT, artist TEXT, subartist TEXT, genre TEXT, md5 TEXT, sha256 TEXT, path TEXT, mode INTEGER, level INTEGER)");
                statement.execute("INSERT INTO song VALUES ('Test A','','Artist','','','" + "a".repeat(32) + "','" + "1".repeat(64)
                        + "','test-a.bms',7,0), ('Test B','','Artist','','','" + "b".repeat(32) + "','" + "2".repeat(64) + "','test-b.bms',14,12)");
            }
            editor = TableEditorView.open(path, new TableChartLookup(database.toString()));
            var root = editor.getScene().getRoot();
            root.applyCss();
            check(editor.isShowing(), "The standalone editor should open");
            check(root.lookup("#tableName") instanceof TextField name && "New Table".equals(name.getText()),
                    "The empty table should be shown");
            check(root.lookupAll(".button").stream().filter(node -> node instanceof Button button
                    && "Save".equals(button.getText())).count() == 1, "Exactly one Save button");
            check(Files.readString(path).equals(content), "Opening the editor should not rewrite the draft");
            verifyWorkspace(root, path);
            editor.hide();
            var file = new LocalTableFile(path);
            var table = file.load();
            var folder = table.getFolder()[0];
            var a = folder.getSong()[0];
            var b = folder.getSong()[1];
            b.setSha256("");
            b.setMd5("B".repeat(32));
            var missing = new SongData();
            missing.setTitle("Missing chart");
            missing.setSha256("3".repeat(64));
            missing.setMd5(a.getMd5());
            folder.setSong(new SongData[]{a, b, missing});
            file.save(table);
            String saved = Files.readString(path);
            editor = TableEditorView.open(path, new TableChartLookup(database.toString()));
            verifyReopened(editor.getScene().getRoot());
            check(Files.readString(path).equals(saved), "Display lookup does not rewrite the saved file");
        } finally {
            if (editor != null) editor.hide();
            Files.deleteIfExists(path);
            Files.deleteIfExists(database);
            Files.deleteIfExists(directory);
        }
    }

    @SuppressWarnings("unchecked")
    private static void verifyWorkspace(Parent root, java.nio.file.Path path) {
        check(((SplitPane) root.lookup("#tableEditorPanes")).getItems().size() == 3, "Three panes");
        var results = (TableView<SongData>) root.lookup("#searchResults");
        var charts = (TableView<SongData>) root.lookup("#editingCharts");
        var entries = (ListView<?>) root.lookup("#tableEntries");
        var query = (TextField) root.lookup("#chartQuery");
        var search = (Button) root.lookup("#searchCharts");
        var add = (Button) root.lookup("#addSelectedCharts");
        var drag = (TableEditorChartDrag) root.lookup("#chartDragTarget");
        var waitKey = new Object();
        var timeout = new PauseTransition(Duration.seconds(5));
        timeout.setOnFinished(event -> Platform.exitNestedEventLoop(waitKey, false));
        ListChangeListener<SongData> ready = change -> {
            if (results.getItems().size() == 2) Platform.exitNestedEventLoop(waitKey, true);
        };
        results.getItems().addListener(ready);
        query.setText("Old query");
        search.fire();
        query.setText("Test");
        search.fire();
        check(results.getItems().isEmpty(), "Search is not synchronous on the FX thread");
        timeout.play();
        try {
            check(Boolean.TRUE.equals(Platform.enterNestedEventLoop(waitKey)), "Latest search supplies both results");
        } finally {
            timeout.stop();
            results.getItems().removeListener(ready);
        }
        ((Button) root.lookup("#addFolder")).fire();
        results.getSelectionModel().select(1);
        results.getSelectionModel().select(0);
        check(drag.capture(results) && drag.drop(0), "Drop into an empty folder");
        add.fire();
        check(drag.capture(results) && drag.drop(1), "Drop duplicates into a folder");
        check(charts.getItems().equals(results.getItems()), "Folder adds in display order and skips duplicates");
        check(drag.capture(results), "Capture before switching the editing destination");
        ((Button) root.lookup("#addCourse")).fire();
        check(!drag.drop(0), "Switching the editing destination invalidates the drop");
        check(root.lookup("#courseSettings").isVisible(), "Course settings remain accessible");
        add.fire();
        var a = results.getItems().get(0);
        var b = results.getItems().get(1);
        root.layout();
        var row = charts.lookupAll(".table-row-cell").stream().filter(n -> n instanceof TableRow<?> r
                && !r.isEmpty() && r.getIndex() == 0).map(n -> (TableRow<?>) n).findFirst().orElseThrow();
        var bounds = row.localToScene(row.getBoundsInLocal());
        check(drag.capture(results), "Capture selected search rows");
        var before = TableEditorChartDrag.rowPosition(row, bounds.getMinY() + 1, 2);
        var after = TableEditorChartDrag.rowPosition(row, bounds.getMaxY() - 1, 2);
        check(before.index() == 0 && after.index() == 1, "Upper and lower row halves target adjacent positions");
        drag.showMarker(after);
        check(root.lookup("#chartInsertionLine").isVisible(), "Insertion line is visible before the drop");
        check(drag.drop(after.index()) && charts.getItems().equals(List.of(a, a, b, b)),
                "Middle drop inserts search rows in display order and allows course duplicates");
        check(!root.lookup("#chartInsertionLine").isVisible(), "Drop clears the insertion line");
        charts.getSelectionModel().clearSelection();
        charts.getSelectionModel().select(3);
        charts.getSelectionModel().select(1);
        check(drag.capture(charts) && drag.drop(0) && charts.getItems().equals(List.of(a, b, a, b)),
                "Move only the selected occurrences to the start and preserve relative order");
        check(charts.getSelectionModel().getSelectedIndices().equals(List.of(0, 1)), "Moved rows remain selected");
        charts.getSelectionModel().clearAndSelect(0);
        check(drag.capture(charts) && drag.drop(4) && charts.getItems().equals(List.of(b, a, b, a)),
                "Drop at the end accounts for removed rows");
        check(drag.capture(charts), "Capture before mutating the chart list");
        charts.getItems().add(b);
        check(!drag.drop(0), "Changing the chart list invalidates the drop");
        charts.getItems().remove(4);
        entries.getSelectionModel().select(0);
        check(charts.getItems().equals(results.getItems()) && !root.lookup("#courseSettings").isVisible(),
                "Switching back restores the folder and hides course settings");
        root.lookupAll(".button").stream().filter(node -> node instanceof Button button && "Save".equals(button.getText()))
                .map(node -> (Button) node).findFirst().orElseThrow().fire();
        var saved = TableData.read(path);
        check(saved != null && saved.getFolder()[0].getSong().length == 2 && saved.getCourse()[0].getSong().length == 4
                && saved.getCourse()[0].getTrophy().length == 3, "Save stays compatible with the game reader");
        checkDetail(results, 0, a.getSha256(), a.getMd5(), true);
    }

    @SuppressWarnings("unchecked")
    private static void verifyReopened(Parent root) {
        root.applyCss();
        var charts = (TableView<SongData>) root.lookup("#editingCharts");
        var entries = (ListView<?>) root.lookup("#tableEntries");
        check("\u2014 / \u2014".equals(modeLevel(charts, 0)), "Metadata lookup does not run on the FX thread");
        entries.getSelectionModel().select(1);
        entries.getSelectionModel().select(0);
        await(() -> "7 / 0".equals(modeLevel(charts, 0)) && "14 / 12".equals(modeLevel(charts, 1)),
                "SHA-256 and legacy MD5 resolve after rapidly switching entries");
        check("\u2014 / \u2014".equals(modeLevel(charts, 2)), "Missing SHA-256 does not match an unrelated MD5");
        check(charts.getItems().get(1).getMode() == 0 && charts.getItems().get(1).getSha256().isEmpty(),
                "Resolved metadata never mutates the saved entry");
        checkDetail(charts, 1, "2".repeat(64), "B".repeat(32), true);
        checkDetail(charts, 2, "3".repeat(64), "a".repeat(32), false);
        Stage editor = (Stage) root.getScene().getWindow();
        Platform.runLater(() -> List.copyOf(Window.getWindows()).stream()
                .filter(w -> w instanceof Stage s && s.getOwner() == editor).forEach(Window::hide));
        editor.fireEvent(new javafx.stage.WindowEvent(editor, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
        check(!editor.isShowing(), "Metadata refresh does not create unsaved changes");
    }

    private static Object modeLevel(TableView<SongData> table, int row) {
        return table.getColumns().get(2).getCellData(row);
    }

    private static void checkDetail(TableView<SongData> table, int index, String sha256, String md5, boolean local) {
        var root = table.getScene().getRoot();
        root.applyCss();
        root.layout();
        var row = table.lookupAll(".table-row-cell").stream().filter(n -> n instanceof TableRow<?> r
                && !r.isEmpty() && r.getIndex() == index).findFirst().orElseThrow();
        table.getSelectionModel().clearAndSelect(index == 0 ? 1 : 0);
        for (var button : List.of(MouseButton.SECONDARY, MouseButton.PRIMARY)) {
            int clicks = button == MouseButton.SECONDARY ? 2 : 1;
            click(row, button, clicks);
            check(detailWindow() == null, "Only a primary double-click opens details");
        }
        click(row, MouseButton.PRIMARY, 2);
        if (detailWindow() == null) await(() -> detailWindow() != null, "Double-click opens the details dialog");
        var window = detailWindow();
        var details = window.getScene().getRoot();
        try {
            details.applyCss();
            var clipboard = Clipboard.getSystemClipboard();
            var previous = new ClipboardContent();
            for (var type : clipboard.getContentTypes()) previous.put(type, clipboard.getContent(type));
            try {
                for (String hash : List.of(sha256, md5)) {
                    var field = details.lookupAll(".text-field").stream().filter(n -> n instanceof TextField input
                            && hash.equals(input.getText()) && !input.isEditable()).map(n -> (TextField) n).findFirst().orElseThrow();
                    field.selectAll();
                    field.copy();
                    check(hash.equals(clipboard.getString()), "The displayed hash can be copied");
                }
            } finally {
                clipboard.setContent(previous);
            }
            check(details.lookupAll(".button").stream().anyMatch(n -> n instanceof Button b
                    && "Open Folder".equals(b.getText())) == local, "Folder action is available only for local charts");
        } finally {
            window.hide();
        }
    }

    private static Window detailWindow() {
        return Window.getWindows().stream().filter(w -> w instanceof Stage s && "Chart Details".equals(s.getTitle()))
                .findFirst().orElse(null);
    }

    private static void click(javafx.scene.Node row, MouseButton button, int count) {
        Event.fireEvent(row, new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, button, count,
                false, false, false, false, false, false, false, false, false, true, null));
    }

    private static void await(BooleanSupplier condition, String message) {
        var key = new Object();
        boolean[] done = {false};
        var poll = new Timeline(new KeyFrame(Duration.millis(20), event -> {
            if (!done[0] && condition.getAsBoolean()) {
                done[0] = true;
                Platform.exitNestedEventLoop(key, true);
            }
        }));
        poll.setCycleCount(Timeline.INDEFINITE);
        var timeout = new PauseTransition(Duration.seconds(5));
        timeout.setOnFinished(event -> {
            if (!done[0]) {
                done[0] = true;
                Platform.exitNestedEventLoop(key, false);
            }
        });
        poll.play();
        timeout.play();
        try {
            check(Boolean.TRUE.equals(Platform.enterNestedEventLoop(key)), message);
        } finally {
            poll.stop();
            timeout.stop();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static final class Probe extends Application {
        @Override public void start(Stage stage) throws Exception {
            try {
                verify();
                System.out.println("TableEditorWindowTest passed");
            } finally {
                Platform.exit();
            }
        }
    }
}
