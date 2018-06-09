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
import bms.player.beatoraja.input.keyData;
import bms.player.beatoraja.play.JudgeProperty.MissCondition;
import bms.player.beatoraja.skin.SkinPropertyMapper;

/**
 * �뜝�럡�돺�뜝�럡�떖�뜝�럡�돦�뜝�럥�뼋�븨�똻�뼋�뜝�뜦���뵳�끏�삕�얇굩�삕亦낆떣�삕�댆占썲뜝�럡�븣
 *
 * @author exch
 */
public class JudgeManager {

	// TODO HCN�뜝�럥諭ｅ뜝�럡愿쇔뜝�럩�윫�뜝�럡愿쇔뜝�럡荑곩뜝�럩�뇼�뜝�럩�몱�뜝�럡荑귛뜝�럡苡삣뜝�럡肄ⓨ뜝�럡援됧뜝�럡�뜲櫻뗫뵃�삕

	private final BMSPlayer main;
	/**
	 * LN type
	 */
	private int lntype;
	private Lane[] lanes;

	/**
	 * �뜝�럥夷닷뜝�럩紐쎾뜝�럡荑곩뜝�럥�뼋�븨�똻�뼅亦낅슁�삕亦낅쵓�삕�댆�엪�삕繹먮맮�삕占쎄틣佯몃돍�삕
	 */
	private IRScoreData score = new IRScoreData();

	/**
	 * �뜝�럥夷닷뜝�럩紐쎾뜝�럡荑곩뜝�럡�뀷�뜝�럡�떋�뜝�럡臾뜹뜝�럥�돯
	 */
	private int combo;
	/**
	 * �뜝�럡�뀷�뜝�럡�떖�뜝�럡�븣�뜝�럩�걢�뜝�럡荑곩뜝�럥夷닷뜝�럩紐쎾뜝�럡荑곩뜝�럡�뀷�뜝�럡�떋�뜝�럡臾뜹뜝�럥�돯
	 */
	private int coursecombo;
	/**
	 * �뜝�럡�뀷�뜝�럡�떖�뜝�럡�븣�뜝�럩�걢�뜝�럡荑곩뜝�룞�삕�땱�떏�젦亦낆쥜�삕�댆�엪�삕繹먲옙�뜝�럥�돯
	 */
	private int coursemaxcombo;
	/**
	 * �뜝�럥�뼋�븨�똻�뼐�뜝�룞�맀占쎈묍�뜝�럡�땿�뜝�럡�떖�뜝�럡�븗�뜝�럡�떖�뜝�럡荑곩뜝�럥猷�
	 */
	private int[][] judge;
	/**
	 * �뜝�럥夷닷뜝�럩紐쏉옙源쀯옙紐븝옙�뤻펶�빢�삕�뜝�럡荑곩뜝�럥�뼋�븨�뙋�삕
	 */
	private int[] judgenow;
	private int[] judgecombo;
	/**
	 * �뜝�럥�뼋�븨�똻�뼇占쎌쎗�뜝�럩�걢�뜝�럥�렮(ms , +�뜝�럡荑귛뜝�럥�뿬�뜝�럥諭ｅ뜝�럡愿쇔뜝�럡苡�-�뜝�럡荑귛뜝�럡肄ｅ뜝�럥諭ｅ뜝�럡愿�)
	 */
	private long[] judgefast;
	/**
	 * �뜝�럥�듋�뜝�럥�뵍庸뉗빢�삕�뜝�럡荑갟N
	 */
	private LongNote[] processing;
	/**
	 * �뜝�럩�뼐椰꾆몄칮�뜝�룞�삕野껊풁CN
	 */
	private LongNote[] passing;
	/**
	 * HCN�뇾�슣占싸삳츢�뜝�럥�뼋�븨�뙋�삕
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
	 * HCN�뜝�럡荑곭뇾�슣占쏙옙占쎈떓�뜝�럥�렮�뜝�럩�뭵(ms)
	 */
	private static final int hcnduration = 200;
	/**
	 * �뜝�럡�돺�뜝�럡�떖�뜝�럡�돦�뜝�럥�뼋�븨�똻�뼅繹먮돍�삕�댆戮녹삕繹먲퐦�삕�댆占�
	 */
	private int[][] njudge;
	private long judgestart;
	private long judgeend;
	/**
	 * CN癲덈㈇�뇥占쎄텥�뜝�럥�뼋�븨�똻�뼅繹먮돍�삕�댆戮녹삕繹먲퐦�삕�댆占�
	 */
	private int[][] cnendjudge;
	/**
	 * �뜝�럡�븣�뜝�럡�뀯�뜝�럡占썲뜝�럡留쇿뜝�럡留믣뜝�럥�뼋�븨�똻�뼅繹먮돍�삕�댆戮녹삕繹먲퐦�삕�댆占�
	 */
	private int[][] sjudge;
	private int[][] scnendjudge;
	/**
	 * PMS�뜝�럥�럞�뜝�럥�뼋�븨�똻�뼅亦끤우삕亦끹룇�삕繹먮돍�삕繹먲옙(�솾�뫂�뮚OOR�뜝�럡苡멨뜝�럡�뀷�뜝�럡�떋�뜝�럡臾뜹뜝�럡�뀪�뜝�럡留쇿뜝�럡�돮�뜝�룞�삕1�뜝�럡�돺�뜝�럡�떖�뜝�럡�돦�뜝�럡苡썲뜝�럡苡긷뜝�럡愿�1�솾�뫂�뮚OOR�뜝�럡猿섇뜝�럡苡�)�뜝�럡荑곩뜝�럩留뗥뜝�럥諭�/�뜝�럡�맗�뜝�럥諭�
	 */
	private boolean[] combocond;

