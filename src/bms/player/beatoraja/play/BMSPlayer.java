package bms.player.beatoraja.play;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.Config.SkinConfig;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.pattern.*;
import bms.player.beatoraja.play.PracticeConfiguration.PracticeProperty;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.play.gauge.*;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.song.SongData;

import com.badlogic.gdx.math.Rectangle;

import static bms.player.beatoraja.Resolution.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * BMSプレイヤー本体
 * 
 * @author exch
 */
public class BMSPlayer extends MainState {

	// TODO GLAssistから起動すると楽曲ロード中に止まる

	private BMSModel model;

	private LaneRenderer lanerender;
	private JudgeManager judge;

	private BGAProcessor bga;

	private String playstop;

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

	private final List<Float> gaugelog;

	private int playspeed = 100;

	/**
	 * 処理済ノート数
	 */
	private int notes;

	private int scratch1;
	private int scratch2;

	private static final int TIME_MARGIN = 5000;

	public BMSPlayer(MainController main, PlayerResource resource) {
		super(main);
		this.model = resource.getBMSModel();
		this.autoplay = resource.getAutoplay();
		Config config = resource.getConfig();

		if (autoplay >= 3) {
			if (resource.getCourseBMSModels() != null) {
				if (resource.getCourseReplay().length == 0) {
					ReplayData[] replays = main.getPlayDataAccessor().readReplayData(resource.getCourseBMSModels(),
							config.getLnmode(), autoplay - 3, resource.getConstraint());
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
						if (resource.getCourseBMSModels()[i].getMD5().equals(resource.getBMSModel().getMD5())) {
							replay = resource.getCourseReplay()[i];
						}
					}
				}
			} else {
				replay = main.getPlayDataAccessor().readReplayData(model, config.getLnmode(), autoplay - 3);
				if (replay == null) {
					autoplay = 0;
				}
			}
		}

		if (model.getRandom() != null && model.getRandom().length > 0) {
			if (autoplay >= 3) {
				model = resource.getGenerator().generate(replay.rand);
			} else if (resource.getReplayData().pattern != null) {
				model = resource.getGenerator().generate(resource.getReplayData().rand);
			}
			Logger.getGlobal().info("譜面分岐 : " + Arrays.toString(model.getRandom()));
		}
		// 通常プレイの場合は最後のノーツ、オートプレイの場合はBG/BGAを含めた最後のノーツ
		playtime = (autoplay == 1 ? model.getLastTime() : model.getLastNoteTime()) + TIME_MARGIN;
		gaugelog = new ArrayList<Float>(playtime / 500 + 2);

