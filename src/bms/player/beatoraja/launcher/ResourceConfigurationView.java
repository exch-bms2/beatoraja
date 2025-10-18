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

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
		ResourceBundle bundle = ResourceBundle.getBundle("resources.UIResources");
		// Functions in ResourceConfigurationView are not run on the JavaFX Application Thread and so this little
		// workaround has to be performed here to allow the progress bar to function as expected.
		final Stage[] loadingBarStage = new Stage[1];
		Runnable progressRunnable = () -> {
			// JavaFX UI code must be run inside a Platform run context
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					loadingBarStage[0] = new Stage();
					loadingBarStage[0].setResizable(false);
					// This modality freezes the launcher/primary stage
					loadingBarStage[0].initModality(Modality.APPLICATION_MODAL);
					loadingBarStage[0].setTitle(bundle.getString("PROGRESS_TABLE_TITLE"));
					// This prevents users from seeing typical windowing system buttons
					loadingBarStage[0].initStyle(StageStyle.UNDECORATED);

					ProgressBar progressBar = new ProgressBar();
					progressBar.setPrefWidth(300);

					Label messageLabel = new Label(bundle.getString("PROGRESS_TABLE_LABEL"));

					VBox root = new VBox(10);
					root.setStyle("-fx-padding: 20; -fx-alignment: center;");
					root.getChildren().addAll(messageLabel, progressBar);

					Scene scene = new Scene(root);
					loadingBarStage[0].setScene(scene);

					// Prevents closing. This has the side effect of preventing windowing system close requests but
					// the application can still be force killed by the user if necessary
					loadingBarStage[0].setOnCloseRequest(Event::consume);
					loadingBarStage[0].show();
				}
			});
		};

		Runnable loadTableRunnable = () -> {
			String[] urls = TableInfo.toUrlArray(tableurl.getItems());
			TableDataAccessor tda = new TableDataAccessor(config.getTablepath());
			HashMap<String,String> urlToTableNameMap = tda.readLocalTableNames(urls);
			for (TableInfo tableInfo : tableurl.getItems()) {
				String tableName = (urlToTableNameMap == null) ? null : urlToTableNameMap.get(tableInfo.getUrl());
				tableInfo.setNameStatus((tableName == null) ? "not loaded" : tableName);
			}

			// Once again, JavaFX UI code must be run inside a Platform context. Hide progress bar and resume
			// normal launcher behaviour
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					loadingBarStage[0].hide();
				}
			});
		};

		new Thread(progressRunnable).start();
		new Thread(loadTableRunnable).start();
	}

    @FXML
	public void loadAllTables() {
		commit();
		ResourceBundle bundle = ResourceBundle.getBundle("resources.UIResources");
		try {
			Files.createDirectories(Paths.get(config.getTablepath()));
		} catch (IOException e) {
		}

		final Stage[] loadingBarStage = new Stage[1];
		Runnable progressRunnable = () -> {
			// JavaFX UI code must be run inside a Platform run context
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					loadingBarStage[0] = new Stage();
					loadingBarStage[0].setResizable(false);
					// This modality freezes the launcher/primary stage
					loadingBarStage[0].initModality(Modality.APPLICATION_MODAL);
					loadingBarStage[0].setTitle(bundle.getString("PROGRESS_TABLE_TITLE"));
					// This prevents users from seeing typical windowing system buttons
					loadingBarStage[0].initStyle(StageStyle.UNDECORATED);

					ProgressBar progressBar = new ProgressBar();
					progressBar.setPrefWidth(300);

					Label messageLabel = new Label(bundle.getString("PROGRESS_TABLE_LABEL"));

					VBox root = new VBox(10);
					root.setStyle("-fx-padding: 20; -fx-alignment: center;");
					root.getChildren().addAll(messageLabel, progressBar);

					Scene scene = new Scene(root);
					loadingBarStage[0].setScene(scene);

					// Prevents closing. This has the side effect of preventing windowing system close requests but
					// the application can still be force killed by the user if necessary
					loadingBarStage[0].setOnCloseRequest(Event::consume);
					loadingBarStage[0].show();
				}
			});
		};

		Runnable loadTableRunnable = () -> {
			try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(config.getTablepath()))) {
				paths.forEach((p) -> {
					if(p.toString().toLowerCase().endsWith(".bmt")) {
						try {
							Files.deleteIfExists(p);
						} catch (IOException ignored) {
						}
					}
				});
			} catch (IOException ignored) {
			}

			TableDataAccessor tda = new TableDataAccessor(config.getTablepath());
			tda.updateTableData(config.getTableURL());
			refreshLocalTableInfo();

			// Once again, JavaFX UI code must be run inside a Platform context. Hide progress bar and resume
			// normal launcher behaviour
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					loadingBarStage[0].hide();
				}
			});
		};

		new Thread(progressRunnable).start();
		new Thread(loadTableRunnable).start();
	}

    @FXML
	public void loadSelectedTables() {
		commit();
		ResourceBundle bundle = ResourceBundle.getBundle("resources.UIResources");
		try {
			Files.createDirectories(Paths.get(config.getTablepath()));
		} catch (IOException e) {
		}

		final Stage[] loadingBarStage = new Stage[1];
		Runnable progressRunnable = () -> {
			// JavaFX UI code must be run inside a Platform run context
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					loadingBarStage[0] = new Stage();
					loadingBarStage[0].setResizable(false);
					// This modality freezes the launcher/primary stage
					loadingBarStage[0].initModality(Modality.APPLICATION_MODAL);
					loadingBarStage[0].setTitle(bundle.getString("PROGRESS_TABLE_TITLE"));
					// This prevents users from seeing typical windowing system buttons
					loadingBarStage[0].initStyle(StageStyle.UNDECORATED);

					ProgressBar progressBar = new ProgressBar();
					progressBar.setPrefWidth(300);

					Label messageLabel = new Label(bundle.getString("PROGRESS_TABLE_LABEL"));

					VBox root = new VBox(10);
					root.setStyle("-fx-padding: 20; -fx-alignment: center;");
					root.getChildren().addAll(messageLabel, progressBar);

					Scene scene = new Scene(root);
					loadingBarStage[0].setScene(scene);

					// Prevents closing. This has the side effect of preventing windowing system close requests but
					// the application can still be force killed by the user if necessary
					loadingBarStage[0].setOnCloseRequest(Event::consume);
					loadingBarStage[0].show();
				}
			});
		};

		Runnable loadTableRunnable = () -> {
			TableDataAccessor tda = new TableDataAccessor(config.getTablepath());
			String[] urls = TableInfo.toUrlArray(tableurl.getSelectionModel().getSelectedItems());
			tda.updateTableData(urls);
			refreshLocalTableInfo();

			// Once again, JavaFX UI code must be run inside a Platform context. Hide progress bar and resume
			// normal launcher behaviour
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					loadingBarStage[0].hide();
				}
			});
		};

		new Thread(progressRunnable).start();
		new Thread(loadTableRunnable).start();
	}

    @FXML
	public void loadNewTables() {
		commit();
		ResourceBundle bundle = ResourceBundle.getBundle("resources.UIResources");
		try {
			Files.createDirectories(Paths.get(config.getTablepath()));
		} catch (IOException e) {
		}

		final Stage[] loadingBarStage = new Stage[1];
		Runnable progressRunnable = () -> {
			// JavaFX UI code must be run inside a Platform run context
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					loadingBarStage[0] = new Stage();
					loadingBarStage[0].setResizable(false);
					// This modality freezes the launcher/primary stage
					loadingBarStage[0].initModality(Modality.APPLICATION_MODAL);
					loadingBarStage[0].setTitle(bundle.getString("PROGRESS_TABLE_TITLE"));
					// This prevents users from seeing typical windowing system buttons
					loadingBarStage[0].initStyle(StageStyle.UNDECORATED);

					ProgressBar progressBar = new ProgressBar();
					progressBar.setPrefWidth(300);

					Label messageLabel = new Label(bundle.getString("PROGRESS_TABLE_LABEL"));

					VBox root = new VBox(10);
					root.setStyle("-fx-padding: 20; -fx-alignment: center;");
					root.getChildren().addAll(messageLabel, progressBar);

					Scene scene = new Scene(root);
					loadingBarStage[0].setScene(scene);

					// Prevents closing. This has the side effect of preventing windowing system close requests but
					// the application can still be force killed by the user if necessary
					loadingBarStage[0].setOnCloseRequest(Event::consume);
					loadingBarStage[0].show();
				}
			});
		};

		Runnable loadTableRunnable = () -> {
			TableDataAccessor tda = new TableDataAccessor(config.getTablepath());
			tda.loadNewTableData(config.getTableURL());
			refreshLocalTableInfo();

			// Once again, JavaFX UI code must be run inside a Platform context. Hide progress bar and resume
			// normal launcher behaviour
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					loadingBarStage[0].hide();
				}
			});
		};

		new Thread(progressRunnable).start();
		new Thread(loadTableRunnable).start();
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
