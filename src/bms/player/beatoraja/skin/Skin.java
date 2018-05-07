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
 * 占쎄때占쎄텣占쎄틡
 * 
 * @author exch
 */
public class Skin {
	
	// TODO 占쎄껑占쎄껙占쎄턁占쎄텣占쎄뭇占쎄�璵뱀궍�늾占쎈┐占쎄쿁占쎈듊占쎌뜶

	/**
	 * 畑뤄옙
	 */
	private final float width;
	/**
	 * �걲�꼨嫄�
	 */
	private final float height;
	/**
	 * 占쎈�뉛옙源덌옙爰뽳옙沅�占쎄괍占쎄덩占쎄쿁畑룸겲罹덌옙�윣
	 */
	private final float dw;
	/**
	 * 占쎈�뉛옙源덌옙爰뽳옙沅�占쎄괍占쎄덩占쎄쿁�걲�꼨嫄э쫯遺븍윣
	 */
	private final float dh;

	/**
	 * 占쎌뇡占쎈솺占쎄괵占쎄덱占쎄쾷占쎄콢占쎄데占쎄때占쎄텣占쎄틡占쎄텞占쎄묍占쎄땋占쎄텙占쎄텥占쎄퉱
	 */
	private Array<SkinObject> objects = new Array<SkinObject>();
	private SkinObject[] objectarray = new SkinObject[0];
	/**
	 * 占쎌넯勇싲쉑嫄э옙援�占쎄쾷占쎄콢占쎄데占쎄때占쎄텣占쎄틡占쎄텞占쎄묍占쎄땋占쎄텙占쎄텥占쎄퉱
	 */
	private Array<SkinObject> removes = new Array<SkinObject>();
	/**
	 * 占쎈��占쎈뮓占쎈짏繞볦꼳堉��넼�뿦�끋占쎈펻(ms)
	 */
	private int input;
	/**
	 * 占쎄땅占쎄틬占쎄틡占쎄쿁占쎌끋占쎈펻(ms)
	 */
	private int scene = 3600000 * 24;
	/**
	 * 占쎄땅占쎄틬占쎄틡繞벿뱀냹濚밸쉘援븝옙堉��넼�뿣嫄�占쎄덩占쎄땅占쎄틬占쎄틡�뇖�궘二깍옙寃뀐옙寃롳옙寃쀯옙�끋占쎈펻(ms)
	 */
	private int fadeout;

	private Map<Integer, Boolean> option = new HashMap<Integer, Boolean>();
	
	private Map<Integer, Offset> offset = new HashMap<Integer, Offset>();

	/**
	 * 亦껓옙占쎄껙歷��뇠寃놂옙�끋占쎄괍占쎄덩鼇앸씢夷�占쎄쾸占쎄콟占쎄데占쎄괭占쎄쾹占쎄괏�뼸酉댁르占쎄괼占쎄쾷占쎄콢占쎄데op
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
		Logger.getGlobal().info("占쎈짂占쎈돕占쎄괵占쎄덱占쎄쾼占쎄콢占쎄괭占쎄쾹占쎄괏�뼸酉댁르占쎄괼占쎄쾷占쎄콢占쎄데SkinObject占쎈렩占쎌넯 : " + removes.size + " / " + objects.size);
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
			// x,y占쎄괏*.5占쎄쿁占쎌뒎占쎄쾽(Windows占쎄쿁占쎄껙)TextureRegion占쎄괏占쎄교占쎄덱占쎄데占쎄굴占쎄탽占쎄낙�뒣畑댁떑占쎌뼃�늾
			sprite.draw(image,  x + 0.01f, y + 0.01f, w, h);
			postDraw();
		}

		public void draw(TextureRegion image, float x, float y, float w, float h, float cx, float cy, float angle) {
			preDraw(image);
			// x,y占쎄괏*.5占쎄쿁占쎌뒎占쎄쾽(Windows占쎄쿁占쎄껙)TextureRegion占쎄괏占쎄교占쎄덱占쎄데占쎄굴占쎄탽占쎄낙�뒣畑댁떑占쎌뼃�늾
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
				// TODO 癲뚯뢾痢욑옙猷믭옙逾쏙옙寃섓옙�뜷占쎄괼占쎄콢占쎄괍塋딉옙
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


}
