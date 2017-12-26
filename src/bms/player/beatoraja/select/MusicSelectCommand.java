package bms.player.beatoraja.select;

import bms.player.beatoraja.PlayConfig;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerInformation;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.FolderBar;
import bms.player.beatoraja.select.bar.SelectableBar;
import bms.player.beatoraja.select.bar.SongBar;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;

import static bms.player.beatoraja.select.MusicSelector.SOUND_CHANGEOPTION;

public enum MusicSelectCommand {

    NEXT_MODE {
        @Override
        public void execute(MusicSelector selector) {
            int mode = 0;
            PlayerConfig config = selector.getMainController().getPlayerConfig();
            for(;mode < MusicSelector.MODE.length && MusicSelector.MODE[mode] != config.getMode();mode++);
            config.setMode(MusicSelector.MODE[(mode + 1) % MusicSelector.MODE.length]);
            selector.getBarRender().updateBar();
            selector.play(SOUND_CHANGEOPTION);
        }
    },
    NEXT_SORT {
        @Override
        public void execute(MusicSelector selector) {
            selector.setSort((selector.getSort() + 1) % BarSorter.values().length);
            selector.getBarRender().updateBar();
            selector.play(SOUND_CHANGEOPTION);
        }
    },
    NEXT_LNMODE {
        @Override
        public void execute(MusicSelector selector) {
            PlayerConfig config = selector.getMainController().getPlayerConfig();
            config.setLnmode((config.getLnmode() + 1) % 3);
            selector.getBarRender().updateBar();
            selector.play(SOUND_CHANGEOPTION);
        }
    },
    RESET_REPLAY {
        @Override
        public void execute(MusicSelector selector) {
            if (selector.getBarRender().getSelected() instanceof SelectableBar) {
                boolean[] replays = ((SelectableBar) selector.getBarRender().getSelected()).getExistsReplayData();
                for (int i = 0; i < replays.length; i++) {
                    if (replays[i]) {
                        selector.setSelectedReplay(i);
                        return;
                    }
                }
            }
            selector.setSelectedReplay(-1);
        }
    },
    NEXT_REPLAY {
        @Override
        public void execute(MusicSelector selector) {
            Bar current = selector.getBarRender().getSelected();
            if (current != null && current instanceof SelectableBar) {
                boolean[] replays = ((SelectableBar) current).getExistsReplayData();
                for (int i = 1; i < replays.length; i++) {
                    final int selectedreplay = selector.getSelectedReplay();
                    if (replays[(i + selectedreplay) % replays.length]) {
                        selector.setSelectedReplay((i + selectedreplay) % replays.length);
                        selector.play(SOUND_CHANGEOPTION);
                        break;
                    }
                }
            }
        }
    },
    NEXT_RIVAL {
        @Override
        public void execute(MusicSelector selector) {
            PlayerInformation nowrival = selector.getRival();
            boolean match = (nowrival == null);
            for(PlayerInformation rival : selector.getRivals()) {
                if(match) {
                    nowrival = rival;
                    match = false;
                    break;
                }
                match = (nowrival == rival);
            }
            if(match) {
                nowrival = null;
            }
            selector.setRival(nowrival);
            selector.play(SOUND_CHANGEOPTION);
        }
    },
    OPEN_WITH_EXPLORER {
        @Override
        public void execute(MusicSelector selector) {
            Bar current = selector.getBarRender().getSelected();
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
    }
    ;

    public abstract void execute(MusicSelector selector);
}
