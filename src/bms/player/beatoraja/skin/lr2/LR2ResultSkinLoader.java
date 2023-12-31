package bms.player.beatoraja.skin.lr2;

import java.io.*;
import java.util.function.BiConsumer;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.*;

import bms.player.beatoraja.*;
import bms.player.beatoraja.result.*;
import bms.player.beatoraja.skin.*;

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

	public MusicResultSkin loadSkin(MainState state, SkinHeader header, IntIntMap option) throws IOException {
		return this.loadSkin(new MusicResultSkin(header), state, option);
	}

}

enum ResultCommand implements LR2SkinLoader.Command<LR2ResultSkinLoader> {
	
	STARTINPUT ((loader, str) -> {
		loader.skin.setInput(Integer.parseInt(str[1]));
		loader.skin.setRankTime(Integer.parseInt(str[2]));
	}),
	SRC_GAUGECHART_1P ((loader, str) -> {
		int[] values = loader.parseInt(str);
		loader.gaugeobj = new SkinGaugeGraphObject();
		loader.gaugeobj.setLineWidth(values[6]);
		loader.gaugeobj.setDelay(values[14] - values[13]);
		loader.gauge = new Rectangle(0, 0, values[11], values[12]);
		loader.skin.add(loader.gaugeobj);
	}),
	DST_GAUGECHART_1P ((loader, str) -> {
		int[] values = loader.parseInt(str);
		loader.gauge.x = values[3];
		loader.gauge.y = loader.src.height - values[4];
		loader.skin.setDestination(loader.gaugeobj, values[2], loader.gauge.x, loader.gauge.y, loader.gauge.width, loader.gauge.height, values[7],
				values[8], values[9], values[10], values[11], values[12], values[13], values[14],
				values[15], values[16], values[17], values[18], values[19], values[20], loader.readOffset(str, 21));
	}),
	SRC_NOTECHART_1P ((loader, str) -> {
		//#SRC_NOTECHART_1P,(index),(gr),(x),(y),(w),(h),(div_x),(div_y),(cycle),(timer),field_w,field_h,(start),(end),delay,backTexOff,orderReverse,noGap
		int[] values = loader.parseInt(str);
		loader.noteobj = new SkinNoteDistributionGraph(values[1], values[15], values[16], values[17], values[18], values[19]);
		loader.gauge = new Rectangle(0, 0, values[11], values[12]);
		loader.skin.add(loader.noteobj);
	}),
	DST_NOTECHART_1P ((loader, str) -> {
		int[] values = loader.parseInt(str);
		loader.gauge.x = values[3];
		loader.gauge.y = loader.src.height - values[4];
		loader.skin.setDestination(loader.noteobj, values[2], loader.gauge.x, loader.gauge.y, loader.gauge.width, loader.gauge.height, values[7],
				values[8], values[9], values[10], values[11], values[12], values[13], values[14],
				values[15], values[16], values[17], values[18], values[19], values[20], loader.readOffset(str, 21));
	}),
	SRC_BPMCHART ((loader, str) -> {
		//#SRC_BPMCHART, field_w, field_h, delay, lineWidth, mainBPMColor, minBPMColor, maxBPMColor, otherBPMColor, stopLineColor, transitionLineColor
		int[] values = loader.parseInt(str);
		loader.bpmgraphobj = new SkinBPMGraph(values[3], values[4], str[5], str[6], str[7], str[8], str[9], str[10]);
		loader.gauge = new Rectangle(0, 0, values[1], values[2]);
		loader.skin.add(loader.bpmgraphobj);
	}),
	DST_BPMCHART ((loader, str) -> {
		int[] values = loader.parseInt(str);
		loader.gauge.x = values[3];
		loader.gauge.y = loader.src.height - values[4];
		loader.skin.setDestination(loader.bpmgraphobj, values[2], loader.gauge.x, loader.gauge.y, loader.gauge.width, loader.gauge.height, values[7],
				values[8], values[9], values[10], values[11], values[12], values[13], values[14],
				values[15], values[16], values[17], values[18], values[19], values[20], loader.readOffset(str, 21));
	}),
	SRC_TIMINGCHART_1P ((loader, str) -> {
		//#SRC_TIMINGCHART_1P,(index),(gr),(x),width,height,lineWidth,graphColor,averageColor,devColor,PGColor,GRColor,GDColor,BDColor,PRColor,drawAverage,drawDev
		int[] values = loader.parseInt(str);
		loader.timinggraphobj = new SkinTimingDistributionGraph(values[4], values[6], str[7], str[8], str[9], str[10], str[11], str[12], str[13], str[14], values[15], values[16]);
		loader.gauge = new Rectangle(0, 0, values[4], values[5]);
		loader.skin.add(loader.timinggraphobj);
	}),
	DST_TIMINGCHART_1P ((loader, str) -> {
		int[] values = loader.parseInt(str);
		loader.gauge.x = values[3];
		loader.gauge.y = loader.src.height - values[4];
		loader.skin.setDestination(loader.timinggraphobj, values[2], loader.gauge.x, loader.gauge.y, loader.gauge.width, loader.gauge.height, values[7],
				values[8], values[9], values[10], values[11], values[12], values[13], values[14],
				values[15], values[16], values[17], values[18], values[19], values[20], loader.readOffset(str, 21));
	});

	public final BiConsumer<LR2ResultSkinLoader, String[]> function;
	
	private ResultCommand(BiConsumer<LR2ResultSkinLoader, String[]> function) {
		this.function = function;
	}
	
	public void execute(LR2ResultSkinLoader loader, String[] str) {
		function.accept(loader, str);
	}
}
