package bms.player.beatoraja;

import java.util.Collection;
import java.util.EnumSet;
import bms.player.beatoraja.ScoreData.SongTrophy;

/** Shared trophy semantics for canonical and rule-specific attempts. */
public final class ScoreTrophies {
    private ScoreTrophies() {}

    public static EnumSet<SongTrophy> forAttempt(ScoreData score) {
        var trophies = EnumSet.noneOf(SongTrophy.class);
        if (score.getClear() < ClearType.Easy.id) return trophies;
        if (score.getGauge() != -1) {
            if (score.getClear() >= ClearType.Hard.id) {
                trophies.add(SongTrophy.HARD);
                if (score.getClear() == ClearType.ExHard.id) trophies.add(SongTrophy.EXHARD);
            } else {
                trophies.add(SongTrophy.EASY);
                if (score.getClear() >= ClearType.Normal.id) trophies.add(SongTrophy.GROOVE);
            }
        }
        SongTrophy[] options = {SongTrophy.NORMAL, SongTrophy.MIRROR, SongTrophy.RANDOM, SongTrophy.R_RANDOM,
                SongTrophy.S_RANDOM, SongTrophy.SPIRAL, SongTrophy.H_RANDOM, SongTrophy.ALL_SCR, SongTrophy.EX_RANDOM, SongTrophy.EX_S_RANDOM};
        int option = Math.max(score.getOption() % 10, score.getOption() / 10 % 10);
        if (option >= 0 && option < options.length) trophies.add(options[option]);
        return trophies;
    }

    public static String merge(String previous, Collection<SongTrophy> added) {
        var trophies = EnumSet.noneOf(SongTrophy.class);
        if (previous != null) for (char value : previous.toCharArray()) {
            var trophy = SongTrophy.getTrophy(value);
            if (trophy != null) trophies.add(trophy);
        }
        trophies.addAll(added);
        var result = new StringBuilder();
        for (var trophy : trophies) result.append(trophy.character);
        return result.toString();
    }
}
