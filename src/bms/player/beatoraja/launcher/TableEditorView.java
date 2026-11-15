package bms.player.beatoraja.launcher;

import bms.player.beatoraja.external.DesktopActions;
import bms.player.beatoraja.song.archive.SongArchives;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.CourseData.CourseDataConstraint;
import static bms.player.beatoraja.CourseData.CourseDataConstraint.*;
import bms.player.beatoraja.TableData;
import bms.player.beatoraja.LocalTableFile;
import bms.player.beatoraja.TableData.TableFolder;
import bms.player.beatoraja.song.SongData;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.Scene;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


import java.util.regex.Pattern;

public class TableEditorView {

	private static final Pattern hexadecimalPattern = Pattern.compile("[0-9a-fA-F]*");

	private TableData loaded;
	private final ListView<Entry> entries = new ListView<>();
	private final TableView<SongData> charts = chartTable("editingCharts");
	private final TableView<SongData> results = chartTable("searchResults");
	private final TextField query = new TextField();
	private final Label searchStatus = new Label();
	private final java.util.concurrent.ExecutorService searchWorker = Executors.newSingleThreadExecutor(task -> {
		var thread = new Thread(task, "Table chart search");
		thread.setDaemon(true);
		return thread;
	});
	private Future<?> searchTask;
	private long searchVersion;

	@FXML
	private TextField tableName;
	@FXML private BorderPane workspace;

	private record Entry(TableFolder folder, CourseData course) {
		String name() { return folder != null ? folder.getName() : course.getName(); }
		void rename(String name) { if (folder != null) folder.setName(name); else course.setName(name); }
		SongData[] songs() { return folder != null ? folder.getSong() : course.getSong(); }
		void songs(SongData[] songs) { if (folder != null) folder.setSong(songs); else course.setSong(songs); }
	}

	private void init(TableChartLookup lookup) {
		entries.setId("tableEntries");
		entries.setCellFactory(list -> new ListCell<>() {
			@Override protected void updateItem(Entry entry, boolean empty) {
				super.updateItem(entry, empty);
				setText(empty || entry == null ? null : (entry.folder != null ? "Folder: " : "Course: ") + entry.name());
			}
		});
		var selected = entries.getSelectionModel().selectedItemProperty();
		selected.addListener((obs, before, after) -> {
			if (before != null) storeCharts(before);
			charts.getItems().clear();
			if (after != null) charts.getItems().setAll(after.songs());
		});
		var rename = button("Rename", () -> {
			var entry = selected.get();
			if (entry == null) return;
			var dialog = new TextInputDialog(entry.name());
			dialog.initOwner(workspace.getScene().getWindow());
			dialog.setTitle("Rename");
			dialog.setHeaderText(null);
			dialog.showAndWait().map(String::strip).filter(s -> !s.isEmpty()).ifPresent(name -> {
				entry.rename(name);
				entries.refresh();
			});
		});
		var removeEntry = button("Remove", () -> {
			var entry = selected.get();
			if (entry != null) entries.getItems().remove(entry);
		});
		rename.disableProperty().bind(selected.isNull());
		removeEntry.disableProperty().bind(selected.isNull());
		var addFolder = button("+ Folder", () -> {
			var folder = new TableFolder();
			folder.setName("New Folder");
			addEntry(new Entry(folder, null));
		});
		addFolder.setId("addFolder");
		var addCourse = button("+ Course", () -> {
			var course = new CourseData();
			course.setName("New Course");
			course.setTrophy(new CourseData.TrophyData[]{
					new CourseData.TrophyData("bronzemedal", 7.5f, 55),
					new CourseData.TrophyData("silvermedal", 5, 70),
					new CourseData.TrophyData("goldmedal", 2.5f, 85)});
			addEntry(new Entry(null, course));
		});
		addCourse.setId("addCourse");
		var left = pane("Folders and Courses", entries, new HBox(4, addFolder, addCourse),
				new HBox(4, rename, removeEntry), new HBox(4, button("Up", () -> moveEntry(-1)),
						button("Down", () -> moveEntry(1))));
		var settings = button("Course Settings", () -> editCourse(selected.get().course));
		settings.setId("courseSettings");
		selected.addListener((obs, before, after) -> {
			settings.setVisible(after != null && after.course != null);
			settings.setManaged(settings.isVisible());
		});
		settings.setVisible(false);
		settings.setManaged(false);
		var removeChart = button("Remove", () -> {
			var indices = new ArrayList<>(charts.getSelectionModel().getSelectedIndices());
			for (int i = indices.size() - 1; i >= 0; i--) charts.getItems().remove((int) indices.get(i));
		});
		removeChart.disableProperty().bind(charts.getSelectionModel().selectedItemProperty().isNull());
		var drag = new TableEditorChartDrag(charts, results, selected,
				(songs, index) -> insertCharts(charts.getItems(), songs, index, selected.get().course != null));
		var center = pane("Charts", drag, new HBox(4, removeChart, button("Up", () -> moveChart(-1)),
				button("Down", () -> moveChart(1)), settings));

		query.setId("chartQuery");
		query.setPromptText("Title, artist or hash");
		HBox.setHgrow(query, Priority.ALWAYS);
		var search = button("Search", () -> search(lookup));
		search.setId("searchCharts");
		query.setOnAction(event -> search(lookup));
		var add = button("Add Selected", this::addSelectedCharts);
		add.setId("addSelectedCharts");
		add.disableProperty().bind(selected.isNull().or(results.getSelectionModel().selectedItemProperty().isNull()));
		var right = pane("Search Results", results, new HBox(4, add, searchStatus));
		right.getChildren().add(1, new HBox(4, query, search));
		var split = new SplitPane(left, center, right);
		split.setId("tableEditorPanes");
		split.setDividerPositions(0.23, 0.59);
		workspace.setCenter(split);
	}

