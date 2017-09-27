package bms.player.beatoraja;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.portaudio.DeviceInfo;
import com.synthbot.jasiohost.AsioDriver;

import bms.player.beatoraja.audio.PortAudioDriver;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.play.JudgeAlgorithm;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinHeader.*;
import bms.player.beatoraja.skin.JSONSkinLoader;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.skin.lr2.LR2SkinHeaderLoader;
import bms.player.beatoraja.song.SQLiteSongDatabaseAccessor;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;
import bms.player.beatoraja.song.SongInformationAccessor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
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
import javafx.util.Callback;

/**
 * Beatorajaの設定ダイアログ
 *
 * @author exch
 */
public class PlayConfigurationView implements Initializable {

	// TODO スキンプレビュー機能

	@FXML
	private ComboBox<Resolution> resolution;

	@FXML
	private ComboBox<Integer> playconfig;
	/**
	 * ハイスピード
	 */
	@FXML
	private Spinner<Double> hispeed;

	@FXML
	private GridPane lr2configuration;
	@FXML
	private ComboBox<Integer> fixhispeed;
	@FXML
	private Spinner<Integer> gvalue;
	@FXML
	private Spinner<Integer> inputduration;
	@FXML
	private CheckBox fullscreen;
	@FXML
	private CheckBox vsync;
	@FXML
	private ComboBox<Integer> bgaop;
	@FXML
	private ComboBox<Integer> bgaexpand;

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
	private CheckBox exjudge;
	@FXML
	private CheckBox nomine;

	@FXML
	private CheckBox judgeregion;
	@FXML
	private CheckBox markprocessednote;
	@FXML
	private CheckBox showhiddennote;
	@FXML
	private ComboBox<Integer> target;

	@FXML
	private Spinner<Integer> maxfps;
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
	private ComboBox<Integer> judgealgorithm;
	@FXML
	private Spinner<Integer> misslayertime;

    @FXML
	private ComboBox<Integer> autosavereplay1;
	@FXML
	private ComboBox<Integer> autosavereplay2;
	@FXML
	private ComboBox<Integer> autosavereplay3;
	@FXML
	private ComboBox<Integer> autosavereplay4;

    @FXML
    private ComboBox<Integer> jkoc_hack;
    @FXML
    private CheckBox usecim;
    @FXML
    private CheckBox useSongInfo;

	private Config config;
	private PlayerConfig player;
	@FXML
	private CheckBox folderlamp;

	private SkinConfigurationView skinview;
	@FXML
	private ComboBox<SkinType> skincategory;
	@FXML
	private ComboBox<SkinHeader> skin;
	@FXML
	private ScrollPane skinconfig;

	@FXML
	private ComboBox<String> irname;
	@FXML
	private TextField iruserid;
	@FXML
	private PasswordField irpassword;

	private MainLoader loader;

	private boolean songUpdated = false;

	private void initComboBox(ComboBox<Integer> combo, final String[] values) {
		combo.setCellFactory(new Callback<ListView<Integer>, ListCell<Integer>>() {
			public ListCell<Integer> call(ListView<Integer> param) {
				return new OptionListCell(values);
			}
		});
		combo.setButtonCell(new OptionListCell(values));
		for (int i = 0; i < values.length; i++) {
			combo.getItems().add(i);
		}
	}

