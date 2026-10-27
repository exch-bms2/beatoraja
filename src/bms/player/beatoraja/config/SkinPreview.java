package bms.player.beatoraja.config;

import java.util.logging.Logger;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.SkinRenderTarget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;

/**
 * SkinConfiguration上で選択中スキンを縮小表示するプレビュー。
 */
public class SkinPreview extends SkinObject {

	private SkinConfiguration configuration;
	private SpriteBatch previewBatch;
	private FrameBuffer frameBuffer;
	private TextureRegion frameRegion;
	private Skin lastSkin;
	private int bufferWidth;
	private int bufferHeight;
	private boolean disabled;
	private final SkinRenderTarget renderTarget = new SkinRenderTarget();
	private final Matrix4 savedProjection = new Matrix4(), savedTransform = new Matrix4();
	private final Matrix4 projection = new Matrix4(), identity = new Matrix4();

	@Override
	public void prepare(long time, MainState state) {
		if (configuration == null && state instanceof SkinConfiguration skinConfiguration) {
			configuration = skinConfiguration;
		}
		super.prepare(time, state);
	}

	@Override
	public void draw(SkinObjectRenderer renderer) {
		if (!draw || configuration == null) {
			return;
		}

		Skin previewSkin = configuration.getSelectedSkin();
		if (previewSkin == null) {
			return;
		}
		if (previewSkin != lastSkin) {
			lastSkin = previewSkin;
			disabled = false;
		}
		if (disabled) {
			return;
		}

		SpriteBatch currentBatch = renderer.getSpriteBatch();
		currentBatch.flush();
		currentBatch.end();
		try {
			renderPreview(previewSkin);
		} catch (Throwable e) {
			disabled = true;
			Logger.getGlobal().warning("スキンプレビュー描画に失敗しました : " + e.getMessage());
		} finally {
			currentBatch.begin();
		}

		if (!disabled && frameRegion != null) draw(renderer, frameRegion);
	}

	private void renderPreview(Skin previewSkin) {
		int width = Math.max(1, Math.round(previewSkin.getWidth()));
		int height = Math.max(1, Math.round(previewSkin.getHeight()));
		try (SkinRenderTarget ignored = renderTarget.begin()) {
			ensureFrameBuffer(width, height);
			savedProjection.set(previewBatch.getProjectionMatrix());
			savedTransform.set(previewBatch.getTransformMatrix());
			renderTarget.bind(frameBuffer);
			Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			previewBatch.setProjectionMatrix(projection.setToOrtho2D(0, 0, width, height));
			previewBatch.setTransformMatrix(identity);
			try {
				previewBatch.begin();
				previewSkin.updateCustomObjects(configuration);
				previewSkin.drawAllObjectsSafely(previewBatch, configuration);
			} finally {
				try { if (previewBatch.isDrawing()) previewBatch.end(); }
				finally {
					previewBatch.setProjectionMatrix(savedProjection);
					previewBatch.setTransformMatrix(savedTransform);
				}
			}
		}
	}

	private void ensureFrameBuffer(int width, int height) {
		if (previewBatch == null) {
			previewBatch = new SpriteBatch();
		}
		if (frameBuffer != null && bufferWidth == width && bufferHeight == height) {
			return;
		}

		if (frameBuffer != null) {
			frameBuffer.dispose();
			frameBuffer = null;
			frameRegion = null;
		}
		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
		bufferWidth = width;
		bufferHeight = height;
		Texture texture = frameBuffer.getColorBufferTexture();
		frameRegion = new TextureRegion(texture);
		frameRegion.flip(false, true);
	}

	@Override
	public void dispose() {
		if (frameBuffer != null) {
			frameBuffer.dispose();
			frameBuffer = null;
		}
		if (previewBatch != null) {
			previewBatch.dispose();
			previewBatch = null;
		}
	}
}
