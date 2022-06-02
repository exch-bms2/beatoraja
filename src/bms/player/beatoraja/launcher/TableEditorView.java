package bms.player.beatoraja.launcher;

import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

import bms.player.beatoraja.TableData;
import bms.player.beatoraja.song.SongDatabaseAccessor;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;

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
    
    public static boolean isMd5Hash(String text) {
        return text.length() == 32 && hexadecimalPattern.matcher(text).matches();
    }
}
