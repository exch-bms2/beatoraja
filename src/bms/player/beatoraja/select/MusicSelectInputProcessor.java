package bms.player.beatoraja.select;

import bms.player.beatoraja.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyCommand;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.select.MusicSelectKeyProperty.MusicSelectKey;
import bms.player.beatoraja.select.bar.*;
import bms.player.beatoraja.skin.property.EventFactory;
import bms.player.beatoraja.skin.property.EventFactory.EventType;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import java.util.*;
import java.util.stream.Collectors;

import static bms.player.beatoraja.select.MusicSelector.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import static bms.player.beatoraja.select.MusicSelectKeyProperty.MusicSelectKey.*;

/**
 * 選曲の入力処理用クラス
 *
 * @author exch
 */
public class MusicSelectInputProcessor {

    /**
     * バー移動中のカウンタ
     */
    private long duration;
    /**
     * バーの移動方向
     */
    private int angle;

    private final int durationlow;
    private final int durationhigh;

    /**
     * バー移動中のカウンタ（アナログスクロール）
     */
    private int analogScrollBuffer = 0;
    private final int analogTicksPerScroll;


    boolean isOptionKeyPressed = false;
    boolean isOptionKeyReleased = false;

    // ノーツ表示時間変更のカウンタ
    private long timeChangeDuration;
    private int countChangeDuration;

    private final MusicSelector select;

    public MusicSelectInputProcessor(MusicSelector select) {
        this.select = select;

        durationlow = select.main.getConfig().getScrollDurationLow();
        durationhigh = select.main.getConfig().getScrollDurationHigh();
        analogTicksPerScroll = select.main.getConfig().getAnalogTicksPerScroll();
    }

