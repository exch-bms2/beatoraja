package bms.player.beatoraja.launcher;

import java.net.URL;
import java.util.ResourceBundle;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;

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
	private CheckBox folderlamp;
    @FXML
    private CheckBox useSongInfo;
	@FXML
	private CheckBox shownoexistingbar;
	@FXML
	private CheckBox randomselect;
	@FXML
	private NumericSpinner<Integer> maxsearchbar;
	@FXML
	private CheckBox generateFolderBars;

    private Config config;
    private PlayerConfig player;

	public void initialize(URL arg0, ResourceBundle arg1) {
	}

	public void update(Config config) {
		this.config = config;
		
		scrolldurationlow.getValueFactory().setValue(config.getScrollDurationLow());
		scrolldurationhigh.getValueFactory().setValue(config.getScrollDurationHigh());

		generateFolderBars.setSelected(config.isGenerateFolderBars());

        useSongInfo.setSelected(config.isUseSongInfo());
		folderlamp.setSelected(config.isFolderlamp());
		shownoexistingbar.setSelected(config.isShowNoSongExistingBar());
		
		maxsearchbar.getValueFactory().setValue(config.getMaxSearchBarCount());
	}
	
	public void commit() {
		config.setScrollDutationLow(scrolldurationlow.getValue());
		config.setScrollDutationHigh(scrolldurationhigh.getValue());

        config.setUseSongInfo(useSongInfo.isSelected());
        config.setFolderlamp(folderlamp.isSelected());
        config.setShowNoSongExistingBar(shownoexistingbar.isSelected());
        config.setGenerateFolderBars(generateFolderBars.isSelected());
        
        config.setMaxSearchBarCount(maxsearchbar.getValue());
	}

	public void updatePlayer(PlayerConfig player) {
		this.player = player;
		if(player == null) {
			return;
		}

		randomselect.setSelected(player.isRandomSelect());
	}

	public void commitPlayer() {
		if(player == null) {
			return;
		}
		player.setRandomSelect(randomselect.isSelected());
	}

}
