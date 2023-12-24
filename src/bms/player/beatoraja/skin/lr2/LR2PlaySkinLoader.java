package bms.player.beatoraja.skin.lr2;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.io.*;
import java.util.Arrays;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.*;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinObject.SkinObjectDestination;

/**
 * LR2プレイスキンローダー
 *
 * @author exch
 */
public class LR2PlaySkinLoader extends LR2SkinCSVLoader<PlaySkin> {

	private SkinBGA bga;

	private SkinNote lanerender;

	private Rectangle[] playerr;

	private Mode mode;

	private SkinSource[] note = new SkinSource[8];
	private SkinSource[] lnstart = new SkinSource[8];
	private SkinSource[] lnend = new SkinSource[8];
	private SkinSource[] lnbody = new SkinSource[8];
	private SkinSource[] lnbodya = new SkinSource[8];
	private SkinSource[] hcnstart = new SkinSource[8];
	private SkinSource[] hcnend = new SkinSource[8];
	private SkinSource[] hcnbody = new SkinSource[8];
	private SkinSource[] hcnbodya = new SkinSource[8];
	private SkinSource[] hcnbodyd = new SkinSource[8];
	private SkinSource[] hcnbodyr = new SkinSource[8];
	private SkinSource[] mine = new SkinSource[8];
	private Rectangle[] laner = new Rectangle[8];
	private float[] scale = new float[8];
	private int[] dstnote2 = new int[8];
	private SkinImage line;
	private SkinImage[] lines = new SkinImage[8];
	private String[][] linevalues = new String[2][];

	private SkinJudge[] judge = new SkinJudge[3];

	private SkinType type;

	final float srcw;
	final float srch;
	final float dstw;
	final float dsth;


	private Rectangle gauge = new Rectangle();
	private SkinNoteDistributionGraph noteobj;
	private SkinBPMGraph bpmgraphobj;
	private SkinTimingVisualizer timingobj;
	private SkinHidden hidden;

	SkinSlider laneCover;

