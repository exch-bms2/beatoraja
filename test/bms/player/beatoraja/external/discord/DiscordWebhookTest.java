package bms.player.beatoraja.external.discord;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.sun.net.httpserver.HttpServer;

import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.ScoreData;

public final class DiscordWebhookTest {
    public static void main(String[] args) throws Exception {
        var json = new Json();
        var config = json.fromJson(Config.class, "{}");
        config.validate();
        check(config.getWebhookOption() == DiscordWebhook.OFF && config.getWebhookUrl().length == 0, "Old config is opt-in");
        config = json.fromJson(Config.class, """
                {"webhookOption":2,"webhookName":"リザルト","webhookAvatar":"https://example.com/icon.png",
                 "webhookUrl":["https://discordapp.com/api/v10/webhooks/123/token",
                               "https://discord.com/api/webhooks/123/token",
                               "https://discord.com/api/webhooks/123/token?thread_id=456",
                               "https://discord.com.evil.test/api/webhooks/123/token",null]}
                """);
        config.validate();
        check(config.getWebhookOption() == DiscordWebhook.SCORE_CARD && config.getWebhookUrl().length == 2,
                "Imported config should deduplicate URLs and keep separate threads");
        check(DiscordWebhook.normalizeUrl("http://discord.com/api/webhooks/123/token") == null
                && DiscordWebhook.normalizeUrl("https://user@discord.com/api/webhooks/123/token") == null
                && DiscordWebhook.normalizeUrl("https://discord.com:8443/api/webhooks/123/token") == null
                && DiscordWebhook.normalizeUrl("https://discord.com/api/webhooks/123/token/extra") == null,
                "Only Discord HTTPS incoming webhook destinations are accepted");

        var score = new ScoreData();
        score.setNotes(100);
        score.setEpg(85);
        score.setEbd(3);
        var old = new ScoreData();
        old.setClear(ClearType.Normal.id);
        old.setEpg(80);
        old.setEbd(5);
        String description = DiscordWebhook.scoreDescription(score, old, "AAA", "HARD CLEAR", 3, 20, 5);
        check(description.contains("AAA (85.00%)") && description.contains("170 (+10)")
                && description.contains("3 (-2)") && description.contains("3/20 (-2)"), "Score card fields and previous score deltas");
        String first = DiscordWebhook.scoreDescription(score, null, "AAA", "HARD CLEAR", 0, 0, 0);
        check(!first.contains("IR Rank") && !first.contains("(+"), "First play and unavailable IR");

        var nativeScore = new ScoreData(); nativeScore.setNotes(2); nativeScore.setEpg(1);
        nativeScore.setNativeScore(700); nativeScore.setNativeMaxScore(750);
        var nativeBest = new ScoreData(); nativeBest.setClear(ClearType.Normal.id); nativeBest.setNativeScore(650);
        String nativeDescription = DiscordWebhook.scoreDescription(nativeScore, nativeBest, "AAA", "NORMAL", 3, 20, 5);
        check(nativeDescription.contains("AAA (93.33%)") && nativeDescription.contains("**Score:** 700/750 (+50)")
                && !nativeDescription.contains("**EX Score:") && nativeDescription.contains("IR Rank (beatoraja)"),
                "Native cards use native points, maximum, rate and improvement; IR rank identifies its separate rule");

        var reader = new JsonReader();
        var imageOnly = reader.parse(DiscordWebhook.payload(config, null));
        check(!imageOnly.has("embeds") && imageOnly.get("allowed_mentions").get("parse").size == 0, "Image only and mentions disabled");
        String payload = DiscordWebhook.payload(config, DiscordWebhook.scoreCard("曲名", description));
        var card = reader.parse(payload);
        check(card.getString("username").equals("リザルト") && card.getString("avatar_url").equals("https://example.com/icon.png")
                && card.get("embeds").get(0).getString("title").equals("曲名")
                && card.get("embeds").get(0).get("image").getString("url").equals("attachment://screenshot.png")
                && card.get("attachments").get(0).getInt("id") == 0
                && !payload.contains("\"class\""), "Discord JSON schema and UTF-8 strings");
        verifyMultipart(payload);
        System.out.println("DiscordWebhookTest passed");
    }

    private static void verifyMultipart(String payload) throws Exception {
        var body = new CompletableFuture<byte[]>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var file = Files.createTempFile("beatoraja-webhook-test", ".png");
        server.createContext("/api/webhooks/123/token", exchange -> {
            try (exchange) {
                check("POST".equals(exchange.getRequestMethod()), "HTTP method");
                check("thread_id=456&wait=true".equals(exchange.getRequestURI().getQuery()), "Thread and confirmation query");
                body.complete(exchange.getRequestBody().readAllBytes());
                exchange.sendResponseHeaders(200, -1);
            } catch (Throwable failure) {
                body.completeExceptionally(failure);
            }
        });
        server.start();
        try {
            byte[] image = {(byte) 0x89, 'P', 'N', 'G', 0, (byte) 0xff};
            Files.write(file, image);
            var destination = URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                    + "/api/webhooks/123/token?thread_id=456");
            var request = DiscordWebhook.request(destination, payload, file);
            check(HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200, "Response");
            String multipart = new String(body.get(5, TimeUnit.SECONDS), StandardCharsets.ISO_8859_1);
            String boundary = request.headers().firstValue("Content-Type").orElseThrow().split("boundary=")[1];
            check(multipart.startsWith("--" + boundary + "\r\n")
                    && multipart.contains("name=\"payload_json\"")
                    && multipart.contains(new String(payload.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
                    && multipart.contains("name=\"files[0]\"; filename=\"screenshot.png\"")
                    && multipart.endsWith(new String(image, StandardCharsets.ISO_8859_1) + "\r\n--" + boundary + "--\r\n"),
                    "Multipart headers, JSON and exact binary attachment");
        } finally {
            server.stop(0);
            Files.deleteIfExists(file);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