	private static VBox pane(String title, javafx.scene.Node list, javafx.scene.Node... controls) {
		var pane = new VBox(6, new Label(title), list);
		pane.setPadding(new Insets(8));
		pane.setMinWidth(0);
		pane.getChildren().addAll(controls);
		VBox.setVgrow(list, Priority.ALWAYS);
		return pane;
	}

	private static Button button(String title, Runnable action) {
		var button = new Button(title);
		button.setOnAction(event -> action.run());
		return button;
	}

	private static TableView<SongData> chartTable(String id) {
		var table = new TableView<SongData>();
		table.setId(id);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.getColumns().add(column("Title", SongData::getFullTitle));
		table.getColumns().add(column("Artist", SongData::getFullArtist));
		table.getColumns().add(column("Mode / Level", song -> song.getMode() + " / " + song.getLevel()));
		return table;
	}

	private static TableColumn<SongData, String> column(String title, Function<SongData, String> value) {
		var column = new TableColumn<SongData, String>(title);
		column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
		column.setSortable(false);
		return column;
	}

	private void addEntry(Entry entry) {
		int index = entries.getItems().size();
		if (entry.folder != null) {
			index = 0;
			while (index < entries.getItems().size() && entries.getItems().get(index).folder != null) index++;
		}
		entries.getItems().add(index, entry);
		entries.getSelectionModel().select(entry);
	}

	private void storeCharts(Entry entry) { entry.songs(charts.getItems().toArray(SongData[]::new)); }

	private void moveEntry(int delta) {
		var entry = entries.getSelectionModel().getSelectedItem();
		if (entry == null) return;
		int index = entries.getSelectionModel().getSelectedIndex(), next = index + delta;
		if (next < 0 || next >= entries.getItems().size()
				|| (entry.folder == null) != (entries.getItems().get(next).folder == null)) return;
		storeCharts(entry);
		entries.getSelectionModel().clearSelection();
		entries.getItems().remove(index);
		entries.getItems().add(next, entry);
		entries.getSelectionModel().select(entry);
	}

	private void moveChart(int delta) {
		int index = charts.getSelectionModel().getSelectedIndex();
		int next = index + delta;
		if (index < 0 || next < 0 || next >= charts.getItems().size()) return;
		var song = charts.getItems().remove(index);
		charts.getItems().add(next, song);
		charts.getSelectionModel().clearAndSelect(next);
	}

	private void addSelectedCharts() {
		var entry = entries.getSelectionModel().getSelectedItem();
		if (entry == null) return;
		insertCharts(charts.getItems(), TableEditorChartDrag.selectedRows(results).stream()
				.map(results.getItems()::get).toList(), charts.getItems().size(), entry.course != null);
	}

