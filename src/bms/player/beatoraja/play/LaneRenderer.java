package bms.player.beatoraja.play;

import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.*;
import bms.player.beatoraja.play.SkinNote.SkinLane;

import com.badlogic.gdx.utils.LongArray;
import org.lwjgl.opengl.GL11;

import bms.model.*;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.SkinImage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

import static bms.player.beatoraja.CourseData.CourseDataConstraint.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * レーン描画用クラス
 * 
 * @author exch
 */
public class LaneRenderer {

	/**
	 * レーンカバーの量
	 */
	private float lanecover = 0.2f;
	/**
	 * レーンカバーを表示するかどうか
	 */
	private boolean enableLanecover = true;
	/**
	 * リフトの量
	 */
	private float lift = 0.05f;
	/**
	 * リフトを使用するかどうか
	 */
	private boolean enableLift = true;

	private float hispeed = 1.0f;

	private int gvalue;

	private int fixhispeed;
	private float basehispeed;

	private BMSModel model;
	private TimeLine[] timelines;

	private int pos;

	private final BMSPlayer main;

	private BitmapFont font;
	private final PlaySkin skin;

	private final Config conf;
	private final PlayerConfig config;
	private PlayConfig playconfig;

	private int currentduration;

	private double basebpm;
	private double nowbpm;

	private TextureRegion[] noteimage;
	private TextureRegion[][] longnote;
	private TextureRegion[] mnoteimage;
	private TextureRegion[] hnoteimage;
	private TextureRegion[] pnoteimage;
	private Rectangle[] laneregion;

	private TextureRegion blank;

	//PMSのリズムに合わせたノートの拡大用
	//4分から最大拡大までの時間
	private final float noteExpansionTime = 9;
	//最大拡大から通常サイズに戻るまでの時間
	private final float noteContractionTime = 150;

	public LaneRenderer(BMSPlayer main, BMSModel model) {

		this.main = main;
		Pixmap hp = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		hp.drawPixel(0, 0, Color.toIntBits(255, 255, 255, 255));
		blank = new TextureRegion(new Texture(hp));
		hp.dispose();

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 18;
		font = generator.generateFont(parameter);
		generator.dispose();

		this.skin = (PlaySkin) main.getSkin();
		this.conf = main.getMainController().getPlayerResource().getConfig();
		this.config = main.getMainController().getPlayerResource().getPlayerConfig();
		this.playconfig = main.getPlayConfig(this.config);

		this.enableLanecover = playconfig.isEnablelanecover();
		this.enableLift = playconfig.isEnablelift();
		this.lift = playconfig.getLift();
		this.fixhispeed = config.getFixhispeed();
		this.gvalue = playconfig.getDuration();
		hispeed = playconfig.getHispeed();
		init(model);

		for (CourseData.CourseDataConstraint i : main.getMainController().getPlayerResource().getConstraint()) {
			if (i == NO_SPEED) {
				hispeed = 1.0f;
				lanecover = 0;
				lift = 0;
			}
		}

		noteimage = new TextureRegion[model.getMode().key];
		longnote = new TextureRegion[model.getMode().key][10];
		mnoteimage = new TextureRegion[model.getMode().key];
		hnoteimage = new TextureRegion[model.getMode().key];
		pnoteimage = new TextureRegion[model.getMode().key];
		laneregion = new Rectangle[model.getMode().key];
	}

