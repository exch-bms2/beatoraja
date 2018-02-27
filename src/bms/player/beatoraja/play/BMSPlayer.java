package bms.player.beatoraja.play;

import java.util.*;
import static bms.player.beatoraja.play.GrooveGauge.*;
import java.util.logging.Logger;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.pattern.*;
import bms.player.beatoraja.play.PracticeConfiguration.PracticeProperty;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.song.SongData;

import com.badlogic.gdx.utils.*;

import static bms.player.beatoraja.CourseData.CourseDataConstraint.*;
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
	private LaneProperty laneProperty;
	private JudgeManager judge;

	private BGAProcessor bga;

	private GrooveGauge gauge;

	private int playtime;

	private PlayMode autoplay = PlayMode.PLAY;
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

	private final FloatArray[] gaugelog;

	private int playspeed = 100;

	/**
	 * 処理済ノート数
	 */
	private int notes;
	/**
	 * 閉店フラグ
	 */
	private boolean isFailed = false;
	/**
	 * PMS キャラ用 ニュートラルモーション開始時の処理済ノート数{1P,2P} (ニュートラルモーション一周時に変化がなければニュートラルモーションを継続するため)
	 */
	private int[] PMcharaLastnotes = {0, 0};

	static final int TIME_MARGIN = 5000;

	public static final int SOUND_READY = 0;
	public static final int SOUND_PLAYSTOP = 1;

	public BMSPlayer(MainController main, PlayerResource resource) {
		super(main);
		this.model = resource.getBMSModel();
		this.autoplay = resource.getPlayMode();
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
				}
			}
		}

		if(replay != null && main.getInputProcessor().getKeystate()[1]) {
			resource.setReplayData(replay);
			replay = null;
			autoplay = PlayMode.PLAY;
		}

		if (model.getRandom() != null && model.getRandom().length > 0) {
			if (autoplay.isReplayMode()) {
				model = resource.getGenerator().generate(replay.rand);
			} else if (resource.getReplayData().pattern != null) {
				model = resource.getGenerator().generate(resource.getReplayData().rand);
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
		BMSPlayerRule.setSevenToNine(false);
		if (replay != null) {
			if(replay.sevenToNinePattern > 0 && model.getMode() == Mode.BEAT_7K) {
				model.setMode(Mode.POPN_9K);
				BMSPlayerRule.setSevenToNine(true);
			}
			PatternModifier.modify(model, Arrays.asList(replay.pattern));
		} else if (resource.getReplayData().pattern != null) {
			if(resource.getReplayData().sevenToNinePattern > 0 && model.getMode() == Mode.BEAT_7K) {
				model.setMode(Mode.POPN_9K);
				BMSPlayerRule.setSevenToNine(true);
			}
			pattern = Arrays.asList(resource.getReplayData().pattern);
			PatternModifier.modify(model, pattern);
			Logger.getGlobal().info("譜面オプション : 保存された譜面変更ログから譜面再現");
		} else if (autoplay != PlayMode.PRACTICE) {
			PatternModifier.setPlayerConfig(config);
			if(model.getMode().player == 2) {
				if (config.getDoubleoption() == 1) {
					LaneShuffleModifier mod = new LaneShuffleModifier(LaneShuffleModifier.FLIP);
					pattern = PatternModifier.merge(pattern,mod.modify(model));
				}
				pattern = PatternModifier.merge(pattern,
								PatternModifier.create(config.getRandom2(), PatternModifier.SIDE_2P)
										.modify(model));
				if (config.getRandom2() >= 6) {
					assist = (assist == 0) ? 1 : assist;
					score = false;
				}
				Logger.getGlobal().info("譜面オプション :  " + config.getRandom2());
			}

			if(model.getMode().scratchKey.length == 0) {
				if (config.getRandom() == 7 && model.getMode() != Mode.POPN_9K) {
					config.setRandom(0);
				} else if (config.getRandom() == 8 && model.getMode() != Mode.POPN_9K) {
					config.setRandom(2);
				} else if (config.getRandom() == 9 && model.getMode() != Mode.POPN_9K) {
					config.setRandom(4);
				}
			}
			pattern = PatternModifier.merge(pattern,
					PatternModifier
							.create(config.getRandom(), PatternModifier.SIDE_1P)
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
				BMSPlayerRule.setSevenToNine(true);
				if(config.getSevenToNineType() != 0) {
					assist = 1;
					score = false;
				}
			}
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
		int coursetype = 0;
		GaugeProperty gauges = null;
		if(resource.getCourseBMSModels() != null){
			coursetype = 1;
			for (CourseData.CourseDataConstraint i : resource.getConstraint()) {
				switch(i) {
				case GAUGE_5KEYS:
					gauges = GaugeProperty.FIVEKEYS;
					break;
				case GAUGE_7KEYS:
					gauges = GaugeProperty.SEVENKEYS;
					break;
				case GAUGE_9KEYS:
					gauges = GaugeProperty.PMS;
					break;
				case GAUGE_24KEYS:
					gauges = GaugeProperty.KEYBOARD;
					break;
				case GAUGE_LR2:
					gauges = GaugeProperty.LR2;
					break;
				default:
					break;
				}
			}
		}
		gauge = GrooveGauge.create(model, replay != null ? replay.gauge : config.getGauge(), coursetype, gauges);
		FloatArray[] f = resource.getGauge();
		if (f != null) {
			for(int i = 0; i < f.length; i++) {
				gauge.setValue(i, f[i].get(f[i].size - 1));
			}
		}
		gaugelog = new FloatArray[gauge.getGaugeTypeLength()];
		for(int i = 0; i < gaugelog.length; i++) {
			gaugelog[i] = new FloatArray(playtime / 500 + 2);
		}

		resource.setUpdateScore(score);
		final int difficulty = resource.getSongdata() != null ? resource.getSongdata().getDifficulty() : 0;
		resource.setSongdata(new SongData(model, false));
		resource.getSongdata().setDifficulty(difficulty);
	}

	private SkinType getSkinType() {
		for(SkinType type : SkinType.values()) {
			if(type.getMode() == model.getMode()) {
				return type;
			}
		}
		return null;
	}

	public PlayConfig getPlayConfig(PlayerConfig config) {
		switch (model.getMode()) {
		case BEAT_7K:
		case BEAT_5K:
			return config.getMode7();
		case BEAT_14K:
		case BEAT_10K:
			return config.getMode14();
		case POPN_5K:
		case POPN_9K:
			return config.getMode9();
		case KEYBOARD_24K:
			return config.getMode24();
		case KEYBOARD_24K_DOUBLE:
			return config.getMode24double();
		default:
			return null;
		}
	}

	public void create() {
		final PlayerResource resource = main.getPlayerResource();
		laneProperty = new LaneProperty(model.getMode());
		judge = new JudgeManager(this);
		control = new ControlInputProcessor(this, autoplay);
		keyinput = new KeyInputProccessor(this, laneProperty);
		Config conf = resource.getConfig();
		PlayerConfig config = resource.getPlayerConfig();

		loadSkin(getSkinType());

		if(BMSPlayerRule.isSevenToNine()) {
			//7to9 ボーダーが丁度割り切れるゲージ粒数に変更
			Skin skin = getSkin();
			int setParts = skin.getGaugeParts();
			for(int type = 0; type < gauge.getGaugeTypeLength(); type++) {
				for(int i = skin.getGaugeParts(); i <= gauge.getMaxValue(type); i++) {
					if(gauge.getBorder(type) % (gauge.getMaxValue(type) / i) == 0) {
						setParts = Math.max(setParts, i);
						break;
					}
				}
			}
			skin.setGaugeParts(setParts);
		}

		setSound(SOUND_READY, "playready.wav", SoundType.SOUND, false);
		setSound(SOUND_PLAYSTOP, "playstop.wav", SoundType.SOUND, false);

		final BMSPlayerInputProcessor input = main.getInputProcessor();
		input.setMinimumInputDutration(conf.getInputduration());
		PlayConfig pc = getPlayConfig(config);
		if(autoplay == PlayMode.PLAY || autoplay == PlayMode.PRACTICE) {
			input.setPlayConfig(pc);
		}
		if (autoplay.isAutoPlayMode() || autoplay.isReplayMode()) {
			input.setEnable(false);
		}
		input.setKeyboardConfig(pc.getKeyboardConfig());
		input.setControllerConfig(pc.getController());
		input.setMidiConfig(pc.getMidiConfig());
		lanerender = new LaneRenderer(this, model);
		for (CourseData.CourseDataConstraint i : resource.getConstraint()) {
			if (i == NO_SPEED) {
				control.setEnableControl(false);
				break;
			}
		}

		judge.init(model, resource);

		final PlaySkin skin = (PlaySkin) getSkin();
		isNoteExpansion = (skin.getNoteExpansionRate()[0] != 100 || skin.getNoteExpansionRate()[1] != 100);
		LongArray sectiontimes = new LongArray();
		LongArray quarterNoteTimes = new LongArray();
		TimeLine[] timelines = model.getAllTimeLines();
		for (int i = 0; i < timelines.length; i++) {
			if(timelines[i].getSectionLine()) {
				sectiontimes.add(timelines[i].getMicroTime());

				if(isNoteExpansion) {
					quarterNoteTimes.add(timelines[i].getMicroTime());
					double sectionLineSection = timelines[i].getSection();
					double nextSectionLineSection = timelines[i].getSection() - sectionLineSection;
					boolean last = false;
					for(int j = i + 1; j < timelines.length; j++) {
						if(timelines[j].getSectionLine()) {
							nextSectionLineSection = timelines[j].getSection() - sectionLineSection;
							break;
						} else if(j == timelines.length - 1) {
							nextSectionLineSection = timelines[j].getSection() - sectionLineSection;
							last = true;
						}
					}
					for(double j = 0.25; j <= nextSectionLineSection; j += 0.25) {
						if((!last && j != nextSectionLineSection) || last) {
							int prevIndex;
							for(prevIndex = i; timelines[prevIndex].getSection() - sectionLineSection < j; prevIndex++) {}
							prevIndex--;
							quarterNoteTimes.add((long) (timelines[prevIndex].getMicroTime() + timelines[prevIndex].getMicroStop() + (j+sectionLineSection-timelines[prevIndex].getSection()) * 240000000 / timelines[prevIndex].getBPM()));
						}
					}
				}

			}
		}
		this.sectiontimes = sectiontimes.toArray();
		this.quarterNoteTimes = quarterNoteTimes.toArray();

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
			getScoreDataProperty().setTargetScore(0, 0, model.getTotalNotes());
			practice.create(model);
			state = STATE_PRACTICE;
		} else {
			getScoreDataProperty().setTargetScore(score.getExscore(), rivalscore, model.getTotalNotes());
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
	private long starttimeoffset;

	private long[] sectiontimes;
	private int sections = 0;
	private long rhythmtimer;
	private long startpressedtime;
	
	//4分のタイミングの時間 PMSのリズムに合わせたノート拡大用
	private long[] quarterNoteTimes;
	private int quarterNote = 0;
	private long nowQuarterNoteTime = 0;
	//ノートを拡大するかどうか
	boolean isNoteExpansion = false;

	@Override
	public void render() {
		final PlaySkin skin = (PlaySkin) getSkin();
		final PlayerResource resource = main.getPlayerResource();
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
				control.setEnableControl(true);
				control.setEnableCursor(true);
				if (property.freq != 100) {
					model.setFrequency(property.freq / 100f);
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
					PatternModifier.create(property.random2, PatternModifier.SIDE_2P).modify(model);
				}
				PatternModifier.create(property.random, PatternModifier.SIDE_1P).modify(model);

				gauge = practice.getGauge(model);
				model.setJudgerank(property.judgerank);
				lanerender.init(model);
				judge.init(model, resource);
				notes = 0;
				isFailed = false;
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
				main.changeState(MainController.STATE_SELECTMUSIC);
			}
			break;
			// GET READY
		case STATE_READY:
			if (main.getNowTime(TIMER_READY) > skin.getPlaystart()) {
				state = STATE_PLAY;
                main.setMicroTimer(TIMER_PLAY, micronow - starttimeoffset * 1000);
                main.setMicroTimer(TIMER_RHYTHM, micronow - starttimeoffset * 1000);

				input.setStartTime(now + main.getStartTime() - starttimeoffset);
				List<KeyInputLog> keylog = null;
				if (autoplay.isReplayMode()) {
					keylog = Arrays.asList(replay.keylog);
				}
				keyinput.startJudge(model, keylog);
				autoThread = new AutoplayThread(starttimeoffset * 1000);
				autoThread.start();
				Logger.getGlobal().info("STATE_PLAYに移行");
			}
			break;
		// プレイ
		case STATE_PLAY:
			final long deltatime = micronow - prevtime;
			final long deltaplay = deltatime * (100 - playspeed) / 100;
			PracticeProperty property = practice.getPracticeProperty();
			main.setMicroTimer(TIMER_PLAY, main.getMicroTimer(TIMER_PLAY) + deltaplay);
            rhythmtimer += deltatime * (100 - lanerender.getNowBPM() * playspeed / 60) / 100;
            main.setMicroTimer(TIMER_RHYTHM, rhythmtimer);

            if(sections < sectiontimes.length && (sectiontimes[sections] * (100 / property.freq)) <= main.getNowMicroTime(TIMER_PLAY)) {
				sections++;;
				main.setTimerOn(TIMER_RHYTHM);
				rhythmtimer = micronow;
			}
            if(isNoteExpansion) {
				if(quarterNote < quarterNoteTimes.length && (quarterNoteTimes[quarterNote] * (100 / property.freq)) <= main.getNowMicroTime(TIMER_PLAY)) {
					quarterNote++;
					nowQuarterNoteTime = now;
				} else if(quarterNote == quarterNoteTimes.length && ((nowQuarterNoteTime + 60000 / lanerender.getNowBPM()) * (100 / property.freq)) <= now)  {
					nowQuarterNoteTime = now;
				}
            }

            final long ptime = main.getNowTime(TIMER_PLAY);
			float g = gauge.getValue();
			for(int i = 0; i < gaugelog.length; i++) {
				if (gaugelog[i].size <= ptime / 500) {
					gaugelog[i].add(gauge.getValue(i));
				}
			}
			main.switchTimer(TIMER_GAUGE_MAX_1P, g == gauge.getMaxValue());

			if(main.isTimerOn(TIMER_PM_CHARA_1P_NEUTRAL) && main.getNowTime(TIMER_PM_CHARA_1P_NEUTRAL) >= skin.getPMcharaTime(TIMER_PM_CHARA_1P_NEUTRAL - TIMER_PM_CHARA_1P_NEUTRAL) && main.getNowTime(TIMER_PM_CHARA_1P_NEUTRAL) % skin.getPMcharaTime(TIMER_PM_CHARA_1P_NEUTRAL - TIMER_PM_CHARA_1P_NEUTRAL) < 17) {
				if(PMcharaLastnotes[0] != notes && judge.getPMcharaJudge() > 0) {
					if(judge.getPMcharaJudge() == 1 || judge.getPMcharaJudge() == 2) {
						if(g == gauge.getMaxValue()) main.setTimerOn(TIMER_PM_CHARA_1P_FEVER);
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
			if (playtime < ptime && !isFailed) {
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
			if (g == 0) {
				if(config.isContinueUntilEndOfSong() && notes != main.getPlayerResource().getSongdata().getNotes() && !isFailed && gauge.getType() != GrooveGauge.HAZARD) {
					if(gauge.getType() != GrooveGauge.CLASS) {
						gauge.downType();
						if(resource.getPlayMode() == PlayMode.PLAY) config.setGauge(config.getGauge() > 0 ? config.getGauge() - 1 :0);
					} else {
						isFailed = true;
					}
				} else if(!config.isContinueUntilEndOfSong() || gauge.getType() == GrooveGauge.HAZARD || (config.isContinueUntilEndOfSong() && notes == main.getPlayerResource().getSongdata().getNotes())) {
					state = STATE_FAILED;
					main.setTimerOn(TIMER_FAILED);
					if (resource.mediaLoadFinished()) {
						main.getAudioProcessor().stop((Note) null);
					}
					play(SOUND_PLAYSTOP);
					Logger.getGlobal().info("STATE_FAILEDに移行");
				}
			}
			break;
		// 閉店処理
		case STATE_FAILED:
			if (autoThread != null) {
				autoThread.stop = true;
			}
			keyinput.stopJudge();

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
			if (main.getNowTime(TIMER_MUSIC_END) > skin.getFinishMargin()) {
				main.switchTimer(TIMER_FADEOUT, true);
			}
			if (main.getNowTime(TIMER_FADEOUT) > skin.getFadeout()) {
				if(config.isContinueUntilEndOfSong()) {
					if(resource.getCourseBMSModels() == null) {
						int changedGaugeType = gauge.changeTypeOfClear(gauge.getType());
						if(resource.getPlayMode() == PlayMode.PLAY) config.setGauge(changedGaugeType);
					}
				}
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
					main.changeState(MainController.STATE_RESULT);
				} else {
					if (resource.mediaLoadFinished()) {
						main.getAudioProcessor().stop((Note) null);
					}
					if (resource.getCourseBMSModels() != null && resource.nextCourse()) {
						main.changeState(MainController.STATE_PLAYBMS);
					} else if(resource.nextSong()){
						main.changeState(MainController.STATE_DECIDE);
					} else {
						main.changeState(MainController.STATE_SELECTMUSIC);
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
		PlayConfig pc = getPlayConfig(resource.getPlayerConfig());
		if (lanerender.getFixHispeed() != PlayerConfig.FIX_HISPEED_OFF) {
			pc.setDuration(lanerender.getGreenValue());
		} else {
			pc.setHispeed(lanerender.getHispeed());
		}
		pc.setLanecover(lanerender.getLanecover());
		pc.setLift(lanerender.getLiftRegion());
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
		score.setGauge(GrooveGauge.getGaugeID(gauge));
		score.setOption(config.getRandom() + (model.getMode().player == 2
				? (config.getRandom2() * 10 + config.getDoubleoption() * 100) : 0));
		// リプレイデータ保存。スコア保存されない場合はリプレイ保存しない
		final ReplayData replay = resource.getReplayData();
		replay.player = main.getPlayerConfig().getName();
		replay.sha256 = model.getSHA256();
		replay.mode = config.getLnmode();
		replay.date = Calendar.getInstance().getTimeInMillis() / 1000;
		replay.keylog = main.getInputProcessor().getKeyInputLog();
		replay.pattern = pattern.toArray(new PatternModifyLog[pattern.size()]);
		replay.rand = model.getRandom();
		replay.gauge = config.getGauge();
		replay.sevenToNinePattern = config.getSevenToNinePattern();

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
		if (state != STATE_FINISHED && notes == main.getPlayerResource().getSongdata().getNotes() && !isFailed) {
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

	public void play(Note note, float volume, int pitchShift) {
		main.getAudioProcessor().play(note, volume, pitchShift);
	}

	public void stop(Note note) {
		main.getAudioProcessor().stop(note);
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

	/**
	 * BGレーン再生用スレッド
	 *
	 * @author exch
	 */
	class AutoplayThread extends Thread {

		private boolean stop = false;

		private final long starttime;

		public AutoplayThread(long starttime) {
			this.starttime = starttime;
		}

		@Override
		public void run() {
			Array<TimeLine> tls = new Array<TimeLine>();
			for(TimeLine tl : model.getAllTimeLines()) {
				if(tl.getBackGroundNotes().length > 0) {
					tls.add(tl);
				}
			}
			final TimeLine[] timelines = tls.toArray(TimeLine.class);
			final long lasttime = timelines[timelines.length - 1].getMicroTime() + BMSPlayer.TIME_MARGIN * 1000;
			final Config config = main.getPlayerResource().getConfig();
			int p = 0;
			for (long time = starttime; p < timelines.length && timelines[p].getMicroTime() < time; p++)
				;

			while (!stop) {
				final long time = main.getNowMicroTime(TIMER_PLAY);
				// BGレーン再生
				while (p < timelines.length && timelines[p].getMicroTime() <= time) {
					for (Note n : timelines[p].getBackGroundNotes()) {
						play(n, config.getBgvolume(), 0);
					}
					p++;
				}
				if (p < timelines.length) {
					try {
						final long sleeptime = timelines[p].getMicroTime() - time;
						if (sleeptime > 0) {
							sleep(sleeptime / 1000);
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
		switch (id) {
		case NUMBER_LANECOVER1:
			return (int) (lanerender.getLanecover() * 1000);
		case NUMBER_PLAYTIME_MINUTE:
			return (int) (((int) (main.isTimerOn(TIMER_PLAY) ? main.getNowTime(TIMER_PLAY) : 0))
					/ 60000);
		case NUMBER_PLAYTIME_SECOND:
			return (((int) (main.isTimerOn(TIMER_PLAY) ? main.getNowTime(TIMER_PLAY) : 0))
					/ 1000) % 60;
		case NUMBER_TIMELEFT_MINUTE:
			return (int) (Math.max((playtime
					- (int) (main.isTimerOn(TIMER_PLAY) ? main.getNowTime(TIMER_PLAY) : 0)
					+ 1000), 0) / 60000);
		case NUMBER_TIMELEFT_SECOND:
			return (Math.max((playtime
					- (int) (main.isTimerOn(TIMER_PLAY) ? main.getNowTime(TIMER_PLAY) : 0)
					+ 1000), 0) / 1000) % 60;
		case NUMBER_LOADING_PROGRESS:
			return (int) ((main.getAudioProcessor().getProgress() + bga.getProgress()) * 50);
		case NUMBER_GROOVEGAUGE:
			return (int) gauge.getValue();
		case NUMBER_GROOVEGAUGE_AFTERDOT:
			return  (gauge.getType() == GrooveGauge.HARD || gauge.getType() == GrooveGauge.EXHARD || gauge.getType() == GrooveGauge.HAZARD || gauge.getType() == GrooveGauge.CLASS || gauge.getType() == GrooveGauge.EXCLASS || gauge.getType() == GrooveGauge.EXHARDCLASS)
					&& gauge.getValue() > 0 && gauge.getValue() < 0.1 
					? 1 : ((int) (gauge.getValue() * 10)) % 10;
		case NUMBER_HISPEED_LR2:
			return (int) (lanerender.getHispeed() * 100);
		case NUMBER_HISPEED:
			return (int) lanerender.getHispeed();
		case NUMBER_HISPEED_AFTERDOT:
			return (int) (lanerender.getHispeed() * 100) % 100;
		case NUMBER_DURATION:
			return lanerender.getCurrentDuration();
		case NUMBER_DURATION_GREEN:
			return lanerender.getCurrentDuration() * 3 / 5;
		case NUMBER_NOWBPM:
			return (int) lanerender.getNowBPM();
		case NUMBER_MAXCOMBO:
		case NUMBER_MAXCOMBO2:
			return judge.getScoreData().getCombo();
		case VALUE_JUDGE_1P_DURATION:
			return (int) judge.getRecentJudgeTiming()[0];
		case VALUE_JUDGE_2P_DURATION:
			return (int) (judge.getRecentJudgeTiming().length > 1 ? judge.getRecentJudgeTiming()[1] : judge.getRecentJudgeTiming()[0]);
		case VALUE_JUDGE_3P_DURATION:
			return (int) (judge.getRecentJudgeTiming().length > 2 ? judge.getRecentJudgeTiming()[2] : judge.getRecentJudgeTiming()[0]);
		}
		return super.getNumberValue(id);
	}

	@Override
	public float getSliderValue(int id) {
		switch (id) {
		case SLIDER_MUSIC_PROGRESS:
			if (main.isTimerOn(TIMER_PLAY)) {
				return Math.min((float) main.getNowTime(TIMER_PLAY) / playtime , 1);
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
		case BARGRAPH_MUSIC_PROGRESS:
			if (main.isTimerOn(TIMER_PLAY)) {
				return Math.min((float) main.getNowTime(TIMER_PLAY) / playtime , 1);
			}
			return 0;
		case BARGRAPH_LOAD_PROGRESS:
			float value = (main.getAudioProcessor().getProgress() + bga.getProgress()) / 2;
			return value;
		}
		return super.getSliderValue(id);
	}

    public SkinOffset getOffsetValue(int id) {
    	// TODO 各クラスでoffsetを算出してこのメソッドは廃止したい
    	SkinOffset offset = main.getOffset(id);
        switch (id) {
		case OFFSET_SCRATCHANGLE_1P:
			offset.r = keyinput.getScratchState(0);
			break;
		case OFFSET_SCRATCHANGLE_2P:
			offset.r = keyinput.getScratchState(1);
			break;
        case OFFSET_LIFT:
        case OFFSET_LIFT_OBSOLETE:
            if (lanerender.isEnableLift()) {
                final PlaySkin skin = (PlaySkin) getSkin();
                offset.y = lanerender.getLiftRegion() * skin.getLaneRegion()[0].height;
            } else {
            	offset.y = 0;
            }
			break;
        case OFFSET_LANECOVER:
        case OFFSET_LANECOVER_OBSOLETE:
            if (lanerender.isEnableLanecover()) {
                final PlaySkin skin = (PlaySkin) getSkin();
                if (lanerender.isEnableLift()) {
                    offset.y =  -(1 - lanerender.getLiftRegion()) * lanerender.getLanecover()
                            * skin.getLaneRegion()[0].height;
                } else {
                    offset.y =  -lanerender.getLanecover() * skin.getLaneRegion()[0].height;
                }
            } else {
            	offset.y = 0;
            }
			break;
        }
        return offset;
    }

    public boolean getBooleanValue(int id) {
		switch (id) {
		case OPTION_GAUGE_GROOVE:
			return gauge.getType() <= 2;
		case OPTION_GAUGE_HARD:
			return gauge.getType() >= 3;
		case OPTION_GAUGE_EX:
			final int type = gauge.getType();
			return type == 0 || type == 1 || type == 4 || type == 5 || type == 7 || type == 8;
		case OPTION_AUTOPLAYON:
			return autoplay == PlayMode.AUTOPLAY;
		case OPTION_AUTOPLAYOFF:
			return autoplay != PlayMode.AUTOPLAY;
		case OPTION_REPLAY_OFF:
			return autoplay == PlayMode.PLAY || autoplay == PlayMode.PRACTICE;
		case OPTION_REPLAY_PLAYING:
			return autoplay.isReplayMode();
		case OPTION_BGAON:
			return main.getPlayerResource().getConfig().getBga() == Config.BGA_ON
					|| (main.getPlayerResource().getConfig().getBga() == Config.BGA_AUTO
							&& (autoplay == PlayMode.AUTOPLAY || autoplay.isReplayMode()));
		case OPTION_BGAOFF:
			return main.getPlayerResource().getConfig().getBga() == Config.BGA_OFF
					|| (main.getPlayerResource().getConfig().getBga() == Config.BGA_AUTO
							&& (autoplay == PlayMode.PLAY || autoplay == PlayMode.PRACTICE));
		case OPTION_NOW_LOADING:
			return state == STATE_PRELOAD;
		case OPTION_LOADED:
			return state != STATE_PRELOAD;
		case OPTION_LANECOVER1_CHANGING:
			return main.getInputProcessor().startPressed() ||
					main.getInputProcessor().isSelectPressed();
		case OPTION_1P_0_9:
			return gauge.getValue() >= 0 && gauge.getValue() < 0.1 * gauge.getMaxValue();
		case OPTION_1P_10_19:
			return gauge.getValue() >= 0.1 * gauge.getMaxValue() && gauge.getValue() < 0.2 * gauge.getMaxValue();
		case OPTION_1P_20_29:
			return gauge.getValue() >= 0.2 * gauge.getMaxValue() && gauge.getValue() < 0.3 * gauge.getMaxValue();
		case OPTION_1P_30_39:
			return gauge.getValue() >= 0.3 * gauge.getMaxValue() && gauge.getValue() < 0.4 * gauge.getMaxValue();
		case OPTION_1P_40_49:
			return gauge.getValue() >= 0.4 * gauge.getMaxValue() && gauge.getValue() < 0.5 * gauge.getMaxValue();
		case OPTION_1P_50_59:
			return gauge.getValue() >= 0.5 * gauge.getMaxValue() && gauge.getValue() < 0.6 * gauge.getMaxValue();
		case OPTION_1P_60_69:
			return gauge.getValue() >= 0.6 * gauge.getMaxValue() && gauge.getValue() < 0.7 * gauge.getMaxValue();
		case OPTION_1P_70_79:
			return gauge.getValue() >= 0.7 * gauge.getMaxValue() && gauge.getValue() < 0.8 * gauge.getMaxValue();
		case OPTION_1P_80_89:
			return gauge.getValue() >= 0.8 * gauge.getMaxValue() && gauge.getValue() < 0.9 * gauge.getMaxValue();
		case OPTION_1P_90_99:
			return gauge.getValue() >= 0.9 * gauge.getMaxValue() && gauge.getValue() < gauge.getMaxValue();
		case OPTION_1P_100:
			return gauge.getValue() == gauge.getMaxValue();
		case OPTION_1P_BORDER_OR_MORE:
			return gauge.getValue() >= gauge.getBorder();
		case OPTION_1P_PERFECT:
			return judge.getNowJudge()[0] == 1;
		case OPTION_1P_EARLY:
			return judge.getNowJudge()[0] > 1 && judge.getRecentJudgeTiming()[0] > 0;
		case OPTION_1P_LATE:
			return judge.getNowJudge()[0] > 1 && judge.getRecentJudgeTiming()[0] < 0;
		case OPTION_2P_PERFECT:
			return judge.getNowJudge().length > 1 && judge.getNowJudge()[1] == 1;
		case OPTION_2P_EARLY:
			return judge.getNowJudge().length > 1 && judge.getNowJudge()[1] > 1
					&& judge.getRecentJudgeTiming()[1] > 0;
		case OPTION_2P_LATE:
			return judge.getNowJudge().length > 1 && judge.getNowJudge()[1] > 1
					&& judge.getRecentJudgeTiming()[1] < 0;
		case OPTION_3P_PERFECT:
			return judge.getNowJudge().length > 2 && judge.getNowJudge()[2] == 1;
		case OPTION_3P_EARLY:
			return judge.getNowJudge().length > 2 && judge.getNowJudge()[2] > 1
					&& judge.getRecentJudgeTiming()[2] > 0;
		case OPTION_3P_LATE:
			return judge.getNowJudge().length > 2 && judge.getNowJudge()[2] > 1
					&& judge.getRecentJudgeTiming()[2] < 0;
		case OPTION_PERFECT_EXIST:
			return judge.getJudgeCount(0) > 0;
		case OPTION_GREAT_EXIST:
			return judge.getJudgeCount(1) > 0;
		case OPTION_GOOD_EXIST:
			return judge.getJudgeCount(2) > 0;
		case OPTION_BAD_EXIST:
			return judge.getJudgeCount(3) > 0;
		case OPTION_POOR_EXIST:
			return judge.getJudgeCount(4) > 0;
		case OPTION_MISS_EXIST:
			return judge.getJudgeCount(5) > 0;
		}
		return super.getBooleanValue(id);
	}

	public int getImageIndex(int id) {
		if (SkinPropertyMapper.isKeyJudgeValueId(id)) {
			return judge.getJudge(id);
		}
		return super.getImageIndex(id);
	}

	public boolean isNoteEnd() {
		return notes == main.getPlayerResource().getSongdata().getNotes();
	}

	public void setPastNotes(int notes) {
		this.notes = notes;
	}

	public Mode getMode() {
		return model.getMode();
	}

	public long getNowQuarterNoteTime() {
		return nowQuarterNoteTime;
	}
}
