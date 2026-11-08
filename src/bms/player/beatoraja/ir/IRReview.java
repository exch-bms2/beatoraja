package bms.player.beatoraja.ir;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Partial update to the authenticated player's review of a chart.
 * Null leaves a field unchanged. Zero level, empty comment and false favorite
 * clear their respective fields. Favorite is chart-specific, not a song flag.
 */
public record IRReview(String sha256, Double level, String comment, Boolean favorite) {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");
    static final IRResponse<Object> UNSUPPORTED = new SimpleIRResponse<>(false, "Review updates not supported", null);

    public IRReview {
        if (sha256 == null || !SHA256.matcher(sha256).matches())
            throw new IllegalArgumentException("Invalid review chart SHA-256");
        sha256 = sha256.toLowerCase(Locale.ROOT);
        if (level != null && (!Double.isFinite(level) || level < 0))
            throw new IllegalArgumentException("Invalid review level");
        if (comment != null && comment.length() > 4096)
            throw new IllegalArgumentException("Review comment too long");
        if (level == null && comment == null && favorite == null)
            throw new IllegalArgumentException("Review update has no fields");
    }
}
