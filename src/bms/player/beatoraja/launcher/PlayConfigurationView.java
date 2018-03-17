package bms.player.beatoraja.launcher;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.portaudio.DeviceInfo;

import bms.model.Mode;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.MainLoader;
import bms.player.beatoraja.PlayConfig;
import bms.player.beatoraja.PlayConfig.ControllerConfig;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.ScoreDatabaseAccessor;
import bms.player.beatoraja.TableDataAccessor;
import bms.player.beatoraja.audio.PortAudioDriver;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.play.JudgeAlgorithm;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.song.SQLiteSongDatabaseAccessor;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;
import bms.player.beatoraja.song.SongInformationAccessor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Beatorajaの設定ダイアログ
 *
 * @author exch
 */
public class PlayConfigurationView implements Initializable {

	// TODO スキンプレビュー機能

	@FXML
	private VBox root;
	@FXML
	private HBox playerPanel;
	@FXML
	private Tab videoTab;
	@FXML
	private Tab audioTab;
	@FXML
	private Tab resourceTab;
	@FXML
	private Tab inputTab;
	@FXML
	private Tab skinTab;
	@FXML
	private Tab optionTab;
	@FXML
	private Tab otherTab;
	@FXML
	private Tab irTab;
	@FXML
	private Tab courseTab;
	@FXML
	private HBox controlPanel;

	@FXML
	private ComboBox<String> players;
	@FXML
	private TextField playername;

	@FXML
	private ComboBox<PlayMode> inputconfig;
	@FXML
	private ComboBox<PlayMode> playconfig;
	/**
	 * ハイスピード
	 */
	@FXML
	private Spinner<Double> hispeed;

	@FXML
	private GridPane lr2configuration;
	@FXML
	private GridPane lr2configurationassist;
	@FXML
	private ComboBox<Integer> fixhispeed;
	@FXML
	private Spinner<Integer> gvalue;
	@FXML
	private Spinner<Double> hispeedmargin;
	@FXML
	private Spinner<Integer> inputduration;
	@FXML
	private Spinner<Integer> scrolldurationlow;
	@FXML
	private Spinner<Integer> scrolldurationhigh;

	@FXML
	private ListView<String> bmsroot;
	@FXML
	private TextField url;
	@FXML
	private ListView<String> tableurl;
	@FXML
	private CheckBox updatesong;

	@FXML
	private ComboBox<Integer> scoreop;
	@FXML
	private ComboBox<Integer> scoreop2;
	@FXML
	private ComboBox<Integer> doubleop;
	@FXML
	private ComboBox<Integer> gaugeop;
	@FXML
	private ComboBox<Integer> lntype;
	@FXML
	private CheckBox enableLanecover;
	@FXML
	private Spinner<Integer> lanecover;
	@FXML
	private CheckBox enableLift;
	@FXML
	private Spinner<Integer> lift;

	@FXML
	private TextField bgmpath;
	@FXML
	private TextField soundpath;

	@FXML
	private Spinner<Integer> judgetiming;
	@FXML
	private CheckBox constant;
	@FXML
	private CheckBox bpmguide;
	@FXML
	private CheckBox legacy;
	@FXML
	private CheckBox continueuntilendofsong;
	@FXML
	private Spinner<Integer> exjudge;
	@FXML
	private CheckBox nomine;
	@FXML
	private Spinner<Integer> hranthresholdbpm;
	@FXML
	private ComboBox<Integer> seventoninepattern;
	@FXML
	private ComboBox<Integer> seventoninetype;

	@FXML
	private CheckBox judgeregion;
	@FXML
	private CheckBox markprocessednote;
	@FXML
	private CheckBox showhiddennote;
	@FXML
	private ComboBox<Integer> target;

	@FXML
	private ComboBox<Integer> audio;
	@FXML
	private ComboBox<String> audioname;
	@FXML
	private Spinner<Integer> audiobuffer;
	@FXML
	private Spinner<Integer> audiosim;
	@FXML
	private Slider systemvolume;
	@FXML
	private Slider keyvolume;
	@FXML
	private Slider bgvolume;
	@FXML
	private ComboBox<Integer> audioFreqOption;
	@FXML
	private ComboBox<Integer> audioFastForward;
	@FXML
	private ComboBox<Integer> judgealgorithm;