	private MissCondition miss;
	/**
	 * �뜝�럥�봻�뜝�럥�뼋�븨�똻�뼊燁살슱�삕野껋���삕繹먮벨�삕�댆戮녹삕繹먮맮�삕野껋���삕占쎈뼋�븨�똻�뼅�뤃占쏙옙�굦占쎈쎕占쎈�끻뜝�럡援됧뜝�럡�뜲�뜝�럡愿띶뜝�럡苡삣뜝�럡肄ⓨ뜝�럡愿띶뜝�럡�눢G, GR, GD, BD, PR, MS�뜝�럡荑곩뜝�럩�읆
	 */
	private boolean[] judgeVanish;

	private long prevtime;

	private boolean autoplay = false;
	private long[] auto_presstime;
	/**
	 * �뜝�럡�뀧�뜝�럡�떖�뜝�럡�돮�뜝�럡臾뤷뜝�럡�땿�뜝�럡�뀞�뜝�럡苡멨뜝�럡�뀭�뜝�럡�떖�뜝�럡�럩�뜝�럥諭ｉ펶�빖肉ｅ쳞�먯삕�뤃恝�삕�뜝�뜴�꽑筌뚮챷�걢�뜝�럥�렮(ms)
	 */
	private final int auto_minduration = 80;

	private final JudgeAlgorithm algorithm;

	/**
	 * �뜝�럥�듋�뜝�럥�뵍�솾�슢占썸틦類㏃삕�댆戮녹삕繹먮맮�삕�뜮占�
	 */
	private int pastNotes = 0;

	/**
	 * PMS �뜝�럡�뀭�뜝�럡萸뉐뜝�럡占썲뜝�럥�럞 �뜝�럥�뼋�븨�뙋�삕
	 */
	private int PMcharaJudge = 0;

