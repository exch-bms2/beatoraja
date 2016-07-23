package bms.player.beatoraja.play;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import org.lwjgl.opengl.GL11;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.gauge.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.pattern.*;
import bms.player.beatoraja.play.audio.AudioProcessor;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.skin.LR2PlaySkinLoader;
import bms.player.beatoraja.skin.SkinNumber;
import bms.player.beatoraja.skin.SkinImage;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

/**
 * BMSプレイヤー本体
 * 
 * @author exch
 */
public class BMSPlayer extends MainState {

	// TODO GLAssistから起動すると楽曲ロード中に止まる

	private BitmapFont titlefont;
	private BitmapFont judgefont;
	private BitmapFont systemfont;
	private BMSModel model;
	private int totalnotes;
	private int minbpm;
	private int maxbpm;

	private BMSPlayerInputProcessor input;
	private LaneRenderer lanerender;
	private ScoreGraphRenderer graphrender;
	private JudgeManager judge;
	private AudioProcessor audio;

	private BGAProcessor bga;

	private PlayerResource resource;

	private PlaySkin skin;

	private GrooveGauge gauge;

	private long readytime;
	/**
	 * プレイ開始時間。0の場合はプレイ開始前
	 */
	private long starttime;
	/**
	 * プレイ終了時間。0の場合はプレイ終了前
	 */
	private long finishtime;

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

	private MainController main;

	private List<Float> gaugelog = new ArrayList<Float>();
	
	private int playspeed = 100;
	
	/**
	 * 処理済ノート数
	 */
	private int notes;

