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
	private Spinner<Double> hispeedmargin;

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
	private NumericSpinner<Integer> judgetiming;
	@FXML
	private CheckBox constant;
	@FXML
	private CheckBox bpmguide;
	@FXML
	private CheckBox legacy;
	@FXML
	private ComboBox<Integer> gaugeautoshift;
	@FXML
	private ComboBox<Integer> bottomshiftablegauge;
	@FXML
	private Spinner<Integer> exjudge;
	@FXML
	private ComboBox<Integer> minemode;
	@FXML
	private ComboBox<Integer> scrollmode;
	@FXML
	private Spinner<Integer> hranthresholdbpm;
	@FXML
	private ComboBox<Integer> seventoninepattern;
	@FXML
	private ComboBox<Integer> seventoninetype;
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
	private ComboBox<Integer> target;

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

	private Config config;
	private PlayerConfig player;

	private MainLoader loader;

	private boolean songUpdated = false;

	private RequestToken requestToken = null;

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

		judgetiming.setValueFactoryValues(PlayerConfig.JUDGETIMING_MIN, PlayerConfig.JUDGETIMING_MAX, 0, 1);
		resourceController.init(this);

		newVersionCheck();
		Logger.getGlobal().info("初期化時間(ms) : " + (System.currentTimeMillis() - t));
	}

	@JsonIgnoreProperties(ignoreUnknown=true)
	static class GithubLastestRelease{
		public String name;
	}

        private void newVersionCheck() {
        	Runnable newVersionCheckRunnable = () -> {
        	    try {
        		URL url = new URL("https://api.github.com/repos/exch-bms2/beatoraja/releases/latest");
        		ObjectMapper mapper = new ObjectMapper();
        		GithubLastestRelease lastestData = mapper.readValue(url, GithubLastestRelease.class);
        		final String name = lastestData.name;
        		final String downloadURL = "https://mocha-repository.info/download/beatoraja" + name + ".zip";
        		Platform.runLater(() -> {
        
        		    if (MainController.VERSION.contains(name)) {
        			newversion.setText("最新版を利用中です");
        		    } else {
        			newversion.setText(String.format("最新版[%s]を利用可能です。", name));
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
        	    } catch (Exception e) {
        		Logger.getGlobal().warning("最新版URL取得時例外:" + e.getMessage());
        	    }
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
		audioController.update(config);
		musicselectController.update(config);

		bgmpath.setText(config.getBgmpath());
		soundpath.setText(config.getSoundpath());

		resourceController.update(config);

		showhiddennote.setSelected(config.isShowhiddennote());

		autosavereplay1.getSelectionModel().select(config.getAutoSaveReplay()[0]);
		autosavereplay2.getSelectionModel().select(config.getAutoSaveReplay()[1]);
		autosavereplay3.getSelectionModel().select(config.getAutoSaveReplay()[2]);
		autosavereplay4.getSelectionModel().select(config.getAutoSaveReplay()[3]);

		skinController.update(config);
        // int b = Boolean.valueOf(config.getJKOC()).compareTo(false);

        usecim.setSelected(config.isCacheSkinImage());

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
		guidese.setSelected(player.isGuideSE());
		windowhold.setSelected(player.isWindowHold());
		gaugeop.getSelectionModel().select(player.getGauge());
		lntype.getSelectionModel().select(player.getLnmode());

		judgetiming.getValueFactory().setValue(player.getJudgetiming());

		constant.setSelected(player.isConstant());
		bpmguide.setSelected(player.isBpmguide());
		legacy.setSelected(player.isLegacynote());
		gaugeautoshift.setValue(player.getGaugeAutoShift());
		bottomshiftablegauge.setValue(player.getBottomShiftableGauge());

		exjudge.getValueFactory().setValue(player.getJudgewindowrate());
		minemode.getSelectionModel().select(player.getMineMode());
		scrollmode.getSelectionModel().select(player.getScrollMode());
		hranthresholdbpm.getValueFactory().setValue(player.getHranThresholdBPM());
		judgeregion.setSelected(player.isShowjudgearea());
		markprocessednote.setSelected(player.isMarkprocessednote());
		extranotedepth.getValueFactory().setValue(player.getExtranoteDepth());

		target.setValue(player.getTarget());

		irController.update(player);

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

		resourceController.commit(config);

		config.setShowhiddennote(showhiddennote.isSelected());

		config.setAutoSaveReplay( new int[]{autosavereplay1.getValue(),autosavereplay2.getValue(),
				autosavereplay3.getValue(),autosavereplay4.getValue()});

        // jkoc_hack is integer but *.setJKOC needs boolean type

        config.setCacheSkinImage(usecim.isSelected());

		config.setEnableIpfs(enableIpfs.isSelected());
		config.setIpfsUrl(ipfsurl.getText());

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
		player.setGuideSE(guidese.isSelected());
		player.setWindowHold(windowhold.isSelected());
		player.setGauge(gaugeop.getValue());
		player.setLnmode(lntype.getValue());
		player.setJudgetiming(getValue(judgetiming));

		player.setConstant(constant.isSelected());
		player.setBpmguide(bpmguide.isSelected());
		player.setLegacynote(legacy.isSelected());
		player.setGaugeAutoShift(gaugeautoshift.getValue());
		player.setBottomShiftableGauge(bottomshiftablegauge.getValue());
		player.setJudgewindowrate(getValue(exjudge));
		player.setMineMode(minemode.getValue());
		player.setScrollMode(scrollmode.getValue());
		player.setHranThresholdBPM(getValue(hranthresholdbpm));
		player.setMarkprocessednote(markprocessednote.isSelected());
		player.setExtranoteDepth(extranotedepth.getValue());

		player.setShowjudgearea(judgeregion.isSelected());
		player.setTarget(target.getValue());

		inputController.commit();
		irController.commit();

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

		}
		pc = playconfig.getValue();
		PlayConfig conf = player.getPlayConfig(Mode.valueOf(pc.name())).getPlayconfig();
		hispeed.getValueFactory().setValue((double) conf.getHispeed());
		gvalue.getValueFactory().setValue(conf.getDuration());
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
		controlPanel.setDisable(true);

		MainLoader.play(null, bms.player.beatoraja.PlayerResource.PlayMode.PLAY, true, config, player, songUpdated);
	}

    @FXML
	public void loadAllBMS() {
		loadBMS(null, true);
	}

    @FXML
	public void loadDiffBMS() {
		loadBMS(null, false);
	}

	public void loadBMSPath(String updatepath){
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
			songdb.updateSongDatas(updatepath, updateAll, infodb);
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
			Files.createDirectories(Paths.get(config.getTablepath()));
		} catch (IOException e) {
		}

		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(config.getTablepath()))) {
			paths.forEach((p) -> {
				if(p.toString().toLowerCase().endsWith(".bmt")) {
					try {
						Files.deleteIfExists(p);
					} catch (IOException e) {
					}					
				}
			});
		} catch (IOException e) {
		}

		TableDataAccessor tda = new TableDataAccessor(config.getTablepath());
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

		try {
			Class.forName("org.sqlite.JDBC");
			SongDatabaseAccessor songdb = MainLoader.getScoreDatabaseAccessor();
			String player = "player1";
			ScoreDatabaseAccessor scoredb = new ScoreDatabaseAccessor(config.getPlayerpath() + "/" + player + "/score.db");
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

