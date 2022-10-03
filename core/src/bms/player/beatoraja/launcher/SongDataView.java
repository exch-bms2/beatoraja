package bms.player.beatoraja.launcher;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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
	private TableColumn<SongData, String> genre;
	@FXML
	private TableColumn<SongData, Integer> mode;
	@FXML
	private TableColumn<SongData, Integer> notes;
	@FXML
	private TableColumn<SongData, Integer> level;
	@FXML
	private TableColumn<SongData, String> sha256;
	
	private Map<String, TableColumn> columnMap = new HashMap<String, TableColumn>();

	public void initialize(URL arg0, ResourceBundle arg1) {
		// タイトル
		initColumn(title, "fullTitle");
		initColumn(artist, "fullArtist");
		initColumn(genre, "genre");
		initColumn(mode, "mode");
		initColumn(notes, "notes");
		initColumn(level, "level");
		initColumn(sha256, "sha256");
	}
	
	private void initColumn(TableColumn column, String value) {
		column.setCellValueFactory(new PropertyValueFactory(value));		
		columnMap.put(value, column);
	}
	
	public void setVisible(String... values) {
		for(TableColumn column : columnMap.values()) {
			column.setVisible(false);
		}
		
		for(String value : values) {
			TableColumn column = columnMap.get(value);
			if(column != null) {
				column.setVisible(true);;
			}
		}
	}
}
