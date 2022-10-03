package bms.player.beatoraja.launcher;

import java.awt.Desktop;
import java.io.IOException;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import bms.player.beatoraja.TableData;
import bms.player.beatoraja.TableData.TableFolder;
import bms.player.beatoraja.song.SongDatabaseAccessor;
import bms.player.beatoraja.song.SongData;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;


import java.util.regex.Pattern;

public class TableEditorView implements Initializable {

	private static final Pattern hexadecimalPattern = Pattern.compile("[0-9a-fA-F]*");

	private Path filepath;

	@FXML
	private TextField tableName;

	@FXML
	private CourseEditorView courseController;
	@FXML
	private FolderEditorView folderController;

	public void initialize(URL arg0, ResourceBundle arg1) {
		
	}
	
	protected void init(SongDatabaseAccessor songdb) {
		courseController.setSongDatabaseAccessor(songdb);
		folderController.init(songdb);
	}

	public void update(Path p) {
		TableData td = TableData.read(p);
		if(td == null) {
			td = new TableData();
			td.setName("New Table");
		}
		
		courseController.setCourseData(td.getCourse());
		folderController.setTableFolder(td.getFolder());
		tableName.setText(td.getName());
		filepath = p;
	}
	
	public void commit() {
		TableData td = new TableData();
		td.setName(tableName.getText());
		td.setCourse(courseController.getCourseData());
		td.setFolder(folderController.getTableFolder());
		
		TableData.write(filepath, td);
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

	protected static void displayChartDetailsDialog(SongDatabaseAccessor songdb, SongData song, String... extraData) {
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
			SongData[] foundSongs = songdb.getSongDatas(new String[]{song.getSha256()});
			if (foundSongs.length != 0) {
				song = foundSongs[0];
			} else {
				foundSongs = songdb.getSongDatas(new String[]{song.getMd5()});
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
					if (Desktop.isDesktopSupported()) {
						Desktop.getDesktop().open(Paths.get(songPath).getParent().toFile());
					}
				} catch (IOException|IllegalArgumentException e) {
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