	/**
	 * �뜝�럩�윫占쎈��뜝占�100�뜝�럡�돺�뜝�럡�떖�뜝�럡�돦�뜝�럡荑곩뜝�럥�뼋�븨�똻�뼇占쎌쎗�뜝�럩�걢�뜝�럥�렮
	 */
	private long[] recentJudges = new long[100];
	/**
	 * �뜝�럥�뼋�븨�똻�뼇占쎌쎗�뜝�럩�걢�뜝�럥�렮�뜝�럡荑곩뜝�럡臾섇뜝�럡留쇿뜝�럡�돰
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
		// �뜝�럩�뼐椰꾆뜯뀳占쎄텊野껋���삕占쎈뼋�븨�뙋�삕
		Arrays.fill(next_inclease, false);
		for (int lane = 0; lane < laneassign.length; lane++) {
			final Lane lanemodel = lanes[lane];
			lanemodel.mark((int) (prevtime + judgestart - 100));
			boolean pressed = false;
			for (int key : laneassign[lane]) {
				if (keyData.getKeyState(key)) {
					pressed = true;
					break;
				}
			}
			
			for (Note note = lanemodel.getNote(); note != null && note.getTime() <= time; note = lanemodel.getNote()) {
				if (note.getTime() <= prevtime) {
					continue;
				}
				if (note instanceof LongNote) {
					// HCN�뜝�럥�뼋�븨�뙋�삕

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
					System.out.println("E");
					final MineNote mnote = (MineNote) note;
					// �뜝�럩紐ｅ뜝�럩�윲�뜝�럡�돺�뜝�럡�떖�뜝�럡�돮�뜝�럥�뼋�븨�뙋�삕
					main.getGauge().addValue(-mnote.getDamage());
					System.out.println("Mine Damage : " + mnote.getWav());
				}
				if (autoplay) {
					// �뜝�럡愿��뜝�럡愿��뜝�럡苡썲뜝�럡�뀧�뜝�럡�떖�뜝�럡�돮�뜝�럡臾뤷뜝�럡�땿�뜝�럡�뀞�뜝�럥�듋�뜝�럥�뵍�뜝�럡�럩�뜝�럥占쏙옙�뜝�럡�뜳�뜝�럡�뜲
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
								//LN�뜝�럩�걢�뜝�럡荑곩뜝�럡�땿�뜝�럡�떖�뜝�럡�븗�뜝�럡�떖�뜝�럥猷얍땱�떜留싷옙�윫�뜝�럥�듋�뜝�럥�뵍
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
			// HCN�뜝�럡�뀵�뜝�럡�떖�뜝�럡�븢�뇾�슣占쏙옙占쎈떓�뜝�럥�뼋�븨�뙋�삕
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
			final long ptime = keyData.getKeyTime(key);
			if (ptime == 0) {
				continue;
			}
			final Lane lanemodel = lanes[lane];
			lanemodel.reset();
			final int sc = sckeyassign[lane];

			if (keyData.getKeyState(key)) {
				// �뜝�럡�뀭�뜝�럡�떖�뜝�럡愿뤷뜝�럥諭ｅ뜝�럡愿드뜝�럡�뜳�뜝�럡援닷뜝�럡苡밧뜝�럡愿묈뜝�럡荑곩뜝�럥�듋�뜝�럥�뵍
				if (processing[lane] != null) {
					// BSS癲덈㈇�뇥占쎄텥�뜝�럥�듋�뜝�럥�뵍
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
						//						 System.out.println("BSS癲덈㈇�뇥占쎄텥�뜝�럥�뼋�븨�뙋�삕 - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane].hashCode());
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
						// �뜝�럡愿��뜝�럡愿��뜝�럡苡썲뜝�럩逾듿뜝�럡�뜲�뜝�럡荑곩뜝�럡荑귛뜝�럡臾삣뜝�럡�땽�뜝�럡留믣뜝�럡�뀭�뜝�럡�떖�뜝�럡�뀘�뜝�럡�븕�뜝�럡�뀞�뜝�럡�떋濚욌꼬�벊�돧�뜝�럡肄잌뜝�럡�뜮�뜝�럡肄ュ뜝�럡苡쇔뜝�럡肄℡뜝�럡荑귛뜝�럡援�
					}
				} else {
					final int[][] judge = sc >= 0 ? sjudge : njudge;
					// 占쎈눇占쎈펾�꽴�슪�삕繹먮벨�삕�댆戮녹삕繹먭랬�삕野껋���삕占쎈광�뜝�럥�떑
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
						// TODO �뜝�럡愿��뜝�럡荑곩뜝�럩�걢�뜝�럡�븣�뜝�럡苡며솾�뫂�뮚OOR�뜝�럥�듋�뜝�럥�뵍�뜝�럡�럩�뜝�럥�뱤�븨�쓹캉椰꾊띿삕椰꾨벝�삕�뤃恝�삕野껁룇�삕椰꾠끏�삕椰꾬옙
						if (tnote instanceof LongNote) {
							// �뜝�럡�떃�뜝�럡�떋�뜝�럡�뀰�뜝�럡�돺�뜝�럡�떖�뜝�럡�돮�뜝�럥�듋�뜝�럥�뵍
							final LongNote ln = (LongNote) tnote;
							main.play(tnote, config.getKeyvolume(), 0);
							if (((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
									|| ln.getType() == LongNote.TYPE_LONGNOTE)
									&& j < 4) {
								passingcount[lane] = (int) (tnote.getTime() - ptime);
								//LN�뜝�럩�걢�뜝�럡荑곩뜝�럡�땿�뜝�럡�떖�뜝�럡�븗�뜝�럡�떖�뜝�럥猷얍땱�떜留싷옙�윫�뜝�럥�듋�뜝�럥�뵍
								this.judge[player[lane]][offset[lane]] = 8;
							} else {
								final int dtime = (int) (tnote.getTime() - ptime);
								this.update(lane, ln, time, j, dtime);
							}
							if (j < 4) {
								processing[lane] = ln.getPair();
								if (sc >= 0) {
									// BSS�뜝�럥�듋�뜝�럥�뵍�뜝�럥占쏙옙�꽱�뜝占�
									//									 System.out.println("BSS�뜝�럥占쏙옙�꽱占쎈엠占쎈뼋�븨�뙋�삕 - Time : " + ptime + " Judge : " + j + " KEY : " + key + " LN : " + ln.getPair().hashCode());
									sckey[sc] = key;
								}
							}
						} else {
							main.play(tnote, config.getKeyvolume(), 0);
							// �뜝�럩�뼇筌띾슪�삕繹먮벨�삕�댆戮녹삕繹먮맮�삕占쎈듋�뜝�럥�뵍
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
						// �솾�뫂�뮚OOR�뜝�럥�뼋�븨�똻�뼅椰꾬퐦�삕野껊���삕椰꾩떣�삕野껊쪋�삕椰꾠끏�삕野껋���삕�댆猿볦삕�댆戮녹삕亦낉퐦�삕�댆戮녹삕占쎈＞�땱�떜留싷옙�윫�뜝�럥�듋�뜝�럥�뵍
						this.judge[player[lane]][offset[lane]] = 0;

						// �솾�뫂�뮚OOR�뜝�럥�뼋�븨�똻�뼅椰꾬퐦�삕野껊���삕椰꾩떣�삕野껊쪋�삕椰꾠끏�삕野껋���삕亦낆꼻�삕�댆戮녹삕占쎌몱�뜝�럥�듋�뜝�럥�뵍
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
				// �뜝�럡�뀭�뜝�럡�떖�뜝�럡愿뤷뜝�럩�쑗�뜝�럡愿드뜝�럡�뜳�뜝�럡援닷뜝�럡苡밧뜝�럡愿묈뜝�럡荑곩뜝�럥�듋�뜝�럥�뵍
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
						// CN, HCN�뜝�럩�쑗�뜝�럡愿쇔뜝�럥�듋�뜝�럥�뵍
						boolean release = true;
						if (sc >= 0) {
							if (j != 4 || key != sckey[sc]) {
								release = false;
							} else {
								//								 System.out.println("BSS�뜝�럥�뼭�뵳濡녹삕占쎈쑗�뜝�럡愿쇔뜝�럥�뼋�븨�뙋�삕 - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane]);
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
						// LN�뜝�럩�쑗�뜝�럡愿쇔뜝�럥�듋�뜝�럥�뵍
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

			// LN癲덈㈇�뇥占쎄텥�뜝�럥�뼋�븨�뙋�삕
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
			// 亦껁끇肉⒴뜝�럡�뜇椰꾩퇆OOR�뜝�럥�뼋�븨�뙋�삕
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
			// LN�뜝�럥�듋�뜝�럥�뵍�뜝�럡�븶�뜝�럡�뀞�뜝�럡臾삣뜝�럡�떖
			// TODO processing�뜝�뜫�썲칰�⑺닧占쎈쭠占쎌넁�뜝�럡荑곩뜝�럡苡밧뜝�럡愿묈뜝�럡荑곩뜝�럡猿숂븨�똻�삃雅뚭퉵�삕椰꾬옙�뜝�럡援닷뜝�럡肄�
			// TODO HCN�뜝�럡荑귛뜝�럥�뼌�뜝�럡�븶�뜝�럡�뀞�뜝�럡臾삣뜝�럡�떖�뜝�럡苡썲뜝�럡援됧뜝�럡�뜲�뜝�럡愿띶뜝�럡�꺘
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
	 * �뜝�럥夷닷뜝�럩紐쎾뜝�럡荑�1�뜝�럩�윧�뜝�럥爰뤷뜝�럡荑곩뜝�럡�뀷�뜝�럡�떋�뜝�럡臾뜹뜝�럥�돯�뜝�럡�럩�뜝�럥吏롨�띔퍔占쏙퐡援됧뜝�럡�뜲
	 *
	 * @return �뜝�럥夷닷뜝�럩紐쎾뜝�럡荑곩뜝�럡�뀷�뜝�럡�떋�뜝�럡臾뜹뜝�럥�돯
	 */
	public int getCombo() {
		return combo;
	}

