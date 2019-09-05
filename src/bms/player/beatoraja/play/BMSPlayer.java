package bms.player.beatoraja.play;

import static bms.player.beatoraja.CourseData.CourseDataConstraint.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.*;
import java.util.logging.Logger;

import com.badlogic.gdx.utils.FloatArray;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayerResource.PlayMode;
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

	// TODO GLAssistから起動すると楽曲ロード中に止まる

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

	private List<PatternModifyLog> pattern = new ArrayList<PatternModifyLog>();

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
	public static final int SOUND_GUIDE_SE_PG = 10;
	public static final int SOUND_GUIDE_SE_GR = 11;
	public static final int SOUND_GUIDE_SE_GD = 12;

	public BMSPlayer(MainController main, PlayerResource resource) {
		super(main);
		this.model = resource.getBMSModel();
		PlayMode autoplay = resource.getPlayMode();
		PlayerConfig config = resource.getPlayerConfig();

		if (autoplay.isReplayMode()) {
			if (resource.getCourseBMSModels() != null) {
				if (resource.getCourseReplay().length == 0) {
					ReplayData[] replays = main.getPlayDataAccessor().readReplayData(resource.getCourseBMSModels(),
							config.getLnmode(), autoplay.getReplayIndex(), resource.getConstraint());
					if (replays != null) {
						for (ReplayData rd : replays) {
							resource.addCourseReplay(rd);
						}
						replay = replays[0];
					} else {
						autoplay = PlayMode.PLAY;
						resource.setPlayMode(autoplay);
					}
				} else {
					for (int i = 0; i < resource.getCourseBMSModels().length; i++) {
						if (resource.getCourseBMSModels()[i].getMD5().equals(resource.getBMSModel().getMD5())) {
							replay = resource.getCourseReplay()[i];
						}
					}
				}
			} else {
				replay = main.getPlayDataAccessor().readReplayData(model, config.getLnmode(), autoplay.getReplayIndex());
				if (replay == null) {
					autoplay = PlayMode.PLAY;
					resource.setPlayMode(autoplay);
				}
			}
		}

		boolean isReplayPatternPlay = false;
		ReplayData HSReplay = null;
		if(replay != null && main.getInputProcessor().getKeystate()[1]) {
			//保存された譜面変更ログから譜面再現
			resource.setReplayData(replay);
			isReplayPatternPlay = true;
		} else if(replay != null && main.getInputProcessor().getKeystate()[2]) {
			//保存された譜面オプションログから譜面オプション再現
			config.setRandom(replay.randomoption);
			config.setRandom2(replay.randomoption2);
			config.setDoubleoption(replay.doubleoption);
			isReplayPatternPlay = true;
		}
		if(replay != null && main.getInputProcessor().getKeystate()[4]) {
			//保存されたHSオプションログからHSオプション再現
			HSReplay = replay;
			isReplayPatternPlay = true;
		}
		if(isReplayPatternPlay) {
			replay = null;
			autoplay = PlayMode.PLAY;
			resource.setPlayMode(autoplay);
		}

		if (model.getRandom() != null && model.getRandom().length > 0) {
			if (autoplay.isReplayMode()) {
				model = resource.loadBMSModel(replay.rand);
				// 暫定処置
				BMSModelUtils.setStartNoteTime(model, 1000);
				BMSPlayerRule.validate(model);
			} else if (resource.getReplayData().pattern != null) {
				model = resource.loadBMSModel(resource.getReplayData().rand);
				// 暫定処置
				BMSModelUtils.setStartNoteTime(model, 1000);
				BMSPlayerRule.validate(model);
			}
			Logger.getGlobal().info("譜面分岐 : " + Arrays.toString(model.getRandom()));
		}
		// 通常プレイの場合は最後のノーツ、オートプレイの場合はBG/BGAを含めた最後のノーツ
		playtime = (autoplay.isAutoPlayMode() ? model.getLastTime() : model.getLastNoteTime()) + TIME_MARGIN;

		boolean score = true;

		Logger.getGlobal().info("アシストオプション設定");
		if (resource.getCourseBMSModels() == null && autoplay == PlayMode.PLAY || autoplay.isAutoPlayMode()) {
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
				// LNがなければアシストなし
				LongNoteModifier mod = new LongNoteModifier();
				mod.modify(model);
				if (mod.longNoteExists()) {
					assist = 2;
					score = false;
				}
			}
			if (config.getJudgewindowrate() > 100) {
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
			if (config.getDoubleoption() >= 2 && (model.getMode() == Mode.BEAT_5K || model.getMode() == Mode.BEAT_7K || model.getMode() == Mode.KEYBOARD_24K)) {
				// SPでなければBATTLEは未適用
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
				LaneShuffleModifier mod = new LaneShuffleModifier(LaneShuffleModifier.BATTLE);
				mod.setModifyTarget(PatternModifier.SIDE_1P);
				mod.modify(model);
				if(config.getDoubleoption() == 3) {
					PatternModifier as = new AutoplayModifier(model.getMode().scratchKey);
					as.modify(model);
				}
				assist = 1;
				score = false;
			}
		}

		Logger.getGlobal().info("譜面オプション設定");
		if (replay != null) {
			if(replay.sevenToNinePattern > 0 && model.getMode() == Mode.BEAT_7K) {
				model.setMode(Mode.POPN_9K);
			}
			PatternModifier.modify(model, Arrays.asList(replay.pattern));
		} else if (resource.getReplayData().pattern != null) {
			if(resource.getReplayData().sevenToNinePattern > 0 && model.getMode() == Mode.BEAT_7K) {
				model.setMode(Mode.POPN_9K);
			}
			pattern = Arrays.asList(resource.getReplayData().pattern);
			PatternModifier.modify(model, pattern);
			Logger.getGlobal().info("譜面オプション : 保存された譜面変更ログから譜面再現");
		} else if (autoplay != PlayMode.PRACTICE) {
			Randomizer.setPlayerConfig(config);
			PatternModifier.setPlayerConfig(config);
			if(model.getMode().player == 2) {
				if (config.getDoubleoption() == 1) {
					LaneShuffleModifier mod = new LaneShuffleModifier(LaneShuffleModifier.FLIP);
					pattern = PatternModifier.merge(pattern,mod.modify(model));
				}
				pattern = PatternModifier.merge(pattern,
								PatternModifier.create(config.getRandom2(), PatternModifier.SIDE_2P, model.getMode())
										.modify(model));
				if (config.getRandom2() >= 6) {
					assist = (assist == 0) ? 1 : assist;
					score = false;
				}
				Logger.getGlobal().info("譜面オプション :  " + config.getRandom2());
			}

			// POPN_9KのSCR系RANDOMにPOPN_5Kは対応していないため、非SCR系RANDOMに変更
			if(model.getMode() == Mode.POPN_5K) {
				switch(Random.getRandom(config.getRandom())) {
				case ALL_SCR: config.setRandom(Random.IDENTITY.id);
					break;
				case RANDOM_EX: config.setRandom(Random.RANDOM.id);
					break;
				case S_RANDOM_EX: config.setRandom(Random.S_RANDOM.id);
					break;
				default:
					break;
				}
			}

			pattern = PatternModifier.merge(pattern,
					PatternModifier
							.create(config.getRandom(), PatternModifier.SIDE_1P, model.getMode())
							.modify(model));
			if (config.getRandom() >= 6 && !(config.getRandom() == 8 && model.getMode() == Mode.POPN_9K)) {
				assist = (assist == 0) ? 1 : assist;
				score = false;
			}
			Logger.getGlobal().info("譜面オプション :  " + config.getRandom());
			if (config.getSevenToNinePattern() >= 1 && model.getMode() == Mode.BEAT_7K) {
				//7to9
				model.setMode(Mode.POPN_9K);
				NoteShuffleModifier mod = new NoteShuffleModifier(NoteShuffleModifier.SEVEN_TO_NINE);
				mod.setModifyTarget(PatternModifier.SIDE_1P);
				pattern = mod.modify(model);
				if(config.getSevenToNineType() != 0) {
					assist = 1;
					score = false;
				}
			}
		}

		if(HSReplay != null && HSReplay.config != null) {
			//保存されたHSオプションログからHSオプション再現
			config.getPlayConfig(model.getMode()).setPlayconfig(HSReplay.config);
		}

		Logger.getGlobal().info("ゲージ設定");
		if(replay != null) {
			boolean[] keystate = main.getInputProcessor().getKeystate();
			for(int count = (keystate[5] ? 1 : 0) + (keystate[3] ? 2 : 0);count > 0; count--) {
				if (replay.gauge != GrooveGauge.HAZARD || replay.gauge != GrooveGauge.EXHARDCLASS) {
					replay.gauge++;
				}
			}
		}
		if(replay != null && main.getInputProcessor().getKeystate()[5]) {
		}
		// プレイゲージ、初期値設定
		gauge = GrooveGauge.create(model, replay != null ? replay.gauge : config.getGauge(), resource);
		// ゲージログ初期化
		gaugelog = new FloatArray[gauge.getGaugeTypeLength()];
		for(int i = 0; i < gaugelog.length; i++) {
			gaugelog[i] = new FloatArray(playtime / 500 + 2);
		}

		resource.setUpdateScore(score);
		final int difficulty = resource.getSongdata() != null ? resource.getSongdata().getDifficulty() : 0;
		resource.getSongdata().setBMSModel(model);
		resource.getSongdata().setDifficulty(difficulty);
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
		final PlayMode autoplay = resource.getPlayMode();
		laneProperty = new LaneProperty(model.getMode());
		keysound = new KeySoundProcessor(this);
		judge = new JudgeManager(this);
		control = new ControlInputProcessor(this, autoplay);
		keyinput = new KeyInputProccessor(this, laneProperty);
		Config conf = resource.getConfig();
		PlayerConfig config = resource.getPlayerConfig();

		loadSkin(getSkinType());

		setSound(SOUND_READY, "playready.wav", SoundType.SOUND, false);
		setSound(SOUND_PLAYSTOP, "playstop.wav", SoundType.SOUND, false);
		setSound(SOUND_GUIDE_SE_PG, "guide-pg.wav", SoundType.SOUND, false);
		setSound(SOUND_GUIDE_SE_GR, "guide-gr.wav", SoundType.SOUND, false);
		setSound(SOUND_GUIDE_SE_GD, "guide-gd.wav", SoundType.SOUND, false);

		final BMSPlayerInputProcessor input = main.getInputProcessor();
		if(autoplay == PlayMode.PLAY || autoplay == PlayMode.PRACTICE) {
			input.setPlayConfig(config.getPlayConfig(model.getMode()));
		} else if (autoplay.isAutoPlayMode() || autoplay.isReplayMode()) {
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

		IRScoreData score = main.getPlayDataAccessor().readScoreData(model, config.getLnmode());
		Logger.getGlobal().info("スコアデータベースからスコア取得");
		if (score == null) {
			score = new IRScoreData();
		}

		int rivalscore = TargetProperty.getAllTargetProperties()[config.getTarget()]
				.getTarget(main);
		resource.setRivalScoreData(rivalscore);

		if (autoplay == PlayMode.PRACTICE) {
			getScoreDataProperty().setTargetScore(0, null, 0, null, model.getTotalNotes());
			practice.create(model);
			state = STATE_PRACTICE;
		} else {
			getScoreDataProperty().setTargetScore(score.getExscore(), score.decodeGhost(), rivalscore, null, model.getTotalNotes());
		}
	}

	public static final int STATE_PRELOAD = 0;
	public static final int STATE_PRACTICE = 1;
	public static final int STATE_PRACTICE_FINISHED = 2;
	public static final int STATE_READY = 3;
	public static final int STATE_PLAY = 4;
	public static final int STATE_FAILED = 5;
	public static final int STATE_FINISHED = 6;

	private int state = STATE_PRELOAD;

	private long prevtime;

	private PracticeConfiguration practice = new PracticeConfiguration();
	private long starttimeoffset;

	private RhythmTimerProcessor rhythm;
	private long startpressedtime;

	@Override
	public void render() {
		final PlaySkin skin = (PlaySkin) getSkin();
		if(skin == null) {
			main.changeState(MainStateType.MUSICSELECT);
			return;
		}
		final PlayerResource resource = main.getPlayerResource();
		final PlayMode autoplay = resource.getPlayMode();
		final BMSPlayerInputProcessor input = main.getInputProcessor();
		final PlayerConfig config = resource.getPlayerConfig();

		final long now = main.getNowTime();
		final long micronow = main.getNowMicroTime();

		if(now > skin.getInput()){
			main.switchTimer(TIMER_STARTINPUT, true);
		}
		if(input.startPressed() || input.isSelectPressed()){
			startpressedtime = now;
		}
		switch (state) {
		// 楽曲ロード
		case STATE_PRELOAD:
			if (resource.mediaLoadFinished() && now > skin.getLoadstart() + skin.getLoadend()
					&& now - startpressedtime > 1000) {
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

			if (input.getKeystate()[0] && resource.mediaLoadFinished() && now > skin.getLoadstart() + skin.getLoadend()
					&& now - startpressedtime > 1000) {
				PracticeProperty property = practice.getPracticeProperty();
				Randomizer.setPlayerConfig(config);
				control.setEnableControl(true);
				control.setEnableCursor(true);
				if (property.freq != 100) {
					BMSModelUtils.changeFrequency(model, property.freq / 100f);
					if (main.getConfig().getAudioFreqOption() == Config.AUDIO_PLAY_FREQ) {
						main.getAudioProcessor().setGlobalPitch(property.freq / 100f);
					}
				}
				model.setTotal(property.total);
				PracticeModifier pm = new PracticeModifier(property.starttime * 100 / property.freq,
						property.endtime * 100 / property.freq);
				pm.modify(model);
				if (model.getMode().player == 2) {
					if (property.doubleop == 1) {
						new LaneShuffleModifier(LaneShuffleModifier.FLIP).modify(model);
					}
					PatternModifier.create(property.random2, PatternModifier.SIDE_2P, model.getMode()).modify(model);
				}
				PatternModifier.create(property.random, PatternModifier.SIDE_1P, model.getMode()).modify(model);

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

				input.setStartTime(now + main.getStartTime() - starttimeoffset);
				if (autoplay.isReplayMode()) {
					for(KeyInputLog keyinput : replay.keylog) {
						keyinput.time += resource.getMarginTime();
					}
				}
				keyinput.startJudge(model, replay != null ? replay.keylog : null);
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

			if (main.getNowTime(TIMER_FAILED) > skin.getClose()) {
				main.getAudioProcessor().setGlobalPitch(1f);
				if (resource.mediaLoadFinished()) {
					resource.getBGAManager().stop();
				}
				if (!autoplay.isAutoPlayMode() && autoplay != PlayMode.PRACTICE) {
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
				if (autoplay == PlayMode.PRACTICE) {
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
				if (!autoplay.isAutoPlayMode() && autoplay != PlayMode.PRACTICE) {
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
				if (autoplay == PlayMode.PRACTICE) {
					state = STATE_PRACTICE;
				} else if (resource.getScoreData() != null) {
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
		if (main.getConfig().getAudioFastForward() == Config.AUDIO_PLAY_FREQ) {
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

	public IRScoreData createScoreData() {
		final PlayerResource resource = main.getPlayerResource();
		final PlayerConfig config = resource.getPlayerConfig();
		IRScoreData score = judge.getScoreData();
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
		score.setOption(config.getRandom() + (model.getMode().player == 2
				? (config.getRandom2() * 10 + config.getDoubleoption() * 100) : 0));
		score.encodeGhost(judge.getGhost());
		// リプレイデータ保存。スコア保存されない場合はリプレイ保存しない
		final ReplayData replay = resource.getReplayData();
		replay.player = main.getPlayerConfig().getName();
		replay.sha256 = model.getSHA256();
		replay.mode = config.getLnmode();
		replay.date = Calendar.getInstance().getTimeInMillis() / 1000;
		replay.keylog = main.getInputProcessor().getKeyInputLog();
		for(KeyInputLog keyinput : replay.keylog) {
			keyinput.time -= resource.getMarginTime();
		}
		replay.pattern = pattern.toArray(new PatternModifyLog[pattern.size()]);
		replay.rand = model.getRandom();
		replay.gauge = config.getGauge();
		replay.sevenToNinePattern = config.getSevenToNinePattern();
		replay.randomoption = config.getRandom();
		replay.randomoption2 = config.getRandom2();
		replay.doubleoption = config.getDoubleoption();
		replay.config = replayConfig;

		score.setMinbp(score.getEbd() + score.getLbd() + score.getEpr() + score.getLpr() + score.getEms() + score.getLms() + resource.getSongdata().getNotes() - notes);
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
