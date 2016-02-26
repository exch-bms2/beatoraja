package bms.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 小節
 * 
 * @author exch
 */
public class Section {

	public static final int LANE_AUTOPLAY = 1;
	public static final int SECTION_RATE = 2;
	public static final int BPM_CHANGE = 3;
	public static final int BGA_PLAY = 4;
	public static final int POOR_PLAY = 6;
	public static final int LAYER_PLAY = 7;
	public static final int BPM_CHANGE_EXTEND = 8;
	public static final int STOP = 9;

	public static final int P1_KEY_BASE = 11;
	public static final int P2_KEY_BASE = 21;
	public static final int P1_INVISIBLE_KEY_BASE = 31;
	public static final int P2_INVISIBLE_KEY_BASE = 41;
	public static final int P1_LONG_KEY_BASE = 51;
	public static final int P2_LONG_KEY_BASE = 61;
	public static final int P1_MINE_KEY_BASE = 131;
	public static final int P2_MINE_KEY_BASE = 141;

	/**
	 * 小節の拡大倍率
	 */
	private double rate = 1.0;
	/**
	 * ストップシーケンス
	 */
	private double[] stop = new double[0];
	/**
	 * BPM変更
	 */
	private double[] bpm_change = new double[0];
	/**
	 * BGレーン
	 */
	private List<String[]> auto = new ArrayList<String[]>();
	/**
	 * BGA
	 */
	private String[] bga = new String[0];
	/**
	 * レイヤー
	 */
	private String[] layer = new String[0];
	/**
	 * POORアニメーション
	 */
	private String[] poor = new String[0];
	/**
	 * 1P通常ノート
	 */
	private String[][] play_1 = new String[9][0];
	/**
	 * 1P不可視ノート
	 */
	private String[][] play_1_invisible = new String[9][0];
	/**
	 * 1Pロングノート
	 */
	private String[][] play_1_ln = new String[9][0];
	/**
	 * 1P地雷ノート
	 */
	private String[][] play_1_mine = new String[9][0];
	/**
	 * 2P通常ノート
	 */
	private String[][] play_2 = new String[9][0];
	/**
	 * 2P不可視ノート
	 */
	private String[][] play_2_invisible = new String[9][0];
	/**
	 * 2Pロングノート
	 */
	private String[][] play_2_ln = new String[9][0];
	/**
	 * 2P地雷ノート
	 */
	private String[][] play_2_mine = new String[9][0];
	/**
	 * 前の小節
	 */
	private Section prev;

	private int usekeys = 5;

	private BMSModel model;

	private int sectionnum;

