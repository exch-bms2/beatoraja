package bms.player.beatoraja.play;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.ScoreData;

import java.util.*;

/**
 * Created by exch on 2017/02/28.
 */
public abstract class TargetProperty {

    private String name;

    private static TargetProperty[] available;

    public static TargetProperty[] getAllTargetProperties() {
        if(available == null) {
            List<TargetProperty> targets = new ArrayList<TargetProperty>();
            targets.add(new StaticTargetProperty("RANK A-", 66.6f));
            targets.add(new StaticTargetProperty("RANK A", 70.3f));
            targets.add(new StaticTargetProperty("RANK A+", 74.0f));
            targets.add(new StaticTargetProperty("RANK AA-", 77.7f));
            targets.add(new StaticTargetProperty("RANK AA", 81.4f));
            targets.add(new StaticTargetProperty("RANK AA+", 85.1f));
            targets.add(new StaticTargetProperty("RANK AAA-", 88.8f));
            targets.add(new StaticTargetProperty("RANK AAA", 92.5f));
            targets.add(new StaticTargetProperty("RANK AAA+", 96.2f));
            targets.add(new StaticTargetProperty("MAX", 100.0f));
            targets.add(new NextRankTargetProperty());
            available = targets.toArray(new TargetProperty[targets.size()]);
        }
        return available;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract int getTarget(MainController main);
}

class StaticTargetProperty extends TargetProperty{

    private float rate;

    public StaticTargetProperty(String name, float rate) {
        setName(name);
        this.rate = rate;
    }

    @Override
    public int getTarget(MainController main) {
        return (int) (main.getPlayerResource().getBMSModel().getTotalNotes() * 2 * rate / 100f);
    }
}

class RivalTargetProperty extends TargetProperty{

    private String id;

    public RivalTargetProperty(String name, String id) {
        setName(name);
        this.id = id;
    }

    @Override
    public int getTarget(MainController main) {
        // TODO 指定プレイヤーのスコアデータ取得
        return 0;
    }
}

class NextRankTargetProperty extends TargetProperty{

    public NextRankTargetProperty() {
        setName("NEXT RANK");
    }

    @Override
    public int getTarget(MainController main) {
        ScoreData now = main.getPlayDataAccessor().readScoreData(main.getPlayerResource().getBMSModel()
                , main.getPlayerConfig().getLnmode());
        final int nowscore = now != null ? now.getExscore() : 0;
        final int max = main.getPlayerResource().getBMSModel().getTotalNotes() * 2;
        for(int i = 15;i < 27;i++) {
            int target = max * i / 27;
            if(nowscore < target) {
                return target;
            }
        }
        return max;
    }
}