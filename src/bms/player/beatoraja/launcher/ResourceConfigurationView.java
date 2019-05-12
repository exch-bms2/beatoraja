package bms.player.beatoraja.launcher;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import bms.player.beatoraja.Config;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TextField;
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
	private ListView<String> tableurl;
	@FXML
	private CheckBox updatesong;
	
	private PlayConfigurationView main;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
	}
	
	void init(PlayConfigurationView main) {
		this.main = main;
	}

    public void update(Config config) {
		bmsroot.getItems().setAll(config.getBmsroot());
		updatesong.setSelected(config.isUpdatesong());
		tableurl.getItems().setAll(config.getTableURL());
	}

	public void commit(Config config) {
		config.setBmsroot(bmsroot.getItems().toArray(new String[0]));
		config.setUpdatesong(updatesong.isSelected());
		config.setTableURL(tableurl.getItems().toArray(new String[0]));
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
			tableurl.getItems().add(url.getText());
		}
	}

    @FXML
	public void removeTableURL() {
		tableurl.getItems().removeAll(tableurl.getSelectionModel().getSelectedItems());
	}

	public void moveTableURLUp() {
		final int index = tableurl.getSelectionModel().getSelectedIndex();
		if(index > 0) {
			String table = tableurl.getSelectionModel().getSelectedItem();
			tableurl.getItems().remove(index);
			tableurl.getItems().add(index - 1, table);

			SelectionModel m = tableurl.getSelectionModel();
			m.select(index - 1);
			tableurl.setSelectionModel((MultipleSelectionModel)m);
		}
	}

	public void moveTableURLDown() {
		final int index = tableurl.getSelectionModel().getSelectedIndex();
		if(index >= 0 && index < tableurl.getItems().size() - 1) {
			String table = tableurl.getSelectionModel().getSelectedItem();
			tableurl.getItems().remove(index);
			tableurl.getItems().add(index + 1, table);

			SelectionModel m = tableurl.getSelectionModel();
			m.select(index + 1);
			tableurl.setSelectionModel((MultipleSelectionModel)m);
		}
	}

}
