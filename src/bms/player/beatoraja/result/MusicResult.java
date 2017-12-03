package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.badlogic.gdx.utils.FloatArray;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.SkinType;

/**
 * リザルト
 *
 * @author exch
 */
public class MusicResult extends MainState {

	private int oldclear;
	private int oldexscore;
	private int oldmisscount;
	private int oldcombo;

	/**
	 * 全ノーツの平均ズレ
	 */
	private float avgduration;

	/**
	 * 状態
	 */
	private int state;

	public static final int STATE_OFFLINE = 0;
	public static final int STATE_IR_PROCESSING = 1;
	public static final int STATE_IR_FINISHED = 2;

	private int next;

	private int irrank;
	private int irprevrank;
	private int irtotal;

	private int saveReplay[] = new int[4];
	private static final int replay= 4;

	public static final int SOUND_CLEAR = 0;
	public static final int SOUND_FAIL = 1;
	public static final int SOUND_CLOSE = 2;

	private ResultKeyProperty property;

	public MusicResult(MainController main) {
		super(main);
	}

	public void create() {
		final PlayerResource resource = getMainController().getPlayerResource();

		setSound(SOUND_CLEAR, "clear.wav", SoundType.SOUND,false);
		setSound(SOUND_FAIL, "fail.wav", SoundType.SOUND,false);
		setSound(SOUND_CLOSE, "resultclose.wav", SoundType.SOUND,false);

		property = ResultKeyProperty.get(resource.getBMSModel().getMode());
		if(property == null) {
			property = ResultKeyProperty.BEAT_7K;
		}

		updateScoreDatabase();
		// リプレイの自動保存
		if(resource.getAutoplay() == 0){
			for(int i=0;i<replay;i++){
				/*
				 * コンフィグ値:0=保存しない 1=スコア更新時 2=スコアが自己ベスト以上 3=BP更新時 4=BPが自己ベスト以下
				 * 5=COMBO更新時 6=COMBOが自己ベスト以上 7=ランプ更新時 8=ランプが自己ベスト以上 9=何か更新した時 10=毎回
				 */
				switch(resource.getConfig().getAutoSaveReplay()[i]){
				case 0:
					break;
				case 1:
					if(resource.getScoreData().getExscore() > oldexscore)
						saveReplayData(i);
					break;
				case 2:
					if(resource.getScoreData().getExscore() >= oldexscore)
						saveReplayData(i);
					break;
				case 3:
					if(resource.getScoreData().getMinbp() < oldmisscount || oldclear == NoPlay.id)
						saveReplayData(i);
					break;
				case 4:
					if(resource.getScoreData().getMinbp() <= oldmisscount || oldclear == NoPlay.id)
						saveReplayData(i);
					break;
				case 5:
					if(resource.getScoreData().getCombo() > oldcombo)
						saveReplayData(i);
					break;
				case 6:
					if(resource.getScoreData().getCombo() >= oldcombo)
						saveReplayData(i);
					break;
				case 7:
					if(resource.getScoreData().getClear() > oldclear)
						saveReplayData(i);
					break;
				case 8:
					if(resource.getScoreData().getClear() >= oldclear)
						saveReplayData(i);
					break;
				case 9:
					if(resource.getScoreData().getClear() > oldclear || resource.getScoreData().getCombo() > oldcombo||
						resource.getScoreData().getMinbp() < oldmisscount || resource.getScoreData().getExscore() > oldexscore)
						saveReplayData(i);
					break;
				case 10:
						saveReplayData(i);
					break;
				}
			}
		}
		// コースモードの場合はリプレイデータをストックする
		if (resource.getCourseBMSModels() != null) {
			resource.addCourseReplay(resource.getReplayData());
			resource.addCourseGauge(resource.getGauge());
		}

		loadSkin(SkinType.RESULT);
	}

