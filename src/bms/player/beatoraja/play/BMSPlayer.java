package bms.player.beatoraja.play;

import static bms.player.beatoraja.CourseData.CourseDataConstraint.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.config.Discord;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.AudioConfig.FrequencyType;
import bms.player.beatoraja.input.*;
import bms.player.beatoraja.pattern.*;
import bms.player.beatoraja.pattern.Random;
import bms.player.beatoraja.play.PracticeConfiguration.PracticeProperty;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.skin.SkinType;

/**
 * BMSプレイヤー本体
 *
 * @author exch
 */
public class BMSPlayer extends MainState {

	private BMSModel model;

	private LaneRenderer lanerender;
	private LaneProperty laneProperty;
	private JudgeManager judge;

	private BGAProcessor bga;

	private GrooveGauge gauge;

	private int playtime;

	/**
	 * キー入力用スレッド
	 */
	private KeyInputProccessor keyinput;
	private ControlInputProcessor control;

	private KeySoundProcessor keysound;

	private int assist = 0;

	private ReplayData playinfo = new ReplayData();
	/**
	 * リプレイデータ
	 */
	private ReplayData replay = null;

	private final FloatArray[] gaugelog;

	private int playspeed = 100;

	/**
	 * 処理済ノート数
	 */
	private int notes;
	/**
	 * PMS キャラ用 ニュートラルモーション開始時の処理済ノート数{1P,2P} (ニュートラルモーション一周時に変化がなければニュートラルモーションを継続するため)
	 */
	private int[] PMcharaLastnotes = {0, 0};
	/**
	 * リプレイHS保存用 STATE READY時に保存
	 */
	private PlayConfig replayConfig;

	static final int TIME_MARGIN = 5000;

	public static final int SOUND_READY = 0;
	public static final int SOUND_PLAYSTOP = 1;

	private int state = STATE_PRELOAD;

	public static final int STATE_PRELOAD = 0;
	public static final int STATE_PRACTICE = 1;
	public static final int STATE_PRACTICE_FINISHED = 2;
	public static final int STATE_READY = 3;
	public static final int STATE_PLAY = 4;
	public static final int STATE_FAILED = 5;
	public static final int STATE_FINISHED = 6;

	private long prevtime;

	private PracticeConfiguration practice = new PracticeConfiguration();
	private long starttimeoffset;

	private RhythmTimerProcessor rhythm;
	private long startpressedtime;

	public static Discord discord;

