package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.ShaderManager;
import bms.player.beatoraja.SkinConfig.Offset;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.skin.property.BooleanProperty;
import bms.player.beatoraja.play.BMSPlayer;

import bms.player.beatoraja.skin.property.TimerProperty;
import bms.player.beatoraja.skin.property.TimerPropertyFactory;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.*;

import java.util.function.Consumer;
import java.util.logging.Logger;

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
	private Array<SkinObject> objects = new Array<SkinObject>();
	private SkinObject[] objectarray = new SkinObject[0];
	/**
	 * 除外されているスキンオブジェクト
	 */
	private Array<SkinObject> removes = new Array<SkinObject>();
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

	private IntIntMap option = new IntIntMap();
	
	private IntMap<Offset> offset = new IntMap<Offset>();

	private final IntMap<CustomEvent> customEvents = new IntMap<CustomEvent>();
	private final IntMap<CustomTimer> customTimers = new IntMap<CustomTimer>();

	public Skin(Resolution org, Resolution dst) {
		width = dst.width;
		height = dst.height;
		dw = ((float)dst.width) / org.width;
		dh = ((float)dst.height) / org.height;
	}

	public void add(SkinObject object) {
		objects.add(object);
	}

	public void setDestination(SkinObject object, long time, float x, float y, float w, float h, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2,
			int op3, int[] offset) {
		object.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer > 0 ? TimerPropertyFactory.getTimerProperty(timer) : null, op1, op2, op3, offset);
	}

	public void setDestination(SkinObject object, long time, float x, float y, float w, float h, int acc, int a,
	                           int r, int g, int b, int blend, int filter, int angle, int center, int loop, TimerProperty timer, int[] op) {
		object.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op);
	}

	public void setDestination(SkinObject object, long time, float x, float y, float w, float h, int acc, int a,
	                           int r, int g, int b, int blend, int filter, int angle, int center, int loop, TimerProperty timer, BooleanProperty draw) {
		object.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, draw);
	}

	public void addNumber(SkinNumber number, long time, float x, float y, float w, float h, int acc, int a, int r,
			int g, int b, int blend, int filter, int angle, int center, int loop, TimerProperty timer, int op1, int op2, int op3, int offset) {
		number.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3, offset);
		objects.add(number);
	}

	public SkinImage addImage(TextureRegion tr, long time, float x, float y, float w, float h, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, TimerProperty timer, int op1, int op2,
			int op3, int offset) {
		SkinImage si = new SkinImage(tr);
		si.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center, loop,
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
			if(!obj.validate()) {
				removes.add(obj);
			} else {
				Array<BooleanProperty> bp = new Array();
				for(BooleanProperty op : obj.getDrawCondition()) {
					if(op.isStatic(state)) {
						if(!op.get(state)) {
							removes.add(obj);							
						}
					} else {
						bp.add(op);
					}
				}
				obj.setDrawCondition(bp.toArray(BooleanProperty.class));

				IntArray l = new IntArray();
				for(int op : obj.getOption()) {
					if(op > 0) {
						final int value = option.get(op, -1);
						if(value != 1) {
							removes.add(obj);						
						}				
					} else {
						final int value = option.get(-op, -1);
						if(value != 0) {
							removes.add(obj);						
						}
					}				
				}
				obj.setOption(l.toArray());
			}
			
 		}
		Logger.getGlobal().info("描画されないことが確定しているSkinObject削除 : " + removes.size + " / " + objects.size);
		objects.removeAll(removes, true);
		objectarray = objects.toArray(SkinObject.class);
		option.clear();

		for(SkinObject obj : objects) {
			obj.load();
		}
		
		prepareduration = 1000000 / state.main.getConfig().getPrepareFramePerSecond();
		nextpreparetime = -1;
	}
	
	private SkinObjectRenderer renderer;
	
	private long nextpreparetime;
	private long prepareduration;

	public void drawAllObjects(SpriteBatch sprite, MainState state) {
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
		
		final long microtime = state.main.getNowMicroTime();
		if(nextpreparetime <= microtime) {
			final long time = state.main.getNowTime();
			for (SkinObject obj : objectarray) {
				obj.prepare(time, state);
			}
			
			nextpreparetime += ((microtime - nextpreparetime) / prepareduration + 1) * prepareduration;
		}
		
		for (SkinObject obj : objectarray) {
			if (obj.draw) {
				obj.draw(renderer);
			}
		}
	}

	public void mousePressed(MainState state, int button, int x, int y) {
		for (int i = objectarray.length - 1; i >= 0; i--) {
			final SkinObject obj = objectarray[i];
			if (obj.draw && obj.mousePressed(state, button, x, y)) {
				break;
			}
		}
	}

	public void mouseDragged(MainState state, int button, int x, int y) {
		for (int i = objectarray.length - 1; i >= 0; i--) {
			final SkinObject obj = objectarray[i];
			if (obj instanceof SkinSlider && obj.draw && obj.mousePressed(state, button, x, y)) {
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

	public IntIntMap getOption() {
		return option;
	}

	public void setOption(IntIntMap option) {
		this.option = option;
	}

	public IntMap<Offset> getOffset() {
		return offset;
	}

	public void setOffset(IntMap<Offset> offset) {
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
		
		private ShaderProgram[] shaders = new ShaderProgram[6];
		
		private int current;
		
		private int blend;
		
		private int type;
		
		public static final int TYPE_NORMAL = 0;
		public static final int TYPE_LINEAR = 1;
		public static final int TYPE_BILINEAR = 2;
		public static final int TYPE_FFMPEG = 3;
		public static final int TYPE_LAYER = 4;
		public static final int TYPE_DISTANCE_FIELD = 5;
		
		private Color color;
		
		private Color orgcolor;
		
		public SkinObjectRenderer(SpriteBatch sprite) {
			this.sprite = sprite;
			shaders[TYPE_BILINEAR] = ShaderManager.getShader("bilinear");
			shaders[TYPE_FFMPEG] = ShaderManager.getShader("ffmpeg");
			shaders[TYPE_LAYER] = ShaderManager.getShader("layer");
			shaders[TYPE_DISTANCE_FIELD] = ShaderManager.getShader("distance_field");

			sprite.setShader(shaders[current]);
			sprite.setColor(Color.WHITE);
		}

		public void draw(BitmapFont font, String s, float x, float y, Color c) {
			for (TextureRegion region : font.getRegions()) {
				setFilter(region);
			}
			preDraw();
			font.setColor(c);
			font.draw(sprite, s, x, y);
			postDraw();
		}

		public void draw(BitmapFont font, GlyphLayout layout, float x, float y) {
			draw(font, layout, x, y, null);
		}

		public void draw(BitmapFont font, GlyphLayout layout, float x, float y, Consumer<ShaderProgram> shaderVariableSetter) {
			for (TextureRegion region : font.getRegions()) {
				setFilter(region);
			}
			preDraw(shaderVariableSetter);
			font.draw(sprite, layout, x, y);
			postDraw();
		}

		public void draw(Texture image, float x, float y, float w, float h) {
			setFilter(image);
			preDraw();
			sprite.draw(image, x, y, w, h);
			postDraw();
		}

		public void draw(TextureRegion image, float x, float y, float w, float h) {
			setFilter(image);
			preDraw();
			// x,yが*.5の際に(Windowsのみ)TextureRegionがずれるため、暫定対処
			sprite.draw(image,  x + 0.01f, y + 0.01f, w, h);
			postDraw();
		}

		public void draw(TextureRegion image, float x, float y, float w, float h, float cx, float cy, float angle) {
			setFilter(image);
			preDraw();
			// x,yが*.5の際に(Windowsのみ)TextureRegionがずれるため、暫定対処
			sprite.draw(image, x + 0.01f, y + 0.01f, cx * w, cy * h, w, h, 1, 1, angle);
			postDraw();
		}

		private void setFilter(TextureRegion image) {
			setFilter(image.getTexture());
		}

		private void setFilter(Texture image) {
			if(type == TYPE_LINEAR || type == TYPE_FFMPEG || type == TYPE_DISTANCE_FIELD) {
				image.setFilter(TextureFilter.Linear, TextureFilter.Linear);
			}
		}
		
		private void preDraw() {
			preDraw(null);
		}
		
		private void preDraw(Consumer<ShaderProgram> shaderVariableSetter) {
			if(shaders[current] != shaders[type]) {
				sprite.setShader(shaders[type]);
				current = type;
			}

			if (shaders[type] != null && shaderVariableSetter != null) {
				// シェーダの変数を変更する場合はバッチを切る
				// （shader.begin() - end() で囲うのは正しく動作しないため不可）
				sprite.flush();
				shaderVariableSetter.accept(shaders[type]);
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

	// TODO ぽみゅキャラ系処理の分離

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


	public void addCustomEvent(CustomEvent event) {
		customEvents.put(event.getId(), event);
	}

	public void executeCustomEvent(MainState state, int id, int arg1, int arg2) {
		if (customEvents.containsKey(id)) {
			customEvents.get(id).execute(state, arg1, arg2);
		}
	}

	public void addCustomTimer(CustomTimer timer) {
		customTimers.put(timer.getId(), timer);
	}

	/**
	 * カスタムタイマーの値を設定する。
	 * 能動的・受動的にかかわらず、取得の度にタイマーの値を再計算しないため、
	 * 同一フレームでの値は一意であることが保証される。
	 * @param id カスタムタイマーID
	 * @return タイマーの値 (micro sec)
	 */
	public long getMicroCustomTimer(int id) {
		if (customTimers.containsKey(id)) {
			return customTimers.get(id).getMicroTimer();
		} else {
			return Long.MIN_VALUE;
		}
	}

	/**
	 * (受動的な)カスタムタイマーの値を設定する。
	 * タイマーが存在しない場合は追加する。
	 * @param id カスタムタイマーID
	 * @param time タイマーの値 (micro sec)
	 */
	public void setMicroCustomTimer(int id, long time) {
		if (customTimers.containsKey(id)) {
			customTimers.get(id).setMicroTimer(time);
		} else {
			CustomTimer timer = new CustomTimer(id, null);
			timer.setMicroTimer(time);
			customTimers.put(id, timer);
		}
	}

	/**
	 * ユーザー定義のオブジェクトを1フレームに1回ずつ更新する。
	 * 更新順: タイマー -> イベント
	 * それぞれ ID が小さい順
	 * @param state MainState
	 */
	public void updateCustomObjects(MainState state) {
		for (IntMap.Entry<CustomTimer> timer : customTimers) {
			timer.value.update(state);
		}
		for (IntMap.Entry<CustomEvent> event : customEvents) {
			event.value.update(state);
		}
	}
}
