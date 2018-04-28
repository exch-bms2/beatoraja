package bms.player.beatoraja.skin.lr2;

import java.io.File;
import java.io.IOException;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.ObjectMap;

import bms.model.Mode;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.play.SkinGauge;
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
	int groovex = 0;
	int groovey = 0;
	SkinGauge gauger = null;
	Mode mode;

	public LR2ResultSkinLoader(final Resolution src, final Config c) {
		super(src, c);
		addCommandWord(ResultCommand.values());
	}

	public MusicResultSkin loadSkin(File f, MainState state, SkinHeader header, IntIntMap option,
			ObjectMap property) throws IOException {
		mode = header.getSkinType().getMode();
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
	},
	SRC_GROOVEGAUGE {
		//SRC定義,index,gr,x,y,w,h,div_x,div_y,cycle,timer,add_x,add_y,parts,animation_type,animation_range,animation_cycle,starttime,endtime
		@Override
		public void execute(LR2ResultSkinLoader loader, String[] str) {
			loader.gauger = null;
			int[] values = loader.parseInt(str);
			if (values[2] < loader.imagelist.size && loader.imagelist.get(values[2]) != null) {
				int playside = values[1];
				int divx = values[7];
				if (divx <= 0) {
					divx = 1;
				}
				int divy = values[8];
				if (divy <= 0) {
					divy = 1;
				}
				TextureRegion[][] gauge;
				if(values[14] == 3 && divx * divy % 6 == 0) {
					//アニメーションタイプがPMS用明滅アニメーションの場合 表赤、表緑、裏赤、裏緑、発光表赤、発光表緑の順にsrc分割
					gauge = new TextureRegion[(divx * divy) / 6][12];
					final int w = values[5];
					final int h = values[6];
					for (int x = 0; x < divx; x++) {
						for (int y = 0; y < divy; y++) {
							if ((y * divx + x) / 6 < gauge.length) {
								if((y * divx + x) % 6 < 4) {
									gauge[(y * divx + x) / 6][(y * divx + x) % 6] = new TextureRegion(
											(Texture) loader.imagelist.get(values[2]), values[3] + w * x / divx,
											values[4] + h * y / divy, w / divx, h / divy);
									gauge[(y * divx + x) / 6][(y * divx + x) % 6 + 4] = new TextureRegion(
											(Texture) loader.imagelist.get(values[2]), values[3] + w * x / divx,
											values[4] + h * y / divy, w / divx, h / divy);
								} else {
									gauge[(y * divx + x) / 6][(y * divx + x) % 6 + 4] = new TextureRegion(
											(Texture) loader.imagelist.get(values[2]), values[3] + w * x / divx,
											values[4] + h * y / divy, w / divx, h / divy);
									gauge[(y * divx + x) / 6][(y * divx + x) % 6 + 6] = new TextureRegion(
											(Texture) loader.imagelist.get(values[2]), values[3] + w * x / divx,
											values[4] + h * y / divy, w / divx, h / divy);
								}
							}
						}
					}
				} else {
					gauge = new TextureRegion[(divx * divy) / 4][8];
					final int w = values[5];
					final int h = values[6];
					for (int x = 0; x < divx; x++) {
						for (int y = 0; y < divy; y++) {
							if ((y * divx + x) / 4 < gauge.length) {
								gauge[(y * divx + x) / 4][(y * divx + x) % 4] = new TextureRegion(
										(Texture) loader.imagelist.get(values[2]), values[3] + w * x / divx,
										values[4] + h * y / divy, w / divx, h / divy);
								gauge[(y * divx + x) / 4][(y * divx + x) % 4 + 4] = new TextureRegion(
										(Texture) loader.imagelist.get(values[2]), values[3] + w * x / divx,
										values[4] + h * y / divy, w / divx, h / divy);
							}
						}
					}
				}
				loader.groovex = values[11];
				loader.groovey = values[12];
				if (loader.gauger == null) {
					if(values[13] == 0) {
						loader.gauger = new SkinGauge(gauge, values[10], values[9], loader.mode == Mode.POPN_9K ? 24 : 50, 0, loader.mode == Mode.POPN_9K ? 0 : 3, 33);
					} else {
						loader.gauger = new SkinGauge(gauge, values[10], values[9], values[13], values[14], values[15], values[16]);
					}

					loader.skin.add(loader.gauger);

					loader.gauger.setStarttime(values[17]);
					loader.gauger.setEndtime(values[18]);
				}
			}
		}
	},
	SRC_GROOVEGAUGE_EX {
		//JSONスキンと同形式版 表赤、表緑、裏赤、裏緑、EX表赤、EX表緑、EX裏赤、EX裏緑の順にsrc分割
		//SRC定義,index,gr,x,y,w,h,div_x,div_y,cycle,timer,add_x,add_y,parts,animation_type,animation_range,animation_cycle,starttime,endtime
		@Override
		public void execute(LR2ResultSkinLoader loader, String[] str) {
			loader.gauger = null;
			int[] values = loader.parseInt(str);
			if (values[2] < loader.imagelist.size && loader.imagelist.get(values[2]) != null) {
				int playside = values[1];
				int divx = values[7];
				if (divx <= 0) {
					divx = 1;
				}
				int divy = values[8];
				if (divy <= 0) {
					divy = 1;
				}
				TextureRegion[][] gauge;
				if(values[14] == 3 && divx * divy % 12 == 0) {
					//アニメーションタイプがPMS用明滅アニメーションの場合 表赤、表緑、裏赤、裏緑、EX表赤、EX表緑、EX裏赤、EX裏緑、発光表赤、発光表緑、発光EX表赤、発光EX表緑の順にsrc分割
					gauge = new TextureRegion[(divx * divy) / 12][12];
					final int w = values[5];
					final int h = values[6];
					for (int x = 0; x < divx; x++) {
						for (int y = 0; y < divy; y++) {
							if ((y * divx + x) / 12 < gauge.length) {
									gauge[(y * divx + x) / 12][(y * divx + x) % 12] = new TextureRegion(
											(Texture) loader.imagelist.get(values[2]), values[3] + w * x / divx,
											values[4] + h * y / divy, w / divx, h / divy);
							}
						}
					}
				} else {
					gauge = new TextureRegion[(divx * divy) / 8][8];
					final int w = values[5];
					final int h = values[6];
					for (int x = 0; x < divx; x++) {
						for (int y = 0; y < divy; y++) {
							if ((y * divx + x) / 8 < gauge.length) {
								gauge[(y * divx + x) / 8][(y * divx + x) % 8] = new TextureRegion(
										(Texture) loader.imagelist.get(values[2]), values[3] + w * x / divx,
										values[4] + h * y / divy, w / divx, h / divy);
							}
						}
					}
				}
				loader.groovex = values[11];
				loader.groovey = values[12];
				if (loader.gauger == null) {
					if(values[13] == 0) {
						loader.gauger = new SkinGauge(gauge, values[10], values[9], loader.mode == Mode.POPN_9K ? 24 : 50, 0, loader.mode == Mode.POPN_9K ? 0 : 3, 33);
					} else {
						loader.gauger = new SkinGauge(gauge, values[10], values[9], values[13], values[14], values[15], values[16]);
					}

					loader.skin.add(loader.gauger);

					loader.gauger.setStarttime(values[17]);
					loader.gauger.setEndtime(values[18]);
				}
			}
		}
	},
	DST_GROOVEGAUGE {
		@Override
		public void execute(LR2ResultSkinLoader loader, String[] str) {
			if (loader.gauger != null) {
				float width = (Math.abs(loader.groovex) >= 1) ? (loader.groovex * 50 * loader.dst.width / loader.src.width)
						: (Integer.parseInt(str[5]) * loader.dst.width / loader.src.width);
				float height = (Math.abs(loader.groovey) >= 1) ? (loader.groovey * 50 * loader.dst.height / loader.src.height)
						: (Integer.parseInt(str[6]) * loader.dst.height / loader.src.height);
				float x = Integer.parseInt(str[3]) * loader.dst.width / loader.src.width - (loader.groovex < 0 ? loader.groovex * loader.dst.width / loader.src.width : 0);
				float y = loader.dst.height - Integer.parseInt(str[4]) * loader.dst.height / loader.src.height - height;
				int[] values = loader.parseInt(str);
				loader.gauger.setDestination(values[2], x, y, width, height, values[7],
						values[8], values[9], values[10], values[11], values[12], values[13], values[14],
						values[15], values[16], values[17], values[18], values[19], values[20], loader.readOffset(str, 21));
			}
		}
	}


}
