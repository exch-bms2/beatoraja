package bms.player.beatoraja.input;

/** Explicit completion/command ordering; does not use any OS IME or clipboard. */
public final class MockImeBridge implements ImeBridge {
    public long id;
    public Sink sink;
    public int begins, finishes, closes, confirms, cancels;
    public boolean failFinish;
    public TextInputSession.EditorState editor;
    public boolean isAvailable() { return true; }
    public void begin(long id, TextInputSession.EditorState state, Sink sink) {
        this.id = id; this.sink = sink; editor = state; begins++;
    }
    public void update(long id, TextInputSession.EditorState state) { editor = state; }
    public void confirmComposition(long id) { confirms++; }
    public void cancelComposition(long id) { cancels++; }
    public void finish(long id) { finishes++; if (failFinish) throw new IllegalStateException("Injected finish failure"); }
    public void close(long id) { closes++; }
    public void preedit(String text) { sink.preedit(id, text, text.length()); }
    public void commit(String text) { sink.committed(id, new TextInputSession.EditorState(text, text.length(), text.length())); }
    public void command(Command command, boolean consumed) { sink.command(id, command, consumed); }
    public void done() { sink.finished(id); }
}
