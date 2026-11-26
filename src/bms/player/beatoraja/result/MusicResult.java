package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.*;
import static bms.player.beatoraja.skin.SkinProperty.*;
import static bms.player.beatoraja.SystemSoundManager.SoundType.*;

import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.input.keyboard.KeyboardShortcut;
import bms.player.beatoraja.input.keyboard.KeyBoardInputProcesseor.ControlKeys;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.graphics.Color;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.MainController.IRStatus;
import bms.player.beatoraja.MainState.MainStateType;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.ir.*;
import bms.player.beatoraja.play.BMSPlayerRule;
import bms.player.beatoraja.play.GrooveGauge;
import bms.player.beatoraja.play.SecondaryScoringRule;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.skin.property.EventFactory.EventType;
import bms.player.beatoraja.song.SongData;

/**
 * リザルト
 *
 * @author exch
 */
public class MusicResult extends AbstractResult {

	private ResultKeyProperty property;
	private LevelReviewTarget levelReviewTarget;
	private IRChartData resultChart;
	private List<SecondaryScoreDisplay> alternateScores = List.of();
	private int scoreDisplayIndex;
	private MessageRenderer.Message scoreRuleNotice;
	private final Map<PlayerResource.TimingData, TimingDistribution> scoringDistributions = new IdentityHashMap<>();
	private final TimingDistribution emptyTimingDistribution = emptyTimingDistribution();
	private TimingDistribution emptyTimingDistribution() {
		var distribution = new TimingDistribution(distRange);
		distribution.init();
		return distribution;
	}

	@Override public TimingDistribution getTimingDistribution() {
		var timing = resource.getScoringTiming(currentTimingRuleId());
		if (timing == null) return isShowingSecondaryScore() || !resource.getPrimaryScoringSelection().isStandard()
				? emptyTimingDistribution : super.getTimingDistribution();
		return scoringDistributions.computeIfAbsent(timing, data -> {
			var distribution = new TimingDistribution(distRange);
			distribution.init();
			for (int value : data.timings()) distribution.add(value);
			distribution.statisticValueCalcuate();
			return distribution;
		});
	}
	private String currentTimingRuleId() {
		var display = secondaryDisplay();
		return display != null ? display.result.ruleId() : resource.getPrimaryScoringSelection().effective().id();
	}
	public int[] getDisplayTimingJudgeArea() {
		var timing = resource.getScoringTiming(currentTimingRuleId());
		return timing != null ? timing.judgeArea() : bms.player.beatoraja.skin.SkinTimingVisualizer.getJudgeArea(resource);
	}

	private static final class SecondaryScoreDisplay {
		final PlayerResource.AlternateResultScore result;
		final ScoreData previousBest;
		final ScoreDataProperty properties = new ScoreDataProperty();
		int gaugeType;

		SecondaryScoreDisplay(PlayerResource.AlternateResultScore result) {
			this.result = result;
			gaugeType = result.gaugeHistory() != null && result.gaugeHistory().gauge() != null
					? result.gaugeHistory().gauge().getType() : result.score().getGauge();
			previousBest = result.previousBest() != null ? result.previousBest() : new ScoreData();
			properties.setTargetScore(previousBest.getRuleScore(), null, 0, null, result.score().getNotes(), result.score().getRuleMaxScore());
			properties.update(result.score());
		}
	}

	public record LevelReviewTarget(SongData song, int lnmode) {}
	
	private List<IRSendStatus> irSendStatus = new ArrayList<IRSendStatus>();

	public MusicResult(MainController main) {
		super(main, MainStateType.RESULT);
	}

	public void create() {
		prepareScoreDisplay();
		for(int i = 0;i < REPLAY_SIZE;i++) {
			saveReplay[i] = main.getPlayDataAccessor().existsReplayData(resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), i) ? ReplayStatus.EXIST : ReplayStatus.NOT_EXIST ;			
		}

		property = ResultKeyProperty.get(resource.getBMSModel().getMode());
		if (property == null) {
			property = ResultKeyProperty.BEAT_7K;
		}

