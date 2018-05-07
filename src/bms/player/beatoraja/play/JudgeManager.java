package bms.player.beatoraja.play;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;

import com.badlogic.gdx.utils.FloatArray;

import bms.model.BMSModel;
import bms.model.Lane;
import bms.model.LongNote;
import bms.model.MineNote;
import bms.model.Mode;
import bms.model.NormalNote;
import bms.model.Note;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.play.JudgeProperty.MissCondition;
import bms.player.beatoraja.skin.SkinPropertyMapper;

/**
 * �깕�꺖�깂�닩若싩�←릤�뵪�궚�꺀�궧
 *
 * @author exch
 */
public class JudgeManager {

	// TODO HCN�듉�걮�쎍�걮�겗�쇇�윹�겘�겑�걝�걲�굥竊�

	private final BMSPlayer main;
	/**
	 * LN type
	 */
	private int lntype;
	private Lane[] lanes;

	/**
	 * �뤎�쑉�겗�닩若싥궖�궑�꺍�깉�냵鼇�
	 */
	private IRScoreData score = new IRScoreData();

	/**
	 * �뤎�쑉�겗�궠�꺍�깭�빊
	 */
	private int combo;
	/**
	 * �궠�꺖�궧�셽�겗�뤎�쑉�겗�궠�꺍�깭�빊
	 */
	private int coursecombo;
	/**
	 * �궠�꺖�궧�셽�겗��鸚㎯궠�꺍�깭�빊
	 */
	private int coursemaxcombo;
	/**
	 * �닩若싮�ｅ땿�꺃�꺖�궣�꺖�겗�돯
	 */
	private int[][] judge;
	/**
	 * �뤎�쑉烏①ㅊ訝��겗�닩若�
	 */
	private int[] judgenow;
	private int[] judgecombo;
	/**
	 * �닩若싧량�셽�뼋(ms , +�겘�뿩�듉�걮�겎-�겘�걛�듉�걮)
	 */
	private long[] judgefast;
	/**
	 * �눇�릤訝��겗LN
	 */
	private LongNote[] processing;
	/**
	 * �싮걥訝��겗HCN
	 */
	private LongNote[] passing;
	/**
	 * HCN罌쀥뒥�닩若�
	 */
	private boolean[] inclease = new boolean[8];
	private boolean[] next_inclease = new boolean[8];
	private int[] passingcount;

	private int[] keyassign;

	private int[] sckeyassign;
	private int[] sckey;
	private int[] offset;
	private int[] player;
	private int[][] laneassign;
	/**
	 * HCN�겗罌쀦툤�뼋�슂(ms)
	 */
	private static final int hcnduration = 200;
	/**
	 * �깕�꺖�깂�닩若싥깇�꺖�깣�꺂
	 */
	private int[][] njudge;
	private long judgestart;
	private long judgeend;
	/**
	 * CN永귞ク�닩若싥깇�꺖�깣�꺂
	 */
	private int[][] cnendjudge;
	/**
	 * �궧�궚�꺀�긿�긽�닩若싥깇�꺖�깣�꺂
	 */
	private int[][] sjudge;
	private int[][] scnendjudge;
	/**
	 * PMS�뵪�닩若싥궥�궧�깇�깲(令튡OOR�겎�궠�꺍�깭�궖�긿�깉��1�깕�꺖�깂�겓�겇�걤1令튡OOR�겲�겎)�겗�쐣�듅/�꽒�듅
	 */
	private boolean[] combocond;

	private MissCondition miss;
	/**
	 * �릢�닩若싨칿�겗�깕�꺖�깉�겗�닩若싥굮易덂ㅁ�걲�굥�걢�겑�걝�걢�괦G, GR, GD, BD, PR, MS�겗�젂
	 */
	private boolean[] judgeVanish;

	private long prevtime;

