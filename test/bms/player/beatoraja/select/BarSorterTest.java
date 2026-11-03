package bms.player.beatoraja.select;

import java.util.Arrays;

import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.SongBar;
import bms.player.beatoraja.song.SongData;

/** Regression tests for missing charts sorted after available charts. */
public final class BarSorterTest {
    public static void main(String[] args) {
        for (BarSorter sorter : BarSorter.values()) {
            assertMissingChartsLast(sorter);
        }
        System.out.println("BarSorterTest passed");
    }

    private static void assertMissingChartsLast(BarSorter sorter) {
        SongBar first = song("Z available", true, 1);
        SongBar second = song("A available", true, 2);
        SongBar missingA = song("aaa missing", false, 9);
        SongBar missingZ = song("zzz missing", false, 0);
        // Missing charts may still have historic local or rival scores.
        missingZ.setScore(null);
        missingZ.setRivalScore(null);
        Bar[] bars = { missingZ, second, missingA, first };
        Arrays.sort(bars, sorter.sorter);
        boolean titleOrder = sorter == BarSorter.TITLE;
        check(bars[0] == (titleOrder ? second : first)
                && bars[1] == (titleOrder ? first : second),
                sorter + " must preserve the selected sort for available charts");
        check(bars[2] == missingA && bars[3] == missingZ,
                sorter + " must put missing charts last in case-insensitive title order");

        Bar nonSong = new Bar() {
            @Override public String getTitle() { return "Other bar"; }
            @Override public int getLamp(boolean isPlayer) { return 0; }
        };
        check(sorter.sorter.compare(nonSong, missingA) < 0
                && sorter.sorter.compare(missingA, nonSong) > 0,
                sorter + " must put missing charts after non-song bars too");

        SongBar subtitleA = song("Same title", false, 2);
        subtitleA.getSongData().setSubtitle("[A]");
        SongBar subtitleZ = song("Same title", false, 1);
        subtitleZ.getSongData().setSubtitle("[Z]");
        check(sorter.sorter.compare(subtitleA, subtitleZ) < 0,
                sorter + " must include the subtitle when sorting missing charts");
    }

    private static SongBar song(String title, boolean available, int value) {
        var data = new SongData();
        data.setTitle(title);
        data.setArtist(value == 1 ? "A artist" : "Z artist");
        data.setLevel(value);
        data.setDifficulty(value);
        data.setMaxbpm(value * 100);
        data.setLength(value * 100);
        if (available) data.setPath("charts/" + value + ".bms");
        var bar = new SongBar(data);
        bar.setScore(score(value));
        bar.setRivalScore(score(0));
        return bar;
    }

    private static ScoreData score(int value) {
        var score = new ScoreData();
        score.setClear(value);
        score.setNotes(100);
        score.setEpg(value * 10);
        score.setMinbp(value);
        score.setAvgjudge(value * 100);
        score.setDate(value);
        return score;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
