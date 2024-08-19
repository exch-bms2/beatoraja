package bms.player.beatoraja.pattern;

import bms.model.*;

/**
 * 指定のレーンを自動演奏にするオプション
 * 
 * @author exch
 */
public class AutoplayModifier extends PatternModifier {

	/**
	 * 自動演奏にするレーン
	 */
	private final int[] lanes;

	private final int margin;

	public AutoplayModifier(int[] lanes) {
		this(lanes, 0);
	}

	public AutoplayModifier(int[] lanes, int margin) {
		this.lanes = lanes;
		this.margin = margin;
	}
	@Override
	public void modify(BMSModel model) {
		AssistLevel assist = AssistLevel.NONE;
		TimeLine[] tls = model.getAllTimeLines();
		boolean[] lns = new boolean[model.getMode().key];
		for (int i = 0, pos = 0;i < tls.length;i++) {
			final TimeLine tl = tls[i];
			boolean remove = false;

			if(margin > 0) {
				while(tls[pos].getTime() < tl.getTime() - margin) {
					for(int lane = 0;lane < lns.length;lane++) {
						if(tls[pos].getNote(lane) instanceof LongNote note) {
							lns[lane] = !note.isEnd();
						}
					}
					pos++;
				}
				int endtime = tl.getTime() + margin;
				for(int lane : lanes) {
					if(tl.getNote(lane) instanceof LongNote ln && !ln.isEnd()) {
						endtime = Math.max(ln.getPair().getTime() + margin, endtime);
					}
				}

				for(int j = pos;j < tls.length && tls[j].getTime() < endtime;j++) {
					for(int lane = 0;lane < model.getMode().key;lane++) {
						boolean b = true;
						for(int rlane :lanes) {
							if(lane == rlane) {
								b = false;
								break;
							}
						}
						if(b && (tls[j].getNote(lane) != null || lns[lane])) {
							remove = true;
							break;
						}
					}
				}
			} else {
				remove = true;
			}

			if(remove) {
				for(int lane : lanes) {
					if(tl.existNote(lane)) {
						assist = AssistLevel.ASSIST;
					}
					moveToBackground(tls, tl, lane);
				}
			}
		}
		setAssistLevel(assist);
	}

}
