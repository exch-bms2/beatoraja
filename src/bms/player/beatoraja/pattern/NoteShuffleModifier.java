package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.NormalNote;
import bms.model.Note;
import bms.model.TimeLine;

/**
 * タイムライン単位でノーツを入れ替えるためのクラス．
 * 
 * @author exch
 */
public class NoteShuffleModifier extends PatternModifier {

	/**
	 * タイムライン毎にノーツをランダムに入れ替える
	 */
	public static final int S_RANDOM = 0;
	/**
	 * 初期の並べ替えをベースに、螺旋状に並べ替える
	 */
	public static final int SPIRAL = 1;
	/**
	 * ノーツをスクラッチレーンに集約する
	 */
	public static final int ALL_SCR = 2;
	/**
	 * S-RANDOMに縦連が極力来ないように配置する
	 */
	public static final int H_RANDOM = 3;
	/**
	 * スクラッチレーンを含めたS-RANDOM
	 */
	public static final int S_RANDOM_EX = 4;

	private int type;
	
	private int inc;

	public NoteShuffleModifier(int type) {
		super(type >= ALL_SCR ? 1 : 0);
		this.type = type;
	}

	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		List<PatternModifyLog> log = new ArrayList<PatternModifyLog>();
		int lanes = 8;
		int[] random = new int[0];
		int[] ln = new int[lanes];
		Arrays.fill(ln, -1);
		for (int time : model.getAllTimes()) {
			TimeLine tl = model.getTimeLine(time);
			Note[] notes = new Note[lanes];
			for (int i = 0; i < lanes; i++) {
				notes[i] = tl.getNote(i);
			}
			List<Integer> l;
			switch (type) {
			case S_RANDOM:
				l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
				random = new int[8];
				for (int lane = 0; lane < 7; lane++) {
					if(ln[lane] != -1) {
						random[lane] = ln[lane];
						l.remove((Integer)ln[lane]);
					}
				}
				for (int lane = 0; lane < 7; lane++) {
					if(ln[lane] == -1) {
						int r = (int) (Math.random() * l.size());
						random[lane] = l.get(r);
						l.remove(r);						
					}
				}
				random[7] = 7;
				break;
			case SPIRAL:
				if(random.length == 0) {
					// 初期値の作成
					random = new int[8];
					int index = (int) (Math.random() * 7);
					int j = (int) (Math.random() * 2) >= 1 ? 1 : 6;
					for(int i = 0;i < random.length - 1;i++) {
						random[i] = index;
						index = (index + j) % (random.length - 1);
					}
					inc = (int) (Math.random() * 6) + 1;
				} else {
					boolean cln = false;
					for (int lane = 0; lane < 7; lane++) {
						if(ln[lane] != -1) {
							cln = true;
						}
					}
					if(!cln) {
						int[] nrandom = new int[random.length];
						int index = inc;
						for(int i = 0;i < random.length - 1;i++) {
							nrandom[i] = random[index];
							index = (index + 1) % (random.length - 1);
						}
						random = nrandom;						
					}
				}
				random[7] = 7;
				break;
			case ALL_SCR:
				random = new int[]{0,1,2,3,4,5,6,7};
				if(ln[7] == -1 && notes[7] == null) {
					for (int lane = 0; lane < 7; lane++) {
						if(notes[lane] != null && notes[lane] instanceof NormalNote) {
							random[7] = lane;
							random[lane] = 7;
							break;
						}
					}					
				}
				break;
			case H_RANDOM:
				// TODO 未実装
			case S_RANDOM_EX:
				List<Integer> le = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
				random = new int[8];
				for (int lane = 0; lane < 8; lane++) {
					if(ln[lane] != -1) {
						random[lane] = ln[lane];
						le.remove((Integer)ln[lane]);
					}
				}
				for (int lane = 0; lane < 8; lane++) {
					int re = (int) (Math.random() * le.size());
					random[lane] = le.get(re);
					le.remove(re);
				}
				break;

			}

			for (int i = 0; i < lanes; i++) {
				Note n = notes[random[i]];
				if (n instanceof LongNote) {
					LongNote ln2 = (LongNote) n;
					if (ln2.getStart() == tl) {
						tl.addNote(i, n);
						ln[i] = random[i];
					} else {
						tl.addNote(i, n);
						ln[i] = -1;
					}
				} else {
					tl.addNote(i, n);
				}
			}
			log.add(new PatternModifyLog(time, random));
		}
		return log;
	}
}
