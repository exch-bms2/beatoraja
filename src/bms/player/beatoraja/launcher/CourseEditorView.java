package bms.player.beatoraja.launcher;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import bms.player.beatoraja.CourseData;

import static bms.player.beatoraja.CourseData.CourseDataConstraint.*;
import bms.player.beatoraja.song.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.GridPane;

public class CourseEditorView implements Initializable {

	@FXML
	private TextField search;
	@FXML
	private TableView<SongData> searchSongs;
	@FXML
	private SongDataView searchSongsController;

	@FXML
	private ListView<CourseData> courses;
	@FXML
	private GridPane coursePane;
	@FXML
	private TextField courseName;
	@FXML
	private CheckBox release;
	@FXML
	private ComboBox<CourseData.CourseDataConstraint> gradeType;;
	@FXML
	private ComboBox<CourseData.CourseDataConstraint> hispeedType;;
	@FXML
	private ComboBox<CourseData.CourseDataConstraint> judgeType;;
	@FXML
	private ComboBox<CourseData.CourseDataConstraint> gaugeType;;
	@FXML
	private ComboBox<CourseData.CourseDataConstraint> lnType;;
	@FXML
	private Spinner<Double> bronzemiss;
	@FXML
	private Spinner<Double> bronzescore;
	@FXML
	private Spinner<Double> silvermiss;
	@FXML
	private Spinner<Double> silverscore;
	@FXML
	private Spinner<Double> goldmiss;
	@FXML
	private Spinner<Double> goldscore;
	@FXML
	private TableView<SongData> courseSongs;
	@FXML
	private SongDataView courseSongsController;

	private String filename;
	
	private CourseData selectedCourse;
	
	private SongDatabaseAccessor songdb;
	
	public void initialize(URL arg0, ResourceBundle arg1) {
		gradeType.getItems().setAll(null, CLASS, MIRROR, RANDOM);
		hispeedType.getItems().setAll(null, NO_SPEED);
		judgeType.getItems().setAll(null, NO_GOOD, NO_GREAT);
		gaugeType.getItems().setAll(null, GAUGE_LR2,  GAUGE_5KEYS,  GAUGE_7KEYS,  GAUGE_9KEYS,  GAUGE_24KEYS);
		lnType.getItems().setAll(null, LN,  CN,  HCN);
		
		courses.getSelectionModel().selectedIndexProperty().addListener((observable, oldVal, newVal) -> {
			if(oldVal != newVal) {
				updateCourseData();				
			}
		});
		courses.setCellFactory((ListView) -> {
			return new TextFieldListCell<CourseData>() {
				@Override
				public void updateItem(CourseData course, boolean empty) {
					super.updateItem(course, empty);
					setText(empty ? "" : course.getName());
				}
			};
		});
		courseSongsController.setVisible("fullTitle", "sha256");
		searchSongsController.setVisible("fullTitle", "fullArtist", "mode", "level", "notes", "sha256");

		searchSongs.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		updateCourse(null);
	}
	
