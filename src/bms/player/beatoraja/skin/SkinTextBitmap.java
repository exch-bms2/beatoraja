package bms.player.beatoraja.skin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;


public class SkinTextBitmap extends SkinText {

	private SkinTextBitmapSource source;
	private BitmapFont font;
	private GlyphLayout layout;
	private int size;

	public SkinTextBitmap(SkinTextBitmapSource source, int size) {
		this(source, size, -1);
	}

	public SkinTextBitmap(SkinTextBitmapSource source, int size, int id) {
		super(id);
		this.source = source;
		this.size = size;
		this.layout =new GlyphLayout();
	}

	@Override
	public void prepareFont(String text) {
		font = source.getFont();
	}

	@Override
	protected void prepareText(String text) {
		font = source.getFont();
	}

	public void draw(SkinObjectRenderer sprite, long time, MainState state, int offsetX, int offsetY) {
		if (font == null)
			return;

		Rectangle r = this.getDestination(time, state);
		if (r != null) {
			float scale = this.size / source.getOriginalSize();
			font.getData().setScale(scale);
			final Color c = getColor();
			final float x = (getAlign() == 2 ? r.x - r.width : (getAlign() == 1 ? r.x - r.width / 2 : r.x));
			layout.setText(font, getText(), c, r.getWidth(),ALIGN[getAlign()], false);
			sprite.setType(source.isDistanceField() ? SkinObjectRenderer.TYPE_DISTANCE_FIELD : SkinObjectRenderer.TYPE_BILINEAR);
			sprite.draw(font, layout, x + offsetX, r.y + offsetY + r.getHeight());
			font.getData().setScale(1);
		}
	}

	public void dispose() {
		source.dispose();
	}

	public static class SkinTextBitmapSource implements Disposable {

		private boolean usecim;
		private boolean useMipMaps;
		private Path fontPath;
		private BitmapFont.BitmapFontData fontData;
		private Array<TextureRegion> regions;
		private BitmapFont font;
		private float originalSize;
		private boolean distanceField;

		public SkinTextBitmapSource(Path fontPath, boolean usecim) {
			this(fontPath, usecim, true);
		}

		public SkinTextBitmapSource(Path fontPath, boolean usecim, boolean useMipMaps) {
			this.usecim = usecim;
			this.useMipMaps = useMipMaps;
			this.fontPath = fontPath;
		}

		public void load() {
			// TODO: 画像の cim 対応
			try {
				fontData = new BitmapFont.BitmapFontData(new FileHandle(fontPath.toFile()), false);

				regions = new Array<>(fontData.imagePaths.length);
				for (int i = 0; i < fontData.imagePaths.length; ++i) {
					FileHandle file = new FileHandle(new File(fontData.imagePaths[i]));
					this.regions.add(new TextureRegion(new Texture(file, useMipMaps)));
				}

				font = new BitmapFont(fontData, regions, true);

				// size が BitmapFont から取得できないので、独自に取得する
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileHandle(fontPath.toFile()).read()), 512)) {
					String line = reader.readLine();
					originalSize = (float) Integer.parseInt(line.substring(line.indexOf("size=") + 5).split(" ")[0]);
				} catch (Exception e) {
					originalSize = fontData.lineHeight;
				}
			} catch (Exception e) {
				font = null;
			}
		}

		public BitmapFont getFont() {
			if (font == null) {
				load();
			}
			return font;
		}

		public float getOriginalSize() {
			return originalSize;
		}

		public boolean isDistanceField() {
			return distanceField;
		}

		public void setDistanceField(boolean value) {
			distanceField = value;
		}

		@Override
		public void dispose() {
			if (font != null) {
				font.dispose();
				font = null;
			}
		}
	}
}
