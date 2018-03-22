package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.ShaderManager;
import bms.player.beatoraja.SkinConfig.Offset;
import bms.player.beatoraja.play.SkinGauge;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.play.BMSPlayer;

import static bms.player.beatoraja.skin.SkinProperty.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.IntArray;

import java.util.*;
import java.util.logging.Logger;
import java.io.*;

import org.lwjgl.opengl.GL11;

/**
 * スキン
 * 
 * @author exch
 */
public class Skin {

	/**
	 * 幅
	 */
	private final float width;
	/**
	 * 高さ
	 */
	private final float height;
	/**
	 * 元データからの幅比率
	 */
	private final float dw;
	/**
	 * 元データからの高さ比率
	 */
	private final float dh;

	/**
	 * 登録されているスキンオブジェクト
	 */
	private List<SkinObject> objects = new ArrayList<SkinObject>();
	private SkinObject[] objectarray = new SkinObject[0];
	/**
	 * 除外されているスキンオブジェクト
	 */
	private List<SkinObject> removes = new ArrayList<SkinObject>();
	/**
	 * 入力受付開始時間(ms)
	 */
	private int input;
	/**
	 * シーンの時間(ms)
	 */
	private int scene = 3600000 * 24;
	/**
	 * シーン以降準備開始からシーン移行までの時間(ms)
	 */
	private int fadeout;

	private Map<Integer, Boolean> option = new HashMap<Integer, Boolean>();
	
	private Map<Integer, Offset> offset = new HashMap<Integer, Offset>();

	/**
	 * 読み込み時から不変であることが確定しているop
	 */
	private int[] fixopt;

	public Skin(Resolution org, Resolution dst) {
		this(org, dst, new int[0]);
	}

	public Skin(Resolution org, Resolution dst, int[] fixopt) {
		width = dst.width;
		height = dst.height;
		dw = ((float)dst.width) / org.width;
		dh = ((float)dst.height) / org.height;
		this.fixopt = fixopt;
	}

	public void add(SkinObject object) {
		objects.add(object);
	}