	static void insertCharts(List<SongData> target, List<SongData> songs, int index, boolean allowDuplicates) {
		var additions = new ArrayList<SongData>();
		for (var song : songs) {
			if (allowDuplicates || (target.stream().noneMatch(existing -> sameChart(existing, song))
					&& additions.stream().noneMatch(existing -> sameChart(existing, song)))) additions.add(song);
		}
		target.addAll(index, additions);
	}

	private static boolean sameChart(SongData first, SongData second) {
		if (hasHash(first.getSha256()) && hasHash(second.getSha256()))
			return first.getSha256().equalsIgnoreCase(second.getSha256());
		return hasHash(first.getMd5()) && first.getMd5().equalsIgnoreCase(second.getMd5());
	}

	private static boolean hasHash(String hash) { return hash != null && !hash.isEmpty(); }

	private void search(TableChartLookup lookup) {
		long version = ++searchVersion;
		if (searchTask != null) searchTask.cancel(true);
		String text = query.getText().strip();
		results.getItems().clear();
		searchStatus.setText(text.isEmpty() ? "" : "Searching...");
		if (text.isEmpty()) return;
		searchTask = searchWorker.submit(() -> {
			var found = isMd5OrSha256Hash(text) ? lookup.byHash(text) : lookup.byText(text);
			Platform.runLater(() -> {
				if (searchVersion != version) return;
				results.getItems().setAll(found);
				searchStatus.setText(found.length + " found");
			});
		});
	}

	private void closeSearch() {
		searchVersion++;
		searchWorker.shutdownNow();
	}