    @FXML
	private ComboBox<Integer> autosavereplay1;
	@FXML
	private ComboBox<Integer> autosavereplay2;
	@FXML
	private ComboBox<Integer> autosavereplay3;
	@FXML
	private ComboBox<Integer> autosavereplay4;

    @FXML
    private CheckBox jkoc_hack;
	@FXML
	private CheckBox analogScratch;
    @FXML
    private CheckBox usecim;
    @FXML
    private CheckBox useSongInfo;

    @FXML
	private TextField txtTwitterConsumerKey;
    @FXML
	private PasswordField txtTwitterConsumerSecret;

    @FXML
    private Button twitterAuthButton;
    @FXML
    private Label txtTwitterAuthenticated;
    @FXML
    private TextField txtTwitterPIN;
    @FXML
    private Button twitterPINButton;

	@FXML
	private VBox skin;
	@FXML
	private VideoConfigurationView videoController;
	@FXML
	private SkinConfigurationView skinController;
	@FXML
	private CourseEditorView courseController;

	private Config config;
	private PlayerConfig player;
	@FXML
	private CheckBox folderlamp;

	@FXML
	private ComboBox<String> irname;
	@FXML
	private TextField iruserid;
	@FXML
	private PasswordField irpassword;
	@FXML
	private ComboBox<Integer> irsend;

	private MainLoader loader;

	private boolean songUpdated = false;

	private RequestToken requestToken = null;

	private void initComboBox(ComboBox<Integer> combo, final String[] values) {
		combo.setCellFactory((param) -> new OptionListCell(values));
		combo.setButtonCell(new OptionListCell(values));
		for (int i = 0; i < values.length; i++) {
			combo.getItems().add(i);
		}
	}

	public void initialize(URL arg0, ResourceBundle arg1) {
		lr2configuration.setHgap(25);
		lr2configuration.setVgap(4);
		lr2configurationassist.setHgap(25);
		lr2configurationassist.setVgap(4);

		String[] scoreOptions = new String[] { "OFF", "MIRROR", "RANDOM", "R-RANDOM", "S-RANDOM", "SPIRAL", "H-RANDOM",
				"ALL-SCR", "RANDOM-EX", "S-RANDOM-EX" };
		initComboBox(scoreop, scoreOptions);
		initComboBox(scoreop2, scoreOptions);
		initComboBox(doubleop, new String[] { "OFF", "FLIP", "BATTLE", "BATTLE AS" });
		initComboBox(seventoninepattern, new String[] { "OFF", "SC1KEY2~8", "SC1KEY3~9", "SC2KEY3~9", "SC8KEY1~7", "SC9KEY1~7", "SC9KEY2~8" });
		String[] seventoninestring = new String[]{arg1.getString("SEVEN_TO_NINE_OFF"),arg1.getString("SEVEN_TO_NINE_NO_MASHING"),arg1.getString("SEVEN_TO_NINE_ALTERNATION")};
		initComboBox(seventoninetype, seventoninestring);
		initComboBox(gaugeop, new String[] { "ASSIST EASY", "EASY", "NORMAL", "HARD", "EX-HARD", "HAZARD" });
		initComboBox(fixhispeed, new String[] { "OFF", "START BPM", "MAX BPM", "MAIN BPM", "MIN BPM" });
		playconfig.getItems().setAll(PlayMode.values());
		inputconfig.getItems().setAll(PlayMode.values());
		initComboBox(lntype, new String[] { "LONG NOTE", "CHARGE NOTE", "HELL CHARGE NOTE" });
		
		TargetProperty[] targets = TargetProperty.getAllTargetProperties();
		String[] targetString = new String[targets.length];
		for(int i  =0;i < targets.length;i++) {
			targetString[i] = targets[i].getName();
		}
		initComboBox(target, targetString);
		initComboBox(judgealgorithm, new String[] { arg1.getString("JUDGEALG_LR2"), arg1.getString("JUDGEALG_AC"), arg1.getString("JUDGEALG_BOTTOM_PRIORITY") });
		String[] autosaves = new String[]{arg1.getString("NONE"),arg1.getString("BETTER_SCORE"),arg1.getString("BETTER_OR_SAME_SCORE"),arg1.getString("BETTER_MISSCOUNT")
				,arg1.getString("BETTER_OR_SAME_MISSCOUNT"),arg1.getString("BETTER_COMBO"),arg1.getString("BETTER_OR_SAME_COMBO"),
				arg1.getString("BETTER_LAMP"),arg1.getString("BETTER_OR_SAME_LAMP"),arg1.getString("BETTER_ALL"),arg1.getString("ALWAYS")};
		initComboBox(autosavereplay1, autosaves);
		initComboBox(autosavereplay2, autosaves);
		initComboBox(autosavereplay3, autosaves);
		initComboBox(autosavereplay4, autosaves);
		initComboBox(irsend, new String[] { arg1.getString("IR_SEND_ALWAYS"), arg1.getString("IR_SEND_FINISH"), arg1.getString("IR_SEND_UPDATE")});
		initComboBox(audio, new String[] { "OpenAL (LibGDX Sound)", "OpenAL (LibGDX AudioDevice)", "PortAudio"});
		audio.getItems().setAll(0, 2);

		String[] audioPlaySpeedControls = new String[] { "UNPROCESSED", "FREQUENCY" };
		initComboBox(audioFreqOption, audioPlaySpeedControls);
		initComboBox(audioFastForward, audioPlaySpeedControls);

		irname.getItems().setAll(IRConnection.getAllAvailableIRConnectionName());

		players.getItems().setAll(PlayerConfig.readAllPlayerID());
	}

