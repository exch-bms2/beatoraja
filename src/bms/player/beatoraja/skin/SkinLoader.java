package bms.player.beatoraja.skin;

import bms.player.beatoraja.*;
import bms.player.beatoraja.skin.json.JSONSkinLoader;
import bms.player.beatoraja.skin.lr2.LR2SkinCSVLoader;
import bms.player.beatoraja.skin.lr2.LR2SkinHeaderLoader;
import bms.player.beatoraja.skin.lua.LuaSkinLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.io.File;
import java.nio.file.*;

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
        final PlayerResource resource = state.main.getPlayerResource();
        try {
            SkinConfig sc = resource.getPlayerConfig().getSkin()[skinType.getId()];
            if (sc.getPath().endsWith(".json")) {
                JSONSkinLoader sl = new JSONSkinLoader(state, resource.getConfig());
                Skin skin = sl.loadSkin(Paths.get(sc.getPath()), skinType, sc.getProperties());
                SkinLoader.resource.disposeOld();
                return skin;
            } else if (sc.getPath().endsWith(".luaskin")) {
                LuaSkinLoader loader = new LuaSkinLoader(state, resource.getConfig());
                Skin skin = loader.loadSkin(Paths.get(sc.getPath()), skinType, sc.getProperties());
                SkinLoader.resource.disposeOld();
                return skin;
            } else {
                LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader(resource.getConfig());
                SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), state, sc.getProperties());
                LR2SkinCSVLoader dloader = LR2SkinCSVLoader.getSkinLoader(skinType,  header.getResolution(), resource.getConfig());
                Skin skin = dloader.loadSkin(Paths.get(sc.getPath()), state, header, loader.getOption(),
                        sc.getProperties());
                SkinLoader.resource.disposeOld();
                return skin;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        JSONSkinLoader sl = new JSONSkinLoader(state, resource.getConfig());
        Skin skin =  sl.loadSkin(Paths.get(SkinConfig.Default.get(skinType).path), skinType, new SkinConfig.Property());
        SkinLoader.resource.disposeOld();
        return skin;
    }

    public static PixmapResourcePool getResource() {
    	if(resource == null) {
    		initPixmapResourcePool(1);
    	}
        return resource;
    }

    protected static File getPath(String imagepath, ObjectMap<String, String> filemap) {
        File imagefile = new File(imagepath);
        for (String key : filemap.keys()) {
            if (imagepath.startsWith(key)) {
                String foot = imagepath.substring(key.length());
                imagefile = new File(
                        imagepath.substring(0, imagepath.lastIndexOf('*')) + filemap.get(key) + foot);
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

    protected static Texture getTexture(String path, boolean usecim) {
        return getTexture(path, usecim, false);
    }

    protected static Texture getTexture(String path, boolean usecim, boolean useMipMaps) {
    	final PixmapResourcePool resource = SkinLoader.getResource();
        if(resource.exists(path)) {
            return new Texture(resource.get(path), useMipMaps);
        }
        try {
            long modifiedtime = Files.getLastModifiedTime(Paths.get(path)).toMillis() / 1000;
            String cim = path.substring(0, path.lastIndexOf('.')) + "__" + modifiedtime + ".cim";
            if(resource.exists(cim)) {
                return new Texture(resource.get(cim), useMipMaps);
            }

            if (Files.exists(Paths.get(cim))) {
                Pixmap pixmap = resource.get(cim);
                return new Texture(pixmap, useMipMaps);
            } else if(usecim){
                Pixmap pixmap = resource.get(path);

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
                Pixmap pixmap = resource.get(path);
                return new Texture(pixmap, useMipMaps);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
