package bms.player.beatoraja.skin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

import bms.player.beatoraja.skin.property.StringProperty;
import bms.player.beatoraja.skin.property.StringPropertyFactory;
import bms.player.beatoraja.skin.BitmapFontCache.CacheableBitmapFont;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

/**
 * .fnt ファイルをソースとして持つスキン用テキスト
 */
public final class SkinTextBitmap extends SkinText {

	private final SkinTextBitmapSource source;
	private final BitmapFont font;
	private final GlyphLayout layout;
	private final float size;

	public SkinTextBitmap(SkinTextBitmapSource source, float size) {
		this(source, size, StringPropertyFactory.getStringProperty(-1));
	}

	public SkinTextBitmap(SkinTextBitmapSource source, float size, StringProperty property) {
		super(property);
		this.source = source;
		this.size = size;
		this.layout =new GlyphLayout();
		this.font = source.getFont();
	}

	@Override
	public void load() {
	}

	@Override
	public void prepareFont(String text) {
	}

	@Override
	protected void prepareText(String text) {
	}

	public void draw(SkinObjectRenderer sprite, float offsetX, float offsetY) {
		if (font == null)
			return;

		float scale = this.size / source.getOriginalSize();
		font.getData().setScale(scale);
		final float x = (getAlign() == 2 ? region.x - region.width : (getAlign() == 1 ? region.x - region.width / 2 : region.x));
		if (source.getType() == SkinTextBitmapSource.TYPE_DISTANCE_FIELD ||
				source.getType() == SkinTextBitmapSource.TYPE_COLORED_DISTANCE_FIELD) {
			sprite.setType(SkinObjectRenderer.TYPE_DISTANCE_FIELD);
			setLayout(color, region);
			sprite.draw(font, layout, x + offsetX, region.y + offsetY + region.getHeight(), shader -> {
				shader.setUniformf("u_outlineDistance", Math.max(0.1f, 0.5f - getOutlineWidth()/2f));
				shader.setUniformf("u_outlineColor", getOutlineColor());
				shader.setUniformf("u_shadowColor", getShadowColor());
				shader.setUniformf("u_shadowSmoothing", getShadowSmoothness() / 2f);
				shader.setUniformf("u_shadowOffset",
						new Vector2(getShadowOffset().x / source.getPageWidth(), getShadowOffset().y / source.getPageHeight()));
			});
		} else {
			sprite.setType(SkinObjectRenderer.TYPE_BILINEAR);
			if (!getShadowOffset().isZero()) {
				setLayout(new Color(color.r / 2, color.g / 2, color.b / 2, color.a), region);
				sprite.draw(font, layout, x + getShadowOffset().x + offsetX, region.y - getShadowOffset().y + offsetY + region.getHeight());
			}
			setLayout(color, region);
			sprite.draw(font, layout, x + offsetX, region.y + offsetY + region.getHeight());
		}
		font.getData().setScale(1);
	}

	private void setLayout(Color c, Rectangle r) {
		if (isWrapping()) {
			layout.setText(font, getText(), c, r.getWidth(), ALIGN[getAlign()], true);
		} else {
			switch (getOverflow()) {
				case OVERFLOW_OVERFLOW -> {
					layout.setText(font, getText(), c, r.getWidth(), ALIGN[getAlign()], false);
				}
				case OVERFLOW_SHRINK -> {
					layout.setText(font, getText(), c, r.getWidth(), ALIGN[getAlign()], false);
					float actualWidth = layout.width;
					if (actualWidth > r.getWidth()) {
						font.getData().setScale(font.getData().scaleX * r.getWidth() / actualWidth, font.getData().scaleY);
						layout.setText(font, getText(), c, r.getWidth(), ALIGN[getAlign()], false);
					}
				}
				case OVERFLOW_TRUNCATE -> {
					layout.setText(font, getText(), 0, getText().length(), c, r.getWidth(), ALIGN[getAlign()], false, "");
				}
			}
		}
	}

	public void dispose() {
		source.dispose();
	}

	public static final class SkinTextBitmapSource implements Disposable {

		public static final int TYPE_STANDARD = 0;
		public static final int TYPE_DISTANCE_FIELD = 1;
		public static final int TYPE_COLORED_DISTANCE_FIELD = 2;

		private boolean usecim;
		private boolean useMipMaps;
		private Path fontPath;
		private BitmapFont.BitmapFontData fontData;
		private Array<TextureRegion> regions;
		private BitmapFont font;
		private float originalSize;
		private int type;
		private float pageWidth;
		private float pageHeight;

		public SkinTextBitmapSource(Path fontPath, boolean usecim) {
			this(fontPath, usecim, true);
		}

		public SkinTextBitmapSource(Path fontPath, boolean usecim, boolean useMipMaps) {
			this.usecim = usecim;
			this.useMipMaps = useMipMaps;
			this.fontPath = fontPath;
		}

		public CacheableBitmapFont createCacheableFont(Path _fontPath, int _type) {
			BitmapFont.BitmapFontData _fontData = null;
			Array<TextureRegion> _regions = null;
			BitmapFont _font = null;
			float _originalSize = 0;
			float _pageWidth = 0;
			float _pageHeight = 0;

			try {
				_fontData = new BitmapFont.BitmapFontData(new FileHandle(_fontPath.toFile()), false);

				_regions = new Array<>(_fontData.imagePaths.length);
				for (int i = 0; i < _fontData.imagePaths.length; ++i) {
					_regions.add(new TextureRegion(SkinLoader.getTexture(_fontData.imagePaths[i], usecim, useMipMaps)));
				}

				_font = new BitmapFont(_fontData, _regions, true);

				// size が BitmapFont から取得できないので、独自に取得する
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileHandle(_fontPath.toFile()).read()), 512)) {
					String line = reader.readLine();
					_originalSize = (float) Integer.parseInt(line.substring(line.indexOf("size=") + 5).split(" ")[0]);
					line = reader.readLine();
					_pageWidth = (float) Integer.parseInt(line.substring(line.indexOf("scaleW=") + 7).split(" ")[0]);
					_pageHeight = (float) Integer.parseInt(line.substring(line.indexOf("scaleH=") + 7).split(" ")[0]);
				} catch (Exception e) {
					_originalSize = _fontData.lineHeight;
					if (_regions.size > 0) {
						_pageWidth = (float) _regions.get(0).getRegionWidth();
						_pageHeight = (float) _regions.get(0).getRegionHeight();
					}
				}
			} catch (Exception e) {
				_font = null;
			}

			CacheableBitmapFont result = new CacheableBitmapFont();
			result.fontData = _fontData;
			result.regions = _regions;
			result.font = _font;
			result.originalSize = _originalSize;
			result.type = _type;
			result.pageWidth = _pageWidth;
			result.pageHeight = _pageHeight;

			return result;
		}

		public BitmapFont getFont() {
			if (!BitmapFontCache.Has(fontPath)) {
				CacheableBitmapFont _newFont = createCacheableFont(fontPath, type);
				BitmapFontCache.Set(fontPath, _newFont);
			}

			CacheableBitmapFont cached = BitmapFontCache.Get(fontPath);

			fontData = cached.fontData;
			regions = cached.regions;
			font = cached.font;
			originalSize = cached.originalSize;
			type = cached.type;
			pageWidth = cached.pageWidth;
			pageHeight = cached.pageHeight;

			return font;
		}

		public float getOriginalSize() {
			return originalSize;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public float getPageWidth() {
			return pageWidth;
		}

		public float getPageHeight() {
			return pageHeight;
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
