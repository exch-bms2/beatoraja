package bms.player.beatoraja.skin.lua;

import java.io.*;
import java.net.*;

import com.badlogic.gdx.math.MathUtils;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;

final class SkinHttpLuaApiExporter implements LuaApiExporter {

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
					int maxBytes = SkinHttpBody.checkedLimit(args.optdouble(3, SkinHttpBody.DEFAULT_MAX_BYTES));
					return LuaValue.varargsOf(readHttpLines(url, timeout, maxBytes), LuaBoolean.TRUE);
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

	private LuaTable readHttpLines(String urlText, int timeout, int maxBytes) throws IOException {
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

		try {
			return toLuaLines(SkinHttpBody.read(connection, maxBytes));
		} finally {
			connection.disconnect();
		}
	}

	static LuaTable toLuaLines(byte[] body) throws IOException {
		LuaTable lines = new LuaTable();
		try (BufferedReader reader = SkinHttpBody.reader(body)) {
			String line;
			int index = 1;
			while ((line = reader.readLine()) != null) lines.set(index++, line);
		}
		return lines;
	}
}
