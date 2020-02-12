package bms.player.beatoraja.select;

import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyCommand;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.select.MusicSelectKeyProperty.MusicSelectKey;
import bms.player.beatoraja.select.bar.*;
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

    private final int durationlow = 300;
    private final int durationhigh = 50;

    boolean isOptionKeyPressed = false;
    boolean isOptionKeyReleased = false;

    // ノーツ表示時間変更のカウンタ
    private long timeChangeDuration;
    private int countChangeDuration;

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
            select.execute(MusicSelectCommand.NEXT_MODE);
        }
        if (numberstate[2] && numtime[2] != 0) {
            // ソートの切り替え
            numtime[2] = 0;
            select.execute(MusicSelectCommand.NEXT_SORT);
        }
        if (numberstate[3] && numtime[3] != 0) {
            // LNモードの切り替え
            numtime[3] = 0;
            select.execute(MusicSelectCommand.NEXT_LNMODE);
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
                select.execute(MusicSelectCommand.NEXT_OPTION_1P);
            }
            if (property.isPressed(keystate, keytime, OPTION1_UP, true)) {
                config.setRandom((config.getRandom() + 9) % 10);
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, GAUGE_DOWN, true)) {
                select.execute(MusicSelectCommand.NEXT_GAUGE_1P);
            }
            if (property.isPressed(keystate, keytime, GAUGE_UP, true)) {
                config.setGauge((config.getGauge() + 5) % 6);
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, OPTIONDP_DOWN, true)) {
                select.execute(MusicSelectCommand.NEXT_OPTION_DP);
            }
            if (property.isPressed(keystate, keytime, OPTIONDP_UP, true)) {
                config.setDoubleoption((config.getDoubleoption() + 2) % 3);
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, OPTION2_DOWN, true)) {
                select.execute(MusicSelectCommand.NEXT_OPTION_2P);
            }
            if (property.isPressed(keystate, keytime, OPTION2_UP, true)) {
                config.setRandom2((config.getRandom2() + 9) % 10);
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, HSFIX_DOWN, true)) {
                select.execute(MusicSelectCommand.NEXT_HSFIX);
            }
            if (property.isPressed(keystate, keytime, HSFIX_UP, true)) {
                select.execute(MusicSelectCommand.PREV_HSFIX);
            }

            // song bar scroll on mouse wheel
            int mov = -input.getScroll();
            input.resetScroll();
            // song bar scroll
            if (property.isPressed(keystate, keytime, TARGET_UP, false) || cursor[1]) {
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
            } else if (property.isPressed(keystate, keytime, TARGET_DOWN, false) || cursor[0]) {
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
            if(isOptionKeyReleased) {
                isOptionKeyPressed = true;
                isOptionKeyReleased = false;
                select.play(SOUND_OPTIONOPEN);
            }
            if (property.isPressed(keystate, keytime, JUDGEWINDOW_UP, true)) {
                config.setJudgewindowrate(config.getJudgewindowrate() == 100 ? 400 : 100);
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, CONSTANT, true)) {
                config.setConstant(!config.isConstant());
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, JUDGEAREA, true)) {
                config.setShowjudgearea(!config.isShowjudgearea());
                select.play(SOUND_OPTIONCHANGE);
            }
            if (property.isPressed(keystate, keytime, LEGACYNOTE, true)) {
                config.setLegacynote(!config.isLegacynote());
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
                select.execute(MusicSelectCommand.NEXT_BGA_SHOW);
            }
            if (property.isPressed(keystate, keytime, GAUGEAUTOSHIFT_DOWN, true)) {
                select.execute(MusicSelectCommand.NEXT_GAUGEAUTOSHIFT);
            }
            if (property.isPressed(keystate, keytime, JUDGETIMING_DOWN, true)) {
                select.execute(MusicSelectCommand.JUDGETIMING_DOWN);
            }
            if (property.isPressed(keystate, keytime, DURATION_DOWN, false)) {
                long l = System.currentTimeMillis();
                if (timeChangeDuration == 0) {
                    timeChangeDuration = l + durationlow;
                    select.execute(MusicSelectCommand.DURATION_DOWN);
                } else if (l > timeChangeDuration) {
                    countChangeDuration++;
                    timeChangeDuration = l + durationhigh;
                    select.execute(countChangeDuration > 50 ? MusicSelectCommand.DURATION_DOWN_LARGE : MusicSelectCommand.DURATION_DOWN);
                }
            } else if (property.isPressed(keystate, keytime, DURATION_UP, false)) {
                long l = System.currentTimeMillis();
                if (timeChangeDuration == 0) {
                    timeChangeDuration = l + durationlow;
                    select.execute(MusicSelectCommand.DURATION_UP);
                } else if (l > timeChangeDuration) {
                    countChangeDuration++;
                    timeChangeDuration = l + durationhigh;
                    select.execute(countChangeDuration > 50 ? MusicSelectCommand.DURATION_UP_LARGE : MusicSelectCommand.DURATION_UP);
                }
            } else {
                timeChangeDuration = 0;
                countChangeDuration = 0;
            }
            if (property.isPressed(keystate, keytime, JUDGETIMING_UP, true)) {
                select.execute(MusicSelectCommand.JUDGETIMING_UP);
            }
        } else {
            bar.input();
            select.setPanelState(0);

            if (current instanceof SelectableBar) {
                if (property.isPressed(keystate, keytime, PLAY, true) || (cursor[3] && cursortime[3] != 0) || input.isEnterPressed()) {
                    // play
                    input.setEnterPressed(false);
                    cursortime[3] = 0;
                    select.selectSong(PlayMode.PLAY);
                } else if (property.isPressed(keystate, keytime, PRACTICE, true)) {
                    // practice mode
                    select.selectSong(PlayMode.PRACTICE);
                } else if (property.isPressed(keystate, keytime, AUTO, true)) {
                    // auto play
                    select.selectSong(PlayMode.AUTOPLAY);
                } else if (property.isPressed(keystate, keytime, MusicSelectKey.REPLAY, true)) {
                    // replay
                    select.selectSong(PlayMode.REPLAY_1);
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
                select.execute(MusicSelectCommand.NEXT_RIVAL);
            }
            if (numberstate[8] && numtime[8] != 0) {
                numtime[8] = 0;
                if (current instanceof SongBar && ((SongBar) current).existsSong() &&
                        (bar.getDirectory().size == 0 || !(bar.getDirectory().last() instanceof SameFolderBar))) {
                    SongData sd = ((SongBar) current).getSongData();
                    bar.updateBar(new SameFolderBar(select, sd.getFullTitle(), sd.getFolder()));
                    select.play(SOUND_FOLDEROPEN);
                } else if (current instanceof GradeBar) {
                    List<Bar> songbars = Arrays.asList(((GradeBar) current).getSongDatas()).stream()
                            .distinct()
                            .map(SongBar::new)
                            .collect(Collectors.toList());
                    bar.updateBar(new ContainerBar(current.getTitle(), songbars.toArray(new Bar[songbars.size()])));
                    select.play(SOUND_FOLDEROPEN);
                }
            }
            if (numberstate[9] && numtime[9] != 0) {
                numtime[9] = 0;
                select.execute(MusicSelectCommand.OPEN_DOCUMENT);
            }
            // close folder
            if (property.isPressed(keystate, keytime, FOLDER_CLOSE, true) || (cursor[2] && cursortime[2] != 0)) {
                keytime[1] = 0;
                cursortime[2] = 0;
                bar.close();
            }

    		if(input.isActivated(KeyCommand.AUTOPLAY_FOLDER)) {
    			if(current instanceof DirectoryBar) {
    				select.selectSong(PlayMode.AUTOPLAY);
    			}
    		}
    		if(input.isActivated(KeyCommand.OPEN_IR)) {
                select.execute(MusicSelectCommand.OPEN_RANKING_ON_IR);
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
