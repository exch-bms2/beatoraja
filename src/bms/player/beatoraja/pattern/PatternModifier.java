package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.utils.IntArray;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.MineNote;
import bms.model.Mode;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.PlayerConfig;

/**
 * 譜面オプションの抽象クラス
 *
 * @author exch
 */
public abstract class PatternModifier {

	/**
	 * 譜面変更のアシストレベル
	 */
	private AssistLevel assist = AssistLevel.NONE;

	/**
	 * 1P側、2P側どちらの譜面を変更するか
	 */
	private int modifyTargetSide;

	public static final int SIDE_1P = 0;
	public static final int SIDE_2P = 1;
	
	private long seed = (long) (Math.random() * 65536 * 256);

	public PatternModifier() {

	}

	public PatternModifier(int assist) {
		this.assist = AssistLevel.values()[assist];
	}

	// TODO PatternModifyLogは現状保存していないため、返り値不要
	public abstract List<PatternModifyLog> modify(BMSModel model);

	/**
	 * 譜面変更ログの通りに譜面オプションをかける
	 *
	 * @param model
	 *            譜面オプションをかける対象のBMSModel
	 * @param log
	 *            譜面変更ログ
	 */
	public static void modify(BMSModel model, List<PatternModifyLog> log) {
		for (TimeLine tl : model.getAllTimeLines()) {
			PatternModifyLog pm = null;
			for (PatternModifyLog pms : log) {
				if (pms.section == tl.getSection()) {
					pm = pms;
					break;
				}
			}
			if (pm != null) {
				int lanes = model.getMode().key;
				Note[] notes = new Note[lanes];
				Note[] hnotes = new Note[lanes];
				for (int i = 0; i < lanes; i++) {
					final int mod = i < pm.modify.length ? pm.modify[i] : i;
					notes[i] = tl.getNote(mod);
					hnotes[i] = tl.getHiddenNote(mod);
				}
				for (int i = 0; i < lanes; i++) {
					tl.setNote(i, notes[i]);
					tl.setHiddenNote(i, hnotes[i]);
				}
			}
		}
	}

	public AssistLevel getAssistLevel() {
		return assist;
	}

	protected void setAssistLevel(AssistLevel assist) {
		this.assist = (assist != null ? assist : AssistLevel.NONE);
	}

	public int getModifyTarget() {
		return modifyTargetSide;
	}

	public void setModifyTarget(int type) {
		this.modifyTargetSide = type;
	}

	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		if(seed >= 0) {
			this.seed = seed;			
		}
	}

    /**
     * 譜面オプションに対応したPatternModifierを生成する
     *
     * @param id 譜面オプションID
     * @param side 譜面オプションサイド(1P or 2P)
     * @param mode 譜面のモード
     * @return
     */
    public static PatternModifier create(int id, int side, Mode mode, PlayerConfig config) {
		final Random r = Random.getRandom(id, mode);
		PatternModifier pm = switch (r.unit) {
			case NONE -> new PatternModifier() {
				@Override
				public List<PatternModifyLog> modify(BMSModel model) {
					return Collections.emptyList();
				}
			};
			case LANE, PLAYER -> new LaneShuffleModifier(r);
			case NOTE -> new NoteShuffleModifier(r, side, mode, config);
		};
		if (pm != null) {
			pm.setModifyTarget(side);
		}
		return pm;
	}

	/**
	 * 変更対象のレーン番号が格納された配列を返す
	 * @param mode
	 * プレイモード
	 * @param containsScratch
	 * スクラッチレーンを含むか
	 * @return レーン番号の配列
	 */
	protected int[] getKeys(Mode mode, boolean containsScratch) {
		int key = (modifyTargetSide == SIDE_2P) ? mode.key / mode.player : 0;
		if (key == mode.key) {
			return new int[0];
		} else {
			IntArray keys = new IntArray();
			for (int i = 0; i < mode.key / mode.player; i++) {
				if (containsScratch || !mode.isScratchKey(key + i)) {
					keys.add(key + i);
				}
			}
			return keys.toArray();
		}
	}

	protected static void moveToBackground(TimeLine[] tls, TimeLine tl, int lane) {
		Note n = tl.getNote(lane);
		if(n == null) {
			return;
		}
		if(n instanceof LongNote) {
			LongNote pln = ((LongNote) tl.getNote(lane)).getPair();
			for(TimeLine tl2 : tls) {
				if(tl2.getNote(lane) == pln) {
					tl2.addBackGroundNote(pln);
					tl2.setNote(lane, null);
					break;
				}
			}
		}

		if(!(n instanceof MineNote)) {
			tl.addBackGroundNote(tl.getNote(lane));
		}
		tl.setNote(lane, null);

	}

	public enum AssistLevel {
		NONE, LIGHT_ASSIST, ASSIST;
	}
}
