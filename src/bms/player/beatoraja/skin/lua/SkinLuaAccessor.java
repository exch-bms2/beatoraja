package bms.player.beatoraja.skin.lua;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.logging.Logger;

import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinProperty;
import bms.player.beatoraja.skin.SkinHeader.CustomOffset;
import bms.player.beatoraja.skin.SkinHeader.CustomOption;
import bms.player.beatoraja.skin.property.*;
import bms.player.beatoraja.SkinConfig;

/**
 * Luaスキンからデータを参照するためのクラス
 * 
 * @author excln
 */
public class SkinLuaAccessor {
	
	private final Globals globals;
	private Path sandboxRoot;

	// 各機能のエクスポート先を globals にするかどうか
	private final boolean isGlobal;

	// isGlobal == false のとき、エクスポートするモジュール名
	private static final String MAIN_STATE = "main_state";
	private static final String TIMER_UTIL = "timer_util";
	private static final String EVENT_UTIL = "event_util";

	public SkinLuaAccessor(boolean isGlobal) {
		globals = JsePlatform.standardGlobals();
		this.isGlobal = isGlobal;

		initializeModules();
	}

	public SkinLuaAccessor(boolean isGlobal, Path sandboxRoot) {
		this.isGlobal = isGlobal;
		this.sandboxRoot = sandboxRoot != null ? normalizeSandboxRoot(sandboxRoot) : Path.of("").toAbsolutePath().normalize();
		globals = createSandboxGlobals(this.sandboxRoot);

		globals.finder = new SkinResourceFinder(this.sandboxRoot);
		restrictPackageLoaders();
		initializeModules();
	}

	private void initializeModules() {
		if (!isGlobal) {
			// ヘッダ読み込み時に require("main_state") だけでエラーになると面倒なので、空のテーブルを入れておく
			globals.package_.setIsLoaded(MAIN_STATE, new LuaTable());
			globals.package_.setIsLoaded(TIMER_UTIL, new LuaTable());
			globals.package_.setIsLoaded(EVENT_UTIL, new LuaTable());
		}
	}

	private static Globals createSandboxGlobals(Path sandboxRoot) {
		Globals sandbox = new Globals();
		sandbox.load(new BaseLib());
		sandbox.load(new PackageLib());
		sandbox.load(new Bit32Lib());
		sandbox.load(new TableLib());
		sandbox.load(new StringLib());
		sandbox.load(new CoroutineLib());
		sandbox.load(new MathLib());
		sandbox.load(new SandboxIoLib(sandboxRoot));
		LoadState.install(sandbox);
		LuaC.install(sandbox);
		return sandbox;
	}

	private void restrictPackageLoaders() {
		globals.set("os", LuaValue.NIL);
		globals.set("luajava", LuaValue.NIL);
		globals.set("debug", LuaValue.NIL);

		LuaTable pkg = globals.get("package").checktable();
		pkg.set("loadlib", LuaValue.NIL);
		pkg.set("cpath", "");
		removePackageSearcher(pkg.get("searchers"));
		removePackageSearcher(pkg.get("loaders"));
	}

	private void removePackageSearcher(LuaValue searchers) {
		if (searchers.istable()) {
			searchers.set(3, LuaValue.NIL);
			searchers.set(4, LuaValue.NIL);
		}
	}

	private static class SandboxIoLib extends IoLib {
		private final Path root;

		private SandboxIoLib(Path root) {
			this.root = root;
		}

		@Override
		protected File wrapStdin() {
			return new SandboxFile(new byte[0]);
		}

		@Override
		protected File wrapStdout() {
			return new SandboxFile();
		}

		@Override
		protected File wrapStderr() {
			return new SandboxFile();
		}

		@Override
		protected File openFile(String filename, boolean readMode, boolean appendMode, boolean updateMode, boolean binaryMode) throws IOException {
			Path path = resolve(filename);
			if (!path.startsWith(root)) {
				throw new IOException("Lua sandbox access denied: " + filename);
			}
			if (readMode) {
				if (!Files.isRegularFile(path)) {
					throw new IOException("Lua sandbox file not found: " + filename);
				}
				return new SandboxFile(Files.readAllBytes(path));
			}
			return new SandboxFile();
		}

		@Override
		protected File tmpFile() {
			return new SandboxFile();
		}

		@Override
		protected File openProgram(String prog, String mode) throws IOException {
			throw new IOException("Lua sandbox popen is not allowed");
		}

