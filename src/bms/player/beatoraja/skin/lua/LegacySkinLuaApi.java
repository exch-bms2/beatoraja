package bms.player.beatoraja.skin.lua;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;

/**
 * luajavaを使用している旧スキン向けのfacadeクラス.
 * 限定的なJavaクラス/メソッドへのアクセスを提供する.
 *
 * 暫定対応のため、将来的には削除される予定.
 *
 * @author exch
 */
final class LegacySkinLuaApi {

	private LegacySkinLuaApi() {
	}

	private static final String CLASS_NAME = "__legacy_class";
	private static final int HTTP_MAX_LINES = 1024;
	private static final int HTTP_MAX_CHARS = 65536;
	private static final int HTTP_DEFAULT_TIMEOUT_MS = 1000;
	private static final int HTTP_MAX_TIMEOUT_MS = 5000;

	static void install(Globals globals, Supplier<Path> skinDirectorySupplier) {
		SkinLuaPathResolver pathResolver = new SkinLuaPathResolver(skinDirectorySupplier);
		LuaTable luajava = new LuaTable();
		luajava.set("bindClass", new BindClassFunction());
		luajava.set("new", new NewFunction(pathResolver));
		luajava.set("newInstance", new NewInstanceFunction());
		globals.set("luajava", luajava);
		globals.package_.setIsLoaded("luajava", luajava);

		LuaTable debug = new LuaTable();
		debug.set("getmetatable", globals.get("getmetatable"));
		globals.set("debug", debug);
		globals.package_.setIsLoaded("debug", debug);
	}

