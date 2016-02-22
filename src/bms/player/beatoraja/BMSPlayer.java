package bms.player.beatoraja;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import bms.model.*;
import bms.player.beatoraja.PlaySkin.SkinPart;
import bms.player.beatoraja.audio.AudioProcessor;
import bms.player.beatoraja.bga.BGAManager;
import bms.player.beatoraja.gauge.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.pattern.*;
import bms.player.beatoraja.skin.LR2SkinLoader;
import bms.player.lunaticrave2.IRScoreData;

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
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

/**
 * BMSプレイヤー本体
 * 
 * @author exch
 */
public class BMSPlayer extends ApplicationAdapter {

	// TODO LR2スキンローダー

	// TODO STATE_PRELOAD中の中断時に再度同じ楽曲を選択すると音が出ない
	// TODO GLAssistから起動すると楽曲ロード中に止まる
	// TODO layerの(0,0,0)を透過するShaderの実装

	private BitmapFont titlefont;
	private BitmapFont judgefont;
	private BitmapFont systemfont;
	private BMSModel model;
	private TimeLine[] timelines;
	private int totalnotes;
	private int minbpm;
	private int maxbpm;

	private BMSPlayerInputProcessor input;
	private LaneRenderer lanerender;
	private ScoreGraphRenderer graphrender;
	private JudgeManager judge;
	private AudioProcessor audio;

	private BGAManager bga;

	private PlayerResource resource;

	private PlaySkin skin;

	private GrooveGauge gauge;

	private long starttime;
	private long finishtime;

	private int autoplay = 0;

	private AutoplayThread autoThread;
	private KeyInputThread keyinput;

	private final String[] judgename = { "PG ", "GR ", "GD ", "BD ", "PR ", "MS " };

	private int prevrendertime;
	private int playingbga = -1;
	private int playinglayer = -1;
	private int[] misslayer = null;

	private int assist = 0;

	private List<PatternModifyLog> pattern = new ArrayList<PatternModifyLog>();

	private ReplayData replay = null;
	private ShaderProgram layershader;

	private MainController main;

	private List<Float> gaugelog = new ArrayList<Float>();