	public void render() {
		int time = getNowTime();
		if (getTimer()[TIMER_RESULTGRAPH_BEGIN] == Long.MIN_VALUE) {
			getTimer()[TIMER_RESULTGRAPH_BEGIN] = time;
		}
		if (getTimer()[TIMER_RESULTGRAPH_END] == Long.MIN_VALUE) {
			getTimer()[TIMER_RESULTGRAPH_END] = time;
		}
		if (getTimer()[TIMER_RESULT_UPDATESCORE] == Long.MIN_VALUE
				&& ((MusicResultSkin) getSkin()).getRankTime() == 0) {
			getTimer()[TIMER_RESULT_UPDATESCORE] = time;
		}
	    if(getTimer()[TIMER_STARTINPUT] == Long.MIN_VALUE && time > getSkin().getInput()){
	    		getTimer()[TIMER_STARTINPUT] =  time;
	    }

		final MainController main = getMainController();

		final PlayerResource resource = getMainController().getPlayerResource();

		if (getTimer()[TIMER_FADEOUT] != Long.MIN_VALUE) {
			if (time > getTimer()[TIMER_FADEOUT] + getSkin().getFadeout()) {
				stop(SOUND_CLEAR);
				stop(SOUND_FAIL);
				stop(SOUND_CLOSE);
				getMainController().getAudioProcessor().stop((Note) null);

				boolean[] keystate = main.getInputProcessor().getKeystate();
//				System.out.println(Arrays.toString(keystate));
				long[] keytime = main.getInputProcessor().getTime();
				Arrays.fill(keytime, 0);

				if (resource.getCourseBMSModels() != null) {
					if (resource.getGauge().get(resource.getGauge().size - 1) <= 0) {
						if (resource.getCourseScoreData() != null) {
							// 未達曲のノーツをPOORとして加算
							final List<FloatArray> coursegauge = resource.getCourseGauge();
							final int cg = resource.getCourseBMSModels().length;
							for (int i = 0; i < cg; i++) {
								if (coursegauge.size() <= i) {
									resource.getCourseScoreData().setMinbp(resource.getCourseScoreData().getMinbp()
											+ resource.getCourseBMSModels()[i].getTotalNotes());
								}
							}
							// 不合格リザルト
							main.changeState(MainController.STATE_GRADE_RESULT);
						} else {
							// コーススコアがない場合は選曲画面へ
							main.changeState(MainController.STATE_SELECTMUSIC);
						}
					} else if (resource.nextCourse()) {
						main.changeState(MainController.STATE_PLAYBMS);
					} else {
						// 合格リザルト
						main.changeState(MainController.STATE_GRADE_RESULT);
					}
				} else {
					ResultKeyProperty.ResultKey key = null;
					for(int i = 0; i < property.getAssignLength(); i++) {
						if(property.getAssign(i) == ResultKeyProperty.ResultKey.REPLAY_DIFFERENT && keystate[i]) {
							key = ResultKeyProperty.ResultKey.REPLAY_DIFFERENT;
							break;
						}
						if(property.getAssign(i) == ResultKeyProperty.ResultKey.REPLAY_SAME && keystate[i]) {
							key = ResultKeyProperty.ResultKey.REPLAY_SAME;
							break;
						}
					}
					if (resource.getAutoplay() == 0 && key == ResultKeyProperty.ResultKey.REPLAY_DIFFERENT) {
						Logger.getGlobal().info("オプションを変更せずリプレイ");
						// オプションを変更せず同じ譜面でリプレイ
						resource.getReplayData().pattern = null;
						resource.reloadBMSFile();
						main.changeState(MainController.STATE_PLAYBMS);
					} else if (resource.getAutoplay() == 0 && key == ResultKeyProperty.ResultKey.REPLAY_SAME) {
						// 同じ譜面でリプレイ
						Logger.getGlobal().info("同じ譜面でリプレイ");
						resource.reloadBMSFile();
						main.changeState(MainController.STATE_PLAYBMS);
					} else {
						main.changeState(MainController.STATE_SELECTMUSIC);
					}
				}
			}
		} else {
			if (time > getSkin().getScene()) {
				getTimer()[TIMER_FADEOUT] = time;
				if(getSound(SOUND_CLOSE) != null) {
					stop(SOUND_CLEAR);
					stop(SOUND_FAIL);
					play(SOUND_CLOSE);
				}
			}
		}

	}