	public void init(BMSModel model) {
		pos = 0;
		this.model = model;
		List<TimeLine> tls = new ArrayList<TimeLine>();
		double cbpm = model.getBpm();
		for (TimeLine tl : model.getAllTimeLines()) {
			if (cbpm != tl.getBPM() || tl.getStop() > 0 || tl.getSectionLine()) {
				tls.add(tl);
			} else if (tl.existNote() || tl.existHiddenNote()) {
				tls.add(tl);
			}
			cbpm = tl.getBPM();
		}
		this.timelines = tls.toArray(new TimeLine[tls.size()]);
		// Logger.getGlobal().info("省略したTimeLine数:" +
		// (model.getAllTimeLines().length - timelines.length) + " / " +
		// model.getAllTimeLines().length);

		switch (config.getFixhispeed()) {
		case PlayerConfig.FIX_HISPEED_OFF:
			break;
		case PlayerConfig.FIX_HISPEED_STARTBPM:
			basebpm = model.getBpm();
			break;
		case PlayerConfig.FIX_HISPEED_MINBPM:
			basebpm = model.getMinBPM();
			break;
		case PlayerConfig.FIX_HISPEED_MAXBPM:
			basebpm = model.getMaxBPM();
			break;
		case PlayerConfig.FIX_HISPEED_MAINBPM:
			Map<Double, Integer> m = new HashMap<Double, Integer>();
			for (TimeLine tl : model.getAllTimeLines()) {
				Integer count = m.get(tl.getBPM());
				if (count == null) {
					count = 0;
				}
				m.put(tl.getBPM(), count + tl.getTotalNotes());
			}
			int maxcount = 0;
			for (double bpm : m.keySet()) {
				if (m.get(bpm) > maxcount) {
					maxcount = m.get(bpm);
					basebpm = bpm;
				}
			}
			break;
		}

		this.setLanecover(playconfig.getLanecover());
		if (this.fixhispeed != PlayerConfig.FIX_HISPEED_OFF) {
			basehispeed = hispeed;
		}
	}

	public int getFixHispeed() {
		return fixhispeed;
	}

	public float getHispeed() {
		return hispeed;
	}

	public int getGreenValue() {
		return gvalue;
	}

	public void setGreenValue(int gvalue) {
		this.gvalue = gvalue < 1 ? 1 : gvalue;
		setLanecover(lanecover);
	}

	public int getCurrentDuration() {
		return currentduration;
	}

	public boolean isEnableLift() {
		return enableLift;
	}

	public float getLiftRegion() {
		return lift;
	}

	public float getLanecover() {
		return lanecover;
	}
	
	public void setLanecover(float lanecover) {
		lanecover = (lanecover < 0 ? 0 : (lanecover > 1 ? 1 : lanecover));
		this.lanecover = lanecover;
		if (this.fixhispeed != PlayerConfig.FIX_HISPEED_OFF) {
			hispeed = (float) ((2400f / (basebpm / 100) / gvalue) * (1 - (enableLanecover ? lanecover : 0)));
		}
	}

	public void setEnableLanecover(boolean b) {
		enableLanecover = b;
	}

	public boolean isEnableLanecover() {
		return enableLanecover;
	}

	public void changeHispeed(boolean b) {
		float f = 0;
		if (this.fixhispeed != PlayerConfig.FIX_HISPEED_OFF) {
			f = basehispeed * 0.25f * (b ? 1 : -1);
		} else {
			f = 0.125f * (b ? 1 : -1);
		}
		if (hispeed + f > 0 && hispeed + f < 20) {
			hispeed += f;
		}
	}