	public BMSPlayer(MainController main, PlayerResource resource) {
		this.main = main;
		this.resource = resource;
		this.model = resource.getBMSModel();
		minbpm = (int) model.getMinBPM();
		maxbpm = (int) model.getMaxBPM();
		Config config = resource.getConfig();
		this.autoplay = resource.getAutoplay();
		timelines = model.getAllTimeLines();
		totalnotes = model.getTotalNotes();
		if (resource.getCourseBMSModels() == null) {
			if (config.isBpmguide()) {
				assist = 1;
			}

			if (config.isConstant()) {
				new ConstantBPMModifier().modify(model);
				assist = 1;
			}

			if (config.getLnassist() == 1) {
				new LongNoteModifier().modify(model);
				assist = 2;
			}
		}

		if (autoplay == 2) {
			autoplay = 0;
			if (new File("replay" + File.separator + model.getHash() + ".json").exists()) {
				Json json = new Json();
				try {
					replay = (ReplayData) json.fromJson(ReplayData.class,
							new FileReader("replay" + File.separator + model.getHash() + ".json"));
					autoplay = 2;
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		Logger.getGlobal().info("アシストオプション設定完了");
		if (replay != null) {
			PatternModifier.modify(model, Arrays.asList(replay.pattern));
		} else if (resource.getPatternModifyLog() != null) {
			PatternModifier.modify(model, Arrays.asList(resource.getPatternModifyLog()));
			Logger.getGlobal().info("譜面オプション : 保存された譜面変更ログから譜面再現");
		} else if (resource.getCourseBMSModels() == null || config.getRandom() == 1) {
			switch (config.getRandom()) {
			case 0:
				break;
			case 1:
				pattern = new LaneShuffleModifier(LaneShuffleModifier.MIRROR).modify(model);
				break;
			case 2:
				pattern = new LaneShuffleModifier(LaneShuffleModifier.R_RANDOM).modify(model);
				break;
			case 3:
				pattern = new LaneShuffleModifier(LaneShuffleModifier.RANDOM).modify(model);
				break;
			case 4:
				pattern = new NoteShuffleModifier(NoteShuffleModifier.S_RANDOM).modify(model);
				break;
			case 5:
				pattern = new NoteShuffleModifier(NoteShuffleModifier.SPIRAL).modify(model);
				break;
			case 6:
				pattern = new NoteShuffleModifier(NoteShuffleModifier.H_RANDOM).modify(model);
				break;
			case 7:
				pattern = new NoteShuffleModifier(NoteShuffleModifier.ALL_SCR).modify(model);
				break;
			case 8:
				pattern = new LaneShuffleModifier(LaneShuffleModifier.RANDOM_EX).modify(model);
				break;
			case 9:
				pattern = new NoteShuffleModifier(NoteShuffleModifier.S_RANDOM_EX).modify(model);
				break;
			}
			Logger.getGlobal().info("譜面オプション :  "  + config.getRandom());
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
			case 4:
				gauge = new ExgradeGrooveGauge(model);
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
		List<Float> f = resource.getGauge();
		if (f != null) {
			gauge.setValue(f.get(f.size() - 1));
		}
		Logger.getGlobal().info("ゲージ設定完了");

	}

	@Override
	public void create() {
		final ShapeRenderer shape = main.getShapeRenderer();
		final SpriteBatch sprite = main.getSpriteBatch();

		Config config = resource.getConfig();
		Logger.getGlobal().info("create");
		if (config.getLR2PlaySkinPath() != null) {
			try {
				skin = new LR2SkinLoader().loadPlaySkin(new File(config.getLR2PlaySkinPath()));
			} catch (IOException e) {
				e.printStackTrace();
				skin = new PlaySkin();
			}
		} else {
			skin = new PlaySkin();
		}

		input = main.getInputProcessor();
		input.setEnableKeyInput(autoplay == 0);
		lanerender = new LaneRenderer(this, sprite, skin, resource, model);
		Logger.getGlobal().info("描画クラス準備");

		Logger.getGlobal().info("hash");
		IRScoreData score = main.getScoreDatabase().getScoreData("Player", model.getHash(), false);
		Logger.getGlobal().info("スコアデータベースからスコア取得");
		if (score == null) {
			score = new IRScoreData();
		}
		graphrender = new ScoreGraphRenderer(model, score.getExscore(), score.getExscore());
		Logger.getGlobal().info("スコアグラフ描画クラス準備");
		judge = new JudgeManager(this, model);

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		parameter.characters += model.getFullTitle();
		titlefont = generator.generateFont(parameter);
		parameter.size = 18;
		systemfont = generator.generateFont(parameter);
		judgefont = generator.generateFont(parameter);
		generator.dispose();

		String vertex = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
				+ "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
				+ "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
				+ "uniform mat4 u_projTrans;\n" //
				+ "varying vec4 v_color;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
				+ "   v_color.a = v_color.a * (256.0/255.0);\n" //
				+ "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
				+ "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
				+ "}\n";

		String fragment = "#ifdef GL_ES\n" //
				+ "#define LOWP lowp\n" //
				+ "precision mediump float;\n" //
				+ "#else\n" //
				+ "#define LOWP \n" //
				+ "#endif\n" //
				+ "varying LOWP vec4 v_color;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "uniform sampler2D u_texture;\n" //
				+ "void main()\n"//
				+ "{\n" //
				+ "    vec4 c4 = texture2D(u_texture, v_texCoords);\n"
				+ "    if(c4.r == 0 && c4.g == 0 && c4.b == 0) { gl_FragColor = v_color * vec4(c4.r, c4.g, c4.b, 1);} else {gl_FragColor = v_color * vec4(c4.r, c4.g, c4.b, c4.a);}\n"
				+ "}";
		layershader = new ShaderProgram(vertex, fragment);

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

	@Override
	public void render() {
		final ShapeRenderer shape = main.getShapeRenderer();
		final SpriteBatch sprite = main.getSpriteBatch();

		final int time = (int) (System.currentTimeMillis() - starttime);
		switch (state) {
		// 楽曲ロード
		case STATE_PRELOAD:
			renderMain(0);
			shape.begin(ShapeType.Filled);
			shape.setColor(Color.YELLOW);
			shape.rect(skin.getLaneregion()[7].getX(),
					skin.getLaneregion()[7].getY() + skin.getLaneregion()[7].getHeight() / 2f,
					(audio.getProgress() + bga.getProgress()) * (skin.getLaneregion()[6].getX()
							+ skin.getLaneregion()[6].getWidth() - skin.getLaneregion()[7].getX()) / 2,
					4);
			shape.end();

			if (resource.mediaLoadFinished()) {
				state = STATE_READY;
				starttime = System.currentTimeMillis();
				Logger.getGlobal().info("STATE_READYに移行");
			}
			break;
		// GET READY
		case STATE_READY:
			renderMain(0);
			sprite.begin();
			systemfont.setColor(Color.WHITE);
			systemfont.draw(sprite, "GET READY", skin.getLaneregion()[7].getX() + 140,
					skin.getLaneregion()[7].getY() + skin.getLaneregion()[7].getHeight() / 2f);
			sprite.end();
			if (time > 1000) {
				state = STATE_PLAY;
				starttime = System.currentTimeMillis();
				input.setStartTime(starttime);
				prevrendertime = -1;
				List<KeyInputLog> keylog = null;
				if (autoplay == 1) {
					keylog = this.createAutoplayLog();
				} else if (autoplay == 2) {
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
			final float g = gauge.getValue();
			if (gaugelog.size() <= time / 500) {
				gaugelog.add(g);
			}
			// System.out.println("playing time : " + time);
			if (starttime != 0 && timelines[timelines.length - 1].getTime() + 5000 < time) {
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
			renderMain(time);

			shape.begin(ShapeType.Filled);
			long l = System.currentTimeMillis() - finishtime;
			shape.setColor(0, 0, 0, ((float) l) / 1000f);
			float width = 640f * l / 1000;
			float height = 360f * l / 1000;
			shape.rect(640 - width, 360 - height, width * 2, height * 2);
			shape.end();
			if (l > 1000) {
				if (keyinput != null) {
					Logger.getGlobal().info("入力パフォーマンス(max ms) : " + keyinput.frametimes);
				}
				if (autoplay == 0) {
					resource.setScoreData(createScoreData());
				}
				saveConfig();
				gaugelog.add(0f);
				resource.setGauge(gaugelog);
				resource.setGrooveGauge(gauge);
				if (pattern != null) {
					resource.setPatternModifyLog(pattern.toArray(new PatternModifyLog[0]));
				}
				input.setEnableKeyInput(true);
				input.setStartTime(0);
				main.changeState(MainController.STATE_RESULT, resource);
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
			shape.begin(ShapeType.Filled);
			long l2 = System.currentTimeMillis() - finishtime;
			shape.setColor(1, 1, 1, ((float) l2) / 1000f);
			float width2 = 640f * l2 / 1000;
			float height2 = 360f * l2 / 1000;
			shape.rect(640 - width2, 360 - height2, width2 * 2, height2 * 2);
			shape.end();
			if (l2 > 1000) {
				if (keyinput != null) {
					Logger.getGlobal().info("入力パフォーマンス(max ms) : " + keyinput.frametimes);
				}
				if (autoplay == 0) {
					resource.setScoreData(createScoreData());
				}
				saveConfig();
				resource.setGauge(gaugelog);
				resource.setGrooveGauge(gauge);
				if (pattern != null) {
					resource.setPatternModifyLog(pattern.toArray(new PatternModifyLog[0]));
				}
				input.setEnableKeyInput(true);
				input.setStartTime(0);
				main.changeState(MainController.STATE_RESULT, resource);
			}
			break;
		}
		if(input.isExitPressed()) {
			stopPlay();
		}
	}

	private void saveConfig() {
		Config config = resource.getConfig();
		if (lanerender.isFixHispeed()) {
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
		score.setHash(model.getHash());
		score.setNotes(model.getTotalNotes());
		int clear = GrooveGauge.CLEARTYPE_FAILED;
		if (state != STATE_FAILED && gauge.isQualified()) {
			if (assist > 0) {
				clear = assist == 1 ? GrooveGauge.CLEARTYPE_LIGHT_ASSTST : GrooveGauge.CLEARTYPE_ASSTST;
			} else {
				if (judge.getJudgeCount(3) + judge.getJudgeCount(4) == 0) {
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
		// リプレイデータ保存
		if (pattern != null) {
			ReplayData rd = new ReplayData();
			rd.keylog = input.getKeyInputLog().toArray(new KeyInputLog[0]);
			rd.pattern = pattern.toArray(new PatternModifyLog[0]);
			rd.gauge = resource.getConfig().getGauge();
			File replaydir = new File("replay");
			if (!replaydir.exists()) {
				replaydir.mkdirs();
			}
			if (score.getClear() < clear || (clear != GrooveGauge.CLEARTYPE_FAILED
					&& !new File("replay" + File.separatorChar + model.getHash() + ".json").exists())) {
				Json json = new Json();
				json.setOutputType(OutputType.json);
				try {
					FileWriter fw = new FileWriter("replay" + File.separatorChar + model.getHash() + ".json");
					fw.write(json.prettyPrint(rd));
					fw.flush();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		score.setPg(pgreat);
		score.setGr(great);
		score.setGd(good);
		score.setBd(bad);
		score.setPr(poor + miss);
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

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		float w = 1280;
		float h = 720;

		// 背景描画
		// sprite.begin();
		// sprite.draw(skin.getBackground(), 0, 0, w, h);
		// sprite.end();

		sprite.begin();
		for (SkinPart part : skin.getSkinPart()) {
			if (part.timing != 3 && part.op[0] == 0 && part.op[1] == 0 && part.op[2] == 0) {
				sprite.draw(part.image, part.dst.x, part.dst.y, part.dst.width, part.dst.height);
			}
		}
		sprite.end();

		// グラフ描画
		graphrender.drawGraph(skin, sprite, systemfont, shape, this.judge);

		// プログレス描画
		Rectangle progress = new Rectangle(4, 140, 12, 540);
		shape.begin(ShapeType.Line);
		shape.setColor(Color.WHITE);
		shape.rect(progress.x, progress.y, progress.width, progress.height);
		shape.end();
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.BLACK);
		shape.rect(progress.x + 1, progress.y + 1, progress.width - 2, progress.height - 2);

		shape.setColor(Color.ORANGE);
		shape.rect(progress.x + 1,
				progress.y + 1
						+ progress.height * (1.0f - (float) time / (timelines[timelines.length - 1].getTime() + 5000)),
				progress.width - 2, 20);
		shape.end();
		// レーン描画
		lanerender.drawLane(shape, systemfont, model, timelines, starttime,
				time);

		// BGA再生
		for (TimeLine tl : timelines) {
			if (tl.getTime() > time) {
				break;
			}

			if (tl.getTime() > prevrendertime) {
				if (tl.getBGA() != -1) {
					playingbga = tl.getBGA();
				}
				if (tl.getLayer() != -1) {
					playinglayer = tl.getLayer();
				}
				if (tl.getPoor() != null && tl.getPoor().length > 0) {
					misslayer = tl.getPoor();
				}
			}
		}
		Rectangle r = skin.getBGAregion();
		shape.begin(ShapeType.Line);
		shape.setColor(Color.WHITE);
		shape.rect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);
		shape.end();
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.BLACK);
		shape.rect(r.x, r.y, r.width, r.height);
		shape.end();
		if (state == STATE_PRELOAD && bga.getBackbmpData() != null) {
			sprite.begin();
			Texture bgatex = new Texture(bga.getBackbmpData());
			sprite.draw(bgatex, r.x, r.y, r.width, r.height);
			sprite.end();
			bgatex.dispose();
		} else if (misslayer != null && judge.getMisslayer() != 0 && time >= judge.getMisslayer()
				&& time < judge.getMisslayer() + 500) {
			// ミスレイヤー表示
			Pixmap miss = bga.getBGAData(misslayer[misslayer.length * (time - judge.getMisslayer()) / 500]);
			if (miss != null) {
				sprite.begin();
				Texture bgatex = new Texture(miss);
				sprite.draw(bgatex, r.x, r.y, r.width, r.height);
				sprite.end();
				bgatex.dispose();
			}
		} else {
			if (bga.getBGAData(playingbga) != null) {
				sprite.begin();
				Texture bgatex = new Texture(bga.getBGAData(playingbga));
				sprite.draw(bgatex, r.x, r.y, r.width, r.height);
				sprite.end();
				bgatex.dispose();
			}
			if (bga.getBGAData(playinglayer) != null) {
				sprite.begin();
				Texture bgatex = new Texture(bga.getBGAData(playinglayer));
				// sprite.setShader(layershader);
				sprite.draw(bgatex, r.x, r.y, r.width, r.height);
				// sprite.setShader(null);
				sprite.end();
				bgatex.dispose();
			}
		}

		sprite.begin();
		titlefont.setColor(Color.WHITE);
		titlefont.draw(sprite, model.getFullTitle(), r.x, r.y + r.height);
		sprite.end();

		// ゲージ描画
		Rectangle gr = skin.getGaugeRegion();
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.DARK_GRAY);
		shape.rect(gr.x, gr.y, gr.width, gr.height);
		shape.rect(gr.x + gr.width - 80, gr.y + gr.height, 80, 30);
		shape.end();
		gauge.draw(skin, sprite, gr.x, gr.y, gr.width, gr.height);
		sprite.begin();
		titlefont.setColor(Color.WHITE);
		titlefont.draw(sprite, String.format("%5.1f", gauge.getValue()) + "%", gr.x + gr.width - 75,
				gr.y + gr.height + 25);
		sprite.end();
		// ジャッジカウント描画
		Rectangle judge = skin.getJudgecountregion();
		shape.begin(ShapeType.Line);
		shape.setColor(Color.WHITE);
		shape.rect(judge.x - 1, judge.y - 19, 122, 122);
		shape.end();
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.BLACK);
		shape.rect(judge.x, judge.y - 18, 120, 120);
		shape.end();

		sprite.begin();
		for (int i = 0; i < judgename.length; i++) {
			judgefont.setColor(Color.WHITE);
			judgefont.draw(sprite, judgename[i]
					+ String.format("%4d /%4d", this.judge.getJudgeCount(i, true), this.judge.getJudgeCount(i, false)),
					judge.x, judge.y + 20 * (5 - i));
		}
		sprite.end();
		// BPM描画
		sprite.begin();
		titlefont.setColor(Color.WHITE);
		titlefont.draw(sprite, "BPM", 650, 44);
		titlefont.draw(sprite, minbpm + " - " + (int) lanerender.getNowBPM() + " - " + maxbpm, 600, 22);
		sprite.end();
		// ハイスピード、デュレーション描画
		sprite.begin();
		titlefont.setColor(Color.WHITE);
		titlefont.draw(sprite, "HISPEED - " + String.format("%.2f", lanerender.getHispeed()) + "  DURATION - " + lanerender.getGreenValue(),
				30, 22);
		sprite.end();

		prevrendertime = time;
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

	public void play(int id) {
		audio.play(id);
	}

	public BMSPlayerInputProcessor getBMSPlayerInputProcessor() {
		return input;
	}

	public LaneRenderer getLaneRenderer() {
		return lanerender;
	}

	public JudgeManager getJudgeManager() {
		return judge;
	}

	private int notes;

	public void update(int judge) {
		if (judge < 5) {
			notes++;
		}
		gauge.update(judge);
		// System.out.println(
		// "Now count : " + notes + " - " + totalnotes);
	}

	private final List<KeyInputLog> createAutoplayLog() {
		List<KeyInputLog> keylog = new ArrayList<KeyInputLog>();
		Note[] ln = new Note[8];
		for (int i : model.getAllTimes()) {
			TimeLine tl = model.getTimeLine(i);
			for (int lane = 0; lane < 8; lane++) {
				Note note = tl.getNote(lane);
				if (note != null) {
					if (note instanceof LongNote) {
						if (((LongNote) note).getEnd() == tl) {
							keylog.add(new KeyInputLog(i, lane, false));
							if (lane == 7) {
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
						if (lane == 7) {
							keylog.add(new KeyInputLog(i, lane + 1, false));
						}
					}
				}
			}
		}
		return keylog;
	}

	class KeyInputThread extends Thread {
		private boolean stop = false;
		private long frametimes = 1;
		private List<KeyInputLog> keylog;

		public KeyInputThread(List<KeyInputLog> keylog) {
			this.keylog = keylog;
		}

		@Override
		public void run() {
			final TimeLine[] timelines = model.getAllTimeLines();
			int index = 0;

			int time = 0;
			while (time < timelines[timelines.length - 1].getTime() + 5000 && !stop) {
				time = (int) (System.currentTimeMillis() - starttime);
				// リプレイデータ再生
				if (keylog != null) {
					while (index < keylog.size() && keylog.get(index).time <= time) {
						KeyInputLog key = keylog.get(index);
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

	class AutoplayThread extends Thread {

		private boolean stop = false;

		@Override
		public void run() {
			final TimeLine[] timelines = model.getAllTimeLines();
			int time = 0;
			for (int p = 0; time < timelines[timelines.length - 1].getTime() + 5000 && !stop;) {
				time = (int) (System.currentTimeMillis() - starttime);
				// BGレーン再生
				while (p < timelines.length && timelines[p].getTime() <= time) {
					for (Note n : timelines[p].getBackGroundNotes()) {
						audio.play(n.getWav());
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
}