	public void input() {
		int time = getNowTime();
		final MainController main = getMainController();
		final PlayerResource resource = getMainController().getPlayerResource();

		if (getTimer()[TIMER_FADEOUT] != Long.MIN_VALUE || getTimer()[TIMER_STARTINPUT] == Long.MIN_VALUE) {

		} else {
			if (time > getSkin().getInput()) {
				boolean[] keystate = main.getInputProcessor().getKeystate();
				long[] keytime = main.getInputProcessor().getTime();
				boolean ok = false;
				for(int i = 0; i < property.getAssignLength(); i++) {
					if(property.getAssign(i) != null && keystate[i] && keytime[i] != 0) {
						keytime[i] = 0;
						ok = true;
					}
				}

				if (resource.getScoreData() == null || ok) {
					if (((MusicResultSkin) getSkin()).getRankTime() != 0
							&& getTimer()[TIMER_RESULT_UPDATESCORE] == Long.MIN_VALUE) {
						getTimer()[TIMER_RESULT_UPDATESCORE] = time;
					} else if(state == STATE_OFFLINE || state == STATE_IR_FINISHED){
						getTimer()[TIMER_FADEOUT] = time;
						if(getSound(SOUND_CLOSE) != null) {
							stop(SOUND_CLEAR);
							stop(SOUND_FAIL);
							play(SOUND_CLOSE);
						}
					}
				}

				for (int i = 0; i < MusicSelector.REPLAY; i++) {
					if (main.getInputProcessor().getNumberState()[i + 1]) {
						saveReplayData(i);
						break;
					}
				}
			}
		}
	}

	private void saveReplayData(int index) {
		final PlayerResource resource = getMainController().getPlayerResource();
		if (resource.getAutoplay() == 0 && resource.getCourseBMSModels() == null && resource.getScoreData() != null) {
			if (saveReplay[index] == -1 && resource.isUpdateScore()) {
				ReplayData rd = resource.getReplayData();
				getMainController().getPlayDataAccessor().wrireReplayData(rd, resource.getBMSModel(),
						resource.getPlayerConfig().getLnmode(), index);
				saveReplay[index] = 1;
			}
		}
	}