	/**
	 * �뜝�럥夷닷뜝�럩紐쎾뜝�럡荑곩뜝�럡�뀷�뜝�럡�떖�뜝�럡�븣�뜝�럥爰뤷뜝�럡荑곩뜝�럡�뀷�뜝�럡�떋�뜝�럡臾뜹뜝�럥�돯�뜝�럡�럩�뜝�럥吏롨�띔퍔占쏙퐡援됧뜝�럡�뜲
	 *
	 * @return �뜝�럥夷닷뜝�럩紐쎾뜝�럡荑곩뜝�럡�뀷�뜝�럡�떋�뜝�럡臾뜹뜝�럥�돯
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
	 * �뜝�럥�끉�븨�똻�뼅野껋���삕占쎈뼋�븨�똻�뼅野껋���삕亦낅슁�삕亦낅쵓�삕�댆�엪�삕繹먮맮�삕�뜮�뵃�삕�뤃占쏙옙踰��겫�벀援�
	 *
	 * @param judge
	 *            0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
	 * @return �뜝�럥�뼋�븨�똻�뼅野껋���삕亦낅슁�삕亦낅쵓�삕�댆�엪�삕繹먮맮�삕�뜮占�
	 */
	public int getJudgeCount(int judge) {
		return score.getJudgeCount(judge);
	}

	/**
	 * �뜝�럥�끉�븨�똻�뼅野껋���삕占쎈뼋�븨�똻�뼅野껋���삕亦낅슁�삕亦낅쵓�삕�댆�엪�삕繹먮맮�삕�뜮�뵃�삕�뤃占쏙옙踰��겫�벀援�
	 *
	 * @param judge
	 *            0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
	 * @param fast
	 *            true:FAST, flase:SLOW
	 * @return �뜝�럥�뼋�븨�똻�뼅野껋���삕亦낅슁�삕亦낅쵓�삕�댆�엪�삕繹먮맮�삕�뜮占�
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