	public BMSPlayer(MainController main, PlayerResource resource) {
		super(main);
		this.model = resource.getBMSModel();
		BMSPlayerMode autoplay = resource.getPlayMode();
		PlayerConfig config = resource.getPlayerConfig();

		playinfo.randomoption = config.getRandom();
		playinfo.randomoption2 = config.getRandom2();
		playinfo.doubleoption = config.getDoubleoption();

		ReplayData HSReplay = null;

		// TODO ターゲットスコアはPlayerResourceで受け渡す
		if(resource.getRivalScoreData() == null) {
		} else {
			ScoreData rival = resource.getRivalScoreData();
			if(rival.getSeed() != -1) {
				playinfo.randomoption = rival.getOption() % 10;
				playinfo.randomoption2 = (rival.getOption() / 10) % 10;
				playinfo.doubleoption = rival.getOption() / 100;
				playinfo.randomoptionseed = rival.getSeed() % (65536 * 256);
				playinfo.randomoption2seed = rival.getSeed() / (65536 * 256);
//				main.getMessageRenderer().addMessage("Rival Chart Option Mode - Option : " + playinfo.randomoption + "/" +
//						playinfo.randomoption2 + "/" + playinfo.doubleoption + " , Seed : " + playinfo.randomoptionseed + "/" + playinfo.randomoption2seed, 3000, Color.GOLD, 0);
			}
		}

		if (autoplay.mode == BMSPlayerMode.Mode.REPLAY) {
			if (resource.getCourseBMSModels() != null) {
				// コースモードのリプレイ読み込み
				if (resource.getCourseReplay().length == 0) {
					// コースモード1曲目の処理
					ReplayData[] replays = main.getPlayDataAccessor().readReplayData(resource.getCourseBMSModels(),
							config.getLnmode(), autoplay.id, resource.getConstraint());
					if (replays != null) {
						for (ReplayData rd : replays) {
							resource.addCourseReplay(rd);
						}
						replay = replays[0];
					} else {
						Logger.getGlobal().info("リプレイデータを読み込めなかったため、通常プレイモードに移行");
						autoplay = BMSPlayerMode.PLAY;
						resource.setPlayMode(autoplay);
					}
				} else {
					// 2曲目以降の処理
					for (int i = 0; i < resource.getCourseBMSModels().length; i++) {
						if (resource.getCourseBMSModels()[i].getMD5().equals(resource.getBMSModel().getMD5())) {
							replay = resource.getCourseReplay()[i];
						}
					}
				}
			} else {
				// 1曲モードのリプレイ読み込み
				replay = main.getPlayDataAccessor().readReplayData(model, config.getLnmode(), autoplay.id);
				if (replay != null) {
					boolean isReplayPatternPlay = false;
					if(main.getInputProcessor().getKeyState(1)) {
						//保存された譜面オプション/Random Seedから譜面再現
						Logger.getGlobal().info("リプレイ再現モード : 譜面");
						playinfo.randomoption = replay.randomoption;
						playinfo.randomoptionseed = replay.randomoptionseed;
						playinfo.randomoption2 = replay.randomoption2;
						playinfo.randomoption2seed = replay.randomoption2seed;
						playinfo.doubleoption = replay.doubleoption;
						playinfo.rand = replay.rand;
						isReplayPatternPlay = true;
					} else if(main.getInputProcessor().getKeyState(2)) {
						//保存された譜面オプションログから譜面オプション再現
						Logger.getGlobal().info("リプレイ再現モード : オプション");
						playinfo.randomoption = replay.randomoption;
						playinfo.randomoption2 = replay.randomoption2;
						playinfo.doubleoption = replay.doubleoption;
						isReplayPatternPlay = true;
					}
					if(main.getInputProcessor().getKeyState(4)) {
						//保存されたHSオプションログからHSオプション再現
						Logger.getGlobal().info("リプレイ再現モード : ハイスピード");
						HSReplay = replay;
						isReplayPatternPlay = true;
					}
					if(isReplayPatternPlay) {
						replay = null;
						autoplay = BMSPlayerMode.PLAY;
						resource.setPlayMode(autoplay);
					}
				} else {
					Logger.getGlobal().info("リプレイデータを読み込めなかったため、通常プレイモードに移行");
					autoplay = BMSPlayerMode.PLAY;
					resource.setPlayMode(autoplay);
				}
			}
		}

		boolean score = true;

		// RANDOM構文処理
		if (model.getRandom() != null && model.getRandom().length > 0) {
			if (autoplay.mode == BMSPlayerMode.Mode.REPLAY) {
				playinfo.rand = replay.rand;
			} else if (resource.getReplayData().randomoptionseed != -1) {
				// この処理はMusicResult、QuickRetry時にのみ通る
				playinfo.rand = resource.getReplayData().rand;
			}

			if(playinfo.rand != null && playinfo.rand.length > 0) {
				model = resource.loadBMSModel(playinfo.rand);
				// 暫定処置
				BMSModelUtils.setStartNoteTime(model, 1000);
				BMSPlayerRule.validate(model);
			}
			playinfo.rand = model.getRandom();
			Logger.getGlobal().info("譜面分岐 : " + Arrays.toString(playinfo.rand));
		}
		// 通常プレイの場合は最後のノーツ、オートプレイの場合はBG/BGAを含めた最後のノーツ
		playtime = (autoplay.mode == BMSPlayerMode.Mode.AUTOPLAY ? model.getLastTime() : model.getLastNoteTime()) + TIME_MARGIN;

		if (autoplay.mode == BMSPlayerMode.Mode.PLAY || autoplay.mode == BMSPlayerMode.Mode.AUTOPLAY) {
			if (config.isBpmguide() && (model.getMinBPM() < model.getMaxBPM())) {
				// BPM変化がなければBPMガイドなし
				assist = Math.max(assist, 1);
				score = false;
			}

			if (config.isCustomJudge() &&
					(config.getKeyJudgeWindowRatePerfectGreat() > 100 || config.getKeyJudgeWindowRateGreat() > 100 || config.getKeyJudgeWindowRateGood() > 100
					|| config.getScratchJudgeWindowRatePerfectGreat() > 100 || config.getScratchJudgeWindowRateGreat() > 100 || config.getScratchJudgeWindowRateGood() > 100)) {
				assist = Math.max(assist, 2);
				score = false;
			}

			Array<PatternModifier> mods = new Array<PatternModifier>();

			if(config.getScrollMode() > 0) {
				mods.add(new ScrollSpeedModifier(config.getScrollMode() - 1, config.getScrollSection(), config.getScrollRate()));
			}
			if(config.getLongnoteMode() > 0) {
				mods.add(new LongNoteModifier(config.getLongnoteMode() - 1, config.getLongnoteRate()));
			}
			if(config.getMineMode() > 0) {
				mods.add(new MineNoteModifier(config.getMineMode() - 1));
			}
			if(config.getExtranoteDepth() > 0) {
				mods.add(new ExtraNoteModifier(config.getExtranoteType(), config.getExtranoteDepth(), config.isExtranoteScratch()));
			}

			for(PatternModifier mod : mods) {
				mod.modify(model);
				if(mod.getAssistLevel() != PatternModifier.AssistLevel.NONE) {
					assist = Math.max(assist, mod.getAssistLevel() == PatternModifier.AssistLevel.ASSIST ? 2 : 1);
					score = false;
				}
			}

			if (playinfo.doubleoption >= 2) {
				if(model.getMode() == Mode.BEAT_5K || model.getMode() == Mode.BEAT_7K || model.getMode() == Mode.KEYBOARD_24K) {
					switch (model.getMode()) {
					case BEAT_5K:
						model.setMode(Mode.BEAT_10K);
						break;
					case BEAT_7K:
						model.setMode(Mode.BEAT_14K);
						break;
					case KEYBOARD_24K:
						model.setMode(Mode.KEYBOARD_24K_DOUBLE);
						break;
					}
					LaneShuffleModifier mod = new LaneShuffleModifier(Random.BATTLE);
					mod.setModifyTarget(PatternModifier.SIDE_1P);
					mod.modify(model);
					if(playinfo.doubleoption == 3) {
						PatternModifier as = new AutoplayModifier(model.getMode().scratchKey);
						as.modify(model);
					}
					assist = Math.max(assist, 1);
					score = false;
					Logger.getGlobal().info("譜面オプション : BATTLE (L-ASSIST)");
				} else {
					// SPでなければBATTLEは未適用
					playinfo.doubleoption = 0;
				}
			}
		}

		Logger.getGlobal().info("譜面オプション設定");
		if (replay != null && replay.pattern != null) {
			// リプレイ譜面再現(PatternModifyLog使用。旧verとの互換性維持用)
			if(replay.sevenToNinePattern > 0 && model.getMode() == Mode.BEAT_7K) {
				model.setMode(Mode.POPN_9K);
			}
			PatternModifier.modify(model, Arrays.asList(replay.pattern));
			Logger.getGlobal().info("リプレイデータから譜面再現 : PatternModifyLog");
		} else if (autoplay.mode != BMSPlayerMode.Mode.PRACTICE) {

			// リプレイデータからのoption/seed再現
			ReplayData rd = null;
			if(replay != null) {
				rd = replay;
				Logger.getGlobal().info("リプレイデータから譜面再現 : option/seed");
			} else if(resource.getReplayData().randomoptionseed != -1) {
				rd = resource.getReplayData();
				Logger.getGlobal().info("前回プレイ時の譜面再現");
			}
			if (rd != null) {
				if(rd.sevenToNinePattern > 0 && model.getMode() == Mode.BEAT_7K) {
					model.setMode(Mode.POPN_9K);
				}
				playinfo.randomoption = rd.randomoption;
				playinfo.randomoptionseed = rd.randomoptionseed;
				playinfo.randomoption2 = rd.randomoption2;
				playinfo.randomoption2seed = rd.randomoption2seed;
				playinfo.doubleoption = rd.doubleoption;
			}

			Array<PatternModifier> mods = new Array<PatternModifier>();
			// DP譜面オプション
			if(model.getMode().player == 2) {
				if (playinfo.doubleoption == 1) {
					mods.add(new LaneShuffleModifier(Random.FLIP));
				}
				Logger.getGlobal().info("譜面オプション(DP) :  " + playinfo.doubleoption);

				PatternModifier pm = PatternModifier.create(playinfo.randomoption2, PatternModifier.SIDE_2P, model.getMode(), config);
				if(playinfo.randomoption2seed != -1) {
					pm.setSeed(playinfo.randomoption2seed);
				} else {
					playinfo.randomoption2seed = pm.getSeed();
				}
				mods.add(pm);
				Logger.getGlobal().info("譜面オプション(2P) :  " + playinfo.randomoption2 + ", Seed : " + playinfo.randomoption2seed);
			}

			// POPN_9KのSCR系RANDOMにPOPN_5Kは対応していないため、非SCR系RANDOMに変更
			if(model.getMode() == Mode.POPN_5K) {
				switch(Random.getRandom(playinfo.randomoption)) {
				case ALL_SCR: playinfo.randomoption = Random.IDENTITY.id;
					break;
				case RANDOM_EX: playinfo.randomoption = Random.RANDOM.id;
					break;
				case S_RANDOM_EX: playinfo.randomoption = Random.S_RANDOM.id;
					break;
				default:
					break;
				}
			}

			// SP譜面オプション
			PatternModifier pm = PatternModifier.create(playinfo.randomoption, PatternModifier.SIDE_1P, model.getMode(), config);
			if(playinfo.randomoptionseed != -1) {
				pm.setSeed(playinfo.randomoptionseed);
			} else {
				playinfo.randomoptionseed = pm.getSeed();
			}
			mods.add(pm);
			Logger.getGlobal().info("譜面オプション(1P) :  " + playinfo.randomoption + ", Seed : " + playinfo.randomoptionseed);

			if (config.getSevenToNinePattern() >= 1 && model.getMode() == Mode.BEAT_7K) {
				//7to9
				ModeModifier mod = new ModeModifier(Mode.BEAT_7K, Mode.POPN_9K, config);
				mod.setModifyTarget(PatternModifier.SIDE_1P);
				mods.add(mod);
			}

			List<PatternModifyLog> pattern = new ArrayList<PatternModifyLog>();
			for(PatternModifier mod : mods) {
				pattern = PatternModifier.merge(pattern,mod.modify(model));
				if(mod.getAssistLevel() != PatternModifier.AssistLevel.NONE) {
					Logger.getGlobal().info("アシスト譜面オプションが選択されました");
					assist = Math.max(assist, mod.getAssistLevel() == PatternModifier.AssistLevel.ASSIST ? 2 : 1);
					score = false;
				}
			}
//			playinfo.pattern = pattern.toArray(new PatternModifyLog[pattern.size()]);

		}

		if(HSReplay != null && HSReplay.config != null) {
			//保存されたHSオプションログからHSオプション再現
			config.getPlayConfig(model.getMode()).setPlayconfig(HSReplay.config);
		}

		Logger.getGlobal().info("ゲージ設定");
		if(replay != null) {
			for(int count = (main.getInputProcessor().getKeyState(5) ? 1 : 0) + (main.getInputProcessor().getKeyState(3) ? 2 : 0);count > 0; count--) {
				if (replay.gauge != GrooveGauge.HAZARD || replay.gauge != GrooveGauge.EXHARDCLASS) {
					replay.gauge++;
				}
			}
		}
		if(replay != null && main.getInputProcessor().getKeyState(5)) {
		}
		// プレイゲージ、初期値設定
		gauge = GrooveGauge.create(model, replay != null ? replay.gauge : config.getGauge(), resource);
		// ゲージログ初期化
		gaugelog = new FloatArray[gauge.getGaugeTypeLength()];
		for(int i = 0; i < gaugelog.length; i++) {
			gaugelog[i] = new FloatArray(playtime / 500 + 2);
		}

		Logger.getGlobal().info("アシストレベル : " + assist + " - スコア保存 : " + score);

		resource.setUpdateScore(score);
		resource.setUpdateCourseScore(resource.isUpdateCourseScore() && score);
		final int difficulty = resource.getSongdata() != null ? resource.getSongdata().getDifficulty() : 0;
		resource.getSongdata().setBMSModel(model);
		resource.getSongdata().setDifficulty(difficulty);

		discord = Discord.playingSong(resource.getSongdata().getFullTitle(), resource.getSongdata().getArtist(), resource.getSongdata().getMode());
		discord.update();
	}