	public LR2PlaySkinLoader(final SkinType type, final Resolution src, final Config c) {
		super(src, c);

		this.type = type;
		srcw = src.width;
		srch = src.height;
		dstw = dst.width;
		dsth = dst.height;
		
		addCommandWord(PlayCommand.values());

		addCommandWord(new CommandWord("FINISHMARGIN") {
			@Override
			//STATE_FINISHEDからフェードアウトを開始するまでのマージン(ms)
			public void execute(String[] str) {
				skin.setFinishMargin(Integer.parseInt(str[1]));
			}
		});
		addCommandWord(new CommandWord("JUDGETIMER") {
			@Override
			public void execute(String[] str) {
				skin.setJudgetimer(Integer.parseInt(str[1]));
			}
		});
		addCommandWord(new CommandWord("SRC_BGA") {
			@Override
			public void execute(String[] str) {
				bga = new SkinBGA(c.getBgaExpand());
				skin.add(bga);
			}
		});
		addCommandWord(new CommandWord("DST_BGA") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if (bga != null) {
					skin.setDestination(bga, 0, values[3], srch - values[4] - values[6], values[5], values[6],
							values[7], values[8], values[9], values[10], values[11], values[12], values[13], values[14],
							values[15], values[16], values[17], values[18], values[19], values[20], readOffset(str, 21));
					bga.setStretch(stretch);
				}
			}
		});
		addCommandWord(new CommandWord("SRC_LINE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					SkinImage li = new SkinImage(images, values[10], values[9]);
					lines[values[1]] = li;
					// System.out.println("Object Added - " +
					// (part.getTiming()));
				}
			}
		});
		addCommandWord(new CommandWord("DST_LINE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if (lines[values[1]] != null) {
					if (values[5] < 0) {
						values[3] += values[5];
						values[5] = -values[5];
					}
					if (values[6] < 0) {
						values[4] += values[6];
						values[6] = -values[6];
					}
					lines[values[1]].setDestination(values[2], values[3] * dstw / srcw, dsth - (values[4] + values[6]) * dsth / srch,
							values[5] * dstw / srcw, values[6] * dsth / srch, values[7], values[8], values[9],
							values[10], values[11], values[12], values[13], values[14], values[15], values[16],
							values[17], values[18], values[19], values[20], readOffset(str, 21, new int[]{OFFSET_LIFT}));
					if(playerr[values[1] % 2] != null) {
						playerr[values[1] % 2] = new Rectangle(values[3] * dstw / srcw,
								dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
								(values[4] + values[6]) * dsth / srch);
					}
					linevalues[values[1] % 2] = str.clone();
				}
			}
		});
		addCommandWord(new CommandWord("SRC_NOTE") {
			@Override
			public void execute(String[] str) {
				addNote(str, note, true);
			}
		});
		addCommandWord(new CommandWord("SRC_LN_END") {
			@Override
			public void execute(String[] str) {
				addNote(str, lnend, true);
			}
		});
		addCommandWord(new CommandWord("SRC_LN_START") {
			@Override
			public void execute(String[] str) {
				addNote(str, lnstart, true);
			}
		});
		addCommandWord(new CommandWord("SRC_LN_BODY") {
			@Override
			public void execute(String[] str) {
				addNote(str, lnbody, false);
				addNote(str, lnbodya, true);
			}
		});
		addCommandWord(new CommandWord("SRC_LN_BODY_INACTIVE") {
			@Override
			public void execute(String[] str) {
				addNote(str, lnbody, true);
			}
		});
		addCommandWord(new CommandWord("SRC_LN_BODY_ACTIVE") {
			@Override
			public void execute(String[] str) {
				addNote(str, lnbodya, true);
			}
		});
		addCommandWord(new CommandWord("SRC_HCN_END") {
			@Override
			public void execute(String[] str) {
				addNote(str, hcnend, true);
			}
		});
		addCommandWord(new CommandWord("SRC_HCN_START") {
			@Override
			public void execute(String[] str) {
				addNote(str, hcnstart, true);
			}
		});
		addCommandWord(new CommandWord("SRC_HCN_BODY") {
			@Override
			public void execute(String[] str) {
				addNote(str, hcnbody, false);
				addNote(str, hcnbodya, true);
			}
		});
		addCommandWord(new CommandWord("SRC_HCN_BODY_INACTIVE") {
			@Override
			public void execute(String[] str) {
				addNote(str, hcnbody, true);
			}
		});
		addCommandWord(new CommandWord("SRC_HCN_BODY_ACTIVE") {
			@Override
			public void execute(String[] str) {
				addNote(str, hcnbodya, true);
			}
		});
		addCommandWord(new CommandWord("SRC_HCN_DAMAGE") {
			@Override
			public void execute(String[] str) {
				addNote(str, hcnbodyd, true);
			}
		});
		addCommandWord(new CommandWord("SRC_HCN_REACTIVE") {
			@Override
			public void execute(String[] str) {
				addNote(str, hcnbodyr, true);
			}
		});

		addCommandWord(new CommandWord("SRC_MINE") {
			@Override
			public void execute(String[] str) {
				addNote(str, mine, true);
			}
		});

		addCommandWord(new CommandWord("DST_NOTE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				int lane = Integer.parseInt(str[1]);
				if (lane % 10 == 0) {
					lane = mode.scratchKey.length > (lane / 10) ? mode.scratchKey[(lane / 10)] : -1;
				} else {
					final int offset = (lane / 10) * (laner.length / playerr.length);
					lane = (lane > 10) ? lane - 11 : lane - 1;
					if (lane >= (laner.length - mode.scratchKey.length) / playerr.length) {
						lane = -1;
					} else {
						lane += offset;
					}
				}
				if(lane < 0) {
					return;
				}
				if (laner[lane] == null) {
					laner[lane] = new Rectangle(values[3] * dstw / srcw, dsth - (values[4] + values[6]) * dsth / srch,
							values[5] * dstw / srcw, (values[4] + values[6]) * dsth / srch);
					scale[lane] = values[6] * dsth / srch;
				}
				if (lanerender == null) {
					if(hcnend[0] == null) {
						hcnend = lnend;
					}
					if(hcnstart[0] == null) {
						hcnstart = lnstart;
					}
					if(hcnbody[0] == null) {
						hcnbody = lnbody;
					}
					if(hcnbodya[0] == null) {
						hcnbodya = lnbodya;
					}
					if(hcnbodyd[0] == null) {
						hcnbodyd = hcnbody;
					}
					if(hcnbodyr[0] == null) {
						hcnbodyr = hcnbodya;
					}
					final SkinSource[][] lns = new SkinSource[][]{ lnend, lnstart, lnbodya, lnbody, hcnend,
							hcnstart, hcnbodya, hcnbody, hcnbodyr, hcnbodyd };
					final SkinSource[][] lnss = new SkinSource[lnstart.length][10];
					for(int i = 0;i < 10;i++) {
						for(int j = 0;j < lnstart.length;j++) {
							lnss[j][i] = lns[i][j];
						}
					}
					lanerender = new SkinNote(note, lnss, mine);
					lanerender.setOffsetID(readOffset(str, 21, new int[]{OFFSET_NOTES_1P}));
					skin.add(lanerender);
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOTE2") {
			@Override
			//PMSの見逃しPOOR時のノートが落ちる演出の消失点のy座標
			public void execute(String[] str) {
				Arrays.fill(dstnote2, (int) (dsth - (Integer.parseInt(str[1]) * dsth / srch + scale[0])));
			}
		});

		addCommandWord(new CommandWord("DST_NOTE_EXPANSION_RATE") {
			@Override
			//PMSのリズムに合わせたノートの拡大の最大拡大率(%) 「w,h」
			public void execute(String[] str) {
				skin.setNoteExpansionRate(new int[]{Integer.parseInt(str[1]),Integer.parseInt(str[2])});
			}
		});

		addCommandWord(new CommandWord("SRC_NOWJUDGE_1P") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					if (judge[0] == null) {
						judge[0] = new SkinJudge(0, (values[11] != 1));
						skin.add(judge[0]);
					}
					judge[0].getJudge()[values[1] <= 5  ? (5 - values[1]) : values[1]] = new SkinImage(images, values[10], values[9]);
					// System.out.println("Nowjudge Added - " + (5 -
					// values[1]));
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOWJUDGE_1P") {

			private boolean detail = false;

			@Override
			public void execute(String[] str) {
				if (judge[0] != null && judge[0].getJudge()[Integer.parseInt(str[1]) <= 5 ? (5 - Integer.parseInt(str[1])) : Integer.parseInt(str[1])] != null) {
					try {
						int[] values = parseInt(str);
						if (values[5] < 0) {
							values[3] += values[5];
							values[5] = -values[5];
						}
						if (values[6] < 0) {
							values[4] += values[6];
							values[6] = -values[6];
						}
						judge[0].getJudge()[values[1] <= 5  ? (5 - values[1]) : values[1]].setDestination(values[2], values[3] * dstw / srcw,
								dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
								values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
								values[12], values[13], values[14], values[15], values[16], values[17], values[18],
								values[19], values[20], readOffset(str, 21, new int[]{OFFSET_JUDGE_1P, OFFSET_LIFT}));

						if (!detail) {
							detail = true;
							addJudgeDetail(skin, values, srcw, dstw, srch, dsth, 0);
						}

					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOWJUDGE_2P") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					if (judge[1] == null) {
						judge[1] = new SkinJudge(1, (values[11] != 1));
						skin.add(judge[1]);
					}
					judge[1].getJudge()[values[1] <= 5  ? (5 - values[1]) : values[1]] = new SkinImage(images, values[10], values[9]);
					// System.out.println("Nowjudge Added - " + (5 -
					// values[1]));
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOWJUDGE_2P") {

			private boolean detail = false;

			@Override
			public void execute(String[] str) {
				if (judge[1] != null && judge[1].getJudge()[Integer.parseInt(str[1]) <= 5 ? (5 - Integer.parseInt(str[1])) : Integer.parseInt(str[1])] != null) {
					try {
						int[] values = parseInt(str);
						if (values[5] < 0) {
							values[3] += values[5];
							values[5] = -values[5];
						}
						if (values[6] < 0) {
							values[4] += values[6];
							values[6] = -values[6];
						}
						judge[1].getJudge()[values[1] <= 5  ? (5 - values[1]) : values[1]].setDestination(values[2], values[3] * dstw / srcw,
								dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
								values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
								values[12], values[13], values[14], values[15], values[16], values[17], values[18],
								values[19], values[20], readOffset(str, 21, new int[]{OFFSET_JUDGE_2P, OFFSET_LIFT}));

						if (!detail) {
							detail = true;
							addJudgeDetail(skin, values, srcw, dstw, srch, dsth, 1);
						}
					} catch(Throwable e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOWJUDGE_3P") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					if (judge[2] == null) {
						judge[2] = new SkinJudge(2, (values[11] != 1));
						skin.add(judge[2]);
					}
					judge[2].getJudge()[values[1] <= 5  ? (5 - values[1]) : values[1]] = new SkinImage(images, values[10], values[9]);
					// System.out.println("Nowjudge Added - " + (5 -
					// values[1]));
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOWJUDGE_3P") {

			private boolean detail = false;

			@Override
			public void execute(String[] str) {
				if (judge[2] != null && judge[2].getJudge()[Integer.parseInt(str[1]) <= 5 ? (5 - Integer.parseInt(str[1])) : Integer.parseInt(str[1])] != null) {
					try {
						int[] values = parseInt(str);
						if (values[5] < 0) {
							values[3] += values[5];
							values[5] = -values[5];
						}
						if (values[6] < 0) {
							values[4] += values[6];
							values[6] = -values[6];
						}
						judge[2].getJudge()[values[1] <= 5  ? (5 - values[1]) : values[1]].setDestination(values[2], values[3] * dstw / srcw,
								dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
								values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
								values[12], values[13], values[14], values[15], values[16], values[17], values[18],
								values[19], values[20], readOffset(str, 21, new int[]{OFFSET_JUDGE_3P, OFFSET_LIFT}));

						if (!detail) {
							detail = true;
							addJudgeDetail(skin, values, srcw, dstw, srch, dsth, 2);
						}

					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOWCOMBO_1P") {
			@Override
			public void execute(String[] str) {
				setSrcNowCombo(0, str);
			}
		});

		addCommandWord(new CommandWord("DST_NOWCOMBO_1P") {
			@Override
			public void execute(String[] str) {
				setDstNowCombo(0, str, readOffset(str, 21, new int[]{OFFSET_JUDGE_1P, OFFSET_LIFT}));
			}
		});

		addCommandWord(new CommandWord("SRC_NOWCOMBO_2P") {
			@Override
			public void execute(String[] str) {
				setSrcNowCombo(1, str);
			}
		});

		addCommandWord(new CommandWord("DST_NOWCOMBO_2P") {
			@Override
			public void execute(String[] str) {
				setDstNowCombo(1, str, readOffset(str, 21, new int[]{OFFSET_JUDGE_2P, OFFSET_LIFT}));
			}
		});

		addCommandWord(new CommandWord("SRC_NOWCOMBO_3P") {
			@Override
			public void execute(String[] str) {
				setSrcNowCombo(2, str);
			}
		});

		addCommandWord(new CommandWord("DST_NOWCOMBO_3P") {
			@Override
			public void execute(String[] str) {
				setDstNowCombo(2, str, readOffset(str, 21, new int[]{OFFSET_JUDGE_3P, OFFSET_LIFT}));
			}
		});

		addCommandWord(new CommandWord("SRC_JUDGELINE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					line = new SkinImage(images, values[10], values[9]);
					// System.out.println("Object Added - " +
					// (part.getTiming()));
				}
				if (line != null) {
					skin.add(line);
				}
			}
		});

		addCommandWord(new CommandWord("DST_JUDGELINE") {
			@Override
			public void execute(String[] str) {
				if (line != null) {
					int[] values = parseInt(str);
					if (values[5] < 0) {
						values[3] += values[5];
						values[5] = -values[5];
					}
					if (values[6] < 0) {
						values[4] += values[6];
						values[6] = -values[6];
					}
					line.setDestination(values[2], values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], readOffset(str, 21, new int[]{OFFSET_LIFT}));
				}
			}
		});
		addCommandWord(new CommandWord("SRC_NOTECHART_1P") {
			//#SRC_NOTECHART_1P,(index),(gr),(x),(y),(w),(h),(div_x),(div_y),(cycle),(timer),field_w,field_h,(start),(end),delay,backTexOff,orderReverse,noGap
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				noteobj = new SkinNoteDistributionGraph(values[1], values[15], values[16], values[17], values[18], values[19]);
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
						values[16], values[17], values[18], values[19], values[20], readOffset(str, 21));
			}
		});
		addCommandWord(new CommandWord("SRC_TIMING_1P") {
			//#SRC_NOTECHART_1P,(index),(gr),(x),width,height,judgeWidthMillis,lineWidth,lineColor,centerColor,PGColor,GRColor,GDColor,BDColor,PRColor,transparent,drawDecay,(),()

			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				timingobj = new SkinTimingVisualizer(values[4], values[6], values[7], str[8], str[9], str[10], str[11], str[12], str[13], str[14], values[15], values[16]);
				gauge = new Rectangle(0, 0, values[4], values[5]);
				skin.add(timingobj);
			}
		});
		addCommandWord(new CommandWord("DST_TIMING_1P") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				gauge.x = values[3];
				gauge.y = src.height - values[4];
				skin.setDestination(timingobj, values[2], gauge.x, gauge.y, gauge.width, gauge.height, values[7], values[8],
						values[9], values[10], values[11], values[12], values[13], values[14], values[15],
						values[16], values[17], values[18], values[19], values[20], readOffset(str, 21));
			}
		});

		addCommandWord(new CommandWord("SRC_HIDDEN") {
			//#SRC_HIDDEN, (NULL), g, x, y, w, h,div_x, div_y, cycle, timer, disapear_line, isDisapearLineLinkLift
			@Override
			public void execute(String[] str) {
				setSrcHidden(str);
			}
		});
		addCommandWord(new CommandWord("DST_HIDDEN") {
			@Override
			public void execute(String[] str) {
				if (hidden != null) {
					setDstHidden(str, new int[]{OFFSET_LIFT, OFFSET_HIDDEN_COVER});
				}
			}
		});
		addCommandWord(new CommandWord("SRC_LIFT") {
			//#SRC_LIFT, (NULL), g, x, y, w, h,div_x, div_y, cycle, timer, disapear_line, isDisapearLineLinkLift
			@Override
			public void execute(String[] str) {
				setSrcHidden(str);
			}
		});
		addCommandWord(new CommandWord("DST_LIFT") {
			@Override
			public void execute(String[] str) {
				if (hidden != null) {
					setDstHidden(str, new int[]{OFFSET_LIFT});
				}
			}
		});
	}
	
	private void setSrcNowCombo(int index, String[] str) {
		int[] values = parseInt(str);
		final int divx = values[7] > 0 ? values[7] : 1;
		final int divy = values[8] > 0 ? values[8] : 1;
		TextureRegion[] simages = getSourceImage(values);
		if (simages != null) {
			TextureRegion[][] images = new TextureRegion[divy][divx];
			for (int i = 0; i < divx; i++) {
				for (int j = 0; j < divy; j++) {
					images[j][i] = simages[j * divx + i];
				}
			}

			judge[index].getJudgeCount()[values[1] <= 5  ? (5 - values[1]) : values[1]] = new SkinNumber(images, values[10], values[9], values[13],
					images.length > 10 ? 2 : 0, values[15], values[11]);
			judge[index].getJudgeCount()[values[1] <= 5  ? (5 - values[1]) : values[1]].setAlign(values[12] == 1 ?  2 : values[12]);
			// System.out.println("Number Added - " +
			// (num.getId()));
		}
	}

	private void setDstNowCombo(int index, String[] str, int[] offset) {
		final SkinJudge sj = judge[index];
		if (sj != null && sj.getJudgeCount()[Integer.parseInt(str[1]) <= 5 ? (5 - Integer.parseInt(str[1])) : Integer.parseInt(str[1])] != null) {
			int[] values = parseInt(str);
			sj.getJudgeCount()[values[1] <= 5  ? (5 - values[1]) : values[1]].setRelative(true);
			float x = values[3];
			if(sj.getJudgeCount()[values[1] <= 5  ? (5 - values[1]) : values[1]].getAlign() == 2) {
				x -= sj.getJudgeCount()[values[1] <= 5  ? (5 - values[1]) : values[1]].getKeta() * values[5] / 2;
			}
			sj.getJudgeCount()[values[1] <= 5  ? (5 - values[1]) : values[1]].setDestination(values[2], x * dstw / srcw,
					-values[4] * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch, values[7],
					values[8], values[9], values[10], values[11], values[12], values[13], values[14],
					values[15], values[16], values[17], values[18], values[19], values[20], offset);
		}
	}

	private void addNote(String[] str, SkinSource[] note, boolean animation) {
		int[] values = parseInt(str);
		int lane = values[1];
		if (lane % 10 == 0) {
			lane = mode.scratchKey.length > (lane / 10) ? mode.scratchKey[(lane / 10)] : -1;
		} else {
			final int offset = (lane / 10) * (laner.length / playerr.length);
			lane = (lane > 10) ? lane - 11 : lane - 1;
			if (lane >= (laner.length - mode.scratchKey.length) / playerr.length) {
				lane = -1;
			} else {
				lane += offset;
			}
		}
		if(lane < 0) {
			return;
		}
		if (lane < note.length && note[lane] == null) {
			TextureRegion[] images = getSourceImage(values);
			if (images != null) {
				note[lane] = new SkinSourceImage(images, animation ? values[10] : 0, animation ? values[9] : 0);
			}
		}
	}

	private static void addJudgeDetail(Skin skin, int[] values, float srcw, float dstw, float srch, float dsth, int side) {
		Texture tex = new Texture("skin/default/judgedetail.png");

		final float dw = dstw / 1280f;
		final float dh = dsth / 720f;

		final int[] JUDGE_TIMER = { TIMER_JUDGE_1P, TIMER_JUDGE_2P, TIMER_JUDGE_3P };
		final int[] OPTION_EARLY = { OPTION_1P_EARLY, OPTION_2P_EARLY, OPTION_3P_EARLY };
		final int[] OPTION_LATE = { OPTION_1P_LATE, OPTION_2P_LATE, OPTION_3P_LATE };
		final int[] VALUE_JUDGE_DURATION = { VALUE_JUDGE_1P_DURATION, VALUE_JUDGE_2P_DURATION, VALUE_JUDGE_3P_DURATION };
		final int[] OPTION_PERFECT = { OPTION_1P_PERFECT, OPTION_2P_PERFECT, OPTION_3P_PERFECT };
		final int[] OFFSET_JUDGE = { OFFSET_JUDGE_1P, OFFSET_JUDGE_2P, OFFSET_JUDGE_3P };
		final int[] OFFSET_JUDGE_DETAIL = { OFFSET_JUDGEDETAIL_1P, OFFSET_JUDGEDETAIL_2P, OFFSET_JUDGEDETAIL_3P };

		SkinImage early = new SkinImage(new TextureRegion(tex, 0, 0, 50,20));
		early.setDestination(0, (values[3] + values[5] / 2) * dstw / srcw,
				dsth - (values[4] - 5) * dsth / srch, 40 * dw, 16 * dh, 0, 255,
				255, 255, 255, 0, 0, 0, 0, -1, JUDGE_TIMER[side], 1998, 0, OPTION_EARLY[side], new int[]{OFFSET_JUDGE_DETAIL[side], OFFSET_LIFT});
		early.setDestination(500, (values[3] + values[5] / 2) * dstw / srcw,
				dsth - (values[4] - 5) * dsth / srch, 40 * dw, 16 * dh, 0, 255,
				255, 255, 255, 0, 0, 0, 0, -1, JUDGE_TIMER[side], 1998, 0, OPTION_EARLY[side], new int[]{OFFSET_JUDGE_DETAIL[side], OFFSET_LIFT});
		skin.add(early);
		SkinImage late = new SkinImage(new TextureRegion(tex, 50, 0, 50,20));
		late.setDestination(0, (values[3] + values[5] / 2) * dstw / srcw,
				dsth - (values[4] - 5) * dsth / srch, 40 * dw, 16 * dh, 0, 255,
				255, 255, 255, 0, 0, 0, 0, -1, JUDGE_TIMER[side], 1998, 0, OPTION_LATE[side], new int[]{OFFSET_JUDGE_DETAIL[side], OFFSET_LIFT});
		late.setDestination(500, (values[3] + values[5] / 2) * dstw / srcw,
				dsth - (values[4] - 5) * dsth / srch, 40 * dw, 16 * dh, 0, 255,
				255, 255, 255, 0, 0, 0, 0, -1, JUDGE_TIMER[side], 1998, 0, OPTION_LATE[side], new int[]{OFFSET_JUDGE_DETAIL[side], OFFSET_LIFT});
		skin.add(late);

		TextureRegion[][] images = TextureRegion.split(tex, 10, 20);
		SkinNumber num = new SkinNumber(new TextureRegion[][] { images[1] },
				new TextureRegion[][] { images[2] }, 0, 0, 4, 0, values[15], VALUE_JUDGE_DURATION[side]);
		num.setAlign(values[12]);
		num.setDestination(0, (values[3] + values[5] / 2) * dstw / srcw,
				dsth - (values[4] - 5) * dsth / srch, 8 * dw, 16 * dh, 0, 255,
				255, 255, 255, 0, 0, 0, 0, -1, JUDGE_TIMER[side], 1999, 0, OPTION_PERFECT[side], new int[]{OFFSET_JUDGE_DETAIL[side], OFFSET_LIFT});
		num.setDestination(500, (values[3] + values[5] / 2) * dstw / srcw,
				dsth - (values[4] - 5) * dsth / srch, 8 * dw, 16 * dh, 0, 255,
				255, 255, 255, 0, 0, 0, 0, -1, JUDGE_TIMER[side], 1999, 0, OPTION_PERFECT[side], new int[]{OFFSET_JUDGE_DETAIL[side], OFFSET_LIFT});
		skin.add(num);
		SkinNumber num2 = new SkinNumber(new TextureRegion[][] { images[3] },
				new TextureRegion[][] { images[4] }, 0, 0, 4, 0, values[15], VALUE_JUDGE_DURATION[side]);
		num2.setAlign(values[12]);
		num2.setDestination(0, (values[3] + values[5] / 2) * dstw / srcw,
				dsth - (values[4] - 5) * dsth / srch, 8 * dw, 16 * dh, 0, 255,
				255, 255, 255, 0, 0, 0, 0, -1, JUDGE_TIMER[side], 1999, 0, -(OPTION_PERFECT[side]), new int[]{OFFSET_JUDGE_DETAIL[side], OFFSET_LIFT});
		num2.setDestination(500, (values[3] + values[5] / 2) * dstw / srcw,
				dsth - (values[4] - 5) * dsth / srch, 8 * dw, 16 * dh, 0, 255,
				255, 255, 255, 0, 0, 0, 0, -1, JUDGE_TIMER[side], 1999, 0, -(OPTION_PERFECT[side]), new int[]{OFFSET_JUDGE_DETAIL[side], OFFSET_LIFT});
		skin.add(num2);
	}

	private void setSrcHidden(String[] str) {
		hidden = null;
		int[] values = parseInt(str);
		TextureRegion[] images = getSourceImage(values);
		if (images != null) {
			hidden = new SkinHidden(images, values[10], values[9]);
		}
		if (hidden != null) {
			if(str[11].length() > 0 && values[11] > 0) hidden.setDisapearLine(dsth - values[11] * dsth / srch);
			hidden.setDisapearLineLinkLift(str[12].length() == 0 || values[12] != 0);
			skin.add(hidden);
		}
	}

	private void setDstHidden(String[] str, int[] offset) {
		int[] values = parseInt(str);
		if (values[5] < 0) {
			values[3] += values[5];
			values[5] = -values[5];
		}
		if (values[6] < 0) {
			values[4] += values[6];
			values[6] = -values[6];
		}
		hidden.setDestination(values[2], values[3] * dstw / srcw,
				dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
				values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
				values[12], values[13], values[14], values[15], values[16], values[17], values[18],
				values[19], values[20], readOffset(str, 21, offset));
	}

	public PlaySkin loadSkin(MainState player, SkinHeader header, IntIntMap option) throws IOException {
		mode = type.getMode();
		note = new SkinSource[mode.key];
		lnstart = new SkinSource[mode.key];
		lnend = new SkinSource[mode.key];
		lnbody = new SkinSource[mode.key];
		lnbodya = new SkinSource[mode.key];
		hcnstart = new SkinSource[mode.key];
		hcnend = new SkinSource[mode.key];
		hcnbody = new SkinSource[mode.key];
		hcnbodya = new SkinSource[mode.key];
		hcnbodyd = new SkinSource[mode.key];
		hcnbodyr = new SkinSource[mode.key];
		mine = new SkinSource[mode.key];
		laner = new Rectangle[mode.key];
		scale = new float[mode.key];
		dstnote2 = new int[mode.key];
		Arrays.fill(dstnote2,  Integer.MIN_VALUE);

		playerr = new Rectangle[mode.player];
		for(int i = 0;i < playerr.length;i++) {
			playerr[i] = new Rectangle();
		}

		this.loadSkin(new PlaySkin(header), player, option);

		//白数字が0の時のレーンカバーのy座標の分だけレーンの高さを減らす
		//TODO 固有実装の汎用化
		float laneCoverPosition = getLaneCoverPosition();
		if(laneCoverPosition > 0) {
			for(int i = 0; i < laner.length; i++) {
				laner[i].height = laner[i].height - (dsth - laneCoverPosition);
			}
		}
		lanerender.setLaneRegion(laner, scale, dstnote2, skin);

		SkinImage[] skinline = new SkinImage[lines[0] != null ? (lines[1] != null ? 2 : 1) : 0];
		for(int i = 0;i < skinline.length;i++) {
			skinline[i] = lines[i];
		}
		skin.setLine(skinline);
		SkinImage[] skintime = new SkinImage[skinline.length];
		for(int i = 0;i < skintime.length;i++) {
			if(lines[i + 6] == null && lines[i] != null) {
				makeDefaultLines(i + 6, 1, 64, 192, 192);
			}
			skintime[i] = lines[i + 6];
		}
		skin.setTimeLine(skintime);

		SkinImage[] skinbpm = new SkinImage[skinline.length];
		for(int i = 0;i < skinbpm.length;i++) {
			if(lines[i + 2] == null && lines[i] != null) {
				makeDefaultLines(i + 2, 2, 0, 192, 0);
			}
			skinbpm[i] = lines[i + 2];
		}
		skin.setBPMLine(skinbpm);

		SkinImage[] skinstop = new SkinImage[skinline.length];
		for(int i = 0;i < skinstop.length;i++) {
			if(lines[i + 4] == null && lines[i] != null) {
				makeDefaultLines(i + 4, 2, 192, 192, 0);
			}
			skinstop[i] = lines[i + 4];
		}
		skin.setStopLine(skinstop);

		int judge_reg = 1;
		for(int i = 1 ; i < judge.length ; i++) {
			if(judge[i] != null) judge_reg++;
			else break;
		}
		skin.setJudgeregion(judge_reg);
		skin.setLaneRegion(laner);
		skin.setLaneGroupRegion(playerr);

		return skin;
	}

	private void makeDefaultLines(int index, int h, int r, int g, int b) {
		Texture tex = new Texture("skin/default/system.png");
		int[] values = parseInt(linevalues[index % 2]);
		SkinImage li = new SkinImage(new TextureRegion(tex, 0, 0, 1,1));
		lines[index] = li;
		lines[index].setDestination(values[2], values[3] * dstw / srcw, dsth - (values[4] + values[6]) * dsth / srch,
				values[5] * dstw / srcw, values[6] * dsth / srch * h, values[7], 255, r, g,
				b, values[12], values[13], values[14], values[15], values[16],
				values[17], values[18], values[19], values[20], readOffset(linevalues[index % 2], 21, new int[]{OFFSET_LIFT}));
	}

	/*
	 * 白数字が0の時のレーンカバーのy座標
	 */
	public float getLaneCoverPosition() {
		if(skin.laneCover != null) {
			SkinObjectDestination[] dst = skin.laneCover.getAllDestination();
			return dst[dst.length - 1].region.y;
		}
		return -1;
	}
}

enum PlayCommand implements LR2SkinLoader.Command<LR2PlaySkinLoader> {
	CLOSE {
		@Override
		public void execute(LR2PlaySkinLoader loader, String[] str) {
			loader.skin.setClose(Integer.parseInt(str[1]));
		}
	},
	PLAYSTART {
		@Override
		public void execute(LR2PlaySkinLoader loader, String[] str) {
			loader.skin.setPlaystart(Integer.parseInt(str[1]));
		}
	},
	LOADSTART {
		@Override
		public void execute(LR2PlaySkinLoader loader, String[] str) {
			loader.skin.setLoadstart(Integer.parseInt(str[1]));
		}
	},
	LOADEND {
		@Override
		public void execute(LR2PlaySkinLoader loader, String[] str) {
			loader.skin.setLoadend(Integer.parseInt(str[1]));
		}
	}
	;

}
