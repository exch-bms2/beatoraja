package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by exch on 2017/09/03.
 */
public abstract class Bar {

    private IRScoreData score;

    public abstract String getTitle();

    public IRScoreData getScore() {
        return score;
    }

    public void setScore(IRScoreData score) {
        this.score = score;
    }

    public abstract int getLamp();
}

