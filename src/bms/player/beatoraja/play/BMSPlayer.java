package bms.player.beatoraja.play;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.Config.SkinConfig;
import bms.player.beatoraja.gauge.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.pattern.*;
import bms.player.beatoraja.play.audio.AudioProcessor;
import bms.player.beatoraja.play.audio.SoundProcessor;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.result.MusicResultSkin;
import bms.player.beatoraja.skin.LR2PlaySkinLoader;
import bms.player.beatoraja.skin.LR2ResultSkinLoader;
import bms.player.beatoraja.skin.LR2SkinHeader;
import bms.player.beatoraja.skin.LR2SkinHeaderLoader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * BMSプレイヤー本体
 * 
 * @author exch
 */
public class BMSPlayer extends MainState {

	// TODO GLAssistから起動すると楽曲ロード中に止まる

	public static final int TIMER_BOMB_1P_SCRATCH = 50;
	public static final int TIMER_BOMB_1P_KEY1 = 51;
	public static final int TIMER_BOMB_1P_KEY2 = 52;
	public static final int TIMER_BOMB_1P_KEY3 = 53;
	public static final int TIMER_BOMB_1P_KEY4 = 54;
	public static final int TIMER_BOMB_1P_KEY5 = 55;
	public static final int TIMER_BOMB_1P_KEY6 = 56;
	public static final int TIMER_BOMB_1P_KEY7 = 57;
	public static final int TIMER_BOMB_1P_KEY8 = 58;
	public static final int TIMER_BOMB_1P_KEY9 = 59;
	public static final int TIMER_BOMB_2P_SCRATCH = 60;
	public static final int TIMER_BOMB_2P_KEY1 = 61;
	public static final int TIMER_BOMB_2P_KEY2 = 62;
	public static final int TIMER_BOMB_2P_KEY3 = 63;
	public static final int TIMER_BOMB_2P_KEY4 = 64;
	public static final int TIMER_BOMB_2P_KEY5 = 65;
	public static final int TIMER_BOMB_2P_KEY6 = 66;
	public static final int TIMER_BOMB_2P_KEY7 = 67;
	public static final int TIMER_BOMB_2P_KEY8 = 68;
	public static final int TIMER_BOMB_2P_KEY9 = 69;
	public static final int TIMER_HOLD = 70;
	public static final int TIMER_HOLD_1P_KEY1 = 71;
	public static final int TIMER_KEYON_1P_SCRATCH = 100;
	public static final int TIMER_KEYON_1P_KEY1 = 101;
	public static final int TIMER_KEYON_1P_KEY2 = 102;
	public static final int TIMER_KEYON_1P_KEY3 = 103;
	public static final int TIMER_KEYON_1P_KEY4 = 104;
	public static final int TIMER_KEYON_1P_KEY5 = 105;
	public static final int TIMER_KEYON_1P_KEY6 = 106;
	public static final int TIMER_KEYON_1P_KEY7 = 107;
	public static final int TIMER_KEYON_1P_KEY8 = 108;
	public static final int TIMER_KEYON_1P_KEY9 = 109;
	public static final int TIMER_KEYON_2P_SCRATCH = 110;
	public static final int TIMER_KEYON_2P_KEY1 = 111;
	public static final int TIMER_KEYON_2P_KEY2 = 112;
	public static final int TIMER_KEYON_2P_KEY3 = 113;
	public static final int TIMER_KEYON_2P_KEY4 = 114;
	public static final int TIMER_KEYON_2P_KEY5 = 115;
	public static final int TIMER_KEYON_2P_KEY6 = 116;
	public static final int TIMER_KEYON_2P_KEY7 = 117;
	public static final int TIMER_KEYON_2P_KEY8 = 118;
	public static final int TIMER_KEYON_2P_KEY9 = 119;
	public static final int TIMER_KEYOFF_1P_SCRATCH = 120;
	public static final int TIMER_KEYOFF_1P_KEY1 = 121;
	public static final int TIMER_KEYOFF_1P_KEY2 = 122;
	public static final int TIMER_KEYOFF_1P_KEY3 = 123;
	public static final int TIMER_KEYOFF_1P_KEY4 = 124;
	public static final int TIMER_KEYOFF_1P_KEY5 = 125;
	public static final int TIMER_KEYOFF_1P_KEY6 = 126;
	public static final int TIMER_KEYOFF_1P_KEY7 = 127;
	public static final int TIMER_KEYOFF_1P_KEY8 = 128;
	public static final int TIMER_KEYOFF_1P_KEY9 = 129;
	public static final int TIMER_KEYOFF_2P_SCRATCH = 130;
	public static final int TIMER_KEYOFF_2P_KEY1 = 131;
	public static final int TIMER_KEYOFF_2P_KEY2 = 132;
	public static final int TIMER_KEYOFF_2P_KEY3 = 133;
	public static final int TIMER_KEYOFF_2P_KEY4 = 134;
	public static final int TIMER_KEYOFF_2P_KEY5 = 135;
	public static final int TIMER_KEYOFF_2P_KEY6 = 136;
	public static final int TIMER_KEYOFF_2P_KEY7 = 137;
	public static final int TIMER_KEYOFF_2P_KEY8 = 138;
	public static final int TIMER_KEYOFF_2P_KEY9 = 139;
	public static final int TIMER_RHYTHM = 140;

	public static final int VALUE_JUDGE_1P_SCRATCH = 500;
	public static final int VALUE_JUDGE_1P_KEY1 = 501;

