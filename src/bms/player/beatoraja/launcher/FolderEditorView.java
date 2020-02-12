package bms.player.beatoraja.launcher;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.TableData.TableFolder;
import bms.player.beatoraja.song.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.GridPane;

public class FolderEditorView implements Initializable {

	@FXML
	private TextField search;
	@FXML
	private TableView<SongData> searchSongs;
	@FXML
	private SongDataView searchSongsController;

	@FXML
	private ListView<TableFolder> folders;
	@FXML
	private GridPane folderPane;	
	@FXML
	private TextField folderName;
	@FXML
	private TableView<SongData> folderSongs;
	@FXML
	private SongDataView folderSongsController;

	private Path filepath;
	
	private TableFolder selectedFolder;
	
	private SongDatabaseAccessor songdb;
	
	private CourseData[] courses;
	
	public void initialize(URL arg0, ResourceBundle arg1) {		
		folders.getSelectionModel().selectedIndexProperty().addListener((observable, oldVal, newVal) -> {
			if(oldVal != newVal) {
				updateTableFolder();				
			}
		});
		folders.setCellFactory((ListView) -> {
			return new TextFieldListCell<TableFolder>() {
				@Override
				public void updateItem(TableFolder course, boolean empty) {
					super.updateItem(course, empty);
					setText(empty ? "" : course.getName());
				}
			};
		});
		folderSongsController.setVisible("fullTitle", "sha256");
		searchSongsController.setVisible("fullTitle", "fullArtist", "mode", "level", "notes", "sha256");

		searchSongs.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		updateFolder(null);
	}
	
	protected void init(SongDatabaseAccessor songdb) {
		this.songdb = songdb;
	}

	public void searchSongs() {
		if(songdb == null) {
			return;
		}
		if(search.getText().length() > 1) {
			searchSongs.getItems().setAll(songdb.getSongDatasByText(search.getText()));			
		}
	}

	public void updateTableFolder() {
		commitFolder();
		updateFolder(folders.getSelectionModel().getSelectedItem());
	}
	
	private void commitFolder() {
		if(selectedFolder == null) {
			return;
		}
		selectedFolder.setName(folderName.getText());		
		selectedFolder.setSong(folderSongs.getItems().toArray(new SongData[folderSongs.getItems().size()]));		
	}

	private void updateFolder(TableFolder course) {
		selectedFolder = course;
		if(selectedFolder == null) {
			folderPane.setVisible(false);
			return;
		}
		folderPane.setVisible(true);
		
		folderName.setText(selectedFolder.getName());
		folderSongs.getItems().setAll(course.getSong());
	}
	
	public void addTableFolder() {
		TableFolder course = new TableFolder();
		course.setName("New Folder");
		folders.getItems().add(course);
	}

	public void removeTableFolder() {
		TableFolder song = folders.getSelectionModel().getSelectedItem();
		if(song != null) {
			folders.getItems().remove(song);
		}
	}
 
	public void moveTableFolderUp() {
		final int index = folders.getSelectionModel().getSelectedIndex();
		if(index > 0) {
			TableFolder song = folders.getSelectionModel().getSelectedItem();
			folders.getItems().remove(index);
			folders.getItems().add(index - 1, song);
			folders.getSelectionModel().select(index - 1);
		}
	}

	public void moveTableFolderDown() {
		final int index = folders.getSelectionModel().getSelectedIndex();
		if(index >= 0 && index < folders.getItems().size() - 1) {
			TableFolder song = folders.getSelectionModel().getSelectedItem();
			folders.getItems().remove(index);
			folders.getItems().add(index + 1, song);
			folders.getSelectionModel().select(index + 1);
		}
	}
	
	public void addSongData() {
		List<SongData> songs = searchSongs.getSelectionModel().getSelectedItems();
		for (SongData song : songs) {
			if (song != null) {
				folderSongs.getItems().add(song);
			}
		}
	}

	public void removeSongData() {
		SongData song = folderSongs.getSelectionModel().getSelectedItem();
		if(song != null) {
			folderSongs.getItems().remove(song);
		}
	}
 
	public void moveSongDataUp() {
		final int index = folderSongs.getSelectionModel().getSelectedIndex();
		if(index > 0) {
			SongData song = folderSongs.getSelectionModel().getSelectedItem();
			folderSongs.getItems().remove(index);
			folderSongs.getItems().add(index - 1, song);
			folderSongs.getSelectionModel().select(index - 1);
		}
	}

	public void moveSongDataDown() {
		final int index = folderSongs.getSelectionModel().getSelectedIndex();
		if(index >= 0 && index < folderSongs.getItems().size() - 1) {
			SongData song = folderSongs.getSelectionModel().getSelectedItem();
			folderSongs.getItems().remove(index);
			folderSongs.getItems().add(index + 1, song);
			folderSongs.getSelectionModel().select(index + 1);
		}
	}
	
	public TableFolder[] getTableFolder() {
		commitFolder();
		return folders.getItems().toArray(new TableFolder[folders.getItems().size()]);
	}
	
	public void setTableFolder(TableFolder[] folder) {
		folders.getItems().setAll(folder);
	}
}