	private void updateScoreDatabase() {
		Arrays.fill(saveReplay, -1);
		state = STATE_OFFLINE;
		irrank = irprevrank = irtotal = 0;
		final PlayerResource resource = getMainController().getPlayerResource();
		IRScoreData newscore = resource.getScoreData();
		if (newscore == null) {
			if (resource.getCourseScoreData() != null) {
				resource.getCourseScoreData()
						.setMinbp(resource.getCourseScoreData().getMinbp() + resource.getBMSModel().getTotalNotes());
				resource.getCourseScoreData().setClear(Failed.id);
			}
			return;
		}
		IRScoreData score = getMainController().getPlayDataAccessor().readScoreData(resource.getBMSModel(),
				resource.getPlayerConfig().getLnmode());
		if (score == null) {
			score = new IRScoreData();
		}
		oldclear = score.getClear();
		oldexscore = score.getExscore();
		oldmisscount = score.getMinbp();
		oldcombo = score.getCombo();

		getScoreDataProperty().setTargetScore(oldexscore, resource.getRivalScoreData(), resource.getBMSModel().getTotalNotes());
		getScoreDataProperty().update(newscore);
		next = 0;
		for (int i = 2; i < 9; i++) {
			if (newscore.getExscore() < i * 1111 * (resource.getBMSModel().getTotalNotes() * 2) / 10000) {
				next = newscore.getExscore() - i * 1111 * (resource.getBMSModel().getTotalNotes() * 2) / 10000;
				break;
			}
		}
		// duration average
		int count = 0;
		avgduration = 0;
		final int lanes = resource.getBMSModel().getMode().key;
		for (TimeLine tl : resource.getBMSModel().getAllTimeLines()) {
			for (int i = 0; i < lanes; i++) {
				Note n = tl.getNote(i);
				if (n != null && !(resource.getBMSModel().getLntype() == BMSModel.LNTYPE_LONGNOTE
						&& n instanceof LongNote && ((LongNote) n).isEnd())) {
					int state = n.getState();
					int time = n.getPlayTime();
					if (state >= 1) {
						count++;
						avgduration += Math.abs(time);
					}
				}
			}
		}
		avgduration /= count;

		// コースモードの場合はコーススコアに加算・累積する
		if (resource.getCourseBMSModels() != null) {
			if (resource.getScoreData().getClear() == Failed.id) {
				resource.getScoreData().setClear(NoPlay.id);
			}
			IRScoreData cscore = resource.getCourseScoreData();
			if (cscore == null) {
				cscore = new IRScoreData();
				cscore.setMinbp(0);
				int notes = 0;
				for (BMSModel mo : resource.getCourseBMSModels()) {
					notes += mo.getTotalNotes();
				}
				cscore.setNotes(notes);
				resource.setCourseScoreData(cscore);
			}
			cscore.setEpg(cscore.getEpg() + newscore.getEpg());
			cscore.setLpg(cscore.getLpg() + newscore.getLpg());
			cscore.setEgr(cscore.getEgr() + newscore.getEgr());
			cscore.setLgr(cscore.getLgr() + newscore.getLgr());
			cscore.setEgd(cscore.getEgd() + newscore.getEgd());
			cscore.setLgd(cscore.getLgd() + newscore.getLgd());
			cscore.setEbd(cscore.getEbd() + newscore.getEbd());
			cscore.setLbd(cscore.getLbd() + newscore.getLbd());
			cscore.setEpr(cscore.getEpr() + newscore.getEpr());
			cscore.setLpr(cscore.getLpr() + newscore.getLpr());
			cscore.setEms(cscore.getEms() + newscore.getEms());
			cscore.setLms(cscore.getLms() + newscore.getLms());
			cscore.setMinbp(cscore.getMinbp() + newscore.getMinbp());
			if (resource.getGauge().get(resource.getGauge().size - 1) > 0) {
				cscore.setClear(resource.getGrooveGauge().getClearType().id);
			} else {
				cscore.setClear(Failed.id);

				boolean b = false;
				// 残りの曲がある場合はtotalnotesをBPに加算する
				for (BMSModel m : resource.getCourseBMSModels()) {
					if (b) {
						cscore.setMinbp(cscore.getMinbp() + m.getTotalNotes());
					}
					if (m == resource.getBMSModel()) {
						b = true;
					}
				}
			}
			newscore = cscore;
		}

		if (resource.getAutoplay() == 0) {
			getMainController().getPlayDataAccessor().writeScoreDara(resource.getScoreData(), resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), resource.isUpdateScore());
			// TODO スコアハッシュがあり、有効期限が切れていないものを送信する？
			IRConnection ir = getMainController().getIRConnection();
			if (ir != null) {
				boolean send = true;
				switch(getMainController().getPlayerConfig().getIrsend()) {
				case PlayerConfig.IR_SEND_ALWAYS:
					break;
				case PlayerConfig.IR_SEND_COMPLETE_SONG:
					FloatArray gauge = resource.getGauge();
					send = gauge.get(gauge.size - 1) > 0.0;
					break;
				case PlayerConfig.IR_SEND_UPDATE_SCORE:
					IRScoreData current = resource.getScoreData();
					send = (current.getExscore() > oldexscore || current.getClear() > oldclear
							|| current.getCombo() > oldcombo || current.getMinbp() < oldmisscount);
					break;
				}
				
				if(send) {
					Logger.getGlobal().info("IRへスコア送信中");
					state = STATE_IR_PROCESSING;
					final IRScoreData oldscore = score;
					Thread irprocess = new Thread() {

						@Override
						public void run() {
							ir.sendPlayData(resource.getBMSModel(), resource.getScoreData());
							IRResponse<IRScoreData[]> response = ir.getPlayData(null, resource.getBMSModel());
							if(response.isSuccessed()) {
								IRScoreData[] scores = response.getData();
								irtotal = scores.length;

								for(int i = 0;i < scores.length;i++) {
									if(irrank == 0 && scores[i].getExscore() <= resource.getScoreData().getExscore() ) {
										irrank = i + 1;
									}
									if(irprevrank == 0 && scores[i].getExscore() <= oldscore.getExscore() ) {
										irprevrank = i + 1;
										if(irrank == 0) {
											irrank = irprevrank;
										}
									}
								}
								Logger.getGlobal().info("IRへスコア送信完了");
							} else {
								Logger.getGlobal().warning("IRからのスコア取得失敗 : " + response.getMessage());
							}

							state = STATE_IR_FINISHED;
						}
					};
					irprocess.start();					
				}
			}
		}

