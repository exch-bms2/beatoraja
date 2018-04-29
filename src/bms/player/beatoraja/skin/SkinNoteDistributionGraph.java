package bms.player.beatoraja.skin;

import bms.model.*;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.song.SongData;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * ノーツ分布を表示するグラフ
 * 
 * @author exch
 */
public class SkinNoteDistributionGraph extends SkinObject {

	private MainState state;

	private TextureRegion backtex;
	private TextureRegion shapetex;
	private Pixmap shape = null;

	private TextureRegion startcursor;
	private TextureRegion endcursor;

	private BMSModel model;
	private SongData current;
	private int[][] data = new int[0][0];

	private static final Color[][] JGRAPH = {
			{ Color.valueOf("44ff44"), Color.valueOf("228822"), Color.valueOf("ff4444"), Color.valueOf("4444ff"), Color.valueOf("222288"), Color.valueOf("cccccc"),
					Color.valueOf("880000") },
			{ Color.valueOf("555555"), Color.valueOf("0088ff"), Color.valueOf("00ff88"), Color.valueOf("ffff00"),
					Color.valueOf("ff8800"), Color.valueOf("ff0000") },
			{ Color.valueOf("555555"), Color.valueOf("44ff44"), Color.valueOf("0088ff"), Color.valueOf("0066cc"),
					Color.valueOf("004488"), Color.valueOf("002244"), Color.valueOf("ff8800"), Color.valueOf("cc6600"),
					Color.valueOf("884400"), Color.valueOf("442200") } };

	private static final Color[][] pmsGraphColor = {
					{ Color.valueOf("44ff44"), Color.valueOf("228822"), Color.valueOf("ff4444"), Color.valueOf("4444ff"), Color.valueOf("222288"), Color.valueOf("cccccc"),
							Color.valueOf("880000") },
					{ Color.valueOf("555555"), Color.valueOf("ff5eb0"), Color.valueOf("ffbe32"), Color.valueOf("dc463c"),
							Color.valueOf("6cc6ff"), Color.valueOf("6cc6ff") },
					{ Color.valueOf("555555"), Color.valueOf("ff5eb0"), Color.valueOf("0088ff"), Color.valueOf("0066cc"),
							Color.valueOf("004488"), Color.valueOf("002244"), Color.valueOf("ff8800"), Color.valueOf("cc6600"),
							Color.valueOf("884400"), Color.valueOf("442200") } };

	private Pixmap[] chips;

	private int max = 20;

	private int type;

	public static final int TYPE_NORMAL = 0;
	public static final int TYPE_JUDGE = 1;
	public static final int TYPE_EARLYLATE = 2;
	
	private static final int[] DATA_LENGTH = {7, 6, 10};

	private boolean isBackTexOff = false;
	private int delay = 500;
	private boolean isOrderReverse = false;
	private boolean isNoGap = false;

	/*
	 * 処理済みノート数 プレイ時は処理済みノート数に変化があった時だけ更新する
	 */
	private int pastNotes = 0;

	private static final Color TRANSPARENT_COLOR = Color.valueOf("00000000");

	public SkinNoteDistributionGraph() {
		this(TYPE_NORMAL, 500, 0, 0, 0);
	}

	public SkinNoteDistributionGraph(int type, int delay, int backTexOff, int orderReverse, int noGap) {
		this(null, type, delay, backTexOff, orderReverse, noGap);
	}
	
