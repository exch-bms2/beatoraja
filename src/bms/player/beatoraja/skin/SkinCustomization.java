package bms.player.beatoraja.skin;

import bms.player.beatoraja.PixmapResourcePool;
import bms.player.beatoraja.SkinConfig;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static bms.player.beatoraja.skin.SkinProperty.OPTION_RANDOM_VALUE;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

public final class SkinCustomization {
    public static final String RANDOM_FILE = "Random";

    private SkinCustomization() { }

    private static Path directory(SkinHeader.CustomFile file) {
        int slash = file.path.lastIndexOf('/');
        return slash < 0 ? Path.of(".") : Path.of(file.path.substring(0, Math.max(1, slash)));
    }

    public static List<String> fileChoices(SkinHeader.CustomFile file) {
        if (file.path == null || file.path.isEmpty()) return List.of();
        String glob = file.path.substring(file.path.lastIndexOf('/') + 1).replace("|", "");
        if (glob.isEmpty()) return List.of();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory(file),
                "{" + glob.toLowerCase(Locale.ROOT) + "," + glob.toUpperCase(Locale.ROOT) + "}")) {
            var result = new ArrayList<String>();
            for (Path path : paths) {
                String name = path.getFileName().toString();
                if (!name.equals(RANDOM_FILE) && Files.isRegularFile(path, NOFOLLOW_LINKS)) result.add(name);
            }
            result.sort(String.CASE_INSENSITIVE_ORDER.thenComparing(java.util.Comparator.naturalOrder()));
            return result;
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
    }

    public static String defaultFile(SkinHeader.CustomFile file, List<String> choices) {
        if (file.def != null) {
            for (String name : choices) {
                int dot = name.lastIndexOf('.');
                if (name.equalsIgnoreCase(file.def) || (dot >= 0 && name.substring(0, dot).equalsIgnoreCase(file.def)))
                    return name;
            }
        }
        return choices.isEmpty() ? null : choices.get(0);
    }

    /** Resolve and validate changed choices before the loader uses them to map paths. */
    static Map<String, String> resolveRequiredFiles(SkinHeader header, SkinConfig.Selection selection) {
        if (selection == null) return Map.of();
        for (var change : selection.options()) {
            boolean allowed = Arrays.stream(header.getCustomOptions()).anyMatch(option ->
                    change.name.equals(option.name) && option.option.length > 0 &&
                            (change.value == OPTION_RANDOM_VALUE || Arrays.stream(option.option).anyMatch(id -> id == change.value)));
            if (!allowed) throw new IllegalArgumentException("Skin option no longer available: " + change.name);
        }
        var resolved = new HashMap<String, String>();
        for (var change : selection.files()) {
            var file = Arrays.stream(header.getCustomFiles()).filter(item -> change.name.equals(item.name))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Skin file option no longer available: " + change.name));
            var choices = fileChoices(file);
            String name = change.path;
            if (RANDOM_FILE.equals(name) && !choices.isEmpty())
                name = choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
            if (!choices.contains(name))
                throw new IllegalArgumentException("Skin file no longer available: " + change.name + " / " + change.path);
            var path = directory(file).resolve(name);
            if (!Files.isReadable(path))
                throw new IllegalArgumentException("Skin file is not readable: " + path);
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.matches(".*\\.(png|jpe?g|bmp|gif|webp|tga|cim)$")) {
                var image = PixmapResourcePool.loadPictureResource(path.toString());
                if (image == null) throw new IllegalArgumentException("Skin image could not be decoded: " + path);
                image.dispose();
            }
            resolved.put(change.name, name);
        }
        return resolved;
    }
}
