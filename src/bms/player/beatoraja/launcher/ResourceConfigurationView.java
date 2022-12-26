package bms.player.beatoraja.launcher;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.List;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.TableDataAccessor;

import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;

public class ResourceConfigurationView implements Initializable {

	@FXML
	private ListView<String> bmsroot;
	@FXML
	private TextField url;
	@FXML
	private EditableTableView<TableInfo> tableurl;
	@FXML
	private CheckBox updatesong;

	private Config config;
	
	private PlayConfigurationView main;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
	}
	
	void init(PlayConfigurationView main) {
		this.main = main;

		TableColumn<TableInfo,String> urlColumn = new TableColumn<TableInfo,String>("URL");
		urlColumn.setCellValueFactory((p) -> p.getValue().urlProperty());
		urlColumn.setSortable(false);
		urlColumn.setMinWidth(560);
		urlColumn.setMinWidth(0);

		TableColumn<TableInfo,String> nameColumn = new TableColumn<TableInfo,String>("STATUS/NAME");
		nameColumn.setCellValueFactory((p) -> p.getValue().nameStatusProperty());
		nameColumn.setSortable(false);
		nameColumn.setMinWidth(200);
		nameColumn.setMinWidth(0);
	  
 		tableurl.getColumns().setAll(urlColumn, nameColumn);
 		tableurl.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}

    public void update(Config config) {
    	this.config = config;
		bmsroot.getItems().setAll(config.getBmsroot());
		updatesong.setSelected(config.isUpdatesong());
		TableInfo.populateList(tableurl.getItems(), config.getTableURL());
	}

	public void commit() {
		config.setBmsroot(bmsroot.getItems().toArray(new String[0]));
		config.setUpdatesong(updatesong.isSelected());
		config.setTableURL(TableInfo.toUrlArray(tableurl.getItems()));
	}

    @FXML
	public void refreshLocalTableInfo() {
		String[] urls = TableInfo.toUrlArray(tableurl.getItems());
		TableDataAccessor tda = new TableDataAccessor(config.getTablepath());
		HashMap<String,String> urlToTableNameMap = tda.readLocalTableNames(urls);
		for (TableInfo tableInfo : tableurl.getItems()) {
			String tableName = (urlToTableNameMap == null) ? null : urlToTableNameMap.get(tableInfo.getUrl());
			tableInfo.setNameStatus((tableName == null) ? "not loaded" : tableName);
		}
	}

    @FXML
	public void loadAllTables() {
		commit();
		try {
			Files.createDirectories(Paths.get(config.getTablepath()));
		} catch (IOException e) {
		}

		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(config.getTablepath()))) {
			paths.forEach((p) -> {
				if(p.toString().toLowerCase().endsWith(".bmt")) {
					try {
						Files.deleteIfExists(p);
					} catch (IOException e) {
					}
				}
			});
		} catch (IOException e) {
		}

		TableDataAccessor tda = new TableDataAccessor(config.getTablepath());
		tda.updateTableData(config.getTableURL());
		refreshLocalTableInfo();
	}

    @FXML
	public void loadSelectedTables() {
		commit();
		try {
			Files.createDirectories(Paths.get(config.getTablepath()));
		} catch (IOException e) {
		}

		TableDataAccessor tda = new TableDataAccessor(config.getTablepath());
		String[] urls = TableInfo.toUrlArray(tableurl.getSelectionModel().getSelectedItems());
		tda.updateTableData(urls);
		refreshLocalTableInfo();
	}

    @FXML
	public void loadNewTables() {
		commit();
		try {
			Files.createDirectories(Paths.get(config.getTablepath()));
		} catch (IOException e) {
		}

		TableDataAccessor tda = new TableDataAccessor(config.getTablepath());
		tda.loadNewTableData(config.getTableURL());
		refreshLocalTableInfo();
	}


    @FXML
	public void addSongPath() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("楽曲のルートフォルダを選択してください");
		File f = chooser.showDialog(null);
		if (f != null) {
			final String defaultPath = new File(".").getAbsoluteFile().getParent() + File.separatorChar;;
			String targetPath = f.getAbsolutePath();
			if(targetPath.startsWith(defaultPath)) {
				targetPath = f.getAbsolutePath().substring(defaultPath.length());
			}
			boolean unique = true;
			for (String path : bmsroot.getItems()) {
				if (path.equals(targetPath) || targetPath.startsWith(path + File.separatorChar)) {
					unique = false;
					break;
				}
			}
			if (unique) {
				bmsroot.getItems().add(targetPath);
				main.loadBMSPath(targetPath);
			}
		}
	}

    @FXML
	public void onSongPathDragOver(DragEvent ev) {
		Dragboard db = ev.getDragboard();
		if (db.hasFiles()) {
			ev.acceptTransferModes(TransferMode.COPY_OR_MOVE);
		}
		ev.consume();
	}

    @FXML
	public void songPathDragDropped(final DragEvent ev) {
		Dragboard db = ev.getDragboard();
		if (db.hasFiles()) {
			for (File f : db.getFiles()) {
				if (f.isDirectory()) {
					final String defaultPath = new File(".").getAbsoluteFile().getParent() + File.separatorChar;;
					String targetPath = f.getAbsolutePath();
					if(targetPath.startsWith(defaultPath)) {
						targetPath = f.getAbsolutePath().substring(defaultPath.length());
					}
					boolean unique = true;
					for (String path : bmsroot.getItems()) {
						if (path.equals(targetPath) || targetPath.startsWith(path + File.separatorChar)) {
							unique = false;
							break;
						}
					}
					if (unique) {
						bmsroot.getItems().add(targetPath);
						main.loadBMSPath(targetPath);
					}
				}
			}
		}
	}

    @FXML
	public void removeSongPath() {
		bmsroot.getItems().removeAll(bmsroot.getSelectionModel().getSelectedItems());
	}

    @FXML
	public void addTableURL() {
		String s = url.getText();
		if (s.startsWith("http") && !tableurl.getItems().contains(s)) {
			tableurl.addItem(new TableInfo(url.getText()));
		}
	}

    @FXML
	public void removeTableURL() {
		tableurl.removeSelectedItems();
	}

	public void moveTableURLUp() {
		tableurl.moveSelectedItemsUp();
	}

	public void moveTableURLDown() {
		tableurl.moveSelectedItemsDown();
	}

 	private static class TableInfo {
		public StringProperty url;
		public void setUrl(String value) { urlProperty().set(value); }
		public String getUrl() { return urlProperty().get(); }
		public StringProperty urlProperty() { 
			if (url == null) url = new SimpleStringProperty(this, "url");
			return url; 
		}
		public StringProperty nameStatus;
		public void setNameStatus(String value) { nameStatusProperty().set(value); }
		public String getNameStatus() { return nameStatusProperty().get(); }
		public StringProperty nameStatusProperty() { 
			if (nameStatus == null) nameStatus = new SimpleStringProperty(this, "nameStatus");
			return nameStatus; 
		}

		public TableInfo(String url) {
			setUrl(url);
			setNameStatus("");
		}

		public static String[] toUrlArray(List<TableInfo> list) {
			String[] urls = new String[list.size()];
			int i = 0;
			for (TableInfo tableInfo : list) {
				urls[i++] = tableInfo.getUrl();
			}
			return urls;
		}

		public static void populateList(List<TableInfo> list, String[] urls) {
			list.clear();
			for (String url : urls) {
				list.add(new TableInfo(url));
			}
		}
	}
}
