package bms.player.beatoraja.launcher;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.external.ScoreDataImporter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.play.JudgeAlgorithm;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.song.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
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
import twitter4j.conf.ConfigurationBuilder;

/**
 * Beatorajaの設定ダイアログ
 *
 * @author exch
 */
public class PlayConfigurationView implements Initializable {


	// TODO スキンプレビュー機能

	@FXML
	private Hyperlink newversion;

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
	private Tab musicselectTab;
	@FXML
	private Tab optionTab;
	@FXML
	private Tab otherTab;
	@FXML
	private Tab irTab;
	@FXML
	private Tab courseTab;
	@FXML
    private Tab streamTab;
	@FXML
	private HBox controlPanel;

	@FXML
	private ComboBox<String> players;
	@FXML
	private TextField playername;

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
	private CheckBox enableConstant;
	@FXML
	private Spinner<Integer> constFadeinTime;
	@FXML
	private Spinner<Double> hispeedmargin;
	@FXML
	private CheckBox hispeedautoadjust;

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
	private Spinner<Integer> lanecovermarginlow;
	@FXML
	private Spinner<Integer> lanecovermarginhigh;
	@FXML
	private Spinner<Integer> lanecoverswitchduration;
	@FXML
	private CheckBox enableLift;
	@FXML
	private Spinner<Integer> lift;
	@FXML
	private CheckBox enableHidden;
	@FXML
	private Spinner<Integer> hidden;

	@FXML
	private TextField bgmpath;
	@FXML
	private TextField soundpath;

	@FXML
	private NumericSpinner<Integer> notesdisplaytiming;
	@FXML
	private CheckBox notesdisplaytimingautoadjust;
	@FXML
	private CheckBox bpmguide;
	@FXML
	private ComboBox<Integer> gaugeautoshift;
	@FXML
	private ComboBox<Integer> bottomshiftablegauge;
	@FXML
	private CheckBox customjudge;
	@FXML
	private Spinner<Integer> njudgepg;
	@FXML
	private Spinner<Integer> njudgegr;
	@FXML
	private Spinner<Integer> njudgegd;
	@FXML
	private Spinner<Integer> sjudgepg;
	@FXML
	private Spinner<Integer> sjudgegr;
	@FXML
	private Spinner<Integer> sjudgegd;
	@FXML
	private ComboBox<Integer> minemode;
	@FXML
	private ComboBox<Integer> scrollmode;
	@FXML
	private ComboBox<Integer> longnotemode;
	@FXML
	private Slider longnoterate;
	@FXML
	private Spinner<Integer> hranthresholdbpm;
	@FXML
	private ComboBox<Integer> seventoninepattern;
	@FXML
	private ComboBox<Integer> seventoninetype;
	@FXML
	private Spinner<Integer> exitpressduration;
	@FXML
	private CheckBox chartpreview;
	@FXML
	private CheckBox guidese;
	@FXML
	private CheckBox windowhold;
	@FXML
	private Spinner<Integer> extranotedepth;

	@FXML
	private CheckBox judgeregion;
	@FXML
	private CheckBox markprocessednote;
	@FXML
	private CheckBox showhiddennote;
	@FXML
	private ComboBox<String> target;

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
    private CheckBox usecim;

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
	private CheckBox enableIpfs;
	@FXML
	private TextField ipfsurl;

	@FXML
	private VBox skin;
	@FXML
	private VideoConfigurationView videoController;
	@FXML
	private AudioConfigurationView audioController;
	@FXML
	private InputConfigurationView inputController;
	@FXML
	private ResourceConfigurationView resourceController;
	@FXML
	private MusicSelectConfigurationView musicselectController;
	@FXML
	private SkinConfigurationView skinController;
	@FXML
	private IRConfigurationView irController;
	@FXML
	private TableEditorView tableController;
	@FXML
    private StreamEditorView streamController;

	private Config config;
	private PlayerConfig player;

	private MainLoader loader;

