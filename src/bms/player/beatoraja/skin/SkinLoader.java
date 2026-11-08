package bms.player.beatoraja.skin;

import bms.player.beatoraja.*;
import bms.player.beatoraja.skin.json.JSONSkinLoader;
import bms.player.beatoraja.skin.lr2.LR2SkinCSVLoader;
import bms.player.beatoraja.skin.lr2.LR2SkinHeaderLoader;
import bms.player.beatoraja.skin.lua.LuaSkinLoader;
import bms.player.beatoraja.play.SkinNote;
import bms.player.beatoraja.play.SkinOffsetSettings;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.io.File;
import java.nio.file.*;
import java.util.Locale;

/**
 * スキンローダー
 *
 * @author exch
 */
public abstract class SkinLoader {
    /**
     * スキンイメージのリソースプール
     */
    private static PixmapResourcePool resource;
    private static final ThreadLocal<PixmapResourcePool> previewResources = new ThreadLocal<>();

    public static void initPixmapResourcePool(int gen) {
    	if(resource != null) {
    		resource.dispose();
    	}
    	resource = new PixmapResourcePool(gen);
    }
    
    /**
     * スキンデータを読み込む
     *
     * @param state
     * @param skinType スキンタイプ
     * @return
     */
    public static Skin load(MainState state, SkinType skinType) {
        SkinConfig skinConfig = state.resource.getPlayerConfig().getSkin()[skinType.getId()];
        Skin skin = null;
        if (skinConfig != null && skinConfig.validate()) {
            var mode = state.resource.getPlayMode();
            if (state instanceof bms.player.beatoraja.play.BMSPlayer && mode != null
                    && (mode.mode == bms.player.beatoraja.BMSPlayerMode.Mode.PRACTICE
                        || mode.mode == bms.player.beatoraja.BMSPlayerMode.Mode.AUTOPLAY)
                    && skinConfig.getProperties().getPendingSelection() != null) {
                skin = loadPending(skinConfig, candidate -> load(state, skinType, candidate));
                PlayerConfig.write(state.resource.getConfig().getPlayerpath(), state.resource.getPlayerConfig());
            }
            if (skin == null) skin = load(state, skinType, skinConfig);
        }
        if (skin instanceof bms.player.beatoraja.play.PlaySkin playSkin) {
            var properties = skinConfig.getProperties();
            if (SkinOffsetSettings.migrateLegacy(playSkin, properties, skinConfig.getPath())) {
                PlayerConfig.write(state.resource.getConfig().getPlayerpath(), state.resource.getPlayerConfig());
            } else if (properties.hasLegacyNoteAdjustment()) {
                java.util.logging.Logger.getGlobal().warning("Legacy note adjustment retained but not applied: "
                        + skinConfig.getPath() + " " + java.util.Arrays.toString(properties.getNoteAdjustment()));
            }
            playSkin.setEditableNoteProperties(properties);
        }
        if(skin == null) {
            SkinConfig.Default defaultSkin = SkinConfig.Default.get(skinType);
            if (defaultSkin == null) {
                return null;
            }
            skinConfig = new SkinConfig(defaultSkin.path);
            skin = load(state, skinType, skinConfig);
        }
        return skin;
    }

    static Skin loadPending(SkinConfig config, java.util.function.Function<SkinConfig, Skin> loader) {
        var property = config.getProperties();
        var pending = property.getPendingSelection();
        if (pending == null) return null;
        var candidate = new SkinConfig(config.getPath());
        var values = property.candidateFor(pending);
        candidate.setProperties(values);
        property.setPendingSelection(null);
        Skin loaded = loader.apply(candidate);
        if (loaded instanceof bms.player.beatoraja.play.PlaySkin) {
            property.setOption(values.getOption());
            property.setFile(values.getFile());
            return loaded;
        }
        if (loaded != null) loaded.dispose();
        java.util.logging.Logger.getGlobal().warning("Keeping previous skin settings after deferred load failed: " + config.getPath());
        return null;
    }

    public static Skin load(MainState state, SkinType skinType, SkinConfig sc) {
        return load(state, skinType, sc, true);
    }

    public static Skin loadPracticePreview(MainState state, SkinType skinType, SkinConfig sc) {
        var previous = previewResources.get();
        var temporary = new PixmapResourcePool();
        previewResources.set(temporary);
        try {
            return load(state, skinType, sc, false);
        } finally {
            if (previous == null) previewResources.remove();
            else previewResources.set(previous);
            temporary.dispose();
        }
    }

