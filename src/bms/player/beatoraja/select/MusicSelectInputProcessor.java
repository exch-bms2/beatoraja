package bms.player.beatoraja.select;

import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.select.MusicSelectKeyProperty.MusicSelectKey;
import bms.player.beatoraja.select.bar.*;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import static bms.player.beatoraja.select.MusicSelector.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import static bms.player.beatoraja.select.MusicSelectKeyProperty.MusicSelectKey.*;

/**
 * 選曲の入力処理用クラス
 * Class for input processing of music selection
 *
 * @author exch
 */
public class MusicSelectInputProcessor {

    /**
     * バー移動中のカウンタ
     * Counter in moving bar
     */
    private long duration;
    /**
     * バーの移動方向
     * Direction of movement of the bar
     */
    private int angle;

    private final int durationlow = 300;
    private final int durationhigh = 50;

    private final MusicSelector select;

    public MusicSelectInputProcessor(MusicSelector select) {
        this.select = select;
    }

    public void input() {
    	final MainController main = select.main;
        final BMSPlayerInputProcessor input = main.getInputProcessor();
        final PlayerResource resource = main.getPlayerResource();
        final PlayerConfig config = resource.getPlayerConfig();
        final BarRenderer bar = select.getBarRender();
        final Bar current = bar.getSelected();

        if (input.checkIfNumberPressed(0)) {
            // 検索用ポップアップ表示。これ必要？
        	// Popup display for search.
            input.resetNumberTime(0);
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

        if (input.checkIfNumberPressed(1)) {
            // KEYフィルターの切り替え
        	// Switching KEY Filters
        	input.resetNumberTime(1);
            select.execute(MusicSelectCommand.NEXT_MODE);
        }
        if (input.checkIfNumberPressed(2)) {
            // ソートの切り替え
        	// Switching the sort
            input.resetNumberTime(2);
            select.execute(MusicSelectCommand.NEXT_SORT);
        }
        if (input.checkIfNumberPressed(3)) {
            // LNモードの切り替え
        	// LN mode switching
            input.resetNumberTime(3);
            select.execute(MusicSelectCommand.NEXT_LNMODE);
        }
        
        final MusicSelectKeyProperty property = MusicSelectKeyProperty.values()[config.getMusicselectinput()];

        if (input.checkIfNumberPressed(4) || (!input.startPressed() && !input.isSelectPressed() && property.isPressed(input, NEXT_REPLAY, true))) {
            // change replay
            input.resetNumberTime(4);
            select.execute(MusicSelectCommand.NEXT_REPLAY);
        }
        if (input.startPressed() && !input.isSelectPressed()) {
            bar.resetInput();
            // show play option
            select.setPanelState(1);
            if (property.isPressed(input, OPTION1_DOWN, true)) {
                select.execute(MusicSelectCommand.NEXT_OPTION_1P);
            }
            if (property.isPressed(input, OPTION1_UP, true)) {
                config.setRandom((config.getRandom() + 9) % 10);
                select.play(SOUND_CHANGEOPTION);
            }
            if (property.isPressed(input, GAUGE_DOWN, true)) {
                select.execute(MusicSelectCommand.NEXT_GAUGE_1P);
            }
            if (property.isPressed(input, GAUGE_UP, true)) {
                config.setGauge((config.getGauge() + 5) % 6);
                select.play(SOUND_CHANGEOPTION);
            }
            if (property.isPressed(input, OPTIONDP_DOWN, true)) {
                select.execute(MusicSelectCommand.NEXT_OPTION_DP);
            }
            if (property.isPressed(input, OPTIONDP_UP, true)) {
                config.setDoubleoption((config.getDoubleoption() + 2) % 3);
                select.play(SOUND_CHANGEOPTION);
            }
            if (property.isPressed(input, OPTION2_DOWN, true)) {
                select.execute(MusicSelectCommand.NEXT_OPTION_2P);
            }
            if (property.isPressed(input, OPTION2_UP, true)) {
                config.setRandom2((config.getRandom2() + 9) % 10);
                select.play(SOUND_CHANGEOPTION);
            }
            if (property.isPressed(input, HSFIX_DOWN, true)) {
                select.execute(MusicSelectCommand.NEXT_HSFIX);
            }
            if (property.isPressed(input, HSFIX_UP, true)) {
                select.execute(MusicSelectCommand.PREV_HSFIX);
            }

            // song bar scroll on mouse wheel
            int mov = -input.getScroll();
            input.resetScroll();
            // song bar scroll
            if (property.isPressed(input, TARGET_UP, false) || input.getCursorState(1)) {
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
            } else if (property.isPressed(input, TARGET_DOWN, false) || input.getCursorState(1)) {
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
                config.setTarget((config.getTarget() + 1) % targets.length);
                select.play(SOUND_SCRATCH);
                mov--;
            }
            while(mov < 0) {
                config.setTarget((config.getTarget() + targets.length - 1) % targets.length);
                select.play(SOUND_SCRATCH);
                mov++;
            }
        } else if (input.isSelectPressed() && !input.startPressed()) {
            bar.resetInput();
            // show assist option
            select.setPanelState(2);
            if (property.isPressed(input, JUDGEWINDOW_UP, true)) {
                config.setJudgewindowrate(config.getJudgewindowrate() == 100 ? 400 : 100);
                select.play(SOUND_CHANGEOPTION);
            }
            if (property.isPressed(input, CONSTANT, true)) {
                config.setConstant(!config.isConstant());
                select.play(SOUND_CHANGEOPTION);
            }
            if (property.isPressed(input, JUDGEAREA, true)) {
                config.setShowjudgearea(!config.isShowjudgearea());
                select.play(SOUND_CHANGEOPTION);
            }
            if (property.isPressed(input, LEGACYNOTE, true)) {
                config.setLegacynote(!config.isLegacynote());
                select.play(SOUND_CHANGEOPTION);
            }
            if (property.isPressed(input, MARKNOTE, true)) {
                config.setMarkprocessednote(!config.isMarkprocessednote());
                select.play(SOUND_CHANGEOPTION);
            }
            if (property.isPressed(input, BPMGUIDE, true)) {
                config.setBpmguide(!config.isBpmguide());
                select.play(SOUND_CHANGEOPTION);
            }
            if (property.isPressed(input, NOMINE, true)) {
                config.setNomine(!config.isNomine());
                select.play(SOUND_CHANGEOPTION);
            }
        } else if (input.getNumberState(5) || (input.startPressed() && input.isSelectPressed())) {
            bar.resetInput();
            // show detail option
            select.setPanelState(3);
            if (property.isPressed(input, BGA_DOWN, true)) {
                select.execute(MusicSelectCommand.NEXT_BGA_SHOW);
            }
            if (property.isPressed(input, DURATION_DOWN, true)) {
                select.execute(MusicSelectCommand.DURATION_DOWN);
            }
            if (property.isPressed(input, JUDGETIMING_DOWN, true)) {
                select.execute(MusicSelectCommand.JUDGETIMING_DOWN);
            }
            if (property.isPressed(input, DURATION_UP, true)) {
                select.execute(MusicSelectCommand.DURATION_UP);
            }
            if (property.isPressed(input, JUDGETIMING_UP, true)) {
                select.execute(MusicSelectCommand.JUDGETIMING_UP);
            }
        } else {
            bar.input();
            select.setPanelState(0);

            if (current instanceof SelectableBar) {
                if (property.isPressed(input, PLAY, true) || input.checkIfCursorPressed(3) || input.isEnterPressed()) {
                    // play
                    input.setEnterPressed(false);
                    input.resetCursorTime(3);
                    select.selectSong(PlayMode.PLAY);
                } else if (property.isPressed(input, PRACTICE, true)) {
                    // practice mode
                    select.selectSong(PlayMode.PRACTICE);
                } else if (property.isPressed(input, AUTO, true)) {
                    // auto play
                    select.selectSong(PlayMode.AUTOPLAY);
                } else if (property.isPressed(input, MusicSelectKey.REPLAY, true)) {
                    // replay
                    select.selectSong(PlayMode.REPLAY_1);
                }
            } else {
                if (property.isPressed(input, FOLDER_OPEN, true) || input.checkIfCursorPressed(3) || input.isEnterPressed()) {
                    input.setEnterPressed(false);
                    // open folder
                    input.resetCursorTime(3);
                    if (bar.updateBar(current)) {
                        select.play(SOUND_FOLDEROPEN);
                    }
                }
            }

            if (input.checkIfNumberPressed(7)) {
            	input.resetNumberTime(7);
                select.execute(MusicSelectCommand.NEXT_RIVAL);
            }
            if (input.checkIfNumberPressed(8)) {
            	input.resetNumberTime(8);
                if (current instanceof SongBar && ((SongBar) current).existsSong() && 
                        (bar.getDirectory().isEmpty() || !(bar.getDirectory().getLast() instanceof SameFolderBar))) {
                    SongData sd = ((SongBar) current).getSongData();
                    bar.updateBar(new SameFolderBar(select, sd.getTitle(), sd.getFolder()));
                    select.play(SOUND_FOLDEROPEN);
                }
            }
            if (input.checkIfNumberPressed(9)) {
                input.resetNumberTime(9);
                select.execute(MusicSelectCommand.OPEN_DOCUMENT);
            }
            // close folder
            if (property.isPressed(input, FOLDER_CLOSE, true) || input.checkIfCursorPressed(2)) {
            	input.resetKeyTime(1);
                input.resetCursorTime(2);
                bar.close();
            }
            
    		if(input.checkIfFunctionPressed(9)) {
    			input.resetFunctionTime(9);
    			if(current instanceof DirectoryBar) {
    				select.selectSong(PlayMode.AUTOPLAY);
    			}
    		}
        }

        // song bar moved
        if (bar.getSelected() != current) {
            select.selectedBarMoved();
        }
        main.switchTimer(TIMER_SONGBAR_CHANGE, true);
        // update folder
        if (input.checkIfFunctionPressed(1)) {
        	input.resetFunctionTime(1);
            select.execute(MusicSelectCommand.UPDATE_FOLDER);
        }
        // open explorer with selected song
        if (input.checkIfFunctionPressed(2)) {
            input.resetFunctionTime(2);
            select.execute(MusicSelectCommand.OPEN_WITH_EXPLORER);
        }

        if (input.isExitPressed()) {
            select.main.exit();
        }
    }
}