	public void drawLane(SkinObjectRenderer sprite, long time, SkinLane[] lanes, int dstNote2) {
		for (int i = 0; i < lanes.length; i++) {
			if (i >= noteimage.length) {
				break;
			}
			if (lanes[i].note != null) {
				noteimage[i] = lanes[i].note.getImage(time, main);
			}
			for (int type = 0; type < 10; type++) {
				if (lanes[i].longnote[type] != null) {
					longnote[i][type] = lanes[i].longnote[type].getImage(time, main);
				}
			}
			if (lanes[i].minenote != null) {
				mnoteimage[i] = lanes[i].minenote.getImage(time, main);
			}
			if (lanes[i].hiddennote != null) {
				hnoteimage[i] = lanes[i].hiddennote.getImage(time, main);
			}
			if (lanes[i].processednote != null) {
				pnoteimage[i] = lanes[i].processednote.getImage(time, main);
			}
			laneregion[i] = lanes[i].getDestination(time, main);
		}

		time = (main.getTimer()[TIMER_PLAY] != Long.MIN_VALUE ? (time - main.getTimer()[TIMER_PLAY]) : 0)
				+ config.getJudgetiming();
		if (main.getState() == BMSPlayer.STATE_PRACTICE) {
			time = main.getPracticeConfiguration().getPracticeProperty().starttime;
			pos = 0;
		}
		final long microtime = time * 1000;
		final boolean showTimeline = (main.getState() == BMSPlayer.STATE_PRACTICE);

		final float hispeed = main.getState() != BMSPlayer.STATE_PRACTICE ? this.hispeed : 1.0f;
		final Rectangle[] playerr = skin.getLaneGroupRegion();
		double bpm = model.getBpm();
		double nbpm = bpm;
		for (int i = (pos > 5 ? pos - 5 : 0); i < timelines.length && timelines[i].getMicroTime() <= microtime; i++) {
			nbpm = timelines[i].getBPM();
		}
		nowbpm = nbpm;
		final int region = (int) (240000 / nbpm / hispeed);
		// double sect = (bpm / 60) * 4 * 1000;
		final double hu = laneregion[0].y + laneregion[0].height;
		final double hl = enableLift ? laneregion[0].y + laneregion[0].height * lift : laneregion[0].y;
		final double rxhs = (hu - hl) * hispeed;
		double y = hl;

		currentduration = Math.round(region * (1 - (enableLanecover ? lanecover : 0)));

		// 判定エリア表示
		// TODO 実装が古いため、書き直す
		if (config.isShowjudgearea()) {
			final Color[] color = { Color.valueOf("0000ff20"), Color.valueOf("00ff0020"), Color.valueOf("ffff0020"),
					Color.valueOf("ff800020"), Color.valueOf("ff000020") };
			for (int lane = 0; lane < laneregion.length; lane++) {
				final int[][] judgetime = main.getJudgeManager().getJudgeTimeRegion(lane);
				for (int i = pos; i < timelines.length; i++) {
					final TimeLine tl = timelines[i];
					if (tl.getMicroTime() >= microtime) {
						double rate = (tl.getSection() - (i > 0 ? timelines[i - 1].getSection() : 0)) * rxhs * 1000
								/ (tl.getMicroTime() - (i > 0
										? timelines[i - 1].getMicroTime() + timelines[i - 1].getMicroStop() : 0));
						for (int j = color.length - 1; j >= 0; j--) {
							sprite.setColor(color[j]);
							int nj = j > 0 ? judgetime[j - 1][1] : 0;
							sprite.draw(blank, laneregion[lane].x, (float) (hl + nj * rate), laneregion[lane].width,
									(float) ((judgetime[j][1] - nj) * rate));
						}
						break;
					}
				}
			}
		}

		final double orgy = y;
		for (int i = pos; i < timelines.length && y <= hu; i++) {
			final TimeLine tl = timelines[i];
			if (tl.getMicroTime() >= microtime) {
				if (i > 0) {
					final TimeLine prevtl = timelines[i - 1];
					if (prevtl.getMicroTime() + prevtl.getMicroStop() > microtime) {
						y += (tl.getSection() - prevtl.getSection()) * rxhs;
					} else {
						y += (tl.getSection() - prevtl.getSection()) * (tl.getMicroTime() - microtime)
								/ (tl.getMicroTime() - prevtl.getMicroTime() - prevtl.getMicroStop()) * rxhs;
					}
				} else {
					y += tl.getSection() * (tl.getMicroTime() - microtime) / tl.getMicroTime() * rxhs;
				}
				if (showTimeline && (i > 0 && (tl.getTime() / 1000) > (timelines[i - 1].getTime() / 1000))) {
					for (SkinImage line : skin.getTimeLine()) {
						line.draw(sprite, time, main, 0, (int) (y - hl));
					}
					for (Rectangle r : playerr) {
						// TODO 数値もスキンベースへ移行
						sprite.draw(font, String.format("%2d:%02d.%1d", tl.getTime() / 60000,
								(tl.getTime() / 1000) % 60, (tl.getTime() / 100) % 10), r.x + 4, (float) (y + 20),
								Color.valueOf("40c0c0"));
					}
				}

				if (config.isBpmguide() || showTimeline) {
					if (tl.getBPM() != nbpm) {
						for (SkinImage line : skin.getBPMLine()) {
							line.draw(sprite, time, main, 0, (int) (y - hl));
						}
						for (Rectangle r : playerr) {
							// TODO 数値もスキンベースへ移行
							sprite.draw(font, "BPM" + ((int) tl.getBPM()), r.x + r.width / 2, (float) (y + 20),
									Color.valueOf("00c000"));
						}

					}
					if (tl.getStop() > 0) {
						for (SkinImage line : skin.getStopLine()) {
							line.draw(sprite, time, main, 0, (int) (y - hl));
						}
						for (Rectangle r : playerr) {
							// TODO 数値もスキンベースへ移行
							sprite.draw(font, "STOP " + ((int) tl.getStop()) + "ms", r.x + r.width / 2,
									(float) (y + 20), Color.valueOf("c0c000"));
						}
					}

				}
				// 小節線描画
				if (tl.getSectionLine()) {
					for (SkinImage line : skin.getLine()) {
						line.draw(sprite, time, main, 0, (int) (y - hl));
					}
				}
				nbpm = tl.getBPM();
			} else if (pos == i - 1) {
				boolean b = true;
				for (int lane = 0; lane < laneregion.length; lane++) {
					final Note note = tl.getNote(lane);
					if (note != null && ((note instanceof LongNote
							&& (((LongNote) note).isEnd() ? (LongNote) note : ((LongNote) note).getPair())
									.getMicroTime() >= microtime)
							|| (conf.isShowpastnote() && note instanceof NormalNote && note.getState() == 0))) {
						b = false;
						break;
					}
				}
				if (b) {
					pos = i;
				}
			}
		}

		sprite.setColor(Color.WHITE);
		sprite.setBlend(0);
		sprite.setType(SkinObjectRenderer.TYPE_NORMAL);
		y = orgy;
		for (int i = pos; i < timelines.length && y <= hu; i++) {
			final TimeLine tl = timelines[i];
			if (tl.getMicroTime() >= microtime) {
				if (i > 0) {
					final TimeLine prevtl = timelines[i - 1];
					if (prevtl.getMicroTime() + prevtl.getMicroStop() > microtime) {
						y += (tl.getSection() - prevtl.getSection()) * rxhs;
					} else {
						y += (tl.getSection() - prevtl.getSection()) * (tl.getMicroTime() - microtime)
								/ (tl.getMicroTime() - prevtl.getMicroTime() - prevtl.getMicroStop()) * rxhs;
					}
				} else {
					y += tl.getSection() * (tl.getMicroTime() - microtime) / tl.getMicroTime() * rxhs;
				}
			}
			// ノート描画
			for (int lane = 0; lane < laneregion.length; lane++) {
				final float scale = lanes[lane].scale;
				final Note note = tl.getNote(lane);
				if (note != null) {
					//4分のタイミングでノートを拡大する
					float dstx = laneregion[lane].x;
					float dsty = (float) y;
					float dstw = laneregion[lane].width;
					float dsth = scale;
					if(skin.getNoteExpansionRate()[0] != 100 || skin.getNoteExpansionRate()[1] != 100) {
						if((main.getNowTime() - main.getNowQuarterNoteTime()) < noteExpansionTime) {
							dstw *= 1 + (skin.getNoteExpansionRate()[0]/100.0f - 1) * (main.getNowTime() - main.getNowQuarterNoteTime()) / noteExpansionTime;
							dsth *= 1 + (skin.getNoteExpansionRate()[1]/100.0f - 1) * (main.getNowTime() - main.getNowQuarterNoteTime()) / noteExpansionTime;
							dstx -= (dstw - laneregion[lane].width) / 2;
							dsty -= (dsth - scale) / 2;
						} else if((main.getNowTime() - main.getNowQuarterNoteTime()) >= noteExpansionTime && (main.getNowTime() - main.getNowQuarterNoteTime()) <= (noteExpansionTime + noteContractionTime)) {
							dstw *= 1 + (skin.getNoteExpansionRate()[0]/100.0f - 1) * (noteContractionTime - (main.getNowTime() - main.getNowQuarterNoteTime() - noteExpansionTime)) / noteContractionTime;
							dsth *= 1 + (skin.getNoteExpansionRate()[1]/100.0f - 1) * (noteContractionTime - (main.getNowTime() - main.getNowQuarterNoteTime() - noteExpansionTime)) / noteContractionTime;
							dstx -= (dstw - laneregion[lane].width) / 2;
							dsty -= (dsth - scale) / 2;
						}
					}
					if (note instanceof NormalNote) {
						// draw normal note
						if (dstNote2 != Integer.MIN_VALUE) {
							if (tl.getMicroTime() >= microtime && (note.getState() == 0 || note.getState() >= 4)) {
								final TextureRegion s = config.isMarkprocessednote() && note.getState() != 0
								? pnoteimage[lane] : noteimage[lane];
								sprite.draw(s, dstx, dsty, dstw, dsth);
							}
						} else if (tl.getMicroTime() >= microtime || (conf.isShowpastnote() && note.getState() == 0)) {
							final TextureRegion s = config.isMarkprocessednote() && note.getState() != 0
									? pnoteimage[lane] : noteimage[lane];
							sprite.draw(s, dstx, dsty, dstw, dsth);
						}
					} else if (note instanceof LongNote) {
						final LongNote ln = (LongNote) note;
						if (!ln.isEnd() && ln.getPair().getMicroTime() >= microtime) {
							// if (((LongNote) note).getEnd() == null) {
							// Logger.getGlobal().warning(
							// "LN終端がなく、モデルが正常に表示されません。LN開始時間:"
							// + ((LongNote) note)
							// .getStart()
							// .getTime());
							// } else {
							double dy = 0;
							TimeLine prevtl = tl;
							for (int j = i + 1; j < timelines.length
									&& prevtl.getSection() != ln.getPair().getSection(); j++) {
								final TimeLine nowtl = timelines[j];
								if (nowtl.getMicroTime() >= microtime) {
									if (prevtl.getMicroTime() + prevtl.getMicroStop() > microtime) {
										dy += (nowtl.getSection() - prevtl.getSection()) * rxhs;
									} else {
										dy += (nowtl.getSection() - prevtl.getSection())
												* (nowtl.getMicroTime() - microtime)
												/ (nowtl.getMicroTime() - prevtl.getMicroTime() - prevtl.getMicroStop())
												* rxhs;
									}
								}
								prevtl = nowtl;
							}
							if (dy > 0) {
								this.drawLongNote(sprite, dstx, (float) (dsty + dy), dstw,
										(float) (dsty < (laneregion[lane].y - (dsth - scale) / 2) ? dsty - (laneregion[lane].y - (dsth - scale) / 2) : dy), dsth, lane,
										ln);
							}
							// System.out.println(dy);
						}
					} else if (note instanceof MineNote) {
						// draw mine note
						if (tl.getMicroTime() >= microtime) {
							final TextureRegion s = mnoteimage[lane];
							sprite.draw(s, laneregion[lane].x, (float) y, laneregion[lane].width, scale);
						}
					}
				}
				// hidden note
				if (conf.isShowhiddennote() && tl.getMicroTime() >= microtime) {
					final Note hnote = tl.getHiddenNote(lane);
					if (hnote != null) {
						sprite.draw(hnoteimage[lane], laneregion[lane].x, (float) y, laneregion[lane].width, scale);
					}
				}
			}
		}
		// System.out.println("time :" + ltime + " y :" + yy + " real time : "
		// + (ltime * (hu - hl) / yy));
		
		//PMS見逃しPOOR描画
		if (dstNote2 != Integer.MIN_VALUE) {
			//遅BADからノースピの速度で落下
			final long badTime = Math.abs( main.getJudgeManager().getJudgeTable(false)[2][0] ) * 1000;
			double stopTime;
			double orgy2 = dstNote2;
			if(orgy2 < -laneregion[0].height) orgy2 = -laneregion[0].height;
			if(orgy2 > orgy) orgy2 = orgy;
			final double rxhs2 = (hu - hl);
			int nowPos = timelines.length - 1;
			for (int i = pos; i < timelines.length; i++) {
				final TimeLine tl = timelines[i];
				if (tl.getMicroTime() >= microtime) {
					nowPos = i;
					break;
				}
			}
			for (int i = nowPos; i >= 0 && y >= orgy2; i--) {
				final TimeLine tl = timelines[i];
				y = orgy;
				if (i + 1 < timelines.length) {
					int j;
					for (j = i; j + 1 < timelines.length && timelines[j + 1].getMicroTime() < microtime; j++) {
						if(timelines[j + 1].getMicroTime() > tl.getMicroTime() + tl.getMicroStop() + badTime) {
							stopTime = Math.max(tl.getMicroTime() + tl.getMicroStop() + badTime - timelines[j].getMicroTime() - timelines[j].getMicroStop(), 0);
							y -= (timelines[j + 1].getMicroTime() - timelines[j].getMicroTime() - timelines[j].getMicroStop() - stopTime) * rxhs2 * timelines[j].getBPM() / 240000000;
							//4分の画面上での長さ rxhs2 / 4 [pixel] 4分の時間 60 / BPM [second] 落下速度 rxhs2 * BPM / 240 [pixel/second]
						}
					}
					if(timelines[j].getMicroTime() + timelines[j].getMicroStop() < microtime) {
						if(microtime > tl.getMicroTime() + tl.getMicroStop() + badTime) {
							stopTime = Math.max(tl.getMicroTime() + tl.getMicroStop() + badTime - timelines[j].getMicroTime() - timelines[j].getMicroStop(), 0);
							y -= (microtime - timelines[j].getMicroTime() - timelines[j].getMicroStop() - stopTime) * rxhs2 * timelines[j].getBPM() / 240000000;
						}
					}
				} else {
					if(tl.getMicroTime() + tl.getMicroStop() < microtime) {
						if(microtime > tl.getMicroTime() + tl.getMicroStop() + badTime) {
							stopTime = Math.max(tl.getMicroTime() + tl.getMicroStop() + badTime - tl.getMicroTime() - tl.getMicroStop(), 0);
							y -= (microtime - tl.getMicroTime() - tl.getMicroStop() - stopTime) * rxhs2 * tl.getBPM() / 240000000;
						}
					}
				}
				// ノート描画
				for (int lane = 0; lane < laneregion.length; lane++) {
					final float scale = lanes[lane].scale;
					final Note note = tl.getNote(lane);
					if (note != null) {
						if (note instanceof NormalNote) {
							// draw normal note
							//4分のタイミングでノートを拡大する
							float dstx = laneregion[lane].x;
							float dsty = (float) y;
							float dstw = laneregion[lane].width;
							float dsth = scale;
							if(skin.getNoteExpansionRate()[0] != 100 || skin.getNoteExpansionRate()[1] != 100) {
								if((main.getNowTime() - main.getNowQuarterNoteTime()) < noteExpansionTime) {
									dstw *= 1 + (skin.getNoteExpansionRate()[0]/100.0f - 1) * (main.getNowTime() - main.getNowQuarterNoteTime()) / noteExpansionTime;
									dsth *= 1 + (skin.getNoteExpansionRate()[1]/100.0f - 1) * (main.getNowTime() - main.getNowQuarterNoteTime()) / noteExpansionTime;
									dstx -= (dstw - laneregion[lane].width) / 2;
									dsty -= (dsth - scale) / 2;
								} else if((main.getNowTime() - main.getNowQuarterNoteTime()) >= noteExpansionTime && (main.getNowTime() - main.getNowQuarterNoteTime()) <= (noteExpansionTime + noteContractionTime)) {
									dstw *= 1 + (skin.getNoteExpansionRate()[0]/100.0f - 1) * (noteContractionTime - (main.getNowTime() - main.getNowQuarterNoteTime() - noteExpansionTime)) / noteContractionTime;
									dsth *= 1 + (skin.getNoteExpansionRate()[1]/100.0f - 1) * (noteContractionTime - (main.getNowTime() - main.getNowQuarterNoteTime() - noteExpansionTime)) / noteContractionTime;
									dstx -= (dstw - laneregion[lane].width) / 2;
									dsty -= (dsth - scale) / 2;
								}
							}
							if ( ((note.getState() == 0 || note.getState() >= 4) && tl.getMicroTime() <= microtime) && y >= orgy2) {
								final TextureRegion s = noteimage[lane];
								if(y > orgy) sprite.draw(s, dstx, (float) (orgy - (dsth - scale) / 2), dstw, dsth);
								else sprite.draw(s, dstx, dsty, dstw, dsth);
							}
						}
					}
				}
			}
		}
	}