	public void initialize(URL arg0, ResourceBundle arg1) {
		lr2configuration.setHgap(25);
		lr2configuration.setVgap(4);

		resolution.setCellFactory(new Callback<ListView<Resolution>, ListCell<Resolution>>() {
			public ListCell<Resolution> call(ListView<Resolution> param) {
				return new ResolutionListCell();
			}
		});
		resolution.setButtonCell(new ResolutionListCell());
		initComboBox(scoreop, new String[] { "OFF", "MIRROR", "RANDOM", "R-RANDOM", "S-RANDOM", "SPIRAL", "H-RANDOM",
				"ALL-SCR", "RANDOM-EX", "S-RANDOM-EX" });
		initComboBox(gaugeop, new String[] { "ASSIST EASY", "EASY", "NORMAL", "HARD", "EX-HARD", "HAZARD" });
		initComboBox(bgaop, new String[] { "ON", "AUTOPLAY ", "OFF" });
		initComboBox(bgaexpand, new String[] { "Full", "Keep Aspect Ratio", "Off" });
                initComboBox(jkoc_hack, new String[] {"False", "True"});
		initComboBox(fixhispeed, new String[] { "OFF", "START BPM", "MAX BPM", "MAIN BPM", "MIN BPM" });
		initComboBox(playconfig, new String[] { "5/7KEYS", "10/14KEYS", "9KEYS", "24KEYS", "24KEYS DOUBLE" });
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
		skincategory.setCellFactory(new Callback<ListView<SkinType>, ListCell<SkinType>>() {
			public ListCell<SkinType> call(ListView<SkinType> param) { return new SkinTypeCell(); }
		});
		skincategory.setButtonCell(new SkinTypeCell());
		skincategory.getItems().addAll(SkinType.values());

		initComboBox(audio, new String[] { "OpenAL (LibGDX Sound)", "OpenAL (LibGDX AudioDevice)", "PortAudio", "Java ASIO Host" });
		audio.getItems().setAll(0, 2, 3);

		skin.setCellFactory(new Callback<ListView<SkinHeader>, ListCell<SkinHeader>>() {
			public ListCell<SkinHeader> call(ListView<SkinHeader> param) {
				return new SkinListCell();
			}
		});
		skin.setButtonCell(new SkinListCell());

		irname.getItems().setAll(IRConnection.AVAILABLE);
	}

	public void setBMSInformationLoader(MainLoader loader) {
		this.loader = loader;
	}

	/**
	 * ダイアログの項目を更新する
	 */
	public void update(Config config) {
		this.config = config;
		fullscreen.setSelected(config.isFullscreen());
		updateResolutions();
		resolution.setValue(config.getResolution());
		vsync.setSelected(config.isVsync());
		bgaop.setValue(config.getBga());
		bgaexpand.setValue(config.getBgaExpand());
		systemvolume.setValue((double)config.getSystemvolume());
		keyvolume.setValue((double)config.getKeyvolume());
		bgvolume.setValue((double)config.getBgvolume());

		bgmpath.setText(config.getBgmpath());
		soundpath.setText(config.getSoundpath());

		bmsroot.getItems().setAll(config.getBmsroot());
		updatesong.setSelected(config.isUpdatesong());
		tableurl.getItems().setAll(config.getTableURL());

		audio.setValue(config.getAudioDriver());
		maxfps.getValueFactory().setValue(config.getMaxFramePerSecond());
		audiobuffer.getValueFactory().setValue(config.getAudioDeviceBufferSize());
		audiosim.getValueFactory().setValue(config.getAudioDeviceSimultaneousSources());
		showhiddennote.setSelected(config.isShowhiddennote());

		judgealgorithm.setValue(JudgeAlgorithm.getIndex(config.getJudgealgorithm()));

		autosavereplay1.getSelectionModel().select(config.getAutoSaveReplay()[0]);
		autosavereplay2.getSelectionModel().select(config.getAutoSaveReplay()[1]);
		autosavereplay3.getSelectionModel().select(config.getAutoSaveReplay()[2]);
		autosavereplay4.getSelectionModel().select(config.getAutoSaveReplay()[3]);

        // int b = Boolean.valueOf(config.getJKOC()).compareTo(false);

        jkoc_hack.setValue(Boolean.valueOf(config.getJKOC()).compareTo(false));
        usecim.setSelected(config.isCacheSkinImage());
        useSongInfo.setSelected(config.isUseSongInfo());

		folderlamp.setSelected(config.isFolderlamp());

		inputduration.getValueFactory().setValue(config.getInputduration());

		skinview = new SkinConfigurationView();

		updateAudioDriver();
		updatePlayer();
	}

	@FXML
	public void updateResolutions() {
		Resolution oldValue = resolution.getValue();
		resolution.getItems().clear();
		if (fullscreen.isSelected()) {
			Graphics.DisplayMode[] displays = MainLoader.getAvailableDisplayMode();
			for(Resolution r : Resolution.values()) {
				for(Graphics.DisplayMode display : displays) {
					if(display.width == r.width && display.height == r.height) {
						resolution.getItems().add(r);
						break;
					}
				}
			}
		} else {
			Graphics.DisplayMode display = MainLoader.getDesktopDisplayMode();
			for(Resolution r : Resolution.values()) {
				if (r.width <= display.width && r.height <= display.height) {
					resolution.getItems().add(r);
				}
			}
		}
		resolution.setValue(resolution.getItems().contains(oldValue)
				? oldValue : resolution.getItems().get(resolution.getItems().size() - 1));
	}

