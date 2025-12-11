package bms.player.beatoraja.select;

import bms.player.beatoraja.*;
import bms.player.beatoraja.SystemSoundManager.SoundType;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyCommand;
import bms.player.beatoraja.input.KeyBoardInputProcesseor.ControlKeys;
import bms.player.beatoraja.select.MusicSelectKeyProperty.MusicSelectKey;
import bms.player.beatoraja.select.bar.*;
import bms.player.beatoraja.skin.property.EventFactory.EventType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import static bms.player.beatoraja.SystemSoundManager.SoundType.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import static bms.player.beatoraja.select.MusicSelectKeyProperty.MusicSelectKey.*;

/**
 * 選曲の入力処理用クラス
 *
 * @author exch
 */
public final class MusicSelectInputProcessor {

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
        final BarManager barManager = select.getBarManager();
        final Bar current = select.getBarManager().getSelected();

        if (input.isControlKeyPressed(ControlKeys.NUM0)) {
            // 検索用ポップアップ表示。これ必要？
            Gdx.input.getTextInput(new Input.TextInputListener() {
                @Override
                public void input(String text) {
                    if (text.length() > 1) {
                    	barManager.addSearch(new SearchWordBar(select, text));
                    	barManager.updateBar(null);
                    }
                }

                @Override
                public void canceled() {
                }
            }, "Search", "", "Search bms title");
        }

        // KEYフィルターの切り替え
        if (input.isControlKeyPressed(ControlKeys.NUM1)) {
            select.executeEvent(EventType.mode);
        }
        // ソートの切り替え
        if (input.isControlKeyPressed(ControlKeys.NUM2)) {
            select.executeEvent(EventType.sort);
        }
        // LNモードの切り替え
        if (input.isControlKeyPressed(ControlKeys.NUM3)) {
            select.executeEvent(EventType.lnmode);
        }

        final MusicSelectKeyProperty property = MusicSelectKeyProperty.values()[config.getMusicselectinput()];

        if(!input.startPressed() && !input.isSelectPressed() && !input.getControlKeyState(ControlKeys.NUM5)){
            //オプションキー入力なし
            isOptionKeyReleased = true;
            if(isOptionKeyPressed) {
                isOptionKeyPressed = false;
                select.play(OPTION_CLOSE);
            }
        }

