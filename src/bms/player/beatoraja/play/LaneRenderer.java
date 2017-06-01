package bms.player.beatoraja.play;

import java.util.HashMap;
import java.util.Map;

import bms.player.beatoraja.*;
import bms.player.beatoraja.play.SkinNote.SkinLane;

import org.lwjgl.opengl.GL11;

import bms.model.*;
import bms.player.beatoraja.skin.SkinImage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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

	private final SpriteBatch sprite;
	private ShapeRenderer shape;
	private BitmapFont font;
	private final PlaySkin skin;

	private final Config config;
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

	public LaneRenderer(BMSPlayer main, BMSModel model) {

		this.main = main;
		this.sprite = main.getMainController().getSpriteBatch();
		this.shape = new ShapeRenderer();

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 18;
		font = generator.generateFont(parameter);
		generator.dispose();

		this.skin = (PlaySkin) main.getSkin();
		this.config = main.getMainController().getPlayerResource().getConfig();
		this.playconfig = (model.getMode() == Mode.BEAT_5K || model.getMode() == Mode.BEAT_7K ? config.getMode7()
				: (model.getMode() == Mode.BEAT_10K || model.getMode() == Mode.BEAT_14K ? config.getMode14()
						: config.getMode9()));

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
		this.timelines = model.getAllTimeLines();
		switch (config.getFixhispeed()) {
		case Config.FIX_HISPEED_OFF:
			break;
		case Config.FIX_HISPEED_STARTBPM:
			basebpm = model.getBpm();
			break;
		case Config.FIX_HISPEED_MINBPM:
			basebpm = model.getMinBPM();
			break;
		case Config.FIX_HISPEED_MAXBPM:
			basebpm = model.getMaxBPM();
			break;
		case Config.FIX_HISPEED_MAINBPM:
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
		if (this.fixhispeed != Config.FIX_HISPEED_OFF) {
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
		if (this.fixhispeed != Config.FIX_HISPEED_OFF) {
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
		if (this.fixhispeed != Config.FIX_HISPEED_OFF) {
			f = basehispeed * 0.25f * (b ? 1 : -1);
		} else {
			f = 0.125f * (b ? 1 : -1);
		}
		if (hispeed + f > 0 && hispeed + f < 20) {
			hispeed += f;
		}
	}

	public void drawLane(long time, SkinLane[] lanes) {
		for (int i = 0; i < lanes.length; i++) {
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
		final boolean showTimeline = (main.getState() == BMSPlayer.STATE_PRACTICE);

		final float hispeed = main.getState() != BMSPlayer.STATE_PRACTICE ? this.hispeed : 1.0f;
		final Rectangle[] playerr = skin.getLaneGroupRegion();
		double bpm = model.getBpm();
		double nbpm = bpm;
		for (int i = (pos > 5 ? pos - 5 : 0); i < timelines.length; i++) {
			if (timelines[i].getTime() > time) {
				break;
			}
			if (timelines[i].getBPM() > 0) {
				bpm = timelines[i].getBPM();
			}
			nbpm = timelines[i].getBPM();
		}
		nowbpm = nbpm;
		final int region = (int) (240000 / nbpm / hispeed);
		// double sect = (bpm / 60) * 4 * 1000;
		final float hu = laneregion[0].y + laneregion[0].height;
		final float hl = enableLift ? laneregion[0].y + laneregion[0].height * lift : laneregion[0].y;
		final float rxhs = (hu - hl) * hispeed;
		float y = hl;

		currentduration = Math.round(region * (1 - (enableLanecover ? lanecover : 0)));

		// 判定エリア表示
		if (config.isShowjudgearea()) {
			sprite.end();
			Gdx.gl.glEnable(GL11.GL_BLEND);
			Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			shape.begin(ShapeType.Filled);
			final Color[] color = { Color.valueOf("0000ff20"), Color.valueOf("00ff0020"), Color.valueOf("ffff0020"),
					Color.valueOf("ff800020"), Color.valueOf("00000000"), Color.valueOf("ff000020") };
			final int[][] judgetime = main.getJudgeManager().getJudgeTimeRegion();
			for (int i = pos; i < timelines.length; i++) {
				final TimeLine tl = timelines[i];
				if (tl.getTime() >= time) {
					float rate = (tl.getSection() - (i > 0 ? timelines[i - 1].getSection() : 0))
							/ (tl.getTime() - (i > 0 ? timelines[i - 1].getTime() + timelines[i - 1].getStop() : 0))
							* rxhs;
					for (int j = color.length - 1; j >= 0; j--) {
						shape.setColor(color[j]);
						int nj = j > 0 ? judgetime[j - 1][1] : 0;
						for (int p = 0; p < playerr.length; p++) {
							shape.rect(playerr[p].x, hl + nj * rate, playerr[p].width, (judgetime[j][1] - nj) * rate);
						}
					}
					break;
				}
			}
			shape.end();
			Gdx.gl.glDisable(GL11.GL_BLEND);
			sprite.begin();
		}

		final float orgy = y;
		for (int i = pos; i < timelines.length && y <= hu; i++) {
			final TimeLine tl = timelines[i];
			if (tl.getTime() >= time) {
				if (nbpm > 0) {
					if (i > 0) {
						final TimeLine prevtl = timelines[i - 1];
						if (prevtl.getTime() + prevtl.getStop() > time) {
							y += (tl.getSection() - prevtl.getSection()) * rxhs;
						} else {
							y += (tl.getSection() - prevtl.getSection()) * (tl.getTime() - time)
									/ (tl.getTime() - prevtl.getTime() - prevtl.getStop()) * rxhs;
						}
					} else {
						y += tl.getSection() * (tl.getTime() - time) / tl.getTime() * rxhs;
					}

				}
				if (showTimeline) {
					for (Rectangle r : playerr) {
						if (i > 0 && (tl.getTime() / 1000) > (timelines[i - 1].getTime() / 1000)) {
							sprite.end();
							shape.begin(ShapeType.Line);
							shape.setColor(Color.valueOf("40c0c0"));
							shape.line(r.x, y, r.x + r.width, y);
							shape.end();
							sprite.begin();
							font.setColor(Color.valueOf("40c0c0"));
							font.draw(sprite, String.format("%2d:%02d.%1d", tl.getTime() / 60000,
									(tl.getTime() / 1000) % 60, (tl.getTime() / 100) % 10), r.x + 4, y + 20);
						}
					}
				}

				if (config.isBpmguide() || showTimeline) {
					for (Rectangle r : playerr) {
						if (tl.getBPM() != nbpm) {
							// BPMガイド描画
							sprite.end();
							shape.begin(ShapeType.Line);
							shape.setColor(Color.valueOf("00c000"));
							shape.line(r.x, y + 2, r.x + r.width, y + 2);
							shape.line(r.x, y, r.x + r.width, y);
							shape.line(r.x, y - 2, r.x + r.width, y - 2);
							shape.end();
							sprite.begin();
							font.setColor(Color.valueOf("00c000"));
							font.draw(sprite, "BPM" + ((int) tl.getBPM()), r.x + r.width / 2, y + 20);
						}
						if (tl.getStop() > 0) {
							// STOPガイド描画
							sprite.end();
							shape.begin(ShapeType.Line);
							shape.setColor(Color.valueOf("c0c000"));
							shape.line(r.x, y + 2, r.x + r.width, y + 2);
							shape.line(r.x, y, r.x + r.width, y);
							shape.line(r.x, y - 2, r.x + r.width, y - 2);
							shape.end();
							sprite.begin();
							font.setColor(Color.valueOf("c0c000"));
							font.draw(sprite, "STOP " + ((int) tl.getStop()) + "ms", r.x + r.width / 2, y + 20);
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
									.getSectiontime() >= time)
							|| (config.isShowpastnote() && note instanceof NormalNote && note.getState() == 0))) {
						b = false;
						break;
					}
				}
				if (b) {
					pos = i;
				}
			}
		}

		y = orgy;
		for (int i = pos; i < timelines.length && y <= hu; i++) {
			final TimeLine tl = timelines[i];
			if (tl.getTime() >= time) {
				if (nbpm > 0) {
					if (i > 0) {
						final TimeLine prevtl = timelines[i - 1];
						if (prevtl.getTime() + prevtl.getStop() > time) {
							y += (tl.getSection() - prevtl.getSection()) * rxhs;
						} else {
							y += (tl.getSection() - prevtl.getSection()) * (tl.getTime() - time)
									/ (tl.getTime() - prevtl.getTime() - prevtl.getStop()) * rxhs;
						}
					} else {
						y += tl.getSection() * (tl.getTime() - time) / tl.getTime() * rxhs;
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
						if (tl.getTime() >= time || (config.isShowpastnote() && note.getState() == 0)) {
							final TextureRegion s = config.isMarkprocessednote() && note.getState() != 0
									? pnoteimage[lane] : noteimage[lane];
							sprite.draw(s, laneregion[lane].x, y, laneregion[lane].width, scale);
						}
					} else if (note instanceof LongNote) {
						final LongNote ln = (LongNote) note;
						if (!ln.isEnd() && ln.getPair().getSectiontime() >= time) {
							// if (((LongNote) note).getEnd() == null) {
							// Logger.getGlobal().warning(
							// "LN終端がなく、モデルが正常に表示されません。LN開始時間:"
							// + ((LongNote) note)
							// .getStart()
							// .getTime());
							// } else {
							float dy = 0;
							TimeLine prevtl = tl;
							for (int j = i + 1; j < timelines.length
									&& prevtl.getSection() != ln.getPair().getSection(); j++) {
								final TimeLine nowtl = timelines[j];
								if (nowtl.getTime() >= time) {
									if (prevtl.getTime() + prevtl.getStop() > time) {
										dy += (float) (nowtl.getSection() - prevtl.getSection()) * rxhs;
									} else {
										dy += (nowtl.getSection() - prevtl.getSection()) * (nowtl.getTime() - time)
												/ (nowtl.getTime() - prevtl.getTime()) * rxhs;
									}
								}
								prevtl = nowtl;
							}
							if (dy > 0) {
								this.drawLongNote(laneregion[lane].x, y + dy, laneregion[lane].width,
										y < laneregion[lane].y ? y - laneregion[lane].y : dy, scale, lane, ln);
							}
							// System.out.println(dy);
						}
					} else if (note instanceof MineNote) {
						// draw mine note
						if (tl.getTime() >= time) {
							final TextureRegion s = mnoteimage[lane];
							sprite.draw(s, laneregion[lane].x, y, laneregion[lane].width, scale);
						}
					}
				}
				// hidden note
				if (config.isShowhiddennote() && tl.getTime() >= time) {
					final Note hnote = tl.getHiddenNote(lane);
					if (hnote != null) {
						sprite.draw(hnoteimage[lane], laneregion[lane].x, y, laneregion[lane].width, scale);
					}
				}
			}
		}
		// System.out.println("time :" + ltime + " y :" + yy + " real time : "
		// + (ltime * (hu - hl) / yy));
	}

	public double getNowBPM() {
		return nowbpm;
	}

	final private void drawLongNote(float x, float y, float width, float height, float scale, int lane, LongNote ln) {
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
		if (shape != null) {
			shape.dispose();
			shape = null;
		}
	}
}
