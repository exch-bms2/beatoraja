package bms.player.beatoraja.skin;

import java.util.function.Supplier;
import java.util.logging.Logger;
import com.badlogic.gdx.utils.Clipboard;

/** Keep failed paste non-destructive; failed copy must abort TextField's subsequent cut. */
final class SkinTextClipboard implements Clipboard {
    private final Supplier<Clipboard> clipboard;
    SkinTextClipboard(Supplier<Clipboard> clipboard) { this.clipboard = clipboard; }

    @Override public boolean hasContents() {
        try { return clipboard.get().hasContents(); }
        catch (RuntimeException | LinkageError e) { warn(e); return false; }
    }
    @Override public String getContents() {
        try { return clipboard.get().getContents(); }
        catch (RuntimeException | LinkageError e) { warn(e); return null; }
    }
    @Override public void setContents(String value) {
        try { clipboard.get().setContents(value); }
        catch (RuntimeException | LinkageError e) { warn(e); throw new WriteFailure(e); }
    }
    private static void warn(Throwable e) {
        Logger.getGlobal().warning("Skin text clipboard unavailable: " + e.getClass().getSimpleName());
    }
    static final class WriteFailure extends RuntimeException {
        WriteFailure(Throwable cause) { super(cause); }
    }
}