	public SkinType getSkinType() {
		for(SkinType type : SkinType.values()) {
			if(type.getMode() == model.getMode()) {
				return type;
			}
		}
		return null;
	}

	public void create() {
		final PlayerResource resource = main.getPlayerResource();
		final BMSPlayerMode autoplay = resource.getPlayMode();
		laneProperty = new LaneProperty(model.getMode());
		keysound = new KeySoundProcessor(this);
		judge = new JudgeManager(this);
		control = new ControlInputProcessor(this, autoplay);
		keyinput = new KeyInputProccessor(this, laneProperty);
		PlayerConfig config = resource.getPlayerConfig();

		loadSkin(getSkinType());

		setSound(SOUND_READY, "playready.wav", SoundType.SOUND, false);
		setSound(SOUND_PLAYSTOP, "playstop.wav", SoundType.SOUND, false);

		final String[] guideses = {"guide-pg.wav","guide-gr.wav","guide-gd.wav","guide-bd.wav","guide-pr.wav","guide-ms.wav"};
		for(int i = 0;i < 6;i++) {
			if(config.isGuideSE()) {
				Path[] paths = getSoundPaths(guideses[i], SoundType.SOUND);
				if(paths.length > 0) {
					main.getAudioProcessor().setAdditionalKeySound(i, true, paths[0].toString());
					main.getAudioProcessor().setAdditionalKeySound(i, false, paths[0].toString());
				}
			} else {
				main.getAudioProcessor().setAdditionalKeySound(i, true, null);
				main.getAudioProcessor().setAdditionalKeySound(i, false, null);
			}
		}

		final BMSPlayerInputProcessor input = main.getInputProcessor();
		if(autoplay.mode == BMSPlayerMode.Mode.PLAY || autoplay.mode == BMSPlayerMode.Mode.PRACTICE) {
			input.setPlayConfig(config.getPlayConfig(model.getMode()));
		} else if (autoplay.mode == BMSPlayerMode.Mode.AUTOPLAY || autoplay.mode == BMSPlayerMode.Mode.REPLAY) {
			input.setEnable(false);
		}
		lanerender = new LaneRenderer(this, model);
		for (CourseData.CourseDataConstraint i : resource.getConstraint()) {
			if (i == NO_SPEED) {
				control.setEnableControl(false);
				break;
			}
		}

		judge.init(model, resource);

		rhythm = new RhythmTimerProcessor(model,
				(getSkin() instanceof PlaySkin) ? ((PlaySkin) getSkin()).getNoteExpansionRate()[0] != 100 || ((PlaySkin) getSkin()).getNoteExpansionRate()[1] != 100 : false);

		bga = resource.getBGAManager();

		ScoreData score = main.getPlayDataAccessor().readScoreData(model, config.getLnmode());
		Logger.getGlobal().info("スコアデータベースからスコア取得");
		if (score == null) {
			score = new ScoreData();
		}

		if (autoplay.mode == BMSPlayerMode.Mode.PRACTICE) {
			getScoreDataProperty().setTargetScore(0, null, 0, null, model.getTotalNotes());
			practice.create(model);
			state = STATE_PRACTICE;
		} else {
			
			if(resource.getRivalScoreData() == null || resource.getCourseBMSModels() != null) {
				ScoreData rivalScore = TargetProperty.getTargetProperty(config.getTargetid()).getTarget(main);
				resource.setRivalScoreData(rivalScore);
			}
			getScoreDataProperty().setTargetScore(score.getExscore(), score.decodeGhost(), resource.getRivalScoreData() != null ? resource.getRivalScoreData().getExscore() : 0 , null, model.getTotalNotes());
		}
	}

