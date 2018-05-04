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
import com.badlogic.gdx.utils.*;

import java.util.*;
import java.util.logging.Logger;
import java.io.*;

import org.lwjgl.opengl.GL11;

/**
 * �궧�궘�꺍
 * 
 * @author exch
 */
public class Skin {
	
	// TODO �겱�겳�굝�궘�깵�꺀楹삣눇�릤�겗�늽�썴

	/**
	 * 亮�
	 */
	private final float width;
	/**
	 * 遙섅걬
	 */
	private final float height;
	/**
	 * �뀇�깈�꺖�궭�걢�굢�겗亮끾캈�럤
	 */
	private final float dw;
	/**
	 * �뀇�깈�꺖�궭�걢�굢�겗遙섅걬驪붺럤
	 */
	private final float dh;

	private final int CharBMPIndex = 0;
	private final int CharTexIndex = 2;
	private final int CharFaceIndex = 4;
	private final int SelectCGIndex = 6;
	private final PMparseMapping parseMapping = new PMparseMapping(CharBMPIndex, CharTexIndex, CharFaceIndex, SelectCGIndex);

	
	private int loop[];
	private int[][] xywh;
	private int anime = 100;
	
	private int setColor = 1;
	private int setMotion = Integer.MIN_VALUE;
	private int frame[];
	private int size[] = {0, 0}; 
	/**
	 * �쇉�뙯�걬�굦�겍�걚�굥�궧�궘�꺍�궕�깣�궦�궒�궚�깉
	 */
	private Array<SkinObject> objects = new Array<SkinObject>();
	private SkinObject[] objectarray = new SkinObject[0];
	/**
	 * �솮鸚뽧걬�굦�겍�걚�굥�궧�궘�꺍�궕�깣�궦�궒�궚�깉
	 */
	private Array<SkinObject> removes = new Array<SkinObject>();
	/**
	 * �뀯�뒟�룛餓섌뼀冶뗦셽�뼋(ms)
	 */
	private int input;
	/**
	 * �궥�꺖�꺍�겗�셽�뼋(ms)
	 */
	private int scene = 3600000 * 24;
	/**
	 * �궥�꺖�꺍餓ι솉繹뽩굺�뼀冶뗣걢�굢�궥�꺖�꺍燁삭죱�겲�겎�겗�셽�뼋(ms)
	 */
	private int fadeout;

	private Map<Integer, Boolean> option = new HashMap<Integer, Boolean>();
	
	private Map<Integer, Offset> offset = new HashMap<Integer, Offset>();

