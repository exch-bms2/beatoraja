package bms.player.beatoraja.skin.lr2;

import java.io.File;
import java.io.IOException;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.ObjectMap;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.result.MusicResultSkin;
import bms.player.beatoraja.result.SkinGaugeGraphObject;
import bms.player.beatoraja.skin.SkinBPMGraph;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinNoteDistributionGraph;
import bms.player.beatoraja.skin.SkinTimingDistributionGraph;

/**
 * LR2リザルトスキン読み込み用クラス
 *
 * @author exch
 */
public class LR2ResultSkinLoader extends LR2SkinCSVLoader<MusicResultSkin> {

	Rectangle gauge = new Rectangle();
	SkinGaugeGraphObject gaugeobj;
	SkinNoteDistributionGraph noteobj;
	SkinBPMGraph bpmgraphobj;
	SkinTimingDistributionGraph timinggraphobj;

	public LR2ResultSkinLoader(final Resolution src, final Config c) {
		super(src, c);
		addCommandWord(ResultCommand.values());
	}

	public MusicResultSkin loadSkin(File f, MainState state, SkinHeader header, IntIntMap option,
			ObjectMap property) throws IOException {
		return this.loadSkin(new MusicResultSkin(src, dst), f, state, header, option, property);
	}

}

enum ResultCommand implements LR2SkinLoader.Command<LR2ResultSkinLoader> {
	STARTINPUT {
		@Override
		public void execute(LR2ResultSkinLoader loader, String[] str) {
			loader.skin.setInput(Integer.parseInt(str[1]));
			loader.skin.setRankTime(Integer.parseInt(str[2]));
		}
	},
	SRC_GAUGECHART_1P {
		@Override
		public void execute(LR2ResultSkinLoader loader, String[] str) {
			int[] values = loader.parseInt(str);
			loader.gaugeobj = new SkinGaugeGraphObject();
			loader.gaugeobj.setLineWidth(values[6]);
			loader.gaugeobj.setDelay(values[14] - values[13]);
			loader.gauge = new Rectangle(0, 0, values[11], values[12]);
			loader.skin.add(loader.gaugeobj);
		}
	},
	DST_GAUGECHART_1P {
		public void execute(LR2ResultSkinLoader loader, String[] str) {
			int[] values = loader.parseInt(str);
			loader.gauge.x = values[3];
			loader.gauge.y = loader.src.height - values[4];
			loader.skin.setDestination(loader.gaugeobj, values[2], loader.gauge.x, loader.gauge.y, loader.gauge.width, loader.gauge.height, values[7],
					values[8], values[9], values[10], values[11], values[12], values[13], values[14],
					values[15], values[16], values[17], values[18], values[19], values[20], loader.readOffset(str, 21));

		}
	},
	SRC_NOTECHART_1P {
		//#SRC_NOTECHART_1P,(index),(gr),(x),(y),(w),(h),(div_x),(div_y),(cycle),(timer),field_w,field_h,(start),(end),delay,backTexOff,orderReverse,noGap

		public void execute(LR2ResultSkinLoader loader, String[] str) {
			int[] values = loader.parseInt(str);
			loader.noteobj = new SkinNoteDistributionGraph(values[1], values[15], values[16], values[17], values[18]);
			loader.gauge = new Rectangle(0, 0, values[11], values[12]);
			loader.skin.add(loader.noteobj);

		}
	},
	DST_NOTECHART_1P {
		public void execute(LR2ResultSkinLoader loader, String[] str) {
			int[] values = loader.parseInt(str);
			loader.gauge.x = values[3];
			loader.gauge.y = loader.src.height - values[4];
			loader.skin.setDestination(loader.noteobj, values[2], loader.gauge.x, loader.gauge.y, loader.gauge.width, loader.gauge.height, values[7],
					values[8], values[9], values[10], values[11], values[12], values[13], values[14],
					values[15], values[16], values[17], values[18], values[19], values[20], loader.readOffset(str, 21));

		}
	},
	SRC_BPMCHART {
		//#SRC_BPMCHART, field_w, field_h, delay, lineWidth, mainBPMColor, minBPMColor, maxBPMColor, otherBPMColor, stopLineColor, transitionLineColor

		public void execute(LR2ResultSkinLoader loader, String[] str) {
			int[] values = loader.parseInt(str);
			loader.bpmgraphobj = new SkinBPMGraph(values[3], values[4], str[5], str[6], str[7], str[8], str[9], str[10]);
			loader.gauge = new Rectangle(0, 0, values[1], values[2]);
			loader.skin.add(loader.bpmgraphobj);
		}
	},
	DST_BPMCHART {
		public void execute(LR2ResultSkinLoader loader, String[] str) {
			int[] values = loader.parseInt(str);
			loader.gauge.x = values[3];
			loader.gauge.y = loader.src.height - values[4];
			loader.skin.setDestination(loader.bpmgraphobj, values[2], loader.gauge.x, loader.gauge.y, loader.gauge.width, loader.gauge.height, values[7],
					values[8], values[9], values[10], values[11], values[12], values[13], values[14],
					values[15], values[16], values[17], values[18], values[19], values[20], loader.readOffset(str, 21));
		}
	},
	SRC_TIMINGCHART_1P {
		//#SRC_TIMINGCHART_1P,(index),(gr),(x),(y),(w),(h),(div_x),(div_y),(cycle),(timer),field_w,field_h,(start),(end),drawAverage
		@Override
		public void execute(LR2ResultSkinLoader loader, String[] str) {
			int[] values = loader.parseInt(str);
			loader.timinggraphobj = new SkinTimingDistributionGraph(values[15]);
			loader.gauge = new Rectangle(0, 0, values[11], values[12]);
			loader.skin.add(loader.timinggraphobj);
		}
	},
	DST_TIMINGCHART_1P {
		@Override
		public void execute(LR2ResultSkinLoader loader, String[] str) {
			int[] values = loader.parseInt(str);
			loader.gauge.x = values[3];
			loader.gauge.y = loader.src.height - values[4];
			loader.skin.setDestination(loader.timinggraphobj, values[2], loader.gauge.x, loader.gauge.y, loader.gauge.width, loader.gauge.height, values[7],
					values[8], values[9], values[10], values[11], values[12], values[13], values[14],
					values[15], values[16], values[17], values[18], values[19], values[20], loader.readOffset(str, 21));

		}
	}


}
