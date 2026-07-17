package bms.player.beatoraja.skin;

import bms.player.beatoraja.skin.property.StringProperty;
import bms.player.beatoraja.skin.property.StringPropertyFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.GlyphLayout.GlyphRun;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;

import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * フォントデータをソースとして持つスキン用テキスト
 * 
 * @author exch
 */
public final class SkinTextFont extends SkinText {

    /**
     * ビットマップフォント
     */
    private BitmapFont font;

    private GlyphLayout layout;
    private final GlyphLayout shadowLayout = new GlyphLayout();

    private FreeTypeFontGenerator generator;
    private FreeTypeFontGenerator[] fallbackGenerators = new FreeTypeFontGenerator[0];
    private FreeTypeFontGenerator.FreeTypeFontParameter parameter;
    private String preparedFonts;
    private final Set<Integer> activeBmpFallbackGlyphs = new HashSet<>();
    private final Set<Integer> requiredSupplementaryGlyphs = new HashSet<>();
    private final Map<Integer, Character> activeSupplementaryGlyphMap = new HashMap<>();
    private final Map<Character, Integer> activePrivateUseGlyphMap = new HashMap<>();
    private int packedGlyphSequence;
    private static final int PRIVATE_USE_AREA_START = 0xe000;
    private static final int PRIVATE_USE_AREA_END = 0xf8ff;
    private static final int[] MISSING_GLYPH_CANDIDATES = { 0x25a1, 0x25a2, 0x2610, 0x25a0, '?' }; // □, ▢, ☐, ■, ?
    
    private final Color shadowcolor = new Color();
    private String layoutText;
    private float layoutWidth = Float.NaN;
    private float layoutScaleY = Float.NaN;
    private float layoutScaleX;
    private int layoutAlign;
    private int layoutOverflow;
    private boolean layoutWrapping;
    private boolean layoutHasShadow;
    private boolean layoutValid;
    private long glyphRevision;

    public SkinTextFont(String fontpath, int cycle, int size, int shadow) {
        this(fontpath, new String[0], cycle, size, shadow, StringPropertyFactory.getStringProperty(-1));
    }

    public SkinTextFont(String fontpath, int cycle, int size, int shadow, StringProperty property) {
        this(fontpath, new String[0], cycle, size, shadow, property);
    }

    public SkinTextFont(String fontpath, String[] fallbackFontPaths, int cycle, int size, int shadow, StringProperty property) {
    	super(property);
    	try {
            generator = new FreeTypeFontGenerator(Gdx.files.internal(fontpath));
            fallbackGenerators = loadFallbackGenerators(fallbackFontPaths);
            parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.characters = "";
            parameter.incremental = true;
//            this.setCycle(cycle);
            parameter.size = size;
            setShadowOffset(new Vector2(shadow, shadow));    		
    	} catch (GdxRuntimeException e) {
    		Logger.getGlobal().warning("Skin Font読み込み失敗");
    	}
    }

    private FreeTypeFontGenerator[] loadFallbackGenerators(String[] fallbackFontPaths) {
        if (fallbackFontPaths == null || fallbackFontPaths.length == 0) {
            return new FreeTypeFontGenerator[0];
        }

        FreeTypeFontGenerator[] generators = new FreeTypeFontGenerator[fallbackFontPaths.length];
        int count = 0;
        for (String fallbackFontPath : fallbackFontPaths) {
            if (fallbackFontPath == null || fallbackFontPath.isEmpty()) {
                continue;
            }
            try {
                generators[count++] = new FreeTypeFontGenerator(Gdx.files.internal(fallbackFontPath));
            } catch (GdxRuntimeException e) {
                Logger.getGlobal().warning("Fallback Skin Font load failed: " + fallbackFontPath);
            }
        }

        FreeTypeFontGenerator[] result = new FreeTypeFontGenerator[count];
        System.arraycopy(generators, 0, result, 0, count);
        return result;
    }
    
    public boolean validate() {
    	if(generator == null) {
    		return false;
    	}
    	return super.validate();
    }

    @Override
    public boolean requiresFontPreparation() {
        return true;
    }

    public void prepareFont(String text) {
        if(font != null) {
            font.dispose();                	
            font = null;
        }
        
        try {
            parameter.characters = toBmpCharacters(text);
            font = generator.generateFont(parameter);
            disableIncrementalGlyphGeneration();
            layout = new GlyphLayout(font, "");
            preparedFonts = text;
            clearSupplementaryGlyphs();
            invalidateLayout();
        } catch (GdxRuntimeException e) {
    		Logger.getGlobal().warning("Font準備失敗 : " + text + " - " + e.getMessage());
    	}
    }    

