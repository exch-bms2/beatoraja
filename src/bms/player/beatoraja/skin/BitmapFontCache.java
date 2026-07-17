package bms.player.beatoraja.skin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class BitmapFontCache {
    static private final Map<CacheKey, CacheableBitmapFont> _cacheStore = new HashMap<>();

    static public class CacheableBitmapFont {
        public BitmapFont.BitmapFontData fontData;
        public Array<TextureRegion> regions;
        public BitmapFont font;
        public float originalSize;
        public int type;
        public float pageWidth;
        public float pageHeight;
        public int base;
        public int glyphYOffset;
        public Map<Integer, BitmapFont.Glyph> supplementaryGlyphs;
        private int references;
    }

    static public boolean Has(Path path) {
        return Has(path, 0);
    }

    static public boolean Has(Path path, int type) {
        if (path == null)
            return false;

        return _cacheStore.containsKey(new CacheKey(path, type));
    }

    static public void Set(Path path, CacheableBitmapFont font) {
        Set(path, font != null ? font.type : 0, font);
    }

    static public void Set(Path path, int type, CacheableBitmapFont font) {
        _cacheStore.put(new CacheKey(path, type), font);
    }

    static public CacheableBitmapFont acquire(Path path, int type, Supplier<CacheableBitmapFont> factory) {
        CacheKey key = new CacheKey(path, type);
        CacheableBitmapFont font = _cacheStore.get(key);
        if (font == null) {
            font = factory.get();
            _cacheStore.put(key, font);
        }
        font.references++;
        return font;
    }

    static public void release(Path path, int type) {
        CacheKey key = new CacheKey(path, type);
        CacheableBitmapFont font = _cacheStore.get(key);
        if (font == null || --font.references > 0) {
            return;
        }
        _cacheStore.remove(key);
        if (font.font != null) {
            font.font.dispose();
        }
    }

    static public CacheableBitmapFont Get(Path path) {
        return Get(path, 0);
    }

    static public CacheableBitmapFont Get(Path path, int type) {
        return _cacheStore.get(new CacheKey(path, type));
    }

    static private final class CacheKey {
        private final Path path;
        private final int type;

        private CacheKey(Path path, int type) {
            this.path = path;
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            return type == other.type && path.equals(other.path);
        }

        @Override
        public int hashCode() {
            return 31 * path.hashCode() + type;
        }
    }
}
