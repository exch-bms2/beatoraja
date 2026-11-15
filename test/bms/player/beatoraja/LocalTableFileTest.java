package bms.player.beatoraja;

import bms.player.beatoraja.song.SongData;
import java.io.IOException;
import java.nio.file.Files;

public final class LocalTableFileTest {
    public static void main(String[] args) throws IOException {
        var dir = Files.createTempDirectory("local-table-test");
        var path = dir.resolve(LocalTableFile.FILE_NAME);
        try {
            var file = new LocalTableFile(path);
            var draft = file.load();
            check(!Files.exists(path), "Opening a missing table must not create a file");
            String legacyDraft = "{\"name\":\"New Table\"}";
            Files.writeString(path, legacyDraft);
            draft = file.load();
            check("New Table".equals(draft.getName()) && draft.getFolder().length == 0
                    && draft.getCourse().length == 0, "Open a named empty draft written by an older launcher");
            expectSaveFailure(file, draft);
            check(TableData.read(path) == null, "Music Select must still reject an empty draft");
            draft.setName("First");
            var song = new SongData();
            song.setTitle("Chart");
            song.setMd5("a".repeat(32));
            var folder = new TableData.TableFolder();
            folder.setName("Folder");
            folder.setSong(new SongData[]{song});
            draft.setFolder(new TableData.TableFolder[]{folder});
            check(legacyDraft.equals(Files.readString(path)), "Opening and editing a draft must not rewrite the file");
            file.save(draft);
            check("First".equals(TableData.read(path).getName()), "Explicit save uses the runtime table format");
            var second = new LocalTableFile(path);
            var newer = second.load();
            newer.setName("Second");
            second.save(newer);
            draft.setName("Stale");
            expectSaveFailure(file, draft);
            check("Second".equals(TableData.read(path).getName()), "Stale save must not overwrite a newer table");
            newer.setFolder(TableData.TableFolder.EMPTY);
            expectSaveFailure(second, newer);
            check("Second".equals(TableData.read(path).getName()), "Invalid edits must not overwrite the existing table");
            Files.writeString(path, "{broken");
            check(TableData.read(path) == null, "Runtime reader must reject malformed replacement");
            try (var entries = Files.list(dir)) {
                check(entries.count() == 1, "No staging files remain");
            }
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(dir);
        }
    }

    private static void expectSaveFailure(LocalTableFile file, TableData data) throws IOException {
        try {
            file.save(data);
            throw new AssertionError("Expected save failure");
        } catch (IOException expected) { }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
