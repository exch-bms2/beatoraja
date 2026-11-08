package bms.player.beatoraja.skin;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.skin.json.JSONSkinLoader;
import bms.player.beatoraja.skin.lr2.LR2SkinHeaderLoader;
import bms.player.beatoraja.skin.lua.LuaSkinLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

public final class SkinCatalog {
    private SkinCatalog() { }

    public static List<SkinHeader> discover(Config config, Path root, SkinType type) {
        var result = new ArrayList<SkinHeader>();
        try (var files = Files.walk(root)) {
            files.filter(path -> Files.isRegularFile(path, NOFOLLOW_LINKS)).forEach(path -> {
                String name = path.toString();
                try {
                    SkinHeader header = name.endsWith(".json") ? new JSONSkinLoader().loadHeader(path)
                            : name.endsWith(".luaskin") ? LuaSkinLoader.loadSandboxedHeader(path)
                            : name.endsWith(".lr2skin") ? new LR2SkinHeaderLoader(config).loadSkin(path, null)
                            : null;
                    if (supports(header, type)) result.add(header);
                } catch (IOException | RuntimeException e) {
                    Logger.getGlobal().warning("Skipping unreadable skin header: " + path + " : " + e.getMessage());
                }
            });
        } catch (IOException | java.io.UncheckedIOException | SecurityException e) {
            Logger.getGlobal().warning("Skin scan incomplete: " + root + " : " + e.getMessage());
        }
        result.sort(Comparator.comparing((SkinHeader h) -> h.getName() == null ? "" : h.getName().toLowerCase(Locale.ROOT))
                .thenComparing(h -> h.getPath().toString()));
        return result;
    }

    public static boolean supports(SkinHeader header, SkinType type) {
        if (header == null || type == null) return false;
        if (header.getSkinType() == type) return true;
        return header.getType() == SkinHeader.TYPE_LR2SKIN
                && ((header.getSkinType() == SkinType.PLAY_7KEYS && type == SkinType.PLAY_5KEYS)
                    || (header.getSkinType() == SkinType.PLAY_14KEYS && type == SkinType.PLAY_10KEYS));
    }

    public static boolean samePath(String first, String second) {
        if (first == null || second == null) return false;
        try {
            return Path.of(first).toAbsolutePath().normalize().equals(Path.of(second).toAbsolutePath().normalize());
        } catch (RuntimeException e) {
            return false;
        }
    }
}