	@Override
	public void render() {
		final PlaySkin skin = (PlaySkin) getSkin();
		if(skin == null) {
			main.changeState(MainStateType.MUSICSELECT);
			return;
		}
		final PlayerResource resource = main.getPlayerResource();
		final BMSPlayerMode autoplay = resource.getPlayMode();
		final BMSPlayerInputProcessor input = main.getInputProcessor();
		final PlayerConfig config = resource.getPlayerConfig();

		final long micronow = main.getNowMicroTime();

		if(micronow > skin.getInput() * 1000){
			main.switchTimer(TIMER_STARTINPUT, true);
		}
		if(input.startPressed() || input.isSelectPressed()){
			startpressedtime = micronow;
		}
		switch (state) {
		// 楽曲ロード
		case STATE_PRELOAD:
			if (resource.mediaLoadFinished() && micronow > (skin.getLoadstart() + skin.getLoadend()) * 1000
					&& micronow - startpressedtime > 1000000) {
				bga.prepare(this);
				final long mem = Runtime.getRuntime().freeMemory();
				System.gc();
				final long cmem = Runtime.getRuntime().freeMemory();
				Logger.getGlobal().info("current free memory : " + (cmem / (1024 * 1024)) + "MB , disposed : "
						+ ((cmem - mem) / (1024 * 1024)) + "MB");
				state = STATE_READY;
				main.setTimerOn(TIMER_READY);
				play(SOUND_READY);
				Logger.getGlobal().info("STATE_READYに移行");
			}
			if(!main.isTimerOn(TIMER_PM_CHARA_1P_NEUTRAL) || !main.isTimerOn(TIMER_PM_CHARA_2P_NEUTRAL)){
				main.setTimerOn(TIMER_PM_CHARA_1P_NEUTRAL);
				main.setTimerOn(TIMER_PM_CHARA_2P_NEUTRAL);
			}
			break;
		// practice mode
		case STATE_PRACTICE:
			if (main.isTimerOn(TIMER_PLAY)) {
				resource.reloadBMSFile();
				model = resource.getBMSModel();
				main.getPlayerResource().getSongdata().setBMSModel(model);
				lanerender.init(model);
				keyinput.setKeyBeamStop(false);
				main.setTimerOff(TIMER_PLAY);
				main.setTimerOff(TIMER_RHYTHM);
				main.setTimerOff(TIMER_FAILED);
				main.setTimerOff(TIMER_FADEOUT);
				main.setTimerOff(TIMER_ENDOFNOTE_1P);

				for(int i = TIMER_PM_CHARA_1P_NEUTRAL; i <= TIMER_PM_CHARA_DANCE; i++) main.setTimerOff(i);
			}
			if(!main.isTimerOn(TIMER_PM_CHARA_1P_NEUTRAL) || !main.isTimerOn(TIMER_PM_CHARA_2P_NEUTRAL)){
				main.setTimerOn(TIMER_PM_CHARA_1P_NEUTRAL);
				main.setTimerOn(TIMER_PM_CHARA_2P_NEUTRAL);
			}
			control.setEnableControl(false);
			control.setEnableCursor(false);
			practice.processInput(input);

			if (input.getKeyState(0) && resource.mediaLoadFinished() &&  micronow > (skin.getLoadstart() + skin.getLoadend()) * 1000
					&& micronow - startpressedtime > 1000000) {
				PracticeProperty property = practice.getPracticeProperty();
				control.setEnableControl(true);
				control.setEnableCursor(true);
				if (property.freq != 100) {
					BMSModelUtils.changeFrequency(model, property.freq / 100f);
					if (main.getConfig().getAudioConfig().getFreqOption() == FrequencyType.FREQUENCY) {
						main.getAudioProcessor().setGlobalPitch(property.freq / 100f);
					}
				}
				model.setTotal(property.total);
				PracticeModifier pm = new PracticeModifier(property.starttime * 100 / property.freq,
						property.endtime * 100 / property.freq);
				pm.modify(model);
				if (model.getMode().player == 2) {
					if (property.doubleop == 1) {
						new LaneShuffleModifier(Random.FLIP).modify(model);
					}
					PatternModifier.create(property.random2, PatternModifier.SIDE_2P, model.getMode(), config).modify(model);
				}
				PatternModifier.create(property.random, PatternModifier.SIDE_1P, model.getMode(), config).modify(model);

				gauge = practice.getGauge(model);
				model.setJudgerank(property.judgerank);
				lanerender.init(model);
				judge.init(model, resource);
				notes = 0;
				PMcharaLastnotes[0] = 0;
				PMcharaLastnotes[1] = 0;
				starttimeoffset = (property.starttime > 1000 ? property.starttime - 1000 : 0) * 100 / property.freq;
				playtime = (property.endtime + 1000) * 100 / property.freq + TIME_MARGIN;
				bga.prepare(this);
				state = STATE_READY;
				main.setTimerOn(TIMER_READY);
				play(SOUND_READY);
				Logger.getGlobal().info("STATE_READYに移行");
			}
			break;
		// practice終了
		case STATE_PRACTICE_FINISHED:
			if (main.getNowTime(TIMER_FADEOUT) > skin.getFadeout()) {
				input.setEnable(true);
				input.setStartTime(0);
				main.changeState(MainStateType.MUSICSELECT);
			}
			break;
			// GET READY
		case STATE_READY:
			if (main.getNowTime(TIMER_READY) > skin.getPlaystart()) {
				replayConfig = lanerender.getPlayConfig().clone();
				state = STATE_PLAY;
				main.setMicroTimer(TIMER_PLAY, micronow - starttimeoffset * 1000);
				main.setMicroTimer(TIMER_RHYTHM, micronow - starttimeoffset * 1000);

				input.setStartTime(micronow + main.getStartMicroTime() - starttimeoffset * 1000);
				input.setKeyLogMarginTime(resource.getMarginTime());
				keyinput.startJudge(model, replay != null ? replay.keylog : null, resource.getMarginTime());
				keysound.startBGPlay(model, starttimeoffset * 1000);
				Logger.getGlobal().info("STATE_PLAYに移行");
			}
			break;
		// プレイ
		case STATE_PLAY:
			final long deltatime = micronow - prevtime;
			final long deltaplay = deltatime * (100 - playspeed) / 100;
			PracticeProperty property = practice.getPracticeProperty();
			main.setMicroTimer(TIMER_PLAY, main.getMicroTimer(TIMER_PLAY) + deltaplay);

			rhythm.update(this, deltatime, lanerender.getNowBPM(), property.freq);

			final long ptime = main.getNowTime(TIMER_PLAY);
			float g = gauge.getValue();
			for(int i = 0; i < gaugelog.length; i++) {
				if (gaugelog[i].size <= ptime / 500) {
					gaugelog[i].add(gauge.getValue(i));
				}
			}
			main.switchTimer(TIMER_GAUGE_MAX_1P, gauge.getGauge().isMax());

			if(main.isTimerOn(TIMER_PM_CHARA_1P_NEUTRAL) && main.getNowTime(TIMER_PM_CHARA_1P_NEUTRAL) >= skin.getPMcharaTime(TIMER_PM_CHARA_1P_NEUTRAL - TIMER_PM_CHARA_1P_NEUTRAL) && main.getNowTime(TIMER_PM_CHARA_1P_NEUTRAL) % skin.getPMcharaTime(TIMER_PM_CHARA_1P_NEUTRAL - TIMER_PM_CHARA_1P_NEUTRAL) < 17) {
				if(PMcharaLastnotes[0] != notes && judge.getPMcharaJudge() > 0) {
					if(judge.getPMcharaJudge() == 1 || judge.getPMcharaJudge() == 2) {
						if(gauge.getGauge().isMax()) main.setTimerOn(TIMER_PM_CHARA_1P_FEVER);
						else main.setTimerOn(TIMER_PM_CHARA_1P_GREAT);
					} else if(judge.getPMcharaJudge() == 3) main.setTimerOn(TIMER_PM_CHARA_1P_GOOD);
					else main.setTimerOn(TIMER_PM_CHARA_1P_BAD);
					main.setTimerOff(TIMER_PM_CHARA_1P_NEUTRAL);
				}
			}
			if(main.isTimerOn(TIMER_PM_CHARA_2P_NEUTRAL) && main.getNowTime(TIMER_PM_CHARA_2P_NEUTRAL) >= skin.getPMcharaTime(TIMER_PM_CHARA_2P_NEUTRAL - TIMER_PM_CHARA_1P_NEUTRAL) && main.getNowTime(TIMER_PM_CHARA_2P_NEUTRAL) % skin.getPMcharaTime(TIMER_PM_CHARA_2P_NEUTRAL - TIMER_PM_CHARA_1P_NEUTRAL) < 17) {
				if(PMcharaLastnotes[1] != notes && judge.getPMcharaJudge() > 0) {
					if(judge.getPMcharaJudge() >= 1 && judge.getPMcharaJudge() <= 3) main.setTimerOn(TIMER_PM_CHARA_2P_BAD);
					else main.setTimerOn(TIMER_PM_CHARA_2P_GREAT);
					main.setTimerOff(TIMER_PM_CHARA_2P_NEUTRAL);
				}
			}
			for(int i = TIMER_PM_CHARA_1P_FEVER; i <= TIMER_PM_CHARA_2P_BAD; i++) {
				if(i != TIMER_PM_CHARA_2P_NEUTRAL && main.isTimerOn(i) && main.getNowTime(i) >= skin.getPMcharaTime(i - TIMER_PM_CHARA_1P_NEUTRAL)) {
					if(i <= TIMER_PM_CHARA_1P_BAD) {
						main.setTimerOn(TIMER_PM_CHARA_1P_NEUTRAL);
						PMcharaLastnotes[0] = notes;
					} else {
						main.setTimerOn(TIMER_PM_CHARA_2P_NEUTRAL);
						PMcharaLastnotes[1] = notes;
					}
					main.setTimerOff(i);
				}
			}
			main.switchTimer(TIMER_PM_CHARA_DANCE, true);

			// System.out.println("playing time : " + time);
			if (playtime < ptime) {
				state = STATE_FINISHED;
				main.setTimerOn(TIMER_MUSIC_END);
				for(int i = TIMER_PM_CHARA_1P_NEUTRAL; i <= TIMER_PM_CHARA_2P_BAD; i++) {
					main.setTimerOff(i);
				}
				main.setTimerOff(TIMER_PM_CHARA_DANCE);

				Logger.getGlobal().info("STATE_FINISHEDに移行");
			} else if(playtime - TIME_MARGIN < ptime) {
				main.switchTimer(TIMER_ENDOFNOTE_1P, true);
			}
			// stage failed判定
			if (config.getGaugeAutoShift() == PlayerConfig.GAUGEAUTOSHIFT_BESTCLEAR || config.getGaugeAutoShift() == PlayerConfig.GAUGEAUTOSHIFT_SELECT_TO_UNDER) {
				final int len = config.getGaugeAutoShift() == PlayerConfig.GAUGEAUTOSHIFT_BESTCLEAR
						? (gauge.getType() >= GrooveGauge.CLASS ? GrooveGauge.EXHARDCLASS + 1 : GrooveGauge.HAZARD + 1)
						: (gauge.isCourseGauge() ? Math.min(Math.max(config.getGauge(), GrooveGauge.NORMAL) + GrooveGauge.CLASS - GrooveGauge.NORMAL, GrooveGauge.EXHARDCLASS) + 1 : config.getGauge() + 1);
				int type = gauge.isCourseGauge() ? GrooveGauge.CLASS
						: gauge.getType() < config.getBottomShiftableGauge() ? gauge.getType() : config.getBottomShiftableGauge();
				for (int i = type; i < len; i++) {
					if (gauge.getGauge(i).getValue() > 0f && gauge.getGauge(i).isQualified()) {
						type = i;
					}
				}
				gauge.setType(type);
			} else if (g == 0) {
				switch(config.getGaugeAutoShift()) {
				case PlayerConfig.GAUGEAUTOSHIFT_NONE:
					// FAILED移行
					state = STATE_FAILED;
					main.setTimerOn(TIMER_FAILED);
					if (resource.mediaLoadFinished()) {
						main.getAudioProcessor().stop((Note) null);
					}
					play(SOUND_PLAYSTOP);
					Logger.getGlobal().info("STATE_FAILEDに移行");
					break;
				case PlayerConfig.GAUGEAUTOSHIFT_CONTINUE:
					break;
				case PlayerConfig.GAUGEAUTOSHIFT_SURVIVAL_TO_GROOVE:
					if(!gauge.isCourseGauge()) {
						// GAS処理
						gauge.setType(GrooveGauge.NORMAL);
					}
					break;
				}
			}
			break;
		// 閉店処理
		case STATE_FAILED:
			keyinput.stopJudge();
			keysound.stopBGPlay();
			if ((input.startPressed() ^ input.isSelectPressed()) && resource.getCourseBMSModels() == null
					&& autoplay.mode == BMSPlayerMode.Mode.PLAY) {
				if (!resource.isUpdateScore()) {
					resource.getReplayData().randomoptionseed = -1;
					Logger.getGlobal().info("アシストモード時は同じ譜面でリプレイできません");
				} else if (input.startPressed()) {
					resource.getReplayData().randomoptionseed = -1;
					Logger.getGlobal().info("オプションを変更せずリプレイ");
				} else {
					resource.setScoreData(createScoreData());
					Logger.getGlobal().info("同じ譜面でリプレイ");
				}
				saveConfig();
				resource.reloadBMSFile();
				main.changeState(MainStateType.PLAY);
			} else if (main.getNowTime(TIMER_FAILED) > skin.getClose()) {
				main.getAudioProcessor().setGlobalPitch(1f);
				if (resource.mediaLoadFinished()) {
					resource.getBGAManager().stop();
				}
				if (autoplay.mode == BMSPlayerMode.Mode.PLAY || autoplay.mode == BMSPlayerMode.Mode.REPLAY) {
					resource.setScoreData(createScoreData());
				}
				resource.setCombo(judge.getCourseCombo());
				resource.setMaxcombo(judge.getCourseMaxcombo());
				saveConfig();
				if (main.isTimerOn(TIMER_PLAY)) {
					for (long l = main.getTimer(TIMER_FAILED) - main.getTimer(TIMER_PLAY); l < playtime + 500; l += 500) {
						for(int i = 0; i < gaugelog.length; i++) {
							gaugelog[i].add(0f);
						}
					}
				}
				resource.setGauge(gaugelog);
				resource.setGrooveGauge(gauge);
				resource.setAssist(assist);
				input.setEnable(true);
				input.setStartTime(0);
				if (autoplay.mode == BMSPlayerMode.Mode.PRACTICE) {
					state = STATE_PRACTICE;
				} else if (resource.getScoreData() != null) {
					main.changeState(MainStateType.RESULT);
				} else {
					main.changeState(MainStateType.MUSICSELECT);
				}
			}
			break;
		// 完奏処理
		case STATE_FINISHED:
			keyinput.stopJudge();
			keysound.stopBGPlay();
			if (main.getNowTime(TIMER_MUSIC_END) > skin.getFinishMargin()) {
				main.switchTimer(TIMER_FADEOUT, true);
			}
			if (main.getNowTime(TIMER_FADEOUT) > skin.getFadeout()) {
				main.getAudioProcessor().setGlobalPitch(1f);
				resource.getBGAManager().stop();

				if (autoplay.mode == BMSPlayerMode.Mode.PLAY || autoplay.mode == BMSPlayerMode.Mode.REPLAY) {
					resource.setScoreData(createScoreData());
				}
				resource.setCombo(judge.getCourseCombo());
				resource.setMaxcombo(judge.getCourseMaxcombo());
				saveConfig();
				resource.setGauge(gaugelog);
				resource.setGrooveGauge(gauge);
				resource.setAssist(assist);
				input.setEnable(true);
				input.setStartTime(0);
				if (autoplay.mode == BMSPlayerMode.Mode.PRACTICE) {
					state = STATE_PRACTICE;
				} else if (resource.getScoreData() != null) {
					Logger.getGlobal().info("\"score\": " + resource.getScoreData());
					main.changeState(MainStateType.RESULT);
				} else {
					if (resource.mediaLoadFinished()) {
						main.getAudioProcessor().stop((Note) null);
					}
					if (resource.getCourseBMSModels() != null && resource.nextCourse()) {
						main.changeState(MainStateType.PLAY);
					} else if(resource.nextSong()){
						main.changeState(MainStateType.DECIDE);
					} else {
						main.changeState(MainStateType.MUSICSELECT);
					}
				}
			}
			break;
		}

		prevtime = micronow;
	}