		if (newscore.getClear() != Failed.id) {
			play(SOUND_CLEAR);
		} else {
			play(SOUND_FAIL);
		}
	}

	public int getJudgeCount(int judge, boolean fast) {
		IRScoreData score = getMainController().getPlayerResource().getScoreData();
		if (score != null) {
			switch (judge) {
			case 0:
				return fast ? score.getEpg() : score.getLpg();
			case 1:
				return fast ? score.getEgr() : score.getLgr();
			case 2:
				return fast ? score.getEgd() : score.getLgd();
			case 3:
				return fast ? score.getEbd() : score.getLbd();
			case 4:
				return fast ? score.getEpr() : score.getLpr();
			case 5:
				return fast ? score.getEms() : score.getLms();
			}
		}
		return 0;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	public int getTotalNotes() {
		final PlayerResource resource = getMainController().getPlayerResource();
		return resource.getBMSModel().getTotalNotes();
	}

	public int getNumberValue(int id) {
		final PlayerResource resource = getMainController().getPlayerResource();
		switch (id) {
		case NUMBER_CLEAR:
			if (resource.getScoreData() != null) {
				return resource.getScoreData().getClear();
			}
			return Integer.MIN_VALUE;
		case NUMBER_TARGET_CLEAR:
			return oldclear;
		case NUMBER_HIGHSCORE:
		case NUMBER_HIGHSCORE2:
			return oldexscore;
		case NUMBER_TARGET_SCORE:
		case NUMBER_TARGET_SCORE2:
			return resource.getRivalScoreData();
		case NUMBER_DIFF_TARGETSCORE:
			return resource.getScoreData().getExscore() - resource.getRivalScoreData();
		case NUMBER_SCORE:
		case NUMBER_SCORE2:
		case NUMBER_SCORE3:
			if (resource.getScoreData() != null) {
				return resource.getScoreData().getExscore();
			}
			return Integer.MIN_VALUE;
		case NUMBER_DIFF_HIGHSCORE:
		case NUMBER_DIFF_HIGHSCORE2:
			return resource.getScoreData().getExscore() - oldexscore;
		case NUMBER_DIFF_NEXTRANK:
			return next;
		case NUMBER_MISSCOUNT:
		case NUMBER_MISSCOUNT2:
			if (resource.getScoreData() != null) {
				return resource.getScoreData().getMinbp();
			}
			return Integer.MIN_VALUE;
		case NUMBER_TARGET_MISSCOUNT:
			if (oldmisscount == Integer.MAX_VALUE) {
				return Integer.MIN_VALUE;
			}
			return oldmisscount;
		case NUMBER_DIFF_MISSCOUNT:
			if (oldmisscount == Integer.MAX_VALUE) {
				return Integer.MIN_VALUE;
			}
			return resource.getScoreData().getMinbp() - oldmisscount;
		case NUMBER_TARGET_MAXCOMBO:
			if (oldcombo > 0) {
				return oldcombo;
			}
			return Integer.MIN_VALUE;
		case NUMBER_MAXCOMBO:
		case NUMBER_MAXCOMBO2:
			if (resource.getScoreData() != null) {
				return resource.getScoreData().getCombo();
			}
			return Integer.MIN_VALUE;
		case NUMBER_DIFF_MAXCOMBO:
			if (oldcombo == 0) {
				return Integer.MIN_VALUE;
			}
			return resource.getScoreData().getCombo() - oldcombo;
		case NUMBER_GROOVEGAUGE:
			return (int) resource.getGauge().get(resource.getGauge().size - 1);
			case NUMBER_AVERAGE_DURATION:
			return (int) avgduration;
		case NUMBER_AVERAGE_DURATION_AFTERDOT:
			return ((int) (avgduration * 100)) % 100;
		case NUMBER_IR_RANK:
			if(state != STATE_OFFLINE) {
				return irrank;
			}
			return Integer.MIN_VALUE;
		case NUMBER_IR_PREVRANK:
			if(state != STATE_OFFLINE) {
				return irprevrank;
			}
			return Integer.MIN_VALUE;
		case NUMBER_IR_TOTALPLAYER:
			if(state != STATE_OFFLINE) {
				return irtotal;
			}
			return Integer.MIN_VALUE;
		}
		return super.getNumberValue(id);
	}

	public boolean getBooleanValue(int id) {
		final PlayerResource resource = getMainController().getPlayerResource();
		final IRScoreData score = resource.getScoreData();
		final IRScoreData cscore = resource.getCourseScoreData();
		switch (id) {
		case OPTION_DISABLE_SAVE_SCORE:
			return !resource.isUpdateScore();
		case OPTION_ENABLE_SAVE_SCORE:
			return resource.isUpdateScore();
		case OPTION_RESULT_CLEAR:
			return score.getClear() != Failed.id
					&& (cscore == null || cscore.getClear() != Failed.id);
		case OPTION_RESULT_FAIL:
			return score.getClear() == Failed.id
					|| (cscore != null && cscore.getClear() == Failed.id);
		case OPTION_UPDATE_SCORE:
			return score.getExscore() > oldexscore;
			case OPTION_DRAW_SCORE:
				return score.getExscore() == oldexscore;
		case OPTION_UPDATE_MAXCOMBO:
			return score.getCombo() > oldcombo;
			case OPTION_DRAW_MAXCOMBO:
				return score.getCombo() == oldcombo;
		case OPTION_UPDATE_MISSCOUNT:
			return score.getMinbp() < oldmisscount;
			case OPTION_DRAW_MISSCOUNT:
				return score.getMinbp() == oldmisscount;
		case OPTION_UPDATE_SCORERANK:
			return getScoreDataProperty().getNowRate() > getScoreDataProperty().getBestScoreRate();
			case OPTION_DRAW_SCORERANK:
				return getScoreDataProperty().getNowRate() == getScoreDataProperty().getBestScoreRate();
		case OPTION_NO_REPLAYDATA:
			return !getMainController().getPlayDataAccessor().existsReplayData(resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), 0);
		case OPTION_NO_REPLAYDATA2:
			return !getMainController().getPlayDataAccessor().existsReplayData(resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), 1);
		case OPTION_NO_REPLAYDATA3:
			return !getMainController().getPlayDataAccessor().existsReplayData(resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), 2);
		case OPTION_NO_REPLAYDATA4:
			return !getMainController().getPlayDataAccessor().existsReplayData(resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), 3);
		case OPTION_REPLAYDATA:
			return getMainController().getPlayDataAccessor().existsReplayData(resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), 0);
		case OPTION_REPLAYDATA2:
			return getMainController().getPlayDataAccessor().existsReplayData(resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), 1);
		case OPTION_REPLAYDATA3:
			return getMainController().getPlayDataAccessor().existsReplayData(resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), 2);
		case OPTION_REPLAYDATA4:
			return getMainController().getPlayDataAccessor().existsReplayData(resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), 3);
		case OPTION_REPLAYDATA_SAVED:
			return saveReplay[0] == 1;
		case OPTION_REPLAYDATA2_SAVED:
			return saveReplay[1] == 1;
		case OPTION_REPLAYDATA3_SAVED:
			return saveReplay[2] == 1;
		case OPTION_REPLAYDATA4_SAVED:
			return saveReplay[3] == 1;
		}
		return super.getBooleanValue(id);
	}

	public int getImageIndex(int id) {
		switch (id) {
			case NUMBER_CLEAR:
				final PlayerResource resource = getMainController().getPlayerResource();
				if (resource.getScoreData() != null) {
					return resource.getScoreData().getClear();
				}
				return Integer.MIN_VALUE;
			case NUMBER_TARGET_CLEAR:
				return oldclear;
		}
		return super.getImageIndex(id);
	}

	public void executeClickEvent(int id) {
		switch (id) {
		case BUTTON_REPLAY:
			saveReplayData(0);
			break;
		case BUTTON_REPLAY2:
			saveReplayData(1);
			break;
		case BUTTON_REPLAY3:
			saveReplayData(2);
			break;
		case BUTTON_REPLAY4:
			saveReplayData(3);
			break;
		}
	}

}
