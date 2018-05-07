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
 * 占쎄퉽占쎄틬占쎄퉪占쎈떓畑댁떓占썩넀由ㅿ옙逾わ옙沅싷옙爰�占쎄때
 *
 * @author exch
 */
public class JudgeManager {

	// TODO HCN占쎈뱣占쎄괼占쎌럪占쎄괼占쎄쿁占쎌뇞占쎌쑚占쎄쿂占쎄쾻占쎄콨占쎄굉占쎄데塋딉옙

	private final BMSPlayer main;
	/**
	 * LN type
	 */
	private int lntype;
	private Lane[] lanes;

	/**
	 * 占쎈쨴占쎌몛占쎄쿁占쎈떓畑댁떏沅뽳옙沅묕옙爰랃옙源됵옙�꺏庸뉛옙
	 */
	private IRScoreData score = new IRScoreData();

	/**
	 * 占쎈쨴占쎌몛占쎄쿁占쎄텭占쎄틡占쎄묶占쎈퉲
	 */
	private int combo;
	/**
	 * 占쎄텭占쎄틬占쎄때占쎌끋占쎄쿁占쎈쨴占쎌몛占쎄쿁占쎄텭占쎄틡占쎄묶占쎈퉲
	 */
	private int coursecombo;
	/**
	 * 占쎄텭占쎄틬占쎄때占쎌끋占쎄쿁占쏙옙勇싥렞沅좑옙爰랃옙源�占쎈퉲
	 */
	private int coursemaxcombo;
	/**
	 * 占쎈떓畑댁떘占쏙퐛�빣占쎄틕占쎄틬占쎄땃占쎄틬占쎄쿁占쎈룾
	 */
	private int[][] judge;
	/**
	 * 占쎈쨴占쎌몛�깗�몺�뀏鼇앾옙占쎄쿁占쎈떓畑댐옙
	 */
	private int[] judgenow;
	private int[] judgecombo;
	/**
	 * 占쎈떓畑댁떑�웾占쎌끋占쎈펻(ms , +占쎄쿂占쎈여占쎈뱣占쎄괼占쎄쾸-占쎄쿂占쎄콣占쎈뱣占쎄괼)
	 */
	private long[] judgefast;
	/**
	 * 占쎈늾占쎈┐鼇앾옙占쎄쿁LN
	 */
	private LongNote[] processing;
	/**
	 * 占쎌떘嫄θ쯁占쏙옙寃뾊CN
	 */
	private LongNote[] passing;
	/**
	 * HCN營뚯�λ뮙占쎈떓畑댐옙
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
	 * HCN占쎄쿁營뚯���닩占쎈펻占쎌뒄(ms)
	 */
	private static final int hcnduration = 200;
	/**
	 * 占쎄퉽占쎄틬占쎄퉪占쎈떓畑댁떏源뉛옙爰뽳옙源ｏ옙爰�
	 */
	private int[][] njudge;
	private long judgestart;
	private long judgeend;
	/**
	 * CN麗멸퇍�궚占쎈떓畑댁떏源뉛옙爰뽳옙源ｏ옙爰�
	 */
	private int[][] cnendjudge;
	/**
	 * 占쎄때占쎄텥占쎄�占쎄맙占쎄맒占쎈떓畑댁떏源뉛옙爰뽳옙源ｏ옙爰�
	 */
	private int[][] sjudge;
	private int[][] scnendjudge;
	/**
	 * PMS占쎈뎁占쎈떓畑댁떏沅ο옙沅㏆옙源뉛옙源�(癲⑦뒦OOR占쎄쾸占쎄텭占쎄틡占쎄묶占쎄텠占쎄맙占쎄퉱占쏙옙1占쎄퉽占쎄틬占쎄퉪占쎄쾽占쎄쾱占쎄광1癲⑦뒦OOR占쎄께占쎄쾸)占쎄쿁占쎌맋占쎈뱟/占쎄퐩占쎈뱟
	 */
	private boolean[] combocond;

	private MissCondition miss;
	/**
	 * 占쎈│占쎈떓畑댁떒移울옙寃쀯옙源뺧옙爰뽳옙源됵옙寃쀯옙�떓畑댁떏援��삌�뛼�뀅占쎄굉占쎄데占쎄괍占쎄쾻占쎄콨占쎄괍占쎄뇹G, GR, GD, BD, PR, MS占쎄쿁占쎌쟼
	 */
	private boolean[] judgeVanish;

