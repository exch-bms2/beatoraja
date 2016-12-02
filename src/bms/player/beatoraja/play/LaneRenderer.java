package bms.player.beatoraja.play;

import java.util.HashMap;
import java.util.Map;

import bms.player.beatoraja.*;
import org.lwjgl.opengl.GL11;

import bms.model.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.skin.SkinImage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

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

	private long lanecovertiming;
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
	private final ShapeRenderer shape;
	private final BitmapFont font;
	private final PlaySkin skin;

	private final Config config;
	private PlayConfig playconfig;
	private final int auto;

	private boolean hschanged;
	private long startpressedtime;
	private boolean startpressed;
	private boolean cursorpressed;

	private boolean enableControl = true;

	/**
	 * ボムの表示開始時間
	 */
	private int[] judge;
	/**
	 * 現在表示中の判定
	 */
	private int[] judgenow;
	/**
	 * 判定の最終更新時間
	 */
	private int[] judgenowt;

	private int[] judgecombo;

	private int[] laneassign;

	private int currentduration;

	public LaneRenderer(BMSPlayer main, SpriteBatch sprite, ShapeRenderer shape, BitmapFont font, PlaySkin skin,
			PlayerResource resource, BMSModel model, int[] mode) {

		this.main = main;
		this.sprite = sprite;
		this.shape = shape;
		this.font = font;
		this.skin = skin;
		this.config = resource.getConfig();
		auto = resource.getAutoplay();
		this.playconfig = (model.getUseKeys() == 5 || model.getUseKeys() == 7 ? config.getMode7()
				: (model.getUseKeys() == 10 || model.getUseKeys() == 14 ? config.getMode14() : config.getMode9()));
		this.enableLanecover = playconfig.isEnablelanecover();
		this.enableLift = playconfig.isEnablelift();
		this.lift = playconfig.getLift();
		this.fixhispeed = config.getFixhispeed();
		this.gvalue = playconfig.getDuration();
		hispeed = playconfig.getHispeed();
		init(model);
		this.setLanecover(playconfig.getLanecover());
		if (this.fixhispeed != Config.FIX_HISPEED_OFF) {
			basehispeed = hispeed;
		}

		for (int i : mode) {
			if (i == TableData.NO_HISPEED) {
				enableControl = false;
				hispeed = 1.0f;
				lanecover = 0;
				lift = 0;
			}
		}
	}

	public void init(BMSModel model) {
		pos = 0;
		judge = new int[20];
		judgenow = new int[skin.getJudgeregion().length];
		judgenowt = new int[skin.getJudgeregion().length];
		judgecombo = new int[skin.getJudgeregion().length];

		this.model = model;
		this.timelines = model.getAllTimeLines();
		if (model.getUseKeys() == 9) {
			laneassign = new int[] { 0, 1, 2, 3, 4, 10, 11, 12, 13 };
		} else {
			laneassign = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16 };
		}
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

	private double basebpm;
	private double nowbpm;

	public void drawLane(TextureRegion[] noteimage, TextureRegion[][] lnoteimage, TextureRegion[] mnoteimage,
			TextureRegion[] pnoteimage, TextureRegion[] hnoteimage, float scale) {
		sprite.end();
		long time = (main.getTimer()[TIMER_PLAY] != Long.MIN_VALUE ? (main.getNowTime() - main.getTimer()[TIMER_PLAY])
				: 0) + config.getJudgetiming();
		if (main.getState() == BMSPlayer.STATE_PRACTICE) {
			 time = main.getPracticeConfiguration().getPracticeProperty().starttime;
			 pos = 0;
		}
		final float hispeed = main.getState() != BMSPlayer.STATE_PRACTICE ? this.hispeed : 1.0f;
		JudgeManager judge = main.getJudgeManager();
		final Rectangle[] laneregion = skin.getLaneregion();
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

		final boolean[] keystate = main.getBMSPlayerInputProcessor().getKeystate();
		for (int lane = 0; lane < laneregion.length; lane++) {
			// キービームフラグON/OFF
			if (model.getUseKeys() == 9) {
				if (keystate[lane]) {
					if (main.getTimer()[TIMER_KEYON_1P_KEY1 + lane] == Long.MIN_VALUE) {
						main.getTimer()[TIMER_KEYON_1P_KEY1 + lane] = main.getNowTime();
						main.getTimer()[TIMER_KEYOFF_1P_KEY1 + lane] = Long.MIN_VALUE;
					}
				} else {
					if (main.getTimer()[TIMER_KEYOFF_1P_KEY1 + lane] == Long.MIN_VALUE) {
						main.getTimer()[TIMER_KEYOFF_1P_KEY1 + lane] = main.getNowTime();
						main.getTimer()[TIMER_KEYON_1P_KEY1 + lane] = Long.MIN_VALUE;
					}
				}
				if (judge.getProcessingLongNotes()[lane] != null) {
					if (main.getTimer()[TIMER_HOLD_1P_KEY1 + lane] == Long.MIN_VALUE) {
						main.getTimer()[TIMER_HOLD_1P_KEY1 + lane] = main.getNowTime();
					}
				} else {
					main.getTimer()[TIMER_HOLD_1P_KEY1 + lane] = Long.MIN_VALUE;
				}
			} else {
				int key = (model.getUseKeys() > 9 && lane >= 8 ? lane + 1 : lane);
				int offset = (lane % 8 == 7 ? -1 : (lane % 8)) + (lane >= 8 ? 10 : 0);
				if (keystate[key] || (key == 7 && keystate[8]) || (key == 16 && keystate[17])) {
					if (main.getTimer()[TIMER_KEYON_1P_KEY1 + offset] == Long.MIN_VALUE) {
						main.getTimer()[TIMER_KEYON_1P_KEY1 + offset] = main.getNowTime();
						main.getTimer()[TIMER_KEYOFF_1P_KEY1 + offset] = Long.MIN_VALUE;
					}
				} else {
					if (main.getTimer()[TIMER_KEYOFF_1P_KEY1 + offset] == Long.MIN_VALUE) {
						main.getTimer()[TIMER_KEYOFF_1P_KEY1 + offset] = main.getNowTime();
						main.getTimer()[TIMER_KEYON_1P_KEY1 + offset] = Long.MIN_VALUE;
					}
				}
				if (judge.getProcessingLongNotes()[lane] != null) {
					if (main.getTimer()[TIMER_HOLD_1P_KEY1 + offset] == Long.MIN_VALUE) {
						main.getTimer()[TIMER_HOLD_1P_KEY1 + offset] = main.getNowTime();
					}
				} else {
					main.getTimer()[TIMER_HOLD_1P_KEY1 + offset] = Long.MIN_VALUE;
				}
			}
		}

		// 各種コントロール入力判定
		// TODO ここで各種コントロール入力判定をやるべきではないかも
		if (enableControl) {
			final BMSPlayerInputProcessor input = main.getBMSPlayerInputProcessor();
			if (input.getCursorState()[0]) {
				if (!cursorpressed) {
					this.setLanecover(lanecover - 0.01f);
					cursorpressed = true;
				}
			} else if (input.getCursorState()[1]) {
				if (!cursorpressed) {
					this.setLanecover(lanecover + 0.01f);
					cursorpressed = true;
				}
			} else {
				cursorpressed = false;
			}
			// move lane cover by mouse wheel
			if (input.getScroll() != 0) {
				this.setLanecover(lanecover - input.getScroll() * 0.005f);
				input.resetScroll();
			}
			if (input.startPressed()) {
				if (auto == 0) {
					// change hi speed by START + Keys
					boolean[] key = input.getKeystate();
					if (key[0] || key[2] || key[4] || key[6]) {
						if (!hschanged) {
							changeHispeed(false);
							hschanged = true;
						}
					} else if (key[1] || key[3] || key[5]) {
						if (!hschanged) {
							changeHispeed(true);
							hschanged = true;
						}
					} else {
						hschanged = false;
					}

					// move lane cover by START + Scratch
					if (keystate[7] | keystate[8]) {
						long l = System.currentTimeMillis();
						if (l - lanecovertiming > 50) {
							this.setLanecover(lanecover + (keystate[7] ? 0.001f : -0.001f));
							lanecovertiming = l;
						}
					}
				}
				// show-hide lane cover by double-press START
				if (!startpressed) {
					long stime = System.currentTimeMillis();
					if (stime < startpressedtime + 500) {
						setEnableLanecover(!isEnableLanecover());
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

		// 判定エリア表示
		if (config.isShowjudgearea()) {
			Gdx.gl.glEnable(GL11.GL_BLEND);
			Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			shape.begin(ShapeType.Filled);
			final Color[] color = { Color.valueOf("0000ff20"), Color.valueOf("00ff0020"), Color.valueOf("ffff0020"),
					Color.valueOf("ff800020"), Color.valueOf("00000000"), Color.valueOf("ff000020") };
			final int[] judgetime = judge.getJudgeTimeRegion();
			for (int i = pos; i < timelines.length; i++) {
				final TimeLine tl = timelines[i];
				if (tl.getTime() >= time) {
					float rate = (tl.getSection() - (i > 0 ? timelines[i - 1].getSection() : 0))
							/ (tl.getTime() - (i > 0 ? timelines[i - 1].getTime() + timelines[i - 1].getStop() : 0))
							* rxhs;
					for (int j = color.length - 1; j >= 0; j--) {
						shape.setColor(color[j]);
						int nj = j > 0 ? judgetime[j - 1] : 0;
						for (int p = 0; p < playerr.length; p++) {
							shape.rect(playerr[p].x, hl + nj * rate, playerr[p].width, (judgetime[j] - nj) * rate);
						}
					}
					break;
				}
			}
			shape.end();
			Gdx.gl.glDisable(GL11.GL_BLEND);
		}

		sprite.begin();
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

					// if(y < 0) {
					// System.out.println(" y : " + y + " line : " + (i > 0 ?
					// timelines[i]
					// .getTime() : 0) + " time : " + time + " stop : " + (i > 0
					// ? timelines[i - 1]
					// .getStop() : 0));
					// }
				}
				// ltime = tl.getTime() - time;
				// yy = y - hl;
				for (Rectangle r : playerr) {
					if (config.isBpmguide()) {
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
					final Note note = tl.getNote(laneassign[lane]);
					if (note != null
							&& ((note instanceof LongNote && ((LongNote) note).getEndnote().getSectiontime() >= time) || (config
									.isShowpastnote() && note instanceof NormalNote && note.getState() == 0))) {
						b = false;
						break;
					}
				}
				if (b) {
					pos = i;
				}
			}
			// ノート描画
			for (int lane = 0; lane < laneregion.length; lane++) {
				final Note note = tl.getNote(laneassign[lane]);
				if (note != null) {
					if (note instanceof NormalNote) {
						// draw normal note
						if (tl.getTime() >= time || (config.isShowpastnote() && note.getState() == 0)) {
							final TextureRegion s = config.isMarkprocessednote() && note.getState() != 0 ? pnoteimage[lane]
									: noteimage[lane];
							sprite.draw(s, laneregion[lane].x, y, laneregion[lane].width, scale);
						}
					} else if (note instanceof LongNote) {
						final LongNote ln = (LongNote) note;
						if (ln.getSection() == tl.getSection() && ln.getEndnote().getSectiontime() >= time) {
							// if (((LongNote) note).getEnd() == null) {
							// Logger.getGlobal().warning(
							// "LN終端がなく、モデルが正常に表示されません。LN開始時間:"
							// + ((LongNote) note)
							// .getStart()
							// .getTime());
							// } else {
							float dy = 0;
							for (int j = 0; timelines[i + j].getSection() != ln.getEndnote().getSection(); j++) {
								if (timelines[i + j + 1].getTime() >= time) {
									if (timelines[i + j].getTime() + timelines[i + j].getStop() > time) {
										dy += (float) (timelines[i + j + 1].getSection() - timelines[i + j]
												.getSection()) * rxhs;
									} else {
										dy += (timelines[i + j + 1].getSection() - timelines[i + j].getSection())
												* (timelines[i + j + 1].getTime() - time)
												/ (timelines[i + j + 1].getTime() - timelines[i + j].getTime()) * rxhs;
									}
								}
							}
							if (dy > 0) {
								this.drawLongNote(laneregion[lane].x, y + dy, laneregion[lane].width, dy, scale, lane,
										ln, lnoteimage);
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
					final Note hnote = tl.getHiddenNote(laneassign[lane]);
					if (hnote != null) {
						sprite.draw(hnoteimage[lane], laneregion[lane].x, y, laneregion[lane].width, scale);
					}
				}
			}
		}
		sprite.end();

		// System.out.println("time :" + ltime + " y :" + yy + " real time : "
		// + (ltime * (hu - hl) / yy));

		// 判定文字描画。描画座標等はリフト量によって可変のためSkin移行は特殊な定義が必要
		sprite.begin();
		for (int jr = 0; jr < skin.getJudgeregion().length; jr++) {
			if (judgenow[jr] > 0 && time < judgenowt[jr] + 500) {
				final Rectangle r = skin.getJudgeregion()[jr].judge[judgenow[jr] - 1].getDestination(main.getNowTime(),
						main);
				if (r != null) {
					int shift = 0;
					if (judgenow[jr] < 4) {
						final Rectangle nr = skin.getJudgeregion()[jr].count[judgenow[jr] - 1].getDestination(
								main.getNowTime(), main);
						if (nr != null) {
							TextureRegion[] ntr = skin.getJudgeregion()[jr].count[judgenow[jr] - 1].getValue(
									main.getNowTime(), judgecombo[jr], 0, main);
							int index = 0;
							int length = 0;
							for (; index < ntr.length && ntr[index] == null; index++)
								;
							for (int i = 0; i < ntr.length; i++) {
								if (ntr[i] != null) {
									length++;
								}
							}
							shift = (int) (length * nr.width / 2);
							// コンボカウント描画
							for (int i = index; i < index + length; i++) {
								if (ntr[i] != null) {
									sprite.draw(ntr[i], r.x + nr.x + (i - index) * nr.width - shift, r.y + nr.y,
											nr.width, nr.height);
								}
							}
						}
					}
					sprite.draw(skin.getJudgeregion()[jr].judge[judgenow[jr] - 1].getImage(main.getNowTime(), main),
							r.x - (skin.getJudgeregion()[jr].shift ? shift : 0), r.y, r.width, r.height);
					// FAST, SLOW描画
					if (config.getJudgedetail() == 1) {
						if (judgenow[jr] > 1) {
							font.setColor(judge.getRecentJudgeTiming() >= 0 ? Color.CYAN : Color.RED);
							font.draw(sprite, judge.getRecentJudgeTiming() >= 0 ? "EARLY" : "LATE", r.x + r.width / 2,
									r.y + r.height + 20);
						}

					} else if (config.getJudgedetail() == 2) {
						if (judgenow[jr] > 0) {
							if (judgenow[jr] == 1) {
								font.setColor(judge.getRecentJudgeTiming() >= 0 ? Color.SKY : Color.PINK);
							} else {
								font.setColor(judge.getRecentJudgeTiming() >= 0 ? Color.BLUE : Color.RED);
							}
							font.draw(sprite,
									(judge.getRecentJudgeTiming() >= 0 ? "+" : "") + judge.getRecentJudgeTiming()
											+ " ms", r.x + r.width / 2, r.y + r.height + 20);
						}
					}
				}
			}
		}
		sprite.end();
		sprite.begin();
	}

	public double getNowBPM() {
		return nowbpm;
	}

	final private void drawLongNote(float x, float y, float width, float height, float scale, int lane, LongNote ln,
			TextureRegion[][] longnote) {
		if ((model.getLntype() == BMSModel.LNTYPE_HELLCHARGENOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
				|| ln.getType() == LongNote.TYPE_HELLCHARGENOTE) {
			// HCN
			if (y - height < skin.getLaneregion()[lane].y) {
				height = y - skin.getLaneregion()[lane].y;
			}
			final JudgeManager judge = main.getJudgeManager();
			TextureRegion le = longnote[5][lane];
			if (main.getJudgeManager().getProcessingLongNotes()[lane] == ln) {
				sprite.draw(longnote[6][lane], x, y - height + le.getRegionHeight(), width,
						height - le.getRegionHeight());
			} else if (judge.getPassingLongNotes()[lane] == ln && ln.getState() != 0) {
				sprite.draw(longnote[judge.getHellChargeJudges()[lane] ? 8 : 9][lane], x,
						y - height + le.getRegionHeight(), width, height - le.getRegionHeight());
			} else {
				sprite.draw(longnote[7][lane], x, y - height + le.getRegionHeight(), width,
						height - le.getRegionHeight());
			}
			TextureRegion ls = longnote[4][lane];
			sprite.draw(ls, x, y, width, scale);
			sprite.draw(le, x, y - height, width, scale);
		}
		if ((model.getLntype() == BMSModel.LNTYPE_CHARGENOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
				|| ln.getType() == LongNote.TYPE_CHARGENOTE) {
			// CN
			if (y - height < skin.getLaneregion()[lane].y) {
				height = y - skin.getLaneregion()[lane].y;
			}
			TextureRegion le = longnote[1][lane];
			if (main.getJudgeManager().getProcessingLongNotes()[lane] == ln) {
				sprite.draw(longnote[2][lane], x, y - height + le.getRegionHeight(), width,
						height - le.getRegionHeight());
			} else {
				sprite.draw(longnote[3][lane], x, y - height + le.getRegionHeight(), width,
						height - le.getRegionHeight());
			}
			TextureRegion ls = longnote[0][lane];
			sprite.draw(ls, x, y, width, scale);
			sprite.draw(le, x, y - height, width, scale);
		}
		if ((model.getLntype() == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
				|| ln.getType() == LongNote.TYPE_LONGNOTE) {
			// LN
			if (y - height < skin.getLaneregion()[lane].y) {
				height = y - skin.getLaneregion()[lane].y;
			}
			final TextureRegion le = longnote[1][lane];
			if (main.getJudgeManager().getProcessingLongNotes()[lane] == ln) {
				sprite.draw(longnote[2][lane], x, y - height + le.getRegionHeight(), width,
						height - le.getRegionHeight());
			} else {
				sprite.draw(longnote[3][lane], x, y - height + le.getRegionHeight(), width,
						height - le.getRegionHeight());
			}
			sprite.draw(le, x, y - height, width, scale);
		}
	}

	public void update(int lane, int judge, int time, int fast) {
		main.getTimer()[TIMER_JUDGE_1P] = main.getNowTime();
		main.getTimer()[TIMER_JUDGE_2P] = main.getNowTime();
		if (judge < 2) {
			if (model.getUseKeys() == 9) {
				main.getTimer()[TIMER_BOMB_1P_KEY1 + lane] = main.getNowTime();
			} else {
				int offset = (lane % 8 == 7 ? -1 : (lane % 8)) + (lane >= 8 ? 10 : 0);
				main.getTimer()[TIMER_BOMB_1P_KEY1 + offset] = main.getNowTime();
			}

		}
		if (model.getUseKeys() == 9) {
			this.judge[lane + 1] = judge == 0 ? 1 : judge * 2 + (fast > 0 ? 0 : 1);
		} else {
			int offset = (lane % 8 == 7 ? -1 : (lane % 8)) + (lane >= 8 ? 10 : 0);
			this.judge[offset + 1] = judge == 0 ? 1 : judge * 2 + (fast > 0 ? 0 : 1);
		}
		if (judgenow.length > 0) {
			judgenow[lane / (skin.getLaneregion().length / judgenow.length)] = judge + 1;
			judgenowt[lane / (skin.getLaneregion().length / judgenow.length)] = time;
			judgecombo[lane / (skin.getLaneregion().length / judgenow.length)] = main.getJudgeManager()
					.getCourseCombo();
		}
	}

	public int[] getJudge() {
		return judge;
	}

	public void setEnableControlInput(boolean enableControl) {
		this.enableControl = enableControl;
	}
}
