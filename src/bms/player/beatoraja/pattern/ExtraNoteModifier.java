package bms.player.beatoraja.pattern;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Note;
import bms.model.TimeLine;

/**
 * BGレーンからノーツを追加する譜面オプション
 *
 * @author exch
 */
public class ExtraNoteModifier extends PatternModifier {

    // TODO noteType未実装

    /**
     * 追加するノーツ種類
     */
    private int noteType;
    /**
     * 同一タイムラインへの最大ノーツ追加数
     */
    private final int depth;
    /**
     * スクラッチレーンを対象にするかどうか
     */
    private boolean scratch;

    public ExtraNoteModifier(int noteType, int depth, boolean scratch) {
        this.noteType = noteType;
        this.depth = depth;
        this.scratch = scratch;
    }

    @Override
    public void modify(BMSModel model) {
        AssistLevel assist = AssistLevel.NONE;
        TimeLine[] tls = model.getAllTimeLines();
        boolean[] lns = new boolean[model.getMode().key];
        boolean[] blank = new boolean[model.getMode().key];
        Note[] lastnote = new Note[model.getMode().key];

        int lastoffset = 0;

        for (int i = 0;i < tls.length;i++) {
            final TimeLine tl = tls[i];

            for(int key = 0;key < model.getMode().key;key++) {
                final Note note = tl.getNote(key);
                if(note instanceof LongNote ln) {
                    lns[key] = !ln.isEnd();
                }
                blank[key] = !lns[key] && note == null && (scratch || !model.getMode().isScratchKey(key));
            }

            for(int d = 0; d < depth;d++) {
                if(tl.getBackGroundNotes().length > 0) {
                    final Note note = tl.getBackGroundNotes()[0];

                    int offset = lastoffset;
                    for(int j = 1;j < model.getMode().key;j++, offset = (offset + 1) % model.getMode().key) {
                        if(lastnote[offset] != null && lastnote[offset].getWav() == note.getWav()) {
                            break;
                        }
                    }
                    lastoffset = offset;

                    for(int j = 0, key = (offset % model.getMode().key);j < model.getMode().key;j++, key = (key + 1) % model.getMode().key) {
                        if(blank[key]) {
                            lastnote[key] = note;
                            tl.setNote(key, note);
                            tl.removeBackGroundNote(note);
                            assist = AssistLevel.ASSIST;
                            break;
                        }
                    }
                }
            }
        }

        setAssistLevel(assist);
    }
}
