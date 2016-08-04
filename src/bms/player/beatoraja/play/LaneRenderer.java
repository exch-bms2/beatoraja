package bms.player.beatoraja.play;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import bms.model.*;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.TableData;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

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

	private BMSPlayer main;

	private SpriteBatch sprite;
	private ShapeRenderer shape;
	private BitmapFont font;
	private PlaySkin skin;

	private Config config;
	private int auto;

	private boolean drawline = true;

	private boolean hschanged;
	private long startpressedtime;
	private boolean startpressed;
	private boolean cursorpressed;

	private boolean enableControl = true;

	private final float dw;
	private final float dh;

	/**
	 * ボムの表示開始時間
	 */
	private long[] bomb;
	private int[] bombtype;
	/**
	 * 現在表示中の判定
	 */
	private int[] judgenow;
	/**
	 * 判定の最終更新時間
	 */
	private int[] judgenowt;

	private int[] judgecombo;

	public LaneRenderer(BMSPlayer main, SpriteBatch sprite, ShapeRenderer shape, BitmapFont font, PlaySkin skin,
			PlayerResource resource, BMSModel model, int[] mode) {
		dw = MainController.RESOLUTION[resource.getConfig().getResolution()].width / 1280f;
		dh = MainController.RESOLUTION[resource.getConfig().getResolution()].height / 720f;
		bomb = new long[skin.getLaneregion().length];
		bombtype = new int[skin.getLaneregion().length];
		judgenow = new int[skin.getJudgeregion().length];
		judgenowt = new int[skin.getJudgeregion().length];
		judgecombo = new int[skin.getJudgeregion().length];

		Arrays.fill(bomb, -1000);
		this.main = main;
		this.sprite = sprite;
		this.shape = shape;
		this.font = font;
		this.skin = skin;
		this.config = resource.getConfig();
		auto = resource.getAutoplay();
		this.enableLanecover = config.isEnablelanecover();
		this.enableLift = config.isEnablelift();
		this.lift = config.getLift();
		this.fixhispeed = config.getFixhispeed();
		this.gvalue = config.getGreenvalue();
		this.model = model;
		this.timelines = model.getAllTimeLines();
		if (model.getUseKeys() == 9) {
			drawline = false;
		}
		hispeed = config.getHispeed();
		switch (config.getFixhispeed()) {
		case Config.FIX_HISPEED_OFF:
			break;
		case Config.FIX_HISPEED_STARTBPM:
			basebpm = model.getBpm();
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
		this.setLanecover(config.getLanecover());
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

	public int getFixHispeed() {
		return fixhispeed;
	}

	public float getHispeed() {
		return hispeed;
	}

	public int getGreenValue() {
		return gvalue;
	}

	public float getLaneCoverRegion() {
		return lanecover;
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
			hispeed = (float) ((2400f / (basebpm / 100) / gvalue) * 0.6 * (1 - (enableLanecover ? lanecover : 0)));
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

	public void drawLane() {
		sprite.end();
		long time = main.getPlayTime() != 0 ? (System.currentTimeMillis() - main.getPlayTime()) : 0;
		time += config.getJudgetiming();
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
		int region = (int) (240000 / nbpm / hispeed);
		// double sect = (bpm / 60) * 4 * 1000;
		float hu = laneregion[0].y + laneregion[0].height;
		float hl = laneregion[0].y;

		// リフト描画
		if (enableLift) {
			for (int p = 0; p < playerr.length; p++) {
				sprite.begin();
				sprite.draw(skin.getLanecover(), playerr[p].x, hl, playerr[p].width, (hu - hl) * lift);
				// 緑数字、白数字描画
				if (main.getBMSPlayerInputProcessor().startPressed()) {
					font.setColor(Color.WHITE);
					font.draw(sprite, String.format("%5d", Math.round(lift * 1000)), playerr[p].x + playerr[p].width
							* 0.25f, hl + (hu - hl) * lift);
					font.setColor(Color.GREEN);
					font.draw(sprite,
							String.format("%5d", Math.round(region * 0.6 * (1 - (enableLanecover ? lanecover : 0)))),
							playerr[p].x + playerr[p].width * 0.75f, hl + (hu - hl) * lift);
				}
				sprite.end();
			}
			hl = hl + (hu - hl) * lift;
		}
		// 判定ライン描画
		for (int p = 0; p < playerr.length; p++) {
			shape.begin(ShapeType.Filled);
			shape.setColor(Color.RED);
			shape.rect(playerr[p].x, hl - 3, playerr[p].width, 6);

			shape.end();

		}

		boolean[] keystate = main.getBMSPlayerInputProcessor().getKeystate();
		for (int lane = 0; lane < laneregion.length; lane++) {
			// キービームフラグON/OFF
			sprite.begin();
			if (model.getUseKeys() == 9) {
				if (keystate[lane]) {
					if(main.getTimer()[BMSPlayer.TIMER_KEYON_1P_KEY1 + lane] == -1) {
						main.getTimer()[BMSPlayer.TIMER_KEYON_1P_KEY1 + lane] = main.getNowTime();
						main.getTimer()[BMSPlayer.TIMER_KEYOFF_1P_KEY1 + lane] = -1;
					}
				} else {
					if(main.getTimer()[BMSPlayer.TIMER_KEYOFF_1P_KEY1 + lane] == -1) {
						main.getTimer()[BMSPlayer.TIMER_KEYOFF_1P_KEY1 + lane] = main.getNowTime();
						main.getTimer()[BMSPlayer.TIMER_KEYON_1P_KEY1 + lane] = -1;
					}
				}
			} else {
				int key = (model.getUseKeys() > 9 && lane >= 8 ? lane + 1 : lane);
				if (keystate[key] || (key == 7 && keystate[8]) || (key == 16 && keystate[17])) {
					int offset = (lane % 8 == 7 ? -1 : (lane % 8)) + (lane >= 8 ? 10 : 0);
					if (main.getTimer()[BMSPlayer.TIMER_KEYON_1P_KEY1 + offset] == -1) {
						main.getTimer()[BMSPlayer.TIMER_KEYON_1P_KEY1 + offset] = main.getNowTime();
						main.getTimer()[BMSPlayer.TIMER_KEYOFF_1P_KEY1 + offset] = -1;
					}
				} else {
					int offset = (lane % 8 == 7 ? -1 : (lane % 8)) + (lane >= 8 ? 10 : 0);
					if (main.getTimer()[BMSPlayer.TIMER_KEYOFF_1P_KEY1 + offset] == -1) {
						main.getTimer()[BMSPlayer.TIMER_KEYOFF_1P_KEY1 + offset] = main.getNowTime();
						main.getTimer()[BMSPlayer.TIMER_KEYON_1P_KEY1 + offset] = -1;
					}
				}
			}
			sprite.end();
		}

		// 各種コントロール入力判定
		// TODO ここで各種コントロール入力判定をやるべきではないかも
		if (enableControl) {
			BMSPlayerInputProcessor input = main.getBMSPlayerInputProcessor();
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

		float y = hl;

		// long ltime = 0;
		// float yy = 0;

		// 判定エリア表示
		if (config.isShowjudgearea()) {
			Gdx.gl.glEnable(GL11.GL_BLEND);
			Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			shape.begin(ShapeType.Filled);
			final Color[] color = { Color.valueOf("0000ff20"), Color.valueOf("00ff0020"), Color.valueOf("ffff0020"),
					Color.valueOf("ff800020"), Color.valueOf("00000000"), Color.valueOf("ff000020") };
			int[] judgetime = judge.getJudgeTimeRegion();
			for (int i = pos; i < timelines.length && y <= hu; i++) {
				TimeLine tl = timelines[i];
				if (tl.getTime() >= time) {
					float rate = (float) (timelines[i].getSection() - (i > 0 ? timelines[i - 1].getSection() : 0))
							/ (timelines[i].getTime() - (i > 0 ? timelines[i - 1].getTime()
									+ timelines[i - 1].getStop() : 0)) * (hu - hl) * hispeed;
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

		for (int i = pos; i < timelines.length && y <= hu; i++) {
			TimeLine tl = timelines[i];
			if (tl.getTime() >= time) {
				if (nbpm > 0) {
					if ((i > 0 && timelines[i - 1].getTime() + timelines[i - 1].getStop() > time)) {
						y += (float) (timelines[i].getSection() - timelines[i - 1].getSection()) * (hu - hl) * hispeed;
					} else {
						y += (float) (timelines[i].getSection() - (i > 0 ? timelines[i - 1].getSection() : 0))
								* (timelines[i].getTime() - time)
								/ (timelines[i].getTime() - (i > 0 ? timelines[i - 1].getTime()
										+ timelines[i - 1].getStop() : 0)) * (hu - hl) * hispeed;
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
				for (int p = 0; p < playerr.length; p++) {
					if (config.isBpmguide()) {
						if(tl.getBPM() != nbpm) {
							// BPMガイド描画
							shape.begin(ShapeType.Line);
							shape.setColor(Color.valueOf("00c000"));
							shape.line(playerr[p].x, y + 2, playerr[p].x + playerr[p].width, y + 2);
							shape.line(playerr[p].x, y, playerr[p].x + playerr[p].width, y);
							shape.line(playerr[p].x, y - 2, playerr[p].x + playerr[p].width, y - 2);
							shape.end();
							sprite.begin();
							font.setColor(Color.valueOf("00c000"));
							font.draw(sprite, "BPM" + ((int) tl.getBPM()), playerr[p].x + playerr[p].width / 2, y + 20);
							sprite.end();
						}
						if(tl.getStop() > 0) {
							// STOPガイド描画
							shape.begin(ShapeType.Line);
							shape.setColor(Color.valueOf("c0c000"));
							shape.line(playerr[p].x, y + 2, playerr[p].x + playerr[p].width, y + 2);
							shape.line(playerr[p].x, y, playerr[p].x + playerr[p].width, y);
							shape.line(playerr[p].x, y - 2, playerr[p].x + playerr[p].width, y - 2);
							shape.end();
							sprite.begin();
							font.setColor(Color.valueOf("c0c000"));
							font.draw(sprite, "STOP " + ((int) tl.getStop()) + "ms", playerr[p].x + playerr[p].width / 2, y + 20);
							sprite.end();
						}
					}
					// 小節線描画
					if (drawline && tl.getSectionLine()) {
						shape.begin(ShapeType.Line);
						shape.setColor(Color.GRAY);
						shape.line(playerr[p].x, y, playerr[p].x + playerr[p].width, y);
						shape.end();
					}
				}
				nbpm = tl.getBPM();
			} else if (pos == i - 1) {
				boolean b = true;
				for (int lane = 0; lane < laneregion.length; lane++) {
					Note note = tl.getNote(model.getUseKeys() == 9 && lane >= 5 ? lane + 5 : (model.getUseKeys() > 9
							&& lane >= 8 ? lane + 1 : lane));
					if (note != null && note instanceof LongNote && ((LongNote) note).getEnd().getTime() >= time) {
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
				Note note = tl.getNote(model.getUseKeys() == 9 && lane >= 5 ? lane + 5 : (model.getUseKeys() > 9
						&& lane >= 8 ? lane + 1 : lane));
				if (note != null) {
					float dy = 1;
					if (note instanceof LongNote) {
						if (((LongNote) note).getStart() == tl && ((LongNote) note).getEnd().getTime() >= time) {
							// if (((LongNote) note).getEnd() == null) {
							// Logger.getGlobal().warning(
							// "LN終端がなく、モデルが正常に表示されません。LN開始時間:"
							// + ((LongNote) note)
							// .getStart()
							// .getTime());
							// } else {
							dy = 0;
							for (int j = 0; timelines[i + j] != ((LongNote) note).getEnd(); j++) {
								if (timelines[i + j + 1].getTime() >= time) {
									if (timelines[i + j].getTime() + timelines[i + j].getStop() > time) {
										dy += (float) (timelines[i + j + 1].getSection() - timelines[i + j]
												.getSection()) * (hu - hl) * hispeed;
									} else {
										dy += (timelines[i + j + 1].getSection() - timelines[i + j].getSection())
												* (timelines[i + j + 1].getTime() - time)
												/ (timelines[i + j + 1].getTime() - timelines[i + j].getTime())
												* (hu - hl) * hispeed;
									}
								}
							}
							// System.out.println(dy);
						} else {
							dy = 0;
						}
					} else {
						if (timelines[i].getTime() < time) {
							dy = 0;
						}
					}
					if (dy > 0) {
						this.drawNote(laneregion[lane].x, y + dy, laneregion[lane].width, dy, 2.0f, lane, note);
					}
				}
				// hidden note
				if (config.isShowhiddennote()) {
					Note hnote = tl.getHiddenNote(model.getUseKeys() == 9 && lane >= 5 ? lane + 5
							: (model.getUseKeys() > 9 && lane >= 8 ? lane + 1 : lane));
					if (hnote != null) {
						shape.begin(ShapeType.Line);
						shape.setColor(Color.ORANGE);
						shape.rect(laneregion[lane].x, y - 4, laneregion[lane].width, 8);
						shape.end();
					}
				}
			}
		}

		// System.out.println("time :" + ltime + " y :" + yy + " real time : "
		// + (ltime * (hu - hl) / yy));

		// ボム描画。描画座標等はリフト量によって可変のためSkin移行は特殊な定義が必要
		sprite.begin();
		// sprite.enableBlending();
		sprite.setBlendFunction(GL11.GL_ONE, GL11.GL_ONE);
		for (int lane = 0; lane < laneregion.length; lane++) {
			if (time >= bomb[lane]) {
				if (judge.getProcessingLongNotes()[lane] != null) {
					sprite.draw(skin.getBomb()[3].getImage(time - bomb[lane]), laneregion[lane].x
							+ laneregion[lane].width / 2 - 110 * dw, hl - 155 * dh, 260 * dw, 270 * dh);
				} else if (time <= bomb[lane] + 150) {
					sprite.draw(skin.getBomb()[bombtype[lane]].getImage(time - bomb[lane]), laneregion[lane].x
							+ laneregion[lane].width / 2 - 110 * dw, hl - 155 * dh, 260 * dw, 270 * dh);
				}
			}
		}
		sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		// sprite.disableBlending();

		// 判定文字描画。描画座標等はリフト量によって可変のためSkin移行は特殊な定義が必要
		for (int jr = 0; jr < skin.getJudgeregion().length; jr++) {
			if (judgenow[jr] > 0 && time < judgenowt[jr] + 500) {
				Rectangle r = skin.getJudgeregion()[jr].judge[judgenow[jr] - 1].getDestination(time);
				int shift = 0;
				if (judgenow[jr] < 4) {
					Rectangle nr = skin.getJudgeregion()[jr].count[judgenow[jr] - 1].getDestination(time);
					TextureRegion[] ntr = skin.getJudgeregion()[jr].count[judgenow[jr] - 1].getValue(judgecombo[jr], 0);
					int index = 0;
					for (; index < ntr.length && ntr[index] == null; index++)
						;
					if (skin.getJudgeregion()[jr].shift) {
						shift = (int) ((ntr.length - index) * nr.width / 2);
					}
					for (int i = index; i < ntr.length; i++) {
						sprite.draw(ntr[i], r.x + nr.x + (i - index) * nr.width - (ntr.length - index) * nr.width / 2,
								r.y + nr.y, nr.width, nr.height);
					}
				}
				sprite.draw(skin.getJudgeregion()[jr].judge[judgenow[jr] - 1].getImage(time), r.x - shift, r.y,
						r.width, r.height);
				// FAST, SLOW描画
				if (config.getJudgedetail() == 1) {
					if (judgenow[jr] > 1) {

						font.setColor(judge.getRecentJudgeTiming() >= 0 ? Color.CYAN : Color.RED);
						font.draw(sprite, judge.getRecentJudgeTiming() >= 0 ? "EARLY" : "LATE", r.x + r.width / 2, r.y
								+ r.height + 20);
					}

				} else if (config.getJudgedetail() == 2) {
					if (judgenow[jr] > 0) {

						if (judgenow[jr] == 1) {
							font.setColor(judge.getRecentJudgeTiming() >= 0 ? Color.SKY : Color.PINK);
						} else {
							font.setColor(judge.getRecentJudgeTiming() >= 0 ? Color.BLUE : Color.RED);
						}
						font.draw(sprite, (judge.getRecentJudgeTiming() >= 0 ? "+" : "") + judge.getRecentJudgeTiming()
								+ " ms", r.x + r.width / 2, r.y + r.height + 20);
					}

				}
			}
		}

		// レーンカバー描画
		for (int p = 0; p < playerr.length; p++) {
			if (enableLanecover) {
				sprite.draw(skin.getLanecover(), playerr[p].x, hl + (hu - hl) * (1 - lanecover), playerr[p].width,
						(hu - hl));
			}
			// 緑数字、白数字描画
			if (main.getBMSPlayerInputProcessor().startPressed()) {
				font.setColor(Color.WHITE);
				font.draw(sprite, String.format("%5d", Math.round(lanecover * 1000)), playerr[p].x + playerr[p].width
						* 0.25f, hl + (hu - hl) * (enableLanecover ? (1 - lanecover) : 1));
				font.setColor(Color.GREEN);
				font.draw(sprite,
						String.format("%5d", Math.round(region * 0.6 * (1 - (enableLanecover ? lanecover : 0)))),
						playerr[p].x + playerr[p].width * 0.75f, hl + (hu - hl)
								* (enableLanecover ? (1 - lanecover) : 1));
			}
		}
		sprite.end();
		sprite.begin();
	}

	public double getNowBPM() {
		return nowbpm;
	}

	private void drawNote(float x, float y, float width, float height, float scale, int lane, Note note) {
		if (note instanceof NormalNote) {
			final Sprite s = skin.getNote()[lane];
			if (config.isMarkprocessednote() && note.getState() != 0) {
				// 処理済みノートの描画
				shape.begin(ShapeType.Line);
				shape.setColor(Color.CYAN);
				shape.rect(x, y - s.getHeight() * scale / 2, width, s.getHeight() * scale);
				shape.end();
			} else {
				sprite.begin();
				sprite.draw(s, x, y - s.getHeight() * scale / 2, width, s.getHeight() * scale);
				sprite.end();
			}
		}
		if (note instanceof LongNote) {
			sprite.begin();
			if (model.getLntype() == BMSModel.LNTYPE_HELLCHARGENOTE) {
				// HCN
				if (y - height < skin.getLaneregion()[lane].y) {
					height = y - skin.getLaneregion()[lane].y;
				}
				final JudgeManager judge = main.getJudgeManager();

				if (main.getJudgeManager().getProcessingLongNotes()[lane] == note) {
					sprite.draw(skin.getLongnote()[6][lane], x, y - height - 2, width, height + 4);
				} else if (judge.getPassingLongNotes()[lane] == note && note.getState() != 0) {
					sprite.draw(skin.getLongnote()[judge.getHellChargeJudges()[lane] ? 8 : 9][lane], x, y - height - 2,
							width, height + 4);
				} else {
					sprite.draw(skin.getLongnote()[7][lane], x, y - height - 2, width, height + 4);
				}
				Sprite ls = skin.getLongnote()[4][lane];
				sprite.draw(ls, x, y - ls.getHeight() * scale / 2, width, ls.getHeight() * scale);
				Sprite le = skin.getLongnote()[5][lane];
				sprite.draw(le, x, y - height - le.getHeight() * scale / 2, width, le.getHeight() * scale);
			}
			if (model.getLntype() == BMSModel.LNTYPE_CHARGENOTE) {
				// CN
				if (y - height < skin.getLaneregion()[lane].y) {
					height = y - skin.getLaneregion()[lane].y;
				}
				if (main.getJudgeManager().getProcessingLongNotes()[lane] == note) {
					sprite.draw(skin.getLongnote()[2][lane], x, y - height - 2, width, height + 4);
				} else {
					sprite.draw(skin.getLongnote()[3][lane], x, y - height - 2, width, height + 4);
				}
				Sprite ls = skin.getLongnote()[0][lane];
				sprite.draw(ls, x, y - ls.getHeight() * scale / 2, width, ls.getHeight() * scale);
				Sprite le = skin.getLongnote()[1][lane];
				sprite.draw(le, x, y - height - le.getHeight() * scale / 2, width, le.getHeight() * scale);
			}
			if (model.getLntype() == BMSModel.LNTYPE_LONGNOTE) {
				// LN
				if (y - height < skin.getLaneregion()[lane].y) {
					height = y - skin.getLaneregion()[lane].y;
				}
				if (main.getJudgeManager().getProcessingLongNotes()[lane] == note) {
					sprite.draw(skin.getLongnote()[2][lane], x, y - height - 2, width, height + 4);
				} else {
					sprite.draw(skin.getLongnote()[3][lane], x, y - height - 2, width, height + 4);
				}
				Sprite le = skin.getLongnote()[1][lane];
				sprite.draw(le, x, y - height - le.getHeight() * scale / 2, width, le.getHeight() * scale);
			}
			sprite.end();
		}
		if (note instanceof MineNote) {
			sprite.begin();
			Sprite s = skin.getMinenote()[lane];
			sprite.draw(s, x, y - s.getHeight() * scale / 2, width, s.getHeight() * scale);
			sprite.end();
		}
	}

	public void update(int lane, int judge, int time, int fast) {
		if (judge < 2) {
			bomb[lane] = time;
			bombtype[lane] = judge == 0 ? 0 : (fast > 0 ? 1 : 2);
		}
		judgenow[lane / (bomb.length / judgenow.length)] = judge + 1;
		judgenowt[lane / (bomb.length / judgenow.length)] = time;
		judgecombo[lane / (bomb.length / judgenow.length)] = main.getJudgeManager().getCourseCombo();
		;
	}
}