        if (input.isControlKeyPressed(ControlKeys.NUM4)
                || (!input.startPressed() && !input.isSelectPressed() && !input.getControlKeyState(ControlKeys.NUM5) && property.isPressed(input, NEXT_REPLAY, true))) {
            // change replay
            select.execute(MusicSelectCommand.NEXT_REPLAY);
        }
        if (input.startPressed() && !input.isSelectPressed()) {
            bar.resetInput();
            // show play option
            select.setPanelState(1);
            if(isOptionKeyReleased) {
                isOptionKeyPressed = true;
                isOptionKeyReleased = false;
                select.play(OPTION_OPEN);
            }
            if (property.isPressed(input, OPTION1_DOWN, true)) {
                select.executeEvent(EventType.option1p, 1);
            }
            if (property.isPressed(input, OPTION1_UP, true)) {
                select.executeEvent(EventType.option1p, -1);
            }
            if (property.isPressed(input, GAUGE_DOWN, true)) {
                select.executeEvent(EventType.gauge1p, 1);
            }
            if (property.isPressed(input, GAUGE_UP, true)) {
                select.executeEvent(EventType.gauge1p, -1);
            }
            if (property.isPressed(input, OPTIONDP_DOWN, true)) {
                select.executeEvent(EventType.optiondp, 1);
            }
            if (property.isPressed(input, OPTIONDP_UP, true)) {
                select.executeEvent(EventType.optiondp, -1);
            }
            if (property.isPressed(input, OPTION2_DOWN, true)) {
                select.executeEvent(EventType.option2p, 1);
            }
            if (property.isPressed(input, OPTION2_UP, true)) {
                select.executeEvent(EventType.option2p, -1);
            }
            if (property.isPressed(input, HSFIX_DOWN, true)) {
                select.executeEvent(EventType.hsfix, 1);
            }
            if (property.isPressed(input, HSFIX_UP, true)) {
                select.executeEvent(EventType.hsfix, -1);
            }

            // song bar scroll on mouse wheel
            int mov = -input.getScroll();
            input.resetScroll();

            analogScrollBuffer += property.getAnalogChange(input, TARGET_UP) - property.getAnalogChange(input, TARGET_DOWN);
            mov += analogScrollBuffer/analogTicksPerScroll;
            analogScrollBuffer %= analogTicksPerScroll;

            // song bar scroll
            if (property.isNonAnalogPressed(input, TARGET_UP, false) || input.getControlKeyState(ControlKeys.DOWN)) {
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
            } else if (property.isNonAnalogPressed(input, TARGET_DOWN, false) ||  input.getControlKeyState(ControlKeys.UP)) {
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

            while(mov > 0) {
            	select.executeEvent(EventType.target, -1);
                select.play(SCRATCH);
                mov--;
            }
            while(mov < 0) {
            	select.executeEvent(EventType.target, 1);
                select.play(SCRATCH);
                mov++;
            }
        } else if (input.isSelectPressed() && !input.startPressed()) {
            bar.resetInput();
            // show assist option
            select.setPanelState(2);
            if(isOptionKeyReleased) {
                isOptionKeyPressed = true;
                isOptionKeyReleased = false;
                select.play(OPTION_OPEN);
            }
            if (property.isPressed(input, JUDGEWINDOW_UP, true)) {
                config.setCustomJudge(!config.isCustomJudge());
                select.play(OPTION_CHANGE);
            }
            if (property.isPressed(input, CONSTANT, true)) {
                config.setScrollMode(config.getScrollMode() == 1 ? 0 : 1);
                select.play(OPTION_CHANGE);
            }
            if (property.isPressed(input, JUDGEAREA, true)) {
                config.setShowjudgearea(!config.isShowjudgearea());
                select.play(OPTION_CHANGE);
            }
            if (property.isPressed(input, LEGACYNOTE, true)) {
                config.setLongnoteMode(config.getLongnoteMode() == 1 ? 0 : 1);
                select.play(OPTION_CHANGE);
            }
            if (property.isPressed(input, MARKNOTE, true)) {
                config.setMarkprocessednote(!config.isMarkprocessednote());
                select.play(OPTION_CHANGE);
            }
            if (property.isPressed(input, BPMGUIDE, true)) {
                config.setBpmguide(!config.isBpmguide());
                select.play(OPTION_CHANGE);
            }
            if (property.isPressed(input, NOMINE, true)) {
                config.setMineMode(config.getMineMode() == 1 ? 0 : 1);
                select.play(OPTION_CHANGE);
            }
        } else if (input.getControlKeyState(ControlKeys.NUM5) || (input.startPressed() && input.isSelectPressed())) {
            bar.resetInput();
            // show detail option
            select.setPanelState(3);
            if(isOptionKeyReleased) {
                isOptionKeyPressed = true;
                isOptionKeyReleased = false;
                select.play(OPTION_OPEN);
            }
            if (property.isPressed(input, BGA_DOWN, true)) {
            	select.executeEvent(EventType.bga);
            }
            if (property.isPressed(input, GAUGEAUTOSHIFT_DOWN, true)) {
            	select.executeEvent(EventType.gaugeautoshift);
            }
            if (property.isPressed(input, NOTESDISPLAYTIMING_DOWN, true)) {
                select.executeEvent(EventType.notesdisplaytiming, -1);
            }
            if (property.isPressed(input, DURATION_DOWN, false)) {
                long l = System.currentTimeMillis();
                if (timeChangeDuration == 0) {
                    timeChangeDuration = l + durationlow;
                	select.executeEvent(EventType.duration1p, -1);
                } else if (l > timeChangeDuration) {
                    countChangeDuration++;
                    timeChangeDuration = l + durationhigh;
                	select.executeEvent(EventType.duration1p, -1, countChangeDuration > 50 ? 10 : 0);
                }
            } else if (property.isPressed(input, DURATION_UP, false)) {
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
            if (property.isPressed(input, NOTESDISPLAYTIMING_UP, true)) {
                select.executeEvent(EventType.notesdisplaytiming);
            }
            if (property.isPressed(input, NOTESDISPLAYTIMING_AUTOADJUST, true)) {
                select.executeEvent(EventType.notesdisplaytimingautoadjust);
            }
        } else {
            bar.input();
            select.setPanelState(0);

            if (current instanceof SelectableBar) {
                if (property.isPressed(input, PLAY, true) || input.isControlKeyPressed(ControlKeys.RIGHT) || input.isControlKeyPressed(ControlKeys.ENTER)) {
                    // play
                    select.selectSong(BMSPlayerMode.PLAY);
                } else if (property.isPressed(input, PRACTICE, true)) {
                    // practice mode
                    select.selectSong(config.isEventMode() ? BMSPlayerMode.PLAY : BMSPlayerMode.PRACTICE);
                } else if (property.isPressed(input, AUTO, true)) {
                    // auto play
                    select.selectSong(config.isEventMode() ? BMSPlayerMode.PLAY : BMSPlayerMode.AUTOPLAY);
                } else if (property.isPressed(input, MusicSelectKey.REPLAY, true)) {
                    // replay
                    select.selectSong(config.isEventMode() ? BMSPlayerMode.PLAY : ((select.getSelectedReplay() >= 0) ? BMSPlayerMode.getReplayMode(select.getSelectedReplay()) : BMSPlayerMode.PLAY));
                }
            } else if (current instanceof DirectoryBar dirbar) {
                if (property.isPressed(input, MusicSelectKey.FOLDER_OPEN, true) || input.isControlKeyPressed(ControlKeys.RIGHT) || input.isControlKeyPressed(ControlKeys.ENTER)) {
                    // open folder
                    if (select.getBarManager().updateBar(dirbar)) {
                        select.play(SoundType.FOLDER_OPEN);
                    }
                }
            }

            if (input.isControlKeyPressed(ControlKeys.NUM7)) {
                select.executeEvent(EventType.rival);
            }
            if (input.isControlKeyPressed(ControlKeys.NUM8)) {
                select.execute(MusicSelectCommand.SHOW_SONGS_ON_SAME_FOLDER);
            }
            if (input.isControlKeyPressed(ControlKeys.NUM9)) {
                select.executeEvent(EventType.open_document);
            }
            // close folder
            if (property.isPressed(input, MusicSelectKey.FOLDER_CLOSE, true) || input.isControlKeyPressed(ControlKeys.LEFT)) {
                input.resetKeyChangedTime(1);
                select.getBarManager().close();
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
        if (select.getBarManager().getSelected() != current) {
            select.selectedBarMoved();
        }
        select.timer.switchTimer(TIMER_SONGBAR_CHANGE, true);
        // update folder
		if(input.isActivated(KeyCommand.UPDATE_FOLDER)) {
            select.executeEvent(EventType.update_folder);
        }
        // open explorer with selected song
		if(input.isActivated(KeyCommand.OPEN_EXPLORER)) {
            select.executeEvent(EventType.open_with_explorer);
        }
        // copy song MD5 hash
        if(input.isActivated(KeyCommand.COPY_SONG_MD5_HASH)) {
            select.execute(MusicSelectCommand.COPY_MD5_HASH);
        }
        // copy song SHA256 hash
        if(input.isActivated(KeyCommand.COPY_SONG_SHA256_HASH)) {
            select.execute(MusicSelectCommand.COPY_SHA256_HASH);
        }

        if (input.isControlKeyPressed(ControlKeys.ESCAPE)) {
            select.main.exit();
        }
    }
}
