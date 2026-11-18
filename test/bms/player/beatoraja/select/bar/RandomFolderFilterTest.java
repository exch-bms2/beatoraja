package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.ScoreData;
import java.util.LinkedHashMap;

public final class RandomFolderFilterTest {
    public static void main(String[] args) {
        var folder = new ExecutableBar.RandomFolder();
        var conditions = new LinkedHashMap<String, Object>();
        folder.setFilter(conditions);
        var score = new ScoreData();
        score.setClear(5);
        score.setPlaycount(9);

        conditions.put("clear", 5);
        check(folder.filter(score), "A single matching integer condition still passes");
        conditions.put("playcount", ">=10");
        check(!folder.filter(score), "Matching clear must not skip the minimum play count");
        score.setPlaycount(10);
        check(folder.filter(score), "Both conditions pass at the inclusive boundary");
        score.setClear(4);
        check(!folder.filter(score), "Play count alone is not sufficient");

        conditions.clear();
        conditions.put("playcount", ">=10");
        conditions.put("clear", 5);
        check(!folder.filter(score), "Reversing the condition order does not change the result");
        score.setClear(5);
        check(folder.filter(score), "Both conditions pass in reverse order");

        conditions.clear();
        conditions.put("clear", 0);
        check(folder.filter(null), "An unplayed chart can match a single zero condition");
        conditions.put("playcount", ">=10");
        check(!folder.filter(null), "A matching zero condition must not skip later conditions for an unplayed chart");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