	private long prevtime;

	private boolean autoplay = false;
	private long[] auto_presstime;
	/**
	 * 占쎄텞占쎄틬占쎄퉱占쎄묏占쎄틕占쎄텕占쎄쾸占쎄텣占쎄틬占쎄뎌占쎈뱣鼇앸뿣嫄뀐옙援ο옙占썲선琉몄끋占쎈펻(ms)
	 */
	private final int auto_minduration = 80;

	private final JudgeAlgorithm algorithm;

	/**
	 * 占쎈늾占쎈┐癲뚮�源뺧옙爰뽳옙源됵옙鍮�
	 */
	private int pastNotes = 0;

	/**
	 * PMS 占쎄텣占쎄뭇占쎄�占쎈뎁 占쎈떓畑댐옙
	 */
	private int PMcharaJudge = 0;

	/**
	 * 占쎌럪�벀占�100占쎄퉽占쎄틬占쎄퉪占쎄쿁占쎈떓畑댁떑�웾占쎌끋占쎈펻
	 */
	private long[] recentJudges = new long[100];
	/**
	 * 占쎈떓畑댁떑�웾占쎌끋占쎈펻占쎄쿁占쎄묘占쎄맙占쎄퉳
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
		// 占쎌떘嫄ζⅩ�궇寃쀯옙�떓畑댐옙
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
					// HCN占쎈떓畑댐옙

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
					// 占쎌몣占쎌럯占쎄퉽占쎄틬占쎄퉱占쎈떓畑댐옙
					main.getGauge().addValue(-mnote.getDamage());
					System.out.println("Mine Damage : " + mnote.getWav());
				}
				if (autoplay) {
					// 占쎄괭占쎄괭占쎄쾽占쎄텞占쎄틬占쎄퉱占쎄묏占쎄틕占쎄텕占쎈늾占쎈┐占쎄뎌占쎈��占쎄덱占쎄데
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
								//LN占쎌끋占쎄쿁占쎄틕占쎄틬占쎄땃占쎄틬占쎈룾勇싲맚�럪占쎈늾占쎈┐
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
			// HCN占쎄텫占쎄틬占쎄땋營뚯���닩占쎈떓畑댐옙
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
				// 占쎄텣占쎄틬占쎄괏占쎈뱣占쎄괵占쎄덱占쎄굴占쎄쾹占쎄광占쎄쿁占쎈늾占쎈┐
				if (processing[lane] != null) {
					// BSS麗멸퇍�궚占쎈늾占쎈┐
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
						//						 System.out.println("BSS麗멸퇍�궚占쎈떓畑댐옙 - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane].hashCode());
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
						// 占쎄괭占쎄괭占쎄쾽占쎌뵊占쎄데占쎄쿁占쎄쿂占쎄묻占쎄틓占쎄맒占쎄텣占쎄틬占쎄텑占쎄땁占쎄텕占쎄틡繞벿듭㉫占쎄콟占쎄덮占쎄콫占쎄쾼占쎄콢占쎄쿂占쎄교
					}
				} else {
					final int[][] judge = sc >= 0 ? sjudge : njudge;
					// �뇦�뼍愿뚳옙源뺧옙爰뽳옙源귨옙寃쀯옙�뱤占쎈닧
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
						// TODO 占쎄괭占쎄쿁占쎌끋占쎄때占쎄쾸癲⑦뒦OOR占쎈늾占쎈┐占쎄뎌占쎈듊畑띕Ĳ嫄э옙嫄듸옙援ο옙寃㏆옙嫄ㅿ옙嫄�
						if (tnote instanceof LongNote) {
							// 占쎄틙占쎄틡占쎄텦占쎄퉽占쎄틬占쎄퉱占쎈늾占쎈┐
							final LongNote ln = (LongNote) tnote;
							main.play(tnote, config.getKeyvolume(), 0);
							if (((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
									|| ln.getType() == LongNote.TYPE_LONGNOTE)
									&& j < 4) {
								passingcount[lane] = (int) (tnote.getTime() - ptime);
								//LN占쎌끋占쎄쿁占쎄틕占쎄틬占쎄땃占쎄틬占쎈룾勇싲맚�럪占쎈늾占쎈┐
								this.judge[player[lane]][offset[lane]] = 8;
							} else {
								final int dtime = (int) (tnote.getTime() - ptime);
								this.update(lane, ln, time, j, dtime);
							}
							if (j < 4) {
								processing[lane] = ln.getPair();
								if (sc >= 0) {
									// BSS占쎈늾占쎈┐占쎈��넼占�
									//									 System.out.println("BSS占쎈��넼�뿥�떓畑댐옙 - Time : " + ptime + " Judge : " + j + " KEY : " + key + " LN : " + ln.getPair().hashCode());
									sckey[sc] = key;
								}
							}
						} else {
							main.play(tnote, config.getKeyvolume(), 0);
							// 占쎌떑留뚳옙源뺧옙爰뽳옙源됵옙�늾占쎈┐
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
						// 癲⑦뒦OOR占쎈떓畑댁떏嫄ｏ옙寃믭옙嫄싷옙寃륅옙嫄ㅿ옙寃쀯옙爰껓옙爰뽳옙沅ｏ옙爰뽳옙�룾勇싲맚�럪占쎈늾占쎈┐
						this.judge[player[lane]][offset[lane]] = 0;

						// 癲⑦뒦OOR占쎈떓畑댁떏嫄ｏ옙寃믭옙嫄싷옙寃륅옙嫄ㅿ옙寃쀯옙沅섓옙爰뽳옙�쑚占쎈늾占쎈┐
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
				// 占쎄텣占쎄틬占쎄괏占쎌뜶占쎄괵占쎄덱占쎄굴占쎄쾹占쎄광占쎄쿁占쎈늾占쎈┐
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
						// CN, HCN占쎌뜶占쎄괼占쎈늾占쎈┐
						boolean release = true;
						if (sc >= 0) {
							if (j != 4 || key != sckey[sc]) {
								release = false;
							} else {
								//								 System.out.println("BSS占쎈떰由롳옙�뜶占쎄괼占쎈떓畑댐옙 - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane]);
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
						// LN占쎌뜶占쎄괼占쎈늾占쎈┐
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

			// LN麗멸퇍�궚占쎈떓畑댐옙
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
			// 沃ㅻ뿩占쎄퍊嫄췗OOR占쎈떓畑댐옙
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
			// LN占쎈늾占쎈┐占쎄땟占쎄텕占쎄묻占쎄틬
			// TODO processing占썬뀽寃쀩툣�맕�솑占쎄쿁占쎄쾹占쎄광占쎄쿁占쎄껙畑댁옊二깍옙嫄�占쎄굴占쎄콢
			// TODO HCN占쎄쿂占쎈떔占쎄땟占쎄텕占쎄묻占쎄틬占쎄쾽占쎄굉占쎄데占쎄괍占쎄탾
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
	 * 占쎈쨴占쎌몛占쎄쿁1占쎌럧占쎈꺏占쎄쿁占쎄텭占쎄틡占쎄묶占쎈퉲占쎄뎌占쎈짎耶껋�ｊ굉占쎄데
	 *
	 * @return 占쎈쨴占쎌몛占쎄쿁占쎄텭占쎄틡占쎄묶占쎈퉲
	 */
	public int getCombo() {
		return combo;
	}