	public void setDestination(SkinObject object, long time, float x, float y, float w, float h, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2,
			int op3, int offset) {
		object.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3, offset);
	}

	public void setDestination(SkinObject object, long time, float x, float y, float w, float h, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int[] op) {
		object.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op);
	}

	public void addNumber(SkinNumber number, long time, float x, float y, float w, float h, int acc, int a, int r,
			int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3, int offset) {
		number.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3, offset);
		objects.add(number);
	}

	public SkinImage addImage(TextureRegion tr, long time, float x, float y, float w, float h, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2,
			int op3, int offset) {
		SkinImage si = new SkinImage(tr);
		si.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center, loop,
				timer, op1, op2, op3, offset);
		objects.add(si);
		return si;
	}

	public SkinObject[] getAllSkinObjects() {
		return objects.toArray(new SkinObject[objects.size()]);
	}

	public void removeSkinObject(SkinObject obj) {
		objects.remove(obj);
	}
	
	public void prepare(MainState state) {
		for(SkinObject obj : objects) {
			if(obj.getAllDestination().length == 0) {
				removes.add(obj);
			} else {
				IntArray l = new IntArray();
				for(int op : obj.getOption()) {
					if(op > 0) {
						if(option.containsKey(op)) {
							if(!option.get(op)) {
								removes.add(obj);						
							}
						} else {
							boolean fix = false;
							for(int fop : fixopt) {
								if(op == fop) {
									fix = true;
									if(!state.getBooleanValue(op)) {
										removes.add(obj);						
									}							
									break;
								}
							}
							if(!fix) {
								l.add(op);
							}
						}					
					} else {
						if(option.containsKey(-op)) {
							if(option.get(-op)) {
								removes.add(obj);						
							}
						} else {
							boolean fix = false;
							for(int fop : fixopt) {
								if(-op == fop) {
									fix = true;
									if(state.getBooleanValue(-op)) {
										removes.add(obj);						
									}							
									break;
								}
							}
							if(!fix) {
								l.add(op);
							}
						}
					}				
				}
				obj.setOption(l.toArray());
			}
			
 		}
		Logger.getGlobal().info("描画されないことが確定しているSkinObject削除 : " + removes.size() + " / " + objects.size());
		objects.removeAll(removes);
		objectarray = objects.toArray(new SkinObject[objects.size()]);
		option.clear();
	}
	
	private SkinObjectRenderer renderer;

	public void drawAllObjects(SpriteBatch sprite, MainState state) {
		final long time = state.main.getNowTime();
		if(renderer == null) {
			renderer = new SkinObjectRenderer(sprite);
		}
		for (SkinObject obj : objectarray) {
			if (isDraw(obj.getOption(), state)) {
				obj.draw(renderer, time, state);
			}
		}
		SkinOffset offsetAll = getOffsetAll(state);
		Matrix4 transform = new Matrix4();
		if(offsetAll != null) transform.set(width * offsetAll.x /100, height * offsetAll.y / 100, 0, 0, 0, 0, 0, (offsetAll.w + 100) / 100, (offsetAll.h + 100) / 100, 1);
		else transform.set(0, 0, 0, 0, 0, 0, 0, 1, 1, 1);
		sprite.setTransformMatrix(transform);
	}

	private final boolean isDraw(int[] opt, MainState state) {
		for (int op : opt) {
			if (op > 0) {
				if (!state.getBooleanValue(op)) {
					return false;
				}
			} else {
				if (state.getBooleanValue(-op)) {
					return false;
				}				
			}
		}
		return true;
	}

	public void mousePressed(MainState state, int button, int x, int y) {
		for (int i = objectarray.length - 1; i >= 0; i--) {
			final SkinObject obj = objectarray[i];
			if (isDraw(obj.getOption(), state) && obj.mousePressed(state, button, x, y)) {
				break;
			}
		}
	}

	public void mouseDragged(MainState state, int button, int x, int y) {
		for (int i = objectarray.length - 1; i >= 0; i--) {
			final SkinObject obj = objectarray[i];
			if (obj instanceof SkinSlider && isDraw(obj.getOption(), state) && obj.mousePressed(state, button, x, y)) {
				break;
			}
		}
	}

	public void dispose() {
		for (SkinObject obj : objects) {
			obj.dispose();
		}
		for (SkinObject obj : removes) {
			obj.dispose();
		}
	}

	public int getFadeout() {
		return fadeout;
	}

	public void setFadeout(int fadeout) {
		this.fadeout = fadeout;
	}

	public int getInput() {
		return input;
	}

	public void setInput(int input) {
		this.input = input;
	}

	public int getScene() {
		return scene;
	}

	public void setScene(int scene) {
		this.scene = scene;
	}

	public Map<Integer,Boolean> getOption() {
		return option;
	}

	public void setOption(Map<Integer, Boolean> option) {
		this.option = option;
	}

	public Map<Integer, Offset> getOffset() {
		return offset;
	}

	public void setOffset(Map<Integer, Offset> offset) {
		this.offset = offset;
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}

	public double getScaleX() {
		return dw;
	}

	public double getScaleY() {
		return dh;
	}
	
	public static class SkinObjectRenderer {
		
		private final SpriteBatch sprite;
		
		private ShaderProgram[] shaders = new ShaderProgram[5];
		
		private int current;
		
		private int blend;
		
		private int type;
		
		public static final int TYPE_NORMAL = 0;
		public static final int TYPE_LINEAR = 1;
		public static final int TYPE_BILINEAR = 2;
		public static final int TYPE_FFMPEG = 3;
		public static final int TYPE_LAYER = 4;
		
		private Color color;
		
		private Color orgcolor;
		
		public SkinObjectRenderer(SpriteBatch sprite) {
			this.sprite = sprite;
			shaders[TYPE_BILINEAR] = ShaderManager.getShader("bilinear");
			shaders[TYPE_FFMPEG] = ShaderManager.getShader("ffmpeg");
			shaders[TYPE_LAYER] = ShaderManager.getShader("layer");

			sprite.setShader(shaders[current]);
			sprite.setColor(Color.WHITE);
		}

		public void draw(BitmapFont font, String s, float x, float y, Color c) {
			preDraw(font.getRegion());
			font.setColor(c);
			font.draw(sprite, s, x, y);
			postDraw();
		}

		public void draw(BitmapFont font, GlyphLayout layout, float x, float y) {
			preDraw(font.getRegion());
			font.draw(sprite, layout, x, y);
			postDraw();
		}

		public void draw(Texture image, float x, float y, float w, float h) {
			preDraw(image);
			sprite.draw(image, x, y, w, h);
			postDraw();
		}

		public void draw(TextureRegion image, float x, float y, float w, float h) {
			preDraw(image);
			sprite.draw(image, (int)(x + 0.5f), (int)(y + 0.5f), (int)(w + (x + 0.5f) % 1.0f), (int)(h + (y + 0.5f) % 1.0f));
			postDraw();
		}

		public void draw(TextureRegion image, float x, float y, float w, float h, float cx, float cy, float angle) {
			preDraw(image);
			sprite.draw(image, (int)(x + 0.5f), (int)(y + 0.5f), cx * w, cy * h, (int)(w + (x + 0.5f) % 1.0f),
					(int)(h + (y + 0.5f) % 1.0f), 1, 1, angle);
			postDraw();
		}

		private void preDraw(TextureRegion image) {
			preDraw(image.getTexture());
		}
		
		private void preDraw(Texture image) {
			if(shaders[current] != shaders[type]) {
				sprite.setShader(shaders[type]);
				current = type;
			}
			
			switch (blend) {
			case 2:
				sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
				break;
			case 3:
				// TODO 減算描画は難しいか？
				Gdx.gl.glBlendEquation(GL20.GL_FUNC_SUBTRACT);
				sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
				Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD);
				break;
			case 4:
				sprite.setBlendFunction(GL11.GL_ZERO, GL11.GL_SRC_COLOR);
				break;
			case 9:
				sprite.setBlendFunction(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO);
				break;
			}
			
			if(type == TYPE_LINEAR || type == TYPE_FFMPEG) {
				image.setFilter(TextureFilter.Linear, TextureFilter.Linear);				
			}

			if(color != null) {
				orgcolor = sprite.getColor();
				sprite.setColor(color);				
			} else {
				orgcolor = null;
			}
		}
		
		private void postDraw() {
			if(orgcolor != null) {
				sprite.setColor(orgcolor);				
			}

			if (blend >= 2) {
				sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			}
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public int getBlend() {
			return blend;
		}

		public void setBlend(int blend) {
			this.blend = blend;
		}
		
		public Color getColor() {
			return color;
		}

		public void setColor(Color color) {
			this.color = color;
		}
	}

	public int getGaugeParts() {
		for(SkinObject obj: objects) {
			if(obj instanceof SkinGauge) {
				return ((SkinGauge)obj).getParts();
			}
		}
		return 0;
	}

	public void setGaugeParts(int parts) {
		for(SkinObject obj: objects) {
			if(obj instanceof SkinGauge) {
				((SkinGauge)obj).setParts(parts);
			}
		}
	}

	public SkinOffset getOffsetAll(MainState state) {
		SkinOffset offsetAll = null;
		if(state instanceof BMSPlayer) {
			switch(((BMSPlayer)state).getSkinType()) {
			case PLAY_5KEYS:
			case PLAY_7KEYS:
			case PLAY_9KEYS:
			case PLAY_10KEYS:
			case PLAY_14KEYS:
			case PLAY_24KEYS:
			case PLAY_24KEYS_DOUBLE:
				offsetAll = state.getOffsetValue(SkinProperty.OFFSET_ALL);
				break;
			}
		}
		return offsetAll;
	}

	/**
	 * ぽみゅキャラの各モーションの1周期の時間  0:1P_NEUTRAL 1:1P_FEVER 2:1P_GREAT 3:1P_GOOD 4:1P_BAD 5:2P_NEUTRAL 6:2P_GREAT 7:2P_BAD
	 */
	private int PMcharaTime[] = {1,1,1,1,1,1,1,1};

	public int getPMcharaTime(int index) {
		if(index < 0 || index >= PMcharaTime.length) return 1;
		return PMcharaTime[index];
	}

	public void setPMcharaTime(int index, int value) {
		if(index >= 0 && index < PMcharaTime.length && value >= 1) {
			this.PMcharaTime[index] = value;
		}
	}

	public SkinImage PMcharaLoader(boolean usecim, File imagefile, int type, int color, float dstx, float dsty, float dstw, float dsth, int side, int dsttimer, int dstOp1, int dstOp2, int dstOp3, int dstOffset) {
		//type 0:プレイ 1:キャラ背景 2:名前画像 3:ハリアイ画像(上半身のみ) 4:ハリアイ画像(全体) 5:キャラアイコン 6:NEUTRAL 7:FEVER 8:GREAT 9:GOOD 10:BAD 11:FEVERWIN 12:WIN 13:LOSE 14:OJAMA 15:DANCE
		final int PLAY = 0;
		final int BACKGROUND = 1;
		final int NAME = 2;
		final int FACE_UPPER = 3;
		final int FACE_ALL = 4;
		final int SELECT_CG = 5;
		final int NEUTRAL = 6;
		final int FEVER = 7;
		final int GREAT = 8;
		final int GOOD = 9;
		final int BAD = 10;
		final int FEVERWIN = 11;
		final int WIN = 12;
		final int LOSE = 13;
		final int OJAMA = 14;
		final int DANCE = 15;

		if(type < 0 || type > 15) return null;

		File chp = null;
		File chpdir = null;

		if(imagefile.exists() && imagefile.getPath().substring(imagefile.getPath().length()-4,imagefile.getPath().length()).equalsIgnoreCase(".chp")) {
			chp = new File(imagefile.getPath());
		} else if (!imagefile.exists() && imagefile.getPath().substring(imagefile.getPath().length()-4,imagefile.getPath().length()).equalsIgnoreCase(".chp")) {
			chpdir = new File(imagefile.getPath().substring(0, Math.max(imagefile.getPath().lastIndexOf('\\'), imagefile.getPath().lastIndexOf('/')) + 1));
		} else {
			if(imagefile.getPath().charAt(imagefile.getPath().length()-1) != '/' && imagefile.getPath().charAt(imagefile.getPath().length()-1) != '\\') chpdir = new File(imagefile.getPath()+"/");
			else chpdir = new File(imagefile.getPath());
		}
		if(chp == null && chpdir != null) {
			//chpファイルを探す
			File[] filename = chpdir.listFiles();
			for(int i = 0; i < filename.length; i++) {
				if (filename[i].getPath().substring(filename[i].getPath().length()-4,filename[i].getPath().length()).equalsIgnoreCase(".chp")) {
					chp = new File(filename[i].getPath());
					break;
				}
			}
		}
		if(chp == null) return null;

		//画像データ 0:#CharBMP 1:#CharBMP2P 2:#CharTex 3:#CharTex2P 4:#CharFace 5:#CharFace2P 6:#SelectCG 7:#SelectCG2P
		Texture[] CharBMP = new Texture[8];
		Arrays.fill(CharBMP, null);
		final int CharBMPIndex = 0;
		final int CharTexIndex = 2;
		final int CharFaceIndex = 4;
		final int SelectCGIndex = 6;
		//各パラメータ
		int[][] xywh = new int[1296][4];
		for(int[] i: xywh){
			Arrays.fill(i, 0);
		}
		int[] charFaceUpperXywh = {0, 0, 256, 256};
		int[] charFaceAllXywh = {320, 0, 320, 480};
		int anime = 100;
		int size[] = {0, 0};
		int frame[] = new int[20];
		Arrays.fill(frame, Integer.MIN_VALUE);
		int loop[] = new int[20];
		Arrays.fill(loop, -1);
		//最終的な色
		int setColor = 1;
		//フレーム補間の基準の時間 60FPSの17ms
		int increaseRateThreshold = 17;
		//#Pattern,#Texture,#Layerのデータ
		List<List<String>> patternData = new ArrayList<List<String>>();
		for(int i = 0; i < 3; i++) patternData.add(new ArrayList<String>());

		try (BufferedReader br = new BufferedReader(
			new InputStreamReader(new FileInputStream(chp), "MS932"));) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#") ) {
					String[] str = line.split("\t", -1);
					if (str.length > 1) {
						List<String> data = PMparseStr(str);
						if (str[0].equalsIgnoreCase("#CharBMP")) {
							//#Pattern, #Layer用画像
							if(data.size() > 1) CharBMP[CharBMPIndex] = SkinLoader.getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1).replace("\\", "/"), usecim);
						} else if(str[0].equalsIgnoreCase("#CharBMP2P")) {
							//#Pattern, #Layer用画像2P
							if(data.size() > 1) CharBMP[CharBMPIndex+1] = SkinLoader.getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1).replace("\\", "/"), usecim);
						} else if(str[0].equalsIgnoreCase("#CharTex")) {
							//#Texture用画像
							if(data.size() > 1) CharBMP[CharTexIndex] = SkinLoader.getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1).replace("\\", "/"), usecim);
						} else if(str[0].equalsIgnoreCase("#CharTex2P")) {
							//#Texture用画像2P
							if(data.size() > 1) CharBMP[CharTexIndex+1] = SkinLoader.getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1).replace("\\", "/"), usecim);
						} else if(str[0].equalsIgnoreCase("#CharFace")) {
							//ハリアイ
							if(data.size() > 1) CharBMP[CharFaceIndex] = SkinLoader.getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1).replace("\\", "/"), usecim);
						} else if(str[0].equalsIgnoreCase("#CharFace2P")) {
							//ハリアイ2P
							if(data.size() > 1) CharBMP[CharFaceIndex+1] = SkinLoader.getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1).replace("\\", "/"), usecim);
						} else if(str[0].equalsIgnoreCase("#SelectCG")) {
							//選択画面アイコン
							if(data.size() > 1) CharBMP[SelectCGIndex] = SkinLoader.getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1).replace("\\", "/"), usecim);
						} else if(str[0].equalsIgnoreCase("#SelectCG2P")) {
							//選択画面アイコン2P
							if(data.size() > 1) CharBMP[SelectCGIndex+1] = SkinLoader.getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1).replace("\\", "/"), usecim);
						} else if(str[0].equalsIgnoreCase("#Patern") || str[0].equalsIgnoreCase("#Pattern")) {
							//アニメーションデータ  表示優先度低  「ふぃーりんぐぽみゅ せかんど」ではスペルミスのtが一つ足りない#Paternが正式?
							patternData.get(0).add(line);
						} else if(str[0].equalsIgnoreCase("#Texture")) {
							//アニメーションデータ  表示優先度中
							patternData.get(1).add(line);
						} else if(str[0].equalsIgnoreCase("#Layer")) {
							//アニメーションデータ  表示優先度高
							patternData.get(2).add(line);
						} else if(str[0].equalsIgnoreCase("#Flame") || str[0].equalsIgnoreCase("#Frame")) {
							//アニメ速度 動き毎の1枚あたりの時間(ms) 「ふぃーりんぐぽみゅ せかんど」ではスペルミスの#Flameが正式?
							if(data.size() > 2) {
								if(PMparseInt(data.get(1)) >= 0 && PMparseInt(data.get(1)) < frame.length) frame[PMparseInt(data.get(1))] = PMparseInt(data.get(2));
							}
						} else if(str[0].equalsIgnoreCase("#Anime")) {
							//#Frame定義の指定がない時のアニメ速度 1枚あたりの時間(ms)
							if(data.size() > 1) anime = PMparseInt(data.get(1));
						} else if(str[0].equalsIgnoreCase("#Size")) {
							//#Patternや背景に用いる大きさ
							if(data.size() > 2) {
								size[0] = PMparseInt(data.get(1));
								size[1] = PMparseInt(data.get(2));
							}
						} else if(str[0].length() == 3 && PMparseInt(str[0].substring(1,3), 36) >= 0 && PMparseInt(str[0].substring(1,3), 36) < xywh.length) {
							//座標定義
							if(data.size() > xywh[0].length) {
								for(int i = 0; i < xywh[0].length; i++) {
									xywh[PMparseInt(str[0].substring(1,3), 36)][i] = PMparseInt(data.get(i+1));
								}
							}
						} else if(str[0].equalsIgnoreCase("#CharFaceUpperSize")) {
							//ハリアイ(上半身のみ) 座標&サイズ
							if(data.size() > charFaceUpperXywh.length) {
								for(int i = 0; i < charFaceUpperXywh.length; i++) {
									charFaceUpperXywh[i] = PMparseInt(data.get(i+1));
								}
							}
						} else if(str[0].equalsIgnoreCase("#CharFaceAllSize")) {
							//ハリアイ(全体) 座標&サイズ
							if(data.size() > charFaceAllXywh.length) {
								for(int i = 0; i < charFaceAllXywh.length; i++) {
									charFaceAllXywh[i] = PMparseInt(data.get(i+1));
								}
							}
						} else if(str[0].equalsIgnoreCase("#Loop")) {
							//ループ位置
							if(data.size() > 2) {
								if(PMparseInt(data.get(1)) >= 0 && PMparseInt(data.get(1)) < loop.length) loop[PMparseInt(data.get(1))] = PMparseInt(data.get(2));
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		//#CharBMPが無い時はreturn
		if(CharBMP[CharBMPIndex] == null) return null;
		//#CharBMP2Pが存在し、かつ#Texture定義があるときは#CharTex2Pが存在するなら2Pカラーとする
		if(color == 2 && CharBMP[CharBMPIndex+1] != null
				&& (patternData.get(1).size() == 0 || (patternData.get(1).size() > 0 && CharBMP[CharTexIndex+1] != null))
				) setColor = 2;
		//#Texture定義があるのに#CharTexが無い時はreturn
		if(setColor == 1 && patternData.get(1).size() > 0 && CharBMP[CharTexIndex] == null) return null;


		//透過処理 右下の1pixelが透過色 選択画面アイコンは透過しない
		for(int i = 0; i < SelectCGIndex; i++) {
			if(CharBMP[i] != null) {
				Pixmap pixmap = new Pixmap( CharBMP[i].getWidth(), CharBMP[i].getHeight(), Format.RGBA8888 );
				int transparentColor = CharBMP[i].getTextureData().consumePixmap().getPixel(CharBMP[i].getWidth() - 1, CharBMP[i].getHeight() - 1);
				for(int x = 0; x < CharBMP[i].getWidth(); x++) {
					for(int y = 0; y < CharBMP[i].getHeight(); y++) {
						if(transparentColor != CharBMP[i].getTextureData().consumePixmap().getPixel(x, y)) {
							pixmap.drawPixel(x, y, CharBMP[i].getTextureData().consumePixmap().getPixel(x, y));
						}
					}
				}
				CharBMP[i].dispose();
				CharBMP[i] = new Texture( pixmap );
				pixmap.dispose();
			}
		}

		TextureRegion[] image = new TextureRegion[1];
		Texture setBMP;
		int setMotion = Integer.MIN_VALUE;
		SkinImage PMcharaPart = null;
		switch(type) {
			case BACKGROUND:
				setBMP = CharBMP[CharBMPIndex + setColor-1];
				image = new TextureRegion[1];
				image[0] = new TextureRegion(setBMP, xywh[1][0], xywh[1][1], xywh[1][2], xywh[1][3]);
				PMcharaPart = new SkinImage(image, 0, 0);
				add(PMcharaPart);
				return PMcharaPart;
			case NAME:
				setBMP = CharBMP[CharBMPIndex + setColor-1];
				image = new TextureRegion[1];
				image[0] = new TextureRegion(setBMP, xywh[0][0], xywh[0][1], xywh[0][2], xywh[0][3]);
				PMcharaPart = new SkinImage(image, 0, 0);
				add(PMcharaPart);
				return PMcharaPart;
			case FACE_UPPER:
				setBMP = setColor == 2 && CharBMP[CharFaceIndex + 1] != null ? CharBMP[CharFaceIndex + 1] : CharBMP[CharFaceIndex];
				if(setBMP == null) break;
				image = new TextureRegion[1];
				image[0] = new TextureRegion(setBMP, charFaceUpperXywh[0], charFaceUpperXywh[1], charFaceUpperXywh[2], charFaceUpperXywh[3]);
				PMcharaPart = new SkinImage(image, 0, 0);
				add(PMcharaPart);
				return PMcharaPart;
			case FACE_ALL:
				setBMP = setColor == 2 && CharBMP[CharFaceIndex + 1] != null ? CharBMP[CharFaceIndex + 1] : CharBMP[CharFaceIndex];
				if(setBMP == null) break;
				image = new TextureRegion[1];
				image[0] = new TextureRegion(setBMP, charFaceAllXywh[0], charFaceAllXywh[1], charFaceAllXywh[2], charFaceAllXywh[3]);
				PMcharaPart = new SkinImage(image, 0, 0);
				add(PMcharaPart);
				return PMcharaPart;
			case SELECT_CG:
				setBMP = setColor == 2 && CharBMP[SelectCGIndex + 1] != null ? CharBMP[SelectCGIndex + 1] : CharBMP[SelectCGIndex];
				if(setBMP == null) break;
				image = new TextureRegion[1];
				image[0] = new TextureRegion(setBMP, 0, 0, setBMP.getWidth(), setBMP.getHeight());
				PMcharaPart = new SkinImage(image, 0, 0);
				add(PMcharaPart);
				return PMcharaPart;
			case NEUTRAL:
				if(setMotion == Integer.MIN_VALUE) setMotion = 1;
			case FEVER:
				if(setMotion == Integer.MIN_VALUE) setMotion = 6;
			case GREAT:
				if(setMotion == Integer.MIN_VALUE) setMotion = 7;
			case GOOD:
				if(setMotion == Integer.MIN_VALUE) setMotion = 8;
			case BAD:
				if(setMotion == Integer.MIN_VALUE) setMotion = 10;
			case FEVERWIN:
				if(setMotion == Integer.MIN_VALUE) setMotion = 17;
			case WIN:
				if(setMotion == Integer.MIN_VALUE) setMotion = 15;
			case LOSE:
				if(setMotion == Integer.MIN_VALUE) setMotion = 16;
			case OJAMA:
				if(setMotion == Integer.MIN_VALUE) setMotion = 3;
			case DANCE:
				if(setMotion == Integer.MIN_VALUE) setMotion = 14;
			case PLAY:
				for(int i = 0; i < frame.length; i++) {
					if(frame[i] == Integer.MIN_VALUE) frame[i] = anime;
					if(frame[i] < 1) frame[i] = 100;
				}
				//ダミー用
				Pixmap pixmap = new Pixmap( 1, 1, Format.RGBA8888 );
				Texture transparent = new Texture( pixmap );
				SkinImage part = null;
				//#Pattern,#Texture,#Layerの順に描画設定を行う
				int[] setBMPIndex = {CharBMPIndex,CharTexIndex,CharBMPIndex};
				for(int patternIndex = 0; patternIndex < 3; patternIndex++) {
					setBMP = CharBMP[setBMPIndex[patternIndex] + setColor-1];
					for(int patternDataIndex = 0; patternDataIndex < patternData.get(patternIndex).size(); patternDataIndex++) {
						String[] str = patternData.get(patternIndex).get(patternDataIndex).split("\t", -1);
						if (str.length > 1) {
							int motion = Integer.MIN_VALUE;
							String dst[] = new String[4];
							Arrays.fill(dst, "");
							List<String> data = PMparseStr(str);
							if(data.size() > 1) motion = PMparseInt(data.get(1));
							for (int i = 0; i < dst.length; i++) {
								if(data.size() > i + 2) dst[i] = data.get(i + 2).replaceAll("[^0-9a-zA-Z-]", "");
							}
							int timer = Integer.MIN_VALUE;
							int op[] = {0,0,0};
							if(setMotion != Integer.MIN_VALUE && setMotion == motion) {
								timer = dsttimer;
								op[0] = dstOp1;
								op[1] = dstOp2;
								op[2] = dstOp3;
							} else if(setMotion == Integer.MIN_VALUE) {
								if(side != 2) {
									if(motion == 1) timer = TIMER_PM_CHARA_1P_NEUTRAL;
									else if(motion == 6) timer = TIMER_PM_CHARA_1P_FEVER;
									else if(motion == 7) timer = TIMER_PM_CHARA_1P_GREAT;
									else if(motion == 8) timer = TIMER_PM_CHARA_1P_GOOD;
									else if(motion == 10) timer = TIMER_PM_CHARA_1P_BAD;
									else if(motion >= 15 && motion <= 17) {
										timer = TIMER_MUSIC_END;
										if(motion == 15) {
											op[0] = OPTION_1P_BORDER_OR_MORE;	//WIN
											op[1] = -OPTION_1P_100;
										}
										else if(motion == 16) op[0] = -OPTION_1P_BORDER_OR_MORE;	//LOSE
										else if(motion == 17) op[0] = OPTION_1P_100;	//FEVERWIN
									}
								} else {
									if(motion == 1) timer = TIMER_PM_CHARA_2P_NEUTRAL;
									else if(motion == 7) timer = TIMER_PM_CHARA_2P_GREAT;
									else if(motion == 10) timer = TIMER_PM_CHARA_2P_BAD;
									else if(motion == 15 || motion == 16) {
										timer = TIMER_MUSIC_END;
										if(motion == 15) op[0] = -OPTION_1P_BORDER_OR_MORE;	//WIN
										else if(motion == 16) op[0] = OPTION_1P_BORDER_OR_MORE;	//LOSE
									}
								}
							}
							if(timer != Integer.MIN_VALUE
									&& (dst[0].length() > 0 && dst[0].length() % 2 == 0)
									&& (dst[1].length() == 0 || (dst[1].length() > 0 && dst[1].length() == dst[0].length()))
									&& (dst[2].length() == 0 || (dst[2].length() > 0 && dst[2].length() == dst[0].length()))
									&& (dst[3].length() == 0 || (dst[3].length() > 0 && dst[3].length() == dst[0].length()))
									) {
								if(loop[motion] >= dst[0].length() / 2 - 1) loop[motion] = dst[0].length() / 2 - 2;
								else if(loop[motion] < -1) loop[motion] = -1;
								int cycle = frame[motion] * dst[0].length() / 2;
								int loopTime = frame[motion] * (loop[motion]+1);
								if(setMotion == Integer.MIN_VALUE && timer >= TIMER_PM_CHARA_1P_NEUTRAL && timer < TIMER_MUSIC_END) {
									setPMcharaTime(timer - TIMER_PM_CHARA_1P_NEUTRAL, cycle);
								}
								boolean hyphenFlag = false;
								for(int i = 1; i < dst.length; i++) {
									if(dst[i].indexOf("-") != -1) {
										hyphenFlag = true;
										break;
									}
								}
								//ハイフンがある時はフレーム補間を行う 60FPSの17msが基準
								int increaseRate = 1;
								if(hyphenFlag && frame[motion] >= increaseRateThreshold) {
									for(int i = 1; i <= frame[motion]; i++) {
										if(frame[motion] / i < increaseRateThreshold && frame[motion] % i == 0) {
											increaseRate = i;
											break;
										}
									}
									for(int i = 1; i < dst.length; i++) {
										int charsIndex = 0;
										char[] chars = new char[dst[i].length() * increaseRate];
										for(int j = 0; j < dst[i].length(); j+=2) {
											for(int k = 0; k < increaseRate; k++) {
												chars[charsIndex] = dst[i].charAt(j);
												charsIndex++;
												chars[charsIndex] = dst[i].charAt(j+1);
												charsIndex++;
											}
										}
										dst[i] = String.valueOf(chars);
									}
								}
								//DST読み込み
								double frameTime = frame[motion]/increaseRate;
								int loopFrame = loop[motion]*increaseRate;
								int dstxywh[][] = new int[dst[1].length() > 0 ? dst[1].length()/2 : dst[0].length()/2][4];
								for(int i = 0; i < dstxywh.length;i++){
									dstxywh[i][0] = 0;
									dstxywh[i][1] = 0;
									dstxywh[i][2] = size[0];
									dstxywh[i][3] = size[1];
								}
								int startxywh[] = {0,0,size[0],size[1]};
								int endxywh[] = {0,0,size[0],size[1]};
								int count;
								for(int i = 0; i < dst[1].length(); i+=2) {
									if(dst[1].length() >= i+2) {
										if(dst[1].substring(i, i+2).equals("--")) {
											count = 0;
											for(int j = i; j < dst[1].length() && dst[1].substring(j, j+2).equals("--"); j+=2) count++;
											if(PMparseInt(dst[1].substring(i+count*2, i+count*2+2), 36) >= 0 && PMparseInt(dst[1].substring(i+count*2, i+count*2+2), 36) < xywh.length) endxywh = xywh[PMparseInt(dst[1].substring(i+count*2, i+count*2+2), 36)];
											for(int j = i; j < dst[1].length() && dst[1].substring(j, j+2).equals("--"); j+=2) {
												int[] value = new int[dstxywh[0].length];
												for(int k = 0; k < dstxywh[0].length; k++) {
													value[k] = startxywh[k] + (endxywh[k] - startxywh[k]) * ((j - i) / 2 + 1) / (count + 1);
												}
												System.arraycopy(value,0,dstxywh[j/2],0,value.length);
											}
											i += (count - 1) * 2;
										} else if(PMparseInt(dst[1].substring(i, i+2), 36) >= 0 && PMparseInt(dst[1].substring(i, i+2), 36) < xywh.length) {
											startxywh = xywh[PMparseInt(dst[1].substring(i, i+2), 36)];
											System.arraycopy(startxywh,0,dstxywh[i/2],0,startxywh.length);
										}
									}
								}
								//alphaとangleの読み込み
								int alphaAngle[][] = new int[dstxywh.length][2];
								for(int i = 0; i < alphaAngle.length; i++){
									alphaAngle[i][0] = 255;
									alphaAngle[i][1] = 0;
								}
								for(int index = 2 ; index < dst.length; index++) {
									int startValue = 0;
									int endValue = 0;
									for(int i = 0; i < dst[index].length(); i+=2) {
										if(dst[index].length() >= i+2) {
											if(dst[index].substring(i, i+2).equals("--")) {
												count = 0;
												for(int j = i; j < dst[index].length() && dst[index].substring(j, j+2).equals("--"); j+=2) count++;
												if(PMparseInt(dst[index].substring(i+count*2, i+count*2+2), 16) >= 0 && PMparseInt(dst[index].substring(i+count*2, i+count*2+2), 16) <= 255) {
													endValue = PMparseInt(dst[index].substring(i+count*2, i+count*2+2), 16);
													if(index == 3) endValue = Math.round(endValue * 360f / 256f);
												}
												for(int j = i; j < dst[index].length() && dst[index].substring(j, j+2).equals("--"); j+=2) {
													alphaAngle[j/2][index - 2] = startValue + (endValue - startValue) * ((j - i) / 2 + 1) / (count + 1);
												}
												i += (count - 1) * 2;
											} else if(PMparseInt(dst[index].substring(i, i+2), 16) >= 0 && PMparseInt(dst[index].substring(i, i+2), 16) <= 255) {
												startValue = PMparseInt(dst[index].substring(i, i+2), 16);
												if(index == 3) startValue = Math.round(startValue * 360f / 256f);;
												alphaAngle[i/2][index - 2] = startValue;
											}
										}
									}
								}
								//ループ開始フレームまで
								if((loopFrame+increaseRate) != 0) {
									TextureRegion[] images = new TextureRegion[(loop[motion]+1)];
									for(int i = 0; i < (loop[motion]+1) * 2; i+=2) {
										int index = PMparseInt(dst[0].substring(i, i+2), 36);
										if(index >= 0 && index < xywh.length && xywh[index][2] > 0 && xywh[index][3] > 0) images[i/2] = new TextureRegion(setBMP, xywh[index][0], xywh[index][1], xywh[index][2], xywh[index][3]);
										else images[i/2] = new TextureRegion(transparent, 0, 0, 1, 1);
									}
									part = new SkinImage(images, timer, loopTime);
									add(part);
									for(int i = 0; i < (loopFrame+increaseRate); i++) {
										part.setDestination((int)(frameTime*i),dstx+dstxywh[i][0]*dstw/size[0], dsty+dsth-(dstxywh[i][1]+dstxywh[i][3])*dsth/size[1], dstxywh[i][2]*dstw/size[0], dstxywh[i][3]*dsth/size[1],3,alphaAngle[i][0],255,255,255,1,0,alphaAngle[i][1],0,-1,timer,op[0],op[1],op[2],0);
									}
									part.setDestination(loopTime-1,dstx+dstxywh[(loopFrame+increaseRate)-1][0]*dstw/size[0], dsty+dsth-(dstxywh[(loopFrame+increaseRate)-1][1]+dstxywh[(loopFrame+increaseRate)-1][3])*dsth/size[1], dstxywh[(loopFrame+increaseRate)-1][2]*dstw/size[0], dstxywh[(loopFrame+increaseRate)-1][3]*dsth/size[1],3,alphaAngle[(loopFrame+increaseRate)-1][0],255,255,255,1,0,alphaAngle[(loopFrame+increaseRate)-1][1],0,-1,timer,op[0],op[1],op[2],dstOffset);
								}
								//ループ開始フレームから
								TextureRegion[] images = new TextureRegion[dst[0].length() / 2 - (loop[motion]+1)];
								for(int i = (loop[motion]+1)  * 2; i < dst[0].length(); i+=2) {
									int index = PMparseInt(dst[0].substring(i, i+2), 36);
									if(index >= 0 && index < xywh.length && xywh[index][2] > 0 && xywh[index][3] > 0) images[i/2-(loop[motion]+1)] = new TextureRegion(setBMP, xywh[index][0], xywh[index][1], xywh[index][2], xywh[index][3]);
									else images[i/2-(loop[motion]+1)] = new TextureRegion(transparent, 0, 0, 1, 1);
								}
								part = new SkinImage(images, timer, cycle - loopTime);
								add(part);
								for(int i = (loopFrame+increaseRate); i < dstxywh.length; i++) {
									part.setDestination((int)(frameTime*i),dstx+dstxywh[i][0]*dstw/size[0], dsty+dsth-(dstxywh[i][1]+dstxywh[i][3])*dsth/size[1], dstxywh[i][2] * dstw / size[0], dstxywh[i][3] * dsth / size[1],3,alphaAngle[i][0],255,255,255,1,0,alphaAngle[i][1],0,loopTime,timer,op[0],op[1],op[2],0);
								}
								part.setDestination(cycle,dstx+dstxywh[dstxywh.length-1][0]*dstw/size[0], dsty+dsth-(dstxywh[dstxywh.length-1][1]+dstxywh[dstxywh.length-1][3])*dsth/size[1], dstxywh[dstxywh.length-1][2] * dstw / size[0], dstxywh[dstxywh.length-1][3] * dsth / size[1],3,alphaAngle[dstxywh.length-1][0],255,255,255,1,0,alphaAngle[dstxywh.length-1][1],0,loopTime,timer,op[0],op[1],op[2],dstOffset);
							}
						}
					}
				}
				break;
		}
		return null;
	}
	private int PMparseInt(String s) {
		return Integer.parseInt(s.replaceAll("[^0-9-]", ""));
	}
	private int PMparseInt(String s, int radix) {
		if(radix == 36) {
			int result = 0;
			final char c1 = s.charAt(0);
			if (c1 >= '0' && c1 <= '9') {
				result = (c1 - '0') * 36;
			} else if (c1 >= 'a' && c1 <= 'z') {
				result = ((c1 - 'a') + 10) * 36;
			} else if (c1 >= 'A' && c1 <= 'Z') {
				result = ((c1 - 'A') + 10) * 36;
			}
			final char c2 = s.charAt(1);
			if (c2 >= '0' && c2 <= '9') {
				result += (c2 - '0');
			} else if (c2 >= 'a' && c2 <= 'z') {
				result += (c2 - 'a') + 10;
			} else if (c2 >= 'A' && c2 <= 'Z') {
				result += (c2 - 'A') + 10;
			}
			return result;
		}
		return Integer.parseInt(s.replaceAll("[^0-9a-fA-F-]", ""), radix);
	}
	private List<String> PMparseStr(String[] s) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < s.length; i++) {
			if(s[i].length() > 0) {
				if(s[i].startsWith("/")) {
					break;
				} else if(s[i].indexOf("//") != -1) {
					list.add(s[i].substring(0, s[i].indexOf("//")));
					break;
				} else {
					list.add(s[i]);
				}
			}
		}
		return list;
	}

}