	public SkinNoteDistributionGraph(Pixmap[] chips, int type, int delay, int backTexOff, int orderReverse, int noGap) {
		this.chips = chips;
		this.type = type;
		this.isBackTexOff = backTexOff == 1 ? true : false;
		this.delay = delay;
		this.isOrderReverse = orderReverse == 1 ? true : false;
		this.isNoGap = noGap == 1 ? true : false;
		pastNotes = 0;

		Pixmap bp = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		bp.drawPixel(0, 0, Color.toIntBits(255, 128, 255, 128));
		startcursor = new TextureRegion(new Texture(bp));
		bp.dispose();
		bp = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		bp.drawPixel(0, 0, Color.toIntBits(255, 128, 128, 255));
		endcursor = new TextureRegion(new Texture(bp));
		bp.dispose();

	}

	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		if(state instanceof BMSPlayer) {
			
		}
		draw(sprite, time, state, getDestination(time, state), -1, -1);
	}

	public void draw(SkinObjectRenderer sprite, long time, MainState state, Rectangle r, int starttime, int endtime) {
		if (r == null) {
			return;
		}
		
		if(this.state != state) {
			this.state = state;
		}

		final SongData song = state.main.getPlayerResource().getSongdata();
		final BMSModel model = song != null ? song.getBMSModel() : null;
		
		// TODO スキン定義側で分岐できないか？
		if(chips == null) {
			Color[] graphcolor = type > 0 && model != null && model.getMode() == Mode.POPN_9K  ? 
					pmsGraphColor[type] : JGRAPH[type];
			chips = new Pixmap[graphcolor.length];
			for(int i = 0;i < graphcolor.length;i++) {
				chips[i] = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
				chips[i].drawPixel(0, 0, Color.toIntBits(255, (int)(graphcolor[i].b * 255), (int)(graphcolor[i].g * 255), (int)(graphcolor[i].r * 255)));
			}
		}

		if(song != current || (this.model == null && model != null)) {
			current = song;
			this.model = model;
			if(type == 0 && song != null && song.getInformation() != null) {
				updateGraph(song.getInformation().getDistributionValues());				
			} else {
				updateGraph(model);
			}
		}
		if (shapetex == null) {
			updateGraph(model);
		}

		//プレイ時、判定をリアルタイムで更新する
		if(model != null && state instanceof BMSPlayer && type > 0 && pastNotes != ((BMSPlayer)state).getPastNotes()) {
			pastNotes = ((BMSPlayer)state).getPastNotes();
			updateGraph(model);
		}

		draw(sprite, backtex, r.x, r.y + r.height, r.width, -r.height, state);
		final float render = time >= delay ? 1.0f : (float) time / delay;
		shapetex.setRegionWidth((int) (shapetex.getTexture().getWidth() * render));
		draw(sprite, shapetex, r.x, r.y + r.height, r.width * render, -r.height, state);
		// スタートカーソル描画
		if (starttime >= 0) {
			int dx = (int) (starttime * r.width / (data.length * 1000));
			sprite.draw(startcursor, r.x + dx, r.y, 1, r.height);
		}
		// エンドカーソル描画
		if (endtime >= 0) {
			int dx = (int) (endtime * r.width / (data.length * 1000));
			sprite.draw(endcursor, r.x + dx, r.y, 1, r.height);
		}

	}
	
	private void updateGraph(int[][] distribution) {
		data = distribution;
		max = 20;
		for(int i = 0;i < distribution.length;i++) {
			int count = 0;
			for(int j = 0;j < distribution[0].length;j++) {
				count += distribution[i][j];
			}
			if (max < count) {
				max = Math.min((count / 10) * 10 + 10, 100);
			}
		}

		updateTexture();
	}

	
	private void updateGraph(BMSModel model) {
		if (model == null) {
			data = new int[0][DATA_LENGTH[type]];
		} else {
			data = new int[model.getLastTime() / 1000 + 1][DATA_LENGTH[type]];				
			int pos = 0;
			int count = 0;
			max = 20;
			for (TimeLine tl : model.getAllTimeLines()) {
				if(tl.getTime() / 1000 >= data.length) {
					break;
				}
				if (tl.getTime() / 1000 != pos) {
					if (max < count) {
						max = Math.min((count / 10) * 10 + 10, 100);
					}
					pos = tl.getTime() / 1000;
					count = type == TYPE_NORMAL ? data[tl.getTime() / 1000][1] + data[tl.getTime() / 1000][4] : 0;
				}
				for (int i = 0; i < model.getMode().key; i++) {
					Note n = tl.getNote(i);
					if (n != null) {
						final int st = n.getState();
						final int t = n.getPlayTime();
						switch (type) {
						case TYPE_NORMAL:
							if (n instanceof NormalNote) {
								data[tl.getTime() / 1000][model.getMode().isScratchKey(i) ? 2 : 5]++;
								count++;
							}
							if (n instanceof LongNote) {
								if(!((LongNote)n).isEnd()) {
									for(int index = tl.getTime() / 1000;index <= ((LongNote)n).getPair().getTime() / 1000;index++) {
										data[index][model.getMode().isScratchKey(i) ? 1 : 4]++;
									}
									count++;
								}
								if((model.getLntype() == BMSModel.LNTYPE_LONGNOTE && n instanceof LongNote
										&& ((LongNote) n).isEnd())) {
									data[tl.getTime() / 1000][model.getMode().isScratchKey(i) ? 0 : 3]++;
									data[tl.getTime() / 1000][model.getMode().isScratchKey(i) ? 1 : 4]--;									
								}
							}
							if (n instanceof MineNote) {
								data[tl.getTime() / 1000][6]++;
								count++;
							}
							break;
						case TYPE_JUDGE:
							if (n instanceof MineNote || (model.getLntype() == BMSModel.LNTYPE_LONGNOTE && n instanceof LongNote
									&& ((LongNote) n).isEnd())) {
								break;
							}
							data[tl.getTime() / 1000][st]++;
							count++;
							break;
						case TYPE_EARLYLATE:
							if (n instanceof MineNote || (model.getLntype() == BMSModel.LNTYPE_LONGNOTE && n instanceof LongNote
							&& ((LongNote) n).isEnd())) {
								break;
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
		
		updateTexture();
	}
	
	private void updateTexture() {
		if (shapetex != null && !(state instanceof BMSPlayer)) {
			shapetex.getTexture().dispose();
			backtex.getTexture().dispose();
			shape.dispose();
		}

		if( shape == null || (shape != null && !(state instanceof BMSPlayer)) ) {
			shape = new Pixmap(data.length * 5, max * 5, Pixmap.Format.RGBA8888);
		} else {
			shape.setColor(TRANSPARENT_COLOR);
			shape.fill();
		}
		if(!isBackTexOff) {
			shape.setColor(0, 0, 0, 0.8f);
			shape.fill();

			for (int i = 10; i < max; i += 10) {
				shape.setColor(0.007f * i, 0.007f * i, 0, 1.0f);
				shape.fillRectangle(0, i * 5, data.length * 5, 50);
			}

			for (int i = 0; i < data.length; i++) {
				// x軸補助線描画
				if (i % 60 == 0) {
					shape.setColor(Color.valueOf("444444"));
					shape.drawLine(i * 5, 0, i * 5, max * 5);
				} else if (i % 10 == 0) {
					shape.setColor(Color.valueOf("222222"));
					shape.drawLine(i * 5, 0, i * 5, max * 5);
				}
			}
		}
		if( backtex == null || (backtex != null && !(state instanceof BMSPlayer)) ) {
			backtex = new TextureRegion(new Texture(shape));
		} else {
			backtex.getTexture().draw(shape, 0, 0);
		}
		shape.setColor(TRANSPARENT_COLOR);
		shape.fill();

		for (int i = 0; i < data.length; i++) {
			int[] n = data[i];
			if(!isOrderReverse) {
				for (int j = 0, k = n[0], index = 0; j < max && index < n.length;) {
					if (k > 0) {
						k--;
						shape.drawPixmap(chips[index], 0, 0, 1, 1, i * 5, j * 5, 4, 4 + (isNoGap ? 1 : 0));
						j++;
					} else {
						index++;
						if (index == n.length) {
							break;
						}
						k = n[index];
					}
				}
			} else {
				for (int j = 0, k = n[n.length - 1], index = n.length - 1; j < max && index < n.length;) {
					if (k > 0) {
						k--;
						shape.drawPixmap(chips[index], 0, 0, 1, 1, i * 5, j * 5, 4, 4 + (isNoGap ? 1 : 0));
						j++;
					} else {
						index--;
						if (index < 0) {
							break;
						}
						k = n[index];
					}
				}
			}
		}
		if( shapetex == null || (shapetex != null && !(state instanceof BMSPlayer)) ) {
			shapetex = new TextureRegion(new Texture(shape));
		} else {
			shapetex.getTexture().draw(shape, 0, 0);
		}
	}

	@Override
	public void dispose() {
		if (shapetex != null) {
			backtex.getTexture().dispose();
			shapetex.getTexture().dispose();
			startcursor.getTexture().dispose();
			endcursor.getTexture().dispose();
			shape.dispose();
			shapetex = null;
		}
		if(chips != null) {
			disposeAll(chips);
		}
	}

}