	public void setBMSInformationLoader(MainLoader loader) {
		this.loader = loader;
	}

	/**
	 * ダイアログの項目を更新する
	 */
	public void update(Config config) {
		this.config = config;

		videoController.update(config);

		systemvolume.setValue((double)config.getSystemVolume());
		keyvolume.setValue((double)config.getKeyVolume());
		bgvolume.setValue((double)config.getBgVolume());

		bgmpath.setText(config.getBgmPath());
		soundpath.setText(config.getSoundPath());

		bmsroot.getItems().setAll(config.getBmsRoot());
		updatesong.setSelected(config.isUpdateSong());
		tableurl.getItems().setAll(config.getTableURL());

		audio.setValue(config.getAudioDriver());
		audiobuffer.getValueFactory().setValue(config.getAudioDeviceBufferSize());
		audiosim.getValueFactory().setValue(config.getAudioDeviceSimultaneousSources());
		audioFreqOption.setValue(config.getAudioFreqOption());
		audioFastForward.setValue(config.getAudioFastForward());
		showhiddennote.setSelected(config.isShowHiddenNote());

		judgealgorithm.setValue(JudgeAlgorithm.getIndex(config.getJudgeType()));

		autosavereplay1.getSelectionModel().select(config.getAutoSaveReplay()[0]);
		autosavereplay2.getSelectionModel().select(config.getAutoSaveReplay()[1]);
		autosavereplay3.getSelectionModel().select(config.getAutoSaveReplay()[2]);
		autosavereplay4.getSelectionModel().select(config.getAutoSaveReplay()[3]);

        // int b = Boolean.valueOf(config.getJKOC()).compareTo(false);

        usecim.setSelected(config.isCacheSkinImage());
        useSongInfo.setSelected(config.isUseSongInfo());

		folderlamp.setSelected(config.isFolderLamp());

		inputduration.getValueFactory().setValue(config.getInputduration());

		scrolldurationlow.getValueFactory().setValue(config.getScrollDurationLow());
		scrolldurationhigh.getValueFactory().setValue(config.getScrollDurationHigh());

		updateAudioDriver();

		if(players.getItems().contains(config.getPlayername())) {
			players.setValue(config.getPlayername());
		} else {
			players.getSelectionModel().select(0);
		}
		updatePlayer();
		
		try {
			Class.forName("org.sqlite.JDBC");
			SongDatabaseAccessor songdb = new SQLiteSongDatabaseAccessor(Paths.get("songdata.db").toString(),
					config.getBmsRoot());
			courseController.setSongDatabaseAccessor(songdb);
			courseController.update("default");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void changePlayer() {
		commitPlayer();
		updatePlayer();
	}
	
	public void addPlayer() {
		String[] ids = PlayerConfig.readAllPlayerID();
		for(int i = 1;i < 1000;i++) {
			String playerid = "player" + i;
			boolean b = true;
			for(String id : ids) {
				if(playerid.equals(id)) {
					b =false;
					break;
				}
			}
			if(b) {
				PlayerConfig.create(playerid);				
				players.getItems().add(playerid);
				break;
			}
		}
	}

	public void updatePlayer() {
		player = PlayerConfig.readPlayerConfig(players.getValue());
		playername.setText(player.getName());

		videoController.updatePlayer(player);

		scoreop.getSelectionModel().select(player.getRandom());
		scoreop2.getSelectionModel().select(player.getRandom2());
		doubleop.getSelectionModel().select(player.getDoubleoption());
		seventoninepattern.getSelectionModel().select(player.getSevenToNinePattern());
		seventoninetype.getSelectionModel().select(player.getSevenToNineType());
		gaugeop.getSelectionModel().select(player.getGauge());
		lntype.getSelectionModel().select(player.getLnmode());

		fixhispeed.setValue(player.getFixhispeed());
		judgetiming.getValueFactory().setValue(player.getJudgetiming());

		constant.setSelected(player.isConstant());
		bpmguide.setSelected(player.isBpmguide());
		legacy.setSelected(player.isLegacynote());
		continueuntilendofsong.setSelected(player.isContinueUntilEndOfSong());
		exjudge.getValueFactory().setValue(player.getJudgewindowrate());
		nomine.setSelected(player.isNomine());
		hranthresholdbpm.getValueFactory().setValue(player.getHranThresholdBPM());
		judgeregion.setSelected(player.isShowjudgearea());
		markprocessednote.setSelected(player.isMarkprocessednote());
		target.setValue(player.getTarget());

		irname.setValue(player.getIrname());
		iruserid.setText(player.getUserid());
		irpassword.setText(player.getPassword());
		irsend.setValue(player.getIrsend());

		if(player.getTwitterAccessToken() != null && !player.getTwitterAccessToken().isEmpty()) {
			twitterAuthButton.setDisable(true);
			txtTwitterPIN.setDisable(true);
			twitterPINButton.setDisable(true);
		} else {
			txtTwitterAuthenticated.setVisible(false);
		}

		playconfig.setValue(PlayMode.BEAT_7K);
		updatePlayConfig();
		inputconfig.setValue(PlayMode.BEAT_7K);
		updateInputConfig();
		skinController.update(player);
	}

	/**
	 * ダイアログの項目をconfig.xmlに反映する
	 */
	public void commit() {
	    videoController.commit(config);

		config.setPlayername(players.getValue());

		config.setBgmPath(bgmpath.getText());
		config.setSoundPath(soundpath.getText());
		config.setSystemVolume((float) systemvolume.getValue());
		config.setKeyVolume((float) keyvolume.getValue());
		config.setBgVolume((float) bgvolume.getValue());

		config.setBmsRoot(bmsroot.getItems().toArray(new String[0]));
		config.setUpdateSong(updatesong.isSelected());
		config.setTableURL(tableurl.getItems().toArray(new String[0]));

		config.setShowHiddenNote(showhiddennote.isSelected());

		config.setAudioDriver(audio.getValue());
		config.setAudioDriverName(audioname.getValue());
		config.setAudioDeviceBufferSize(getValue(audiobuffer));
		config.setAudioDeviceSimultaneousSources(getValue(audiosim));
		config.setAudioFreqOption(audioFreqOption.getValue());
		config.setAudioFastForward(audioFastForward.getValue());

		config.setJudgeType(JudgeAlgorithm.values()[judgealgorithm.getValue()].name());
		config.setAutoSaveReplay( new int[]{autosavereplay1.getValue(),autosavereplay2.getValue(),
				autosavereplay3.getValue(),autosavereplay4.getValue()});

        // jkoc_hack is integer but *.setJKOC needs boolean type

        config.setCacheSkinImage(usecim.isSelected());
        config.setUseSongInfo(useSongInfo.isSelected());
        config.setFolderLamp(folderlamp.isSelected());

		config.setInputduration(getValue(inputduration));

		config.setScrollDurationLow(getValue(scrolldurationlow));
		config.setScrollDurationHigh(getValue(scrolldurationhigh));

		commitPlayer();

		Json json = new Json();
		json.setOutputType(OutputType.json);
		try (FileWriter fw = new FileWriter("config.json")) {
			fw.write(json.prettyPrint(config));
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		courseController.commit();
	}

	public void commitPlayer() {
		if(player == null) {
			return;
		}
		Path p = Paths.get("player/" + player.getId() + "/config.json");
		if(playername.getText().length() > 0) {
			player.setName(playername.getText());			
		}

		videoController.commitPlayer(player);

		player.setRandom(scoreop.getValue());
		player.setRandom2(scoreop2.getValue());
		player.setDoubleoption(doubleop.getValue());
		player.setSevenToNinePattern(seventoninepattern.getValue());
		player.setSevenToNineType(seventoninetype.getValue());
		player.setGauge(gaugeop.getValue());
		player.setLnmode(lntype.getValue());
		player.setFixhispeed(fixhispeed.getValue());
		player.setJudgetiming(getValue(judgetiming));

		player.setConstant(constant.isSelected());
		player.setBpmguide(bpmguide.isSelected());
		player.setLegacynote(legacy.isSelected());
		player.setContinueUntilEndOfSong(continueuntilendofsong.isSelected());
		player.setJudgewindowrate(getValue(exjudge));
		player.setNomine(nomine.isSelected());
		player.setHranThresholdBPM(getValue(hranthresholdbpm));
		player.setMarkprocessednote(markprocessednote.isSelected());

		player.setShowjudgearea(judgeregion.isSelected());
		player.setTarget(target.getValue());

		player.setIrname(irname.getValue());
		player.setUserid(iruserid.getText());
		player.setPassword(irpassword.getText());
		player.setIrsend(irsend.getValue());

		updateInputConfig();
		updatePlayConfig();
		skinController.update(player);

		Json json = new Json();
		json.setOutputType(OutputType.json);
		try (FileWriter fw = new FileWriter(p.toFile())) {
			fw.write(json.prettyPrint(player));
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    @FXML
	public void addSongPath() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("楽曲のルートフォルダを選択してください");
		File f = chooser.showDialog(null);
		if (f != null) {
			boolean unique = true;
			for (String path : bmsroot.getItems()) {
				if (path.equals(f.getPath()) || f.getPath().startsWith(path + File.separatorChar)) {
					unique = false;
					break;
				}
			}
			if (unique) {
				bmsroot.getItems().add(f.getPath());
				loadDiffBMS();
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
					boolean unique = true;
					for (String path : bmsroot.getItems()) {
						if (path.equals(f.getPath()) || f.getPath().startsWith(path + File.separatorChar)) {
							unique = false;
							break;
						}
					}
					if (unique) {
						bmsroot.getItems().add(f.getPath());
						loadDiffBMS();
					}
				}
			}
		}
	}
    
    @FXML
	public void addBGMPath() {
    	String s = showDirectoryChooser("BGMのルートフォルダを選択してください");
    	if(s != null) {
        	bgmpath.setText(s);
    	}
	}

    @FXML
	public void addSoundPath() {
    	String s = showDirectoryChooser("効果音のルートフォルダを選択してください");
    	if(s != null) {
    		soundpath.setText(s);    		
    	}
	}
    
    private String showDirectoryChooser(String title) {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle(title);
		File f = chooser.showDialog(null);
		return f != null ? f.getPath() : null;
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
		}
	}

	public void moveTableURLDown() {
		final int index = tableurl.getSelectionModel().getSelectedIndex();
		if(index >= 0 && index < tableurl.getItems().size() - 1) {
			String table = tableurl.getSelectionModel().getSelectedItem();
			tableurl.getItems().remove(index);
			tableurl.getItems().add(index + 1, table);
		}
	}

	private PlayMode pc = null;

    @FXML
	public void updatePlayConfig() {
		if (pc != null) {
			PlayConfig conf = player.getPlayConfig(Mode.valueOf(pc.name()));
			conf.setHispeed(getValue(hispeed).floatValue());
			conf.setDuration(getValue(gvalue));
			conf.setHispeedMargin(getValue(hispeedmargin).floatValue());
			conf.setEnablelanecover(enableLanecover.isSelected());
			conf.setLanecover(getValue(lanecover) / 1000f);
			conf.setEnablelift(enableLift.isSelected());
			conf.setLift(getValue(lift) / 1000f);
		}
		pc = playconfig.getValue();
		PlayConfig conf = player.getPlayConfig(Mode.valueOf(pc.name()));
		hispeed.getValueFactory().setValue((double) conf.getHispeed());
		gvalue.getValueFactory().setValue(conf.getDuration());
		hispeedmargin.getValueFactory().setValue((double) conf.getHispeedMargin());
		enableLanecover.setSelected(conf.isEnablelanecover());
		lanecover.getValueFactory().setValue((int) (conf.getLanecover() * 1000));
		enableLift.setSelected(conf.isEnablelift());
		lift.getValueFactory().setValue((int) (conf.getLift() * 1000));
	}

	private PlayMode ic = null;

    @FXML
	public void updateInputConfig() {
		if (ic != null) {
			PlayConfig conf = player.getPlayConfig(Mode.valueOf(ic.name()));
			for(ControllerConfig controller : conf.getController()) {
				controller.setJKOC(jkoc_hack.isSelected());
		        controller.setAnalogScratch(analogScratch.isSelected());
			}
		}
		ic = inputconfig.getValue();
		PlayConfig conf = player.getPlayConfig(Mode.valueOf(ic.name()));
		for(ControllerConfig controller : conf.getController()) {
	        jkoc_hack.setSelected(controller.getJKOC());
	        analogScratch.setSelected(controller.isAnalogScratch());
		}
	}

	private <T> T getValue(Spinner<T> spinner) {
		spinner.getValueFactory()
				.setValue(spinner.getValueFactory().getConverter().fromString(spinner.getEditor().getText()));
		return spinner.getValue();
	}

    @FXML
	public void updateAudioDriver() {
		switch(audio.getValue()) {
		case Config.AUDIODRIVER_SOUND:
			audioname.setDisable(true);
			audioname.getItems().clear();
			audiobuffer.setDisable(false);
			audiosim.setDisable(false);
			break;
		case Config.AUDIODRIVER_PORTAUDIO:
			try {
				DeviceInfo[] devices = PortAudioDriver.getDevices();
				List<String> drivers = new ArrayList<String>(devices.length);
				for(int i = 0;i < devices.length;i++) {
					drivers.add(devices[i].name);
				}
				if(drivers.size() == 0) {
					throw new RuntimeException("ドライバが見つかりません");
				}
				audioname.getItems().setAll(drivers);
				if(drivers.contains(config.getAudioDriverName())) {
					audioname.setValue(config.getAudioDriverName());
				} else {
					audioname.setValue(drivers.get(0));
				}
				audioname.setDisable(false);
				audiobuffer.setDisable(false);
				audiosim.setDisable(false);
//				PortAudio.terminate();
			} catch(Throwable e) {
				Logger.getGlobal().severe("PortAudioは選択できません : " + e.getMessage());
				audio.setValue(Config.AUDIODRIVER_SOUND);
			}
			break;
		}
	}

    @FXML
	public void start() {
		commit();
		playerPanel.setDisable(true);
		videoTab.setDisable(true);
		audioTab.setDisable(true);
		inputTab.setDisable(true);
		resourceTab.setDisable(true);
		optionTab.setDisable(true);
		otherTab.setDisable(true);
		irTab.setDisable(true);
		controlPanel.setDisable(true);
		
		MainLoader.play(null, bms.player.beatoraja.PlayerResource.PlayMode.PLAY, true, config, player, songUpdated);
	}

    @FXML
	public void loadAllBMS() {
		loadBMS(true);
	}

    @FXML
	public void loadDiffBMS() {
		loadBMS(false);
	}

	/**
	 * BMSを読み込み、楽曲データベースを更新する
	 *
	 * @param updateAll
	 *            falseの場合は追加削除分のみを更新する
	 */
	public void loadBMS(boolean updateAll) {
		commit();
		try {
			Class.forName("org.sqlite.JDBC");
			SongDatabaseAccessor songdb = new SQLiteSongDatabaseAccessor(Paths.get("songdata.db").toString(),
					config.getBmsRoot());
			SongInformationAccessor infodb = useSongInfo.isSelected() ?
					new SongInformationAccessor(Paths.get("songinfo.db").toString()) : null;
			Logger.getGlobal().info("song.db更新開始");
			songdb.updateSongDatas(null, updateAll, infodb);
			Logger.getGlobal().info("song.db更新完了");
			songUpdated = true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

    @FXML
	public void loadTable() {
		commit();
		try {
			Files.createDirectories(Paths.get("table"));
		} catch (IOException e) {
		}

		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get("table"))) {
			for (Path p : paths) {
				Files.deleteIfExists(p);
			}
		} catch (IOException e) {

		}

		TableDataAccessor tda = new TableDataAccessor();
		tda.updateTableData(config.getTableURL());
	}

    @FXML
	public void importScoreDataFromLR2() {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(new ExtensionFilter("Lunatic Rave 2 Score Database File", "*.db"));
		chooser.setTitle("LRのスコアデータベースを選択してください");
		File dir = chooser.showOpenDialog(null);
		if (dir == null) {
			return;
		}

		final int[] clears = { 0, 1, 4, 5, 6, 8, 9 };
		try {
			Class.forName("org.sqlite.JDBC");
			SongDatabaseAccessor songdb = new SQLiteSongDatabaseAccessor(Paths.get("songdata.db").toString(),
					config.getBmsRoot());
			String player = "player1";
			ScoreDatabaseAccessor scoredb = new ScoreDatabaseAccessor("player/" + player + "/score.db");
			scoredb.createTable();

			try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + dir.getPath())) {
				QueryRunner qr = new QueryRunner();
				MapListHandler rh = new MapListHandler();
				List<Map<String, Object>> scores = qr.query(con, "SELECT * FROM score", rh);

				List<IRScoreData> result = new ArrayList<IRScoreData>();
				for (Map<String, Object> score : scores) {
					final String md5 = (String) score.get("hash");
					SongData[] song = songdb.getSongDatas(new String[] { md5 });
					if (song.length > 0) {
						IRScoreData sd = new IRScoreData();
						sd.setEpg((int) score.get("perfect"));
						sd.setEgr((int) score.get("great"));
						sd.setEgd((int) score.get("good"));
						sd.setEbd((int) score.get("bad"));
						sd.setEpr((int) score.get("poor"));
						sd.setMinBP((int) score.get("minBP"));
						sd.setClear(clears[(int) score.get("clear")]);
						sd.setPlaycount((int) score.get("playcount"));
						sd.setClearcount((int) score.get("clearcount"));
						sd.setNotes(song[0].getNotes());
						sd.setSha256(song[0].getSha256());
						IRScoreData oldsd = scoredb.getScoreData(sd.getSha256(), 0);
						sd.setScorehash("LR2");
						if (oldsd == null || oldsd.getClear() <= sd.getClear()) {
							result.add(sd);
						}
					}
				}
				scoredb.setScoreData(result.toArray(new IRScoreData[result.size()]));
			} catch (Exception e) {
				Logger.getGlobal().severe("スコア移行時の例外:" + e.getMessage());
			}
		} catch (ClassNotFoundException e1) {
		}

	}

