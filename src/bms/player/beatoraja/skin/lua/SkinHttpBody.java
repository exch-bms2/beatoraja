package bms.player.beatoraja.skin.lua;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/** Shared response-body limits for all Lua HTTP entry points. Counts received bytes, including newlines. */
final class SkinHttpBody {
    static final int DEFAULT_MAX_BYTES = 1024 * 1024;
    static final int MAX_BYTES = 8 * 1024 * 1024;
    static final int MAX_RETAINED_BYTES = 16 * 1024 * 1024;

    private SkinHttpBody() { }

    static int checkedLimit(double requested) {
        if (!Double.isFinite(requested) || requested != Math.rint(requested) || requested < 1 || requested > MAX_BYTES) {
            throw new IllegalArgumentException("HTTP max_bytes must be between 1 and " + MAX_BYTES);
        }
        return (int) requested;
    }

    static byte[] read(HttpURLConnection connection, int maxBytes) throws IOException {
        checkedLimit(maxBytes);
        if (connection.getContentLengthLong() > maxBytes) throw new IOException("HTTP response too large: limit=" + maxBytes);
        try (InputStream input = connection.getInputStream(); var output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer, 0, Math.min(buffer.length, maxBytes - output.size() + 1))) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new IOException("HTTP request cancelled");
                if (output.size() + count > maxBytes) throw new IOException("HTTP response too large: limit=" + maxBytes);
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    static BufferedReader reader(byte[] body) {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
    }
}