	public void updatePlayer() {
		player = MainLoader.readPlayerConfig(config.getPlayername());

		scoreop.getSelectionModel().select(player.getRandom());
		gaugeop.getSelectionModel().select(player.getGauge());
		lntype.getSelectionModel().select(player.getLnmode());

		fixhispeed.setValue(player.getFixhispeed());
		judgetiming.getValueFactory().setValue(player.getJudgetiming());

		constant.setSelected(player.isConstant());
		bpmguide.setSelected(player.isBpmguide());
		legacy.setSelected(player.isLegacynote());
		exjudge.setSelected(player.isExpandjudge());
		nomine.setSelected(player.isNomine());
		judgeregion.setSelected(player.isShowjudgearea());
		markprocessednote.setSelected(player.isMarkprocessednote());
		target.setValue(player.getTarget());

		misslayertime.getValueFactory().setValue(player.getMisslayerDuration());

		irname.setValue(player.getIrname());
		iruserid.setText(player.getUserid());
		irpassword.setText(player.getPassword());

		playconfig.setValue(0);
		updatePlayConfig();
		skincategory.setValue(SkinType.PLAY_7KEYS);
		updateSkinCategory();
	}

	/**
	 * ダイアログの項目をconfig.xmlに反映する
	 */
	public void commit() {
		config.setResolution(resolution.getValue());
		config.setFullscreen(fullscreen.isSelected());
		config.setVsync(vsync.isSelected());
		config.setBga(bgaop.getValue());
		config.setBgaExpand(bgaexpand.getValue());

		config.setBgmpath(bgmpath.getText());
		config.setSoundpath(soundpath.getText());
		config.setSystemvolume((float) systemvolume.getValue());
		config.setKeyvolume((float) keyvolume.getValue());
		config.setBgvolume((float) bgvolume.getValue());

		config.setBmsroot(bmsroot.getItems().toArray(new String[0]));
		config.setUpdatesong(updatesong.isSelected());
		config.setTableURL(tableurl.getItems().toArray(new String[0]));

		config.setShowhiddennote(showhiddennote.isSelected());

		config.setAudioDriver(audio.getValue());
		config.setAudioDriverName(audioname.getValue());
		config.setMaxFramePerSecond(getValue(maxfps));
		config.setAudioDeviceBufferSize(getValue(audiobuffer));
		config.setAudioDeviceSimultaneousSources(getValue(audiosim));

		config.setJudgealgorithm(JudgeAlgorithm.values()[judgealgorithm.getValue()]);
		config.setAutoSaveReplay( new int[]{autosavereplay1.getValue(),autosavereplay2.getValue(),
				autosavereplay3.getValue(),autosavereplay4.getValue()});

        // jkoc_hack is integer but *.setJKOC needs boolean type
        if(jkoc_hack.getValue() > 0)
            config.setJKOC(true);
        else
            config.setJKOC(false);

        config.setCacheSkinImage(usecim.isSelected());
        config.setUseSongInfo(useSongInfo.isSelected());
        config.setFolderlamp(folderlamp.isSelected());

		config.setInputduration(getValue(inputduration));

		commitPlayer();

		Json json = new Json();
		json.setOutputType(OutputType.json);
		try (FileWriter fw = new FileWriter("config.json")) {
			fw.write(json.prettyPrint(config));
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void commitPlayer() {
		Path p = Paths.get("player/" + config.getPlayername() + "/config.json");

		player.setRandom(scoreop.getValue());
		player.setGauge(gaugeop.getValue());
		player.setLnmode(lntype.getValue());
		player.setFixhispeed(fixhispeed.getValue());
		player.setJudgetiming(getValue(judgetiming));

		player.setConstant(constant.isSelected());
		player.setBpmguide(bpmguide.isSelected());
		player.setLegacynote(legacy.isSelected());
		player.setExpandjudge(exjudge.isSelected());
		player.setNomine(nomine.isSelected());
		player.setMarkprocessednote(markprocessednote.isSelected());

		player.setShowjudgearea(judgeregion.isSelected());
		player.setTarget(target.getValue());

		player.setMisslayerDuration(getValue(misslayertime));

		player.setIrname(irname.getValue());
		player.setUserid(iruserid.getText());
		player.setPassword(irpassword.getText());

		updatePlayConfig();
		updateSkinCategory();

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
			tableurl.getItems().add(url.getText());
		}
	}

    @FXML
	public void removeTableURL() {
		tableurl.getItems().removeAll(tableurl.getSelectionModel().getSelectedItems());
	}

	private int mode = -1;

	private int pc = -1;

	private PlayConfig getPlayConfig() {
		switch (pc) {
			case 0:
				return player.getMode7();
			case 1:
				return player.getMode14();
			case 2:
				return player.getMode9();
			case 3:
				return player.getMode24();
			case 4:
				return player.getMode24double();
			default:
				return player.getMode7();
		}
	}

    @FXML
	public void updatePlayConfig() {
		if (pc != -1) {
			PlayConfig conf = getPlayConfig();
			conf.setHispeed(getValue(hispeed).floatValue());
			conf.setDuration(getValue(gvalue));
			conf.setEnablelanecover(enableLanecover.isSelected());
			conf.setLanecover(getValue(lanecover) / 1000f);
			conf.setEnablelift(enableLift.isSelected());
			conf.setLift(getValue(lift) / 1000f);
		}
		pc = playconfig.getValue();
		PlayConfig conf = getPlayConfig();
		hispeed.getValueFactory().setValue((double) conf.getHispeed());
		gvalue.getValueFactory().setValue(conf.getDuration());
		enableLanecover.setSelected(conf.isEnablelanecover());
		lanecover.getValueFactory().setValue((int) (conf.getLanecover() * 1000));
		enableLift.setSelected(conf.isEnablelift());
		lift.getValueFactory().setValue((int) (conf.getLift() * 1000));
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
		case Config.AUDIODRIVER_ASIO:
			try {
				List<String> drivers = AsioDriver.getDriverNames();
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
				audiobuffer.setDisable(true);
				audiosim.setDisable(false);
			} catch(Throwable e) {
				Logger.getGlobal().severe("ASIOは選択できません : " + e.getMessage());
				audio.setValue(Config.AUDIODRIVER_SOUND);
			}
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
	public void updateSkinCategory() {
		if (skinview.getSelectedHeader() != null) {
			SkinHeader header = skinview.getSelectedHeader();
			SkinConfig skin = new SkinConfig(header.getPath().toString());
			skin.setProperty(skinview.getProperty());
			player.getSkin()[header.getSkinType().getId()] = skin;
		} else if (mode != -1) {
			player.getSkin()[mode] = null;
		}

		skin.getItems().clear();
		SkinHeader[] headers = skinview.getSkinHeader(skincategory.getValue());
		skin.getItems().addAll(headers);
		mode = skincategory.getValue().getId();
		if (player.getSkin()[skincategory.getValue().getId()] != null) {
			SkinConfig skinconf = player.getSkin()[skincategory.getValue().getId()];
			if (skinconf != null) {
				for (SkinHeader header : skin.getItems()) {
					if (header != null && header.getPath().equals(Paths.get(skinconf.getPath()))) {
						skin.setValue(header);
						skinconfig.setContent(skinview.create(skin.getValue(), skinconf.getProperty()));
						break;
					}
				}
			} else {
				skin.getSelectionModel().select(0);
			}
		}
	}

    @FXML
	public void updateSkin() {
		skinconfig.setContent(skinview.create(skin.getValue(), null));
	}

    @FXML
	public void start() {
		commit();
		loader.hide();
		MainLoader.play(null, 0, true, songUpdated);
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
					config.getBmsroot());
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
					config.getBmsroot());
			String player = "player1";
			ScoreDatabaseAccessor scoredb = new ScoreDatabaseAccessor(player);
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
						sd.setMinbp((int) score.get("minbp"));
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
	public void exit() {
		commit();
		Platform.exit();
		System.exit(0);
	}

	static class ResolutionListCell extends ListCell<Resolution> {

		@Override
		protected void updateItem(Resolution arg0, boolean arg1) {
			super.updateItem(arg0, arg1);
			if (arg0 != null) {
				setText(arg0.name() + " (" + arg0.width + " x " + arg0.height + ")");
			}
		}
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

	class SkinListCell extends ListCell<SkinHeader> {

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

	class SkinTypeCell extends ListCell<SkinType> {

		@Override
		protected void updateItem(SkinType arg0, boolean arg1) {
			super.updateItem(arg0, arg1);
			if (arg0 != null) {
				setText(arg0.getName());
			}
		}
	}

}

/**
 * スキンコンフィグ
 *
 * @author exch
 */
class SkinConfigurationView {

	private List<SkinHeader> lr2skinheader = new ArrayList<SkinHeader>();

	private SkinHeader selected = null;
	private Map<CustomOption, ComboBox<String>> optionbox = new HashMap<CustomOption, ComboBox<String>>();
	private Map<CustomFile, ComboBox<String>> filebox = new HashMap<CustomFile, ComboBox<String>>();
	private Map<CustomOffset, Spinner<Integer>[]> offsetbox = new HashMap<CustomOffset, Spinner<Integer>[]>();

	public SkinConfigurationView() {
		List<Path> lr2skinpaths = new ArrayList<Path>();
		scan(Paths.get("skin"), lr2skinpaths);
		for (Path path : lr2skinpaths) {
			if (path.toString().toLowerCase().endsWith(".json")) {
				JSONSkinLoader loader = new JSONSkinLoader();
				SkinHeader header = loader.loadHeader(path);
				if (header != null) {
					lr2skinheader.add(header);
				}
			} else {
				LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
				try {
					SkinHeader header = loader.loadSkin(path, null);
					// System.out.println(path.toString() + " : " +
					// header.getName()
					// + " - " + header.getMode());
					if(header.getType() == SkinHeader.TYPE_LR2SKIN && header.getSkinType().isPlay()) {
						List<CustomOption> l = new ArrayList(Arrays.asList(header.getCustomOptions()));
						l.add(new CustomOption("BGA Size", new int[]{30,31}, new String[]{"Normal", "Extend"}));
						l.add(new CustomOption("Ghost", new int[]{34,35,36,37}, new String[]{"Off", "Type A", "Type B", "Type C"}));
						l.add(new CustomOption("Score Graph", new int[]{38,39}, new String[]{"Off", "On"}));
						l.add(new CustomOption("Judge Detail", new int[]{1997,1998,1999}, new String[]{"Off", "EARLY/LATE", "+-ms"}));
						header.setCustomOptions(l.toArray(new CustomOption[l.size()]));

					}
					lr2skinheader.add(header);
					// 7/14key skinは5/10keyにも加える
					if(header.getType() == SkinHeader.TYPE_LR2SKIN &&
							(header.getSkinType() == SkinType.PLAY_7KEYS || header.getSkinType() == SkinType.PLAY_14KEYS)) {
						header = loader.loadSkin(path, null);
						List<CustomOption> l = new ArrayList(Arrays.asList(header.getCustomOptions()));
						l.add(new CustomOption("BGA Size", new int[]{30,31}, new String[]{"Normal", "Extend"}));
						l.add(new CustomOption("Ghost", new int[]{34,35,36,37}, new String[]{"Off", "Type A", "Type B", "Type C"}));
						l.add(new CustomOption("Score Graph", new int[]{38,39}, new String[]{"Off", "On"}));
						l.add(new CustomOption("Judge Detail", new int[]{1997,1998,1999}, new String[]{"Off", "EARLY/LATE", "+-ms"}));
						header.setCustomOptions(l.toArray(new CustomOption[l.size()]));
						if(header.getSkinType() == SkinType.PLAY_7KEYS && !header.getName().toLowerCase().contains("7key")) {
							header.setName(header.getName() + " (7KEYS) ");
						} else if(header.getSkinType() == SkinType.PLAY_14KEYS && !header.getName().toLowerCase().contains("14key")) {
							header.setName(header.getName() + " (14KEYS) ");
						}
						header.setSkinType(header.getSkinType() == SkinType.PLAY_7KEYS ? SkinType.PLAY_5KEYS : SkinType.PLAY_10KEYS);
						lr2skinheader.add(header);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public VBox create(SkinHeader header, Map<String, Object> property) {
		selected = header;
		if (header == null) {
			return null;
		}
		VBox main = new VBox();
		optionbox.clear();
		for (CustomOption option : header.getCustomOptions()) {
			HBox hbox = new HBox();
			ComboBox<String> combo = new ComboBox<String>();
			combo.getItems().setAll(option.contents);
			combo.getSelectionModel().select(0);
			if (property != null && property.get(option.name) != null) {
				int i = (int) property.get(option.name);
				for(int index = 0;index < option.option.length;index++) {
					if(option.option[index] == i) {
						combo.getSelectionModel().select(index);
						break;
					}
				}
			}
			Label label = new Label(option.name);
			label.setMinWidth(250.0);
			hbox.getChildren().addAll(label, combo);
			optionbox.put(option, combo);
			main.getChildren().add(hbox);
		}
		filebox.clear();
		for (CustomFile file : header.getCustomFiles()) {
			String name = file.path.substring(file.path.lastIndexOf('/') + 1);
			final Path dirpath = Paths.get(file.path.substring(0, file.path.lastIndexOf('/')));
			if (!Files.exists(dirpath)) {
				continue;
			}
			try (DirectoryStream<Path> paths = Files.newDirectoryStream(dirpath,
					"{" + name.toLowerCase() + "," + name.toUpperCase() + "}")) {
				HBox hbox = new HBox();
				ComboBox<String> combo = new ComboBox<String>();
				for (Path p : paths) {
					combo.getItems().add(p.getFileName().toString());
				}
				String selection = property != null ? (String) property.get(file.name) : null;
				if (selection == null && file.def != null) {
					// デフォルト値のファイル名またはそれに拡張子を付けたものが存在すれば使用する
					for (String item : combo.getItems()) {
						if (item.equalsIgnoreCase(file.def)) {
							selection = item;
							break;
						}
						int point = item.lastIndexOf('.');
						if (point != -1 && item.substring(0, point).equalsIgnoreCase(file.def)) {
							selection = item;
							break;
						}
					}
				}
				if (selection != null) {
					combo.setValue(selection);
				} else {
					combo.getSelectionModel().select(0);
				}

				Label label = new Label(file.name);
				label.setMinWidth(250.0);
				hbox.getChildren().addAll(label, combo);
				filebox.put(file, combo);
				main.getChildren().add(hbox);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		offsetbox.clear();
		for (CustomOffset option : header.getCustomOffsets()) {
			final String[] values = {"x","y","w","h","r"};
			HBox hbox = new HBox();
			Label label = new Label(option.name);
			label.setMinWidth(250.0);
			hbox.getChildren().add(label);
			
			Spinner<Integer>[] spinner = new Spinner[values.length];
			for(int i = 0;i < spinner.length;i++) {
				spinner[i] = new Spinner(-9999,9999,0,1);
				hbox.getChildren().addAll(new Label(values[i]), spinner[i]);
			}
			offsetbox.put(option, spinner);
			main.getChildren().add(hbox);
		}

		return main;
	}

	private void scan(Path p, final List<Path> paths) {
		if (Files.isDirectory(p)) {
			try (Stream<Path> sub = Files.list(p)) {
				sub.forEach(new Consumer<Path>() {
					@Override
					public void accept(Path t) {
						scan(t, paths);
					}
				});
			} catch (IOException e) {
			}
		} else if (p.getFileName().toString().toLowerCase().endsWith(".lr2skin")
				|| p.getFileName().toString().toLowerCase().endsWith(".json")) {
			paths.add(p);
		}
	}

	public SkinHeader getSelectedHeader() {
		return selected;
	}

	public Map<String, Object> getProperty() {
		Map<String, Object> result = new HashMap<String, Object>();
		for (CustomOption option : selected.getCustomOptions()) {
			if (optionbox.get(option) != null) {
				int index = optionbox.get(option).getSelectionModel().getSelectedIndex();
				result.put(option.name, option.option[index]);
			}
		}
		for (CustomFile file : selected.getCustomFiles()) {
			if (filebox.get(file) != null) {
				result.put(file.name, filebox.get(file).getValue());
			}
		}
		for (CustomOffset offset : selected.getCustomOffsets()) {
			if (offsetbox.get(offset) != null) {
				Spinner<Integer>[] spinner = offsetbox.get(offset);
				int[] values = new int[spinner.length];
				for(int i = 0;i < values.length;i++) {
					spinner[i].getValueFactory()
					.setValue(spinner[i].getValueFactory().getConverter().fromString(spinner[i].getEditor().getText()));
					values[i] = spinner[i].getValue();
				}
				result.put(offset.name, values);
			}
		}
		return result;
	}

	public SkinHeader[] getSkinHeader(SkinType mode) {
		List<SkinHeader> result = new ArrayList<SkinHeader>();
		for (SkinHeader header : lr2skinheader) {
			if (header.getSkinType() == mode) {
				result.add(header);
			}
		}
		return result.toArray(new SkinHeader[result.size()]);
	}
}