	@FXML
	public void startTwitterAuth() {
		Twitter twitter = TwitterFactory.getSingleton();
		twitter.setOAuthConsumer(txtTwitterConsumerKey.getText(), txtTwitterConsumerSecret.getText());
		try {
			player.setTwitterConsumerKey(txtTwitterConsumerKey.getText());
			player.setTwitterConsumerSecret(txtTwitterConsumerSecret.getText());
			requestToken = twitter.getOAuthRequestToken();
			Desktop desktop = Desktop.getDesktop();
			URI uri = new URI(requestToken.getAuthorizationURL());
			desktop.browse(uri);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void startPINAuth() {
		Twitter twitter = TwitterFactory.getSingleton();
		try {
			AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, txtTwitterPIN.getText());
			player.setTwitterAccessToken(accessToken.getToken());
			player.setTwitterAccessTokenSecret(accessToken.getTokenSecret());
			commit();
			update(config);
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

    @FXML
	public void exit() {
		commit();
		Platform.exit();
		System.exit(0);
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

	static class SkinListCell extends ListCell<SkinHeader> {

		@Override
		protected void updateItem(SkinHeader arg0, boolean arg1) {
			super.updateItem(arg0, arg1);
			if (arg0 != null) {
				setText(arg0.getName() + (arg0.getType() == SkinHeader.TYPE_BEATORJASKIN ? "" : " (LR2 Skin)"));
			} else {
				setText("");
			}
		}
	}

	static class SkinTypeCell extends ListCell<SkinType> {

		@Override
		protected void updateItem(SkinType arg0, boolean arg1) {
			super.updateItem(arg0, arg1);
			if (arg0 != null) {
				setText(arg0.getName());
			}
		}
	}

	enum PlayMode {
		BEAT_7K("5/7KEYS"),
		BEAT_14K("10/14KEYS"),
		POPN_9K("9KEYS"),
		KEYBOARD_24K("24KEYS"),
		KEYBOARD_24K_DOUBLE("24KEYS DOUBLE");
		
		public final String name;
		
		private PlayMode(String name) {
			this.name = name;
		}
		
		public String toString() {
			return name;
		}
	}
}