	private void editCourse(CourseData course) {
		var dialog = new Dialog<ButtonType>();
		dialog.initOwner(workspace.getScene().getWindow());
		dialog.setTitle("Course Settings");
		dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
		var grid = new GridPane();
		grid.setHgap(8);
		grid.setVgap(6);
		var release = new CheckBox("Release");
		release.setSelected(course.isRelease());
		grid.add(release, 1, 0);
		var groups = List.of(List.of(CLASS, MIRROR, RANDOM), List.of(NO_SPEED), List.of(NO_GREAT, NO_GOOD),
				List.of(GAUGE_LR2, GAUGE_5KEYS, GAUGE_7KEYS, GAUGE_9KEYS, GAUGE_24KEYS), List.of(LN, CN, HCN));
		var labels = List.of("Random", "HI-SPEED", "Judge", "Gauge", "LN Mode");
		var choices = new ArrayList<ComboBox<String>>();
		var originals = new ArrayList<String>();
		for (int i = 0; i < groups.size(); i++) {
			var group = groups.get(i);
			var box = new ComboBox<String>();
			box.getItems().add("None");
			for (var constraint : group) box.getItems().add(constraint.name());
			String original = java.util.Arrays.stream(course.getConstraint()).filter(group::contains)
					.findFirst().map(Enum::name).orElse("None");
			box.setValue(original);
			choices.add(box);
			originals.add(original);
			grid.addRow(i + 1, new Label(labels.get(i)), box);
		}
		int row = groups.size() + 1;
		grid.addRow(row++, new Label("Trophy"), new Label("Miss Rate"), new Label("Score Rate"));
		var trophies = course.getTrophy();
		var miss = new ArrayList<TextField>();
		var score = new ArrayList<TextField>();
		for (var trophy : trophies) {
			var missField = new TextField(Float.toString(trophy.getMissrate()));
			var scoreField = new TextField(Float.toString(trophy.getScorerate()));
			miss.add(missField);
			score.add(scoreField);
			grid.addRow(row++, new Label(trophy.getName()), missField, scoreField);
		}
		var error = new Label();
		grid.add(error, 0, row, 3, 1);
		var scroll = new ScrollPane(grid);
		scroll.setFitToWidth(true);
		dialog.getDialogPane().setContent(scroll);
		dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
			try {
				var updated = new CourseData.TrophyData[trophies.length];
				for (int i = 0; i < trophies.length; i++) {
					updated[i] = new CourseData.TrophyData(trophies[i].getName(),
							parseRate(miss.get(i)), parseRate(score.get(i)));
				}
				var constraints = new ArrayList<>(java.util.Arrays.asList(course.getConstraint()));
				for (int i = 0; i < choices.size(); i++) {
					String value = choices.get(i).getValue();
					if (originals.get(i).equals(value)) continue;
					constraints.removeAll(groups.get(i));
					if (!"None".equals(value)) constraints.add(CourseDataConstraint.valueOf(value));
				}
				course.setRelease(release.isSelected());
				course.setConstraint(constraints.toArray(CourseDataConstraint[]::new));
				course.setTrophy(updated);
			} catch (NumberFormatException e) {
				error.setText("Trophy rates must be between 0 and 100.");
				event.consume();
			}
		});
		dialog.showAndWait();
	}

	private static float parseRate(TextField field) {
		float value = Float.parseFloat(field.getText());
		if (!Float.isFinite(value) || value < 0 || value > 100) throw new NumberFormatException();
		return value;
	}

	private void update(TableData td) {
		loaded = td;
		for (var folder : td.getFolder()) entries.getItems().add(new Entry(folder, null));
		for (var course : td.getCourse()) entries.getItems().add(new Entry(null, course));
		entries.getSelectionModel().selectFirst();
		tableName.setText(td.getName());
	}
	
	private TableData draft() {
		TableData td = new TableData();
		td.setUrl(loaded.getUrl());
		td.setTag(loaded.getTag());
		td.setName(tableName.getText());
		var selected = entries.getSelectionModel().getSelectedItem();
		if (selected != null) storeCharts(selected);
		td.setCourse(entries.getItems().stream().map(Entry::course).filter(java.util.Objects::nonNull).toArray(CourseData[]::new));
		td.setFolder(entries.getItems().stream().map(Entry::folder).filter(java.util.Objects::nonNull).toArray(TableFolder[]::new));
		return td;
	}

	static Stage open(Path path, TableChartLookup songdb) throws IOException {
		var file = new LocalTableFile(path);
		TableData data = file.load();
		var loader = new FXMLLoader(TableEditorView.class.getResource("TableEditorView.fxml"),
				ResourceBundle.getBundle("resources.UIResources"));
		var root = new BorderPane();
		root.setCenter(loader.load());
		TableEditorView editor = loader.getController();
		editor.init(songdb);
		editor.update(data);
		var saved = new String[]{LocalTableFile.snapshot(editor.draft())};

		// No owner: hiding the launcher must not hide this window.
		var stage = new Stage();
		stage.setTitle("Table Editor");
		var save = new Button("Save");
		var cancel = new Button("Cancel");
		var status = new Label();
		var controls = new HBox(10, save, cancel, status);
		controls.setAlignment(Pos.CENTER_LEFT);
		controls.setPadding(new Insets(8));
		root.setBottom(controls);
		save.setOnAction(event -> {
			try {
				var draft = editor.draft();
				file.save(draft);
				saved[0] = LocalTableFile.snapshot(draft);
				status.setText("Saved. Select the local table (or COURSE on first load) and press F2.");
			} catch (IOException | RuntimeException e) {
				var alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
				alert.initOwner(stage);
				alert.showAndWait();
			}
		});
		cancel.setOnAction(event -> stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));
		stage.setOnCloseRequest(event -> {
			if (saved[0].equals(LocalTableFile.snapshot(editor.draft()))) return;
			var discard = new ButtonType("Discard", ButtonData.OK_DONE);
			var alert = new Alert(Alert.AlertType.CONFIRMATION, "Discard unsaved table changes?", discard, ButtonType.CANCEL);
			alert.initOwner(stage);
			if (alert.showAndWait().orElse(ButtonType.CANCEL) != discard) event.consume();
		});
		stage.setOnHidden(event -> editor.closeSearch());
		stage.setScene(new Scene(root, 1180, 680));
		stage.show();
		return stage;
	}

	public static boolean isMd5OrSha256Hash(String text) {
		return (text.length() == 32 || text.length() == 64) && hexadecimalPattern.matcher(text).matches();
	}

	private static void dialogAddCopiableRow(GridPane gridPane, int row, String labelText, String dataText) {
		Label label = new Label(labelText + ": ");
		TextField textField = new TextField(dataText);
		textField.setEditable(false);
		gridPane.add(label, 0, row, 1, 1);
		gridPane.add(textField, 1, row, 2, 1);
	}

	protected static void displayChartDetailsDialog(TableChartLookup songdb, SongData song, String... extraData) {
		if (song == null) return;

		GridPane gridPane = new GridPane();
		gridPane.getColumnConstraints().addAll(
			new ColumnConstraints(90),
			new ColumnConstraints(300),
			new ColumnConstraints(85)
		);
		gridPane.setMaxWidth(Double.MAX_VALUE);

		dialogAddCopiableRow(gridPane, 0, "Title", song.getFullTitle());
		dialogAddCopiableRow(gridPane, 1, "Artist", song.getFullArtist());
		dialogAddCopiableRow(gridPane, 2, "Genre", song.getGenre());

		dialogAddCopiableRow(gridPane, 3, "MD5 Hash", song.getMd5());
		dialogAddCopiableRow(gridPane, 4, "SHA256 Hash", song.getSha256());

		if (song.getPath() == null && songdb != null) {
			// Try to find actual song in songdb, if songdata was not retrieved from songdb
			SongData[] foundSongs = songdb.byHash(song.getSha256());
			if (foundSongs.length != 0) {
				song = foundSongs[0];
			} else {
				foundSongs = songdb.byHash(song.getMd5());
				if (foundSongs.length != 0) {
					song = foundSongs[0];
				}
			}
		}

		if (song.getPath() != null) {
			// Display song details, if song can be found in songdb (otherwise details are inaccessible)
			String levelString = "UNKNOWN";
			switch(song.getDifficulty()) {
				case 1: {levelString = "BEGINNER"; break;}
				case 2: {levelString = "NORMAL"; break;}
				case 3: {levelString = "HYPER"; break;}
				case 4: {levelString = "ANOTHER"; break;}
				case 5: {levelString = "INSANE"; break;}
			}

			String judgeString;
			int judgeRank = song.getJudge();
			if (judgeRank <= 25) {judgeString = "VERY HARD";}
			else if (judgeRank <= 50) {judgeString = "HARD";}
			else if (judgeRank <= 75) {judgeString = "NORMAL";}
			else if (judgeRank <= 100) {judgeString = "EASY";}
			else {judgeString = "VERY EASY";}

			String bpmString;
			if (song.getMinbpm() == song.getMaxbpm()) {
				bpmString = String.format("%dbpm", song.getMaxbpm());
			} else {
				bpmString = String.format("%d-%dbpm", song.getMinbpm(), song.getMaxbpm());
			}

			String timeString = String.format("%d:%02d", song.getLength()/60000, (song.getLength()/1000)%60);


			Label detailsLabel = new Label(String.format("%dkeys / %s %d / %d notes / %s / %s / %s",
					song.getMode(), levelString, song.getLevel(), song.getNotes(), timeString, bpmString, judgeString));
			detailsLabel.setAlignment(Pos.CENTER);
			detailsLabel.setMaxWidth(Double.MAX_VALUE);
			gridPane.add(detailsLabel, 0, 5, 2, 1);

			Button openFolderButton = new Button("Open Folder");
			openFolderButton.setMaxWidth(Double.MAX_VALUE);
			String songPath = song.getPath();
			openFolderButton.setOnAction((actionEvent) -> {
				try {
					var path = Paths.get(songPath).toAbsolutePath();
					var archive = SongArchives.archivePath(path);
					DesktopActions.openPath((archive != null ? archive : path).getParent());
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			});
			gridPane.add(openFolderButton, 2, 5, 1, 1);
		}

		for (int i = 0; i < extraData.length; i++) {
			Label extraLabel = new Label(extraData[i]);
			extraLabel.setAlignment(Pos.CENTER);
			extraLabel.setMaxWidth(Double.MAX_VALUE);
			gridPane.add(extraLabel, 0, 6+i, 3, 1);
		}

		Dialog dialog = new Dialog();
		dialog.setTitle("Chart Details");
		dialog.getDialogPane().setMinWidth(500);
		dialog.getDialogPane().setMaxWidth(500);
		dialog.getDialogPane().setContent(gridPane);
		dialog.getDialogPane().getButtonTypes().add(new ButtonType("OK", ButtonData.CANCEL_CLOSE));
		dialog.show();
	}
}
