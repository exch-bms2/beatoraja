package bms.player.beatoraja;

import java.net.URL;
import java.util.*;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

/**
 * Beatorajaの設定ダイアログ
 * 
 * @author exch
 */
public class PlayConfigurationView implements Initializable {

	/**
	 * ハイスピード
	 */
	@FXML
	private Spinner<Double> hispeed;

	@FXML
	private GridPane lr2configuration;
	@FXML
	private CheckBox fixgvalue;
	@FXML
	private Spinner<Integer> gvalue;
	@FXML
	private CheckBox fullscreen;
	@FXML
	private CheckBox vsync;
	@FXML
	private ComboBox<Integer> bgaop;
	@FXML
	private ComboBox<Integer> scoreop;
	@FXML
	private ComboBox<Integer> gaugeop;
	@FXML
	private ComboBox<String> configBox;
	@FXML
	private CheckBox enableLanecover;
	@FXML
	private Spinner<Double> lanecover;
	@FXML
	private CheckBox enableLift;
	@FXML
	private Spinner<Double> lift;
	
	@FXML
	private TextField vlcpath;

	@FXML
	private Spinner<Integer> judgetiming;
	@FXML
	private CheckBox constant;
	@FXML
	private CheckBox bpmguide;
	@FXML
	private CheckBox legacy;

	private Config config;;

	private static final String[] SCOREOP = { "OFF", "MIRROR", "RANDOM", "R-RANDOM",
			"S-RANDOM", "H-RANDOM", "ALL-SCR", "RANDOM-EX", "S-RANDOM-EX"};

	private static final String[] GAUGEOP = { "ASSIST EASY", "EASY", "NORMAL", "HARD",
		"EX-HARD", "HAZARD"};

	private static final String[] BGAOP = { "ON", "AUTOPLAY ", "OFF"};

	public void initialize(URL arg0, ResourceBundle arg1) {
//		hispeed.setProperty(1.0, 1.0, 9.9, 0.1);
//		gvalue.setProperty(1, 1, 2000, 1);
//		lanecover.setProperty(0.0, 0.0, 1.0, 0.001);
//		lift.setProperty(0.0, 0.0, 1.0, 0.001);
//		judgetiming.setProperty(0, -99, 99, 1);
		lr2configuration.setHgap(25);
		lr2configuration.setVgap(4);

		scoreop.setCellFactory(new Callback<ListView<Integer>, ListCell<Integer>>() {
			public ListCell<Integer> call(ListView<Integer> param) {
				return new OptionListCell(SCOREOP);
			}
		});
		scoreop.setButtonCell(new OptionListCell(SCOREOP));
		scoreop.getItems().setAll(0, 1, 2, 3, 4, 5, 6,7);
		gaugeop.setCellFactory(new Callback<ListView<Integer>, ListCell<Integer>>() {
			public ListCell<Integer> call(ListView<Integer> param) {
				return new OptionListCell(GAUGEOP);
			}
		});
		gaugeop.setButtonCell(new OptionListCell(GAUGEOP));
		gaugeop.getItems().setAll(0, 1, 2, 3, 4);
		bgaop.setCellFactory(new Callback<ListView<Integer>, ListCell<Integer>>() {
			public ListCell<Integer> call(ListView<Integer> param) {
				return new OptionListCell(BGAOP);
			}
		});
		bgaop.setButtonCell(new OptionListCell(BGAOP));
		bgaop.getItems().setAll(0, 1, 2);
	}

	/**
	 * ダイアログの項目を更新する
	 */
	public void update(Config config) {
		this.config = config;
		fullscreen.setSelected(config.isFullscreen());
		vsync.setSelected(config.isVsync());
		bgaop.setValue(config.getBga());
		scoreop.getSelectionModel().select(config.getRandom());
		gaugeop.getSelectionModel().select(config.getGauge());
		
		fixgvalue.setSelected(config.isFixhispeed());
		hispeed.getValueFactory().setValue((double) config.getHispeed());
		gvalue.getValueFactory().setValue(config.getGreenvalue());
		enableLanecover.setSelected(config.isEnablelanecover());
		lanecover.getValueFactory().setValue((double) config.getLanecover());
		enableLift.setSelected(config.isEnablelift());
		lift.getValueFactory().setValue((double) config.getLift());
		
		vlcpath.setText(config.getVlcpath());
		
		constant.setSelected(config.isConstant());
		bpmguide.setSelected(config.isBpmguide());
		legacy.setSelected(config.getLnassist() == 1);
	}

	/**
	 * ダイアログの項目をconfig.xmlに反映する
	 */
	public void commit() {
		config.setFullscreen(fullscreen.isSelected());
		config.setVsync(vsync.isSelected());
		config.setBga(bgaop.getValue());
		config.setRandom(scoreop.getValue());
		config.setGauge(gaugeop.getValue());
		config.setHispeed(hispeed.getValue().floatValue());
		config.setFixhispeed(fixgvalue.isSelected());
		config.setGreenvalue(gvalue.getValue());
		config.setEnablelanecover(enableLanecover.isSelected());
		config.setLanecover(lanecover.getValue().floatValue());
		config.setEnablelift(enableLift.isSelected());
		config.setLift(lift.getValue().floatValue());
		
		config.setVlcpath(vlcpath.getText());
		
		config.setConstant(constant.isSelected());
		config.setBpmguide(bpmguide.isSelected());
		config.setLnassist(legacy.isSelected() ? 1 : 0);
	}

	class OptionListCell extends ListCell<Integer> {
		
		private final String[] strings;
		
		public OptionListCell(String[] strings) {
			this.strings = strings;
		}
		
		@Override
		protected void updateItem(Integer arg0, boolean arg1) {
			super.updateItem(arg0, arg1);
			if (arg0 != null) {
				setText(strings[arg0]);
			}
		}
	}	

}
