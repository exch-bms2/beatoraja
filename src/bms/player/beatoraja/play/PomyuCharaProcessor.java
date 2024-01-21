package bms.player.beatoraja.play;

import bms.player.beatoraja.TimerManager;

import static bms.player.beatoraja.skin.SkinProperty.*;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_DANCE;

public class PomyuCharaProcessor {

    /**
     * ぽみゅキャラの各モーションの1周期の時間  0:1P_NEUTRAL 1:1P_FEVER 2:1P_GREAT 3:1P_GOOD 4:1P_BAD 5:2P_NEUTRAL 6:2P_GREAT 7:2P_BAD
     */
    private int PMcharaTime[] = {1,1,1,1,1,1,1,1};
    /**
     * PMS キャラ用 ニュートラルモーション開始時の処理済ノート数{1P,2P} (ニュートラルモーション一周時に変化がなければニュートラルモーションを継続するため)
     */
    private int[] PMcharaLastnotes = {0, 0};
    /**
     * PMS キャラ用 判定
     */
    int PMcharaJudge = 0;

    public void init() {
        PMcharaLastnotes[0] = 0;
        PMcharaLastnotes[1] = 0;
        PMcharaJudge = 0;
    }

    public int getPMcharaTime(int index) {
        if(index < 0 || index >= PMcharaTime.length) return 1;
        return PMcharaTime[index];
    }

    public void setPMcharaTime(int index, int value) {
        if(index >= 0 && index < PMcharaTime.length && value >= 1) {
            this.PMcharaTime[index] = value;
        }
    }

    public void updateTimer(BMSPlayer player) {
        final TimerManager timer = player.timer;
        if(timer.isTimerOn(TIMER_PM_CHARA_1P_NEUTRAL) && timer.getNowTime(TIMER_PM_CHARA_1P_NEUTRAL) >= getPMcharaTime(TIMER_PM_CHARA_1P_NEUTRAL - TIMER_PM_CHARA_1P_NEUTRAL) && timer.getNowTime(TIMER_PM_CHARA_1P_NEUTRAL) % getPMcharaTime(TIMER_PM_CHARA_1P_NEUTRAL - TIMER_PM_CHARA_1P_NEUTRAL) < 17) {
            if(PMcharaLastnotes[0] != player.getPastNotes() && PMcharaJudge > 0) {
                if(PMcharaJudge == 1 || PMcharaJudge == 2) {
                    if(player.getGauge().getGauge().isMax()) timer.setTimerOn(TIMER_PM_CHARA_1P_FEVER);
                    else timer.setTimerOn(TIMER_PM_CHARA_1P_GREAT);
                } else if(PMcharaJudge == 3) timer.setTimerOn(TIMER_PM_CHARA_1P_GOOD);
                else timer.setTimerOn(TIMER_PM_CHARA_1P_BAD);
                timer.setTimerOff(TIMER_PM_CHARA_1P_NEUTRAL);
            }
        }
        if(timer.isTimerOn(TIMER_PM_CHARA_2P_NEUTRAL) && timer.getNowTime(TIMER_PM_CHARA_2P_NEUTRAL) >= getPMcharaTime(TIMER_PM_CHARA_2P_NEUTRAL - TIMER_PM_CHARA_1P_NEUTRAL) && timer.getNowTime(TIMER_PM_CHARA_2P_NEUTRAL) % getPMcharaTime(TIMER_PM_CHARA_2P_NEUTRAL - TIMER_PM_CHARA_1P_NEUTRAL) < 17) {
            if(PMcharaLastnotes[1] != player.getPastNotes() && PMcharaJudge > 0) {
                if(PMcharaJudge >= 1 && PMcharaJudge <= 3) timer.setTimerOn(TIMER_PM_CHARA_2P_BAD);
                else timer.setTimerOn(TIMER_PM_CHARA_2P_GREAT);
                timer.setTimerOff(TIMER_PM_CHARA_2P_NEUTRAL);
            }
        }
        for(int i = TIMER_PM_CHARA_1P_FEVER; i <= TIMER_PM_CHARA_2P_BAD; i++) {
            if(i != TIMER_PM_CHARA_2P_NEUTRAL && timer.isTimerOn(i) && timer.getNowTime(i) >= getPMcharaTime(i - TIMER_PM_CHARA_1P_NEUTRAL)) {
                if(i <= TIMER_PM_CHARA_1P_BAD) {
                    timer.setTimerOn(TIMER_PM_CHARA_1P_NEUTRAL);
                    PMcharaLastnotes[0] = player.getPastNotes();
                } else {
                    timer.setTimerOn(TIMER_PM_CHARA_2P_NEUTRAL);
                    PMcharaLastnotes[1] = player.getPastNotes();
                }
                timer.setTimerOff(i);
            }
        }
        timer.switchTimer(TIMER_PM_CHARA_DANCE, true);

    }
}