	@Override
	protected void prepareText(String text) {
        invalidateLayout();
        if(preparedFonts != null) {
        	return;
        }
        if(font != null) {
            font.dispose();
            font = null;
        }
        
        try {
            parameter.characters = toBmpCharacters(text);
            font = generator.generateFont(parameter);
            disableIncrementalGlyphGeneration();
            layout = new GlyphLayout(font, "");
            clearSupplementaryGlyphs();
    	} catch (GdxRuntimeException e) {
    		Logger.getGlobal().warning("Font準備失敗 : " + text + " - " + e.getMessage());
    	}
	}
	
	@Override
    public void draw(SkinObjectRenderer sprite, float offsetX, float offsetY) {
        if (font == null || layout == null) {
            return;
        }

        updateLayout(region);
        font.getData().setScale(layoutScaleX, layoutScaleY);
        sprite.setType(getFilter() != 0 ? SkinObjectRenderer.TYPE_LINEAR : SkinObjectRenderer.TYPE_NORMAL);

        final float x = getAlign() == 2 ? region.x - region.width : getAlign() == 1 ? region.x - region.width / 2 : region.x;
        if (layoutHasShadow) {
            shadowcolor.set(color.r / 2, color.g / 2, color.b / 2, color.a);
            applyColor(shadowLayout, shadowcolor);
            sprite.draw(font, shadowLayout, x + getShadowOffset().x + offsetX,
                    region.y - getShadowOffset().y + offsetY + region.getHeight());
        }
        applyColor(layout, color);
        sprite.draw(font, layout, x + offsetX, region.y + offsetY + region.getHeight());
    }

    @Override
    public CachedTextLayout createCachedTextLayout(String text) {
        if (font == null) {
            return null;
        }

        float scaleY = region.height / parameter.size;
        boolean hasShadow = !getShadowOffset().isZero();
        String remappedText = remapMissingGlyphs(text);
        float scaleX = scaleY;
        GlyphLayout cachedLayout = new GlyphLayout();
        font.getData().setScale(scaleX, scaleY);
        setLayout(cachedLayout, remappedText, region.width);
        if (!isWrapping() && getOverflow() == OVERFLOW_SHRINK && cachedLayout.width > region.width) {
            scaleX *= region.width / cachedLayout.width;
            font.getData().setScale(scaleX, scaleY);
            setLayout(cachedLayout, remappedText, region.width);
        }
        GlyphLayout cachedShadowLayout = null;
        if (hasShadow) {
            cachedShadowLayout = new GlyphLayout();
            setLayout(cachedShadowLayout, remappedText, region.width);
        }
        return new FontTextLayout(cachedLayout, cachedShadowLayout, region.width, scaleX, scaleY, glyphRevision,
                getAlign(), getOverflow(), isWrapping());
    }

    @Override
    public boolean isCachedTextLayoutValid(CachedTextLayout layout) {
        if (!(layout instanceof FontTextLayout cached) || font == null) {
            return false;
        }
        return cached.width == region.width && cached.scaleY == region.height / parameter.size
                && cached.glyphRevision == glyphRevision && cached.align == getAlign()
                && cached.overflow == getOverflow() && cached.wrapping == isWrapping()
                && (cached.shadowLayout != null) == !getShadowOffset().isZero();
    }

    @Override
    public boolean drawCachedTextLayout(SkinObjectRenderer sprite, CachedTextLayout layout, float offsetX, float offsetY) {
        if (!(layout instanceof FontTextLayout cached) || !isCachedTextLayoutValid(cached)) {
            return false;
        }

        font.getData().setScale(cached.scaleX, cached.scaleY);
        sprite.setType(getFilter() != 0 ? SkinObjectRenderer.TYPE_LINEAR : SkinObjectRenderer.TYPE_NORMAL);
        final float x = getAlign() == 2 ? region.x - region.width : getAlign() == 1 ? region.x - region.width / 2 : region.x;
        if (cached.shadowLayout != null) {
            shadowcolor.set(color.r / 2, color.g / 2, color.b / 2, color.a);
            applyColor(cached.shadowLayout, shadowcolor);
            sprite.draw(font, cached.shadowLayout, x + getShadowOffset().x + offsetX,
                    region.y - getShadowOffset().y + offsetY + region.getHeight());
        }
        applyColor(cached.layout, color);
        sprite.draw(font, cached.layout, x + offsetX, region.y + offsetY + region.getHeight());
        return true;
    }