	public void setPlaySpeed(int playspeed) {
		this.playspeed = playspeed;
		if (main.getConfig().getAudioConfig().getFastForward() == FrequencyType.FREQUENCY) {
			main.getAudioProcessor().setGlobalPitch(playspeed / 100f);
		}
	}

	public int getPlaySpeed() {
		return playspeed;
	}

	public void input() {
		control.input();
		keyinput.input();
	}

	public KeyInputProccessor getKeyinput() {
		return keyinput;
	}

	public int getState() {
		return state;
	}

	public LaneRenderer getLanerender() {
		return lanerender;
	}

	public LaneProperty getLaneProperty() {
		return laneProperty;
	}

	private void saveConfig() {
		final PlayerResource resource = main.getPlayerResource();
		for (CourseData.CourseDataConstraint c : resource.getConstraint()) {
			if (c == NO_SPEED) {
				return;
			}
		}
		PlayConfig pc = resource.getPlayerConfig().getPlayConfig(model.getMode()).getPlayconfig();
		if (pc.getFixhispeed() != PlayConfig.FIX_HISPEED_OFF) {
			pc.setDuration(lanerender.getGreenValue());
		} else {
			pc.setHispeed(lanerender.getHispeed());
		}
		pc.setLanecover(lanerender.getLanecover());
		pc.setLift(lanerender.getLiftRegion());
		pc.setHidden(lanerender.getHiddenCover());
	}

