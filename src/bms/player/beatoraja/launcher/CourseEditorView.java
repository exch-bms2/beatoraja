package bms.player.beatoraja.launcher;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.CourseDataAccessor;

import static bms.player.beatoraja.CourseData.CourseDataConstraint.*;
import bms.player.beatoraja.song.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;

public class CourseEditorView implements Initializable {

	@FXML
	private TextField search;
	@FXML
	private TableView<SongData> searchSongs;

	@FXML
	private ListView<CourseData> courses;
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

	private String filename;
	
	private CourseData selectedCourse;
	
	private SongDatabaseAccessor songdb;
	
	private CourseDataAccessor courseAccessor = new CourseDataAccessor("course");
	
	public void initialize(URL arg0, ResourceBundle arg1) {
		gradeType.getItems().setAll(null, CLASS, MIRROR, RANDOM);
		hispeedType.getItems().setAll(null, NO_SPEED);
		judgeType.getItems().setAll(null, NO_GOOD, NO_GREAT);
		gaugeType.getItems().setAll(null, GAUGE_LR2,  GAUGE_5KEYS,  GAUGE_7KEYS,  GAUGE_9KEYS,  GAUGE_24KEYS);
		
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

	public void update(String name) {
		courses.getItems().setAll(courseAccessor.read(name));
		filename = name;
	}
	
	public void commit() {
		commitCourse();
		courseAccessor.write(filename, courses.getItems().toArray(new CourseData[courses.getItems().size()]));
	}
	
	public void updateCourseData() {
		commitCourse();
		updateCourse(courses.getSelectionModel().getSelectedItem());
	}
	
	private void commitCourse() {
		if(selectedCourse == null) {
			return;
		}
		final int index = courses.getItems().indexOf(selectedCourse);
		
		selectedCourse = new CourseData();
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
		selectedCourse.setConstraint(constraint.toArray(new CourseData.CourseDataConstraint[constraint.size()]));
		CourseData.TrophyData[] trophy = new CourseData.TrophyData[3];
		trophy[0] = new CourseData.TrophyData("bronzemedal", getValue(bronzemiss).floatValue(), getValue(bronzescore).floatValue());
		trophy[1] = new CourseData.TrophyData("silvermedal", getValue(silvermiss).floatValue(), getValue(silverscore).floatValue());
		trophy[2] = new CourseData.TrophyData("goldmedal", getValue(goldmiss).floatValue(), getValue(goldscore).floatValue());
		selectedCourse.setTrophy(trophy);
		
		selectedCourse.setSong(courseSongs.getItems().toArray(new SongData[courseSongs.getItems().size()]));		
		courses.getItems().set(index, selectedCourse);
	}

	private void updateCourse(CourseData course) {
		selectedCourse = course;
		
		courseName.setText(selectedCourse.getName());
		release.setSelected(selectedCourse.isRelease());
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

	public void addSongData() {
		SongData song = searchSongs.getSelectionModel().getSelectedItem();
		if(song != null) {
			courseSongs.getItems().add(song);
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
		}
	}

	public void moveSongDataDown() {
		final int index = courseSongs.getSelectionModel().getSelectedIndex();
		if(index >= 0 && index < courseSongs.getItems().size() - 1) {
			SongData song = courseSongs.getSelectionModel().getSelectedItem();
			courseSongs.getItems().remove(index);
			courseSongs.getItems().add(index + 1, song);
		}
	}
}
