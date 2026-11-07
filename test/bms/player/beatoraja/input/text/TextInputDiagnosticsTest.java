package bms.player.beatoraja.input.text;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.property.StringPropertyFactory;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** No native window required: opt-in privacy, text comparisons and the actual search writer route. */
public final class TextInputDiagnosticsTest {
    private static final String PROPERTY = "beatoraja.textInputDiagnostics";
    private static final String PREFIX = "Text input diagnostics ";

    public static void main(String[] args) throws Exception {
        String previous = System.getProperty(PROPERTY);
        Logger logger = Logger.getGlobal();
        boolean parents = logger.getUseParentHandlers();
        Level level = logger.getLevel();
        var records = new ArrayList<JsonValue>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) {
                if (record.getMessage().startsWith(PREFIX)) records.add(new JsonReader().parse(record.getMessage().substring(PREFIX.length())));
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(handler); logger.setUseParentHandlers(false); logger.setLevel(Level.ALL);
        try {
            System.clearProperty(PROPERTY);
            var raw = new TextInputSession(ImeBridge.NONE, state("private"));
            TextInputDiagnostics.editor("writer-submit", raw, "private", "private");
            TextInputDiagnostics.search("search-received", "private", -1, false);
            raw.requestFinish();
            check(records.isEmpty(), "Disabled by default: no editor/query contents logged");

            System.setProperty(PROPERTY, "true");
            var bridge = new MockImeBridge();
            var session = new TextInputSession(bridge, state(""));
            bridge.commit("as"); bridge.command(ImeBridge.Command.SUBMIT, false);
            session.drain();
            check(session.isFinishing() && session.state().text().equals("as"), "Queued native commit precedes submit");
            TextInputDiagnostics.editor("writer-submit", session, "", session.state().text());
            var submit = last(records, "writer-submit");
            check(submit.getString("display").isEmpty() && submit.getString("committed").equals("as")
                    && submit.getString("submitted").equals("as") && !submit.getBoolean("displayEqualsCommitted")
                    && submit.getBoolean("submittedEqualsCommitted"), "A stale display is distinguishable from a lost committed value");
            bridge.done(); session.drain();
            check(session.isFinished() && last(records, "session-close").getLong("session") == session.id(), "Completion traced with stable session ID");
            check(index(records, "ime-enqueue", "COMMITTED") < index(records, "ime-apply", "COMMITTED")
                    && index(records, "ime-apply", "COMMITTED") < index(records, "ime-apply", "COMMAND"), "Enqueue/apply order visible");

            var missing = new TextInputSession(new MockImeBridge(), state(""));
            TextInputDiagnostics.editor("writer-submit", missing, "as", "");
            submit = last(records, "writer-submit");
            check(submit.getString("display").equals("as") && submit.getString("committed").isEmpty()
                    && submit.getString("submitted").isEmpty(), "Displayed text with missing commit is identifiable");
            missing.close();

            String escaped = "\u65e5\u672c\u8a9e\"\\\r\n\u0000<&\ud842\udfb7";
            TextInputDiagnostics.search("search-escape", escaped, 0, false);
            check(last(records, "search-escape").getString("query").equals(escaped), "JSON escaping round-trips control characters and Unicode");
            String longText = "x".repeat(1023) + "\ud842\udfb7" + "tail";
            TextInputDiagnostics.search("search-long", longText, 0, false);
            var bounded = last(records, "search-long");
            check(bounded.getBoolean("queryTruncated") && bounded.getInt("queryLength") == longText.length()
                    && bounded.getString("query").length() == 1023, "Bounded records do not split surrogate pairs");
            TextInputDiagnostics.failure("test-failure", session.id(), new IllegalArgumentException("test\nmessage"));
            check(last(records, "test-failure").getString("exception").equals(IllegalArgumentException.class.getName()), "Failure type recorded");

            searchRoute(records);
        } finally {
            logger.removeHandler(handler); logger.setUseParentHandlers(parents); logger.setLevel(level);
            if (previous == null) System.clearProperty(PROPERTY); else System.setProperty(PROPERTY, previous);
        }
        System.out.println("TextInputDiagnosticsTest passed (opt-in, ordered IME events, text comparisons, escaping, limits and search writer)");
    }

    private static void searchRoute(List<JsonValue> records) throws Exception {
        var selector = KeyboardQueueTestSupport.allocate(MusicSelector.class);
        String[] observed = {null};
        var db = (SongDatabaseAccessor)Proxy.newProxyInstance(TextInputDiagnosticsTest.class.getClassLoader(),
                new Class<?>[]{SongDatabaseAccessor.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("getSongDatasByText")) throw new AssertionError(method);
                    observed[0] = (String)args[0]; return SongData.EMPTY;
                });
        KeyboardQueueTestSupport.set(selector, MusicSelector.class, "songdb", db);
        var writer = StringPropertyFactory.getStringWriter(30);
        writer.set(selector, "as");
        check("as".equals(observed[0]) && last(records, "search-received").getString("query").equals("as")
                && last(records, "search-results").getInt("matches") == 0, "Real ID 30 writer reaches search with unchanged text");
        observed[0] = null;
        writer.set(selector, " ");
        check(observed[0] == null && last(records, "search-skipped-blank").getString("query").equals(" "), "Blank query is explicitly diagnosed without DB call");
        writer.set(selector, null);
        check(last(records, "search-skipped-blank").get("query").isNull(), "Null query remains supported");
    }

    private static TextInputSession.EditorState state(String text) {
        return new TextInputSession.EditorState(text, text.length(), text.length());
    }
    private static JsonValue last(List<JsonValue> records, String phase) {
        for (int i = records.size() - 1; i >= 0; i--) if (records.get(i).getString("phase").equals(phase)) return records.get(i);
        throw new AssertionError("Missing phase: " + phase);
    }
    private static int index(List<JsonValue> records, String phase, String kind) {
        for (int i = 0; i < records.size(); i++) if (records.get(i).getString("phase").equals(phase)
                && records.get(i).getString("kind", "").equals(kind)) return i;
        throw new AssertionError("Missing event: " + phase + "/" + kind);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