		boolean score = true;

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
				// SPでなければBATTLEは未適用
				LaneShuffleModifier mod = new LaneShuffleModifier(LaneShuffleModifier.BATTLE);
				mod.modify(model);
				model.setUseKeys(model.getUseKeys() * 2);
				assist = 1;
				score = false;
			}
		}

		judge = new JudgeManager(this, model, autoplay == 1, resource.getConstraint());
		if (exjudge) {
			judge.setJudgeMode(JudgeManager.EXPAND_JUDGE);
		}
		Logger.getGlobal().info("アシストオプション設定完了");

		if (replay != null) {
			PatternModifier.modify(model, Arrays.asList(replay.pattern));
		} else if (resource.getReplayData().pattern != null) {
			PatternModifier.modify(model, Arrays.asList(resource.getReplayData().pattern));
			Logger.getGlobal().info("譜面オプション : 保存された譜面変更ログから譜面再現");
		} else if (autoplay != 2) {
			switch (model.getUseKeys()) {
			case 10:
			case 14:
				if (config.getDoubleoption() == 1) {
					pattern = PatternModifier.merge(pattern,
							new LaneShuffleModifier(LaneShuffleModifier.FLIP).modify(model));
				}
				pattern = PatternModifier
						.merge(pattern,
								PatternModifier
										.create(config.getRandom2(), model.getUseKeys() == 14
												? PatternModifier.PLAYER2_7KEYS : PatternModifier.PLAYER2_5KEYS)
										.modify(model));
				if (config.getRandom2() >= 6) {
					assist = (assist == 0) ? 1 : assist;
					score = false;
				}
				Logger.getGlobal().info("譜面オプション :  " + config.getRandom2());
			case 5:
			case 7:
				pattern = PatternModifier.merge(pattern,
						PatternModifier
								.create(config.getRandom(),
										model.getUseKeys() == 7 || model.getUseKeys() == 14
												? PatternModifier.PLAYER1_7KEYS : PatternModifier.PLAYER1_5KEYS)
								.modify(model));
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
					config.setRandom(2);
				}
				if (config.getRandom() == 9) {
					config.setRandom(4);
				}
				pattern = PatternModifier.merge(pattern,
						PatternModifier.create(config.getRandom(), PatternModifier.NINEKEYS).modify(model));
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
		gauge = GrooveGauge.create(model, g, resource.getCourseBMSModels() != null);

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

		int skinmode = (model.getUseKeys() == 7 ? 0
				: (model.getUseKeys() == 5 ? 1 : (model.getUseKeys() == 14 ? 2 : (model.getUseKeys() == 10 ? 3 : 4))));
		if (resource.getConfig().getSkin()[skinmode] != null) {
			try {
				SkinConfig sc = resource.getConfig().getSkin()[skinmode];
				if(sc.getPath().endsWith(".json")) {
					SkinLoader sl = new SkinLoader(RESOLUTION[resource.getConfig().getResolution()]);
					setSkin(sl.loadPlaySkin(Paths.get(sc.getPath()), skinmode));
				} else {
					LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
					LR2SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), this, sc.getProperty());
					Rectangle srcr = RESOLUTION[header.getResolution()];
					Rectangle dstr = RESOLUTION[resource.getConfig().getResolution()];
					LR2PlaySkinLoader dloader = new LR2PlaySkinLoader(srcr.width, srcr.height, dstr.width, dstr.height);
					setSkin(dloader.loadPlaySkin(Paths.get(sc.getPath()).toFile(), this, header, loader.getOption(),
							sc.getProperty()));
				}
			} catch (IOException e) {
				e.printStackTrace();
				setSkin(new PlaySkin(model.getUseKeys(), config.isUse2pside(),
						RESOLUTION[resource.getConfig().getResolution()]));
			}
		} else {
			setSkin(new PlaySkin(model.getUseKeys(), config.isUse2pside(),
					RESOLUTION[resource.getConfig().getResolution()]));
		}
	}

	public void create() {
		final MainController main = getMainController();
		final PlayerResource resource = main.getPlayerResource();

		if (resource.getConfig().getSoundpath().length() > 0) {
			final File soundfolder = new File(resource.getConfig().getSoundpath());
			if (soundfolder.exists() && soundfolder.isDirectory()) {
				for (File f : soundfolder.listFiles()) {
					if (playstop == null && f.getName().startsWith("playstop.")) {
						playstop = f.getPath();
					}
				}
			}
		}

		Config config = resource.getConfig();
		Logger.getGlobal().info("create");

		final BMSPlayerInputProcessor input = main.getInputProcessor();
		input.setMinimumInputDutration(config.getInputduration());
		input.setDisableDevice(autoplay == 0 || autoplay == 2
				? (resource.getPlayDevice() == 0 ? new int[] { 1, 2 } : new int[] { 0 }) : null);
		PlayConfig pc = (model.getUseKeys() == 5 || model.getUseKeys() == 7 ? config.getMode7()
				: (model.getUseKeys() == 10 || model.getUseKeys() == 14 ? config.getMode14() : config.getMode9()));
		input.setKeyassign(pc.getKeyassign());
		input.setControllerassign(pc.getControllerassign());
		lanerender = new LaneRenderer(this, model);
		for (int i : resource.getConstraint()) {
			if (i == TableData.NO_HISPEED) {
				enableControl = false;
				break;
			}
		}

		((PlaySkin) getSkin()).setBMSPlayer(this);
		bga = resource.getBGAManager();

		IRScoreData score = main.getPlayDataAccessor().readScoreData(model, config.getLnmode());
		Logger.getGlobal().info("スコアデータベースからスコア取得");
		if (score == null) {
			score = new IRScoreData();
		}
		bestscore = score.getExscore();
		rivalscore = model.getTotalNotes() * 8 / 5;
		resource.setRivalScoreData(rivalscore);
		Logger.getGlobal().info("スコアグラフ描画クラス準備");

		if (autoplay == 2) {
			practice.create(model);
			state = STATE_PRACTICE;
		}
	}

	private int bestscore;
	private int rivalscore;

	@Override
	public void resize(int w, int h) {
		System.out.println("resize" + w + "," + h);
	}

	protected static final int STATE_PRELOAD = 0;
	protected static final int STATE_PRACTICE = 1;
	protected static final int STATE_PRACTICE_FINISHED = 2;
	protected static final int STATE_READY = 3;
	protected static final int STATE_PLAY = 4;
	protected static final int STATE_FAILED = 5;
	protected static final int STATE_FINISHED = 6;

	private int state = STATE_PRELOAD;

	private long prevtime;

	private PracticeConfiguration practice = new PracticeConfiguration();
	private int starttimeoffset;

	@Override
	public void render() {
		final PlaySkin skin = (PlaySkin) getSkin();
		final MainController main = getMainController();
		final PlayerResource resource = main.getPlayerResource();
		final BMSPlayerInputProcessor input = main.getInputProcessor();

		final long now = getNowTime();
		switch (state) {
		// 楽曲ロード
		case STATE_PRELOAD:
			if (resource.mediaLoadFinished() && now > skin.getLoadstart() + skin.getLoadend()
					&& !input.startPressed()) {
				bga.prepare(this);
				final long mem = Runtime.getRuntime().freeMemory();
				System.gc();
				final long cmem = Runtime.getRuntime().freeMemory();
				Logger.getGlobal().info("current free memory : " + (cmem / (1024 * 1024)) + "MB , disposed : "
						+ ((cmem - mem) / (1024 * 1024)) + "MB");
				state = STATE_READY;
				getTimer()[TIMER_READY] = now;
				Logger.getGlobal().info("STATE_READYに移行");
			}
			break;
		// practice mode
		case STATE_PRACTICE:
			if (getTimer()[TIMER_PLAY] != Long.MIN_VALUE) {
				resource.reloadBMSFile();
				model = resource.getBMSModel();
				lanerender.init(model);
				getTimer()[TIMER_PLAY] = Long.MIN_VALUE;
				getTimer()[TIMER_RHYTHM] = Long.MIN_VALUE;
				getTimer()[TIMER_FAILED] = Long.MIN_VALUE;
				getTimer()[TIMER_FADEOUT] = Long.MIN_VALUE;
				getTimer()[TIMER_ENDOFNOTE_1P] = Long.MIN_VALUE;
			}
			enableControl = false;
			practice.processInput(input);

			if (input.getKeystate()[0] && resource.mediaLoadFinished() && now > skin.getLoadstart() + skin.getLoadend()
					&& !input.startPressed()) {
				PracticeProperty property = practice.getPracticeProperty();
				enableControl = true;
				if (property.freq != 100) {
					model.setFrequency(property.freq / 100f);
				}
				PracticeModifier pm = new PracticeModifier(property.starttime * 100 / property.freq,
						property.endtime * 100 / property.freq);
				pm.modify(model);
				if (model.getUseKeys() >= 10) {
					if (property.doubleop == 1) {
						new LaneShuffleModifier(LaneShuffleModifier.FLIP).modify(model);
					}

					PatternModifier.create(property.random2,
							model.getUseKeys() == 14 ? PatternModifier.PLAYER2_7KEYS : PatternModifier.PLAYER2_5KEYS)
							.modify(model);
				}
				PatternModifier
						.create(property.random,
								model.getUseKeys() == 9 ? PatternModifier.NINEKEYS
										: (model.getUseKeys() == 7 || model.getUseKeys() == 14
												? PatternModifier.PLAYER1_7KEYS : PatternModifier.PLAYER1_5KEYS))
						.modify(model);

				gauge = practice.getGauge(model);
				model.setJudgerank(property.judgerank);
				lanerender.init(model);
				judge.init(model);
				notes = 0;
				starttimeoffset = (property.starttime > 1000 ? property.starttime - 1000 : 0) * 100 / property.freq;
				playtime = (property.endtime + 1000) * 100 / property.freq;
				bga.prepare(this);
				state = STATE_READY;
				getTimer()[TIMER_READY] = now;
				Logger.getGlobal().info("STATE_READYに移行");
			}
			break;
		// GET READY
		case STATE_PRACTICE_FINISHED:
			if (now - getTimer()[TIMER_FADEOUT] > skin.getFadeout()) {
				input.setDisableDevice(new int[0]);
				getMainController().changeState(MainController.STATE_SELECTMUSIC);
			}
			break;

		case STATE_READY:
			if (now - getTimer()[TIMER_READY] > skin.getPlaystart()) {
				state = STATE_PLAY;
				getTimer()[TIMER_PLAY] = now - starttimeoffset;
				getTimer()[TIMER_RHYTHM] = now - starttimeoffset;

				input.setStartTime(now + getStartTime() - starttimeoffset);
				List<KeyInputLog> keylog = null;
				if (autoplay >= 3) {
					keylog = Arrays.asList(replay.keylog);
				}
				autoThread = new AutoplayThread();
				autoThread.starttime = starttimeoffset;
				autoThread.start();
				keyinput = new KeyInputThread(keylog);
				keyinput.start();
				Logger.getGlobal().info("STATE_PLAYに移行");
			}
			break;
		// プレイ
		case STATE_PLAY:
			final long deltatime = now - prevtime;

			getTimer()[TIMER_PLAY] += deltatime * (100 - playspeed) / 100;
			getTimer()[TIMER_RHYTHM] += deltatime * (100 - lanerender.getNowBPM() * 100 / 60) / 100;
			scratch1 += 2160 - deltatime;
			scratch2 += deltatime;
			if (model.getUseKeys() != 9) {
				final boolean[] state = input.getKeystate();
				if (state[7]) {
					scratch1 += deltatime * 2;
				} else if (state[8]) {
					scratch1 += 2160 - deltatime * 2;
				}
				if (state[16]) {
					scratch2 += deltatime * 2;
				} else if (state[17]) {
					scratch2 += 2160 - deltatime * 2;
				}
			}
			scratch1 %= 2160;
			scratch2 %= 2160;
			final float g = gauge.getValue();
			if (gaugelog.size() <= (now - getTimer()[TIMER_PLAY]) / 500) {
				gaugelog.add(g);
			}
			if (g == gauge.getMaxValue() && getTimer()[TIMER_GAUGE_MAX_1P] == Long.MIN_VALUE) {
				getTimer()[TIMER_GAUGE_MAX_1P] = now;
			} else if (g < gauge.getMaxValue() && getTimer()[TIMER_GAUGE_MAX_1P] != Long.MIN_VALUE) {
				getTimer()[TIMER_GAUGE_MAX_1P] = Long.MIN_VALUE;
			}
			if (notes == getMainController().getPlayerResource().getSongdata().getNotes()
					&& getTimer()[TIMER_ENDOFNOTE_1P] == Long.MIN_VALUE
					&& playtime - TIME_MARGIN < (now - getTimer()[TIMER_PLAY])) {
				getTimer()[TIMER_ENDOFNOTE_1P] = now;
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
				if (resource.mediaLoadFinished()) {
					getMainController().getAudioProcessor().stop((Note) null);
				}
				if (playstop != null) {
					getMainController().getAudioProcessor().play(playstop, false);
				}
				Logger.getGlobal().info("STATE_FAILEDに移行");
			}

			prevtime = now;

			break;
		// 閉店処理
		case STATE_FAILED:
			if (autoThread != null) {
				autoThread.stop = true;
			}
			if (keyinput != null) {
				keyinput.stop = true;
			}

			if (now - getTimer()[TIMER_FAILED] > skin.getClose()) {
				if (resource.mediaLoadFinished()) {
					resource.getBGAManager().stop();
				}
				if (keyinput != null) {
					Logger.getGlobal().info("入力パフォーマンス(max ms) : " + keyinput.frametimes);
				}
				if (autoplay != 1 && autoplay != 2) {
					resource.setScoreData(createScoreData());
				}
				resource.setCombo(judge.getCourseCombo());
				resource.setMaxcombo(judge.getCourseMaxcombo());
				saveConfig();
				if (getTimer()[TIMER_PLAY] != Long.MIN_VALUE) {
					for (long l = getTimer()[TIMER_FAILED] - getTimer()[TIMER_PLAY]; l < playtime + 500; l += 500) {
						gaugelog.add(0f);
					}
				}
				resource.setGauge(gaugelog);
				resource.setGrooveGauge(gauge);
				input.setDisableDevice(new int[0]);
				input.setStartTime(0);
				if (autoplay == 2) {
					state = STATE_PRACTICE;
				} else if (resource.getScoreData() != null) {
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
			if (now - getTimer()[TIMER_FADEOUT] > skin.getFadeout()) {
				resource.getBGAManager().stop();
				if (keyinput != null) {
					Logger.getGlobal().info("入力パフォーマンス(max ms) : " + keyinput.frametimes);
				}
				if (autoplay != 1 && autoplay != 2) {
					resource.setScoreData(createScoreData());
				}
				resource.setCombo(judge.getCourseCombo());
				resource.setMaxcombo(judge.getCourseMaxcombo());
				saveConfig();
				resource.setGauge(gaugelog);
				resource.setGrooveGauge(gauge);
				input.setDisableDevice(new int[0]);
				input.setStartTime(0);
				if (autoplay == 2) {
					state = STATE_PRACTICE;
				} else if (resource.getScoreData() != null) {
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
	}

	private boolean hschanged;
	private long startpressedtime;
	private boolean startpressed;
	private boolean cursorpressed;
	private long lanecovertiming;

	private boolean enableControl = true;

	public void input() {
		final BMSPlayerInputProcessor input = getMainController().getInputProcessor();
		// 各種コントロール入力判定
		if (enableControl) {
			if (input.getCursorState()[0]) {
				if (!cursorpressed) {
					lanerender.setLanecover(lanerender.getLanecover() - 0.01f);
					cursorpressed = true;
				}
			} else if (input.getCursorState()[1]) {
				if (!cursorpressed) {
					lanerender.setLanecover(lanerender.getLanecover() + 0.01f);
					cursorpressed = true;
				}
			} else {
				cursorpressed = false;
			}
			// move lane cover by mouse wheel
			if (input.getScroll() != 0) {
				lanerender.setLanecover(lanerender.getLanecover() - input.getScroll() * 0.005f);
				input.resetScroll();
			}
			if (input.startPressed()) {
				if (autoplay == 0) {
					// change hi speed by START + Keys
					boolean[] key = input.getKeystate();
					if (key[0] || key[2] || key[4] || key[6]) {
						if (!hschanged) {
							lanerender.changeHispeed(false);
							hschanged = true;
						}
					} else if (key[1] || key[3] || key[5]) {
						if (!hschanged) {
							lanerender.changeHispeed(true);
							hschanged = true;
						}
					} else {
						hschanged = false;
					}

					// move lane cover by START + Scratch
					if (key[7] | key[8]) {
						long l = System.currentTimeMillis();
						if (l - lanecovertiming > 50) {
							lanerender.setLanecover(lanerender.getLanecover() + (key[7] ? 0.001f : -0.001f));
							lanecovertiming = l;
						}
					}
				}
				// show-hide lane cover by double-press START
				if (!startpressed) {
					long stime = System.currentTimeMillis();
					if (stime < startpressedtime + 500) {
						lanerender.setEnableLanecover(!lanerender.isEnableLanecover());
						startpressedtime = 0;
					} else {
						startpressedtime = stime;
					}
				}
				startpressed = true;
			} else {
				startpressed = false;
			}
		}

		// stop playing
		if (input.isExitPressed()) {
			input.setExitPressed(false);
			stopPlay();
		}
		// play speed change (autoplay or replay only)
		if (autoplay == 1 || autoplay >= 3) {
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
				: (model.getUseKeys() == 10 || model.getUseKeys() == 14 ? resource.getConfig().getMode14()
						: resource.getConfig().getMode9()));
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
		score.setGauge(GrooveGauge.getGaugeID(gauge));
		score.setOption(resource.getConfig().getRandom() + (model.getUseKeys() == 10 || model.getUseKeys() == 14
				? (resource.getConfig().getRandom2() * 10 + resource.getConfig().getDoubleoption() * 100) : 0));
		// リプレイデータ保存。スコア保存されない場合はリプレイ保存しない
		resource.getReplayData().keylog = getMainController().getInputProcessor().getKeyInputLog()
				.toArray(new KeyInputLog[0]);
		resource.getReplayData().pattern = pattern.toArray(new PatternModifyLog[pattern.size()]);
		resource.getReplayData().rand = model.getRandom();
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
		score.setDevice(resource.getPlayDevice());
		return score;
	}

	public void stopPlay() {
		if (state == STATE_PRACTICE) {
			practice.saveProperty();
			getTimer()[TIMER_FADEOUT] = getNowTime();
			state = STATE_PRACTICE_FINISHED;
			return;
		}
		if (state == STATE_PRELOAD || state == STATE_READY) {
			getTimer()[TIMER_FADEOUT] = getNowTime();
			state = STATE_PRACTICE_FINISHED;
			return;
		}
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
			if (getMainController().getPlayerResource().mediaLoadFinished()) {
				getMainController().getAudioProcessor().stop((Note) null);
			}
			if (playstop != null) {
				getMainController().getAudioProcessor().play(playstop, false);
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
		super.dispose();
		lanerender.dispose();
		Logger.getGlobal().info("システム描画のリソース解放");
	}

	public void play(Note note, float volume) {
		getMainController().getAudioProcessor().play(note, volume);
	}

	public void stop(Note note) {
		getMainController().getAudioProcessor().stop(note);
	}

	public PracticeConfiguration getPracticeConfiguration() {
		return practice;
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
		drate = notes == 0 ? 10000
				: (this.judge.getJudgeCount(0) * 2 + this.judge.getJudgeCount(1)) * 10000 / notes / 2;

		if (notes == getMainController().getPlayerResource().getSongdata().getNotes()
				&& getTimer()[TIMER_FULLCOMBO_1P] == Long.MIN_VALUE && this.judge.getJudgeCount(3) == 0
				&& this.judge.getJudgeCount(4) == 0) {
			getTimer()[TIMER_FULLCOMBO_1P] = getNowTime();
		}
	}

	public GrooveGauge getGauge() {
		return gauge;
	}

	/**
	 * キー入力処理用スレッド
	 * 
	 * @author exch
	 */
	class KeyInputThread extends Thread {
		private boolean stop = false;
		private long frametimes = 1;
		private final KeyInputLog[] keylog;

		public KeyInputThread(List<KeyInputLog> keylog) {
			this.keylog = keylog != null ? keylog.toArray(new KeyInputLog[keylog.size()]) : null;
		}

		@Override
		public void run() {
			int index = 0;

			long framet = 1;
			final TimeLine[] timelines = model.getAllTimeLines();
			final BMSPlayerInputProcessor input = getMainController().getInputProcessor();

			final int lasttime = timelines[timelines.length - 1].getTime() + BMSPlayer.TIME_MARGIN;

			int prevtime = -1;
			while (!stop) {
				final int time = (int) (getNowTime() - getTimer()[TIMER_PLAY]);
				// リプレイデータ再生
				if (time != prevtime) {
					if (keylog != null) {
						while (index < keylog.length && keylog[index].time <= time) {
							final KeyInputLog key = keylog[index];
							// if(input.getKeystate()[key.keycode] ==
							// key.pressed) {
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

					if (prevtime != -1) {
						final long nowtime = time - prevtime;
						framet = nowtime < framet ? framet : nowtime;
					}
					prevtime = time;
				} else {
					try {
						sleep(0, 500000);
					} catch (InterruptedException e) {
					}
				}

				if (time >= lasttime) {
					break;
				}
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

		private int starttime;

		@Override
		public void run() {
			final TimeLine[] timelines = model.getAllTimeLines();
			final int lasttime = timelines[timelines.length - 1].getTime() + BMSPlayer.TIME_MARGIN;
			final Config config = getMainController().getPlayerResource().getConfig();
			int p = 0;
			for (int time = starttime; p < timelines.length && timelines[p].getTime() < time; p++)
				;

			for (; !stop;) {
				final int time = (int) (getNowTime() - getTimer()[TIMER_PLAY]);
				// BGレーン再生
				while (p < timelines.length && timelines[p].getTime() <= time) {
					for (Note n : timelines[p].getBackGroundNotes()) {
						play(n, config.getBgvolume());
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
					}
				}
				if (time >= lasttime) {
					break;
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
		case NUMBER_PLAYTIME_MINUTE:
			return (int) (((int) (getTimer()[TIMER_PLAY] != Long.MIN_VALUE ? getNowTime() - getTimer()[TIMER_PLAY] : 0))
					/ 60000);
		case NUMBER_PLAYTIME_SECOND:
			return (((int) (getTimer()[TIMER_PLAY] != Long.MIN_VALUE ? getNowTime() - getTimer()[TIMER_PLAY] : 0))
					/ 1000) % 60;
		case NUMBER_TIMELEFT_MINUTE:
			return (int) ((playtime
					- (int) (getTimer()[TIMER_PLAY] != Long.MIN_VALUE ? getNowTime() - getTimer()[TIMER_PLAY] : 0)
					+ 1000) / 60000);
		case NUMBER_TIMELEFT_SECOND:
			return ((playtime
					- (int) (getTimer()[TIMER_PLAY] != Long.MIN_VALUE ? getNowTime() - getTimer()[TIMER_PLAY] : 0)
					+ 1000) / 1000) % 60;
		case NUMBER_LOADING_PROGRESS:
			return (int) ((getMainController().getAudioProcessor().getProgress() + bga.getProgress()) * 50);
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
			return lanerender.getCurrentDuration();
		case NUMBER_NOWBPM:
			return (int) lanerender.getNowBPM();
		case NUMBER_SCORE:
		case NUMBER_SCORE2:
			return judge.getJudgeCount(0) * 2 + judge.getJudgeCount(1);
		case NUMBER_TARGET_SCORE:
		case NUMBER_TARGET_SCORE2:
			return rivalscore;
		case NUMBER_DIFF_HIGHSCORE:
			return (judge.getJudgeCount(0) * 2 + judge.getJudgeCount(1)) - (bestscore * notes / song.getNotes());
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
		case NUMBER_SCRATCHANGLE_1P:
			return scratch1 / 6;
		case NUMBER_SCRATCHANGLE_2P:
			return scratch2 / 6;
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
			if (lanerender.isEnableLanecover()) {
				float lane = lanerender.getLanecover();
				if (lanerender.isEnableLift()) {
					lane = lane * (1 - lanerender.getLiftRegion());
				}
				return lane;
			}
			return 0;
		case OFFSET_LIFT:
			if (lanerender.isEnableLift()) {
				final PlaySkin skin = (PlaySkin) getSkin();
				return lanerender.getLiftRegion() * (skin.getHeight() - skin.getLaneGroupRegion()[0].y);
			}
			return 0;
		case OFFSET_LANECOVER:
			if (lanerender.isEnableLanecover()) {
				final PlaySkin skin = (PlaySkin) getSkin();
				if (lanerender.isEnableLift()) {
					return -lanerender.getLiftRegion() * lanerender.getLanecover()
							* (skin.getHeight() - skin.getLaneGroupRegion()[0].y);
				} else {
					return -lanerender.getLanecover() * (skin.getHeight() - skin.getLaneGroupRegion()[0].y);
				}
			}
			return 0;
		case BARGRAPH_LOAD_PROGRESS:
			float value = (getMainController().getAudioProcessor().getProgress() + bga.getProgress()) / 2;
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
			return gauge instanceof AssistEasyGrooveGauge || gauge instanceof EasyGrooveGauge
					|| gauge instanceof NormalGrooveGauge;
		case OPTION_GAUGE_HARD:
			return gauge instanceof HardGrooveGauge || gauge instanceof ExhardGrooveGauge
					|| gauge instanceof HazardGrooveGauge || gauge instanceof GradeGrooveGauge
					|| gauge instanceof ExgradeGrooveGauge || gauge instanceof ExhardGradeGrooveGauge;
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
					|| (getMainController().getPlayerResource().getConfig().getBga() == Config.BGA_AUTO
							&& (autoplay == 1 || autoplay >= 3));
		case OPTION_BGAOFF:
			return getMainController().getPlayerResource().getConfig().getBga() == Config.BGA_OFF
					|| (getMainController().getPlayerResource().getConfig().getBga() == Config.BGA_AUTO
							&& (autoplay == 0 || autoplay == 2));
		case OPTION_NOW_LOADING:
			return state == STATE_PRELOAD;
		case OPTION_LOADED:
			return state != STATE_PRELOAD;
		case OPTION_LANECOVER1_CHANGING:
			return startpressed;
		}
		return super.getBooleanValue(id);
	}

}
