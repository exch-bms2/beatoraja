package bms.player.beatoraja.launcher;

import bms.player.beatoraja.external.DesktopActions;
import bms.player.beatoraja.song.archive.SongArchives;

import java.net.URL;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import bms.player.beatoraja.TableData;
import bms.player.beatoraja.LocalTableFile;
import bms.player.beatoraja.TableData.TableFolder;
import bms.player.beatoraja.song.SongData;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.Scene;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


import java.util.regex.Pattern;

public class TableEditorView implements Initializable {

	private static final Pattern hexadecimalPattern = Pattern.compile("[0-9a-fA-F]*");

	private TableData loaded;

	@FXML
	private TextField tableName;

	@FXML
	private CourseEditorView courseController;
	@FXML
	private FolderEditorView folderController;

	public void initialize(URL arg0, ResourceBundle arg1) {
		
	}
	
	protected void init(TableChartLookup songdb) {
		courseController.setSongDatabaseAccessor(songdb);
		folderController.init(songdb);
	}

	private void update(TableData td) {
		loaded = td;
		courseController.setCourseData(td.getCourse());
		folderController.setTableFolder(td.getFolder());
		tableName.setText(td.getName());
	}
	
	private TableData draft() {
		TableData td = new TableData();
		td.setUrl(loaded.getUrl());
		td.setTag(loaded.getTag());
		td.setName(tableName.getText());
		td.setCourse(courseController.getCourseData());
		td.setFolder(folderController.getTableFolder());
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
		stage.setScene(new Scene(root, 960, 640));
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