    private void updateLayout(Rectangle r) {
        String text = getText();
        float scaleY = r.height / parameter.size;
        boolean hasShadow = !getShadowOffset().isZero();
        if (layoutValid && text.equals(layoutText) && layoutWidth == r.width && layoutScaleY == scaleY
                && layoutAlign == getAlign() && layoutOverflow == getOverflow() && layoutWrapping == isWrapping()
                && layoutHasShadow == hasShadow) {
            return;
        }

        String remappedText = remapMissingGlyphs(text);
        float scaleX = scaleY;
        font.getData().setScale(scaleX, scaleY);
        setLayout(layout, remappedText, r.width);
        if (!isWrapping() && getOverflow() == OVERFLOW_SHRINK && layout.width > r.width) {
            scaleX *= r.width / layout.width;
            font.getData().setScale(scaleX, scaleY);
            setLayout(layout, remappedText, r.width);
        }
        if (hasShadow) {
            setLayout(shadowLayout, remappedText, r.width);
        } else {
            shadowLayout.reset();
        }

        layoutText = text;
        layoutWidth = r.width;
        layoutScaleX = scaleX;
        layoutScaleY = scaleY;
        layoutAlign = getAlign();
        layoutOverflow = getOverflow();
        layoutWrapping = isWrapping();
        layoutHasShadow = hasShadow;
        layoutValid = true;
    }

    private void setLayout(GlyphLayout target, String text, float width) {
        if (isWrapping()) {
            target.setText(font, text, Color.WHITE, width, ALIGN[getAlign()], true);
        } else {
            switch (getOverflow()) {
                case OVERFLOW_OVERFLOW, OVERFLOW_SHRINK -> target.setText(font, text, Color.WHITE, width, ALIGN[getAlign()], false);
                case OVERFLOW_TRUNCATE -> target.setText(font, text, 0, text.length(), Color.WHITE, width, ALIGN[getAlign()], false, "");
            }
        }
    }

    private void applyColor(GlyphLayout target, Color color) {
        for (GlyphRun run : target.runs) {
            run.color.set(color);
        }
    }

    private void invalidateLayout() {
        layoutValid = false;
    }

