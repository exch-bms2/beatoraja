package bms.player.beatoraja.input;

import java.util.logging.Logger;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

/** Opt-in text/IME diagnostics. Records editor contents, so never enabled by default. */
public final class TextInputDiagnostics {
    private static final int TEXT_LIMIT = 1024;

    private TextInputDiagnostics() { }

    public static boolean isEnabled() {
        return Boolean.getBoolean("beatoraja.textInputDiagnostics");
    }

    public static void editor(String phase, TextInputSession session, String display, String submitted) {
        if (!isEnabled()) return;
        var record = record(phase, session != null ? session.id() : 0);
        text(record, "display", display);
        text(record, "committed", session != null ? session.state().text() : null);
        text(record, "submitted", submitted);
        if (session != null) {
            flags(record, session);
            text(record, "preedit", session.preedit());
            record.addChild("displayEqualsCommitted", new JsonValue(java.util.Objects.equals(display, session.state().text())));
            record.addChild("submittedEqualsCommitted", new JsonValue(java.util.Objects.equals(submitted, session.state().text())));
        }
        write(record);
    }

    static void session(String phase, TextInputSession session, ImeBridge bridge) {
        if (!isEnabled()) return;
        var record = record(phase, session.id());
        record.addChild("bridge", new JsonValue(bridge.getClass().getName()));
        text(record, "committed", session.state().text());
        text(record, "preedit", session.preedit());
        flags(record, session);
        write(record);
    }

    static void event(String phase, long id, String kind, TextInputSession.EditorState state,
            String preedit, int caret, ImeBridge.Command command, boolean consumed) {
        if (!isEnabled()) return;
        var record = record(phase, id);
        record.addChild("kind", new JsonValue(kind));
        text(record, "committed", state != null ? state.text() : null);
        if (state != null) {
            record.addChild("anchor", new JsonValue(state.anchor()));
            record.addChild("cursor", new JsonValue(state.cursor()));
        }
        text(record, "preedit", preedit);
        record.addChild("caret", new JsonValue(caret));
        record.addChild("command", new JsonValue(command != null ? command.name() : null));
        record.addChild("consumed", new JsonValue(consumed));
        write(record);
    }

    public static void search(String phase, String query, int matches, boolean selected) {
        if (!isEnabled()) return;
        var record = record(phase, 0);
        text(record, "query", query);
        record.addChild("matches", new JsonValue(matches));
        record.addChild("searchBarSelected", new JsonValue(selected));
        write(record);
    }

    public static void failure(String phase, long id, Throwable failure) {
        if (!isEnabled()) return;
        var record = record(phase, id);
        record.addChild("exception", new JsonValue(failure.getClass().getName()));
        text(record, "message", failure.getMessage());
        write(record);
    }

    private static JsonValue record(String phase, long id) {
        var record = new JsonValue(JsonValue.ValueType.object);
        record.addChild("phase", new JsonValue(phase));
        record.addChild("session", new JsonValue(id));
        return record;
    }

    private static void flags(JsonValue record, TextInputSession session) {
        record.addChild("usesIme", new JsonValue(session.usesIme()));
        record.addChild("composing", new JsonValue(session.isComposing()));
        record.addChild("finishing", new JsonValue(session.isFinishing()));
        record.addChild("finished", new JsonValue(session.isFinished()));
    }

    private static void text(JsonValue record, String name, String value) {
        int end = value != null ? Math.min(value.length(), TEXT_LIMIT) : 0;
        if (value != null && end < value.length() && end > 0
                && Character.isSurrogatePair(value.charAt(end - 1), value.charAt(end))) end--;
        record.addChild(name, new JsonValue(value != null ? value.substring(0, end) : null));
        if (value != null) {
            record.addChild(name + "Length", new JsonValue(value.length()));
            record.addChild(name + "Truncated", new JsonValue(end < value.length()));
        }
    }

    private static void write(JsonValue record) {
        Logger.getGlobal().info("Text input diagnostics " + record.toJson(JsonWriter.OutputType.json));
    }
}