	public ScoreData createScoreData() {
		final PlayerResource resource = main.getPlayerResource();
		final PlayerConfig config = resource.getPlayerConfig();
		ScoreData score = judge.getScoreData();
		if (score.getEpg() + score.getLpg() + score.getEgr() + score.getLgr() + score.getEgd() + score.getLgd() + score.getEbd() + score.getLbd() == 0) {
			return null;
		}

		ClearType clear = ClearType.Failed;
		if (state != STATE_FAILED && gauge.isQualified()) {
			if (assist > 0) {
				if(resource.getCourseBMSModels() == null) clear = assist == 1 ? ClearType.LightAssistEasy : ClearType.AssistEasy;
			} else {
				if (notes == this.judge.getCombo()) {
					if (judge.getJudgeCount(2) == 0) {
						if (judge.getJudgeCount(1) == 0) {
							clear = ClearType.Max;
						} else {
							clear = ClearType.Perfect;
						}
					} else {
						clear = ClearType.FullCombo;
					}
				} else if (resource.getCourseBMSModels() == null) {
					clear = gauge.getClearType();
				}
			}
		}
		score.setClear(clear.id);
		score.setGauge(gauge.isTypeChanged() ? -1 : gauge.getType());
		score.setOption(playinfo.randomoption + (model.getMode().player == 2
				? (playinfo.randomoption2 * 10 + playinfo.doubleoption * 100) : 0));
		score.setSeed((model.getMode().player == 2 ? playinfo.randomoption2seed * 65536 * 256 : 0) + playinfo.randomoptionseed);
		score.encodeGhost(judge.getGhost());
		// リプレイデータ保存。スコア保存されない場合はリプレイ保存しない
		final ReplayData replay = resource.getReplayData();
		replay.player = main.getPlayerConfig().getName();
		replay.sha256 = model.getSHA256();
		replay.mode = config.getLnmode();
		replay.date = Calendar.getInstance().getTimeInMillis() / 1000;
		replay.keylog = main.getInputProcessor().getKeyInputLog();
//		replay.pattern = playinfo.pattern;
		replay.rand = playinfo.rand;
		replay.gauge = config.getGauge();
		replay.sevenToNinePattern = config.getSevenToNinePattern();
		replay.randomoption = playinfo.randomoption;
		replay.randomoptionseed = playinfo.randomoptionseed;
		replay.randomoption2 = playinfo.randomoption2;
		replay.randomoption2seed = playinfo.randomoption2seed;
		replay.doubleoption = playinfo.doubleoption;
		replay.config = replayConfig;

		score.setPassnotes(notes);
		score.setMinbp(score.getEbd() + score.getLbd() + score.getEpr() + score.getLpr() + score.getEms() + score.getLms() + resource.getSongdata().getNotes() - notes);
		
		long count = 0;
		long avgduration = 0;
		final int lanes = model.getMode().key;
		for (TimeLine tl : model.getAllTimeLines()) {
			for (int i = 0; i < lanes; i++) {
				Note n = tl.getNote(i);
				if (n != null && !(model.getLntype() == BMSModel.LNTYPE_LONGNOTE
						&& n instanceof LongNote && ((LongNote) n).isEnd())) {
					int state = n.getState();
					long time = n.getMicroPlayTime();
					avgduration += state >= 1 && state <= 4 ? Math.abs(time) : 1000000;
					count++;
//					System.out.println(time);
				}
			}
		}
		score.setTotalDuration(avgduration);
		score.setAvgjudge(avgduration / count);
//		System.out.println(avgduration + " / " + count + " = " + score.getAvgjudge());

		score.setDeviceType(main.getInputProcessor().getDeviceType());
		return score;
	}