    private String toBmpCharacters(String text) {
        StringBuilder result = null;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            if (codePoint > Character.MAX_VALUE) {
                if (result == null) {
                    result = new StringBuilder(text.length());
                    result.append(text, 0, index);
                }
            } else if (result != null) {
                result.append((char) codePoint);
            }
            index += Character.charCount(codePoint);
        }
        return result != null ? result.toString() : text;
    }

    private String remapMissingGlyphs(String text) {
        StringBuilder result = null;
        requiredSupplementaryGlyphs.clear();
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            if (codePoint > Character.MAX_VALUE) {
                if (result == null) {
                    result = new StringBuilder(text.length());
                    result.append(text, 0, index);
                }
                requiredSupplementaryGlyphs.add(codePoint);
                Character mapped = ensureSupplementaryGlyphMapped(codePoint, requiredSupplementaryGlyphs);
                if (mapped == null) {
                    index += Character.charCount(codePoint);
                    continue;
                }
                result.append(mapped.charValue());
            } else if (result != null) {
                ensureBmpFallbackGlyph(codePoint);
                result.append((char) codePoint);
            } else {
                ensureBmpFallbackGlyph(codePoint);
            }
            index += Character.charCount(codePoint);
        }
        return result != null ? result.toString() : text;
    }

    private void ensureBmpFallbackGlyph(int codePoint) {
        if (activeBmpFallbackGlyphs.contains(codePoint) || font.getData().getGlyph((char) codePoint) != null) {
            return;
        }

        try {
            BitmapFont.Glyph glyph = createGlyph(codePoint, codePoint, true);
            if (glyph == null) {
                glyph = createMissingGlyph(codePoint);
            }
            if (glyph != null) {
                font.getData().setGlyph(codePoint, glyph);
                activeBmpFallbackGlyphs.add(codePoint);
                glyphRevision++;
            }
        } catch (Exception e) {
        }
    }

    private Character ensureSupplementaryGlyphMapped(int codePoint, Set<Integer> requiredCodePoints) {
        Character mapped = activeSupplementaryGlyphMap.get(codePoint);
        if (mapped != null) {
            return mapped;
        }

        mapped = findPrivateUseSlot(requiredCodePoints);
        if (mapped == null) {
            return null;
        }

        try {
            BitmapFont.Glyph glyph = createGlyph(codePoint, mapped.charValue(), false);
            if (glyph == null) {
                glyph = createMissingGlyph(mapped.charValue());
            }
            if (glyph == null) {
            	return null;
            }
            font.getData().setGlyph(mapped.charValue(), glyph);
            activeSupplementaryGlyphMap.put(codePoint, mapped);
            activePrivateUseGlyphMap.put(mapped, codePoint);
            glyphRevision++;
            return mapped;
        } catch (Exception e) {
            return null;
        }
    }

    private Character findPrivateUseSlot(Set<Integer> requiredCodePoints) {
        for (int codePoint = PRIVATE_USE_AREA_START; codePoint <= PRIVATE_USE_AREA_END; codePoint++) {
            Character slot = Character.valueOf((char) codePoint);
            if (!activePrivateUseGlyphMap.containsKey(slot)) {
                return slot;
            }
        }

        for (int codePoint = PRIVATE_USE_AREA_START; codePoint <= PRIVATE_USE_AREA_END; codePoint++) {
            Character slot = Character.valueOf((char) codePoint);
            Integer activeCodePoint = activePrivateUseGlyphMap.get(slot);
            if (!requiredCodePoints.contains(activeCodePoint)) {
                font.getData().setGlyph(slot.charValue(), null);
                activePrivateUseGlyphMap.remove(slot);
                activeSupplementaryGlyphMap.remove(activeCodePoint);
                glyphRevision++;
                return slot;
            }
        }
        return null;
    }

    private BitmapFont.Glyph createMissingGlyph(int mappedCodePoint) throws Exception {
        for (int candidate : MISSING_GLYPH_CANDIDATES) {
            BitmapFont.Glyph glyph = createGlyph(generator, candidate, mappedCodePoint);
            if (glyph != null) {
                return glyph;
            }
            for (FreeTypeFontGenerator fallbackGenerator : fallbackGenerators) {
                glyph = createGlyph(fallbackGenerator, candidate, mappedCodePoint);
                if (glyph != null) {
                    return glyph;
                }
            }
        }

        PixmapPacker packer = getFontDataPacker();
        if (packer == null) {
            return null;
        }

        int height = Math.max(8, parameter.size);
        int width = Math.max(6, Math.round(height * 0.75f));
        int stroke = Math.max(1, height / 12);
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        try {
            pixmap.setColor(0, 0, 0, 0);
            pixmap.fill();
            Color c = parameter.color != null ? parameter.color : Color.WHITE;
            pixmap.setColor(c);
            for (int i = 0; i < stroke; i++) {
                pixmap.drawRectangle(i, i, width - i * 2, height - i * 2);
            }

            String glyphName = "missing-" + mappedCodePoint + "-" + packedGlyphSequence++;
            Rectangle packed = packer.pack(glyphName, pixmap);
            int page = packer.getPageIndex(glyphName);
            if (page < 0) {
                return null;
            }
            packer.updateTextureRegions(font.getRegions(), parameter.minFilter, parameter.magFilter, parameter.genMipMaps);

            BitmapFont.Glyph glyph = new BitmapFont.Glyph();
            glyph.id = mappedCodePoint;
            glyph.srcX = (int) packed.x;
            glyph.srcY = (int) packed.y;
            glyph.width = width;
            glyph.height = height;
            glyph.xoffset = 0;
            glyph.yoffset = -height;
            glyph.xadvance = width + Math.max(1, stroke);
            glyph.page = page;
            font.getData().setGlyphRegion(glyph, font.getRegion(glyph.page));
            return glyph;
        } finally {
            pixmap.dispose();
        }
    }

    private BitmapFont.Glyph createGlyph(int codePoint, int mappedCodePoint, boolean fallbackOnly) throws Exception {
        if (!fallbackOnly) {
            BitmapFont.Glyph glyph = createGlyph(generator, codePoint, mappedCodePoint);
            if (glyph != null) {
                return glyph;
            }
        }
        for (FreeTypeFontGenerator fallbackGenerator : fallbackGenerators) {
            BitmapFont.Glyph glyph = createGlyph(fallbackGenerator, codePoint, mappedCodePoint);
            if (glyph != null) {
                return glyph;
            }
        }
        return null;
    }

    private BitmapFont.Glyph createGlyph(FreeTypeFontGenerator generator, int codePoint, int mappedCodePoint) throws Exception {
        FreeTypeFontGenerator.GlyphAndBitmap glyphAndBitmap =
                generator.generateGlyphAndBitmap(codePoint, parameter.size, parameter.flip);
        if (glyphAndBitmap == null || glyphAndBitmap.glyph == null || glyphAndBitmap.bitmap == null
                || glyphAndBitmap.bitmap.getWidth() == 0 || glyphAndBitmap.bitmap.getRows() == 0) {
            return null;
        }

        Pixmap pixmap = glyphAndBitmap.bitmap.getPixmap(Pixmap.Format.RGBA8888, parameter.color, parameter.gamma);
        try {
            PixmapPacker packer = getFontDataPacker();
            if (packer == null) {
                return null;
            }
            String glyphName = "fallback-" + mappedCodePoint + "-" + codePoint + "-" + packedGlyphSequence++;
            Rectangle packed = packer.pack(glyphName, pixmap);
            int page = packer.getPageIndex(glyphName);
            if (page < 0) {
                return null;
            }
            packer.updateTextureRegions(font.getRegions(), parameter.minFilter, parameter.magFilter, parameter.genMipMaps);

            BitmapFont.Glyph glyph = copyGlyph(glyphAndBitmap.glyph, mappedCodePoint);
            glyph.page = page;
            glyph.srcX = (int) packed.x;
            glyph.srcY = (int) packed.y;
            font.getData().setGlyphRegion(glyph, font.getRegion(glyph.page));
            return glyph;
        } finally {
            pixmap.dispose();
        }
    }

    private PixmapPacker getFontDataPacker() throws Exception {
        Field packerField = font.getData().getClass().getDeclaredField("packer");
        packerField.setAccessible(true);
        return (PixmapPacker) packerField.get(font.getData());
    }

    private void disableIncrementalGlyphGeneration() {
        try {
            Field generatorField = font.getData().getClass().getDeclaredField("generator");
            generatorField.setAccessible(true);
            generatorField.set(font.getData(), null);
        } catch (Exception e) {
        }
    }

    private BitmapFont.Glyph copyGlyph(BitmapFont.Glyph source, int mappedCodePoint) {
        BitmapFont.Glyph glyph = new BitmapFont.Glyph();
        glyph.id = mappedCodePoint;
        glyph.srcX = source.srcX;
        glyph.srcY = source.srcY;
        glyph.width = source.width;
        glyph.height = source.height;
        glyph.xoffset = source.xoffset;
        glyph.yoffset = source.yoffset;
        glyph.xadvance = source.xadvance;
        glyph.page = source.page;
        return glyph;
    }

    private void clearSupplementaryGlyphs() {
        activeBmpFallbackGlyphs.clear();
        activeSupplementaryGlyphMap.clear();
        activePrivateUseGlyphMap.clear();
        packedGlyphSequence = 0;
        glyphRevision++;
    }

    private static final class FontTextLayout implements CachedTextLayout {
        private final GlyphLayout layout;
        private final GlyphLayout shadowLayout;
        private final float width;
        private final float scaleX;
        private final float scaleY;
        private final long glyphRevision;
        private final int align;
        private final int overflow;
        private final boolean wrapping;

        private FontTextLayout(GlyphLayout layout, GlyphLayout shadowLayout, float width, float scaleX, float scaleY,
                long glyphRevision, int align, int overflow, boolean wrapping) {
            this.layout = layout;
            this.shadowLayout = shadowLayout;
            this.width = width;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.glyphRevision = glyphRevision;
            this.align = align;
            this.overflow = overflow;
            this.wrapping = wrapping;
        }
    }

    public void dispose() {
    	Optional.ofNullable(generator).ifPresent(FreeTypeFontGenerator::dispose);
        for (FreeTypeFontGenerator fallbackGenerator : fallbackGenerators) {
            Optional.ofNullable(fallbackGenerator).ifPresent(FreeTypeFontGenerator::dispose);
        }
    	Optional.ofNullable(font).ifPresent(BitmapFont::dispose);
    	setDisposed();
    }
}