	public double getNowBPM() {
		return nowbpm;
	}

	final private void drawLongNote(SkinObjectRenderer sprite, float x, float y, float width, float height, float scale,
			int lane, LongNote ln) {
		final TextureRegion[] longnote = this.longnote[lane];
		if ((model.getLntype() == BMSModel.LNTYPE_HELLCHARGENOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
				|| ln.getType() == LongNote.TYPE_HELLCHARGENOTE) {
			// HCN
			final JudgeManager judge = main.getJudgeManager();
			sprite.draw(
					longnote[judge.getProcessingLongNotes()[lane] == ln.getPair() ? 6
							: (judge.getPassingLongNotes()[lane] == ln && ln.getState() != 0
									? (judge.getHellChargeJudges()[lane] ? 8 : 9) : 7)],
					x, y - height + scale, width, height - scale);
			sprite.draw(longnote[4], x, y, width, scale);
			sprite.draw(longnote[5], x, y - height, width, scale);
		} else if ((model.getLntype() == BMSModel.LNTYPE_CHARGENOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
				|| ln.getType() == LongNote.TYPE_CHARGENOTE) {
			// CN
			sprite.draw(longnote[main.getJudgeManager().getProcessingLongNotes()[lane] == ln.getPair() ? 2 : 3], x,
					y - height + scale, width, height - scale);
			sprite.draw(longnote[0], x, y, width, scale);
			sprite.draw(longnote[1], x, y - height, width, scale);
		} else if ((model.getLntype() == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
				|| ln.getType() == LongNote.TYPE_LONGNOTE) {
			// LN
			sprite.draw(longnote[main.getJudgeManager().getProcessingLongNotes()[lane] == ln.getPair() ? 2 : 3], x,
					y - height + scale, width, height - scale);
			sprite.draw(longnote[1], x, y - height, width, scale);
		}
	}

	public void dispose() {
		if (font != null) {
			font.dispose();
			font = null;
		}
		if (blank != null) {
			blank.getTexture().dispose();
			blank = null;
		}
	}
}
