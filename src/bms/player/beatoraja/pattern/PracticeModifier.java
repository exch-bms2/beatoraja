package bms.player.beatoraja.pattern;

import bms.model.*;

/**
 * プラクティス時に選択範囲以外の可視ノーツをBGノーツに移動するクラス
 *
 * @author exch
 */
public class PracticeModifier extends PatternModifier {

	/**
	 * 開始時間(ms)
	 */
	private long start;
	/**
	 * 終了時間(ms)
	 */
	private long end;

	public PracticeModifier(long start, long end) {
		super(AssistLevel.ASSIST);
		this.start = start;
		this.end = end;
	}

	@Override
	public void modify(BMSModel model) {
		int totalnotes = model.getTotalNotes();
		final TimeLine[] tls = model.getAllTimeLines();
		for (TimeLine tl : tls) {
			for (int i = 0; i < model.getMode().key; i++) {
				if(tl.getTime() < start || tl.getTime() >= end) {
					moveToBackground(tls, tl, i);
				}
			}
		}
		model.setTotal(model.getTotal() * model.getTotalNotes() / totalnotes);
	}

}
