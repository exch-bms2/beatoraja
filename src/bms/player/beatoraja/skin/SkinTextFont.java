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

    private FreeTypeFontGenerator generator;
    private FreeTypeFontGenerator[] fallbackGenerators = new FreeTypeFontGenerator[0];
    private FreeTypeFontGenerator.FreeTypeFontParameter parameter;
    private String preparedFonts;
    private final Set<Integer> activeBmpFallbackGlyphs = new HashSet<>();
    private final Map<Integer, Character> activeSupplementaryGlyphMap = new HashMap<>();
    private final Map<Character, Integer> activePrivateUseGlyphMap = new HashMap<>();
    private int packedGlyphSequence;
    private static final int PRIVATE_USE_AREA_START = 0xe000;
    private static final int PRIVATE_USE_AREA_END = 0xf8ff;
    
    private final Color shadowcolor = new Color();

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
        } catch (GdxRuntimeException e) {
    		Logger.getGlobal().warning("Font準備失敗 : " + text + " - " + e.getMessage());
    	}
    }    

	@Override
	protected void prepareText(String text) {
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
        if(font != null) {
            font.getData().setScale(region.height / parameter.size);
            
            sprite.setType(getFilter() != 0 ? SkinObjectRenderer.TYPE_LINEAR : SkinObjectRenderer.TYPE_NORMAL);

            final float x = (getAlign() == 2 ? region.x - region.width : (getAlign() == 1 ? region.x - region.width / 2 : region.x));
            if(!getShadowOffset().isZero()) {
            	shadowcolor.set(color.r / 2, color.g / 2, color.b / 2, color.a);
                setLayout(shadowcolor, region);
                sprite.draw(font, layout, x + getShadowOffset().x + offsetX, region.y - getShadowOffset().y + offsetY + region.getHeight());
            }
            setLayout(color, region);
            sprite.draw(font, layout, x + offsetX, region.y + offsetY + region.getHeight());
        }
    }

    private void setLayout(Color c, Rectangle r) {
        String text = remapMissingGlyphs(getText());
        if (isWrapping()) {
            layout.setText(font, text, c, r.getWidth(), ALIGN[getAlign()], true);
        } else {
            switch (getOverflow()) {
            	case OVERFLOW_OVERFLOW -> layout.setText(font, text, c, r.getWidth(), ALIGN[getAlign()], false);
            	case OVERFLOW_SHRINK -> {
            		layout.setText(font, text, c, r.getWidth(), ALIGN[getAlign()], false);
            		float actualWidth = layout.width;
            		if (actualWidth > r.getWidth()) {
            			font.getData().setScale(font.getData().scaleX * r.getWidth() / actualWidth, font.getData().scaleY);
            			layout.setText(font, text, c, r.getWidth(), ALIGN[getAlign()], false);
            		}
            	}
            	case OVERFLOW_TRUNCATE -> layout.setText(font, text, 0, text.length(), c, r.getWidth(), ALIGN[getAlign()], false, "");
            }
        }
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
        Set<Integer> requiredCodePoints = null;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            if (codePoint > Character.MAX_VALUE) {
                if (result == null) {
                    result = new StringBuilder(text.length());
                    result.append(text, 0, index);
                }
                if (requiredCodePoints == null) {
                    requiredCodePoints = new HashSet<>();
                }
                requiredCodePoints.add(codePoint);
                Character mapped = ensureSupplementaryGlyphMapped(codePoint, requiredCodePoints);
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
        if (fallbackGenerators.length == 0 || activeBmpFallbackGlyphs.contains(codePoint)
                || font.getData().getGlyph((char) codePoint) != null) {
            return;
        }

        try {
            BitmapFont.Glyph glyph = createGlyph(codePoint, codePoint, true);
            if (glyph != null) {
                font.getData().setGlyph(codePoint, glyph);
                activeBmpFallbackGlyphs.add(codePoint);
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
                return null;
            }
            font.getData().setGlyph(mapped.charValue(), glyph);
            activeSupplementaryGlyphMap.put(codePoint, mapped);
            activePrivateUseGlyphMap.put(mapped, codePoint);
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
                return slot;
            }
        }
        return null;
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
