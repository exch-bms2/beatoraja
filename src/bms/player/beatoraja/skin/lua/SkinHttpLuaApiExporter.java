package bms.player.beatoraja.skin.lua;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import com.badlogic.gdx.math.MathUtils;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;

final class SkinHttpLuaApiExporter implements LuaApiExporter {

	private static final int HTTP_MAX_LINES = 1024;
	private static final int HTTP_MAX_CHARS = 65536;
	private static final int HTTP_DEFAULT_TIMEOUT_MS = 1000;
	private static final int HTTP_MAX_TIMEOUT_MS = 5000;

	@Override
	public void export(LuaTable table) {
		VarArgFunction getLines = new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs args) {
				String url = args.checkjstring(1);
				int timeout = MathUtils.clamp(args.optint(2, HTTP_DEFAULT_TIMEOUT_MS), 1, HTTP_MAX_TIMEOUT_MS);
				try {
					return LuaValue.varargsOf(readHttpLines(url, timeout), LuaBoolean.TRUE);
				} catch(IOException | RuntimeException e) {
					return LuaValue.varargsOf(LuaValue.NIL, LuaString.valueOf(e.getMessage() != null ? e.getMessage() : e.toString()));
				}
			}
		};
		table.set("http_get_lines", getLines);
		table.set("http_get", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs args) {
				Varargs result = getLines.invoke(args);
				if(result.arg1().isnil()) {
					return result;
				}
				LuaTable lines = result.arg1().checktable();
				StringBuilder text = new StringBuilder();
				for(int i = 1;i <= lines.length();i++) {
					text.append(lines.get(i).tojstring());
					if(i < lines.length()) {
						text.append('\n');
					}
				}
				return LuaValue.varargsOf(LuaString.valueOf(text.toString()), LuaBoolean.TRUE);
			}
		});
	}

	private LuaTable readHttpLines(String urlText, int timeout) throws IOException {
		URI uri = URI.create(urlText);
		String scheme = uri.getScheme();
		if(!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
			throw new IOException("unsupported scheme: " + scheme);
		}

		URL url = uri.toURL();
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);

		LuaTable lines = new LuaTable();
		int charCount = 0;
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
			for(int i = 1;i <= HTTP_MAX_LINES;i++) {
				String line = reader.readLine();
				if(line == null) {
					break;
				}
				charCount += line.length();
				if(charCount > HTTP_MAX_CHARS) {
					throw new IOException("response is too large");
				}
				lines.set(i, line);
			}
		} finally {
			connection.disconnect();
		}
		return lines;
	}
}
