package bms.player.beatoraja.skin.lr2;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import com.badlogic.gdx.math.Rectangle;

import bms.player.beatoraja.result.MusicResultSkin;
import bms.player.beatoraja.result.SkinGaugeGraphObject;
import bms.player.beatoraja.skin.SkinBPMGraph;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinNoteDistributionGraph;

/**
 * LR2リザルトスキン読み込み用クラス
 * 
 * @author exch
 */
public class LR2ResultSkinLoader extends LR2SkinCSVLoader<MusicResultSkin> {

	private Rectangle gauge = new Rectangle();
	private SkinGaugeGraphObject gaugeobj;
	private SkinNoteDistributionGraph noteobj;
	private SkinBPMGraph bpmgraphobj;

	public LR2ResultSkinLoader(final Resolution src, final Config c) {
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
						values[15], values[16], values[17], values[18], values[19], values[20], values[21]);
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
						values[16], values[17], values[18], values[19], values[20], values[21]);
			}
		});

		addCommandWord(new CommandWord("SRC_BPMCHART") {
			//#SRC_BPMCHART, field_w, field_h, delay, lineWidth, mainBPMColor, minBPMColor, maxBPMColor, otherBPMColor, stopLineColor, transitionLineColor
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				bpmgraphobj = new SkinBPMGraph(values[3], values[4], str[5], str[6], str[7], str[8], str[9], str[10]);
				gauge = new Rectangle(0, 0, values[1], values[2]);
				skin.add(bpmgraphobj);
			}
		});
		addCommandWord(new CommandWord("DST_BPMCHART") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				gauge.x = values[3];
				gauge.y = src.height - values[4];
				skin.setDestination(bpmgraphobj, values[2], gauge.x, gauge.y, gauge.width, gauge.height, values[7], values[8],
						values[9], values[10], values[11], values[12], values[13], values[14], values[15],
						values[16], values[17], values[18], values[19], values[20], values[21]);
			}
		});
	}

	public MusicResultSkin loadSkin(File f, MainState state, SkinHeader header, Map<Integer, Boolean> option,
			Map property) throws IOException {
		return this.loadSkin(new MusicResultSkin(src, dst), f, state, header, option, property);
	}

}
