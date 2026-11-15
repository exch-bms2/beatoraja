package bms.player.beatoraja.launcher;

import bms.player.beatoraja.song.SongData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ObjIntConsumer;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;

final class TableEditorChartDrag extends StackPane {
    private static final DataFormat CHART_DRAG = new DataFormat("application/x-beatoraja-editor-charts");
    private final TableView<SongData> charts;
    private final ObservableValue<?> destination;
    private final ObjIntConsumer<List<SongData>> insert;
    private final Line marker = new Line();
    private Gesture gesture;

    private record Gesture(boolean move, List<Integer> rows, List<SongData> songs) {}
    record Position(int index, double sceneY) {}

    TableEditorChartDrag(TableView<SongData> charts, TableView<SongData> results,
                         ObservableValue<?> destination, ObjIntConsumer<List<SongData>> insert) {
        this.charts = charts;
        this.destination = destination;
        this.insert = insert;
        setId("chartDragTarget");
        marker.setId("chartInsertionLine");
        marker.setStyle("-fx-stroke: -fx-accent; -fx-stroke-width: 2;");
        marker.setManaged(false);
        marker.setMouseTransparent(true);
        marker.setVisible(false);
        getChildren().addAll(charts, marker);
        installSource(charts);
        installSource(results);
        destination.addListener((obs, old, current) -> clear());
        charts.getItems().addListener((ListChangeListener<SongData>) change -> clear());
        charts.setOnDragOver(event -> {
            var position = position(event);
            if (position == null) {
                marker.setVisible(false);
                return;
            }
            event.acceptTransferModes(gesture.move ? TransferMode.MOVE : TransferMode.COPY);
            showMarker(position);
            event.consume();
        });
        charts.setOnDragExited(event -> marker.setVisible(false));
        charts.setOnDragDropped(event -> {
            var position = position(event);
            event.setDropCompleted(position != null && drop(position.index));
            clear();
            event.consume();
        });
    }

    private void installSource(TableView<SongData> table) {
        table.setRowFactory(ignored -> {
            var row = new TableRow<SongData>();
            row.setOnDragDetected(event -> {
                if (row.isEmpty() || destination.getValue() == null) return;
                if (!table.getSelectionModel().isSelected(row.getIndex()))
                    table.getSelectionModel().clearAndSelect(row.getIndex());
                if (!capture(table)) return;
                var board = row.startDragAndDrop(gesture.move ? TransferMode.MOVE : TransferMode.COPY);
                var content = new ClipboardContent();
                content.put(CHART_DRAG, Boolean.TRUE);
                board.setContent(content);
                event.consume();
            });
            row.setOnDragDone(event -> clear());
            return row;
        });
    }

    static List<Integer> selectedRows(TableView<?> table) {
        return table.getSelectionModel().getSelectedIndices().stream().sorted().toList();
    }

    boolean capture(TableView<SongData> source) {
        clear();
        var rows = selectedRows(source);
        if (destination.getValue() == null || rows.isEmpty()) return false;
        gesture = new Gesture(source == charts, rows, rows.stream().map(source.getItems()::get).toList());
        return true;
    }

    boolean drop(int index) {
        var captured = gesture;
        clear();
        if (captured == null || index < 0 || index > charts.getItems().size()) return false;
        if (!captured.move) {
            insert.accept(captured.songs, index);
            return true;
        }
        var items = new ArrayList<>(charts.getItems());
        int insertion = index;
        // Indexes identify occurrences even when a course contains identical charts.
        for (int i = captured.rows.size() - 1; i >= 0; i--) {
            int row = captured.rows.get(i);
            items.remove(row);
            if (row < index) insertion--;
        }
        items.addAll(insertion, captured.songs);
        charts.getItems().setAll(items);
        charts.getSelectionModel().clearSelection();
        for (int i = 0; i < captured.rows.size(); i++) charts.getSelectionModel().select(insertion + i);
        return true;
    }

    private Position position(DragEvent event) {
        if (gesture == null || !event.getDragboard().hasContent(CHART_DRAG)) return null;
        for (Node node = event.getPickResult().getIntersectedNode(); node != null && node != charts; node = node.getParent()) {
            if (node instanceof TableRow<?> row) return rowPosition(row, event.getSceneY(), charts.getItems().size());
            if (node instanceof javafx.scene.control.ScrollBar) return null;
        }
        var header = charts.lookup(".column-header-background");
        if (header == null) return null;
        double top = header.localToScene(header.getBoundsInLocal()).getMaxY();
        if (event.getSceneY() < top) return null;
        for (Node node : charts.lookupAll(".table-row-cell")) {
            if (node instanceof TableRow<?> row && row.getIndex() == charts.getItems().size() - 1 && !row.isEmpty()) {
                top = Math.max(top, row.localToScene(row.getBoundsInLocal()).getMaxY());
                break;
            }
        }
        return new Position(charts.getItems().size(), top);
    }

    static Position rowPosition(TableRow<?> row, double sceneY, int size) {
        var bounds = row.localToScene(row.getBoundsInLocal());
        if (row.isEmpty()) return new Position(size, bounds.getMinY());
        boolean below = sceneY >= (bounds.getMinY() + bounds.getMaxY()) / 2;
        return new Position(row.getIndex() + (below ? 1 : 0), below ? bounds.getMaxY() : bounds.getMinY());
    }

    void showMarker(Position position) {
        double y = sceneToLocal(0, position.sceneY).getY();
        marker.setStartX(1);
        marker.setEndX(Math.max(1, getWidth() - 2));
        marker.setStartY(y);
        marker.setEndY(y);
        marker.setVisible(true);
    }

    private void clear() {
        gesture = null;
        marker.setVisible(false);
    }
}
