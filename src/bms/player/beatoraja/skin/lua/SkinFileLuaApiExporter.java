package bms.player.beatoraja.skin.lua;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

final class SkinFileLuaApiExporter implements LuaApiExporter {

	private final SkinLuaPathResolver pathResolver;

	SkinFileLuaApiExporter(SkinLuaPathResolver pathResolver) {
		this.pathResolver = pathResolver;
	}

	@Override
	public void export(LuaTable table) {
		table.set("file_exists", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				return LuaBoolean.valueOf(Files.exists(pathResolver.resolve(path.tojstring())));
			}
		});
		table.set("file_mkdir", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				try {
					Files.createDirectories(pathResolver.resolve(path.tojstring()));
					return LuaBoolean.TRUE;
				} catch(IOException | RuntimeException e) {
					return LuaBoolean.FALSE;
				}
			}
		});
		table.set("file_list", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs args) {
				return list(args);
			}
		});
		table.set("file_read_lines", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				return readLines(path.tojstring());
			}
		});
		table.set("file_write", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue path, LuaValue text) {
				return LuaBoolean.valueOf(write(path.tojstring(), text.tojstring(), false));
			}
		});
		table.set("file_append", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue path, LuaValue text) {
				return LuaBoolean.valueOf(write(path.tojstring(), text.tojstring(), true));
			}
		});
		table.set("file_clear", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				return LuaBoolean.valueOf(write(path.tojstring(), "", false));
			}
		});
		table.set("file_count_lines", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				return LuaInteger.valueOf(countLines(path.tojstring()));
			}
		});
	}

	private Varargs list(Varargs args) {
		try {
			Path directory = pathResolver.resolve(args.checkjstring(1));
			String patternText = args.narg() >= 2 && !args.isnil(2) ? args.tojstring(2) : null;
			Pattern pattern = patternText != null && !patternText.isEmpty() ? Pattern.compile(toJavaRegex(patternText)) : null;
			StringBuilder paths = new StringBuilder();
			int count = 0;
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
				for(Path path : stream) {
					String normalized = path.toString().replace('\\', '/');
					if(pattern == null) {
						paths.append(normalized).append('\n');
						count++;
					} else {
						Matcher matcher = pattern.matcher(normalized);
						if(matcher.find()) {
							paths.append(matcher.group()).append('\n');
							count++;
						}
					}
				}
			}
			return LuaValue.varargsOf(LuaString.valueOf(paths.toString()), LuaInteger.valueOf(count));
		} catch(IOException | RuntimeException e) {
			return LuaValue.varargsOf(LuaString.valueOf(""), LuaInteger.ZERO);
		}
	}

	private LuaTable readLines(String path) {
		LuaTable lines = new LuaTable();
		try {
			List<String> fileLines = Files.readAllLines(pathResolver.resolve(path), StandardCharsets.UTF_8);
			for(int i = 0;i < fileLines.size();i++) {
				lines.set(i + 1, fileLines.get(i));
			}
		} catch(IOException | RuntimeException e) {
		}
		return lines;
	}

	private boolean write(String path, String text, boolean append) {
		try {
			Path resolved = pathResolver.resolve(path);
			Path parent = resolved.getParent();
			if(parent != null) {
				Files.createDirectories(parent);
			}
			if(append) {
				Files.writeString(resolved, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} else {
				Files.writeString(resolved, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			}
			return true;
		} catch(IOException | RuntimeException e) {
			return false;
		}
	}

	private int countLines(String path) {
		try {
			return Files.readAllLines(pathResolver.resolve(path), StandardCharsets.UTF_8).size();
		} catch(IOException | RuntimeException e) {
			return 0;
		}
	}

	private static String toJavaRegex(String luaPattern) {
		StringBuilder regex = new StringBuilder();
		boolean escaped = false;
		for(int i = 0;i < luaPattern.length();i++) {
			char c = luaPattern.charAt(i);
			if(escaped) {
				regex.append('\\').append(c);
				escaped = false;
			} else if(c == '%') {
				escaped = true;
			} else {
				regex.append(c);
			}
		}
		if(escaped) {
			regex.append('%');
		}
		return regex.toString();
	}
}