	public Section(BMSModel model, Section prev, String[] lines) {
		this.model = model;
		if (model.getUseKeys() == 9) {
			usekeys = 9;
		} else {
			usekeys = model.getPlayer() > 1 ? 10 : 5;
		}
		this.prev = prev;
		if (prev != null) {
			sectionnum = prev.sectionnum + 1;
		}
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			int channel = 0;
			try {
				channel = Integer.parseInt(String.valueOf(line.charAt(3)), 16)
						* 10
						+ Integer.parseInt(String.valueOf(line.charAt(4)), 16);
			} catch (NumberFormatException e) {
				Logger.getGlobal().warning(
						"BMSファイルの解析中の例外:チャンネル定義が無効です - " + line);
			}
			switch (channel) {
			// BGレーン
			case LANE_AUTOPLAY:
				auto.add(this.splitData(line));
				break;
			// 小節の拡大率
			case SECTION_RATE:
				int colon_index = line.indexOf(":");
				line = line.substring(colon_index + 1, line.length());
				rate = Double.valueOf(line);
				break;
			// BPM変化
			case BPM_CHANGE:
				String[] datas = this.splitData(line);
				double[] d = new double[datas.length];
				for (int j = 0; j < datas.length; j++) {
					if (!datas[j].equals("00")) {
						d[j] = (double) Integer.parseInt(datas[j], 16);
					} else {
						d[j] = 0;
					}
				}
				mergeBPMChange(d);
				break;
			// BGAレーン
			case BGA_PLAY:
				bga = this.splitData(line);
				break;
			// POORアニメーション
			case POOR_PLAY:
				poor = this.splitData(line);
				break;
			// レイヤー
			case LAYER_PLAY:
				layer = this.splitData(line);
				break;
			// BPM変化(拡張)
			case BPM_CHANGE_EXTEND:
				String[] bpmdatas = this.splitData(line);
				double[] d2 = new double[bpmdatas.length];
				for (int j = 0; j < bpmdatas.length; j++) {
					if (!bpmdatas[j].equals("00")) {
						d2[j] = model.getBPM(bpmdatas[j]);
					} else {
						d2[j] = 0;
					}
				}
				mergeBPMChange(d2);
				break;
			// ストップシーケンス
			case STOP:
				String[] stopdatas = this.splitData(line);
				stop = new double[stopdatas.length];
				for (int j = 0; j < stopdatas.length; j++) {
					if (!stopdatas[j].equals("00")) {
						stop[j] = model.getStop(stopdatas[j]);
					} else {
						stop[j] = 0;
					}
				}
				break;
			}
			// 通常ノート(1P側)
			this.convert(channel, P1_KEY_BASE, play_1, line);
			// 通常ノート(2P側)
			this.convert(channel, P2_KEY_BASE, play_2, line);
			// 不可視ノート(1P側)
			this.convert(channel, P1_INVISIBLE_KEY_BASE, play_1_invisible, line);
			// 不可視ノート(2P側)
			this.convert(channel, P2_INVISIBLE_KEY_BASE, play_2_invisible, line);
			// ロングノート(1P側)
			this.convert(channel, P1_LONG_KEY_BASE, play_1_ln, line);
			// ロングノート(2P側)
			this.convert(channel, P2_LONG_KEY_BASE, play_2_ln, line);
			// 地雷ノート(1P側)
			this.convert(channel, P1_MINE_KEY_BASE, play_1_mine, line);
			// 地雷ノート(2P側)
			this.convert(channel, P2_MINE_KEY_BASE, play_2_mine, line);
		}
		if (auto.size() == 0) {
			auto.add(new String[] { "00" });
		}
		if (model.getUseKeys() < usekeys) {
			model.setUseKeys(usekeys);
		}
	}

	private void convert(int channel, int ch, String[][] notes, String line) {
		if (ch <= channel && channel <= ch + 8) {
			channel -= ch;
			if (channel == 5 || channel == 6) {
				channel += 2;
			} else if (channel == 7 || channel == 8) {
				if (usekeys == 5 || usekeys == 10) {
					usekeys = usekeys * 7 / 5;
				}
				channel -= 2;
			}
			notes[channel] = this.mergeData(notes[channel],
					this.splitData(line));
		}
	}

	/**
	 * BPM変化のマージ処理を行う
	 * 
	 * @param b
	 *            マージするBPM変化
	 */
	private void mergeBPMChange(double[] b) {
		if (bpm_change.length == 0) {
			bpm_change = b;
		}
		int d = (bpm_change.length % b.length == 0 ? b.length : (b.length
				% bpm_change.length == 0 ? bpm_change.length : 1));

		double[] result = new double[bpm_change.length * b.length / d];
		Arrays.fill(result, 0.0);
		for (int i = 0; i < bpm_change.length; i++) {
			if (bpm_change[i] != 0.0) {
				result[i * b.length / d] = bpm_change[i];
			}
		}
		for (int i = 0; i < b.length; i++) {
			if (b[i] != 0.0) {
				result[i * bpm_change.length / d] = b[i];
			}
		}
		bpm_change = result;
	}

	/**
	 * ノーツのマージ処理を行う
	 * 
	 * @param b
	 *            マージするノーツ
	 */
	private String[] mergeData(String[] a, String[] b) {
		if (a == null || a.length == 0) {
			return b;
		}
		int d = (a.length % b.length == 0 ? b.length
				: (b.length % a.length == 0 ? a.length : 1));
		String[] result = new String[a.length * b.length / d];
		Arrays.fill(result, "00");
		for (int i = 0; i < a.length; i++) {
			if (!a[i].equals("00")) {
				result[i * b.length / d] = a[i];
			}
		}
		for (int i = 0; i < b.length; i++) {
			if (!b[i].equals("00")) {
				result[i * a.length / d] = b[i];
			}
		}
		return result;
	}

	private String[] splitData(String line) {
		line = line.substring(line.indexOf(":") + 1, line.length());
		int split = line.length() / 2;
		String[] result = new String[split];
		for (int i = 0; i < split; i++) {
			result[i] = line.substring(i * 2, i * 2 + 2);
		}
		return result;
	}

	public double getSectionRate() {
		return rate;
	}

	private double _lastbpm = 0.0;

	public double getStartBPM() {
		if (_lastbpm != 0.0) {
			return _lastbpm;
		}
		if (prev != null) {
			// 前小節の最後のBPMを返す
			double result = prev.getStartBPM();
			for (int i = 0; i < prev.bpm_change.length; i++) {
				if (prev.bpm_change[i] != 0.0) {
					result = prev.bpm_change[i];
				}
			}
			_lastbpm = result;
			return result;
		}
		// 開始時のBPMを返す
		return model.getBpm();
	}

	/**
	 * 小説開始時間(キャッシュ)
	 */
	private int _basetime = -1;

	/**
	 * 小説開始時間を取得する
	 * 
	 * @return 小説開始時間
	 */
	private int getStartTime() {
		if (_basetime != -1) {
			return _basetime;
		}
		if (prev != null) {
			int result = prev.getStartTime();

			double dt = 0.0;
			// 最終BPM取得
			double nowbpm = prev.getStartBPM();
			for (int i = 0; i < prev.bpm_change.length; i++) {
				for (int j = 0; j < prev.stop.length; j++) {
					if (((double) j / prev.stop.length >= (double) i
							/ prev.bpm_change.length)
							&& ((double) j / prev.stop.length < (double) (i + 1)
									/ prev.bpm_change.length)) {
						dt += prev.stop[j] * (1000 * 60 * 4 / nowbpm);
					}
				}
				if (prev.bpm_change[i] != 0.0) {
					nowbpm = prev.bpm_change[i];
				}
				dt += 1000 * 60 * 4 * prev.rate
						* (1.0 / prev.bpm_change.length) / nowbpm;
			}
			if (prev.bpm_change.length == 0) {
				for (int j = 0; j < prev.stop.length; j++) {
					dt += prev.stop[j] * (1000 * 60 * 4 / nowbpm);
				}
				dt += 1000 * 60 * 4 * prev.rate / nowbpm;
			}
			_basetime = (int) (result + dt);
			return _basetime;
		}
		return 0;
	}

	/**
	 * 小節開始時のLN状態(キャッシュ)
	 */
	private String[] _lnstatus = null;

	/**
	 * 小節開始時のLN状態を取得する
	 * 
	 * @return
	 */
	private String[] getStartLNStatus() {
		if (_lnstatus != null) {
			return _lnstatus;
		}
		if (prev != null) {
			_lnstatus = new String[18];
			String[] result = prev.getStartLNStatus();
			for (int i = 0; i < 9; i++) {
				String nowln = result[i];
				for (int j = 0; j < prev.play_1_ln[i].length; j++) {
					if (!prev.play_1_ln[i][j].equals("00")) {
						if (nowln == null) {
							nowln = prev.play_1_ln[i][j];
						} else if (nowln.equals(prev.play_1_ln[i][j])) {
							nowln = null;
						} else {
							Logger.getGlobal().warning(
									model.getTitle() + "はLNの対応が取れていません:"
											+ nowln + " - "
											+ prev.play_1_ln[i][j]);
							nowln = null;
						}
					}
				}
				_lnstatus[i] = nowln;

				nowln = result[i + 9];
				for (int j = 0; j < prev.play_2_ln[i].length; j++) {
					if (!prev.play_2_ln[i][j].equals("00")) {
						if (nowln == null) {
							nowln = prev.play_2_ln[i][j];
						} else if (nowln.equals(prev.play_2_ln[i][j])) {
							nowln = null;
						} else {
							Logger.getGlobal().warning(
									model.getTitle() + "はLNの対応が取れていません:"
											+ nowln + " - "
											+ prev.play_2_ln[i][j]);
							nowln = null;
						}
					}
				}
				_lnstatus[i + 9] = nowln;
			}
			return _lnstatus;
		}
		return new String[18];
	}

	/**
	 * SectionモデルからTimeLineモデルを作成し、BMSModelに登録する
	 */
	public void makeTimeLines(Map<String, Integer> wavmap,
			Map<String, Integer> bgamap) {
		int base = this.getStartTime();
		String[] startln = this.getStartLNStatus().clone();
		// 小節線追加
		model.getTimeLine(sectionnum, base).setSectionLine(true);
		model.getTimeLine(sectionnum, base).setBPM(this.getStartBPM());
		int[] poors = new int[poor.length];
		for (int i = 0; i < poors.length; i++) {
			if (bgamap.get(poor[i]) != null) {
				poors[i] = bgamap.get(poor[i]);
			} else {
				poors[i] = -1;
			}
		}
		model.getTimeLine(sectionnum, base).setPoor(poors);
		// BPM変化。ストップシーケンステーブル準備
		Map<Double, Double> bpmchange = new HashMap<Double, Double>();
		Map<Double, Double> stop = new HashMap<Double, Double>();
		List<Double> l = new ArrayList<Double>();
		for (int i = 0; i < bpm_change.length; i++) {
			if (bpm_change[i] != 0.0) {
				bpmchange.put((double) i / bpm_change.length, bpm_change[i]);
				l.add((double) i / bpm_change.length);
			}
		}
		Double[] bk = (Double[]) l.toArray(new Double[0]);
		l = new ArrayList<Double>();
		for (int i = 0; i < this.stop.length; i++) {
			if (this.stop[i] != 0.0) {
				stop.put((double) i / this.stop.length, this.stop[i]);
				l.add((double) i / this.stop.length);
			}
		}
		Double[] st = (Double[]) l.toArray(new Double[0]);
		// 通常ノート配置
		for (int key = 0; key < 74 + auto.size(); key++) {
			String[] s = null;
			if (key >= 0 && key < 9) {
				s = this.play_1[key % 9];
			}
			if (key >= 9 && key < 18) {
				s = this.play_2[key % 9];
			}
			if (key >= 18 && key < 27) {
				s = this.play_1_invisible[key % 9];
			}
			if (key >= 27 && key < 36) {
				s = this.play_2_invisible[key % 9];
			}
			if (key >= 36 && key < 45) {
				s = this.play_1_ln[key % 9];
			}
			if (key >= 45 && key < 54) {
				s = this.play_2_ln[key % 9];
			}
			if (key >= 54 && key < 63) {
				s = this.play_1_mine[key % 9];
			}
			if (key >= 63 && key < 72) {
				s = this.play_2_mine[key % 9];
			}
			if (key == 72) {
				s = this.bga;
			}
			if (key == 73) {
				s = this.layer;
			}
			if (key >= 74) {
				s = this.auto.get(key - 74);
			}
			double nowbpm = this.getStartBPM();
			double dt = 0.0;
			for (int i = 0; i < s.length; i++) {
				if (!s[i].equals("00")) {
					TimeLine tl = model.getTimeLine(sectionnum + ((float) i) / s.length, base + (int) (dt * rate));
					if (key >= 0 && key < 18) {
						if (tl.existNote(key % 18)) {
							Logger.getGlobal().warning(
									model.getTitle() + "の通常ノート追加時に衝突が発生しました。"
											+ (key + 1) + ":"
											+ (base + (int) (dt * rate)));
						}
						if (s[i].equals(model.getLNObject())) {
							// LN終端処理
							TimeLine[] tl2 = model.getAllTimeLines();
							for (int t = tl2.length - 1; t >= 0; t--) {
								if (base + (int) (dt * rate) > tl2[t].getTime()
										&& tl2[t].existNote(key % 18)) {
									Note note = tl2[t].getNote(key % 18);
									if (note instanceof NormalNote) {
										LongNote ln = new LongNote(
												note.getWav(), tl2[t]);
										ln.setEnd(tl);
										tl2[t].addNote(key % 18, ln);
										tl.addNote(key % 18, ln);
										tl.setBPM(nowbpm);
										break;
									} else if (note instanceof LongNote
											&& ((LongNote) note).getStart() == tl2[t]) {
										Logger.getGlobal()
												.warning(
														model.getTitle()
																+ "はLNレーンで開始定義し、LNオブジェクトで終端定義しています。レーン:"
																+ key
																+ " - Time(ms):"
																+ tl2[t].getTime());
										((LongNote) note).setEnd(tl);
										tl.addNote(key % 18, note);
										tl.setBPM(nowbpm);
										break;
									} else {
										Logger.getGlobal()
												.warning(
														model.getTitle()
																+ "はLNオブジェクトの対応が取れません。レーン:"
																+ key
																+ " - Time(ms):"
																+ tl2[t].getTime());
									}
								}
							}
						} else {
							tl.addNote(key % 18,
									new NormalNote(getId(s[i], wavmap)));
							tl.setBPM(nowbpm);
						}
					}
					if (key >= 18 && key < 36) {
						// Logger.getGlobal().warning(model.getTitle() +
						// "隠しノート追加"
						// + (key - 17) + ":"
						// + (base + (int) (dt * rate)));

						tl.addHiddenNote(key % 18,
								new NormalNote(getId(s[i], wavmap)));

						tl.setBPM(nowbpm);
					}
					if (key >= 36 && key < 54) {
						// LN処理
						if (startln[key % 18] == null) {
							tl.addNote(key % 18,
									new LongNote(getId(s[i], wavmap), tl));
							tl.setBPM(nowbpm);
							startln[key % 18] = s[i];
						} else {
							// LN終端処理
							TimeLine[] tl2 = model.getAllTimeLines();
							for (int t = tl2.length - 1; t >= 0; t--) {
								if (base + (int) (dt * rate) > tl2[t].getTime()
										&& tl2[t].existNote(key % 18)) {
									Note note = tl2[t].getNote(key % 18);
									if (note instanceof LongNote) {
										((LongNote) note).setEnd(tl);
										tl.addNote(key % 18, note);
										tl.setBPM(nowbpm);
										startln[key % 18] = null;
										break;
									} else {
										Logger.getGlobal().warning(
												model.getTitle()
														+ "はLN内に通常ノートが存在します!"
														+ (key - 35) + ":"
														+ tl2[t].getTime());
									}
								}
							}
						}
					}
					if (key >= 54 && key < 72) {
						// 地雷ノート処理
						if (tl.existNote(key % 18)) {
							Logger.getGlobal().warning(
									model.getTitle() + "の地雷ノート追加時に衝突が発生しました。"
											+ (key + 1) + ":"
											+ (base + (int) (dt * rate)));
						}
						tl.addNote(key % 18, new MineNote());
						tl.setBPM(nowbpm);
					}
					if (key == 72) {
						tl.setBGA(getId(s[i], bgamap));
						tl.setBPM(nowbpm);
					}
					if (key == 73) {
						tl.setLayer(getId(s[i], bgamap));
						tl.setBPM(nowbpm);
					}
					if (key >= 74) {
						tl.addBackGroundNote(new NormalNote(getId(s[i], wavmap)));
						tl.setBPM(nowbpm);
					}
				}
				// BPM変化,ストップを考慮したタイム加算
				double se = 0.0;
				for (int j = 0; j < bk.length; j++) {
					// タイムラインにbpm変化を反映
					if (bk[j] >= (double) i / s.length
							&& bk[j] < (double) (i + 1) / s.length) {
						for (int k = 0; k < st.length; k++) {
							// ストップ
							if (st[k] >= (double) i / s.length + se
									&& st[k] < bk[j]) {
								dt += 1000 * 60 * 4
										* (st[k] - (double) i / s.length - se)
										/ nowbpm;
								se = st[k] - (double) i / s.length;
								model.getTimeLine(sectionnum + st[k].floatValue(), base + (int) (dt * rate))
								.setBPM(nowbpm);
								model.getTimeLine(sectionnum + st[k].floatValue(), base + (int) (dt * rate))
								.setStop((int) (stop.get(st[k])
										* (1000 * 60 * 4 / nowbpm)));
								// System.out
								// .println("STOP (BPM変化中) : "
								// + (stop.get(st[k]) * (1000 * 60 * 4 /
								// nowbpm))
								// + " - bpm " + nowbpm
								// + " - key - " + key);
								dt += stop.get(st[k])
										* (1000 * 60 * 4 / nowbpm) / rate;
							}
						}
						dt += 1000 * 60 * 4
								* (bk[j] - (double) i / s.length - se) / nowbpm;
						se = bk[j] - (double) i / s.length;
						nowbpm = bpmchange.get(bk[j]);
						// if (model.getTimeLine(base + (int) (dt * rate))
						// .getBPM() != nowbpm) {
						// System.out.println("登録するBPMが異なる可能性があります。Time " + (
						// base + (int) (dt * rate)) + " section : "
						// + (sectionnum + bk[j].floatValue()) + " BPM : " +
						// model.getTimeLine(
						// base + (int) (dt * rate)).getBPM()
						// + " → " + nowbpm);
						// }
						model.getTimeLine(sectionnum + bk[j].floatValue(), base + (int) (dt * rate)).setBPM(
								nowbpm);
						// Logger.getGlobal().info(
						// "BPM変化:" + nowbpm + "  time:"
						// + (base + (int) (dt * rate)));
					}
				}
				for (int k = 0; k < st.length; k++) {
					// ストップ
					if (st[k] >= (double) i / s.length + se
							&& st[k] < (double) (i + 1) / s.length) {
						dt += 1000 * 60 * 4
								* (st[k] - (double) i / s.length - se) / nowbpm;
						se = st[k] - (double) i / s.length;
						model.getTimeLine(sectionnum + st[k].floatValue(), base + (int) (dt * rate)).setBPM(
								nowbpm);
						model.getTimeLine(sectionnum + st[k].floatValue(), base + (int) (dt * rate))
						.setStop((int) (stop.get(st[k])
								* (1000 * 60 * 4 / nowbpm)));
						// System.out.println("STOP : "
						// + (stop.get(st[k]) * (1000 * 60 * 4 / nowbpm))
						// + " - bpm " + nowbpm + " - key - " + key);
						dt += stop.get(st[k]) * (1000 * 60 * 4 / nowbpm) / rate;
						// if (model.getTimeLine(base + (int) (dt * rate))
						// .getBPM() != nowbpm) {
						// System.out.println("登録するBPMが異なる可能性があります。Time " + (
						// base + (int) (dt * rate)) + " section : "
						// + (sectionnum + st[k].floatValue()) + " BPM : " +
						// model.getTimeLine(
						// base + (int) (dt * rate)).getBPM()
						// + " → " + nowbpm);
						// }
					}
				}
				double dd = 1000 * 60 * 4 * (1.0 / s.length - se) / nowbpm;
				// if (dd * rate < 1.0) {
				// Logger.getGlobal().warning(
				// "時間軸:" + (base + (int) (dt * rate))
				// + "において時間加算が1ms以下で、TimeLine衝突発生");
				// //dt += 1.0 / rate;
				// }
				dt += dd;
			}
		}
	}

	private int getId(String s, Map<String, Integer> map) {
		if (map.containsKey(s)) {
			return map.get(s);
		}
		return -1;
	}
}
