package bms.player.beatoraja.launcher;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.Config.SongPreview;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.select.MusicSelector.ChartReplicationMode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

/**
 * 選曲コンフィグ
 *
 * @author exch
 */
public class MusicSelectConfigurationView implements Initializable {

	@FXML
	private NumericSpinner<Integer> scrolldurationlow;
	@FXML
	private NumericSpinner<Integer> scrolldurationhigh;

	@FXML
	private CheckBox analogScroll;
	@FXML
	private NumericSpinner<Integer> analogTicksPerScroll;

	@FXML
	private CheckBox folderlamp;
    @FXML
    private CheckBox useSongInfo;
	@FXML
	private CheckBox shownoexistingbar;
	@FXML
	private ComboBox<SongPreview> songPreview;
	@FXML
	private CheckBox randomselect;
	@FXML
	private NumericSpinner<Integer> maxsearchbar;
	
	@FXML
	private ComboBox<String> chartReplicationMode;

    private Config config;
    private PlayerConfig player;

	public void initialize(URL arg0, ResourceBundle arg1) {
		songPreview.getItems().setAll(SongPreview.values());
		chartReplicationMode.getItems().setAll(Stream.of(ChartReplicationMode.allMode).map(ChartReplicationMode::name).toList());
	}

	public void update(Config config) {
		this.config = config;
		
		scrolldurationlow.getValueFactory().setValue(config.getScrollDurationLow());
		scrolldurationhigh.getValueFactory().setValue(config.getScrollDurationHigh());
		
		analogScroll.setSelected(config.isAnalogScroll());
		analogTicksPerScroll.getValueFactory().setValue(config.getAnalogTicksPerScroll());

        useSongInfo.setSelected(config.isUseSongInfo());
		folderlamp.setSelected(config.isFolderlamp());
		shownoexistingbar.setSelected(config.isShowNoSongExistingBar());
		songPreview.setValue(config.getSongPreview());
		
		maxsearchbar.getValueFactory().setValue(config.getMaxSearchBarCount());
		
	}
	
	public void commit() {
		config.setScrollDutationLow(scrolldurationlow.getValue());
		config.setScrollDutationHigh(scrolldurationhigh.getValue());

		config.setAnalogScroll(analogScroll.isSelected());
		config.setAnalogTicksPerScroll(analogTicksPerScroll.getValue());

        config.setUseSongInfo(useSongInfo.isSelected());
        config.setFolderlamp(folderlamp.isSelected());
        config.setShowNoSongExistingBar(shownoexistingbar.isSelected());
        config.setSongPreview(songPreview.getValue());
        
        config.setMaxSearchBarCount(maxsearchbar.getValue());
	}

	public void updatePlayer(PlayerConfig player) {
		this.player = player;
		if(player == null) {
			return;
		}

		randomselect.setSelected(player.isRandomSelect());
		
		chartReplicationMode.setValue(player.getChartReplicationMode());
	}

	public void commitPlayer() {
		if(player == null) {
			return;
		}
		player.setRandomSelect(randomselect.isSelected());

		player.setChartReplicationMode(chartReplicationMode.getValue());
	}

}
