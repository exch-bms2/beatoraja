package bms.player.beatoraja.skin.lr2;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.result.CourseResultSkin;
import com.badlogic.gdx.math.Rectangle;

import bms.player.beatoraja.result.SkinGaugeGraphObject;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinNoteDistributionGraph;

/**
 * LR2リザルトスキン読み込み用クラス
 *
 * @author exch
 */
public class LR2CourseResultSkinLoader extends LR2SkinCSVLoader<CourseResultSkin> {

    private Rectangle gauge = new Rectangle();
    private SkinGaugeGraphObject gaugeobj;
    private SkinNoteDistributionGraph noteobj;

    public LR2CourseResultSkinLoader(final Resolution src, final Config c) {
        super(src, c);

        addCommandWord(new CommandWord("STARTINPUT") {
            @Override
            public void execute(String[] str) {
                skin.setInput(Integer.parseInt(str[1]));
                skin.setRankTime(Integer.parseInt(str[2]));
            }
        });
        addCommandWord(new CommandWord("SRC_GAUGECHART_1P") {
            @Override
            public void execute(String[] str) {
                int[] values = parseInt(str);
                gaugeobj = new SkinGaugeGraphObject();
                gaugeobj.setLineWidth(values[6]);
                gaugeobj.setDelay(values[14] - values[13]);
                gauge = new Rectangle(0, 0, values[11], values[12]);
                skin.add(gaugeobj);
            }
        });
        addCommandWord(new CommandWord("DST_GAUGECHART_1P") {
            @Override
            public void execute(String[] str) {
                int[] values = parseInt(str);
                gauge.x = values[3];
                gauge.y = src.height - values[4];
                skin.setDestination(gaugeobj, values[2], gauge.x, gauge.y, gauge.width, gauge.height, values[7],
                        values[8], values[9], values[10], values[11], values[12], values[13], values[14],
                        values[15], values[16], values[17], values[18], values[19], values[20], readOffset(str, 21));
            }
        });
        addCommandWord(new CommandWord("SRC_NOTECHART_1P") {
            //#SRC_NOTECHART_1P,(index),(gr),(x),(y),(w),(h),(div_x),(div_y),(cycle),(timer),field_w,field_h,(start),(end),delay,backTexOff,orderReverse,noGap
            @Override
            public void execute(String[] str) {
                int[] values = parseInt(str);
                noteobj = new SkinNoteDistributionGraph(values[1], values[15], values[16], values[17], values[18]);
                gauge = new Rectangle(0, 0, values[11], values[12]);
                skin.add(noteobj);
            }
        });
        addCommandWord(new CommandWord("DST_NOTECHART_1P") {
            @Override
            public void execute(String[] str) {
                int[] values = parseInt(str);
                gauge.x = values[3];
                gauge.y = src.height - values[4];
                skin.setDestination(noteobj, values[2], gauge.x, gauge.y, gauge.width, gauge.height, values[7], values[8],
                        values[9], values[10], values[11], values[12], values[13], values[14], values[15],
                        values[16], values[17], values[18], values[19], values[20], readOffset(str, 21));
            }
        });
    }

    public CourseResultSkin loadSkin(File f, MainState state, SkinHeader header, Map<Integer, Boolean> option,
                                     Map property) throws IOException {
        return this.loadSkin(new CourseResultSkin(src, dst), f, state, header, option, property);
    }

}
