package bms.player.beatoraja;

import com.badlogic.gdx.utils.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

/** Explicit edit session for the local, launcher-managed table. */
public final class LocalTableFile {
    public static final String FILE_NAME = "default.json";
    private final Path path;
    private byte[] original;

    public LocalTableFile(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public TableData load() throws IOException {
        byte[] bytes = readExisting();
        if (bytes == null) {
            original = null;
            var empty = new TableData();
            empty.setName("New Table");
            return empty;
        }
        TableData data = decode(new String(bytes, StandardCharsets.UTF_8), true);
        original = bytes;
        return data;
    }

    public static String snapshot(TableData data) {
        return TableData.toJson(data);
    }

    public void save(TableData draft) throws IOException {
        // Shrinking removes transient chart metadata; never mutate the editor's live rows.
        TableData copy = decode(snapshot(draft), false);
        copy.shrink();
        byte[] bytes = snapshot(copy).getBytes(StandardCharsets.UTF_8);
        Files.createDirectories(path.getParent());
        Path tmp = Files.createTempFile(path.getParent(), ".local-table-", ".tmp");
        try {
            Files.write(tmp, bytes);
            if (!Arrays.equals(original, readExisting())) {
                throw new IOException("The table changed on disk. Cancel and reopen it before saving.");
            }
            Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            original = bytes;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private byte[] readExisting() throws IOException {
        try {
            return Files.readAllBytes(path);
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    private static TableData decode(String text, boolean allowEmptyDraft) throws IOException {
        try {
            var json = new Json();
            json.setIgnoreUnknownFields(true);
            var data = json.fromJson(TableData.class, text);
            if (data != null) {
                // Older launchers wrote named drafts before any folder or course was added.
                boolean emptyDraft = allowEmptyDraft && data.getName() != null && !data.getName().isEmpty()
                        && (data.getFolder() == null || data.getFolder().length == 0)
                        && (data.getCourse() == null || data.getCourse().length == 0);
                if (data.validate() || emptyDraft) return data;
            }
        } catch (RuntimeException e) {
            throw new IOException("The local table could not be read.", e);
        }
        throw new IOException("The table needs a name and at least one valid folder or course.");
    }
}
