package bms.player.beatoraja.skin;

import bms.model.*;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.song.SongData;

import java.util.Arrays;

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
	private TextureRegion cursortex;
	
	private Pixmap back = null;
	private Pixmap shape = null;
	private Pixmap cursor = null;

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
	private boolean isNoGapX = false;

	/*
	 * 処理済みノート数 プレイ時は処理済みノート数に変化があった時だけ更新する
	 */
	private int pastNotes = 0;
	private long lastUpdateTime;
	
	private int starttime;
	private int endtime;
	private float freq;
	private float render;

	private static final Color TRANSPARENT_COLOR = Color.valueOf("00000000");

	public SkinNoteDistributionGraph() {
		this(TYPE_NORMAL, 500, 0, 0, 0, 0);
	}

	public SkinNoteDistributionGraph(int type, int delay, int backTexOff, int orderReverse, int noGap, int noGapX) {
		this(null, type, delay, backTexOff, orderReverse, noGap, noGapX);
	}
	
	public SkinNoteDistributionGraph(Pixmap[] chips, int type, int delay, int backTexOff, int orderReverse, int noGap, int noGapX) {
		this.chips = chips;
		this.type = type;
		this.isBackTexOff = backTexOff == 1;
		this.delay = delay;
		this.isOrderReverse = orderReverse == 1;
		this.isNoGap = noGap == 1;
		this.isNoGapX = noGapX == 1;
		pastNotes = 0;
	}
	
	public void prepare(long time, MainState state) {
		prepare(time, state, null, -1, -1, -1);
	}

	public void prepare(long time, MainState state, Rectangle r, int starttime, int endtime, float freq) {
		super.prepare(time, state);			
		if(r != null) {
			region.set(r);
			draw = true;
		}
		this.state = state;
		this.starttime = starttime;
		this.endtime = endtime;
		this.freq = freq;
		render = time >= delay ? 1.0f : (float) time / delay;
	}

	public void draw(SkinObjectRenderer sprite) {	
		
		final SongData song = state.resource.getSongdata();
		final BMSModel model = song != null ? song.getBMSModel() : null;
		
		// TODO スキン定義側で分岐できないか？
		if(chips == null) {
			Color[] graphcolor = type != TYPE_NORMAL && model != null && model.getMode() == Mode.POPN_9K  ? 
					pmsGraphColor[type] : JGRAPH[type];
			chips = new Pixmap[graphcolor.length];
			for(int i = 0;i < graphcolor.length;i++) {
				chips[i] = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
				chips[i].drawPixel(0, 0, Color.toIntBits(255, (int)(graphcolor[i].b * 255), (int)(graphcolor[i].g * 255), (int)(graphcolor[i].r * 255)));
			}
		}

		if(shapetex == null || song != current || (this.model == null && model != null)) {
			current = song;
			this.model = model;
			if(type == TYPE_NORMAL && song != null && song.getInformation() != null) {
				updateGraph(song.getInformation().getDistributionValues());				
			} else {
				updateGraph(model);
			}
		}

		//プレイ時、判定をリアルタイムで更新する
		/*
		 * TODO 高速化のアイデア
		 * BMSPlayerから更新したノーツの時間だけを渡し。指定時間のデータ/イメージのみ更新する
		 * backtex更新は初回のみ
		 */
		if(model != null && state instanceof BMSPlayer && type != TYPE_NORMAL 
				&& pastNotes != ((BMSPlayer)state).getPastNotes() && System.currentTimeMillis() > lastUpdateTime + 500) {
			pastNotes = ((BMSPlayer)state).getPastNotes();
			lastUpdateTime = System.currentTimeMillis();
			updateData(model);
			updateTexture(false);
		}

		draw(sprite, backtex, region.x, region.y + region.height, region.width, -region.height);
		shapetex.setRegionWidth((int) (shapetex.getTexture().getWidth() * render));
		draw(sprite, shapetex, region.x, region.y + region.height, region.width * render, -region.height);
		
		final int oldw = cursor != null ? cursor.getWidth() : 0;
		final int oldh = cursor != null ? cursor.getHeight() : 0;
		final int w = data.length * 5;
		final int h = max * 5;
		cursor.setColor(TRANSPARENT_COLOR);
		cursor.fill();
		// スタートカーソル描画		
		if (starttime >= 0) {
			int dx = (int) (starttime * w / (data.length * 1000));
			cursor.setColor(Color.toIntBits(255, 128, 255, 128));
			cursor.fillRectangle(dx, 0, 3, h);
		}
		// エンドカーソル描画
		if (endtime >= 0) {
			int dx = (int) (endtime * w / (data.length * 1000));
			cursor.setColor(Color.toIntBits(255, 128, 128, 255));
			cursor.fillRectangle(dx, 0, 3, h);
		}
		// 現在カーソル描画
		if (state instanceof BMSPlayer && state.timer.isTimerOn(SkinProperty.TIMER_PLAY)) {
			float currenttime = state.timer.getNowTime(SkinProperty.TIMER_PLAY);
			if (freq > 0) {
				currenttime *= freq;
			}
			int dx = (int) (currenttime * w / (data.length * 1000));
			cursor.setColor(Color.toIntBits(255, 255, 255, 255));
			cursor.fillRectangle(dx, 0, 3, h);
		}
		
		if(cursortex == null) {
			cursortex = new TextureRegion(new Texture(cursor));
		} else if(oldw != w || oldh != h) {
			cursortex.getTexture().dispose();
			cursortex = new TextureRegion(new Texture(cursor));
		} else {
			cursortex.getTexture().draw(cursor, 0, 0);
		}
		draw(sprite, cursortex, region.x, region.y + region.height, region.width, -region.height);
	}
	
	public void draw(SkinObjectRenderer sprite, long time, MainState state, Rectangle r, int starttime, int endtime, float freq) {
		prepare(time, state, r, starttime, endtime, freq);
		if(draw) {
			draw(sprite);
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

		updateTexture(true);
	}

	
	private void updateGraph(BMSModel model) {
		if (model == null) {
			data = new int[0][DATA_LENGTH[type]];
		} else {
			data = new int[model.getLastTime() / 1000 + 1][DATA_LENGTH[type]];
			
			updateData(model);
		}
		
		updateTexture(true);
	}
	
	private void updateData(BMSModel modell) {
		int pos = -1;
		int count = 0;
		max = 20;
		for(int[] d : data) {
			Arrays.fill(d, 0);				
		}

		final Mode mode = model.getMode();
		for (TimeLine tl : model.getAllTimeLines()) {
			final int index = tl.getTime() / 1000;
			if(index >= data.length) {
				break;
			}
			if (index != pos) {
				if (max < count) {
					max = Math.min((count / 10) * 10 + 10, 100);
				}
				pos = index;
				count = type == TYPE_NORMAL ? data[index][1] + data[index][4] : 0;
			}
			for (int i = 0; i < mode.key; i++) {
				Note n = tl.getNote(i);
				if (n != null) {
					final int st = n.getState();
					final int t = n.getPlayTime();
					switch (type) {
					case TYPE_NORMAL:
						if (n instanceof NormalNote) {
							data[index][mode.isScratchKey(i) ? 2 : 5]++;
							count++;
						}
						if (n instanceof LongNote) {
							if(!((LongNote)n).isEnd()) {
								for(int lnindex = index;lnindex <= ((LongNote)n).getPair().getTime() / 1000;lnindex++) {
									data[lnindex][mode.isScratchKey(i) ? 1 : 4]++;
								}
								count++;
							}
							if((model.getLntype() == BMSModel.LNTYPE_LONGNOTE && n instanceof LongNote
									&& ((LongNote) n).isEnd())) {
								data[index][mode.isScratchKey(i) ? 0 : 3]++;
								data[index][mode.isScratchKey(i) ? 1 : 4]--;									
							}
						}
						if (n instanceof MineNote) {
							data[index][6]++;
							count++;
						}
						break;
					case TYPE_JUDGE:
						if (n instanceof MineNote || (model.getLntype() == BMSModel.LNTYPE_LONGNOTE && n instanceof LongNote
								&& ((LongNote) n).isEnd())) {
							break;
						}
						data[index][st]++;
						count++;
						break;
					case TYPE_EARLYLATE:
						if (n instanceof MineNote || (model.getLntype() == BMSModel.LNTYPE_LONGNOTE && n instanceof LongNote
						&& ((LongNote) n).isEnd())) {
							break;
						}
						if (st <= 1) {
							data[index][st]++;
						} else {
							data[index][t >= 0 ? st : st + 4]++;
						}
						count++;
						break;
					}							 
				}
			}
		}

	}
	
	private void updateTexture(boolean updateall) {
		final int oldw = shape != null ? shape.getWidth() : 0;
		final int oldh = shape != null ? shape.getHeight() : 0;
		final int w = data.length * 5;
		final int h = max * 5;
		boolean refresh = false;
		if(shape == null) {
			back = new Pixmap(w, h, Pixmap.Format.RGBA8888);									
			shape = new Pixmap(w, h, Pixmap.Format.RGBA8888);
			cursor = new Pixmap(w, h, Pixmap.Format.RGBA8888);
			refresh = true;
		} else if(oldw != w || oldh != h) {
			back.dispose();				
			shape.dispose();
			cursor.dispose();
			back = new Pixmap(w, h, Pixmap.Format.RGBA8888);									
			shape = new Pixmap(w, h, Pixmap.Format.RGBA8888);						
			cursor = new Pixmap(w, h, Pixmap.Format.RGBA8888);						
			refresh = true;
		} else if(updateall){
			back.setColor(TRANSPARENT_COLOR);
			back.fill();
			shape.setColor(TRANSPARENT_COLOR);
			shape.fill();
			cursor.setColor(TRANSPARENT_COLOR);
			cursor.fill();
			refresh = true;
		}

		int start = 0;
		int end = data.length;
		if(updateall) {
			if(!isBackTexOff) {
				back.setColor(0, 0, 0, 0.8f);
				back.fill();

				for (int i = 10; i < max; i += 10) {
					back.setColor(0.007f * i, 0.007f * i, 0, 1.0f);
					back.fillRectangle(0, i * 5, data.length * 5, 50);
				}

				for (int i = 0; i < data.length; i++) {
					// x軸補助線描画
					if (i % 60 == 0) {
						back.setColor(0.25f, 0.25f, 0.25f, 1.0f);
						back.drawLine(i * 5, 0, i * 5, max * 5);
					} else if (i % 10 == 0) {
						back.setColor(0.125f, 0.125f, 0.125f, 1.0f);
						back.drawLine(i * 5, 0, i * 5, max * 5);
					}
				}
			} else if(!refresh){
				for (int i = 0; i < data.length; i++) {
					if(data[i][0] > 0) {
						start = Math.max(0, i - 2);
						end = Math.min(data.length, i + 3);
						break;
					}
				}				
			}
			
			if(backtex == null) {
				backtex = new TextureRegion(new Texture(back));			
			} else if(oldw != w || oldh != h) {
				backtex.getTexture().dispose();
				backtex = new TextureRegion(new Texture(back));
			} else {
				backtex.getTexture().draw(back, 0, 0);
			}
			
		}

		for (int i = start; i < end; i++) {
			int[] n = data[i];
			if(!isOrderReverse) {
				for (int j = 0, k = n[0], index = 0; j < max && index < n.length;) {
					if (k > 0) {
						k--;
						shape.drawPixmap(chips[index], 0, 0, 1, 1, i * 5, j * 5, 4 + (isNoGapX ? 1 : 0), 4 + (isNoGap ? 1 : 0));
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
						shape.drawPixmap(chips[index], 0, 0, 1, 1, i * 5, j * 5, 4 + (isNoGapX ? 1 : 0), 4 + (isNoGap ? 1 : 0));
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
		
		if(shapetex == null) {
			shapetex = new TextureRegion(new Texture(shape));
		} else if(oldw != w || oldh != h) {
			shapetex.getTexture().dispose();
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
			cursortex.getTexture().dispose();
			back.dispose();
			shape.dispose();
			cursor.dispose();
			shapetex = null;
		}
		if(chips != null) {
			disposeAll(chips);
		}
	}

}
