package bms.player.beatoraja.external.discord;

import static bms.player.beatoraja.skin.SkinProperty.STRING_FULLTITLE;
import static bms.player.beatoraja.skin.SkinProperty.STRING_TABLE_LEVEL;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.ValueType;
import com.badlogic.gdx.utils.JsonWriter;

import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.external.ScreenShotExporter;
import bms.player.beatoraja.result.AbstractResult;
import bms.player.beatoraja.skin.property.StringPropertyFactory;

public final class DiscordWebhook {
    public static final int OFF = 0;
    public static final int IMAGE = 1;
    public static final int SCORE_CARD = 2;
    public static final int MAX_DESTINATIONS = 128;
    private static final String ATTACHMENT = "screenshot.png";
    private static final Set<String> HOSTS = Set.of("discord.com", "discordapp.com", "ptb.discord.com", "canary.discord.com");
    private static final Pattern PATH = Pattern.compile("/api/(?:v[0-9]{1,2}/)?webhooks/[0-9]{1,20}/[A-Za-z0-9_-]{1,256}");
    private static final Pattern THREAD = Pattern.compile("thread_id=[0-9]{1,20}");
    private static final Logger LOGGER = Logger.getLogger(DiscordWebhook.class.getName());
    private static final ThreadPoolExecutor UPLOADS = new ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(4), task -> {
                Thread thread = new Thread(task, "Discord webhook");
                thread.setDaemon(true);
                return thread;
            });

    private DiscordWebhook() {}

    public static String normalizeUrl(String value) {
        URI uri = httpsUri(value);
        if (uri == null || !HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT))
                || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getRawFragment() != null
                || !PATH.matcher(uri.getRawPath()).matches()) {
            return null;
        }
        String thread = uri.getRawQuery();
        if (thread != null && !THREAD.matcher(thread).matches()) return null;
        String path = uri.getRawPath().substring(uri.getRawPath().indexOf("/webhooks/"));
        return "https://discord.com/api" + path + (thread == null ? "" : "?" + thread);
    }

    public static String normalizeAvatar(String value) {
        URI uri = httpsUri(value);
        return uri == null ? "" : uri.toASCIIString();
    }

    private static URI httpsUri(String value) {
        if (value == null || value.length() > 4096) return null;
        try {
            URI uri = URI.create(value.strip());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getRawUserInfo() == null ? uri : null;
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    /** Capture result data on the game thread before another song or IR update can change it. */
    public static Post capture(MainState state) {
        Config config = state.resource.getConfig();
        int mode = config.getWebhookOption();
        if (mode != IMAGE && mode != SCORE_CARD) return null;
        List<URI> destinations = Arrays.stream(config.getWebhookUrl())
                .limit(MAX_DESTINATIONS).map(DiscordWebhook::normalizeUrl).filter(Objects::nonNull)
                .distinct().map(URI::create).toList();
        if (destinations.isEmpty()) return null;

        JsonValue card = mode == SCORE_CARD && state instanceof AbstractResult result ? scoreCard(result) : null;
        return new Post(destinations, payload(config, card));
    }

    private static JsonValue scoreCard(AbstractResult result) {
        ScoreData score = result.getNewScore();
        if (score == null) return null;
        String title = (StringPropertyFactory.getStringProperty(STRING_TABLE_LEVEL).get(result) + " "
                + StringPropertyFactory.getStringProperty(STRING_FULLTITLE).get(result)).strip();
        int end = title.offsetByCodePoints(0, Math.min(256, title.codePointCount(0, title.length())));
        return scoreCard(title.isEmpty() ? "beatoraja" : title.substring(0, end),
                scoreDescription(score, result.getOldScore(), ScreenShotExporter.getRankTypeName(result),
                        ScreenShotExporter.getClearTypeName(result), result.getIRRank(),
                        result.getIRTotalPlayer(), result.getOldIRRank()));
    }

    static JsonValue scoreCard(String title, String description) {
        JsonValue card = new JsonValue(ValueType.object);
        card.addChild("title", new JsonValue(title));
        card.addChild("description", new JsonValue(description));
        JsonValue image = new JsonValue(ValueType.object);
        image.addChild("url", new JsonValue("attachment://" + ATTACHMENT));
        card.addChild("image", image);
        return card;
    }

    static String scoreDescription(ScoreData score, ScoreData oldScore, String rank, String clear,
                                   int irRank, int irTotal, int oldIrRank) {
        boolean hasPrevious = oldScore != null && oldScore.getClear() != ClearType.NoPlay.id;
        int exScore = score.getExscore();
        int badPoor = badPoor(score);
        String rate = score.getNotes() > 0 ? String.format(Locale.ROOT, " (%.2f%%)", exScore * 50.0 / score.getNotes()) : "";
        String description = "**Clear:** " + clear + "\n**DJ Level:** " + rank + rate + "\n"
                + "**EX Score:** " + exScore + (hasPrevious ? difference(exScore, oldScore.getExscore()) : "")
                + "\n**BAD/POOR:** " + badPoor + (hasPrevious ? difference(badPoor, badPoor(oldScore)) : "");
        if (irRank > 0 && irTotal > 0) {
            description += "\n**IR Rank:** " + irRank + "/" + irTotal + (oldIrRank > 0 ? difference(irRank, oldIrRank) : "");
        }
        return description;
    }

    private static int badPoor(ScoreData score) {
        return score.getJudgeCount(3) + score.getJudgeCount(4) + score.getJudgeCount(5);
    }

    private static String difference(int current, int previous) {
        return String.format(Locale.ROOT, " (%+d)", current - previous);
    }

    static String payload(Config config, JsonValue card) {
        JsonValue payload = new JsonValue(ValueType.object);
        String name = config.getWebhookName();
        if (name != null && !name.isBlank() && name.length() <= 80) payload.addChild("username", new JsonValue(name.strip()));
        String avatar = normalizeAvatar(config.getWebhookAvatar());
        if (!avatar.isEmpty()) payload.addChild("avatar_url", new JsonValue(avatar));
        JsonValue mentions = new JsonValue(ValueType.object);
        mentions.addChild("parse", new JsonValue(ValueType.array));
        payload.addChild("allowed_mentions", mentions);
        JsonValue attachment = new JsonValue(ValueType.object);
        attachment.addChild("id", new JsonValue(0L));
        attachment.addChild("filename", new JsonValue(ATTACHMENT));
        payload.addChild("attachments", arrayOf(attachment));
        if (card != null) payload.addChild("embeds", arrayOf(card));
        return payload.toJson(JsonWriter.OutputType.json);
    }

    private static JsonValue arrayOf(JsonValue value) {
        JsonValue array = new JsonValue(ValueType.array);
        array.addChild(value);
        return array;
    }

    static HttpRequest request(URI destination, String payload, Path image) throws IOException {
        String boundary = "beatoraja-" + UUID.randomUUID();
        String part = "--" + boundary + "\r\n";
        String prefix = part + "Content-Disposition: form-data; name=\"payload_json\"\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n\r\n" + payload + "\r\n"
                + part + "Content-Disposition: form-data; name=\"files[0]\"; filename=\"" + ATTACHMENT + "\"\r\n"
                + "Content-Type: image/png\r\n\r\n";
        String separator = destination.getRawQuery() == null ? "?" : "&";
        return HttpRequest.newBuilder(URI.create(destination + separator + "wait=true"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(BodyPublishers.concat(BodyPublishers.ofString(prefix), BodyPublishers.ofFile(image),
                        BodyPublishers.ofString("\r\n--" + boundary + "--\r\n")))
                .build();
    }

    private static final class Client {
        private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public static final class Post {
        private final List<URI> destinations;
        private final String payload;

        private Post(List<URI> destinations, String payload) {
            this.destinations = destinations;
            this.payload = payload;
        }

        public void send(Path savedImage) {
            try {
                UPLOADS.execute(() -> upload(savedImage));
            } catch (RejectedExecutionException full) {
                LOGGER.warning("Discord webhook upload queue is full");
            }
        }

        private void upload(Path savedImage) {
            for (URI destination : destinations) {
                try {
                    int status = Client.HTTP.send(request(destination, payload, savedImage),
                            HttpResponse.BodyHandlers.discarding()).statusCode();
                    if (status < 200 || status >= 300) LOGGER.warning("Discord webhook post failed (HTTP " + status + ")");
                } catch (IOException | RuntimeException failure) {
                    // Request URIs and server responses may contain the webhook token.
                    LOGGER.warning("Discord webhook post failed (" + failure.getClass().getSimpleName() + ")");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