	private boolean songUpdated = false;

	private RequestToken requestToken = null;

	@FXML
	public CheckBox discord;

	@FXML
	public CheckBox clipboardScreenshot;

	static void initComboBox(ComboBox<Integer> combo, final String[] values) {
		combo.setCellFactory((param) -> new OptionListCell(values));
		combo.setButtonCell(new OptionListCell(values));
		for (int i = 0; i < values.length; i++) {
			combo.getItems().add(i);
		}
	}

	public void initialize(URL arg0, ResourceBundle arg1) {
		final long t = System.currentTimeMillis();
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
		initComboBox(lntype, new String[] { "LONG NOTE", "CHARGE NOTE", "HELL CHARGE NOTE" });
		initComboBox(gaugeautoshift, new String[] { "NONE", "CONTINUE", "SURVIVAL TO GROOVE","BEST CLEAR","SELECT TO UNDER" });
		initComboBox(bottomshiftablegauge, new String[] { "ASSIST EASY", "EASY", "NORMAL" });
		initComboBox(minemode, new String[] { "OFF", "REMOVE", "ADD RANDOM", "ADD NEAR", "ADD ALL" });
		initComboBox(scrollmode, new String[] { "OFF", "REMOVE", "ADD" });
		initComboBox(longnotemode, new String[] { "OFF", "REMOVE", "ADD LN", "ADD CN", "ADD HCN", "ADD ALL" });

		initComboBox(judgealgorithm, new String[] { arg1.getString("JUDGEALG_LR2"), arg1.getString("JUDGEALG_AC"), arg1.getString("JUDGEALG_BOTTOM_PRIORITY") });
		String[] autosaves = new String[]{arg1.getString("NONE"),arg1.getString("BETTER_SCORE"),arg1.getString("BETTER_OR_SAME_SCORE"),arg1.getString("BETTER_MISSCOUNT")
				,arg1.getString("BETTER_OR_SAME_MISSCOUNT"),arg1.getString("BETTER_COMBO"),arg1.getString("BETTER_OR_SAME_COMBO"),
				arg1.getString("BETTER_LAMP"),arg1.getString("BETTER_OR_SAME_LAMP"),arg1.getString("BETTER_ALL"),arg1.getString("ALWAYS")};
		initComboBox(autosavereplay1, autosaves);
		initComboBox(autosavereplay2, autosaves);
		initComboBox(autosavereplay3, autosaves);
		initComboBox(autosavereplay4, autosaves);

		notesdisplaytiming.setValueFactoryValues(PlayerConfig.JUDGETIMING_MIN, PlayerConfig.JUDGETIMING_MAX, 0, 1);
		resourceController.init(this);

		checkNewVersion();
		Logger.getGlobal().info("初期化時間(ms) : " + (System.currentTimeMillis() - t));
	}

	private void checkNewVersion() {
		Runnable newVersionCheckRunnable = () -> {
			final String message = MainLoader.getVersionChecker().getMessage();
			final String downloadURL = MainLoader.getVersionChecker().getDownloadURL();
			Platform.runLater(() -> {
				newversion.setText(message);
				if(downloadURL != null) {
					newversion.setOnAction(new EventHandler<ActionEvent>() {

						@Override
						public void handle(ActionEvent event) {
							Desktop desktop = Desktop.getDesktop();
							URI uri;
							try {
								uri = new URI(downloadURL);
								desktop.browse(uri);
							} catch (Exception e) {
								Logger.getGlobal().warning("最新版URLアクセス時例外:" + e.getMessage());
							}
						}
					});
				}
			});
		};

		new Thread(newVersionCheckRunnable).start();
	}

	public void setBMSInformationLoader(MainLoader loader) {
		this.loader = loader;
	}

