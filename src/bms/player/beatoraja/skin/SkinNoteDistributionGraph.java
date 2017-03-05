package bms.player.beatoraja.skin;

import bms.model.*;
import bms.player.beatoraja.MainState;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * ノーツ分布を表示するグラフ
 * 
 * @author exch
 */
public class SkinNoteDistributionGraph extends SkinObject {

	private Texture backtex;
	private Texture shapetex;

	private TextureRegion startcursor;
	private TextureRegion endcursor;

	private BMSModel model;
	private int[][] data = new int[0][0];

	private static final Color[][] JGRAPH = {
			{ Color.valueOf("cccccc"), Color.valueOf("4444ff"), Color.valueOf("ff4444"), Color.valueOf("44ff44"),
					Color.valueOf("880000") },
			{ Color.valueOf("555555"), Color.valueOf("0088ff"), Color.valueOf("00ff88"), Color.valueOf("ffff00"),
					Color.valueOf("ff8800"), Color.valueOf("ff0000") },
			{ Color.valueOf("555555"), Color.valueOf("44ff44"), Color.valueOf("0088ff"), Color.valueOf("0066cc"),
					Color.valueOf("004488"), Color.valueOf("002244"), Color.valueOf("ff8800"), Color.valueOf("cc6600"),
					Color.valueOf("884400"), Color.valueOf("442200") } };

	private final Color[] graphcolor;

	private int max = 20;

	private int type;

	public static final int TYPE_NORMAL = 0;
	public static final int TYPE_JUDGE = 1;
	public static final int TYPE_EARLYLATE = 2;

	private int delay = 500;

	public SkinNoteDistributionGraph() {
		this(TYPE_NORMAL);
	}

	public SkinNoteDistributionGraph(int type) {
		this.type = type;
		graphcolor = JGRAPH[type];

		Pixmap bp = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		bp.drawPixel(0, 0, Color.toIntBits(255, 128, 255, 128));
		startcursor = new TextureRegion(new Texture(bp));
		bp.dispose();
		bp = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		bp.drawPixel(0, 0, Color.toIntBits(255, 128, 128, 255));
		endcursor = new TextureRegion(new Texture(bp));
		bp.dispose();

	}

	public void draw(SpriteBatch sprite, long time, MainState state) {
		draw(sprite, time, state, getDestination(time, state), -1, -1);
	}