    private static Skin load(MainState state, SkinType skinType, SkinConfig sc, boolean ageCache) {
        final PlayerResource resource = state.resource;
        if (sc == null || !sc.validate()) {
            return null;
        }
        try {
            Skin skin;
            if (sc.getPath().endsWith(".json")) {
                JSONSkinLoader sl = new JSONSkinLoader(state, resource.getConfig());
                skin = sl.loadSkin(Paths.get(sc.getPath()), skinType, sc.getProperties());
            } else if (sc.getPath().endsWith(".luaskin")) {
                LuaSkinLoader loader = new LuaSkinLoader(state, resource.getConfig());
                skin = loader.loadSkin(Paths.get(sc.getPath()), skinType, sc.getProperties());
            } else {
                LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader(resource.getConfig());
                SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), state, sc.getProperties());
                LR2SkinCSVLoader<?> dloader = LR2SkinCSVLoader.getSkinLoader(skinType,  header.getResolution(), resource.getConfig());
                header.setSourceResolution(dloader.src);
                header.setDestinationResolution(dloader.dst);
                skin = dloader.loadSkin(state, header, loader.getOption());
            }
            applyNotesOffsetCenter(skin, sc.getProperties());
            if (ageCache) SkinLoader.resource.disposeOld();
            return skin;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    static void applyNotesOffsetCenter(Skin skin, SkinConfig.Property property) {
        if (skin != null && property.getNotesOffsetCenter() != 0) {
            for (SkinObject object : skin.getAllSkinObjects()) {
                if (object instanceof SkinNote) {
                    object.setOffsetCenter(property.getNotesOffsetCenter());
                }
            }
        }
    }

    public static PixmapResourcePool getResource() {
    	if(resource == null) {
    		initPixmapResourcePool(1);
    	}
        return resource;
    }

    public static File getPath(String imagepath, ObjectMap<String, String> filemap) {
        File imagefile = new File(imagepath);
        for (String key : filemap.keys()) {
            if (imagepath.startsWith(key)) {
                String foot = imagepath.substring(key.length());
                String filename = filemap.get(key);
                int wildcard = key.lastIndexOf('*');
                int basename = key.lastIndexOf('/', wildcard < 0 ? key.length() - 1 : wildcard) + 1;
                // The UI stores full filenames; retain the old prefix for legacy suffix-only values.
                int prefixEnd = wildcard < 0 || filename.regionMatches(true, 0, key, basename, wildcard - basename)
                        ? basename : wildcard;
                imagefile = new File(key.substring(0, prefixEnd) + filename + foot);
                // System.out.println(imagefile.getPath());
                imagepath = "";
                break;
            }
        }
        if (imagepath.contains("*")) {
            String ext = imagepath.substring(imagepath.lastIndexOf("*") + 1);
            if(imagepath.contains("|")) {
                if(imagepath.length() > imagepath.lastIndexOf('|') + 1) {
                    ext = imagepath.substring(imagepath.lastIndexOf("*") + 1, imagepath.indexOf('|')) + imagepath.substring(imagepath.lastIndexOf('|') + 1);
                } else {
                    ext = imagepath.substring(imagepath.lastIndexOf("*") + 1, imagepath.indexOf('|'));
                }
            }
            File imagedir = new File(imagepath.substring(0, imagepath.lastIndexOf('/')));
            if (imagedir.exists() && imagedir.isDirectory()) {
                Array<File> l = new Array<File>();
                for (File subfile : imagedir.listFiles()) {
                    if (subfile.getPath().toLowerCase().endsWith(ext)) {
                        l.add(subfile);
                    }
                }
                if (l.size > 0) {
                    imagefile = l.get((int) (Math.random() * l.size));
                }
            }
        }
        return imagefile;
    }

    public static Texture getTexture(String path, boolean usecim) {
        return getTexture(path, usecim, false);
    }

    public static Texture getTexture(String path, boolean usecim, boolean useMipMaps) {
        final PixmapResourcePool temporary = previewResources.get();
        final PixmapResourcePool resource = temporary == null ? SkinLoader.getResource() : temporary;
        if(resource.exists(path)) {
            return new Texture(resource.getPixmap(path), useMipMaps);
        }
        try {
            boolean usePixmapCache = temporary == null && usecim && !path.toLowerCase(Locale.ROOT).endsWith(".webp");
            long modifiedtime = Files.getLastModifiedTime(Paths.get(path)).toMillis() / 1000;
            String cim = path.substring(0, path.lastIndexOf('.')) + "__" + modifiedtime + ".cim";
            if(usePixmapCache && resource.exists(cim)) {
                return new Texture(resource.getPixmap(cim), useMipMaps);
            }

            if (usePixmapCache && Files.exists(Paths.get(cim))) {
                Pixmap pixmap = resource.getPixmap(cim);
                return new Texture(pixmap, useMipMaps);
            } else if(usePixmapCache){
                Pixmap pixmap = resource.getPixmap(path);

                try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(path).getParent())) {
                    for (Path p : paths) {
                        final String filename = p.toString();
                        if(filename.startsWith(path.substring(0, path.lastIndexOf('.')) + "__") && filename.endsWith(".cim")) {
                            Files.deleteIfExists(p);
                            break;
                        }
                    }
                } catch(Throwable e) {
                    e.printStackTrace();
                }
                PixmapIO.writeCIM(Gdx.files.local(cim), pixmap);

                Texture tex = new Texture(pixmap, useMipMaps);
                return tex;
            } else {
                Pixmap pixmap = resource.getPixmap(path);
                return new Texture(pixmap, useMipMaps);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
