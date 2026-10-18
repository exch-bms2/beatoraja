package bms.player.beatoraja.skin.lua;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
	private static final int HTTP_DEFAULT_TIMEOUT_MS = 1000;
	private static final int HTTP_MAX_TIMEOUT_MS = 5000;

	static void install(Globals globals, Supplier<Path> skinDirectorySupplier) {
		SkinLuaPathResolver pathResolver = new SkinLuaPathResolver(skinDirectorySupplier);
		LuaTable luajava = new LuaTable();
		luajava.set("bindClass", new BindClassFunction());
		luajava.set("new", new NewFunction(pathResolver));
		luajava.set("newInstance", new NewInstanceFunction(pathResolver));
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
		private final SkinLuaPathResolver pathResolver;

		private NewInstanceFunction(SkinLuaPathResolver pathResolver) {
			this.pathResolver = pathResolver;
		}

		@Override
		public Varargs invoke(Varargs arguments) {
			return switch (arguments.checkjstring(1)) {
				case "java.io.File" -> fileFacade(pathResolver, arguments.checkjstring(2));
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
		LuaTable metatable = new LuaTable();
		metatable.set("__tostring", new OneArgFunction() {
			@Override public LuaValue call(LuaValue ignored) {
				return LuaValue.valueOf(path.toString().replace('\\', '/'));
			}
		});
		facade.setmetatable(metatable);
		facade.set("getName", new OneArgFunction() {
			@Override public LuaValue call(LuaValue ignored) { return LuaValue.valueOf(path.getFileName().toString()); }
		});
		facade.set("getPath", metatable.get("__tostring"));
		facade.set("isFile", new OneArgFunction() {
			@Override public LuaValue call(LuaValue ignored) {
				return LuaValue.valueOf(Files.isRegularFile(pathResolver.resolve(path.toString())));
			}
		});
		facade.set("isDirectory", new OneArgFunction() {
			@Override public LuaValue call(LuaValue ignored) {
				return LuaValue.valueOf(Files.isDirectory(pathResolver.resolve(path.toString())));
			}
		});
		facade.set("mkdir", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				try {
					Files.createDirectory(pathResolver.resolve(path.toString()));
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
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(pathResolver.resolve(path.toString()))) {
					int index = 1;
					for (Path file : stream) {
						if (index > 4096) throw new IOException("Too many skin directory entries");
						if (!Files.isSymbolicLink(file)) files.set(index++, fileFacade(pathResolver, file.toString()));
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
		facade.set("setResponseLimitBytes", new TwoArgFunction() {
			@Override public LuaValue call(LuaValue ignored, LuaValue limit) {
				connection.maxBytes = SkinHttpBody.checkedLimit(limit.checkdouble());
				return LuaValue.NIL;
			}
		});
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
				return linesFacade(connection.readBody());
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

	private static LuaTable linesFacade(byte[] body) {
		LuaTable facade = new LuaTable();
		final BufferedReader[] reader = { SkinHttpBody.reader(body) };
		facade.set("close", new OneArgFunction() {
			@Override public LuaValue call(LuaValue ignored) {
				if (reader[0] != null) {
					try { reader[0].close(); } catch (IOException e) { }
					reader[0] = null;
				}
				return LuaValue.NIL;
			}
		});
		facade.set("readLine", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored) {
				if (reader[0] == null) return LuaValue.NIL;
				try {
					String line = reader[0].readLine();
					if (line != null) return LuaValue.valueOf(line);
					facade.get("close").call();
					return LuaValue.NIL;
				} catch (IOException e) { throw new LuaError("Legacy Lua skin HTTP read failed: " + e.getMessage()); }
			}
		});
		return facade;
	}

	private static final class LegacyHttpConnection {
		private final String urlText;
		private String method = "GET";
		private int timeout = HTTP_DEFAULT_TIMEOUT_MS;
		private int maxBytes = SkinHttpBody.DEFAULT_MAX_BYTES;
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

		private byte[] readBody() {
			connect();
			try {
				return SkinHttpBody.read(connection, maxBytes);
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
				int key = lastArgument(arguments).checkint();
				return LuaValue.valueOf(Gdx.input != null && key >= -1 && key <= 255 && Gdx.input.isKeyPressed(key));
			}
		});
		input.set("isKeyJustPressed", new VarArgFunction() {
			@Override public Varargs invoke(Varargs args) {
				int key = lastArgument(args).checkint();
				return LuaValue.valueOf(Gdx.input != null && key >= 0 && key <= 255 && Gdx.input.isKeyJustPressed(key));
			}
		});
		input.set("isButtonPressed", new VarArgFunction() {
			@Override public Varargs invoke(Varargs args) {
				int button = lastArgument(args).checkint();
				return LuaValue.valueOf(Gdx.input != null && button >= 0 && button <= 4 && Gdx.input.isButtonPressed(button));
			}
		});
		input.set("getX", inputNumber(Input::getX));
		input.set("getY", inputNumber(Input::getY));
		input.set("getDeltaX", inputNumber(Input::getDeltaX));
		input.set("getDeltaY", inputNumber(Input::getDeltaY));
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
					// Legacy skins use Java constant names (UP, Enter), not libGDX display labels.
					return LuaValue.valueOf(Input.Keys.class.getField(key.checkjstring().toUpperCase(java.util.Locale.ROOT)).getInt(null));
				} catch (ReflectiveOperationException | IllegalArgumentException e) {
					int code = Input.Keys.valueOf(key.checkjstring());
					return code >= 0 ? LuaValue.valueOf(code) : LuaValue.NIL;
				}
			}
		});
		keys.setmetatable(metatable);
		facade.set("Keys", keys);
		LuaTable buttons = new LuaTable();
		buttons.set("LEFT", Input.Buttons.LEFT);
		buttons.set("RIGHT", Input.Buttons.RIGHT);
		buttons.set("MIDDLE", Input.Buttons.MIDDLE);
		buttons.set("BACK", Input.Buttons.BACK);
		buttons.set("FORWARD", Input.Buttons.FORWARD);
		facade.set("Buttons", buttons);
		return facade;
	}

	private static LuaValue inputNumber(java.util.function.ToIntFunction<Input> getter) {
		return new OneArgFunction() {
			@Override public LuaValue call(LuaValue ignored) {
				return LuaValue.valueOf(Gdx.input != null ? getter.applyAsInt(Gdx.input) : 0);
			}
		};
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