	private static final class BindClassFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue className) {
			return switch (className.checkjstring()) {
				case "com.badlogic.gdx.Gdx" -> gdxFacade();
				case "com.badlogic.gdx.Input" -> inputClassFacade();
				case "com.badlogic.gdx.controllers.Controllers" -> controllersFacade();
				case "com.badlogic.gdx.controllers.Controller", "java.io.File" -> classFacade(className.tojstring());
				default -> throw new LuaError("Legacy Lua skin class access denied: " + className);
			};
		}
	}

	private static LuaTable classFacade(String className) {
		LuaTable facade = new LuaTable();
		facade.set(CLASS_NAME, className);
		return facade;
	}

	private static final class NewFunction extends VarArgFunction {
		private final SkinLuaPathResolver pathResolver;

		private NewFunction(SkinLuaPathResolver pathResolver) {
			this.pathResolver = pathResolver;
		}

		@Override
		public Varargs invoke(Varargs arguments) {
			LuaValue classFacade = arguments.arg1();
			if (!classFacade.istable()) {
				throw new LuaError("Legacy Lua skin constructor access denied");
			}
			String className = classFacade.get(CLASS_NAME).optjstring(null);
			if ("java.io.File".equals(className)) {
				return fileFacade(pathResolver, arguments.checkjstring(2));
			}
			throw new LuaError("Legacy Lua skin constructor access denied: " + className);
		}
	}

	private static final class NewInstanceFunction extends VarArgFunction {
		@Override
		public Varargs invoke(Varargs arguments) {
			return switch (arguments.checkjstring(1)) {
				case "java.net.URL" -> urlFacade(arguments.checkjstring(2));
				case "java.io.InputStreamReader" -> arguments.arg(2);
				case "java.io.BufferedReader" -> bufferedReaderFacade(arguments.arg(2));
				default -> throw new LuaError("Legacy Lua skin constructor access denied: " + arguments.arg1());
			};
		}
	}

	private static LuaTable fileFacade(SkinLuaPathResolver pathResolver, String pathText) {
		Path path = pathResolver.resolve(pathText);
		LuaTable facade = new LuaTable();
		facade.set("mkdir", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				try {
					Files.createDirectory(path);
					return LuaValue.TRUE;
				} catch (IOException | RuntimeException e) {
					return LuaValue.FALSE;
				}
			}
		});
		facade.set("listFiles", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				LuaTable files = new LuaTable();
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
					int index = 1;
					for (Path file : stream) {
						files.set(index++, file.toString().replace('\\', '/'));
					}
				} catch (IOException | RuntimeException e) {
					return LuaValue.NIL;
				}
				return files;
			}
		});
		return facade;
	}

	private static LuaTable urlFacade(String urlText) {
		LegacyHttpConnection connection = new LegacyHttpConnection(urlText);
		LuaTable facade = new LuaTable();
		facade.set("openConnection", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				return connectionFacade(connection);
			}
		});
		return facade;
	}

	private static LuaTable connectionFacade(LegacyHttpConnection connection) {
		LuaTable facade = new LuaTable();
		facade.set("setRequestMethod", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored, LuaValue method) {
				connection.setRequestMethod(method.checkjstring());
				return LuaValue.NIL;
			}
		});
		facade.set("setConnectTimeout", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored, LuaValue timeout) {
				connection.setTimeout(timeout.checkint());
				return LuaValue.NIL;
			}
		});
		facade.set("connect", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				connection.connect();
				return LuaValue.NIL;
			}
		});
		facade.set("getResponseCode", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				return LuaValue.valueOf(connection.getResponseCode());
			}
		});
		facade.set("getInputStream", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				return linesFacade(connection.readLines());
			}
		});
		return facade;
	}

	private static LuaTable bufferedReaderFacade(LuaValue input) {
		if (!input.istable()) {
			throw new LuaError("Legacy Lua skin reader access denied");
		}
		return input.checktable();
	}

	private static LuaTable linesFacade(List<String> lines) {
		LuaTable facade = new LuaTable();
		final int[] position = { 0 };
		facade.set("readLine", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				return position[0] < lines.size() ? LuaValue.valueOf(lines.get(position[0]++)) : LuaValue.NIL;
			}
		});
		return facade;
	}

	private static final class LegacyHttpConnection {
		private final String urlText;
		private String method = "GET";
		private int timeout = HTTP_DEFAULT_TIMEOUT_MS;
		private HttpURLConnection connection;

		private LegacyHttpConnection(String urlText) {
			this.urlText = urlText;
		}

		private void setRequestMethod(String method) {
			if (!"GET".equals(method)) {
				throw new LuaError("Legacy Lua skin HTTP method denied: " + method);
			}
			this.method = method;
		}

		private void setTimeout(int timeout) {
			this.timeout = Math.max(1, Math.min(timeout, HTTP_MAX_TIMEOUT_MS));
		}

		private void connect() {
			if (connection != null) {
				return;
			}
			try {
				URI uri = URI.create(urlText);
				String scheme = uri.getScheme();
				if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
					throw new IOException("unsupported scheme: " + scheme);
				}
				URL url = uri.toURL();
				connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod(method);
				connection.setConnectTimeout(timeout);
				connection.setReadTimeout(timeout);
				connection.connect();
			} catch (IOException | RuntimeException e) {
				throw new LuaError("Legacy Lua skin HTTP connection failed: " + e.getMessage());
			}
		}

		private int getResponseCode() {
			connect();
			try {
				return connection.getResponseCode();
			} catch (IOException e) {
				throw new LuaError("Legacy Lua skin HTTP response failed: " + e.getMessage());
			}
		}

		private List<String> readLines() {
			connect();
			List<String> lines = new ArrayList<>();
			int characters = 0;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
				for (int i = 0; i < HTTP_MAX_LINES; i++) {
					String line = reader.readLine();
					if (line == null) {
						break;
					}
					characters += line.length();
					if (characters > HTTP_MAX_CHARS) {
						throw new IOException("response is too large");
					}
					lines.add(line);
				}
				return lines;
			} catch (IOException e) {
				throw new LuaError("Legacy Lua skin HTTP read failed: " + e.getMessage());
			} finally {
				connection.disconnect();
			}
		}
	}

	private static LuaTable gdxFacade() {
		LuaTable facade = new LuaTable();
		LuaTable graphics = new LuaTable();
		graphics.set("getWidth", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				return LuaValue.valueOf(Gdx.graphics != null ? Gdx.graphics.getWidth() : 0);
			}
		});
		graphics.set("getHeight", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				return LuaValue.valueOf(Gdx.graphics != null ? Gdx.graphics.getHeight() : 0);
			}
		});
		facade.set("graphics", graphics);

		LuaTable input = new LuaTable();
		input.set("isKeyPressed", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs arguments) {
				return LuaValue.valueOf(Gdx.input != null && Gdx.input.isKeyPressed(lastArgument(arguments).checkint()));
			}
		});
		facade.set("input", input);
		return facade;
	}

	private static LuaTable inputClassFacade() {
		LuaTable facade = new LuaTable();
		LuaTable keys = new LuaTable();
		LuaTable metatable = new LuaTable();
		metatable.set("__index", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue table, LuaValue key) {
				try {
					return LuaValue.valueOf(Input.Keys.valueOf(key.checkjstring()));
				} catch (IllegalArgumentException e) {
					return LuaValue.NIL;
				}
			}
		});
		keys.setmetatable(metatable);
		facade.set("Keys", keys);
		return facade;
	}

	private static LuaTable controllersFacade() {
		LuaTable facade = new LuaTable();
		facade.set("getControllers", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				Array<Controller> controllers = Controllers.getControllers();
				LuaTable result = new LuaTable();
				result.set("size", controllers.size);
				result.set("first", new OneArgFunction() {
					@Override
					public LuaValue call(LuaValue ignored) {
						return controllers.size > 0 ? controllerFacade(controllers.first()) : LuaValue.NIL;
					}
				});
				return result;
			}
		});
		return facade;
	}

	private static LuaTable controllerFacade(Controller controller) {
		LuaTable facade = new LuaTable();
		facade.set("getButton", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs arguments) {
				return LuaValue.valueOf(controller.getButton(lastArgument(arguments).checkint()));
			}
		});
		facade.set("getName", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				return LuaValue.valueOf(controller.getName());
			}
		});
		return facade;
	}

	private static LuaValue lastArgument(Varargs arguments) {
		return arguments.arg(arguments.narg());
	}
}
