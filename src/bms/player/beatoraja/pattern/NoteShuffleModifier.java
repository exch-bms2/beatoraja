package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Mode;
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
	/**
	 * 次のTimeLine増加分(SPIRAL用)
	 */
	private int inc;
	/**
	 * ノーツが存在する直前のTimeLine(H-RANDOM用)
	 */
	private TimeLine prev;

	public NoteShuffleModifier(int type) {
		super(type >= ALL_SCR ? 1 : 0);
		this.type = type;
	}

	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		List<PatternModifyLog> log = new ArrayList<PatternModifyLog>();
		Mode mode = model.getMode();
		int lanes = mode.key;
		int[] random = new int[0];
		int[] ln = new int[lanes];
		Arrays.fill(ln, -1);
		for (TimeLine tl : model.getAllTimeLines()) {
			if (tl.existNote() || tl.existHiddenNote()) {
				Note[] notes = new Note[lanes];
				Note[] hnotes = new Note[lanes];
				for (int i = 0; i < lanes; i++) {
					notes[i] = tl.getNote(i);
					hnotes[i] = tl.getHiddenNote(i);
				}
				List<Integer> l;
				int[] keys;
				switch (type) {
				case S_RANDOM:
					keys = getKeys(mode, false);
					random = keys.length > 0 ? shuffle(keys, ln) : keys;
					break;
				case SPIRAL:
					keys = getKeys(mode, false);
					if (random.length == 0) {
						// 初期値の作成
						int max = 0;
						for (int key : keys) {
							max = Math.max(max, key);
						}
						random = new int[max + 1];
						for (int i = 0; i < random.length; i++) {
							random[i] = i;
						}

						int index = (int) (Math.random() * keys.length);
						int j = (int) (Math.random() * 2) >= 1 ? 1 : keys.length - 1;
						for (int i = 0; i < keys.length; i++) {
							random[keys[i]] = keys[index];
							index = (index + j) % keys.length;
						}
						inc = (int) (Math.random() * (keys.length - 1)) + 1;
						Logger.getGlobal().info("SPIRAL - 開始位置:" + index + " 増分:" + inc);
					} else {
						boolean cln = false;
						for (int lane = 0; lane < keys.length; lane++) {
							if(ln[keys[lane]] != -1) {
								cln = true;
							}
						}
						if (!cln) {
							int[] nrandom = Arrays.copyOf(random, random.length);
							int index = inc;
							for (int i = 0; i < keys.length; i++) {
								nrandom[keys[i]] = random[keys[index]];
								index = (index + 1) % keys.length;
							}
							random = nrandom;
						}
					}

					break;
				case ALL_SCR:
					random = new int[mode.key];
					for (int i = 0; i < random.length; i++) {
						random[i] = i;
					}
					boolean rightside = (getModifyTarget() == SIDE_2P);
					if(!rightside || mode.player == 2) {
						int sckey = rightside ? mode.scratchKey[1] : mode.scratchKey[0];
						if (ln[sckey] == -1 && notes[sckey] == null) {
							for (int i = 0; i < mode.key / mode.player; i++) {
								int lane = i + (rightside ? mode.key / mode.player : 0);
								if (lane != sckey && notes[lane] != null && notes[lane] instanceof NormalNote) {
									random[sckey] = lane;
									random[lane] = sckey;
									break;
								}
							}
						}						
					}
					break;
				case H_RANDOM:
					// TODO ノーツのあるレーンを先行して優先配置する方式へ変更
					l = new ArrayList();
					int max = 0;
					keys = getKeys(mode, false);
					for(int key : keys) {
						l.add(key);
						max = Math.max(max, key);
					}
					random = new int[max + 1];
					for (int i = 0; i < random.length; i++) {
						random[i] = i;
					}
					for (int lane = 0; lane < keys.length; lane++) {
						if (ln[keys[lane]] != -1) {
							random[keys[lane]] = ln[keys[lane]];
							l.remove((Integer)ln[keys[lane]]);
						}
					}
					final int offset = (int)(Math.random() * keys.length);
					for (int index = 0; index < keys.length; index++) {
						final int lane = (index + offset) % keys.length;
						if (ln[keys[lane]] == -1) {
							int r = -1;
							int count = l.size() - tl.getTotalNotes() - (prev != null ? prev.getTotalNotes() : 0);
							for (int i = 0; i < 100; i++) {
								r = (int) (Math.random() * l.size());
								if (prev == null || (prev.existNote(keys[lane]) && !tl.existNote(l.get(r)))
										|| (!prev.existNote(keys[lane]) && tl.existNote(l.get(r)))
										|| (count > 0 && !prev.existNote(keys[lane]) && !tl.existNote(l.get(r)))) {
									if (prev != null && !prev.existNote(keys[lane]) && !tl.existNote(l.get(r))) {
										count--;
									}
									break;
								}
							}
							random[keys[lane]] = l.get(r);
							l.remove(r);
						}
					}
					if (tl.getTotalNotes(BMSModel.LNTYPE_HELLCHARGENOTE) > 0) {
						prev = tl;
					}
					break;
				case S_RANDOM_EX:
					keys = getKeys(mode, true);
					random = keys.length > 0 ? shuffle(keys, ln) : keys;
					break;

				}

				for (int i = 0; i < lanes; i++) {
					final int mod = i < random.length ? random[i] : i;
					Note n = notes[mod];
					Note hn = hnotes[mod];
					if (n instanceof LongNote) {
						LongNote ln2 = (LongNote) n;
						if (ln2.isEnd()) {
							tl.setNote(i, n);
							ln[i] = -1;
						} else {
							tl.setNote(i, n);
							ln[i] = mod;
						}
					} else {
						tl.setNote(i, n);
					}
					tl.setHiddenNote(i, hn);
				}
				log.add(new PatternModifyLog(tl.getSection(), random));
			}
		}
		return log;
	}
}