	/**
	 * 占쎈쨴占쎌몛占쎄쿁占쎄텭占쎄틬占쎄때占쎈꺏占쎄쿁占쎄텭占쎄틡占쎄묶占쎈퉲占쎄뎌占쎈짎耶껋�ｊ굉占쎄데
	 *
	 * @return 占쎈쨴占쎌몛占쎄쿁占쎄텭占쎄틡占쎄묶占쎈퉲
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
	 * 占쎈셼畑댁떏寃쀯옙�떓畑댁떏寃쀯옙沅뽳옙沅묕옙爰랃옙源됵옙鍮딉옙援��벀遺듦굉
	 *
	 * @param judge
	 *            0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
	 * @return 占쎈떓畑댁떏寃쀯옙沅뽳옙沅묕옙爰랃옙源됵옙鍮�
	 */
	public int getJudgeCount(int judge) {
		return score.getJudgeCount(judge);
	}

	/**
	 * 占쎈셼畑댁떏寃쀯옙�떓畑댁떏寃쀯옙沅뽳옙沅묕옙爰랃옙源됵옙鍮딉옙援��벀遺듦굉
	 *
	 * @param judge
	 *            0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
	 * @param fast
	 *            true:FAST, flase:SLOW
	 * @return 占쎈떓畑댁떏寃쀯옙沅뽳옙沅묕옙爰랃옙源됵옙鍮�
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