	public static final int SLIDER_LANECOVER = 4;
	public static final int SLIDER_LANECOVER2 = 5;

	public static final int OPTION_LANECOVER1_CHANGING = 270;

	private BitmapFont judgefont;
	private BitmapFont systemfont;
	private BMSModel model;

	private BMSPlayerInputProcessor input;
	private LaneRenderer lanerender;
	private JudgeManager judge;
	private AudioProcessor audio;

	private BGAProcessor bga;

	private PlaySkin skin;

	private Sound playstop;

	private GrooveGauge gauge;

	private int playtime;

	private int autoplay = 0;
	/**
	 * BGレーン再生用スレッド
	 */
	private AutoplayThread autoThread;
	/**
	 * キー入力用スレッド
	 */
	private KeyInputThread keyinput;

	private int assist = 0;

	private List<PatternModifyLog> pattern = new ArrayList<PatternModifyLog>();

	private ReplayData replay = null;

	private List<Float> gaugelog = new ArrayList<Float>();

	private int playspeed = 100;

	/**
	 * 処理済ノート数
	 */
	private int notes;

	public BMSPlayer(MainController main, PlayerResource resource) {
		super(main);
		this.model = resource.getBMSModel();
		this.autoplay = resource.getAutoplay();
		Config config = resource.getConfig();

		if (autoplay >= 2) {
			if (resource.getCourseBMSModels() != null) {
				if (resource.getCourseReplay().length == 0) {
					ReplayData[] replays = main.getPlayDataAccessor().readReplayData(resource.getCourseBMSModels(),
							config.getLnmode(), autoplay - 2, resource.getConstraint());
					if (replays != null) {
						for (ReplayData rd : replays) {
							resource.addCourseReplay(rd);
						}
						replay = replays[0];
					} else {
						autoplay = 0;
					}
				} else {
					for (int i = 0; i < resource.getCourseBMSModels().length; i++) {
						if (resource.getCourseBMSModels()[i].getHash().equals(resource.getBMSModel().getHash())) {
							replay = resource.getCourseReplay()[i];
						}
					}
				}
			} else {
				replay = main.getPlayDataAccessor().readReplayData(model, config.getLnmode(), autoplay - 2);
				if (replay == null) {
					autoplay = 0;
				}
			}
		}

		if (model.getRandom() > 1) {
			if (autoplay >= 2) {
				model.setSelectedIndexOfTimeLines(replay.random);
			} else if (resource.getReplayData().pattern != null) {
				model.setSelectedIndexOfTimeLines(resource.getReplayData().random);
			} else {
				model.setSelectedIndexOfTimeLines((int) (Math.random() * model.getRandom()) + 1);
			}
			Logger.getGlobal().info("譜面分岐 : " + model.getSelectedIndexOfTimeLines());
		}
		// 通常プレイの場合は最後のノーツ、オートプレイの場合はBG/BGAを含めた最後のノーツ
		playtime = (autoplay == 1 ? model.getLastTime() : model.getLastNoteTime()) + 5000;

		boolean score = true;

		model.setLntype(config.getLnmode());
		boolean exjudge = false;
		if (resource.getCourseBMSModels() == null && autoplay < 2) {
			if (config.isBpmguide() && (model.getMinBPM() < model.getMaxBPM())) {
				// BPM変化がなければBPMガイドなし
				assist = 1;
				score = false;
			}

			if (config.isConstant() && (model.getMinBPM() < model.getMaxBPM())) {
				// BPM変化がなければコンスタントなし
				new ConstantBPMModifier().modify(model);
				assist = 1;
				score = false;
			}

			if (config.isLegacynote()) {
				new LongNoteModifier().modify(model);
				assist = 2;
				score = false;
			}
			if (config.isExpandjudge()) {
				exjudge = true;
				assist = 2;
				score = false;
			}
			if (config.isNomine()) {
				// 地雷ノートがなければアシストなし
				MineNoteModifier mod = new MineNoteModifier();
				mod.modify(model);
				if (mod.mineNoteExists()) {
					assist = 2;
					score = false;
				}
			}
			if (config.getDoubleoption() == 2 && (model.getUseKeys() == 5 || model.getUseKeys() == 7)) {
				// 地雷ノートがなければアシストなし
				LaneShuffleModifier mod = new LaneShuffleModifier(LaneShuffleModifier.BATTLE);
				mod.modify(model);
				model.setUseKeys(model.getUseKeys() * 2);
				assist = 1;
				score = false;
			}
		}

		judge = new JudgeManager(this, model, resource.getConstraint());
		if (exjudge) {
			judge.setExpandJudge(JudgeManager.EXPAND_JUDGE);
		}

		Logger.getGlobal().info("アシストオプション設定完了");
		if (replay != null) {
			PatternModifier.modify(model, Arrays.asList(replay.pattern));
		} else if (resource.getReplayData().pattern != null) {
			PatternModifier.modify(model, Arrays.asList(resource.getReplayData().pattern));
			Logger.getGlobal().info("譜面オプション : 保存された譜面変更ログから譜面再現");
		} else {
			switch (model.getUseKeys()) {
			case 10:
			case 14:
				if (config.getDoubleoption() == 1) {
					pattern = PatternModifier.merge(pattern,
							new LaneShuffleModifier(LaneShuffleModifier.FLIP).modify(model));
				}
				if (config.getRandom2() > 0) {
					random[config.getRandom2()].setModifyTarget(PatternModifier.PLAYER2);
					pattern = PatternModifier.merge(pattern, random[config.getRandom2()].modify(model));
				}
				if (config.getRandom2() >= 6) {
					assist = (assist == 0) ? 1 : assist;
					score = false;
				}
				Logger.getGlobal().info("譜面オプション :  " + config.getRandom2());
			case 5:
			case 7:
				if (config.getRandom() > 0) {
					random[config.getRandom()].setModifyTarget(PatternModifier.PLAYER1);
					pattern = PatternModifier.merge(pattern, random[config.getRandom()].modify(model));
				}
				if (config.getRandom() >= 6) {
					assist = (assist == 0) ? 1 : assist;
					score = false;
				}
				Logger.getGlobal().info("譜面オプション :  " + config.getRandom());
				break;
			case 9:
				if (config.getRandom() == 7) {
					config.setRandom(0);
				}
				if (config.getRandom() == 8) {
					config.setRandom(3);
				}
				if (config.getRandom() == 9) {
					config.setRandom(4);
				}
				if (config.getRandom() > 0) {
					random[config.getRandom()].setModifyTarget(PatternModifier.NINEKEYS);
					pattern = random[config.getRandom()].modify(model);
				}
				if (config.getRandom() >= 6) {
					assist = (assist == 0) ? 1 : assist;
					score = false;
				}
				break;
			}
		}

		int g = config.getGauge();
		if (replay != null) {
			g = replay.gauge;
		}
		if (resource.getCourseBMSModels() != null) {
			// 段位ゲージ
			switch (g) {
			case 0:
			case 1:
			case 2:
				gauge = new GradeGrooveGauge(model);
				break;
			case 3:
				gauge = new ExgradeGrooveGauge(model);
				break;
			case 4:
			case 5:
				gauge = new ExhardGradeGrooveGauge(model);
				break;
			}
		} else {
			switch (g) {
			case 0:
				gauge = new AssistEasyGrooveGauge(model);
				break;
			case 1:
				gauge = new EasyGrooveGauge(model);
				break;
			case 2:
				gauge = new NormalGrooveGauge(model);
				break;
			case 3:
				gauge = new HardGrooveGauge(model);
				break;
			case 4:
				gauge = new ExhardGrooveGauge(model);
				break;
			case 5:
				gauge = new HazardGrooveGauge(model);
				break;
			}
		}
		resource.setUpdateScore(score);
		final int difficulty = resource.getSongdata() != null ? resource.getSongdata().getDifficulty() : 0;
		resource.setSongdata(new SongData(model, false));
		resource.getSongdata().setDifficulty(difficulty);

		List<Float> f = resource.getGauge();
		if (f != null) {
			gauge.setValue(f.get(f.size() - 1));
			judge.setCourseCombo(resource.getCombo());
			judge.setCourseMaxcombo(resource.getMaxcombo());
		}
		Logger.getGlobal().info("ゲージ設定完了");

		int skinmode = (model.getUseKeys() == 7 ? 0 : (model.getUseKeys() == 5 ? 1 : (model.getUseKeys() == 14 ? 2 : (model.getUseKeys() == 10 ? 3 : 4))));
		if (resource.getConfig().getSkin()[skinmode] != null) {
			try {
				SkinConfig sc = resource.getConfig().getSkin()[skinmode];
				LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
				LR2SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), this, sc.getProperty());
				LR2PlaySkinLoader dloader = new LR2PlaySkinLoader();
				skin = dloader.loadPlaySkin(new File(header.getInclude()), this, header,
						loader.getOption(), sc.getProperty());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				skin = new PlaySkin(model.getUseKeys(), config.isUse2pside(), MainController.RESOLUTION[resource
						.getConfig().getResolution()]);
			}
		} else {
			skin = new PlaySkin(model.getUseKeys(), config.isUse2pside(), MainController.RESOLUTION[resource
			                                                                    					.getConfig().getResolution()]);
		}
		this.setSkin(skin);
	}

	private final PatternModifier[] random = { null, new LaneShuffleModifier(LaneShuffleModifier.MIRROR),
			new LaneShuffleModifier(LaneShuffleModifier.RANDOM), new LaneShuffleModifier(LaneShuffleModifier.R_RANDOM),
			new NoteShuffleModifier(NoteShuffleModifier.S_RANDOM), new NoteShuffleModifier(NoteShuffleModifier.SPIRAL),
			new NoteShuffleModifier(NoteShuffleModifier.H_RANDOM),
			new NoteShuffleModifier(NoteShuffleModifier.ALL_SCR),
			new LaneShuffleModifier(LaneShuffleModifier.RANDOM_EX),
			new NoteShuffleModifier(NoteShuffleModifier.S_RANDOM_EX) };

	public void create() {
		final MainController main = getMainController();
		final PlayerResource resource = main.getPlayerResource();
		final ShapeRenderer shape = main.getShapeRenderer();
		final SpriteBatch sprite = main.getSpriteBatch();

		if (resource.getConfig().getSoundpath().length() > 0) {
			final File soundfolder = new File(resource.getConfig().getSoundpath());
			if (soundfolder.exists() && soundfolder.isDirectory()) {
				for (File f : soundfolder.listFiles()) {
					if (playstop == null && f.getName().startsWith("playstop.")) {
						playstop = SoundProcessor.getSound(f.getPath());
					}
				}
			}
		}

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 18;
		systemfont = generator.generateFont(parameter);
		judgefont = generator.generateFont(parameter);
		generator.dispose();

		Config config = resource.getConfig();
		Logger.getGlobal().info("create");

		input = main.getInputProcessor();
		input.setMinimumInputDutration(config.getInputduration());
		input.setEnableKeyInput(autoplay == 0);
		PlayConfig pc = (model.getUseKeys() == 5 || model.getUseKeys() == 7 ? config.getMode7()
				: (model.getUseKeys() == 10 || model.getUseKeys() == 14 ? config.getMode14() : config.getMode9()));
		input.setKeyassign(pc.getKeyassign());
		input.setControllerassign(pc.getControllerassign());
		lanerender = new LaneRenderer(this, sprite, shape, systemfont, skin, resource, model, resource.getConstraint());
		skin.setBMSPlayer(this);
		bga = resource.getBGAManager();

		Logger.getGlobal().info("描画クラス準備");

		Logger.getGlobal().info("hash");
		IRScoreData score = main.getPlayDataAccessor().readScoreData(model, config.getLnmode());
		Logger.getGlobal().info("スコアデータベースからスコア取得");
		if (score == null) {
			score = new IRScoreData();
		}
		bestscore = score.getExscore();
		rivalscore = model.getTotalNotes() * 8 / 5;
		resource.setRivalScoreData(rivalscore);
		Logger.getGlobal().info("スコアグラフ描画クラス準備");

		audio = resource.getAudioProcessor();
	}

	private int bestscore;
	private int rivalscore;

	@Override
	public void resize(int w, int h) {
		System.out.println("resize" + w + "," + h);
	}

	protected static final int STATE_PRELOAD = 0;
	protected static final int STATE_READY = 1;
	protected static final int STATE_PLAY = 2;
	protected static final int STATE_FAILED = 3;
	protected static final int STATE_FINISHED = 4;

	private int state = STATE_PRELOAD;

	private long prevtime;

	@Override
	public void render() {
		final MainController main = getMainController();
		final PlayerResource resource = main.getPlayerResource();

		final long now = getNowTime();
		final long nowtime = System.currentTimeMillis();
		switch (state) {
		// 楽曲ロード
		case STATE_PRELOAD:
			if (resource.mediaLoadFinished() && !input.startPressed()) {
				bga.prepare(this);
				state = STATE_READY;
				getTimer()[TIMER_READY] = now;
				Logger.getGlobal().info("STATE_READYに移行");
			}
			break;
		// GET READY
		case STATE_READY:
			final long rt = now - getTimer()[TIMER_READY];
			if (rt > skin.getPlayStartTime()) {
				state = STATE_PLAY;
                getTimer()[TIMER_PLAY] = now;
				getTimer()[TIMER_RHYTHM] = now;

						input.setStartTime(now + getStartTime());
				List<KeyInputLog> keylog = null;
				if (autoplay == 1) {
					keylog = createAutoplayLog(model);
				} else if (autoplay >= 2) {
					keylog = Arrays.asList(replay.keylog);
				}
				autoThread = new AutoplayThread();
				autoThread.start();
				keyinput = new KeyInputThread(keylog);
				keyinput.start();
				Logger.getGlobal().info("STATE_PLAYに移行");
			}
			break;
		// プレイ
		case STATE_PLAY:
            getTimer()[TIMER_PLAY] += (nowtime - prevtime) * (100 - playspeed) / 100;
            getTimer()[TIMER_RHYTHM] += (nowtime - prevtime) * (100 - lanerender.getNowBPM() * 100 / 60) / 100;
			final float g = gauge.getValue();
			if (gaugelog.size() <= (now - getTimer()[TIMER_PLAY]) / 500) {
				gaugelog.add(g);
			}
			if(g == gauge.getMaxValue() && getTimer()[TIMER_GAUGE_MAX_1P] == Long.MIN_VALUE) {
				getTimer()[TIMER_GAUGE_MAX_1P] = now;
			} else if(g < gauge.getMaxValue() && getTimer()[TIMER_GAUGE_MAX_1P] != Long.MIN_VALUE){
				getTimer()[TIMER_GAUGE_MAX_1P] = Long.MIN_VALUE;
			}
			// System.out.println("playing time : " + time);
			if (playtime < (now - getTimer()[TIMER_PLAY])) {
				state = STATE_FINISHED;
				getTimer()[TIMER_FADEOUT] = now;
				Logger.getGlobal().info("STATE_FINISHEDに移行");
			}
			if (g == 0) {
				state = STATE_FAILED;
				getTimer()[TIMER_FAILED] = now;
				if (playstop != null) {
					playstop.play();
				}
				Logger.getGlobal().info("STATE_FAILEDに移行");
			}

			break;
		// 閉店処理
		case STATE_FAILED:
			if (autoThread != null) {
				autoThread.stop = true;
			}
			if (keyinput != null) {
				keyinput.stop = true;
			}
			if (resource.mediaLoadFinished()) {
				resource.getAudioProcessor().stop(-1, 0, 0);
			}

			if (now - getTimer()[TIMER_FAILED] > skin.getCloseTime()) {
				if (resource.mediaLoadFinished()) {
					resource.getBGAManager().stop();
				}
				if (keyinput != null) {
					Logger.getGlobal().info("入力パフォーマンス(max ms) : " + keyinput.frametimes);
				}
				if (autoplay != 1) {
					resource.setScoreData(createScoreData());
				}
				resource.setCombo(judge.getCourseCombo());
				resource.setMaxcombo(judge.getCourseMaxcombo());
				saveConfig();
				gaugelog.add(0f);
				resource.setGauge(gaugelog);
				resource.setGrooveGauge(gauge);
				input.setEnableKeyInput(true);
				input.setStartTime(0);
				if (resource.getScoreData() != null) {
					main.changeState(MainController.STATE_RESULT);
				} else {
					main.changeState(MainController.STATE_SELECTMUSIC);
				}
			}
			break;
		// 完奏処理
		case STATE_FINISHED:
			if (autoThread != null) {
				autoThread.stop = true;
			}
			if (keyinput != null) {
				keyinput.stop = true;
			}
			long l2 = now - getTimer()[TIMER_FADEOUT];
			if (l2 > skin.getFadeoutTime()) {
				resource.getAudioProcessor().stop(-1, 0, 0);
				resource.getBGAManager().stop();
				if (keyinput != null) {
					Logger.getGlobal().info("入力パフォーマンス(max ms) : " + keyinput.frametimes);
				}
				if (autoplay != 1) {
					resource.setScoreData(createScoreData());
				}
				resource.setCombo(judge.getCourseCombo());
				resource.setMaxcombo(judge.getCourseMaxcombo());
				saveConfig();
				resource.setGauge(gaugelog);
				resource.setGrooveGauge(gauge);
				input.setEnableKeyInput(true);
				input.setStartTime(0);
				if (resource.getScoreData() != null) {
					main.changeState(MainController.STATE_RESULT);
				} else {
					if (resource.getCourseBMSModels() != null && resource.nextCourse()) {
						main.changeState(MainController.STATE_PLAYBMS);
					} else {
						main.changeState(MainController.STATE_SELECTMUSIC);
					}
				}
			}
			break;
		}
		if (input.isExitPressed()) {
			stopPlay();
		}
		// play speed change (autoplay or replay only)
		if(autoplay != 0) {
			if (input.getNumberState()[1]) {
				playspeed = 25;
			} else if (input.getNumberState()[2]) {
				playspeed = 50;
			} else if (input.getNumberState()[3]) {
				playspeed = 200;
			} else if (input.getNumberState()[4]) {
				playspeed = 300;
			} else {
				playspeed = 100;
			}
		}

		prevtime = nowtime;
	}

	public int getState() {
		return state;
	}

	public LaneRenderer getLanerender() {
		return lanerender;
	}

	private void saveConfig() {
		final PlayerResource resource = getMainController().getPlayerResource();
		for (int c : resource.getConstraint()) {
			if (c == TableData.NO_HISPEED) {
				return;
			}
		}
		PlayConfig pc = (model.getUseKeys() == 5 || model.getUseKeys() == 7 ? resource.getConfig().getMode7()
				: (model.getUseKeys() == 10 || model.getUseKeys() == 14 ? resource.getConfig().getMode14() : resource.getConfig().getMode9()));
		if (lanerender.getFixHispeed() != Config.FIX_HISPEED_OFF) {
			pc.setDuration(lanerender.getGreenValue());
		} else {
			pc.setHispeed(lanerender.getHispeed());
		}
		pc.setLanecover(lanerender.getLanecover());
		pc.setLift(lanerender.getLiftRegion());
	}

	public IRScoreData createScoreData() {
		final PlayerResource resource = getMainController().getPlayerResource();
		final int pgreat = judge.getJudgeCount(0);
		final int great = judge.getJudgeCount(1);
		final int good = judge.getJudgeCount(2);
		final int bad = judge.getJudgeCount(3);
		final int poor = judge.getJudgeCount(4);
		final int miss = judge.getJudgeCount(5);
		if (pgreat + great + good + bad == 0) {
			return null;
		}

		IRScoreData score = new IRScoreData();
		score.setSha256(model.getSHA256());
		score.setNotes(model.getTotalNotes());
		score.setCombo(judge.getMaxcombo());
		int clear = GrooveGauge.CLEARTYPE_FAILED;
		if (state != STATE_FAILED && gauge.isQualified()) {
			if (assist > 0) {
				clear = assist == 1 ? GrooveGauge.CLEARTYPE_LIGHT_ASSTST : GrooveGauge.CLEARTYPE_ASSTST;
			} else {
				if (judge.getJudgeCount(3) + judge.getJudgeCount(4) == 0
						&& (model.getUseKeys() != 9 || judge.getJudgeCount(5) == 0)) {
					if (judge.getJudgeCount(2) == 0) {
						if (judge.getJudgeCount(1) == 0) {
							clear = GrooveGauge.CLEARTYPE_MAX;
						} else {
							clear = GrooveGauge.CLEARTYPE_PERFECT;
						}
					} else {
						clear = GrooveGauge.CLEARTYPE_FULLCOMBO;
					}
				} else if (resource.getCourseBMSModels() == null) {
					clear = gauge.getClearType();
				}
			}
		}
		score.setClear(clear);
		score.setOption(resource.getConfig().getRandom()
				+ (model.getUseKeys() == 10 || model.getUseKeys() == 14 ? (resource.getConfig().getRandom2() * 10 + resource
						.getConfig().getDoubleoption() * 100) : 0));
		// リプレイデータ保存。スコア保存されない場合はリプレイ保存しない
		resource.getReplayData().keylog = input.getKeyInputLog().toArray(new KeyInputLog[0]);
		resource.getReplayData().pattern = pattern.toArray(new PatternModifyLog[pattern.size()]);
		resource.getReplayData().random = model.getSelectedIndexOfTimeLines();
		resource.getReplayData().gauge = resource.getConfig().getGauge();

		score.setEpg(judge.getJudgeCount(0, true));
		score.setLpg(judge.getJudgeCount(0, false));
		score.setEgr(judge.getJudgeCount(1, true));
		score.setLgr(judge.getJudgeCount(1, false));
		score.setEgd(judge.getJudgeCount(2, true));
		score.setLgd(judge.getJudgeCount(2, false));
		score.setEbd(judge.getJudgeCount(3, true));
		score.setLbd(judge.getJudgeCount(3, false));
		score.setEpr(judge.getJudgeCount(4, true));
		score.setLpr(judge.getJudgeCount(4, false));
		score.setEms(judge.getJudgeCount(5, true));
		score.setLms(judge.getJudgeCount(5, false));

		score.setMinbp(bad + poor + miss + resource.getSongdata().getNotes() - notes);
		return score;
	}

	public void stopPlay() {
		if (getTimer()[TIMER_FAILED] != Long.MIN_VALUE || getTimer()[TIMER_FADEOUT] != Long.MIN_VALUE) {
			return;
		}
		if (notes == getMainController().getPlayerResource().getSongdata().getNotes()) {
			state = STATE_FINISHED;
			getTimer()[TIMER_FADEOUT] = getNowTime();
			Logger.getGlobal().info("STATE_FINISHEDに移行");
		} else {
			state = STATE_FAILED;
			getTimer()[TIMER_FAILED] = getNowTime();
			if (playstop != null) {
				playstop.play();
			}
			Logger.getGlobal().info("STATE_FAILEDに移行");
		}
	}

	@Override
	public void pause() {
		System.out.println("pause");
	}

	@Override
	public void resume() {
		System.out.println("resume");
	}

	@Override
	public void dispose() {
		if (judgefont != null) {
			judgefont.dispose();
			judgefont = null;
		}
		if (systemfont != null) {
			systemfont.dispose();
			systemfont = null;
		}
		if (playstop != null) {
			playstop.dispose();
			playstop = null;
		}
		if (skin != null) {
			skin.dispose();
			skin = null;
		}
		Logger.getGlobal().info("システム描画のリソース解放");
	}

	public void play(Note note) {
		audio.play(note.getWav(), note.getStarttime(), note.getDuration());
	}

	public BMSPlayerInputProcessor getBMSPlayerInputProcessor() {
		return input;
	}

	public int getJudgeCount(int judge, boolean fast) {
		return this.judge.getJudgeCount(judge, fast);
	}

	public JudgeManager getJudgeManager() {
		return judge;
	}

	public void update(int lane, int judge, int time, int fast) {
		if (judge < 5) {
			notes++;
		}

		if (judge == 3 || judge == 4) {
			bga.setMisslayerTme(time);
		}
		gauge.update(judge);
		// System.out.println(
		// "Now count : " + notes + " - " + totalnotes);
		lanerender.update(lane, judge, time, fast);

		rate = (this.judge.getJudgeCount(0) * 2 + this.judge.getJudgeCount(1)) * 10000
				/ getMainController().getPlayerResource().getSongdata().getNotes() / 2;
        drate = notes == 0 ? 10000 : (this.judge.getJudgeCount(0) * 2 + this.judge.getJudgeCount(1)) * 10000
                / notes / 2;

		if (notes == getMainController().getPlayerResource().getSongdata().getNotes()
				&& getTimer()[TIMER_ENDOFNOTE_1P] == Long.MIN_VALUE) {
			getTimer()[TIMER_ENDOFNOTE_1P] = getNowTime();
			if (this.judge.getJudgeCount(3) == 0 && this.judge.getJudgeCount(4) == 0) {
				getTimer()[TIMER_FULLCOMBO_1P] = getNowTime();
			}
		}
	}

	public GrooveGauge getGauge() {
		return gauge;
	}

	/**
	 * AUTOPLAY用のKeyInputLogを生成する
	 * 
	 * @return AUTOPLAY用のKeyInputLog
	 */
	public static final List<KeyInputLog> createAutoplayLog(BMSModel model) {
		// TODO 地雷を確実に回避するアルゴリズム
		List<KeyInputLog> keylog = new ArrayList<KeyInputLog>();
		int keys = (model.getUseKeys() == 5 || model.getUseKeys() == 7) ? 9 : ((model.getUseKeys() == 10 || model
				.getUseKeys() == 14) ? 18 : 9);
		boolean sc = (model.getUseKeys() == 5 || model.getUseKeys() == 7 || model.getUseKeys() == 10 || model
				.getUseKeys() == 14);
		Note[] ln = new Note[keys];
		for (TimeLine tl : model.getAllTimeLines()) {
			int i = tl.getTime();
			for (int lane = 0; lane < keys; lane++) {
				if (!sc || (lane != 8 && lane != 17)) {
					Note note = tl.getNote(model.getUseKeys() == 9 && lane >= 5 ? lane + 5 : lane);
					if (note != null) {
						if (note instanceof LongNote) {
							if (((LongNote) note).getEnd() == tl) {
								keylog.add(new KeyInputLog(i, lane, false));
								if (model.getLntype() != 0 && sc && (lane == 7 || lane == 16)) {
									// BSS処理
									keylog.add(new KeyInputLog(i, lane + 1, true));
								}
								ln[lane] = null;
							} else {
								keylog.add(new KeyInputLog(i, lane, true));
								ln[lane] = note;
							}
						} else if (note instanceof NormalNote) {
							keylog.add(new KeyInputLog(i, lane, true));
						}
					} else {
						if (ln[lane] == null) {
							keylog.add(new KeyInputLog(i, lane, false));
							if (sc && (lane == 7 || lane == 16)) {
								keylog.add(new KeyInputLog(i, lane + 1, false));
							}
						}
					}
				}
			}
		}
		return keylog;
	}

	/**
	 * キー入力処理用スレッド
	 * 
	 * @author exch
	 */
	class KeyInputThread extends Thread {
		private boolean stop = false;
		private long frametimes = 1;
		private List<KeyInputLog> keylog;

		public KeyInputThread(List<KeyInputLog> keylog) {
			this.keylog = keylog;
		}

		@Override
		public void run() {
			int index = 0;

			int time = 0;
			long framet = 1;
			final TimeLine[] timelines = model.getAllTimeLines();
			final KeyInputLog[] keylog = this.keylog != null ? this.keylog.toArray(new KeyInputLog[0]) : null;

			final int lasttime = timelines[timelines.length - 1].getTime() + 5000;
			while (time < lasttime && !stop) {
				time = (int) (getNowTime() - getTimer()[TIMER_PLAY]);
				// リプレイデータ再生
				if (keylog != null) {
					while (index < keylog.length && keylog[index].time <= time) {
						final KeyInputLog key = keylog[index];
						// if(input.getKeystate()[key.keycode] == key.pressed) {
						// System.out.println("押し離しが行われていません : key - " +
						// key.keycode + " pressed - " + key.pressed +
						// " time - " + key.time);
						// }
						input.getKeystate()[key.keycode] = key.pressed;
						input.getTime()[key.keycode] = key.time;
						index++;
					}
				}
				judge.update(time);

				final long nowtime = (int) (getNowTime() - getTimer()[TIMER_PLAY]) - time;
				framet = nowtime < framet ? framet : nowtime;
			}
			frametimes = framet;
		}

	}

	/**
	 * BGレーン再生用スレッド
	 * 
	 * @author exch
	 */
	class AutoplayThread extends Thread {

		private boolean stop = false;

		@Override
		public void run() {
			int time = 0;
			final TimeLine[] timelines = model.getAllTimeLines();
			final int lasttime = timelines[timelines.length - 1].getTime() + 5000;
			for (int p = 0; time < lasttime && !stop;) {
				time = (int) (getNowTime() - getTimer()[TIMER_PLAY]);
				// BGレーン再生
				while (p < timelines.length && timelines[p].getTime() <= time) {
					for (Note n : timelines[p].getBackGroundNotes()) {
						play(n);
					}
					p++;
				}
				if (p < timelines.length) {
					try {
						final long sleeptime = timelines[p].getTime() - (getNowTime() - getTimer()[TIMER_PLAY]);
						if (sleeptime > 0) {
							sleep(sleeptime);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		}
	}

	public int getCombo() {
		return judge.getCombo();
	}

	public int getNumberValue(int id) {
		final SongData song = getMainController().getPlayerResource().getSongdata();
		switch (id) {
			case NUMBER_LANECOVER1:
				return (int) (lanerender.getLanecover() * 1000);
		case NUMBER_TIMELEFT_MINUTE:
			return (int) ((playtime - (int) (getTimer()[TIMER_PLAY] != Long.MIN_VALUE ? getNowTime() - getTimer()[TIMER_PLAY] : 0) + 1000) / 60000);
		case NUMBER_TIMELEFT_SECOND:
			return ((playtime - (int) (getTimer()[TIMER_PLAY] != Long.MIN_VALUE ? getNowTime() - getTimer()[TIMER_PLAY] : 0) + 1000) / 1000) % 60;
		case NUMBER_LOADING_PROGRESS:
			return (int) ((audio.getProgress() + bga.getProgress()) * 50);
		case NUMBER_GROOVEGAUGE:
			return (int) gauge.getValue();
		case NUMBER_GROOVEGAUGE_AFTERDOT:
			return ((int) (gauge.getValue() * 10)) % 10;
			case NUMBER_HISPEED_LR2:
				return (int) (lanerender.getHispeed() * 100);
		case NUMBER_HISPEED:
			return (int) lanerender.getHispeed();
		case NUMBER_HISPEED_AFTERDOT:
			return (int) (lanerender.getHispeed() * 100) % 100;
		case NUMBER_DURATION:
			return lanerender.getGreenValue();
		case NUMBER_NOWBPM:
			return (int) lanerender.getNowBPM();
		case NUMBER_SCORE:
			case NUMBER_SCORE2:
			return judge.getJudgeCount(0) * 2 + judge.getJudgeCount(1);
		case NUMBER_TARGET_SCORE:
			case NUMBER_TARGET_SCORE2:
			return rivalscore;
			case NUMBER_DIFF_HIGHSCORE:
				return (judge.getJudgeCount(0) * 2 + judge.getJudgeCount(1))- (bestscore * notes / song.getNotes());
			case NUMBER_DIFF_TARGETSCORE:
				return (judge.getJudgeCount(0) * 2 + judge.getJudgeCount(1)) - (rivalscore * notes / song.getNotes());
		case NUMBER_SCORE_RATE:
			return notes == 0 ? 100 : (judge.getJudgeCount(0) * 2 + judge.getJudgeCount(1)) * 100 / (notes * 2);
		case NUMBER_SCORE_RATE_AFTERDOT:
			return notes == 0 ? 0 : ((judge.getJudgeCount(0) * 2 + judge.getJudgeCount(1)) * 1000 / (notes * 2)) % 10;
		case NUMBER_HIGHSCORE:
			return bestscore;
		case NUMBER_MAXCOMBO:
		case NUMBER_MAXCOMBO2:
			return judge.getMaxcombo();
		}
		if (id >= VALUE_JUDGE_1P_SCRATCH && id < VALUE_JUDGE_1P_SCRATCH + 20) {
			return lanerender.getJudge()[id - VALUE_JUDGE_1P_SCRATCH];
		}
		return super.getNumberValue(id);
	}

	@Override
	public float getSliderValue(int id) {
		final SongData song = getMainController().getPlayerResource().getSongdata();
		switch (id) {
		case SLIDER_MUSIC_PROGRESS:
			if (getTimer()[TIMER_PLAY] != Long.MIN_VALUE) {
				return (float) (getNowTime() - getTimer()[TIMER_PLAY]) / playtime;
			}
			return 0;
			case SLIDER_LANECOVER:
			case SLIDER_LANECOVER2:
				if(lanerender.isEnableLanecover()) {
					float lane = lanerender.getLanecover();
					if(lanerender.isEnableLift()) {
						lane = lane * (1 - lanerender.getLiftRegion());
					}
					return lane;
				}
				return 0;
		case OFFSET_LIFT:
			if (lanerender.isEnableLift()) {
				return lanerender.getLiftRegion() * skin.getLaneregion()[0].height;
			}
			return 0;
		case BARGRAPH_LOAD_PROGRESS:
			float value = (audio.getProgress() + bga.getProgress()) / 2;
			if (value >= 1) {
				return -20;
			}
			return value;			
		case BARGRAPH_SCORERATE:
			return (float) (judge.getJudgeCount(0) * 2 + judge.getJudgeCount(1)) / (song.getNotes() * 2);
		case BARGRAPH_SCORERATE_FINAL:
			return (float) (judge.getJudgeCount(0) * 2 + judge.getJudgeCount(1)) / (notes * 2);
		case BARGRAPH_BESTSCORERATE_NOW:
			return (float) (bestscore) * notes / (song.getNotes() * song.getNotes() * 2);
		case BARGRAPH_BESTSCORERATE:
			return (float) (bestscore) / (song.getNotes() * 2);
		case BARGRAPH_TARGETSCORERATE_NOW:
			return (float) (rivalscore) * notes / (song.getNotes() * song.getNotes() * 2);
		case BARGRAPH_TARGETSCORERATE:
			return (float) (rivalscore) / (song.getNotes() * 2);
		}
		return 0;
	}

	private int rate;
    private int drate;

	public boolean getBooleanValue(int id) {
		switch (id) {
			case OPTION_GAUGE_GROOVE:
				return gauge instanceof AssistEasyGrooveGauge || gauge instanceof EasyGrooveGauge || gauge instanceof NormalGrooveGauge;
			case OPTION_GAUGE_HARD:
				return gauge instanceof HardGrooveGauge || gauge instanceof ExhardGrooveGauge || gauge instanceof HazardGrooveGauge
						|| gauge instanceof GradeGrooveGauge || gauge instanceof ExgradeGrooveGauge || gauge instanceof ExhardGradeGrooveGauge;
		case OPTION_F:
			return true;
		case OPTION_E:
			return rate > 2222;
		case OPTION_D:
			return rate > 3333;
		case OPTION_C:
			return rate > 4444;
		case OPTION_B:
			return rate > 5555;
		case OPTION_A:
			return rate > 6666;
		case OPTION_AA:
			return rate > 7777;
		case OPTION_AAA:
			return rate > 8888;
		case OPTION_1P_F:
			return drate <= 2222;
		case OPTION_1P_E:
			return drate > 2222 && drate <= 3333;
		case OPTION_1P_D:
			return drate > 3333 && drate <= 4444;
		case OPTION_1P_C:
			return drate > 4444 && drate <= 5555;
		case OPTION_1P_B:
			return drate > 5555 && drate <= 6666;
		case OPTION_1P_A:
			return drate > 6666 && drate <= 7777;
		case OPTION_1P_AA:
			return drate > 7777 && drate <= 8888;
		case OPTION_1P_AAA:
			return drate > 8888;
		case OPTION_AUTOPLAYON:
			return autoplay == 1;
		case OPTION_AUTOPLAYOFF:
			return autoplay != 1;
		case OPTION_BGAON:
			return getMainController().getPlayerResource().getConfig().getBga() == Config.BGA_ON
					|| (getMainController().getPlayerResource().getConfig().getBga() == Config.BGA_AUTO && (autoplay != 0));
		case OPTION_BGAOFF:
			return getMainController().getPlayerResource().getConfig().getBga() == Config.BGA_OFF
					|| (getMainController().getPlayerResource().getConfig().getBga() == Config.BGA_AUTO && (autoplay == 0));
		case OPTION_NOW_LOADING:
			return state == STATE_PRELOAD;			
		case OPTION_LOADED:
			return state != STATE_PRELOAD;
			case OPTION_LANECOVER1_CHANGING:
				return input.startPressed();
		}
		return super.getBooleanValue(id);
	}

}
