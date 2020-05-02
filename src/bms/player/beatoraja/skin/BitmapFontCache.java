package bms.player.beatoraja.skin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class BitmapFontCache {
    static private final Map<Path, CacheableBitmapFont> _cacheStore = new HashMap<>();

    static public class CacheableBitmapFont {
        public BitmapFont.BitmapFontData fontData;
        public Array<TextureRegion> regions;
        public BitmapFont font;
        public float originalSize;
        public int type;
        public float pageWidth;
        public float pageHeight;
    }

    static public boolean Has(Path path) {
        if (path == null)
            return false;

        return _cacheStore.containsKey(path);
    }

    static public void Set(Path path, CacheableBitmapFont font) {
        _cacheStore.put(path, font);
    }

    static public CacheableBitmapFont Get(Path path) {
        return _cacheStore.get(path);
    }
}
