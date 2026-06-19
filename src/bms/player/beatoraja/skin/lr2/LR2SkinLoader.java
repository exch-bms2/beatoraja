package bms.player.beatoraja.skin.lr2;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.ObjectMap;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.SkinLoader;
import bms.player.beatoraja.skin.property.*;

/**
 * LR2スキンローダー
 * 
 * @author exch
 */
public abstract class LR2SkinLoader extends SkinLoader {

	private final Map<String, Command<LR2SkinLoader>> commands = new HashMap<>();

	protected IntIntMap op = new IntIntMap();

	@SuppressWarnings("unchecked")
	protected <T extends LR2SkinLoader> void addCommandWord(Command<T> cm) {
		commands.putIfAbsent(cm.name().toUpperCase(Locale.ROOT), (Command<LR2SkinLoader>) cm);
	}

	@SafeVarargs
	protected final <T extends LR2SkinLoader> void addCommandWord(Command<T>... cm) {
		for (Command<T> command : cm) {
			addCommandWord(command);
		}
	}

	boolean skip = false;
	boolean ifs = false;

	protected void processLine(String line, MainState state) {
		if (!line.startsWith("#") ) {
			return;
		}
		String[] str = line.split(",", -1);
		if (str.length > 0) {
			if (str[0].equalsIgnoreCase("#IF")) {
				ifs = evaluateCondition(str, state, true);
				skip = !ifs;
			} else if (str[0].equalsIgnoreCase("#ELSEIF")) {
				if (ifs) {
					skip = true;
				} else {
					ifs = evaluateCondition(str, state, false);
					skip = !ifs;
				}
			} else if (str[0].equalsIgnoreCase("#ELSE")) {
				skip = ifs;
			} else if (str[0].equalsIgnoreCase("#ENDIF")) {
				skip = false;
				ifs = false;
			}
			if (!skip) {
				if (str[0].equalsIgnoreCase("#SETOPTION")) {
					int index = Integer.parseInt(str[1]);
					op.put(index, Integer.parseInt(str[2]) >= 1 ? 1 : 0);
				}

				Command<LR2SkinLoader> command = commands.get(str[0].substring(1).toUpperCase(Locale.ROOT));
				if(command != null) {
					command.execute(this, str);					
				}
			}
		}
	}

	public IntIntMap getOption() {
		return op;
	}

	private boolean evaluateCondition(String[] str, MainState state, boolean logParseError) {
		for (int i = 1; i < str.length; i++) {
			if (str[i].isEmpty()) {
				continue;
			}
			try {
				if (!isConditionEnabled(str[i], state)) {
					return false;
				}
			} catch (NumberFormatException e) {
				if (logParseError) {
					e.printStackTrace();
				}
				return false;
			}
		}
		return true;
	}

	private boolean isConditionEnabled(String value, MainState state) {
		int opt = Integer.parseInt(value.replace('!', '-').replaceAll("[^0-9-]", ""));
		boolean enabled = opt >= 0 ? op.get(opt, -1) == 1 : op.get(-opt, -1) == 0;
		if (!enabled && !op.containsKey(Math.abs(opt)) && state != null) {
			BooleanProperty draw = BooleanPropertyFactory.getBooleanProperty(opt);
			enabled = draw != null && draw.get(state);
		}
		return enabled;
	}
	
	protected static File getPath(String skinpath, String imagepath, ObjectMap<String, String> filemap) {
		return SkinLoader.getPath(imagepath.replace("LR2files\\Theme", skinpath).replace("\\", "/"), filemap);
	}

	public abstract class CommandWord implements Command<LR2SkinLoader> {

		public final String str;

		public String name() {
			return str;
		}
		
		public CommandWord(String str) {
			this.str = str;
		}

		public void execute(LR2SkinLoader loader, String[] values) {
			execute(values);
		}

		public abstract void execute(String[] values);

	}
	
	public interface Command<T extends LR2SkinLoader> {
		
		public abstract String name();
		public abstract void execute(T loader, String[] values);		
	}
}