	private boolean autoplay = false;
	private long[] auto_presstime;
	/**
	 * �궕�꺖�깉�깤�꺃�궎�겎�궘�꺖�굮�듉訝뗣걲�굥��弱뤸셽�뼋(ms)
	 */
	private final int auto_minduration = 80;

	private final JudgeAlgorithm algorithm;

	/**
	 * �눇�릤歷덀깕�꺖�깉�빊
	 */
	private int pastNotes = 0;

	/**
	 * PMS �궘�깵�꺀�뵪 �닩若�
	 */
	private int PMcharaJudge = 0;

	/**
	 * �쎍瓦�100�깕�꺖�깂�겗�닩若싧량�셽�뼋
	 */
	private long[] recentJudges = new long[100];
	/**
	 * �닩若싧량�셽�뼋�겗�깦�긿�깋
	 */
	private int recentJudgesIndex = 0;

	public JudgeManager(BMSPlayer main) {
		this.main = main;
		algorithm = JudgeAlgorithm.valueOf(main.main.getPlayerResource().getConfig().getJudgeType());
	}

	public void init(BMSModel model, PlayerResource resource) {
		prevtime = 0;
		judgenow = new int[((PlaySkin) main.getSkin()).getJudgeregion()];
		judgecombo = new int[((PlaySkin) main.getSkin()).getJudgeregion()];
		judgefast = new long[((PlaySkin) main.getSkin()).getJudgeregion()];
		score = new IRScoreData(BMSPlayerRule.isSevenToNine() ? Mode.BEAT_7K : model.getMode());
		score.setNotes(model.getTotalNotes());
		score.setSha256(model.getSHA256());

		this.lntype = model.getLntype();
		lanes = model.getLanes();

		JudgeProperty rule = BMSPlayerRule.getBMSPlayerRule(model.getMode()).judge;
		combocond = rule.combo;
		miss = rule.miss;
		judgeVanish = rule.judgeVanish;
		pastNotes = 0;
		PMcharaJudge = 0;

		keyassign = main.getLaneProperty().getKeyLaneAssign();
		offset = main.getLaneProperty().getLaneSkinOffset();
		player = main.getLaneProperty().getLanePlayer();
		sckeyassign = main.getLaneProperty().getLaneScratchAssign();
		laneassign = main.getLaneProperty().getLaneKeyAssign();
		sckey = new int[model.getMode().scratchKey.length];

		judge = new int[model.getMode().player][model.getMode().key / model.getMode().player + 1];
		processing = new LongNote[sckeyassign.length];
		passing = new LongNote[sckeyassign.length];
		passingcount = new int[sckeyassign.length];
		inclease = new boolean[sckeyassign.length];
		next_inclease = new boolean[sckeyassign.length];
		auto_presstime = new long[keyassign.length];

		for (int key = 0; key < keyassign.length; key++) {
			auto_presstime[key] = Long.MIN_VALUE;
		}

		final int judgerank = model.getJudgerank();
		final int judgeWindowRate = resource.getPlayerConfig().getJudgewindowrate();
		int constraint = 2;
		for (CourseData.CourseDataConstraint mode : resource.getConstraint()) {
			if (mode == CourseData.CourseDataConstraint.NO_GREAT) {
				constraint = 0;
			} else if (mode == CourseData.CourseDataConstraint.NO_GOOD) {
				constraint = 1;
			}
		}
		njudge = rule.getNoteJudge(judgerank, judgeWindowRate, constraint,
				model.getMode() == Mode.POPN_9K && !BMSPlayerRule.isSevenToNine());
		cnendjudge = rule.getLongNoteEndJudge(judgerank, judgeWindowRate, constraint,
				model.getMode() == Mode.POPN_9K && !BMSPlayerRule.isSevenToNine());
		sjudge = rule.getScratchJudge(judgerank, judgeWindowRate, constraint);
		scnendjudge = rule.getLongScratchEndJudge(judgerank, judgeWindowRate, constraint);
		judgestart = judgeend = 0;
		for (int[] i : njudge) {
			judgestart = Math.min(judgestart, i[0]);
			judgeend = Math.max(judgeend, i[1]);
		}
		for (int[] i : sjudge) {
			judgestart = Math.min(judgestart, i[0]);
			judgeend = Math.max(judgeend, i[1]);
		}

		this.autoplay = resource.getPlayMode().isAutoPlayMode();

		FloatArray[] f = resource.getGauge();
		if (f != null) {
			setCourseCombo(resource.getCombo());
			setCourseMaxcombo(resource.getMaxcombo());
		}

		Arrays.fill(recentJudges, Long.MIN_VALUE);
		this.recentJudgesIndex = 0;
	}

	
	public void update(final long time) {
		final MainController mc = main.main;
		final BMSPlayerInputProcessor input = mc.getInputProcessor();
		final Config config = mc.getPlayerResource().getConfig();
		final PlayerConfig playerConfig = mc.getPlayerResource().getPlayerConfig();
		final long now = mc.getNowTime();
		// �싮걥楹삠겗�닩若�
		Arrays.fill(next_inclease, false);

		for (int lane = 0; lane < laneassign.length; lane++) {
			final Lane lanemodel = lanes[lane];
			lanemodel.mark((int) (prevtime + judgestart - 100));
			boolean pressed = false;
			for (int key : laneassign[lane]) {
				if (input.getKeyState(key)) {
					pressed = true;
					break;
				}
			}
			for (Note note = lanemodel.getNote(); note != null && note.getTime() <= time; note = lanemodel.getNote()) {
				if (note.getTime() <= prevtime) {
					continue;
				}
				if (note instanceof LongNote) {
					// HCN�닩若�
					final LongNote lnote = (LongNote) note;
					if ((lnote.getType() == LongNote.TYPE_UNDEFINED && lntype == BMSModel.LNTYPE_HELLCHARGENOTE)
							|| lnote.getType() == LongNote.TYPE_HELLCHARGENOTE) {
						if (lnote.isEnd()) {
							passing[lane] = null;
							passingcount[lane] = 0;
						} else {
							passing[lane] = lnote;
						}
					}
				} else if (note instanceof MineNote && pressed) {
					final MineNote mnote = (MineNote) note;
					// �쑑�쎐�깕�꺖�깉�닩若�
					main.getGauge().addValue(-mnote.getDamage());
					System.out.println("Mine Damage : " + mnote.getWav());
				}

				if (autoplay) {
					// �걪�걪�겓�궕�꺖�깉�깤�꺃�궎�눇�릤�굮�뀯�굦�굥
					if (note instanceof NormalNote && note.getState() == 0) {
						auto_presstime[laneassign[lane][0]] = now;
						main.play(note, config.getKeyvolume(), 0);
						this.update(lane, note, time, 0, 0);
						if (playerConfig.isGuideSE()) {
							main.play(main.SOUND_GUIDE_SE_PG);
						}
					}
					if (note instanceof LongNote) {
						final LongNote ln = (LongNote) note;
						if (!ln.isEnd() && ln.getState() == 0 && processing[lane] == null) {
							auto_presstime[laneassign[lane][0]] = now;
							main.play(note, config.getKeyvolume(), 0);
							if ((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
									|| ln.getType() == LongNote.TYPE_LONGNOTE) {
								passingcount[lane] = 0;
								//LN�셽�겗�꺃�꺖�궣�꺖�돯鸚됪쎍�눇�릤
								this.judge[player[lane]][offset[lane]] = 8;
							} else {
								this.update(lane, ln, time, 0, 0);
							}
							processing[lane] = ln.getPair();
							if (playerConfig.isGuideSE()) {
								main.play(main.SOUND_GUIDE_SE_PG);
							}
						}
						if (ln.isEnd() && ln.getState() == 0) {
							if ((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
									|| ln.getType() == LongNote.TYPE_CHARGENOTE
									|| ln.getType() == LongNote.TYPE_HELLCHARGENOTE) {
								if (sckeyassign[lane] >= 0 && laneassign[lane].length >= 2) {
									auto_presstime[laneassign[lane][0]] = Long.MIN_VALUE;
									auto_presstime[laneassign[lane][1]] = now;
								}
								this.update(lane, ln, time, 0, 0);
								main.play(processing[lane], config.getKeyvolume(), 0);
								processing[lane] = null;
								if (playerConfig.isGuideSE() && sckeyassign[lane] != -1) {
									main.play(main.SOUND_GUIDE_SE_PG);
								}
							}
						}
					}
				}
			}
			// HCN�궟�꺖�궦罌쀦툤�닩若�
			if (passing[lane] != null
					&& (pressed || (passing[lane].getPair().getState() > 0 && passing[lane].getPair().getState() <= 3)
							|| autoplay)) {
				next_inclease[lane] = true;
			}

			if (autoplay) {
				for (int key : laneassign[lane]) {
					if (auto_presstime[key] != Long.MIN_VALUE && now - auto_presstime[key] > auto_minduration
							&& processing[lane] == null) {
						auto_presstime[key] = Long.MIN_VALUE;
					}
				}
			}
		}

		final boolean[] b = inclease;
		inclease = next_inclease;
		next_inclease = b;

		for (int lane = 0; lane < passing.length; lane++) {
			final int offset = main.getLaneProperty().getLaneSkinOffset()[lane];
			final int timerActive = SkinPropertyMapper.hcnActiveTimerId(main.getLaneProperty().getLanePlayer()[lane],
					offset);
			final int timerDamage = SkinPropertyMapper.hcnDamageTimerId(main.getLaneProperty().getLanePlayer()[lane],
					offset);

			if (passing[lane] == null || passing[lane].getState() == 0) {
				mc.setTimerOff(timerActive);
				mc.setTimerOff(timerDamage);
				continue;
			}

			if (inclease[lane]) {
				passingcount[lane] += (time - prevtime);
				if (passingcount[lane] > hcnduration) {
					main.getGauge().update(1, 0.5f);
					// System.out.println("HCN : Gauge increase");
					passingcount[lane] -= hcnduration;
				}
				mc.switchTimer(timerActive, true);
				mc.setTimerOff(timerDamage);
			} else {
				passingcount[lane] -= (time - prevtime);
				if (passingcount[lane] < -hcnduration) {
					main.getGauge().update(3, 0.5f);
					// System.out.println("HCN : Gauge decrease");
					passingcount[lane] += hcnduration;
				}
				mc.setTimerOff(timerActive);
				mc.switchTimer(timerDamage, true);
			}
		}
		prevtime = time;

		for (int key = 0; key < keyassign.length; key++) {
			final int lane = keyassign[key];
			if (lane == -1) {
				continue;
			}
			final long ptime = input.getKeyTime(key);
			if (ptime == 0) {
				continue;
			}
			final Lane lanemodel = lanes[lane];
			lanemodel.reset();
			final int sc = sckeyassign[lane];
			if (input.getKeyState(key)) {
				// �궘�꺖�걣�듉�걬�굦�걼�겏�걤�겗�눇�릤
				if (processing[lane] != null) {
					// BSS永귞ク�눇�릤
					if (((lntype != BMSModel.LNTYPE_LONGNOTE && processing[lane].getType() == LongNote.TYPE_UNDEFINED)
							|| processing[lane].getType() == LongNote.TYPE_CHARGENOTE
							|| processing[lane].getType() == LongNote.TYPE_HELLCHARGENOTE) && sc >= 0
							&& key != sckey[sc]) {
						final int[][] judge = scnendjudge;
						final int dtime = (int) (processing[lane].getTime() - ptime);
						int j = 0;
						for (; j < judge.length && !(dtime >= judge[j][0] && dtime <= judge[j][1]); j++)
							;

						this.update(lane, processing[lane], time, j, dtime);
						//						 System.out.println("BSS永귞ク�닩若� - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane].hashCode());
						main.play(processing[lane], config.getKeyvolume(), 0);
						processing[lane] = null;
						sckey[sc] = 0;
						if (playerConfig.isGuideSE()) {
							if (j == 0)
								main.play(main.SOUND_GUIDE_SE_PG);
							else if (j == 1)
								main.play(main.SOUND_GUIDE_SE_GR);
							else if (j == 2)
								main.play(main.SOUND_GUIDE_SE_GD);
						}
					} else {
						// �걪�걪�겓�씎�굥�겗�겘�깯�꺂�긽�궘�꺖�궋�궢�궎�꺍餓ε쨼�걗�굤�걟�겒�걚�겘�걳
					}
				} else {
					final int[][] judge = sc >= 0 ? sjudge : njudge;
					// 野얕괌�깕�꺖�깂�겗�듊�눣
					lanemodel.reset();
					Note tnote = null;
					int j = 0;
					for (Note judgenote = lanemodel.getNote(); judgenote != null; judgenote = lanemodel.getNote()) {
						final long dtime = judgenote.getTime() - ptime;
						if (dtime >= judgeend) {
							break;
						}
						if (dtime < judgestart) {
							continue;
						}
						if (judgenote instanceof MineNote || (judgenote instanceof LongNote
								&& ((LongNote) judgenote).isEnd())) {
							continue;
						}
						if (tnote == null || tnote.getState() != 0
								|| algorithm.compare(tnote, judgenote, ptime, judge)) {
							if (!(miss == MissCondition.ONE && (judgenote.getState() != 0
									|| (judgenote.getState() == 0 && judgenote.getPlayTime() != 0
											&& (dtime > judge[2][1] || dtime < judge[2][0]))))) {
								if (judgenote.getState() != 0) {
									j = (dtime >= judge[4][0] && dtime <= judge[4][1]) ? 5 : 6;
								} else {
									for (j = 0; j < judge.length
											&& !(dtime >= judge[j][0] && dtime <= judge[j][1]); j++) {
									}
									j = (j >= 4 ? j + 1 : j);
								}
								if (j < 6 && (j < 4 || tnote == null
										|| Math.abs(tnote.getTime() - ptime) > Math.abs(judgenote.getTime() - ptime))) {
									tnote = judgenote;
								}
							}
						}
					}

					if (tnote != null) {
						// TODO �걪�겗�셽�궧�겎令튡OOR�눇�릤�굮�늽略먦걬�걵�굥�겧�걤�걢
						if (tnote instanceof LongNote) {
							// �꺆�꺍�궛�깕�꺖�깉�눇�릤
							final LongNote ln = (LongNote) tnote;
							main.play(tnote, config.getKeyvolume(), 0);
							if (((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
									|| ln.getType() == LongNote.TYPE_LONGNOTE)
									&& j < 4) {
								passingcount[lane] = (int) (tnote.getTime() - ptime);
								//LN�셽�겗�꺃�꺖�궣�꺖�돯鸚됪쎍�눇�릤
								this.judge[player[lane]][offset[lane]] = 8;
							} else {
								final int dtime = (int) (tnote.getTime() - ptime);
								this.update(lane, ln, time, j, dtime);
							}
							if (j < 4) {
								processing[lane] = ln.getPair();
								if (sc >= 0) {
									// BSS�눇�릤�뼀冶�
									//									 System.out.println("BSS�뼀冶뗥닩若� - Time : " + ptime + " Judge : " + j + " KEY : " + key + " LN : " + ln.getPair().hashCode());
									sckey[sc] = key;
								}
							}
						} else {
							main.play(tnote, config.getKeyvolume(), 0);
							// �싧만�깕�꺖�깉�눇�릤
							final int dtime = (int) (tnote.getTime() - ptime);
							this.update(lane, tnote, time, j, dtime);
						}
						if (playerConfig.isGuideSE()) {
							if (j == 0)
								main.play(main.SOUND_GUIDE_SE_PG);
							else if (j == 1)
								main.play(main.SOUND_GUIDE_SE_GR);
							else if (j == 2)
								main.play(main.SOUND_GUIDE_SE_GD);
						}
					} else {
						// 令튡OOR�닩若싥걣�겒�걚�겏�걤�겗�꺃�꺖�궣�꺖�돯鸚됪쎍�눇�릤
						this.judge[player[lane]][offset[lane]] = 0;

						// 令튡OOR�닩若싥걣�겒�걚�겏�걤�겗�궘�꺖�윹�눇�릤
						final Note[] notes = lanemodel.getNotes();
						Note n = notes.length > 0 ? notes[0] : null;
						for (Note note : lanemodel.getHiddens()) {
							if (note.getTime() >= ptime) {
								break;
							}
							n = note;
						}

						for (Note note : notes) {
							if (note.getTime() >= ptime) {
								break;
							}
							if ((n == null || n.getTime() <= note.getTime())
									&& !(note instanceof LongNote && note.getState() != 0)) {
								n = note;
							}
						}

						if (n != null) {
							main.play(n, config.getKeyvolume(), 0);
						}
					}
				}
				main.getKeyinput().inputKeyOn(lane);
			} else {
				// �궘�꺖�걣�썴�걬�굦�걼�겏�걤�겗�눇�릤
				if (processing[lane] != null) {
					final int[][] judge = sc >= 0 ? scnendjudge : cnendjudge;
					int dtime = (int) (processing[lane].getTime() - ptime);
					int j = 0;
					for (; j < judge.length && !(dtime >= judge[j][0] && dtime <= judge[j][1]); j++)
						;

					if ((lntype != BMSModel.LNTYPE_LONGNOTE
							&& processing[lane].getType() == LongNote.TYPE_UNDEFINED)
							|| processing[lane].getType() == LongNote.TYPE_CHARGENOTE
							|| processing[lane].getType() == LongNote.TYPE_HELLCHARGENOTE) {
						// CN, HCN�썴�걮�눇�릤
						boolean release = true;
						if (sc >= 0) {
							if (j != 4 || key != sckey[sc]) {
								release = false;
							} else {
								//								 System.out.println("BSS�붶릎�썴�걮�닩若� - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane]);
								sckey[sc] = 0;
							}
						}
						if (release) {
							if (j >= 3) {
								main.stop(processing[lane].getPair());
							}
							this.update(lane, processing[lane], time, j, dtime);
							main.play(processing[lane], config.getKeyvolume(), 0);
							processing[lane] = null;
						}
					} else {
						// LN�썴�걮�눇�릤
						if (Math.abs(passingcount[lane]) > Math.abs(dtime)) {
							dtime = passingcount[lane];
							for (; j < judge.length && !(dtime >= judge[j][0] && dtime <= judge[j][1]); j++)
								;
						}
						if (j >= 3) {
							main.stop(processing[lane].getPair());
						}
						this.update(lane, processing[lane].getPair(), time, j, dtime);
						main.play(processing[lane], config.getKeyvolume(), 0);
						processing[lane] = null;
					}
				}
			}
			input.resetCursorTime(key);
		}

		for (int lane = 0; lane < sckeyassign.length; lane++) {
			final int sc = sckeyassign[lane];
			final int[][] judge = sc >= 0 ? sjudge : njudge;

			// LN永귞ク�닩若�
			if (processing[lane] != null
					&& ((lntype == BMSModel.LNTYPE_LONGNOTE && processing[lane].getType() == LongNote.TYPE_UNDEFINED)
							|| processing[lane].getType() == LongNote.TYPE_LONGNOTE)
					&& processing[lane].getTime() < time) {
				int j = 0;
				for (; j < judge.length; j++) {
					if (passingcount[lane] >= judge[j][0] && passingcount[lane] <= judge[j][1]) {
						break;
					}
				}
				this.update(lane, processing[lane].getPair(), time, j, passingcount[lane]);
				main.play(processing[lane], config.getKeyvolume(), 0);
				processing[lane] = null;
			}
			// 誤뗩�껁걮POOR�닩若�
			final Lane lanemodel = lanes[lane];
			lanemodel.reset();
			for (Note note = lanemodel.getNote(); note != null
					&& note.getTime() < time + judge[3][0]; note = lanemodel.getNote()) {
				final long jud = note.getTime() - time;
				if (note instanceof NormalNote && note.getState() == 0) {
					this.update(lane, note, time, 4, jud);
				} else if (note instanceof LongNote) {
					final LongNote ln = (LongNote) note;
					if (!ln.isEnd() && note.getState() == 0) {
						if ((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
								|| ln.getType() == LongNote.TYPE_CHARGENOTE
								|| ln.getType() == LongNote.TYPE_HELLCHARGENOTE) {
							// System.out.println("CN start poor");
							this.update(lane, note, time, 4, jud);
							this.update(lane, ((LongNote) note).getPair(), time, 4, jud);
						}
						if (((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
								|| ln.getType() == LongNote.TYPE_LONGNOTE) && processing[lane] != ln.getPair()) {
							// System.out.println("LN start poor");
							this.update(lane, note, time, 4, jud);
						}

					}
					if (((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
							|| ln.getType() == LongNote.TYPE_CHARGENOTE || ln.getType() == LongNote.TYPE_HELLCHARGENOTE)
							&& ((LongNote) note).isEnd() && ((LongNote) note).getState() == 0) {
						// System.out.println("CN end poor");
						this.update(lane, ((LongNote) note), time, 4, jud);
						processing[lane] = null;
						if (sc >= 0) {
							sckey[sc] = 0;
						}
					}
				}
			}
			// LN�눇�릤�궭�궎�깯�꺖
			// TODO processing�ㅳ겗鸚됧뙑�겗�겏�걤�겗�겳若잒죱�걮�걼�걚
			// TODO HCN�겘�닪�궭�궎�깯�꺖�겓�걲�굥�걢�굚
			mc.switchTimer(SkinPropertyMapper.holdTimerId(player[lane], offset[lane]),
					processing[lane] != null || (passing[lane] != null && inclease[lane]));
		}
	}

	private final int[] JUDGE_TIMER = { TIMER_JUDGE_1P, TIMER_JUDGE_2P, TIMER_JUDGE_3P };
	private final int[] COMBO_TIMER = { TIMER_COMBO_1P, TIMER_COMBO_2P, TIMER_COMBO_3P };

	private void update(int lane, Note n, long time, int judge, long fast) {
		if (judgeVanish[judge]) {
			n.setState(judge + 1);
			pastNotes++;
		}
		if (miss == MissCondition.ONE && judge == 4 && n.getPlayTime() != 0) {
			main.setPastNotes(pastNotes);
			return;
		}
		n.setPlayTime(fast);
		score.addJudgeCount(judge, fast >= 0, 1);

		if (judge < 4) {
			if (recentJudgesIndex == recentJudges.length - 1) {
				recentJudgesIndex = 0;
			} else {
				recentJudgesIndex++;
			}
			recentJudges[recentJudgesIndex] = fast;
		}

		if (combocond[judge] && judge < 5) {
			combo++;
			score.setCombo(Math.max(score.getCombo(), combo));
			coursecombo++;
			coursemaxcombo = coursemaxcombo > coursecombo ? coursemaxcombo : coursecombo;
		}
		if (!combocond[judge]) {
			combo = 0;
			coursecombo = 0;
		}

		if (judge != 4)
			this.judge[player[lane]][offset[lane]] = judge == 0 ? 1 : judge * 2 + (fast > 0 ? 0 : 1);
		if (judge <= ((PlaySkin) main.getSkin()).getJudgetimer()) {
			main.main.setTimerOn(SkinPropertyMapper.bombTimerId(player[lane], offset[lane]));
		}
		PMcharaJudge = judge + 1;

		final int lanelength = sckeyassign.length;
		if (judgenow.length > 0) {
			main.main.setTimerOn(JUDGE_TIMER[lane / (lanelength / judgenow.length)]);
			if (judgenow.length >= 3) {
				for (int i = 0; i < COMBO_TIMER.length; i++) {
					if (i != lane / (lanelength / judgenow.length))
						main.main.setTimerOff(COMBO_TIMER[i]);
				}
			}
			main.main.setTimerOn(COMBO_TIMER[lane / (lanelength / judgenow.length)]);
			judgenow[lane / (lanelength / judgenow.length)] = judge + 1;
			judgecombo[lane / (lanelength / judgenow.length)] = main.getJudgeManager().getCourseCombo();
			judgefast[lane / (lanelength / judgenow.length)] = fast;
		}
		main.update(judge, time);
	}

	public long[] getRecentJudges() {
		return recentJudges;
	}

	public int getRecentJudgesIndex() {
		return recentJudgesIndex;
	}

	public long[] getRecentJudgeTiming() {
		return judgefast;
	}

	public LongNote[] getProcessingLongNotes() {
		return processing;
	}

	public LongNote[] getPassingLongNotes() {
		return passing;
	}

	public boolean[] getHellChargeJudges() {
		return inclease;
	}

	public long[] getAutoPresstime() {
		return auto_presstime;
	}

	/**
	 * �뤎�쑉�겗1�쎊�냵�겗�궠�꺍�깭�빊�굮�룚孃쀣걲�굥
	 *
	 * @return �뤎�쑉�겗�궠�꺍�깭�빊
	 */
	public int getCombo() {
		return combo;
	}

	/**
	 * �뤎�쑉�겗�궠�꺖�궧�냵�겗�궠�꺍�깭�빊�굮�룚孃쀣걲�굥
	 *
	 * @return �뤎�쑉�겗�궠�꺍�깭�빊
	 */
	public int getCourseCombo() {
		return coursecombo;
	}

	public void setCourseCombo(int combo) {
		this.coursecombo = combo;
	}

	public int getCourseMaxcombo() {
		return coursemaxcombo;
	}

	public void setCourseMaxcombo(int combo) {
		this.coursemaxcombo = combo;
	}

	public int[][] getJudgeTimeRegion(int lane) {
		return sckeyassign[lane] >= 0 ? sjudge : njudge;
	}

	public IRScoreData getScoreData() {
		return score;
	}

	/**
	 * �뙁若싥겗�닩若싥겗�궖�궑�꺍�깉�빊�굮瓦붵걲
	 *
	 * @param judge
	 *            0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
	 * @return �닩若싥겗�궖�궑�꺍�깉�빊
	 */
	public int getJudgeCount(int judge) {
		return score.getJudgeCount(judge);
	}

	/**
	 * �뙁若싥겗�닩若싥겗�궖�궑�꺍�깉�빊�굮瓦붵걲
	 *
	 * @param judge
	 *            0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
	 * @param fast
	 *            true:FAST, flase:SLOW
	 * @return �닩若싥겗�궖�궑�꺍�깉�빊
	 */
	public int getJudgeCount(int judge, boolean fast) {
		return score.getJudgeCount(judge, fast);
	}

	public int getJudge(int timerId) {
		int player = SkinPropertyMapper.getKeyJudgeValuePlayer(timerId);
		int offset = SkinPropertyMapper.getKeyJudgeValueOffset(timerId);
		if (player >= judge.length || offset >= judge[player].length)
			return -1;
		return judge[player][offset];
	}

	public int[] getNowJudge() {
		return judgenow;
	}

	public int[] getNowCombo() {
		return judgecombo;
	}

	public int[][] getJudgeTable(boolean sc) {
		return sc ? sjudge : njudge;
	}

	public int getPastNotes() {
		return pastNotes;
	}

	public int getPMcharaJudge() {
		return PMcharaJudge;
	}
}