	private Texture[] CharBMP ;
	/**
	 * 沃��겳渦쇈겳�셽�걢�굢訝띶쨯�겎�걗�굥�걪�겏�걣閻뷴츣�걮�겍�걚�굥op
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

	public void setDestination(SkinObject object, long time, SkinDestinationSize destSize, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer,  int op1, int op2,
			int op3, int offset) {
		destSize = new SkinDestinationSize(destSize.getDstx() * dw, destSize.getDsty() * dh, destSize.getDstw() * dw, destSize.getDsth() * dh);
		object.setDestination(time, destSize, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3, offset);
	}

	public void setDestination(SkinObject object, long time, SkinDestinationSize destSize, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2,
			int op3, int[] offset) {
		destSize = new SkinDestinationSize(destSize.getDstx() * dw, destSize.getDsty() * dh, destSize.getDstw() * dw, destSize.getDsth() * dh);
		object.setDestination(time, destSize, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3, offset);
	}

	public void setDestination(SkinObject object, long time, SkinDestinationSize destSize, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int[] op) {
		destSize = new SkinDestinationSize(destSize.getDstx() * dw, destSize.getDsty() * dh, destSize.getDstw() * dw, destSize.getDsth() * dh);
		object.setDestination(time, destSize, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op);
	}

	public void addNumber(SkinNumber number, long time, SkinDestinationSize destSize, int acc, int a, int r,
			int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3, int offset) {
		destSize = new SkinDestinationSize(destSize.getDstx() * dw, destSize.getDsty() * dh, destSize.getDstw() * dw, destSize.getDsth() * dh);
		number.setDestination(time, destSize, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3, offset);
		objects.add(number);
	}

	public SkinImage addImage(TextureRegion tr, long time, SkinDestinationSize destSize, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2,
			int op3, int offset) {
		SkinImage si = new SkinImage(tr);
		destSize = new SkinDestinationSize(destSize.getDstx() * dw, destSize.getDsty() * dh, destSize.getDstw() * dw, destSize.getDsth() * dh);
		si.setDestination(time, destSize, acc, a, r, g, b, blend, filter, angle, center, loop,
				timer, op1, op2, op3, offset);
		objects.add(si);
		return si;
	}

	public void setMouseRect(SkinObject object, float x, float y, float w, float h) {
		object.setMouseRect(x * dw, y * dh, w * dw, h * dh);
	}

	public SkinObject[] getAllSkinObjects() {
		return objects.toArray(SkinObject.class);
	}

	public void removeSkinObject(SkinObject obj) {
		objects.removeValue(obj, true);
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
		Logger.getGlobal().info("�룒�뵽�걬�굦�겒�걚�걪�겏�걣閻뷴츣�걮�겍�걚�굥SkinObject�뎷�솮 : " + removes.size + " / " + objects.size);
		objects.removeAll(removes, true);
		objectarray = objects.toArray(SkinObject.class);
		option.clear();
	}
	
	private SkinObjectRenderer renderer;

	public void drawAllObjects(SpriteBatch sprite, MainState state) {
		final long time = state.main.getNowTime();
		if(renderer == null) {
			SkinOffset offsetAll = getOffsetAll(state);
			Matrix4 transform = new Matrix4();
			if(offsetAll != null) {
				transform.set(width * offsetAll.x /100, height * offsetAll.y / 100, 0, 0, 0, 0, 0, (offsetAll.w + 100) / 100, (offsetAll.h + 100) / 100, 1);
			} else {
				transform.set(0, 0, 0, 0, 0, 0, 0, 1, 1, 1);
			}
			sprite.setTransformMatrix(transform);
			renderer = new SkinObjectRenderer(sprite);
		}
		for (SkinObject obj : objectarray) {
			if (isDraw(obj.getOption(), state)) {
				obj.draw(renderer, time, state);
			}
		}
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
			// x,y�걣*.5�겗�슋�겓(Windows�겗�겳)TextureRegion�걣�걳�굦�굥�걼�굙�곫슟若싧�얍눇
			sprite.draw(image,  x + 0.01f, y + 0.01f, w, h);
			postDraw();
		}

		public void draw(TextureRegion image, float x, float y, float w, float h, float cx, float cy, float angle) {
			preDraw(image);
			// x,y�걣*.5�겗�슋�겓(Windows�겗�겳)TextureRegion�걣�걳�굦�굥�걼�굙�곫슟若싧�얍눇
			sprite.draw(image, x + 0.01f, y + 0.01f, cx * w, cy * h, w, h, 1, 1, angle);
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
				// TODO 歷쏁츞�룒�뵽�겘�썵�걮�걚�걢竊�
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
	 * �겱�겳�굝�궘�깵�꺀�겗�릢�깴�꺖�궥�깾�꺍�겗1�뫅�쐿�겗�셽�뼋  0:1P_NEUTRAL 1:1P_FEVER 2:1P_GREAT 3:1P_GOOD 4:1P_BAD 5:2P_NEUTRAL 6:2P_GREAT 7:2P_BAD
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
	
	public SkinImage PMcharaLoader(boolean usecim, File imagefile, int type, int color, SkinDestinationSize dstSize, int side, int dsttimer, SkinOption skinOption) {
		//type 0:�깤�꺃�궎 1:�궘�깵�꺀�깒�솺 2:�릫�뎺�뵽�깗 3:�깗�꺁�궋�궎�뵽�깗(訝듿뜇翁ャ겗�겳) 4:�깗�꺁�궋�궎�뵽�깗(�뀲鵝�) 5:�궘�깵�꺀�궋�궎�궠�꺍 6:NEUTRAL 7:FEVER 8:GREAT 9:GOOD 10:BAD 11:FEVERWIN 12:WIN 13:LOSE 14:OJAMA 15:DANCE
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
			//chp�깢�궊�궎�꺂�굮�렋�걲
			File[] filename = chpdir.listFiles();
			for(int i = 0; i < filename.length; i++) {
				if (filename[i].getPath().substring(filename[i].getPath().length()-4,filename[i].getPath().length()).equalsIgnoreCase(".chp")) {
					chp = new File(filename[i].getPath());
					break;
				}
			}
		}
		if(chp == null) return null;

		//�뵽�깗�깈�꺖�궭 0:#CharBMP 1:#CharBMP2P 2:#CharTex 3:#CharTex2P 4:#CharFace 5:#CharFace2P 6:#SelectCG 7:#SelectCG2P
		CharBMP = new Texture[8];
		Arrays.fill(CharBMP, null);
		//�릢�깙�꺀�깳�꺖�궭
		xywh = new int[1296][4];
		for(int[] i: xywh){
			Arrays.fill(i, 0);
		}
		int[] charFaceUpperXywh = {0, 0, 256, 256};
		int[] charFaceAllXywh = {320, 0, 320, 480};
		frame = new int[20];
		Arrays.fill(frame, Integer.MIN_VALUE);
		loop = new int[20];
		Arrays.fill(loop, -1);

		//�깢�꺃�꺖�깲獒쒒뼋�겗�읃繹뽧겗�셽�뼋 60FPS�겗17ms
		//��永귞쉪�겒�돯

		//#Pattern,#Texture,#Layer�겗�깈�꺖�궭
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
						if(checkChar(str)) {
							CharSkinLoader(data, str, usecim, chp);
						}
						else if(str[0].equalsIgnoreCase("#Patern") || str[0].equalsIgnoreCase("#Pattern")) {
							//�궋�깑�깳�꺖�궥�깾�꺍�깈�꺖�궭  烏①ㅊ�꽛�뀍佯╊퐥  �뚣겣�걙�꺖�굤�굯�걧�겱�겳�굝 �걵�걢�굯�겑�띲겎�겘�궧�깪�꺂�깱�궧�겗t�걣訝��겇擁녈굤�겒�걚#Patern�걣閭ｅ폀?
							patternData.get(0).add(line);
						} else if(str[0].equalsIgnoreCase("#Texture")) {
							//�궋�깑�깳�꺖�궥�깾�꺍�깈�꺖�궭  烏①ㅊ�꽛�뀍佯╊릎
							patternData.get(1).add(line);
						} else if(str[0].equalsIgnoreCase("#Layer")) {
							//�궋�깑�깳�꺖�궥�깾�꺍�깈�꺖�궭  烏①ㅊ�꽛�뀍佯�遙�
							patternData.get(2).add(line);
						} else if(str[0].equalsIgnoreCase("#Flame") || str[0].equalsIgnoreCase("#Frame")) {
							//�궋�깑�깳�잌벧 �땿�걤驪롢겗1�옔�걗�걼�굤�겗�셽�뼋(ms) �뚣겣�걙�꺖�굤�굯�걧�겱�겳�굝 �걵�걢�굯�겑�띲겎�겘�궧�깪�꺂�깱�궧�겗#Flame�걣閭ｅ폀?
							if(data.size() > 2) {
								if(PMparseInt(data.get(1)) >= 0 && PMparseInt(data.get(1)) < frame.length) frame[PMparseInt(data.get(1))] = PMparseInt(data.get(2));
							}
						} else if(str[0].equalsIgnoreCase("#Anime")) {
							//#Frame若싩쑴�겗�뙁若싥걣�겒�걚�셽�겗�궋�깑�깳�잌벧 1�옔�걗�걼�굤�겗�셽�뼋(ms)
							if(data.size() > 1) anime = PMparseInt(data.get(1));
						} else if(str[0].equalsIgnoreCase("#Size")) {
							//#Pattern�굜�깒�솺�겓�뵪�걚�굥鸚㎯걤�걬
							if(data.size() > 2) {
								size[0] = PMparseInt(data.get(1));
								size[1] = PMparseInt(data.get(2));
							}
						} else if(str[0].length() == 3 && PMparseInt(str[0].substring(1,3), 36) >= 0 && PMparseInt(str[0].substring(1,3), 36) < xywh.length) {
							//佯㎪쮽若싩쑴
							if(data.size() > xywh[0].length) {
								for(int i = 0; i < xywh[0].length; i++) {
									xywh[PMparseInt(str[0].substring(1,3), 36)][i] = PMparseInt(data.get(i+1));
								}
							}
						} else if(str[0].equalsIgnoreCase("#CharFaceUpperSize")) {
							//�깗�꺁�궋�궎(訝듿뜇翁ャ겗�겳) 佯㎪쮽&�궢�궎�궨
							if(data.size() > charFaceUpperXywh.length) {
								for(int i = 0; i < charFaceUpperXywh.length; i++) {
									charFaceUpperXywh[i] = PMparseInt(data.get(i+1));
								}
							}
						} else if(str[0].equalsIgnoreCase("#CharFaceAllSize")) {
							//�깗�꺁�궋�궎(�뀲鵝�) 佯㎪쮽&�궢�궎�궨
							if(data.size() > charFaceAllXywh.length) {
								for(int i = 0; i < charFaceAllXywh.length; i++) {
									charFaceAllXywh[i] = PMparseInt(data.get(i+1));
								}
							}
						} else if(str[0].equalsIgnoreCase("#Loop")) {
							//�꺂�꺖�깤鵝띸쉰
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

		//#CharBMP�걣�꽒�걚�셽�겘return
		if(CharBMP[CharBMPIndex] == null) return null;
		//#CharBMP2P�걣耶섇쑉�걮�곥걢�겇#Texture若싩쑴�걣�걗�굥�겏�걤�겘#CharTex2P�걣耶섇쑉�걲�굥�겒�굢2P�궖�꺀�꺖�겏�걲�굥
		if(color == 2 && CharBMP[CharBMPIndex+1] != null
				&& (patternData.get(1).size() == 0 || (patternData.get(1).size() > 0 && CharBMP[CharTexIndex+1] != null))
				) setColor = 2;
		//#Texture若싩쑴�걣�걗�굥�겗�겓#CharTex�걣�꽒�걚�셽�겘return
		if(setColor == 1 && patternData.get(1).size() > 0 && CharBMP[CharTexIndex] == null) return null;


		//�뤻걥�눇�릤 �뤂訝뗣겗1pixel�걣�뤻걥�돯 �겦�뒢�뵽�씊�궋�궎�궠�꺍�겘�뤻걥�걮�겒�걚
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
				PlayCharacter(skinOption,dsttimer,side, dstSize, patternData);
				break;
		}
		return null;
	}
	
	private void PlayCharacter(SkinOption skinOption, int dsttimer, int side, SkinDestinationSize dstSize,List<List<String>>  patternData) {
		int increaseRateThreshold = 17;
		for(int i = 0; i < frame.length; i++) {
			if(frame[i] == Integer.MIN_VALUE) frame[i] = anime;
			if(frame[i] < 1) frame[i] = 100;
		}
		//���깱�꺖�뵪
		Pixmap pixmap = new Pixmap( 1, 1, Format.RGBA8888 );
		Texture transparent = new Texture( pixmap );
		SkinImage part = null;
		//#Pattern,#Texture,#Layer�겗�젂�겓�룒�뵽鼇�若싥굮烏뚣걝
		int[] setBMPIndex = {CharBMPIndex,CharTexIndex,CharBMPIndex};
		for(int patternIndex = 0; patternIndex < 3; patternIndex++) {
			Texture setBMP = CharBMP[setBMPIndex[patternIndex] + setColor-1];
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
						op[0] = skinOption.getDstOpt1();
						op[1] = skinOption.getDstOpt2();
						op[2] = skinOption.getDstOpt3();
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
						//�깗�궎�깢�꺍�걣�걗�굥�셽�겘�깢�꺃�꺖�깲獒쒒뼋�굮烏뚣걝 60FPS�겗17ms�걣�읃繹�
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
						//DST沃��겳渦쇈겳
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
						//alpha�겏angle�겗沃��겳渦쇈겳
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
						//�꺂�꺖�깤�뼀冶뗣깢�꺃�꺖�깲�겲�겎
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
								dstSize = new SkinDestinationSize(dstSize.getDstx()+dstxywh[i][0]*dstSize.getDstw()/size[0], dstSize.getDsty()+dstSize.getDsth()-(dstxywh[i][1]+dstxywh[i][3])*dstSize.getDsth()/size[1], dstxywh[i][2]*dstSize.getDstw()/size[0], dstxywh[i][3]*dstSize.getDsth()/size[1]);
								part.setDestination((int)(frameTime*i),dstSize,3,alphaAngle[i][0],255,255,255,1,0,alphaAngle[i][1],0,-1,timer,op[0],op[1],op[2],0);
							}
							dstSize = new SkinDestinationSize(dstSize.getDstx()+dstxywh[(loopFrame+increaseRate)-1][0]*dstSize.getDstw()/size[0], dstSize.getDsty()+dstSize.getDsth()-(dstxywh[(loopFrame+increaseRate)-1][1]+dstxywh[(loopFrame+increaseRate)-1][3])*dstSize.getDsth()/size[1], dstxywh[(loopFrame+increaseRate)-1][2]*dstSize.getDstw()/size[0], dstxywh[(loopFrame+increaseRate)-1][3]*dstSize.getDsth()/size[1]);
							part.setDestination(loopTime-1,dstSize ,3,alphaAngle[(loopFrame+increaseRate)-1][0],255,255,255,1,0,alphaAngle[(loopFrame+increaseRate)-1][1],0,-1,timer,op[0],op[1],op[2],skinOption.getDstOffset());
						}
						//�꺂�꺖�깤�뼀冶뗣깢�꺃�꺖�깲�걢�굢
						TextureRegion[] images = new TextureRegion[dst[0].length() / 2 - (loop[motion]+1)];
						for(int i = (loop[motion]+1)  * 2; i < dst[0].length(); i+=2) {
							int index = PMparseInt(dst[0].substring(i, i+2), 36);
							if(index >= 0 && index < xywh.length && xywh[index][2] > 0 && xywh[index][3] > 0) images[i/2-(loop[motion]+1)] = new TextureRegion(setBMP, xywh[index][0], xywh[index][1], xywh[index][2], xywh[index][3]);
							else images[i/2-(loop[motion]+1)] = new TextureRegion(transparent, 0, 0, 1, 1);
						}
						part = new SkinImage(images, timer, cycle - loopTime);
						add(part);
						for(int i = (loopFrame+increaseRate); i < dstxywh.length; i++) {
							dstSize = new SkinDestinationSize(dstSize.getDstx()+dstxywh[i][0]*dstSize.getDstw()/size[0], dstSize.getDsty()+dstSize.getDsth()-(dstxywh[i][1]+dstxywh[i][3])*dstSize.getDsth()/size[1], dstxywh[i][2]*dstSize.getDstw()/size[0], dstxywh[i][3]*dstSize.getDsth()/size[1]);
							part.setDestination((int)(frameTime*i),dstSize,3,alphaAngle[i][0],255,255,255,1,0,alphaAngle[i][1],0,loopTime,timer,op[0],op[1],op[2],0);
						}
						dstSize = new SkinDestinationSize(dstSize.getDstx()+dstxywh[(loopFrame+increaseRate)-1][0]*dstSize.getDstw()/size[0], dstSize.getDsty()+dstSize.getDsth()-(dstxywh[(loopFrame+increaseRate)-1][1]+dstxywh[(loopFrame+increaseRate)-1][3])*dstSize.getDsth()/size[1], dstxywh[(loopFrame+increaseRate)-1][2]*dstSize.getDstw()/size[0], dstxywh[(loopFrame+increaseRate)-1][3]*dstSize.getDsth()/size[1]);
						part.setDestination(loopTime-1,dstSize ,3,alphaAngle[(loopFrame+increaseRate)-1][0],255,255,255,1,0,alphaAngle[(loopFrame+increaseRate)-1][1],0,-1,timer,op[0],op[1],op[2],skinOption.getDstOffset());
					}
				}
			}
		}
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
	boolean checkChar(String[] str) {
		if(parseMapping.getIndex(str[0]) == 0) {
			return false;
		}
		return true;
	}
	void CharSkinLoader(List<String> data, String[] str, boolean usecim, File chp  ) {
		if(data.size() > 1) CharBMP[parseMapping.getIndex(str[0])] = SkinLoader.getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1).replace("\\", "/"), usecim);
	}

}
