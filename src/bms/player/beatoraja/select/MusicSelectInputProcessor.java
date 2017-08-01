package bms.player.beatoraja.select;

import bms.player.beatoraja.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;

import static bms.player.beatoraja.select.MusicSelector.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

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

    private final MusicSelector select;

    public static final int KEY_PLAY = 1;
    public static final int KEY_AUTO = 2;
    public static final int KEY_REPLAY = 3;
    public static final int KEY_UP = 4;
    public static final int KEY_DOWN = 5;
    public static final int KEY_FOLDER_OPEN = 6;
    public static final int KEY_FOLDER_CLOSE = 7;
    public static final int KEY_PRACTICE = 8;

    public static final int[][][] keyassign = {
            {
                    {KEY_PLAY, KEY_FOLDER_OPEN}, {KEY_FOLDER_CLOSE}, {KEY_PRACTICE, KEY_FOLDER_OPEN}, {KEY_FOLDER_CLOSE}
                    , {KEY_FOLDER_OPEN, KEY_AUTO}, {KEY_FOLDER_CLOSE},{KEY_FOLDER_OPEN, KEY_REPLAY}, {KEY_UP}, {KEY_DOWN}
            },
            {
                    {KEY_AUTO}, {}, {KEY_FOLDER_CLOSE}, {KEY_DOWN}
                    , {KEY_PLAY}, {KEY_UP},{KEY_PRACTICE, KEY_FOLDER_OPEN}, {}, {KEY_REPLAY}
            },
            {
                    {KEY_PLAY, KEY_FOLDER_OPEN}, {KEY_FOLDER_CLOSE}, {KEY_PRACTICE, KEY_FOLDER_OPEN}, {KEY_FOLDER_CLOSE}
                    , {KEY_FOLDER_OPEN, KEY_AUTO}, {KEY_FOLDER_CLOSE},{KEY_FOLDER_OPEN, KEY_REPLAY}, {KEY_UP}, {KEY_DOWN},
                    {KEY_PLAY, KEY_FOLDER_OPEN}, {KEY_FOLDER_CLOSE}, {KEY_PRACTICE, KEY_FOLDER_OPEN}, {KEY_FOLDER_CLOSE}
                    , {KEY_FOLDER_OPEN, KEY_AUTO}, {KEY_FOLDER_CLOSE},{KEY_FOLDER_OPEN, KEY_REPLAY}, {KEY_UP}, {KEY_DOWN},
            }
    };

    public MusicSelectInputProcessor(MusicSelector select) {
        this.select = select;
    }

    public void input() {
        final BMSPlayerInputProcessor input = select.getMainController().getInputProcessor();
        final PlayerResource resource = select.getMainController().getPlayerResource();
        final PlayerConfig config = resource.getPlayerConfig();
        final BarRenderer bar = select.getBarRender();
        final Bar current = bar.getSelected();
        final int nowtime = select.getNowTime();

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
            int mode = 0;
            for(;mode < MODE.length && MODE[mode] != config.getMode();mode++);
            numtime[1] = 0;
            config.setMode(MODE[(mode + 1) % MODE.length]);
            bar.updateBar();
            select.play(SOUND_CHANGEOPTION);
        }
        if (numberstate[2] && numtime[2] != 0) {
            // ソートの切り替え
            select.setSort((select.getSort() + 1) % BarSorter.values().length);
            numtime[2] = 0;
            bar.updateBar();
            select.play(SOUND_CHANGEOPTION);
        }
        if (numberstate[3] && numtime[3] != 0) {
            // LNモードの切り替え
            config.setLnmode((config.getLnmode() + 1) % 3);
            numtime[3] = 0;
            bar.updateBar();
            select.play(SOUND_CHANGEOPTION);
        }
        if (numberstate[4] && numtime[4] != 0) {
            // change replay
            select.changeReplayIndex();
            numtime[4] = 0;
            select.play(SOUND_CHANGEOPTION);
        }

        boolean[] keystate = input.getKeystate();
        long[] keytime = input.getTime();
        boolean[] cursor = input.getCursorState();
        long[] cursortime = input.getCursorTime();

        if (input.startPressed()) {
            bar.resetInput();
            // show play option
            select.setPanelState(1);
            if (keystate[0] && keytime[0] != 0) {
                keytime[0] = 0;
                config.setRandom(config.getRandom() + 1 < 10 ? config.getRandom() + 1 : 0);
            }
            if (keystate[2] && keytime[2] != 0) {
                keytime[2] = 0;
                config.setGauge(config.getGauge() + 1 < 6 ? config.getGauge() + 1 : 0);
            }
            if (keystate[3] && keytime[3] != 0) {
                keytime[3] = 0;
                config.setDoubleoption(config.getDoubleoption() + 1 < 3 ? config.getDoubleoption() + 1 : 0);
            }
            if (keystate[6] && keytime[6] != 0) {
                keytime[6] = 0;
                config.setRandom2(config.getRandom2() + 1 < 10 ? config.getRandom2() + 1 : 0);
            }
            if (keystate[4] && keytime[4] != 0) {
                keytime[4] = 0;
                config.setFixhispeed(config.getFixhispeed() + 1 < 5 ? config.getFixhispeed() + 1 : 0);
            }

            // song bar scroll on mouse wheel
            int mov = -input.getScroll();
            input.resetScroll();
            // song bar scroll
            if (select.isPressed(keystate, keytime, KEY_UP, false) || cursor[1]) {
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
            } else if (select.isPressed(keystate, keytime, KEY_DOWN, false) || cursor[0]) {
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

            TargetProperty[] targets = TargetProperty.getAllTargetProperties(select.getMainController());
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
        } else if (input.isSelectPressed()) {
            bar.resetInput();
            // show assist option
            select.setPanelState(2);
            if (keystate[0] && keytime[0] != 0) {
                keytime[0] = 0;
                config.setExpandjudge(!config.isExpandjudge());
            }
            if (keystate[1] && keytime[1] != 0) {
                keytime[1] = 0;
                config.setConstant(!config.isConstant());
            }
            if (keystate[2] && keytime[2] != 0) {
                keytime[2] = 0;
                config.setShowjudgearea(!config.isShowjudgearea());
            }
            if (keystate[3] && keytime[3] != 0) {
                keytime[3] = 0;
                config.setLegacynote(!config.isLegacynote());
            }
            if (keystate[4] && keytime[4] != 0) {
                keytime[4] = 0;
                config.setMarkprocessednote(!config.isMarkprocessednote());
            }
            if (keystate[5] && keytime[5] != 0) {
                keytime[5] = 0;
                config.setBpmguide(!config.isBpmguide());
            }
            if (keystate[6] && keytime[6] != 0) {
                keytime[6] = 0;
                config.setNomine(!config.isNomine());
            }
        } else if (input.getNumberState()[5]) {
            bar.resetInput();
            // show detail option
            select.setPanelState(3);
            PlayConfig pc = null;
            if (current instanceof SongBar && ((SongBar)current).existsSong()) {
                SongBar song = (SongBar) current;
                pc = (song.getSongData().getMode() == 5 || song.getSongData().getMode() == 7 ? config.getMode7()
                        : (song.getSongData().getMode() == 10 || song.getSongData().getMode() == 14 ? config.getMode14()
                        : config.getMode9()));
            }
            if (keystate[0] && keytime[0] != 0) {
                keytime[0] = 0;
                resource.getConfig().setBga((resource.getConfig().getBga() + 1) % 3);
            }
            if (keystate[3] && keytime[3] != 0) {
                keytime[3] = 0;
                if (pc != null && pc.getDuration() > 1) {
                    pc.setDuration(pc.getDuration() - 1);
                }
            }
            if (keystate[4] && keytime[4] != 0) {
                keytime[4] = 0;
                if (config.getJudgetiming() > -99) {
                    config.setJudgetiming(config.getJudgetiming() - 1);
                }
            }
            if (keystate[5] && keytime[5] != 0) {
                keytime[5] = 0;
                if (pc != null && pc.getDuration() < 2000) {
                    pc.setDuration(pc.getDuration() + 1);
                }
            }
            if (keystate[6] && keytime[6] != 0) {
                keytime[6] = 0;
                if (config.getJudgetiming() < 99) {
                    config.setJudgetiming(config.getJudgetiming() + 1);
                }
            }
        } else {
            bar.input();
            select.setPanelState(0);

            if (current instanceof SelectableBar) {
                if (select.isPressed(keystate, keytime, KEY_PLAY, true) || (cursor[3] && cursortime[3] != 0)) {
                    // play
                    cursortime[3] = 0;
                    if (input.getLastKeyChangedDevice() != null) {
                        resource.setPlayDeviceType(input.getLastKeyChangedDevice().getType());
                    }
                    select.selectSong(0);
                } else if (select.isPressed(keystate, keytime, KEY_PRACTICE, true)) {
                    // practice mode
                    if (input.getLastKeyChangedDevice() != null) {
                        resource.setPlayDeviceType(input.getLastKeyChangedDevice().getType());
                    }
                    select.selectSong(2);
                } else if (select.isPressed(keystate, keytime, KEY_AUTO, true)) {
                    // auto play
                    select.selectSong(1);
                } else if (select.isPressed(keystate, keytime, KEY_REPLAY, true)) {
                    // replay
                    select.selectSong(3);
                }
            } else {
                if (select.isPressed(keystate, keytime, KEY_FOLDER_OPEN, true) || (cursor[3] && cursortime[3] != 0)) {
                    // open folder
                    cursortime[3] = 0;
                    if (bar.updateBar(current)) {
                        select.play(SOUND_FOLDEROPEN);
                    }
                }
            }

            if (numberstate[8] && numtime[8] != 0) {
                numtime[8] = 0;
                if (current instanceof SongBar && ((SongBar) current).existsSong() && 
                        (bar.getDirectory().isEmpty() || !(bar.getDirectory().getLast() instanceof SameFolderBar))) {
                    SongData sd = ((SongBar) current).getSongData();
                    bar.updateBar(new SameFolderBar(select, sd.getTitle(), sd.getFolder()));
                }
            }
            // close folder
            if (select.isPressed(keystate, keytime, KEY_FOLDER_CLOSE, true) || (cursor[2] && cursortime[2] != 0)) {
                keytime[1] = 0;
                cursortime[2] = 0;
                bar.close();
            }
        }

        // song bar moved
        if (bar.getSelected() != current) {
            select.selectedBarMoved();
        }
        if(select.getTimer()[TIMER_SONGBAR_CHANGE] == Long.MIN_VALUE) {
            select.getTimer()[TIMER_SONGBAR_CHANGE] = nowtime;
        }
        // update folder
        if (input.getFunctionstate()[1] && input.getFunctiontime()[1] != 0) {
            input.getFunctiontime()[1] = 0;
            select.updateSong(current);
        }
        // open explorer with selected song
        if (input.getFunctionstate()[2] && input.getFunctiontime()[2] != 0) {
            input.getFunctiontime()[2] = 0;
            try {
                if (Desktop.isDesktopSupported()) {
                    if(current instanceof SongBar && ((SongBar) current).existsSong()) {
                        Desktop.getDesktop().open(Paths.get(((SongBar) current).getSongData().getPath()).getParent().toFile());
                    } else if(current instanceof FolderBar) {
                        Desktop.getDesktop().open(Paths.get(((FolderBar) current).getFolderData().getPath()).toFile());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (input.isExitPressed()) {
            select.getMainController().exit();
        }
    }
}