		private Path resolve(String filename) {
			Path path = Path.of(filename);
			if (path.isAbsolute()) {
				return path.normalize();
			}

			Path workingDirectoryPath = path.toAbsolutePath().normalize();
			if (workingDirectoryPath.startsWith(root)) {
				return workingDirectoryPath;
			}
			return root.resolve(path).toAbsolutePath().normalize();
		}

		private class SandboxFile extends File {
			private byte[] data;
			private int pos;
			private boolean closed;
			private final ByteArrayOutputStream output;

			private SandboxFile() {
				this.data = new byte[0];
				this.output = new ByteArrayOutputStream();
			}

			private SandboxFile(byte[] data) {
				this.data = data;
				this.output = null;
			}

			@Override
			public void write(LuaString string) throws IOException {
				checkClosed();
				if (output != null) {
					output.write(string.m_bytes, string.m_offset, string.m_length);
				}
			}

			@Override
			public void flush() throws IOException {
				checkClosed();
			}

			@Override
			public boolean isstdfile() {
				return false;
			}

			@Override
			public void close() {
				closed = true;
			}

			@Override
			public boolean isclosed() {
				return closed;
			}

			@Override
			public int seek(String option, int bytecount) throws IOException {
				checkClosed();
				int base = switch (option) {
					case "set" -> 0;
					case "cur" -> pos;
					case "end" -> data.length;
					default -> throw new IOException("invalid seek option: " + option);
				};
				pos = Math.max(0, Math.min(data.length, base + bytecount));
				return pos;
			}

			@Override
			public void setvbuf(String mode, int size) {
			}

			@Override
			public int remaining() throws IOException {
				checkClosed();
				return Math.max(0, data.length - pos);
			}

			@Override
			public int peek() throws IOException, EOFException {
				checkClosed();
				if (pos >= data.length) {
					throw new EOFException();
				}
				return data[pos] & 0xff;
			}

			@Override
			public int read() throws IOException, EOFException {
				checkClosed();
				if (pos >= data.length) {
					throw new EOFException();
				}
				return data[pos++] & 0xff;
			}

			@Override
			public int read(byte[] bytes, int offset, int length) throws IOException {
				checkClosed();
				if (pos >= data.length) {
					return -1;
				}
				int readLength = Math.min(length, data.length - pos);
				System.arraycopy(data, pos, bytes, offset, readLength);
				pos += readLength;
				return readLength;
			}

			private void checkClosed() throws IOException {
				if (closed) {
					throw new IOException("file is closed");
				}
			}
		}
	}

