package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PixmapResourcePool;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.lr2.LR2SkinCSVLoader;
import bms.player.beatoraja.skin.lr2.LR2SkinHeaderLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;

import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * スキンローダー
 *
 * @author exch
 */
public abstract class SkinLoader {
    /**
     * スキンイメージのリソースプール
     */
    private static final PixmapResourcePool resource = new PixmapResourcePool();

    /**
     * スキンデータを読み込む
     *
     * @param state
     * @param skinType スキンタイプ
     * @return
     */
    public static Skin load(MainState state, SkinType skinType) {
        final PlayerResource resource = state.getMainController().getPlayerResource();
        try {
            SkinConfig sc = resource.getPlayerConfig().getSkin()[skinType.getId()];
            if (sc.getPath().endsWith(".json")) {
                JSONSkinLoader sl = new JSONSkinLoader(resource.getConfig());
                return sl.loadSkin(Paths.get(sc.getPath()), skinType, sc.getProperties());
            } else {
                LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
                SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), state, sc.getProperties());
                LR2SkinCSVLoader dloader = LR2SkinCSVLoader.getSkinLoader(skinType,  header.getResolution(), resource.getConfig());
                return dloader.loadSkin(Paths.get(sc.getPath()).toFile(), state, header, loader.getOption(),
                        sc.getProperties());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        JSONSkinLoader sl = new JSONSkinLoader(resource.getConfig());
        return sl.loadSkin(Paths.get(SkinConfig.defaultSkinPathMap.get(skinType)), skinType, new SkinConfig.Property());
    }

    public static PixmapResourcePool getResource() {
        return resource;
    }

    protected static File getPath(String imagepath, Map<String, String> filemap) {
        File imagefile = new File(imagepath);
        for (String key : filemap.keySet()) {
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
            File imagedir = new File(imagepath.substring(0, imagepath.lastIndexOf('/')));
            if (imagedir.exists() && imagedir.isDirectory()) {
                List<File> l = new ArrayList<File>();
                for (File subfile : imagedir.listFiles()) {
                    if (subfile.getPath().toLowerCase().endsWith(ext)) {
                        l.add(subfile);
                    }
                }
                if (l.size() > 0) {
                    imagefile = l.get((int) (Math.random() * l.size()));
                }
            }
        }
        return imagefile;
    }

    protected static Texture getTexture(String path, boolean usecim) {
        if(resource.exists(path)) {
            return new Texture(resource.get(path));
        }
        try {
            long modifiedtime = Files.getLastModifiedTime(Paths.get(path)).toMillis() / 1000;
            String cim = path.substring(0, path.lastIndexOf('.')) + "__" + modifiedtime + ".cim";
            if(resource.exists(cim)) {
                return new Texture(resource.get(cim));
            }

            if (Files.exists(Paths.get(cim))) {
                Pixmap pixmap = resource.get(cim);
                return new Texture(pixmap);
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

                Texture tex = new Texture(pixmap);
                return tex;
            } else {
                Pixmap pixmap = resource.get(path);
                return new Texture(pixmap);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }


}