		updateScoreDatabase();
		// リプレイの自動保存
		if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY) {
			for (int i = 0; i < REPLAY_SIZE; i++) {
				if (ReplayAutoSaveConstraint.get(resource.getPlayerConfig().getAutoSaveReplay()[i]).isQualified(oldscore,
						resource.getScoreData())) {
					saveReplayData(i);
				}
			}
		}
		// コースモードの場合はリプレイデータをストックする
		if (resource.getCourseBMSModels() != null) {
			resource.addCourseReplay(resource.getReplayData());
			resource.addCourseGauge(resource.getGauge());
		}
		
		gaugeType = resource.getGrooveGauge().getType();

		resultChart = createResultChart(resource.getSongdata(), resource.getBMSModel());
		prepareLevelReview();
		loadSkin(SkinType.RESULT);
		updateScoreRuleNotice();
	}

	void prepareScoreDisplay() {
		scoringDistributions.clear();
		scoreDisplayIndex = 0;
		alternateScores = List.of();
		var model = resource.getBMSModel();
		if (model == null || resource.getScoreData() == null
				|| resource.getPlayMode() == null || resource.getPlayMode().mode != BMSPlayerMode.Mode.PLAY
				|| resource.getCourseBMSModels() != null) {
			return;
		}
		alternateScores = resource.getAlternateResultScores().stream()
				.filter(candidate -> Objects.equals(candidate.score().getSha256(), model.getSHA256()))
				.filter(candidate -> !candidate.ruleId().equals(resource.getPrimaryScoringSelection().effective().id()))
				.map(SecondaryScoreDisplay::new).toList();
	}

	public boolean canSwitchScoreDisplay() {
		return !alternateScores.isEmpty();
	}

	public boolean isShowingSecondaryScore() {
		return scoreDisplayIndex != 0;
	}

	public boolean isShowingLr2Score() {
		var display = secondaryDisplay();
		return display != null ? SecondaryScoringRule.LR2.id().equals(display.result.ruleId())
				: resource.getPrimaryScoringSelection().effective() == SecondaryScoringRule.LR2;
	}

	public String getDisplayScoringRuleName() {
		var display = secondaryDisplay();
		return display != null ? display.result.ruleName() : resource.getPrimaryScoringSelection().effective().displayName();
	}

	public String getPrimaryScoringDetails() {
		var selection = resource.getPrimaryScoringSelection();
		return selection.requested().displayName() + " → " + selection.effective().displayName()
				+ (selection.reason().isEmpty() ? "" : " (" + selection.reason() + ")");
	}

	public String getDisplayScoringSummary() {
		String[] gauges = {"ASSIST EASY", "EASY", "NORMAL", "HARD", "EX-HARD", "HAZARD", "CLASS", "EX-CLASS", "EX-HARD CLASS"};
		int gauge = getGaugeType();
		String gaugeName = currentTimingRuleId().equals(SecondaryScoringRule.ANGOLMOIS.id()) ? "NORMAL"
				: gauge >= 0 && gauge < gauges.length ? gauges[gauge] : "GAUGE";
		String notice = (isShowingSecondaryScore() ? "Secondary: " : "Primary: ") + getDisplayScoringRuleName() + " / " + gaugeName;
		var score = getDisplayScore();
		if (score != null && score.getNativeScore() >= 0) notice += " / SCORE " + score.getNativeScore() + " / " + score.getNativeMaxScore();
		return notice;
	}

	private void updateScoreRuleNotice() {
		var renderer = main.getMessageRenderer();
		if (renderer == null) return;
		clearScoreRuleNotice();
		scoreRuleNotice = renderer.addMessage(getDisplayScoringSummary(), Color.CYAN, 1);
	}

	private SecondaryScoreDisplay secondaryDisplay() {
		return isShowingSecondaryScore() ? alternateScores.get(scoreDisplayIndex - 1) : null;
	}

	public PlayerResource.GaugeHistory getDisplayGaugeHistory() {
		var display = secondaryDisplay();
		return display != null ? display.result.gaugeHistory() : null;
	}
	@Override public bms.player.beatoraja.play.AbstractGrooveGauge getDisplayGauge() {
		var history = getDisplayGaugeHistory();
		return history != null ? history.gauge() : super.getDisplayGauge();
	}
	@Override public FloatArray[] getDisplayGaugeSamples() {
		var history = getDisplayGaugeHistory();
		return history != null ? history.values() : super.getDisplayGaugeSamples();
	}

	@Override
	public int getGaugeType() {
		var display = secondaryDisplay();
		return display != null && display.result.gaugeHistory() != null ? display.gaugeType : super.getGaugeType();
	}

	@Override
	public float getDisplayGaugeValue() {
		var history = getDisplayGaugeHistory();
		if (history == null) return super.getDisplayGaugeValue();
		return history.values()[getGaugeType()].peek();
	}

	private void changeGaugeGraph() {
		if (currentTimingRuleId().equals(SecondaryScoringRule.ANGOLMOIS.id())) return;
		int type = getGaugeType();
		int next = type >= GrooveGauge.ASSISTEASY && type <= GrooveGauge.HAZARD
				? (type + 1) % 6 : (type - 5) % 3 + 6;
		var display = secondaryDisplay();
		if (display != null) {
			var history = display.result.gaugeHistory();
			if (history != null) display.gaugeType = next < history.values().length ? next : 0;
		} else {
			gaugeType = next < getDisplayGaugeSamples().length ? next : 0;
		}
		updateScoreRuleNotice();
	}

	public ScoreDataProperty getPrimaryScoreDataProperty() {
		return super.getScoreDataProperty();
	}

	@Override
	public ScoreDataProperty getScoreDataProperty() {
		var display = secondaryDisplay();
		return display != null ? display.properties : getPrimaryScoreDataProperty();
	}

	@Override
	public ScoreData getDisplayScore() {
		var display = secondaryDisplay();
		return display != null ? display.result.score() : getNewScore();
	}

	@Override public long getAverageDuration() {
		var score = getDisplayScore();
		return score != null ? score.getAvgjudge() : Long.MAX_VALUE;
	}

	@Override
	public ScoreData getDisplayOldScore() {
		var display = secondaryDisplay();
		return display != null ? display.previousBest : getOldScore();
	}

	void prepareLevelReview() {
		levelReviewTarget = null;
		if (resource.getPlayMode() == null || resource.getPlayMode().mode != BMSPlayerMode.Mode.PLAY
				|| resource.getCourseBMSModels() != null || resource.getScoreData() == null
				|| resource.getScoreData().getRule() == BMSPlayerRule.LunaticRave2) {
			return;
		}
		var song = resource.getSongdata();
		var model = resource.getBMSModel();
		var database = main.getSongDatabase();
		if (song == null || model == null || database == null || song.getSha256().isEmpty()
				|| !song.getSha256().equals(model.getSHA256())) {
			return;
		}
		IRChartData chart = resultChart != null && song.getSha256().equals(resultChart.sha256)
				? resultChart : createResultChart(song, model);
		int lnmode = chart.hasUndefinedLN ? chart.lntype : 0;
		if (lnmode < 0 || lnmode > 2) return;
		song.setSongReview(database.getSongReview(song.getSha256()));
		levelReviewTarget = new LevelReviewTarget(song, lnmode);
	}

	static IRChartData createResultChart(SongData song, BMSModel model) {
		if (song == null) return null;
		// A random branch can replace the model in SongData for this play.
		BMSModel played = song.getBMSModel() != null ? song.getBMSModel() : model;
		return new IRChartData(song, played != null ? played.getLntype() : 0);
	}

	public LevelReviewTarget getEditableLevelReview() {
		return state != STATE_IR_PROCESSING && !timer.isTimerOn(TIMER_FADEOUT) ? levelReviewTarget : null;
	}

	/** IR always uses the independently judged beatoraja result and its own previous best. */
	record IRScoreSelection(ScoreData score, ScoreData previousBest, boolean completed) {
		boolean shouldSend(int policy) {
			return switch (policy) {
				case IRConfig.IR_SEND_COMPLETE_SONG -> completed;
				case IRConfig.IR_SEND_UPDATE_SCORE -> score.getExscore() > previousBest.getExscore()
						|| score.getClear() > previousBest.getClear() || score.getCombo() > previousBest.getCombo()
						|| score.getMinbp() < previousBest.getMinbp();
				default -> true;
			};
		}
	}

	IRScoreSelection selectIRScore() {
		ScoreData score = getNewScore();
		ScoreData previous = oldscore;
		FloatArray[] gauges = resource.getGauge();
		int gauge = resource.getGrooveGauge().getType();
		boolean completed = true;
		if (!resource.getPrimaryScoringSelection().isStandard()) {
			var standard = resource.getAlternateResultScores().stream()
					.filter(candidate -> SecondaryScoringRule.BEATORAJA.id().equals(candidate.ruleId()))
					.filter(candidate -> candidate.score() != null && Objects.equals(candidate.score().getSha256(), resource.getBMSModel().getSHA256()))
					.findFirst().orElse(null);
			if (standard == null) return null;
			score = standard.score();
			previous = standard.previousBest();
			gauges = standard.gaugeHistory() != null ? standard.gaugeHistory().values() : null;
			gauge = standard.gaugeHistory() != null && standard.gaugeHistory().gauge() != null
					? standard.gaugeHistory().gauge().getType() : score.getGauge();
			completed = standard.completed();
		}
		if (score == null) return null;
		FloatArray history = gauges != null && gauge >= 0 && gauge < gauges.length ? gauges[gauge] : null;
		return new IRScoreSelection(score, previous != null ? previous : new ScoreData(),
				completed && history != null && history.size > 0 && history.peek() > 0);
	}
	
	public void prepare() {
		state = STATE_OFFLINE;
		final ScoreData newscore = getNewScore();
		final IRScoreSelection irScore = selectIRScore();

		ranking = resource.getRankingData() != null
				&& resource.getCourseBMSModels() == null ? resource.getRankingData() : new RankingData();
		rankingOffset = 0;
		// TODO スコアハッシュがあり、有効期限が切れていないものを送信する？
		final IRStatus[] ir = main.getIRStatus();
		if (ir.length > 0 && resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY && irScore != null) {
			state = STATE_IR_PROCESSING;
			
        	for(IRStatus irc : ir) {
				boolean send = resource.isUpdateScore() && irScore.shouldSend(irc.config.getIrsend());
    			
    			if(send) {
					irSendStatus.add(new IRSendStatus(irc.connection, resultChart, irScore.score()));
    			}
        	}
			
			Thread irprocess = new Thread(() -> {
				int irsend = 0;
				boolean succeed = true;
				List<IRSendStatus> removeIrSendStatus = new ArrayList<>();

				for (IRSendStatus irc : irSendStatus) {
					try {
						if (irsend == 0) {
							timer.switchTimer(TIMER_IR_CONNECT_BEGIN, true);
						}
						irsend++;
						succeed &= irc.send();
						if (irc.retry < 0 || irc.retry > main.getConfig().getIrSendCount()) {
							removeIrSendStatus.add(irc);
						}
					} catch (Exception e) {
						Logger.getGlobal().warning("IR送信時の例外:" + e.getMessage());
						e.printStackTrace();
						// remove from queue
						removeIrSendStatus.add(irc);
					}
				}
				irSendStatus.removeAll(removeIrSendStatus);

				if(irsend > 0) {
					timer.switchTimer(succeed ? TIMER_IR_CONNECT_SUCCESS : TIMER_IR_CONNECT_FAIL, true);
					try {
						IRResponse<bms.player.beatoraja.ir.IRScoreData[]> response = ir[0].connection.getPlayData(null, resultChart);
						if(response.isSucceeded()) {
								ranking.updateScore(ir[0].player, main.getRivalDataAccessor(), response.getData(),
										irScore.score().getExscore() > irScore.previousBest().getExscore() ? irScore.score() : irScore.previousBest());
							rankingOffset = ranking.getRank() > 10 ? ranking.getRank() - 5 : 0;
							Logger.getGlobal().info("IRからのスコア取得成功 : " + response.getMessage());
						} else {
							Logger.getGlobal().warning("IRからのスコア取得失敗 : " + response.getMessage());
						}
					} catch (Exception e) {
						Logger.getGlobal().warning("IRからのスコア取得時例外:" + e.getMessage());
						e.printStackTrace();
					}
				}
				state = STATE_IR_FINISHED;
            });
			irprocess.start();
		}

		final ScoreData cscore = resource.getCourseScoreData();
		play(newscore.getClear() != Failed.id && (cscore == null || cscore.getClear() != Failed.id) ? RESULT_CLEAR : RESULT_FAIL
				,resource.getConfig().getAudioConfig().isLoopResultSound());
	}

	public void shutdown() {
		stop(RESULT_CLEAR);
		stop(RESULT_FAIL);
		stop(RESULT_CLOSE);
		scoreDisplayIndex = 0;
		alternateScores = List.of();
		resource.setAlternateResultScores(List.of());
		clearScoreRuleNotice();
	}

	public void render() {
		long time = timer.getNowTime();
		timer.switchTimer(TIMER_RESULTGRAPH_BEGIN, true);
		timer.switchTimer(TIMER_RESULTGRAPH_END, true);

		if (((MusicResultSkin) getSkin()).getRankTime() == 0) {
			timer.switchTimer(TIMER_RESULT_UPDATESCORE, true);
		}
		if (time > getSkin().getInput()) {
			timer.switchTimer(TIMER_STARTINPUT, true);
		}

		if (timer.isTimerOn(TIMER_FADEOUT)) {
			if (timer.getNowTime(TIMER_FADEOUT) > getSkin().getFadeout()) {
				main.getAudioProcessor().stop((Note) null);

				final BMSPlayerInputProcessor input = main.getInputProcessor();
				main.getInputProcessor().resetAllKeyChangedTime();

				if (resource.getCourseBMSModels() != null) {
					if (resource.getGauge()[resource.getGrooveGauge().getType()]
							.get(resource.getGauge()[resource.getGrooveGauge().getType()].size - 1) <= 0) {
						if (resource.getCourseScoreData() != null) {
							// 未達曲のノーツをPOORとして加算
							final Array<FloatArray[]> coursegauge = resource.getCourseGauge();
							final int cg = resource.getCourseBMSModels().length;
							for (int i = 0; i < cg; i++) {
								if (coursegauge.size <= i) {
									resource.getCourseScoreData().setMinbp(resource.getCourseScoreData().getMinbp()
											+ resource.getCourseBMSModels()[i].getTotalNotes());
									resource.getCourseScoreData().setTotalDuration(resource.getCourseScoreData().getTotalDuration()
											+ 1000000L * resource.getCourseBMSModels()[i].getTotalNotes());
								}
							}
							// 不合格リザルト
							main.changeState(MainStateType.COURSERESULT);
						} else {
							// コーススコアがない場合は選曲画面へ
							main.changeState(MainStateType.MUSICSELECT);
						}
					} else if (resource.nextCourse()) {
						RankingData songrank = main.getRankingDataCache().get(resource.getSongdata(), main.getPlayerConfig().getLnmode());
						if(main.getIRStatus().length > 0 && songrank == null) {
							songrank = new RankingData();
							main.getRankingDataCache().put(resource.getSongdata(), main.getPlayerConfig().getLnmode(), songrank);
						}
						resource.setRankingData(songrank);

						main.changeState(MainStateType.PLAY);
					} else {
						// 合格リザルト
						main.changeState(MainStateType.COURSERESULT);
					}
				} else {
					resource.getPlayerConfig().setGauge(resource.getOrgGaugeOption());
					ResultKeyProperty.ResultKey key = null;
					for (int i = 0; i < property.getAssignLength(); i++) {
						if (property.getAssign(i) == ResultKeyProperty.ResultKey.REPLAY_DIFFERENT && input.getKeyState(i)) {
							key = ResultKeyProperty.ResultKey.REPLAY_DIFFERENT;
							break;
						}
						if (property.getAssign(i) == ResultKeyProperty.ResultKey.REPLAY_SAME && input.getKeyState(i)) {
							key = ResultKeyProperty.ResultKey.REPLAY_SAME;
							break;
						}
					}
					if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY
							&& key == ResultKeyProperty.ResultKey.REPLAY_DIFFERENT) {
						Logger.getGlobal().info("オプションを変更せずリプレイ");
						// オプションを変更せず同じ譜面でリプレイ
						resource.getReplayData().randomoptionseed = -1;
						resource.reloadBMSFile();
						main.changeState(MainStateType.PLAY);
					} else if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY
							&& key == ResultKeyProperty.ResultKey.REPLAY_SAME) {
						// 同じ譜面でリプレイ
						if(resource.isUpdateScore()) {
							Logger.getGlobal().info("同じ譜面でリプレイ");							
						} else {
							Logger.getGlobal().info("アシストモード時は同じ譜面でリプレイできません");
							resource.getReplayData().randomoptionseed = -1;
						}
						resource.reloadBMSFile();
						main.changeState(MainStateType.PLAY);
					} else {
						main.changeState(MainStateType.MUSICSELECT);
					}
				}
			}
		} else {
			if (time > getSkin().getScene()) {
				timer.switchTimer(TIMER_FADEOUT, true);
				if (getSound(RESULT_CLOSE) != null) {
					stop(RESULT_CLEAR);
					stop(RESULT_FAIL);
					play(RESULT_CLOSE);
				}
			}
		}

	}

	public void input() {
		final BMSPlayerInputProcessor inputProcessor = main.getInputProcessor();
		final boolean switchPressed = canSwitchScoreDisplay() && inputProcessor.isActivated(KeyboardShortcut.SWITCH_RESULT_SCORE);
		if (getSkin() != null && getSkin().isTextInputActive()) return;
		super.input();
		long time = timer.getNowTime();

		if (!timer.isTimerOn(TIMER_FADEOUT) && timer.isTimerOn(TIMER_STARTINPUT)) {
			if (time > getSkin().getInput()) {
				if (switchPressed) {
					scoreDisplayIndex = (scoreDisplayIndex + 1) % (alternateScores.size() + 1);
					updateScoreRuleNotice();
				}
				boolean ok = false;
				for (int i = 0; i < property.getAssignLength(); i++) {
					if (property.getAssign(i) == ResultKeyProperty.ResultKey.CHANGE_GRAPH && inputProcessor.getKeyState(i) && inputProcessor.resetKeyChangedTime(i)) {
						changeGaugeGraph();
					} else if (property.getAssign(i) != null && inputProcessor.getKeyState(i) && inputProcessor.resetKeyChangedTime(i)) {
						ok = true;
					}
				}

				if (inputProcessor.isControlKeyPressed(ControlKeys.ESCAPE) || inputProcessor.isControlKeyPressed(ControlKeys.ENTER)) {
					ok = true;
				}

				if (resource.getScoreData() == null || ok) {
					if (((MusicResultSkin) getSkin()).getRankTime() != 0
							&& !timer.isTimerOn(TIMER_RESULT_UPDATESCORE)) {
						timer.switchTimer(TIMER_RESULT_UPDATESCORE, true);
					} else if (state == STATE_OFFLINE || state == STATE_IR_FINISHED) {
						timer.switchTimer(TIMER_FADEOUT, true);
						if (getSound(RESULT_CLOSE) != null) {
							stop(RESULT_CLEAR);
							stop(RESULT_FAIL);
							play(RESULT_CLOSE);
						}
					}
				}

				if(inputProcessor.isControlKeyPressed(ControlKeys.NUM1)) {
					saveReplayData(0);				
				} else if(inputProcessor.isControlKeyPressed(ControlKeys.NUM2)) {
					saveReplayData(1);				
				} else if(inputProcessor.isControlKeyPressed(ControlKeys.NUM3)) {
					saveReplayData(2);				
				} else if(inputProcessor.isControlKeyPressed(ControlKeys.NUM4)) {
					saveReplayData(3);				
				}

				if(inputProcessor.isActivated(KeyboardShortcut.OPEN_IR)) {
					this.executeEvent(EventType.open_ir);
				}
			}
		}
	}

	public void saveReplayData(int index) {
		if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY && resource.getCourseBMSModels() == null
				&& resource.getScoreData() != null) {
			if (saveReplay[index] != ReplayStatus.SAVED && resource.isUpdateScore()) {
				ReplayData rd = resource.getReplayData();
				if (main.getPlayDataAccessor().wrireReplayData(rd, resource.getBMSModel(),
						resource.getPlayerConfig().getLnmode(), index)) {
					saveReplay[index] = ReplayStatus.SAVED;
					main.obsReplaySaved();
				}
			}
		}
	}

	private void updateScoreDatabase() {
		ScoreData newscore = resource.getScoreData();
		if (newscore == null) {
			if (resource.getCourseScoreData() != null) {
				resource.getCourseScoreData()
						.setMinbp(resource.getCourseScoreData().getMinbp() + resource.getBMSModel().getTotalNotes());
				resource.getCourseScoreData().setClear(Failed.id);
			}
			return;
		}
		if (!resource.getPrimaryScoringSelection().isStandard() && resource.getCourseBMSModels() == null) {
			var data = main.getPlayDataAccessor();
			var model = resource.getBMSModel();
			int mode = model.containsUndefinedLongNote() ? model.getLntype() : 0;
			var rule = resource.getPrimaryScoringSelection().effective();
			var previous = data.readRuleBest(rule, model.getSHA256(), mode);
			oldscore = previous != null ? previous : new ScoreData();
			getPrimaryScoreDataProperty().setTargetScore(oldscore.getRuleScore(), null,
					resource.getTargetScoreData() != null ? resource.getTargetScoreData().getRuleScore() : 0, null,
					newscore.getNotes(), newscore.getRuleMaxScore());
			getPrimaryScoreDataProperty().update(newscore);
			if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY) {
				newscore.setTrophy(ScoreTrophies.merge(null, resource.isUpdateScore() ? ScoreTrophies.forAttempt(newscore) : Set.of()));
				var counts = new ArrayList<Integer>(12);
				for (int i = 0; i < 6; i++) { counts.add(newscore.getJudgeCount(i, true)); counts.add(newscore.getJudgeCount(i, false)); }
				data.writeRuleAttempt(rule, new PlayDataAccessor.ScoreAttempt(model.getSHA256(), mode,
						newscore.getClear() == Failed.id ? PlayDataAccessor.ScoreOutcome.FAILED : PlayDataAccessor.ScoreOutcome.FINISHED,
						newscore.getNotes(), newscore.getPassnotes(), newscore.getCombo(), newscore.getMinbp(),
						ClearType.getClearTypeByID(newscore.getClear()), newscore.getGauge(),
						resource.getGrooveGauge().getValue(), counts, newscore.getNativeScore(), newscore.getNativeMaxScore()).withMetadata(newscore), resource.isUpdateScore());
				for (var alternate : resource.getAlternateResultScores()) {
					if (alternate.ruleId().equals(SecondaryScoringRule.BEATORAJA.id())) {
						alternate.score().setPlaytime(newscore.getPlaytime());
						alternate.score().setOption(newscore.getOption());
						alternate.score().setSeed(newscore.getSeed());
						alternate.score().setDeviceType(newscore.getDeviceType());
						alternate.score().setSkin(newscore.getSkin());
						alternate.score().setAssist(newscore.getAssist());
						alternate.score().setJudgeAlgorithm(bms.player.beatoraja.play.JudgeAlgorithm.valueOf(
								resource.getPlayerConfig().getPlayConfig(model.getMode()).getPlayconfig().getJudgetype()));
						if (alternate.score().getClear() != Failed.id && resource.getAssist() > 0) {
							alternate.score().setClear(resource.getAssist() == 1 ? LightAssistEasy.id : AssistEasy.id);
						}
						data.writeScoreData(alternate.score(), model, resource.getPlayerConfig().getLnmode(), resource.isUpdateScore());
					}
				}
			}
			return;
		}
		final ScoreData oldsc = main.getPlayDataAccessor().readScoreData(resource.getBMSModel(),
				resource.getPlayerConfig().getLnmode());
		oldscore = oldsc != null ? oldsc : new ScoreData();

		getScoreDataProperty().setTargetScore(oldscore.getExscore(), resource.getTargetScoreData() != null ? resource.getTargetScoreData().getExscore() : 0, resource.getBMSModel().getTotalNotes());
		getScoreDataProperty().update(newscore);
		// duration average
		int count = 0;
		avgduration = newscore.getAvgjudge();
		timingDistribution.init();
		BMSModel model = resource.getBMSModel();
		final int lanes = model.getMode().key;
		for (TimeLine tl : model.getAllTimeLines()) {
			for (int i = 0; i < lanes; i++) {
				Note n = tl.getNote(i);
				if (n != null && !((model.getLnmode() == 1 || (model.getLnmode() == 0 && model.getLntype() == BMSModel.LNTYPE_LONGNOTE))
						&& n instanceof LongNote && ((LongNote) n).isEnd())) {
					int state = n.getState();
					int time = n.getPlayTime();
					if (state >= 1) {
						count++;
						timingDistribution.add(time);
					}
				}
			}
		}
		timingDistribution.statisticValueCalcuate();

		// コースモードの場合はコーススコアに加算・累積する
		if (resource.getCourseBMSModels() != null) {
			if (resource.getScoreData().getClear() == Failed.id) {
				resource.getScoreData().setClear(NoPlay.id);
			}
			ScoreData cscore = resource.getCourseScoreData();
			if (cscore == null) {
				cscore = new ScoreData();
				cscore.setMinbp(0);
				cscore.setPlaytime(0);
				int notes = 0;
				for (BMSModel mo : resource.getCourseBMSModels()) {
					notes += mo.getTotalNotes();
				}
				cscore.setNotes(notes);
				cscore.setDeviceType(newscore.getDeviceType());
				cscore.setOption(newscore.getOption());
				cscore.setJudgeAlgorithm(newscore.getJudgeAlgorithm());
				cscore.setRule(newscore.getRule());
				resource.setCourseScoreData(cscore);
			}
			cscore.setPassnotes(cscore.getPassnotes() + newscore.getPassnotes());
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
			cscore.setTotalDuration(cscore.getTotalDuration() + newscore.getTotalDuration());
			cscore.addPlaytime(newscore.getPlaytime());
			if (resource.getGauge()[resource.getGrooveGauge().getType()].get(resource.getGauge()[resource.getGrooveGauge().getType()].size - 1) > 0) {
				if (resource.getAssist() > 0) {
					if(resource.getAssist() == 1 && cscore.getClear() != ClearType.AssistEasy.id) cscore.setClear(ClearType.LightAssistEasy.id);
					else cscore.setClear(ClearType.AssistEasy.id);
				} else if(!(cscore.getClear() == ClearType.LightAssistEasy.id || cscore.getClear() == ClearType.AssistEasy.id)) {
					if(resource.getCourseIndex() == resource.getCourseBMSModels().length - 1) {
						int courseTotalNotes = 0;
						for(int i = 0; i < resource.getCourseBMSModels().length; i++) {
							courseTotalNotes += resource.getCourseBMSModels()[i].getTotalNotes();
						}
						if (courseTotalNotes == resource.getMaxcombo()) {
							if (cscore.getJudgeCount(2) == 0) {
								if (cscore.getJudgeCount(1) == 0) {
									cscore.setClear(ClearType.Max.id);
								} else {
									cscore.setClear(ClearType.Perfect.id);
								}
							} else {
								cscore.setClear(ClearType.FullCombo.id);
							}
						} else {
							cscore.setClear(resource.getGrooveGauge().getClearType().id);
						}
					}
				}
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

		if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY
				&& newscore.getRule() != BMSPlayerRule.LunaticRave2) {
			main.getPlayDataAccessor().writeScoreData(resource.getScoreData(), resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), resource.isUpdateScore());
		} else if (resource.getPlayMode().mode != BMSPlayerMode.Mode.PLAY) {
			Logger.getGlobal().info("プレイモードが" + resource.getPlayMode().mode.name() + "のため、スコア登録はされません");
		}

	}

	public int getJudgeCount(int judge, boolean fast) {
		final ScoreData score = getDisplayScore();
		return score != null ? score.getJudgeCount(judge, fast) : 0;
	}

	@Override
	public void dispose() {
		clearScoreRuleNotice();
		super.dispose();
	}

	private void clearScoreRuleNotice() {
		if (scoreRuleNotice != null) {
			scoreRuleNotice.stop();
			scoreRuleNotice = null;
		}
	}

	public int getTotalNotes() {
		var score = getDisplayScore();
		return score != null ? score.getNotes() : resource.getBMSModel().getTotalNotes();
	}

	public ScoreData getNewScore() {
		return resource.getScoreData();
	}

	static class IRSendStatus {
		public final IRConnection ir;
		public final IRChartData chart;
		public final ScoreData score;
		public int retry = 0;
		
		public IRSendStatus(IRConnection ir, IRChartData chart, ScoreData score) {
			this.ir = ir;
			this.chart = chart;
			this.score = score;
		}
		
		public boolean send() {
			if (retry < 0) return retry == -255;
			Logger.getGlobal().info("IRへスコア送信中 : " + chart.title);
            IRResponse<Object> send1 = ir.sendPlayData(chart, new bms.player.beatoraja.ir.IRScoreData(score));
            if(send1.isSucceeded()) {
                Logger.getGlobal().info("IRスコア送信完了 : " + chart.title);
                retry = -255;
                return true;
            } else {
                Logger.getGlobal().warning("IRスコア送信失敗 : " + send1.getMessage());
                retry = IRSubmissionResponse.isOutcomeUnknown(send1) ? -1 : retry + 1;
                if (retry == -1) Logger.getGlobal().warning("IR送信結果が不明なため自動再送を中止します");
                return false;
            }

		}
	}
}