	public BMSPlayer(MainController main, PlayerResource resource) {
		this.main = main;
		this.resource = resource;
		this.model = resource.getBMSModel();
		this.autoplay = resource.getAutoplay();
		Config config = resource.getConfig();
		
		if (config.getLR2PlaySkinPath() != null) {
			try {
				skin = new LR2PlaySkinLoader().loadPlaySkin(new File(config.getLR2PlaySkinPath()));
			} catch (IOException e) {
				e.printStackTrace();
				skin = new PlaySkin(model.getUseKeys(), main.RESOLUTION[resource.getConfig().getResolution()]);
			}
		} else {
			skin = new PlaySkin(model.getUseKeys(), main.RESOLUTION[resource.getConfig().getResolution()]);
		}
		this.setSkin(skin);

		if (autoplay >= 2) {
			if(resource.getCourseBMSModels() != null) {
				if(resource.getCourseReplay().length == 0) {
					ReplayData[] replays = main.getPlayDataAccessor().readReplayData(resource.getCourseBMSModels(), config.getLnmode(), autoplay - 2, resource.getConstraint());
					if(replays != null) {
						for(ReplayData rd : replays) {
							resource.addCourseReplay(rd);
						}
						replay = replays[0];
					} else {
						autoplay = 0;
					}
				} else {
					for(int i = 0;i < resource.getCourseBMSModels().length;i++) {
						if(resource.getCourseBMSModels()[i].getHash().equals(resource.getBMSModel().getHash())) {
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

		if(model.getRandom() > 1) {
			if(autoplay >= 2) {
				model.setSelectedIndexOfTimeLines(replay.random);								
			} else if(resource.getReplayData().pattern != null) {
				model.setSelectedIndexOfTimeLines(resource.getReplayData().random);												
			} else {
				model.setSelectedIndexOfTimeLines((int) (Math.random() * model.getRandom()) + 1);								
			}
			Logger.getGlobal().info("譜面分岐 : " + model.getSelectedIndexOfTimeLines());
		}
		minbpm = (int) model.getMinBPM();
		maxbpm = (int) model.getMaxBPM();
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
		if(exjudge) {
			judge.setExpandJudge(JudgeManager.EXPAND_JUDGE);
		}
		totalnotes = model.getTotalNotes();

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

		List<Float> f = resource.getGauge();
		if (f != null) {
			gauge.setValue(f.get(f.size() - 1));
			judge.setCourseCombo(resource.getCombo());
			judge.setCourseMaxcombo(resource.getMaxcombo());
		}
		Logger.getGlobal().info("ゲージ設定完了");

	}

	private final PatternModifier[] random = { null, new LaneShuffleModifier(LaneShuffleModifier.MIRROR),
			new LaneShuffleModifier(LaneShuffleModifier.RANDOM), new LaneShuffleModifier(LaneShuffleModifier.R_RANDOM),
			new NoteShuffleModifier(NoteShuffleModifier.S_RANDOM), new NoteShuffleModifier(NoteShuffleModifier.SPIRAL),
			new NoteShuffleModifier(NoteShuffleModifier.H_RANDOM),
			new NoteShuffleModifier(NoteShuffleModifier.ALL_SCR),
			new LaneShuffleModifier(LaneShuffleModifier.RANDOM_EX),
			new NoteShuffleModifier(NoteShuffleModifier.S_RANDOM_EX) };

	public void create() {
		final ShapeRenderer shape = main.getShapeRenderer();
		final SpriteBatch sprite = main.getSpriteBatch();

		Config config = resource.getConfig();
		Logger.getGlobal().info("create");

		input = main.getInputProcessor();
		input.setEnableKeyInput(autoplay == 0);
		PlayConfig pc = (model.getUseKeys() == 5 || model.getUseKeys() == 7 ? config.getMode7() : (model
				.getUseKeys() == 10 || model.getUseKeys() == 14 ? config.getMode14() : config.getMode9()));
		input.setKeyassign(pc.getKeyassign());
		input.setControllerassign(pc.getControllerassign());
		lanerender = new LaneRenderer(this, sprite, shape, skin, resource, model, resource.getConstraint());
		Logger.getGlobal().info("描画クラス準備");

		Logger.getGlobal().info("hash");
		IRScoreData score = main.getPlayDataAccessor().readScoreData(model, config.getLnmode());
		Logger.getGlobal().info("スコアデータベースからスコア取得");
		if (score == null) {
			score = new IRScoreData();
		}
		graphrender = new ScoreGraphRenderer(model, score.getExscore(), score.getExscore());
		Logger.getGlobal().info("スコアグラフ描画クラス準備");

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		parameter.characters += model.getFullTitle();
		titlefont = generator.generateFont(parameter);
		parameter.size = 18;
		systemfont = generator.generateFont(parameter);
		judgefont = generator.generateFont(parameter);
		generator.dispose();

		audio = resource.getAudioProcessor();
		bga = resource.getBGAManager();
	}

	@Override
	public void resize(int w, int h) {
		System.out.println("resize" + w + "," + h);
	}

	private static final int STATE_PRELOAD = 0;
	private static final int STATE_READY = 1;
	private static final int STATE_PLAY = 2;
	private static final int STATE_FAILED = 3;
	private static final int STATE_FINISHED = 4;

	private int state = STATE_PRELOAD;

	private long prevtime;
	
	@Override
	public void render() {
		final ShapeRenderer shape = main.getShapeRenderer();
		final SpriteBatch sprite = main.getSpriteBatch();

		final float w = main.RESOLUTION[resource.getConfig().getResolution()].width;
		final float h = main.RESOLUTION[resource.getConfig().getResolution()].height;

		final long nowtime = System.currentTimeMillis() ;
		final int time = (int) (nowtime - starttime);
		switch (state) {
		// 楽曲ロード
		case STATE_PRELOAD:
			renderMain(0);
			shape.begin(ShapeType.Filled);
			shape.setColor(Color.YELLOW);
			shape.rect(
					skin.getLaneGroupRegion()[0].x,
					skin.getLaneGroupRegion()[0].y + 200,
					(audio.getProgress() + bga.getProgress())
							* (skin.getLaneGroupRegion()[0].width) / 2, 4);
			shape.end();

			if (resource.mediaLoadFinished() && !input.startPressed()) {
				bga.prepare();
				state = STATE_READY;
				readytime = System.currentTimeMillis();
				Logger.getGlobal().info("STATE_READYに移行");
			}
			break;
		// GET READY
		case STATE_READY:
			renderMain(0);
			sprite.begin();
			systemfont.setColor(Color.WHITE);
			systemfont.draw(sprite, "GET READY", skin.getLaneGroupRegion()[0].x + skin.getLaneGroupRegion()[0].width / 2 - 35,
					skin.getLaneGroupRegion()[0].y + 200);
			sprite.end();
			final long rt = System.currentTimeMillis() - readytime;
			if (rt > 1000) {
				state = STATE_PLAY;
				starttime = System.currentTimeMillis();
				input.setStartTime(starttime);
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
			starttime += (nowtime - prevtime) * (100 - playspeed) / 100;
			final float g = gauge.getValue();
			if (gaugelog.size() <= time / 500) {
				gaugelog.add(g);
			}
			// System.out.println("playing time : " + time);
			if (starttime != 0 && playtime < time) {
				state = STATE_FINISHED;
				finishtime = System.currentTimeMillis();
				Logger.getGlobal().info("STATE_FINISHEDに移行");
			}
			if (g == 0) {
				state = STATE_FAILED;
				finishtime = System.currentTimeMillis();
				Logger.getGlobal().info("STATE_FAILEDに移行");
			}
			renderMain(time);
			
			break;
		// 閉店処理
		case STATE_FAILED:
			if (autoThread != null) {
				autoThread.stop = true;
			}
			if (keyinput != null) {
				keyinput.stop = true;
			}
			resource.getAudioProcessor().stop(-1);
			renderMain(starttime != 0 ? time : 0);

			Gdx.gl.glEnable(GL11.GL_BLEND);
			Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			shape.begin(ShapeType.Filled);
			long l = System.currentTimeMillis() - finishtime;
			shape.setColor(0, 0, 0, ((float) l) / 1000f);
			float height = h / 2 * l / 1000;
			shape.rect(0, h - height * 2, w, height * 2);
			shape.rect(0, 0, w, height * 2);
			shape.setColor(0, 0, 0, 1);
			shape.rect(0, h - height, w, height);
			shape.rect(0, 0, w, height);
			shape.end();
			Gdx.gl.glDisable(GL11.GL_BLEND);
			if (l > 1000) {
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
				main.changeState(MainController.STATE_RESULT);
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
			Gdx.gl.glEnable(GL11.GL_BLEND);
			Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			shape.begin(ShapeType.Filled);
			long l2 = System.currentTimeMillis() - finishtime;
			shape.setColor(1, 1, 1, ((float) l2) / 1000f);
			shape.rect(0, 0, w, h);
			shape.end();
			Gdx.gl.glDisable(GL11.GL_BLEND);
			if (l2 > 1000) {
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
				main.changeState(MainController.STATE_RESULT);
			}
			break;
		}
		if (input.isExitPressed()) {
			stopPlay();
		}
		if(autoplay !=0 && input.getNumberState()[1]) {
			playspeed = 25;
		} else if(autoplay !=0 && input.getNumberState()[2]) {
			playspeed = 50;
		} else if(autoplay !=0 && input.getNumberState()[3]) {
			playspeed = 200;
		} else if(autoplay !=0 && input.getNumberState()[4]) {
			playspeed = 300;
		} else {
			playspeed = 100;			
		}
		
		prevtime = nowtime;
	}

	private void saveConfig() {
		for(int c : resource.getConstraint()) {
			if(c == TableData.NO_HISPEED) {
				return;
			}
		}
		Config config = resource.getConfig();
		if (lanerender.getFixHispeed() != Config.FIX_HISPEED_OFF) {
			config.setGreenvalue(lanerender.getGreenValue());
		} else {
			config.setHispeed(lanerender.getHispeed());
		}
		config.setLanecover(lanerender.getLaneCoverRegion());
		config.setLift(lanerender.getLiftRegion());
	}

	public IRScoreData createScoreData() {
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
				if (judge.getJudgeCount(3) + judge.getJudgeCount(4) == 0 && (model.getUseKeys() != 9 || judge.getJudgeCount(5) == 0)) {
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
		resource.getReplayData().pattern = pattern.toArray(new PatternModifyLog[0]);
		resource.getReplayData().random = model.getSelectedIndexOfTimeLines();
		resource.getReplayData().gauge = resource.getConfig().getGauge();

		score.setEpg(judge.getJudgeCount(0,true));
		score.setLpg(judge.getJudgeCount(0,false));
		score.setEgr(judge.getJudgeCount(1,true));
		score.setLgr(judge.getJudgeCount(1,false));
		score.setEgd(judge.getJudgeCount(2,true));
		score.setLgd(judge.getJudgeCount(2,false));
		score.setEbd(judge.getJudgeCount(3,true));
		score.setLbd(judge.getJudgeCount(3,false));
		score.setEpr(judge.getJudgeCount(4,true));
		score.setLpr(judge.getJudgeCount(4,false));
		score.setEms(judge.getJudgeCount(5,true));
		score.setLms(judge.getJudgeCount(5,false));

		final int misscount = bad + poor + miss + totalnotes - notes;
		score.setMinbp(misscount);
		return score;
	}

	public void stopPlay() {
		if (finishtime != 0) {
			return;
		}
		if (notes == totalnotes) {
			state = STATE_FINISHED;
			Logger.getGlobal().info("STATE_FINISHEDに移行");
		} else {
			state = STATE_FAILED;
			Logger.getGlobal().info("STATE_FAILEDに移行");
		}
		finishtime = System.currentTimeMillis();
	}

	private void renderMain(int time) {
		final ShapeRenderer shape = main.getShapeRenderer();
		final SpriteBatch sprite = main.getSpriteBatch();

		// グラフ描画
		graphrender.drawGraph(skin, sprite, systemfont, shape, this.judge);

		// プログレス描画
		Rectangle progress = skin.getProgressRegion();
		shape.begin(ShapeType.Line);
		shape.setColor(Color.WHITE);
		shape.rect(progress.x, progress.y, progress.width, progress.height);
		shape.end();
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.BLACK);
		shape.rect(progress.x + 1, progress.y + 1, progress.width - 2, progress.height - 2);

		shape.setColor(notes == totalnotes ? Color.BLUE : Color.ORANGE);
		shape.rect(progress.x + 1, progress.y + 1 + progress.height * (1.0f - (float) time / playtime),
				progress.width - 2, 20);
		shape.end();
		// レーン描画
		lanerender.drawLane(systemfont, time);

		// BGA再生
		Rectangle[] region = skin.getBGAregion();
		for(Rectangle r : region) {
			shape.begin(ShapeType.Line);
			shape.setColor(Color.GRAY);
			shape.rect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);
			shape.end();
			shape.begin(ShapeType.Filled);
			shape.setColor(Color.BLACK);
			shape.rect(r.x, r.y, r.width, r.height);
			shape.end();			
		}
		
		bga.drawBGA(sprite, region, state == STATE_PRELOAD || state == STATE_READY ? -1 : time);

		// ゲージ描画
		Rectangle gr = skin.getGaugeRegion();
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.valueOf("#001000"));
		shape.rect(gr.x, gr.y, gr.width, gr.height + 24);
		shape.end();
		gauge.draw(skin, sprite, gr.x, gr.y, gr.width, gr.height);

		// ジャッジカウント描画
		Rectangle judge = skin.getJudgecountregion();
		Gdx.gl.glEnable(GL11.GL_BLEND);
		Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		shape.begin(ShapeType.Filled);
		shape.setColor(0, 0, 0, 0.5f);
		shape.rect(judge.x, judge.y, judge.width, judge.height);
		shape.end();
		Gdx.gl.glDisable(GL11.GL_BLEND);
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
		titlefont.dispose();
		judgefont.dispose();
		systemfont.dispose();
		Logger.getGlobal().info("システム描画のリソース解放");
		audio.dispose();
		Logger.getGlobal().info("音源のリソース解放");
		bga.dispose();
		Logger.getGlobal().info("BGAのリソース解放");
	}

	public void play(Note note) {
		audio.play(note.getWav(), note.getStarttime(), note.getDuration());
	}

	public BMSPlayerInputProcessor getBMSPlayerInputProcessor() {
		return input;
	}

	public LaneRenderer getLaneRenderer() {
		return lanerender;
	}
	
	public int getJudgeCount(int judge, boolean fast) {
		return this.judge.getJudgeCount(judge, fast);
	}

	public JudgeManager getJudgeManager() {
		return judge;
	}

	public void update(int lane, int judge, int time, int fase) {
		if (judge < 5) {
			notes++;
		}
		if(judge == 3 || judge == 4) {
			bga.setMisslayerTme(time);
		}
		gauge.update(judge);
		// System.out.println(
		// "Now count : " + notes + " - " + totalnotes);
		lanerender.update(lane, judge, time);

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
			final TimeLine[] timelines = model.getAllTimeLines();
			while (time < timelines[timelines.length - 1].getTime() + 5000 && !stop) {
				time = (int) (System.currentTimeMillis() - starttime);
				// リプレイデータ再生
				if (keylog != null) {
					while (index < keylog.size() && keylog.get(index).time <= time) {
						KeyInputLog key = keylog.get(index);
//						if(input.getKeystate()[key.keycode] == key.pressed) {
//							System.out.println("押し離しが行われていません : key - " + key.keycode + " pressed - " + key.pressed + " time - " + key.time);
//						}
						input.getKeystate()[key.keycode] = key.pressed;
						input.getTime()[key.keycode] = key.time;
						index++;
					}
				}
				judge.update(timelines, time);

				long nowtime = System.currentTimeMillis() - starttime - time;
				frametimes = nowtime < frametimes ? frametimes : nowtime;
			}

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
			for (int p = 0; time < timelines[timelines.length - 1].getTime() + 5000 && !stop;) {
				time = (int) (System.currentTimeMillis() - starttime);
				// BGレーン再生
				while (p < timelines.length && timelines[p].getTime() <= time) {
					for (Note n : timelines[p].getBackGroundNotes()) {
						play(n);
					}
					p++;
				}
				if (p < timelines.length) {
					try {
						final long sleeptime = timelines[p].getTime() - (System.currentTimeMillis() - starttime);
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

	@Override
	public void create(PlayerResource resource) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getMaxcombo() {
		return judge.getMaxcombo();
	}

	public int getCombo() {
		return judge.getCombo();
	}

	@Override
	public int getMinBPM() {
		if(minbpm != maxbpm) {
			return minbpm;			
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getBPM() {
		return (int) lanerender.getNowBPM();
	}

	@Override
	public int getMaxBPM() {
		if(minbpm != maxbpm) {
			return maxbpm;			
		}
		return Integer.MIN_VALUE;
	}
	
	@Override
	public float getHispeed() {
		return lanerender.getHispeed();
	}

	@Override
	public int getDuration() {
		return lanerender.getGreenValue();
	}

	@Override
	public float getGrooveGauge() {
		return gauge.getValue();
	}

	public int getTimeleftMinute() {
		return (int) ((playtime - (int) (starttime != 0 ? System.currentTimeMillis() - starttime : 0) + 1000) / 60000);
	}

	public int getTimeleftSecond() {
		return ((playtime - (int) (starttime != 0 ? System.currentTimeMillis() - starttime : 0) + 1000) / 1000) % 60;
	}

	public String getTitle() {
		return resource.getBMSModel().getTitle();
	}
}