    public void input() {
    	final MainController main = select.main;
        final BMSPlayerInputProcessor input = main.getInputProcessor();
        final PlayerResource resource = main.getPlayerResource();
        final PlayerConfig config = resource.getPlayerConfig();
        final BarRenderer bar = select.getBarRender();
        final Bar current = bar.getSelected();

        boolean[] numberstate = input.getNumberState();
        long[] numtime = input.getNumberTime();
        if (numberstate[0] && numtime[0] != 0) {
            // 検索用ポップアップ表示。これ必要？
            numtime[0] = 0;
            Gdx.input.getTextInput(new Input.TextInputListener() {
                @Override
                public void input(String text) {
                    if (text.length() > 1) {
                        bar.addSearch(new SearchWordBar(select, text));
                        bar.updateBar(null);
                    }
                }

                @Override
                public void canceled() {
                }
            }, "Search", "", "Search bms title");
        }

        if (numberstate[1] && numtime[1] != 0) {
            // KEYフィルターの切り替え
            numtime[1] = 0;
            select.executeEvent(EventType.mode);
        }
        if (numberstate[2] && numtime[2] != 0) {
            // ソートの切り替え
            numtime[2] = 0;
            select.executeEvent(EventType.sort);
        }
        if (numberstate[3] && numtime[3] != 0) {
            // LNモードの切り替え
            numtime[3] = 0;
            select.executeEvent(EventType.lnmode);
        }

        boolean[] keystate = input.getKeystate();
        long[] keytime = input.getTime();
        boolean[] cursor = input.getCursorState();
        long[] cursortime = input.getCursorTime();

        final MusicSelectKeyProperty property = MusicSelectKeyProperty.values()[config.getMusicselectinput()];

        if(!input.startPressed() && !input.isSelectPressed() && !input.getNumberState()[5]){
            //オプションキー入力なし
            isOptionKeyReleased = true;
            if(isOptionKeyPressed) {
                isOptionKeyPressed = false;
                select.play(SOUND_OPTIONCLOSE);
            }
        }

        if (numberstate[4] && numtime[4] != 0
                || (!input.startPressed() && !input.isSelectPressed() && !input.getNumberState()[5] && property.isPressed(keystate, keytime, NEXT_REPLAY, true))) {
            // change replay
            numtime[4] = 0;
            select.execute(MusicSelectCommand.NEXT_REPLAY);
        }
        if (input.startPressed() && !input.isSelectPressed()) {
            bar.resetInput();
            // show play option
            select.setPanelState(1);
            if(isOptionKeyReleased) {
                isOptionKeyPressed = true;
                isOptionKeyReleased = false;
                select.play(SOUND_OPTIONOPEN);
            }
            if (property.isPressed(keystate, keytime, OPTION1_DOWN, true)) {
                select.executeEvent(EventType.option1p, 1);
            }
            if (property.isPressed(keystate, keytime, OPTION1_UP, true)) {
                select.executeEvent(EventType.option1p, -1);
            }
            if (property.isPressed(keystate, keytime, GAUGE_DOWN, true)) {
                select.executeEvent(EventType.gauge1p, 1);
            }
            if (property.isPressed(keystate, keytime, GAUGE_UP, true)) {
                select.executeEvent(EventType.gauge1p, -1);
            }
            if (property.isPressed(keystate, keytime, OPTIONDP_DOWN, true)) {
                select.executeEvent(EventType.optiondp, 1);
            }
            if (property.isPressed(keystate, keytime, OPTIONDP_UP, true)) {
                select.executeEvent(EventType.optiondp, -1);
            }
            if (property.isPressed(keystate, keytime, OPTION2_DOWN, true)) {
                select.executeEvent(EventType.option2p, 1);
            }
            if (property.isPressed(keystate, keytime, OPTION2_UP, true)) {
                select.executeEvent(EventType.option2p, -1);
            }
            if (property.isPressed(keystate, keytime, HSFIX_DOWN, true)) {
                select.executeEvent(EventType.hsfix, 1);
            }
            if (property.isPressed(keystate, keytime, HSFIX_UP, true)) {
                select.executeEvent(EventType.hsfix, -1);
            }

            // song bar scroll on mouse wheel
            int mov = -input.getScroll();
            input.resetScroll();

            analogScrollBuffer += property.getAnalogChange(input, TARGET_UP) - property.getAnalogChange(input, TARGET_DOWN);
            mov += analogScrollBuffer/analogTicksPerScroll;
            analogScrollBuffer %= analogTicksPerScroll;

            // song bar scroll
            if (property.isNonAnalogPressed(input, keystate, keytime, TARGET_UP, false) || cursor[1]) {
                long l = System.currentTimeMillis();
                if (duration == 0) {
                    mov = 1;
                    duration = l + durationlow;
                    angle = durationlow;
                }
                if (l > duration) {
                    duration = l + durationhigh;
                    mov = 1;
                    angle = durationhigh;
                }
            } else if (property.isNonAnalogPressed(input, keystate, keytime, TARGET_DOWN, false) || cursor[0]) {
                long l = System.currentTimeMillis();
                if (duration == 0) {
                    mov = -1;
                    duration = l + durationlow;
                    angle = -durationlow;
                }
                if (l > duration) {
                    duration = l + durationhigh;
                    mov = -1;
                    angle = -durationhigh;
                }
            } else {
                long l = System.currentTimeMillis();
                if (l > duration) {
                    duration = 0;
                }
            }

            TargetProperty[] targets = TargetProperty.getAllTargetProperties();
            while(mov > 0) {
                config.setTarget((config.getTarget() + targets.length - 1) % targets.length);
                select.play(SOUND_SCRATCH);
                mov--;
            }
            while(mov < 0) {
                config.setTarget((config.getTarget() + 1) % targets.length);
                select.play(SOUND_SCRATCH);
                mov++;
            }
        } else if (input.isSelectPressed() && !input.startPressed()) {
            bar.resetInput();
            // show assist option
            select.setPanelState(2);
            if(isOptionKeyReleased) {
                isOptionKeyPressed = true;
                isOptionKeyReleased = false;
                select.play(SOUND_OPTIONOPEN);
            }
            if (property.isPressed(keystate, keytime, JUDGEWINDOW_UP, true)) {
                config.setCustomJudge(!config.isCustomJudge());
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, CONSTANT, true)) {
                config.setScrollMode(config.getScrollMode() == 1 ? 0 : 1);
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, JUDGEAREA, true)) {
                config.setShowjudgearea(!config.isShowjudgearea());
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, LEGACYNOTE, true)) {
                config.setLongnoteMode(config.getLongnoteMode() == 1 ? 0 : 1);
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, MARKNOTE, true)) {
                config.setMarkprocessednote(!config.isMarkprocessednote());
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, BPMGUIDE, true)) {
                config.setBpmguide(!config.isBpmguide());
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, NOMINE, true)) {
                config.setMineMode(config.getMineMode() == 1 ? 0 : 1);
                select.play(SOUND_OPTIONCHANGE);
            }
        } else if (input.getNumberState()[5] || (input.startPressed() && input.isSelectPressed())) {
            bar.resetInput();
            // show detail option
            select.setPanelState(3);
            if(isOptionKeyReleased) {
                isOptionKeyPressed = true;
                isOptionKeyReleased = false;
                select.play(SOUND_OPTIONOPEN);
            }
            if (property.isPressed(keystate, keytime, BGA_DOWN, true)) {
            	select.executeEvent(EventType.bga);
            }
            if (property.isPressed(keystate, keytime, GAUGEAUTOSHIFT_DOWN, true)) {
            	select.executeEvent(EventType.gaugeautoshift);
            }
            if (property.isPressed(keystate, keytime, JUDGETIMING_DOWN, true)) {
                select.executeEvent(EventType.judgetiming, -1);
            }
            if (property.isPressed(keystate, keytime, DURATION_DOWN, false)) {
                long l = System.currentTimeMillis();
                if (timeChangeDuration == 0) {
                    timeChangeDuration = l + durationlow;
                	select.executeEvent(EventType.duration1p, -1);
                } else if (l > timeChangeDuration) {
                    countChangeDuration++;
                    timeChangeDuration = l + durationhigh;
                	select.executeEvent(EventType.duration1p, -1, countChangeDuration > 50 ? 10 : 0);
                }
            } else if (property.isPressed(keystate, keytime, DURATION_UP, false)) {
                long l = System.currentTimeMillis();
                if (timeChangeDuration == 0) {
                    timeChangeDuration = l + durationlow;
                	select.executeEvent(EventType.duration1p, 1);
                } else if (l > timeChangeDuration) {
                    countChangeDuration++;
                    timeChangeDuration = l + durationhigh;
                	select.executeEvent(EventType.duration1p, 1, countChangeDuration > 50 ? 10 : 0);
                }
            } else {
                timeChangeDuration = 0;
                countChangeDuration = 0;
            }
            if (property.isPressed(keystate, keytime, JUDGETIMING_UP, true)) {
                select.executeEvent(EventType.judgetiming);
            }
        } else {
            bar.input();
            select.setPanelState(0);

            if (current instanceof SelectableBar) {
                if (property.isPressed(keystate, keytime, PLAY, true) || (cursor[3] && cursortime[3] != 0) || input.isEnterPressed()) {
                    // play
                    input.setEnterPressed(false);
                    cursortime[3] = 0;
                    select.selectSong(BMSPlayerMode.PLAY);
                } else if (property.isPressed(keystate, keytime, PRACTICE, true)) {
                    // practice mode
                    select.selectSong(BMSPlayerMode.PRACTICE);
                } else if (property.isPressed(keystate, keytime, AUTO, true)) {
                    // auto play
                    select.selectSong(BMSPlayerMode.AUTOPLAY);
                } else if (property.isPressed(keystate, keytime, MusicSelectKey.REPLAY, true)) {
                    // replay
                    select.selectSong((select.getSelectedReplay() >= 0) ? BMSPlayerMode.getReplayMode(select.getSelectedReplay()) : BMSPlayerMode.PLAY);
                }
            } else {
                if (property.isPressed(keystate, keytime, FOLDER_OPEN, true) || (cursor[3] && cursortime[3] != 0) || input.isEnterPressed()) {
                    input.setEnterPressed(false);
                    // open folder
                    cursortime[3] = 0;
                    if (bar.updateBar(current)) {
                        select.play(SOUND_FOLDEROPEN);
                    }
                }
            }

            if (numberstate[7] && numtime[7] != 0) {
                numtime[7] = 0;
                select.executeEvent(EventType.rival);
            }
            if (numberstate[8] && numtime[8] != 0) {
                numtime[8] = 0;
                select.execute(MusicSelectCommand.SHOW_SONGS_ON_SAME_FOLDER);
            }
            if (numberstate[9] && numtime[9] != 0) {
                numtime[9] = 0;
                select.executeEvent(EventType.open_document);
            }
            // close folder
            if (property.isPressed(keystate, keytime, FOLDER_CLOSE, true) || (cursor[2] && cursortime[2] != 0)) {
                keytime[1] = 0;
                cursortime[2] = 0;
                bar.close();
            }

    		if(input.isActivated(KeyCommand.AUTOPLAY_FOLDER)) {
    			if(current instanceof DirectoryBar) {
    				select.selectSong(BMSPlayerMode.AUTOPLAY);
    			}
    		}
    		if(input.isActivated(KeyCommand.OPEN_IR)) {
                select.executeEvent(EventType.open_ir);
            }
    		if(input.isActivated(KeyCommand.ADD_FAVORITE_SONG)) {
                select.executeEvent(EventType.favorite_song);
    		}
    		if(input.isActivated(KeyCommand.ADD_FAVORITE_CHART)) {
                select.executeEvent(EventType.favorite_chart);
    		}

        }

        // song bar moved
        if (bar.getSelected() != current) {
            select.selectedBarMoved();
        }
        main.switchTimer(TIMER_SONGBAR_CHANGE, true);
        // update folder
		if(input.isActivated(KeyCommand.UPDATE_FOLDER)) {
            select.execute(MusicSelectCommand.UPDATE_FOLDER);
        }
        // open explorer with selected song
		if(input.isActivated(KeyCommand.OPEN_EXPLORER)) {
            select.execute(MusicSelectCommand.OPEN_WITH_EXPLORER);
        }

        if (input.isExitPressed()) {
            select.main.exit();
        }
    }
}
