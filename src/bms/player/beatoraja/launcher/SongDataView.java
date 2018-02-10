package bms.player.beatoraja.launcher;

import java.net.URL;
import java.util.ResourceBundle;

import bms.player.beatoraja.song.SongData;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class SongDataView implements Initializable {

	@FXML
	private TableColumn<SongData, String> title;
	@FXML
	private TableColumn<SongData, String> artist;
	@FXML
	private TableColumn<SongData, Integer> mode;
	@FXML
	private TableColumn<SongData, Integer> notes;

	public void initialize(URL arg0, ResourceBundle arg1) {
		// タイトル
		title.setCellValueFactory(new PropertyValueFactory<SongData, String>("fullTitle"));
//		title.setCellFactory((arg) -> {
//			return new TableCell<SongData, String>() {
//
//			};
//		});
		artist.setCellValueFactory(new PropertyValueFactory<SongData, String>("fullArtist"));
		mode.setCellValueFactory(new PropertyValueFactory<SongData, Integer>("mode"));
		notes.setCellValueFactory(new PropertyValueFactory<SongData, Integer>("notes"));
	}
}