	public void stopPlay() {
		if (state == STATE_PRACTICE) {
			practice.saveProperty();
			main.setTimerOn(TIMER_FADEOUT);
			state = STATE_PRACTICE_FINISHED;
			return;
		}
		if (state == STATE_PRELOAD || state == STATE_READY) {
			main.setTimerOn(TIMER_FADEOUT);
			state = STATE_PRACTICE_FINISHED;
			return;
		}
		if (main.isTimerOn(TIMER_FAILED) || main.isTimerOn(TIMER_FADEOUT)) {
			return;
		}
		if (state != STATE_FINISHED && notes == main.getPlayerResource().getSongdata().getNotes()) {
			state = STATE_FINISHED;
			main.setTimerOn(TIMER_FADEOUT);
			Logger.getGlobal().info("STATE_FINISHEDに移行");
		} else if(state == STATE_FINISHED && !main.isTimerOn(TIMER_FADEOUT)) {
			main.setTimerOn(TIMER_FADEOUT);
		} else if(state != STATE_FINISHED) {
			state = STATE_FAILED;
			main.setTimerOn(TIMER_FAILED);
			if (main.getPlayerResource().mediaLoadFinished()) {
				main.getAudioProcessor().stop((Note) null);
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

	public PracticeConfiguration getPracticeConfiguration() {
		return practice;
	}

	public int getJudgeCount(int judge, boolean fast) {
		return this.judge.getJudgeCount(judge, fast);
	}

	public JudgeManager getJudgeManager() {
		return judge;
	}

	public void update(int judge, long time) {
		notes = this.judge.getPastNotes();

		if (this.judge.getCombo() == 0) {
			bga.setMisslayerTme(time);
		}
		gauge.update(judge);
		// System.out.println("Now count : " + notes + " - " + totalnotes);

		//フルコン判定
		main.switchTimer(TIMER_FULLCOMBO_1P, notes == main.getPlayerResource().getSongdata().getNotes()
				&& notes == this.judge.getCombo());

		getScoreDataProperty().update(this.judge.getScoreData(), notes);

		main.switchTimer(TIMER_SCORE_A, getScoreDataProperty().qualifyRank(18));
		main.switchTimer(TIMER_SCORE_AA, getScoreDataProperty().qualifyRank(21));
		main.switchTimer(TIMER_SCORE_AAA, getScoreDataProperty().qualifyRank(24));
		main.switchTimer(TIMER_SCORE_BEST, this.judge.getScoreData().getExscore() >= getScoreDataProperty().getBestScore());
		main.switchTimer(TIMER_SCORE_TARGET, this.judge.getScoreData().getExscore() >= getScoreDataProperty().getRivalScore());
	}

	public GrooveGauge getGauge() {
		return gauge;
	}

	public boolean isNoteEnd() {
		return notes == main.getPlayerResource().getSongdata().getNotes();
	}

	public int getPastNotes() {
		return notes;
	}

	public void setPastNotes(int notes) {
		this.notes = notes;
	}

	public int getPlaytime() {
		return playtime;
	}

	public Mode getMode() {
		return model.getMode();
	}

	public long getNowQuarterNoteTime() {
		return rhythm != null ? rhythm.getNowQuarterNoteTime() : 0;
	}
}