	public BooleanProperty loadBooleanProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			return loadBooleanProperty(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public BooleanProperty loadBooleanProperty(LuaFunction function) {
		return new BooleanProperty() {
			@Override
			public boolean isStatic(MainState state) {
				return false;
			}

			@Override
			public boolean get(MainState state) {
				try {
					return function.call().toboolean();
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外 : " + e.getMessage());
					return false;
				}
			}
		};
	}

	public IntegerProperty loadIntegerProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			return loadIntegerProperty(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public IntegerProperty loadIntegerProperty(LuaFunction function) {
		return new IntegerProperty() {
			@Override
			public int get(MainState state) {
				try{
					return function.call().toint();
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外 : " + e.getMessage());
					return 0;
				}
			}
		};
	}

	public FloatProperty loadFloatProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			return loadFloatProperty(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public FloatProperty loadFloatProperty(LuaFunction function) {
		return new FloatProperty() {
			@Override
			public float get(MainState state) {
				try{
					return function.call().tofloat();
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外 : " + e.getMessage());
					return 0f;
				}
			}
		};
	}

	public StringProperty loadStringProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			return loadStringProperty(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public StringProperty loadStringProperty(LuaFunction function) {
		return new StringProperty() {
			@Override
			public String get(MainState state) {
				try {
					return function.call().tojstring();
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外：" + e.getMessage());
					return "";
				}
			}
		};
	}

	/**
	 * Creates a timer property from Lua code.
	 * If {@code script} returns a function, the returned function is regarded as a timer function
	 * which will be called every frame or more frequently.
	 * Otherwise, {@code script} itself is regarded as a timer function.
	 * <p>NOTE: The former case is useful to synthesize a stateful custom timer in a JSON skin.</p>
	 * <p>NOTE: A timer function returns (i) start time in microseconds if on, or (ii) Long.MIN_VALUE if off.</p>
	 * @param script Lua script producing a function (producing a number) or a number
	 * @return new timer property
	 */
	public TimerProperty loadTimerProperty(String script) {
		try {
			final LuaValue lv = globals.load("return " + script);
			final LuaValue trialCallResult = lv.call();
			if (trialCallResult.isfunction()) {
				// タイマー関数を返す場合
				return loadTimerProperty(trialCallResult.checkfunction());
			} else {
				// 数値を返す場合
				return loadTimerProperty(lv.checkfunction());
			}
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	/**
	 * Creates a timer property from Lua function.
	 * The given function is always regarded as a timer function which will be called every frame.
	 * @param timerFunction Lua function producing a number
	 * @return new timer property
	 */
	public TimerProperty loadTimerProperty(LuaFunction timerFunction) {
		return new TimerProperty() {
			@Override
			public long getMicro(MainState state) {
				try {
					return timerFunction.call().tolong();
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外：" + e.getMessage());
					return Long.MIN_VALUE;
				}
			}
		};
	}

	public Event loadEvent(String script) {
		try {
			final LuaValue lv = globals.load(script);
			return loadEvent(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public Event loadEvent(LuaFunction function) {
		switch (function.narg()) {
		case 0:
			return EventFactory.createZeroArgEvent(state -> {
				try{
					function.call();
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外 : " + e.getMessage());
				}
			});
		case 1:
			return EventFactory.createOneArgEvent((state, arg1) -> {
				try{
					function.call(LuaNumber.valueOf(arg1));
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外 : " + e.getMessage());
				}
			});
		case 2:
			return EventFactory.createTwoArgEvent((state, arg1, arg2) -> {
				try{
					function.call(LuaNumber.valueOf(arg1), LuaNumber.valueOf(arg2));
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外 : " + e.getMessage());
				}
			});
		default:
			return null;
		}
	}

	public FloatWriter loadFloatWriter(String script) {
		try {
			final LuaValue lv = globals.load(script);
			return loadFloatWriter(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public FloatWriter loadFloatWriter(LuaFunction function) {
		return new FloatWriter() {
			@Override
			public void set(MainState state, float value) {
				try{
					function.call(LuaDouble.valueOf(value));
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外：" + e.getMessage());
				}
			}
		};
	}

	public StringWriter loadStringWriter(String script) {
		try {
			final LuaValue lv = globals.load(script);
			return loadStringWriter(lv.checkfunction());
		} catch (RuntimeException e) {
			Logger.getGlobal().warning("Lua解析時の例外 : " + e.getMessage());
		}
		return null;
	}

	public StringWriter loadStringWriter(LuaFunction function) {
		return new StringWriter() {
			@Override
			public void set(MainState state, String value) {
				try{
					function.call(LuaString.valueOf(value));
				} catch (RuntimeException e) {
					Logger.getGlobal().warning("Lua実行時の例外：" + e.getMessage());
				}
			}
		};
	}

	public LuaValue exec(String script) {
		return globals.load(script).call();
	}

	public LuaValue execFile(Path path) {
		if (sandboxRoot != null) {
			Path normalizedPath = normalizeSandboxRoot(path);
			if (normalizedPath == null || !isInsideSandbox(normalizedPath) || !Files.isRegularFile(normalizedPath)) {
				throw new LuaError("Lua sandbox access denied: " + path);
			}
			try (InputStream stream = Files.newInputStream(normalizedPath)) {
				return globals.load(stream, "@" + normalizedPath, "bt", globals).call();
			} catch (IOException e) {
				throw new LuaError("Lua sandbox load failed: " + path + " : " + e.getMessage());
			}
		}
		return globals.loadfile(path.toString()).call();
	}

	public void setDirectory(Path path) {
		LuaTable pkg = globals.get("package").checktable();
		Path normalizedPath = normalizeSandboxRoot(path);
		if (sandboxRoot != null) {
			if (normalizedPath == null) {
				throw new LuaError("Lua sandbox directory is not specified");
			}
			if (!isInsideSandbox(normalizedPath)) {
				throw new LuaError("Lua sandbox access denied: " + path);
			}
			pkg.set("path", normalizedPath + "/?.lua;" + normalizedPath + "/?/init.lua");
		} else {
			pkg.set("path", pkg.get("path").tojstring() + ";" + path.toString() + "/?.lua");
		}
	}

	/**
	 * MainState にアクセスするための機能をエクスポートする。
	 * isGlobal == true のとき、グローバル変数としてそのまま追加
	 * それ以外のとき、モジュール "main_state" にエクスポート
	 * (Lua からは main_state = require("main_state") などとすることで利用可能)
	 * @param state MainState
	 */
	public void exportMainStateAccessor(MainState state) {
		MainStateAccessor accessor = new MainStateAccessor(state);
		if (isGlobal) {
			accessor.export(globals);
		} else {
			LuaTable mainStateTable = new LuaTable();
			accessor.export(mainStateTable);
			globals.package_.setIsLoaded(MAIN_STATE, mainStateTable);
		}
	}

	/**
	 * その他のユーティリティーをエクスポートする。
	 * ロードがそれほど重くなく、JSONスキンから使う可能性もあることが前提
	 * @param state MainState
	 */
	public void exportUtilities(MainState state) {
		TimerUtility timerUtil = new TimerUtility(state);
		EventUtility eventUtil = new EventUtility(state);
		if (isGlobal) {
			timerUtil.export(globals);
			eventUtil.export(globals);
		} else {
			LuaTable timerUtilTable = new LuaTable();
			timerUtil.export(timerUtilTable);
			globals.package_.setIsLoaded(TIMER_UTIL, timerUtilTable);
			LuaTable eventUtilTable = new LuaTable();
			eventUtil.export(eventUtilTable);
			globals.package_.setIsLoaded(EVENT_UTIL, eventUtilTable);
		}
	}

	/**
	 * スキン設定をエクスポートする。
	 * isGlobal にかかわらず、グローバル変数 skin_config にデータがセットされた状態にする。
	 * Lua スキンは skin_config が nil のときヘッダのみ読み込めるようにする。
	 * 
	 * @param header スキンヘッダデータ
	 * @param property Property (スキン設定データ)
	 * @param filePathGetter スキン設定を元にファイルパスを解決する関数
	 */
	public void exportSkinProperty(SkinHeader header, SkinConfig.Property property, Function<String, String> filePathGetter) {
		LuaTable table = new LuaTable();
		exportSkinPropertyToTable(header, property, filePathGetter, table);
		globals.set("skin_config", table);
	}

	private void exportSkinPropertyToTable(SkinHeader header, SkinConfig.Property property, Function<String, String> filePathGetter, LuaTable table) {
		LuaTable file_path = new LuaTable();
		for (SkinConfig.FilePath file : property.getFile()) {
			file_path.set(file.name, file.path);
		}
		table.set("file_path", file_path);
		table.set("get_path", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				return LuaString.valueOf(filePathGetter.apply(value.tojstring()));
			}
		});

		LuaTable options = new LuaTable();
		LuaTable enabled_options = new LuaTable();
		
		for (CustomOption option : header.getCustomOptions()) {
			int opvalue = option.getSelectedOption();
			options.set(option.name, opvalue);
			enabled_options.insert(enabled_options.length() + 1, LuaInteger.valueOf(opvalue));				
		}

		table.set("option", options);
		table.set("enabled_options", enabled_options);

		LuaTable offsets = new LuaTable();
		
		for(CustomOffset offset : header.getCustomOffsets()) {
			SkinConfig.Offset ofs = null;
			for (SkinConfig.Offset of : property.getOffset()) {
				if(offset.name.equals(of.name)) {
					ofs = of;
					break;
				}
			}
			if(ofs == null) {
				ofs = new SkinConfig.Offset();
				ofs.name = offset.name;
			}
			LuaTable offsetTable = new LuaTable();
			offsetTable.set("x", ofs.x);
			offsetTable.set("y", ofs.y);
			offsetTable.set("w", ofs.w);
			offsetTable.set("h", ofs.h);
			offsetTable.set("r", ofs.r);
			offsetTable.set("a", ofs.a);
			offsets.set(ofs.name, offsetTable);			
		}
		table.set("offset", offsets);
	}

	private static Path normalizeSandboxRoot(Path path) {
		return path != null ? path.toAbsolutePath().normalize() : null;
	}

	private boolean isInsideSandbox(Path path) {
		return sandboxRoot == null || path.toAbsolutePath().normalize().startsWith(sandboxRoot);
	}

	private static class SkinResourceFinder implements ResourceFinder {
		private final Path root;

		private SkinResourceFinder(Path root) {
			this.root = root;
		}

		@Override
		public InputStream findResource(String filename) {
			Path resolved = resolve(filename);
			if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
				return null;
			}
			try {
				return Files.newInputStream(resolved);
			} catch (IOException e) {
				return null;
			}
		}

		private Path resolve(String filename) {
			Path path = Path.of(filename);
			if (path.isAbsolute()) {
				return path.normalize();
			}

			Path workingDirectoryPath = path.toAbsolutePath().normalize();
			if (workingDirectoryPath.startsWith(root)) {
				return workingDirectoryPath;
			}
			return root.resolve(path).toAbsolutePath().normalize();
		}
	}
}