	public void draw(SpriteBatch sprite, long time, MainState state, Rectangle r, int starttime, int endtime) {
		final BMSModel model = state.getMainController().getPlayerResource().getSongdata() != null
				? state.getMainController().getPlayerResource().getSongdata().getBMSModel() : null;
		final boolean reload = this.model != model;
		if (reload) {
			if (model == null) {
				this.model = model;
				data = new int[0][graphcolor.length];
			} else {
				this.model = model;
				data = new int[model.getLastTime() / 1000 + 1][graphcolor.length];
				int pos = 0;
				int count = 0;
				max = 20;
				for (TimeLine tl : model.getAllTimeLines()) {
					if (tl.getTime() / 1000 != pos) {
						if (max < count) {
							max = Math.min((count / 10) * 10 + 10, 100);
						}
						pos = tl.getTime() / 1000;
						count = 0;
					}
					for (int i = 0; i < 18; i++) {
						Note n = tl.getNote(i);
						if (n != null && !(model.getLntype() == BMSModel.LNTYPE_LONGNOTE && n instanceof LongNote
								&& ((LongNote) n).getEndnote().getSection() == tl.getSection())) {
							int st = n.getState();
							int t = n.getTime();
							switch (type) {
							case TYPE_NORMAL:
								if (n instanceof NormalNote) {
									data[tl.getTime() / 1000][model.getUseKeys() != 9 && (i == 7 || i == 16) ? 2 : 0]++;
								}
								if (n instanceof LongNote) {
									data[tl.getTime() / 1000][model.getUseKeys() != 9 && (i == 7 || i == 16) ? 3 : 1]++;
								}
								if (n instanceof MineNote) {
									data[tl.getTime() / 1000][4]++;
								}
								count++;
								break;
							case TYPE_JUDGE:
								if (n instanceof MineNote) {
									break;
								}
								if (n instanceof LongNote
										&& ((LongNote) n).getEndnote().getSection() == tl.getSection()) {
									st = ((LongNote) n).getEndnote().getState();
									// if(state == 0) {
									// System.out.println("終端未処理:"+tl.getTime());
									// }
								}
								data[tl.getTime() / 1000][st]++;
								count++;
								break;
							case TYPE_EARLYLATE:
								if (n instanceof MineNote) {
									break;
								}
								if (n instanceof LongNote
										&& ((LongNote) n).getEndnote().getSection() == tl.getSection()) {
									st = ((LongNote) n).getEndnote().getState();
									t = ((LongNote) n).getEndnote().getTime();
									// if(state == 0) {
									// System.out.println("終端未処理:"+tl.getTime());
									// }
								}
								if (st <= 1) {
									data[tl.getTime() / 1000][st]++;
								} else {
									data[tl.getTime() / 1000][t >= 0 ? st : st + 4]++;
								}
								count++;
								break;

							}
						}
					}
				}
			}
		}

		if (r == null) {
			return;
		}

		if (shapetex != null) {
			if (shapetex.getWidth() != (int) r.getWidth() || shapetex.getHeight() != (int) r.getHeight() || reload) {
				shapetex.dispose();
				backtex.dispose();
				shapetex = null;
			} else {
				// shape.setColor(Color.BLACK);
				// shape.fill();
			}
		}

		if (shapetex == null) {
			Pixmap shape = new Pixmap(data.length * 5, max * 5, Pixmap.Format.RGBA8888);
			shape.setColor(0, 0, 0, 0.8f);
			shape.fill();

			for (int i = 10; i < max; i += 10) {
				shape.setColor(0.007f * i, 0.007f * i, 0, 1.0f);
				shape.fillRectangle(0, i * 5, data.length * 5, 50);
			}

			for (int i = 0; i < data.length; i++) {
				// BPM変化地点描画
				// x軸補助線描画
				if (i % 60 == 0) {
					shape.setColor(Color.valueOf("444444"));
					shape.drawLine(i * 5, 0, i * 5, max * 5);
				} else if (i % 10 == 0) {
					shape.setColor(Color.valueOf("222222"));
					shape.drawLine(i * 5, 0, i * 5, max * 5);
				}
			}
			backtex = new Texture(shape);
			shape.dispose();

			shape = new Pixmap(data.length * 5, max * 5, Pixmap.Format.RGBA8888);
			for (int i = 0; i < data.length; i++) {
				int[] n = data[i];
				for (int j = 0, k = n[0], index = 0; j < max && index < graphcolor.length;) {
					if (k > 0) {
						k--;
						shape.setColor(graphcolor[index]);
						shape.fillRectangle(i * 5, j * 5, 4, 4);
						j++;
					} else {
						index++;
						if (index == graphcolor.length) {
							break;
						}
						k = n[index];
					}
				}
			}
			shapetex = new Texture(shape);
			shape.dispose();
		}

		sprite.draw(backtex, r.x, r.y + r.height, r.width, -r.height);
		final float render = time >= delay ? 1.0f : (float) time / delay;
		sprite.draw(shapetex, r.x, r.y + r.height, r.width * render, -r.height);

		if (starttime != -1) {
			int dx = (int) (starttime * r.width / (data.length * 1000));
			sprite.draw(startcursor, r.x + dx, r.y, 1, r.height);
		}
		if (endtime != -1) {
			int dx = (int) (endtime * r.width / (data.length * 1000));
			sprite.draw(endcursor, r.x + dx, r.y, 1, r.height);
		}

	}

	@Override
	public void dispose() {
		if (shapetex != null) {
			backtex.dispose();
			shapetex.dispose();
			startcursor.getTexture().dispose();
			endcursor.getTexture().dispose();
			shapetex = null;
		}
	}

}
