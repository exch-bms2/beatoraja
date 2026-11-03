package bms.player.beatoraja.skin;

import com.badlogic.gdx.utils.Clipboard;

public final class SkinTextClipboardTest {
    public static void main(String[] args) {
        var memory = new Clipboard() {
            String value = "\u65e5\u672c\u8a9e";
            @Override public boolean hasContents() { return !value.isEmpty(); }
            @Override public String getContents() { return value; }
            @Override public void setContents(String text) { value = text; }
        };
        var clipboard = new SkinTextClipboard(() -> memory);
        check(clipboard.hasContents() && clipboard.getContents().equals("\u65e5\u672c\u8a9e"), "Unicode read");
        clipboard.setContents("\ud842\udfb7"); check(memory.getContents().equals("\ud842\udfb7"), "Lossless UTF-16 write");
        var failed = new SkinTextClipboard(() -> { throw new IllegalStateException("Unavailable"); });
        check(!failed.hasContents() && failed.getContents() == null, "Failed paste must be null, not an empty replacement");
        try { failed.setContents("keep"); throw new AssertionError("Copy failure must abort cut"); }
        catch (SkinTextClipboard.WriteFailure expected) { }
        var missing = new SkinTextClipboard(() -> null);
        check(missing.getContents() == null, "Unavailable backend is safe");
        System.out.println("SkinTextClipboardTest passed (Unicode, failed paste/cut, unavailable backend)");
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