	protected void setSongDatabaseAccessor(SongDatabaseAccessor songdb) {
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

	public CourseData[] getCourseData() {
		commitCourse();
		return courses.getItems().toArray(new CourseData[courses.getItems().size()]);
	}
	
	public void setCourseData(CourseData[] course) {
		courses.getItems().setAll(course);
	}
	
	public void updateCourseData() {
		commitCourse();
		updateCourse(courses.getSelectionModel().getSelectedItem());
	}
	
	private void commitCourse() {
		if(selectedCourse == null) {
			return;
		}
		selectedCourse.setName(courseName.getText());
		selectedCourse.setRelease(release.isSelected());
		
		List<CourseData.CourseDataConstraint> constraint = new ArrayList<CourseData.CourseDataConstraint>();
		if(gradeType.getValue() != null) {
			constraint.add(gradeType.getValue());
		}
		if(hispeedType.getValue() != null) {
			constraint.add(hispeedType.getValue());
		}
		if(judgeType.getValue() != null) {
			constraint.add(judgeType.getValue());
		}
		if(gaugeType.getValue() != null) {
			constraint.add(gaugeType.getValue());
		}
		if(lnType.getValue() != null) {
			constraint.add(lnType.getValue());
		}
		selectedCourse.setConstraint(constraint.toArray(new CourseData.CourseDataConstraint[constraint.size()]));
		CourseData.TrophyData[] trophy = new CourseData.TrophyData[3];
		trophy[0] = new CourseData.TrophyData("bronzemedal", getValue(bronzemiss).floatValue(), getValue(bronzescore).floatValue());
		trophy[1] = new CourseData.TrophyData("silvermedal", getValue(silvermiss).floatValue(), getValue(silverscore).floatValue());
		trophy[2] = new CourseData.TrophyData("goldmedal", getValue(goldmiss).floatValue(), getValue(goldscore).floatValue());
		selectedCourse.setTrophy(trophy);
		
		selectedCourse.setSong(courseSongs.getItems().toArray(new SongData[courseSongs.getItems().size()]));		
	}

	private void updateCourse(CourseData course) {
		selectedCourse = course;
		if(selectedCourse == null) {
			coursePane.setVisible(false);
			return;
		} 
		coursePane.setVisible(true);
		
		courseName.setText(selectedCourse.getName());
		release.setSelected(selectedCourse.isRelease());
		gradeType.setValue(null);
		judgeType.setValue(null);
		hispeedType.setValue(null);
		gaugeType.setValue(null);
		lnType.setValue(null);
		for(CourseData.CourseDataConstraint constraint : course.getConstraint()) {
			switch(constraint) {
			case CLASS:
			case MIRROR:
			case RANDOM:
				gradeType.setValue(constraint);
				break;
			case NO_GREAT:
			case NO_GOOD:
				judgeType.setValue(constraint);
				break;
			case NO_SPEED:
				hispeedType.setValue(constraint);
				break;
			case GAUGE_24KEYS:
			case GAUGE_5KEYS:
			case GAUGE_7KEYS:
			case GAUGE_9KEYS:
			case GAUGE_LR2:
				gaugeType.setValue(constraint);
				break;
			case LN:
			case CN:
			case HCN:
				lnType.setValue(constraint);
				break;
			}
		}
		for(CourseData.TrophyData trophy : course.getTrophy()) {
			if(trophy.getName().equals("bronzemedal")) {
				bronzemiss.getValueFactory().setValue(Double.valueOf(trophy.getMissrate()));
				bronzescore.getValueFactory().setValue(Double.valueOf(trophy.getScorerate()));
			}
			if(trophy.getName().equals("silvermedal")) {
				silvermiss.getValueFactory().setValue(Double.valueOf(trophy.getMissrate()));
				silverscore.getValueFactory().setValue(Double.valueOf(trophy.getScorerate()));				
			}
			if(trophy.getName().equals("goldmedal")) {
				goldmiss.getValueFactory().setValue(Double.valueOf(trophy.getMissrate()));
				goldscore.getValueFactory().setValue(Double.valueOf(trophy.getScorerate()));
			}
		}
		
		courseSongs.getItems().setAll(course.getSong());
	}
	
	private <T> T getValue(Spinner<T> spinner) {
		spinner.getValueFactory()
				.setValue(spinner.getValueFactory().getConverter().fromString(spinner.getEditor().getText()));
		return spinner.getValue();
	}

	public void addCourseData() {
		CourseData course = new CourseData();
		course.setName("New Course");
		course.setRelease(false);
		CourseData.TrophyData[] trophy = new CourseData.TrophyData[3];
		trophy[0] = new CourseData.TrophyData("bronzemedal", 7.5f, 55.0f);
		trophy[1] = new CourseData.TrophyData("silvermedal", 5.0f, 70.0f);
		trophy[2] = new CourseData.TrophyData("goldmedal", 2.5f, 85.0f);
		course.setTrophy(trophy);
		courses.getItems().add(course);
	}

	public void removeCourseData() {
		CourseData song = courses.getSelectionModel().getSelectedItem();
		if(song != null) {
			courses.getItems().remove(song);
		}
	}
 
	public void moveCourseDataUp() {
		final int index = courses.getSelectionModel().getSelectedIndex();
		if(index > 0) {
			CourseData song = courses.getSelectionModel().getSelectedItem();
			courses.getItems().remove(index);
			courses.getItems().add(index - 1, song);
			courses.getSelectionModel().select(index - 1);
		}
	}

	public void moveCourseDataDown() {
		final int index = courses.getSelectionModel().getSelectedIndex();
		if(index >= 0 && index < courses.getItems().size() - 1) {
			CourseData song = courses.getSelectionModel().getSelectedItem();
			courses.getItems().remove(index);
			courses.getItems().add(index + 1, song);
			courses.getSelectionModel().select(index + 1);
		}
	}

	public void addSongData() {
		List<SongData> songs = searchSongs.getSelectionModel().getSelectedItems();
		for (SongData song : songs) {
			if (song != null) {
				courseSongs.getItems().add(song);
			}
		}
	}

	public void removeSongData() {
		SongData song = courseSongs.getSelectionModel().getSelectedItem();
		if(song != null) {
			courseSongs.getItems().remove(song);
		}
	}
 
	public void moveSongDataUp() {
		final int index = courseSongs.getSelectionModel().getSelectedIndex();
		if(index > 0) {
			SongData song = courseSongs.getSelectionModel().getSelectedItem();
			courseSongs.getItems().remove(index);
			courseSongs.getItems().add(index - 1, song);
			courseSongs.getSelectionModel().select(index - 1);
		}
	}

	public void moveSongDataDown() {
		final int index = courseSongs.getSelectionModel().getSelectedIndex();
		if(index >= 0 && index < courseSongs.getItems().size() - 1) {
			SongData song = courseSongs.getSelectionModel().getSelectedItem();
			courseSongs.getItems().remove(index);
			courseSongs.getItems().add(index + 1, song);
			courseSongs.getSelectionModel().select(index + 1);
		}
	}
}