	/**
	 * ダイアログの項目を更新する
	 */
	public void update(Config config) {
		this.config = config;

		players.getItems().setAll(PlayerConfig.readAllPlayerID(config.getPlayerpath()));
		videoController.update(config);
		audioController.update(config.getAudioConfig());
		musicselectController.update(config);

		bgmpath.setText(config.getBgmpath());
		soundpath.setText(config.getSoundpath());

		resourceController.update(config);

		skinController.update(config);
        // int b = Boolean.valueOf(config.getJKOC()).compareTo(false);

        usecim.setSelected(config.isCacheSkinImage());
        discord.setSelected(config.isUseDiscordRPC());
        clipboardScreenshot.setSelected(config.isSetClipboardWhenScreenshot());

		enableIpfs.setSelected(config.isEnableIpfs());
		ipfsurl.setText(config.getIpfsUrl());

		if(players.getItems().contains(config.getPlayername())) {
			players.setValue(config.getPlayername());
		} else {
			players.getSelectionModel().select(0);
		}
		updatePlayer();

		try {
			Class.forName("org.sqlite.JDBC");
			tableController.init(MainLoader.getScoreDatabaseAccessor());
			tableController.update(Paths.get(config.getTablepath() + "/" + "default.json"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void changePlayer() {
		commitPlayer();
		updatePlayer();
	}

	public void addPlayer() {
		String[] ids = PlayerConfig.readAllPlayerID(config.getPlayerpath());
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
				PlayerConfig.create(config.getPlayerpath(), playerid);
				players.getItems().add(playerid);
				break;
			}
		}
	}

	public void updatePlayer() {
		player = PlayerConfig.readPlayerConfig(config.getPlayerpath(), players.getValue());
		playername.setText(player.getName());

		videoController.updatePlayer(player);
		musicselectController.updatePlayer(player);

		scoreop.getSelectionModel().select(player.getRandom());
		scoreop2.getSelectionModel().select(player.getRandom2());
		doubleop.getSelectionModel().select(player.getDoubleoption());
		seventoninepattern.getSelectionModel().select(player.getSevenToNinePattern());
		seventoninetype.getSelectionModel().select(player.getSevenToNineType());
		exitpressduration.getValueFactory().setValue(player.getExitPressDuration());
		chartpreview.setSelected(player.isChartPreview());
		guidese.setSelected(player.isGuideSE());
		windowhold.setSelected(player.isWindowHold());
		gaugeop.getSelectionModel().select(player.getGauge());
		lntype.getSelectionModel().select(player.getLnmode());

		notesdisplaytiming.getValueFactory().setValue(player.getJudgetiming());
		notesdisplaytimingautoadjust.setSelected(player.isNotesDisplayTimingAutoAdjust());

		bpmguide.setSelected(player.isBpmguide());
		gaugeautoshift.setValue(player.getGaugeAutoShift());
		bottomshiftablegauge.setValue(player.getBottomShiftableGauge());

		customjudge.setSelected(player.isCustomJudge());
		njudgepg.getValueFactory().setValue(player.getKeyJudgeWindowRatePerfectGreat());
		njudgegr.getValueFactory().setValue(player.getKeyJudgeWindowRateGreat());
		njudgegd.getValueFactory().setValue(player.getKeyJudgeWindowRateGood());
		sjudgepg.getValueFactory().setValue(player.getScratchJudgeWindowRatePerfectGreat());
		sjudgegr.getValueFactory().setValue(player.getScratchJudgeWindowRateGreat());
		sjudgegd.getValueFactory().setValue(player.getScratchJudgeWindowRateGood());
		minemode.getSelectionModel().select(player.getMineMode());
		scrollmode.getSelectionModel().select(player.getScrollMode());
		longnotemode.getSelectionModel().select(player.getLongnoteMode());
		longnoterate.setValue(player.getLongnoteRate());
		hranthresholdbpm.getValueFactory().setValue(player.getHranThresholdBPM());
		judgeregion.setSelected(player.isShowjudgearea());
		markprocessednote.setSelected(player.isMarkprocessednote());
		extranotedepth.getValueFactory().setValue(player.getExtranoteDepth());

		autosavereplay1.getSelectionModel().select(player.getAutoSaveReplay()[0]);
		autosavereplay2.getSelectionModel().select(player.getAutoSaveReplay()[1]);
		autosavereplay3.getSelectionModel().select(player.getAutoSaveReplay()[2]);
		autosavereplay4.getSelectionModel().select(player.getAutoSaveReplay()[3]);

		String[] targets = player.getTargetlist();
		target.getItems().setAll(targets);
		target.setValue(player.getTargetid());
		showhiddennote.setSelected(player.isShowhiddennote());

		irController.update(player);
		streamController.update(player);

		txtTwitterPIN.setDisable(true);
		twitterPINButton.setDisable(true);
		if(player.getTwitterAccessToken() != null && !player.getTwitterAccessToken().isEmpty()) {
			txtTwitterAuthenticated.setVisible(true);
		} else {
			txtTwitterAuthenticated.setVisible(false);
		}

		pc = null;
		playconfig.setValue(PlayMode.BEAT_7K);
		updatePlayConfig();

		inputController.update(player);
		skinController.update(player);
	}

	/**
	 * ダイアログの項目をconfig.xmlに反映する
	 */
	public void commit() {
	    videoController.commit(config);
		audioController.commit();
		musicselectController.commit();

		config.setPlayername(players.getValue());

		config.setBgmpath(bgmpath.getText());
		config.setSoundpath(soundpath.getText());

		resourceController.commit();

        // jkoc_hack is integer but *.setJKOC needs boolean type

        config.setCacheSkinImage(usecim.isSelected());

		config.setEnableIpfs(enableIpfs.isSelected());
		config.setIpfsUrl(ipfsurl.getText());

		config.setUseDiscordRPC(discord.isSelected());
		config.setClipboardWhenScreenshot(clipboardScreenshot.isSelected());

		commitPlayer();

		Config.write(config);

		tableController.commit();
	}

	public void commitPlayer() {
		if(player == null) {
			return;
		}
		if(playername.getText().length() > 0) {
			player.setName(playername.getText());
		}

		videoController.commitPlayer(player);
		musicselectController.commitPlayer();

		player.setRandom(scoreop.getValue());
		player.setRandom2(scoreop2.getValue());
		player.setDoubleoption(doubleop.getValue());
		player.setSevenToNinePattern(seventoninepattern.getValue());
		player.setSevenToNineType(seventoninetype.getValue());
		player.setExitPressDuration(getValue(exitpressduration));
		player.setChartPreview(chartpreview.isSelected());
		player.setGuideSE(guidese.isSelected());
		player.setWindowHold(windowhold.isSelected());
		player.setGauge(gaugeop.getValue());
		player.setLnmode(lntype.getValue());
		player.setJudgetiming(getValue(notesdisplaytiming));
		player.setNotesDisplayTimingAutoAdjust(notesdisplaytimingautoadjust.isSelected());

		player.setBpmguide(bpmguide.isSelected());
		player.setGaugeAutoShift(gaugeautoshift.getValue());
		player.setBottomShiftableGauge(bottomshiftablegauge.getValue());
		player.setCustomJudge(customjudge.isSelected());
		player.setKeyJudgeWindowRatePerfectGreat(getValue(njudgepg));
		player.setKeyJudgeWindowRateGreat(getValue(njudgegr));
		player.setKeyJudgeWindowRateGood(getValue(njudgegd));
		player.setScratchJudgeWindowRatePerfectGreat(getValue(sjudgepg));
		player.setScratchJudgeWindowRateGreat(getValue(sjudgegr));
		player.setScratchJudgeWindowRateGood(getValue(sjudgegd));
		player.setMineMode(minemode.getValue());
		player.setScrollMode(scrollmode.getValue());
		player.setLongnoteMode(longnotemode.getValue());
		player.setLongnoteRate(longnoterate.getValue());
		player.setHranThresholdBPM(getValue(hranthresholdbpm));
		player.setMarkprocessednote(markprocessednote.isSelected());
		player.setExtranoteDepth(extranotedepth.getValue());

		player.setAutoSaveReplay( new int[]{autosavereplay1.getValue(),autosavereplay2.getValue(),
				autosavereplay3.getValue(),autosavereplay4.getValue()});

		player.setShowjudgearea(judgeregion.isSelected());
		player.setTargetid(target.getValue());

		player.setShowhiddennote(showhiddennote.isSelected());

		inputController.commit();
		irController.commit();
		streamController.commit();

		updatePlayConfig();
		skinController.commit();

		PlayerConfig.write(config.getPlayerpath(), player);
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

    private String showFileChooser(String title) {
    	FileChooser chooser = new FileChooser();
		chooser.setTitle(title);
		File f = chooser.showOpenDialog(null);
		return f != null ? f.getPath() : null;
    }

    private String showDirectoryChooser(String title) {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle(title);
		File f = chooser.showDialog(null);
		return f != null ? f.getPath() : null;
    }

	private PlayMode pc = null;

    @FXML
	public void updatePlayConfig() {
		if (pc != null) {
			PlayConfig conf = player.getPlayConfig(Mode.valueOf(pc.name())).getPlayconfig();
			conf.setHispeed(getValue(hispeed).floatValue());
			conf.setDuration(getValue(gvalue));
			conf.setEnableConstant(enableConstant.isSelected());
			conf.setConstantFadeinTime(getValue(constFadeinTime));
			conf.setHispeedMargin(getValue(hispeedmargin).floatValue());
			conf.setFixhispeed(fixhispeed.getValue());
			conf.setEnablelanecover(enableLanecover.isSelected());
			conf.setLanecover(getValue(lanecover) / 1000f);
			conf.setLanecovermarginlow(getValue(lanecovermarginlow) / 1000f);
			conf.setLanecovermarginhigh(getValue(lanecovermarginhigh) / 1000f);
			conf.setLanecoverswitchduration(getValue(lanecoverswitchduration));
			conf.setEnablelift(enableLift.isSelected());
			conf.setEnablehidden(enableHidden.isSelected());
			conf.setLift(getValue(lift) / 1000f);
			conf.setHidden(getValue(hidden) / 1000f);
			conf.setJudgetype(JudgeAlgorithm.values()[judgealgorithm.getValue()].name());
			conf.setHispeedAutoAdjust(hispeedautoadjust.isSelected());
		}
		pc = playconfig.getValue();
		PlayConfig conf = player.getPlayConfig(Mode.valueOf(pc.name())).getPlayconfig();
		hispeed.getValueFactory().setValue((double) conf.getHispeed());
		gvalue.getValueFactory().setValue(conf.getDuration());
		enableConstant.setSelected(conf.isEnableConstant());
		constFadeinTime.getValueFactory().setValue(conf.getConstantFadeinTime());
		hispeedmargin.getValueFactory().setValue((double) conf.getHispeedMargin());
		fixhispeed.setValue(conf.getFixhispeed());
		enableLanecover.setSelected(conf.isEnablelanecover());
		lanecover.getValueFactory().setValue((int) (conf.getLanecover() * 1000));
		lanecovermarginlow.getValueFactory().setValue((int) (conf.getLanecovermarginlow() * 1000));
		lanecovermarginhigh.getValueFactory().setValue((int) (conf.getLanecovermarginhigh() * 1000));
		lanecoverswitchduration.getValueFactory().setValue(conf.getLanecoverswitchduration());
		enableLift.setSelected(conf.isEnablelift());
		enableHidden.setSelected(conf.isEnablehidden());
		lift.getValueFactory().setValue((int) (conf.getLift() * 1000));
		hidden.getValueFactory().setValue((int) (conf.getHidden() * 1000));
		judgealgorithm.setValue(JudgeAlgorithm.getIndex(conf.getJudgetype()));
		hispeedautoadjust.setSelected(conf.isEnableHispeedAutoAdjust());
	}

	private <T> T getValue(Spinner<T> spinner) {
		spinner.getValueFactory()
				.setValue(spinner.getValueFactory().getConverter().fromString(spinner.getEditor().getText()));
		return spinner.getValue();
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
		streamTab.setDisable(true);
		controlPanel.setDisable(true);

		MainLoader.play(null, bms.player.beatoraja.BMSPlayerMode.PLAY, true, config, player, songUpdated);
	}

    @FXML
	public void loadAllBMS() {
		commit();
		loadBMS(null, true);
	}

    @FXML
	public void loadDiffBMS() {
		commit();
		loadBMS(null, false);
	}

	public void loadBMSPath(String updatepath){
		commit();
    	loadBMS(updatepath, false);
	}

	/**
	 * BMSを読み込み、楽曲データベースを更新する
	 *
	 * @param updateAll
	 *            falseの場合は追加削除分のみを更新する
	 */
	public void loadBMS(String updatepath, boolean updateAll) {
		commit();
		try {
			SongDatabaseAccessor songdb = MainLoader.getScoreDatabaseAccessor();
			SongInformationAccessor infodb = config.isUseSongInfo() ?
					new SongInformationAccessor(Paths.get("songinfo.db").toString()) : null;
			Logger.getGlobal().info("song.db更新開始");
			songdb.updateSongDatas(updatepath, config.getBmsroot(), updateAll, infodb);
			Logger.getGlobal().info("song.db更新完了");
			songUpdated = true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
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

		try {
			Class.forName("org.sqlite.JDBC");
			SongDatabaseAccessor songdb = MainLoader.getScoreDatabaseAccessor();
			String player = players.getValue();
			ScoreDatabaseAccessor scoredb = new ScoreDatabaseAccessor(config.getPlayerpath() + File.separatorChar + player + File.separatorChar + "score.db");
			scoredb.createTable();

			ScoreDataImporter scoreimporter = new ScoreDataImporter(scoredb);
			scoreimporter.importFromLR2ScoreDatabase(dir.getPath(), songdb);

		} catch (ClassNotFoundException e1) {
		}

	}

	@FXML
	public void startTwitterAuth() {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setOAuthConsumerKey(txtTwitterConsumerKey.getText());
		cb.setOAuthConsumerSecret(txtTwitterConsumerSecret.getText());
		cb.setOAuthAccessToken(null);
		cb.setOAuthAccessTokenSecret(null);
		TwitterFactory twitterfactory = new TwitterFactory(cb.build());
		Twitter twitter = twitterfactory.getInstance();
		try {
			requestToken = twitter.getOAuthRequestToken();
			Desktop desktop = Desktop.getDesktop();
			URI uri = new URI(requestToken.getAuthorizationURL());
			desktop.browse(uri);
			player.setTwitterConsumerKey(txtTwitterConsumerKey.getText());
			player.setTwitterConsumerSecret(txtTwitterConsumerSecret.getText());
			player.setTwitterAccessToken("");
			player.setTwitterAccessTokenSecret("");
			txtTwitterPIN.setDisable(false);
			twitterPINButton.setDisable(false);
			txtTwitterAuthenticated.setVisible(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void startPINAuth() {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setOAuthConsumerKey(player.getTwitterConsumerKey());
		cb.setOAuthConsumerSecret(player.getTwitterConsumerSecret());
		cb.setOAuthAccessToken(null);
		cb.setOAuthAccessTokenSecret(null);
		TwitterFactory twitterfactory = new TwitterFactory(cb.build());
		Twitter twitter = twitterfactory.getInstance();
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

	static class OptionListCell extends ListCell<Integer> {

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

	enum PlayMode {
		BEAT_5K("5KEYS"),
		BEAT_7K("7KEYS"),
		BEAT_10K("10KEYS"),
		BEAT_14K("14KEYS"),
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

