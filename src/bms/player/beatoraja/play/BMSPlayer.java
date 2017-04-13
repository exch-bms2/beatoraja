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
import bms.player.beatoraja.skin.lr2.*;
import bms.player.beatoraja.song.SongData;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.FloatArray;

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
	private KeyInputProccessor keyinput;
	private ControlInputProcessor control;

	private int assist = 0;

	private List<PatternModifyLog> pattern = new ArrayList<PatternModifyLog>();

	private ReplayData replay = null;

	private final FloatArray gaugelog;

	private int playspeed = 100;

	/**
	 * 処理済ノート数
	 */
	private int notes;

	static final int TIME_MARGIN = 5000;
	
	public static final int SOUND_PLAYSTOP = 0;

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
		gaugelog = new FloatArray(playtime / 500 + 2);

		boolean score = true;

		Logger.getGlobal().info("アシストオプション設定");
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
		
		Logger.getGlobal().info("譜面オプション設定");
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
				} else if (config.getRandom() == 8) {
					config.setRandom(2);
				} else if (config.getRandom() == 9) {
					config.setRandom(4);
				}
				pattern = PatternModifier.merge(pattern,
						PatternModifier.create(config.getRandom(), PatternModifier.NINEKEYS).modify(model));
				if (config.getRandom() >= 6) {
					assist = (assist == 0) ? 1 : assist;
					score = false;
				}
				break;
			case 24:
				// 譜面オプションの扱いは保留
				break;
			}
		}

		Logger.getGlobal().info("ゲージ設定");
		gauge = GrooveGauge.create(model, replay != null ? replay.gauge : config.getGauge(), resource.getCourseBMSModels() != null);
		FloatArray f = resource.getGauge();
		if (f != null) {
			gauge.setValue(f.get(f.size - 1));
		}

		resource.setUpdateScore(score);
		final int difficulty = resource.getSongdata() != null ? resource.getSongdata().getDifficulty() : 0;
		resource.setSongdata(new SongData(model, false));
		resource.getSongdata().setDifficulty(difficulty);
	}

	private SkinType getSkinType() {
		switch (model.getUseKeys()) {
		case 7:
			return SkinType.PLAY_7KEYS;
		case 5:
			return SkinType.PLAY_5KEYS;
		case 14:
			return SkinType.PLAY_14KEYS;
		case 10:
			return SkinType.PLAY_10KEYS;
		case 9:
			return SkinType.PLAY_9KEYS;
		case 24:
			return SkinType.PLAY_24KEYS;
		default:
			return null;
		}
	}

	public PlayConfig getPlayConfig(Config config) {
		switch (model.getUseKeys()) {
		case 7:
		case 5:
			return config.getMode7();
		case 14:
		case 10:
			return config.getMode14();
		case 9:
			return config.getMode9();
		case 24:
			return config.getMode24();
		default:
			return null;
		}
	}


	public void create() {
		final MainController main = getMainController();
		final PlayerResource resource = main.getPlayerResource();
		judge = new JudgeManager(this);
		control = new ControlInputProcessor(this, autoplay);
		keyinput = new KeyInputProccessor(this, model.getUseKeys());
		Config config = resource.getConfig();

		SkinType skinType = getSkinType();
		final String[] defaultskins = { SkinConfig.DEFAULT_PLAY7, SkinConfig.DEFAULT_PLAY5, SkinConfig.DEFAULT_PLAY14,
				SkinConfig.DEFAULT_PLAY10, SkinConfig.DEFAULT_PLAY9 };
		try {
			SkinConfig sc = resource.getConfig().getSkin()[skinType.getId()];
			if (sc.getPath().endsWith(".json")) {
				SkinLoader sl = new SkinLoader(RESOLUTION[resource.getConfig().getResolution()]);
				setSkin(sl.loadPlaySkin(Paths.get(sc.getPath()), skinType, sc.getProperty()));
			} else {
				LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
				SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), this, sc.getProperty());
				Rectangle srcr = RESOLUTION[header.getResolution()];
				Rectangle dstr = RESOLUTION[resource.getConfig().getResolution()];
				LR2PlaySkinLoader dloader = new LR2PlaySkinLoader(srcr.width, srcr.height, dstr.width, dstr.height);
				setSkin(dloader.loadPlaySkin(Paths.get(sc.getPath()).toFile(), this, header, loader.getOption(),
						sc.getProperty()));
			}
		} catch (Throwable e) {
			e.printStackTrace();
			SkinLoader sl = new SkinLoader(RESOLUTION[resource.getConfig().getResolution()]);
			setSkin(sl.loadPlaySkin(Paths.get(SkinConfig.defaultSkinPathMap.get(skinType)), skinType, new HashMap()));
		}

		setSound(SOUND_PLAYSTOP, config.getSoundpath() + File.separatorChar + "playstop.wav", false);

		final BMSPlayerInputProcessor input = main.getInputProcessor();
		input.setMinimumInputDutration(config.getInputduration());
		input.setDisableDevice(autoplay == 0 || autoplay == 2
				? (resource.getPlayDevice() == 0 ? new int[] { 1, 2 } : new int[] { 0 }) : null);
		PlayConfig pc = getPlayConfig(config);
		input.setKeyassign(pc.getKeyassign());
		input.setControllerConfig(pc.getController());
		input.setMidiConfig(pc.getMidiConfig());
		lanerender = new LaneRenderer(this, model);
		for (int i : resource.getConstraint()) {
			if (i == TableData.NO_HISPEED) {
				control.setEnableControl(false);
				break;
			}
		}

		judge.init(model, resource);
		bga = resource.getBGAManager();

		IRScoreData score = main.getPlayDataAccessor().readScoreData(model, config.getLnmode());
		Logger.getGlobal().info("スコアデータベースからスコア取得");
		if (score == null) {
			score = new IRScoreData();
		}

		int rivalscore = TargetProperty.getAllTargetProperties(getMainController())[config.getTarget()]
				.getTarget(getMainController());
		resource.setRivalScoreData(rivalscore);
		getScoreDataProperty().setTargetScore(score.getExscore(), rivalscore, model.getTotalNotes());

		if (autoplay == 2) {
			practice.create(model);
			state = STATE_PRACTICE;
		}
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
        final long[] timer = getTimer();
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
				timer[TIMER_READY] = now;
				Logger.getGlobal().info("STATE_READYに移行");
			}
			break;
		// practice mode
		case STATE_PRACTICE:
			if (getTimer()[TIMER_PLAY] != Long.MIN_VALUE) {
				resource.reloadBMSFile();
				model = resource.getBMSModel();
				lanerender.init(model);
                timer[TIMER_PLAY] = Long.MIN_VALUE;
                timer[TIMER_RHYTHM] = Long.MIN_VALUE;
                timer[TIMER_FAILED] = Long.MIN_VALUE;
                timer[TIMER_FADEOUT] = Long.MIN_VALUE;
                timer[TIMER_ENDOFNOTE_1P] = Long.MIN_VALUE;
			}
			control.setEnableControl(false);
			practice.processInput(input);

			if (input.getKeystate()[0] && resource.mediaLoadFinished() && now > skin.getLoadstart() + skin.getLoadend()
					&& !input.startPressed()) {
				PracticeProperty property = practice.getPracticeProperty();
				control.setEnableControl(true);
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
				judge.init(model, resource);
				notes = 0;
				starttimeoffset = (property.starttime > 1000 ? property.starttime - 1000 : 0) * 100 / property.freq;
				playtime = (property.endtime + 1000) * 100 / property.freq;
				bga.prepare(this);
				state = STATE_READY;
                timer[TIMER_READY] = now;
				Logger.getGlobal().info("STATE_READYに移行");
			}
			break;
		// practice終了
		case STATE_PRACTICE_FINISHED:
			if (now - getTimer()[TIMER_FADEOUT] > skin.getFadeout()) {
				input.setDisableDevice(new int[0]);
				getMainController().changeState(MainController.STATE_SELECTMUSIC);
			}
			break;
			// GET READY
		case STATE_READY:
			if (now - getTimer()[TIMER_READY] > skin.getPlaystart()) {
				state = STATE_PLAY;
                timer[TIMER_PLAY] = now - starttimeoffset;
                timer[TIMER_RHYTHM] = now - starttimeoffset;

				input.setStartTime(now + getStartTime() - starttimeoffset);
				List<KeyInputLog> keylog = null;
				if (autoplay >= 3) {
					keylog = Arrays.asList(replay.keylog);
				}
				keyinput.startJudge(model, keylog);
				autoThread = new AutoplayThread();
				autoThread.starttime = starttimeoffset;
				autoThread.start();
				Logger.getGlobal().info("STATE_PLAYに移行");
			}
			break;
		// プレイ
		case STATE_PLAY:
			// TODO fpsが高い時にスローがかからなくなる
			final long deltatime = now - prevtime;
            timer[TIMER_PLAY] += deltatime * (100 - playspeed) / 100;
            timer[TIMER_RHYTHM] += deltatime * (100 - lanerender.getNowBPM() * 100 / 60) / 100;
            final long ptime = now - timer[TIMER_PLAY];
			final float g = gauge.getValue();
			if (gaugelog.size <= ptime / 500) {
				gaugelog.add(g);
			}
			if (g == gauge.getMaxValue() && timer[TIMER_GAUGE_MAX_1P] == Long.MIN_VALUE) {
                timer[TIMER_GAUGE_MAX_1P] = now;
			} else if (g < gauge.getMaxValue() && timer[TIMER_GAUGE_MAX_1P] != Long.MIN_VALUE) {
                timer[TIMER_GAUGE_MAX_1P] = Long.MIN_VALUE;
			}

            // System.out.println("playing time : " + time);
			if (playtime < ptime) {
				state = STATE_FINISHED;
                timer[TIMER_FADEOUT] = now;
				Logger.getGlobal().info("STATE_FINISHEDに移行");
			} else if(playtime - TIME_MARGIN < ptime && timer[TIMER_ENDOFNOTE_1P] == Long.MIN_VALUE) {
                timer[TIMER_ENDOFNOTE_1P] = now;
            }
			// stage failed判定
			if (g == 0) {
				state = STATE_FAILED;
                timer[TIMER_FAILED] = now;
				if (resource.mediaLoadFinished()) {
					getMainController().getAudioProcessor().stop((Note) null);
				}
				play(SOUND_PLAYSTOP);
				Logger.getGlobal().info("STATE_FAILEDに移行");
			}
			break;
		// 閉店処理
		case STATE_FAILED:
			if (autoThread != null) {
				autoThread.stop = true;
			}
			keyinput.stopJudge();

			if (now - timer[TIMER_FAILED] > skin.getClose()) {
				if (resource.mediaLoadFinished()) {
					resource.getBGAManager().stop();
				}
				if (autoplay != 1 && autoplay != 2) {
					resource.setScoreData(createScoreData());
				}
				resource.setCombo(judge.getCourseCombo());
				resource.setMaxcombo(judge.getCourseMaxcombo());
				saveConfig();
				if (timer[TIMER_PLAY] != Long.MIN_VALUE) {
					for (long l = timer[TIMER_FAILED] - timer[TIMER_PLAY]; l < playtime + 500; l += 500) {
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
			keyinput.stopJudge();
			if (now - timer[TIMER_FADEOUT] > skin.getFadeout()) {
				resource.getBGAManager().stop();
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
					if (resource.mediaLoadFinished()) {
						getMainController().getAudioProcessor().stop((Note) null);
					}
					if (resource.getCourseBMSModels() != null && resource.nextCourse()) {
						main.changeState(MainController.STATE_PLAYBMS);
					} else {
						main.changeState(MainController.STATE_SELECTMUSIC);
					}
				}
			}
			break;
		}
		
		prevtime = now;
	}

	public void setPlaySpeed(int playspeed) {
		this.playspeed = playspeed;
	}

	public void input() {
		control.input();
		keyinput.input();
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
		final ReplayData replay = resource.getReplayData();
		replay.player = getMainController().getPlayerConfig().getName();
		replay.sha256 = model.getSHA256();
		replay.mode = resource.getConfig().getLnmode();
		replay.date = Calendar.getInstance().getTimeInMillis() / 1000;
		replay.keylog = getMainController().getInputProcessor().getKeyInputLog().toArray(new KeyInputLog[0]);
		replay.pattern = pattern.toArray(new PatternModifyLog[pattern.size()]);
		replay.rand = model.getRandom();
		replay.gauge = resource.getConfig().getGauge();

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
			play(SOUND_PLAYSTOP);
			Logger.getGlobal().info("STATE_FAILEDに移行");
		}
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
		// System.out.println("Now count : " + notes + " - " + totalnotes);

		if (notes == getMainController().getPlayerResource().getSongdata().getNotes()) {
			//フルコン判定
			if(getTimer()[TIMER_FULLCOMBO_1P] == Long.MIN_VALUE && this.judge.getJudgeCount(3) == 0
				&& this.judge.getJudgeCount(4) == 0) {
				getTimer()[TIMER_FULLCOMBO_1P] = getNowTime();				
			}
		}

		getScoreDataProperty().update(this.judge.getJudgeCount(0) * 2 + this.judge.getJudgeCount(1),
				getMainController().getPlayerResource().getSongdata().getNotes(), notes);
	}

	public GrooveGauge getGauge() {
		return gauge;
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

			while (!stop) {
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
						final long sleeptime = timelines[p].getTime() - time;
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
		case NUMBER_MAXCOMBO:
		case NUMBER_MAXCOMBO2:
			return judge.getMaxcombo();
		case NUMBER_SCRATCHANGLE_1P:
			return keyinput.getScratchState(0);
		case NUMBER_SCRATCHANGLE_2P:
			return keyinput.getScratchState(1);
		case VALUE_JUDGE_1P_DURATION:
		case VALUE_JUDGE_2P_DURATION:
		case VALUE_JUDGE_3P_DURATION:
			return judge.getRecentJudgeTiming();
		}
		if (id >= VALUE_JUDGE_1P_SCRATCH && id < VALUE_JUDGE_1P_SCRATCH + 20) {
			return judge.getJudge()[id - VALUE_JUDGE_1P_SCRATCH];
		}
		return super.getNumberValue(id);
	}

	@Override
	public float getSliderValue(int id) {
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
					return -(1 - lanerender.getLiftRegion()) * lanerender.getLanecover()
							* (skin.getHeight() - skin.getLaneGroupRegion()[0].y);
				} else {
					return -lanerender.getLanecover() * (skin.getHeight() - skin.getLaneGroupRegion()[0].y);
				}
			}
			return 0;
		case BARGRAPH_LOAD_PROGRESS:
			float value = (getMainController().getAudioProcessor().getProgress() + bga.getProgress()) / 2;
			return value;
		}
		return super.getSliderValue(id);
	}

	public boolean getBooleanValue(int id) {
		switch (id) {
		case OPTION_GAUGE_GROOVE:
			return gauge instanceof AssistEasyGrooveGauge || gauge instanceof EasyGrooveGauge
					|| gauge instanceof NormalGrooveGauge;
		case OPTION_GAUGE_HARD:
			return gauge instanceof HardGrooveGauge || gauge instanceof ExhardGrooveGauge
					|| gauge instanceof HazardGrooveGauge || gauge instanceof GradeGrooveGauge
					|| gauge instanceof ExgradeGrooveGauge || gauge instanceof ExhardGradeGrooveGauge;
		case OPTION_GAUGE_EX:
			return gauge instanceof AssistEasyGrooveGauge || gauge instanceof EasyGrooveGauge
					|| gauge instanceof ExhardGrooveGauge || gauge instanceof ExgradeGrooveGauge
					|| gauge instanceof ExhardGradeGrooveGauge || gauge instanceof HazardGrooveGauge;
		case OPTION_AUTOPLAYON:
			return autoplay == 1;
		case OPTION_AUTOPLAYOFF:
			return autoplay != 1;
		case OPTION_REPLAY_OFF:
			return autoplay == 0 || autoplay == 2;
		case OPTION_REPLAY_PLAYING:
			return autoplay >= 3;
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
			return getMainController().getInputProcessor().startPressed();
		case OPTION_1P_PERFECT:
			return judge.getNowJudge()[0] == 1;
		case OPTION_1P_EARLY:
			return judge.getNowJudge()[0] > 1 && judge.getRecentJudgeTiming() > 0;
		case OPTION_1P_LATE:
			return judge.getNowJudge()[0] > 1 && judge.getRecentJudgeTiming() < 0;
		case OPTION_2P_PERFECT:
			return judge.getNowJudge().length > 1 && judge.getNowJudge()[1] == 1;
		case OPTION_2P_EARLY:
			return judge.getNowJudge().length > 1 && judge.getNowJudge()[1] > 1
					&& judge.getRecentJudgeTiming() > 0;
		case OPTION_2P_LATE:
			return judge.getNowJudge().length > 1 && judge.getNowJudge()[1] > 1
					&& judge.getRecentJudgeTiming() < 0;
		case OPTION_3P_PERFECT:
			return judge.getNowJudge().length > 2 && judge.getNowJudge()[2] == 1;
		case OPTION_3P_EARLY:
			return judge.getNowJudge().length > 2 && judge.getNowJudge()[2] > 1
					&& judge.getRecentJudgeTiming() > 0;
		case OPTION_3P_LATE:
			return judge.getNowJudge().length > 2 && judge.getNowJudge()[2] > 1
					&& judge.getRecentJudgeTiming() < 0;
		}
		return super.getBooleanValue(id);
	}
}
