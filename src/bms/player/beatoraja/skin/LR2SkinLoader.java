package bms.player.beatoraja.skin;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.LR2SkinHeader.CustomFile;
import bms.player.beatoraja.skin.LR2SkinHeader.CustomOption;

public class LR2SkinLoader {

	private List<CommandWord> commands = new ArrayList<CommandWord>();

	protected Map<Integer, Boolean> op = new HashMap<Integer, Boolean>();

	protected void addCommandWord(CommandWord cm) {
		commands.add(cm);
	}

	boolean skip = false;
	boolean ifs = false;

	protected void processLine(String line, MainState state) {
		if (!line.startsWith("#") ) {
			return;
		}
		String[] str = line.split(",", -1);
		if (str.length > 0) {
			if (str[0].equals("#IF")) {
				ifs = true;
				for (int i = 1; i < str.length; i++) {
					boolean b = false;
					if (str[i].length() == 0) {
						continue;
					}
					try {
						int opt = Integer.parseInt(str[i]);
						
						if(op.containsKey(opt) && op.get(opt)) {
							b = true;
						}
						if (!b && state != null) {
							b = state.getBooleanValue(opt);
						}
						if (!b) {
							ifs = false;
							break;
						}
					} catch (NumberFormatException e) {
						e.printStackTrace();
						break;
					}
				}

				skip = !ifs;
			} else if (str[0].equals("#ELSEIF")) {
				if (ifs) {
					skip = true;
				} else {
					ifs = true;
					for (int i = 1; i < str.length; i++) {
						boolean b = false;
						try {
							int opt = Integer.parseInt(str[i]);
							if(op.containsKey(opt) && op.get(opt)) {
								b = true;
							}
							if (!b && state != null) {
								b = state.getBooleanValue(opt);
							}
							if (!b) {
								ifs = false;
								break;
							}
						} catch (NumberFormatException e) {
							break;
						}
					}

					skip = !ifs;
				}
			} else if (str[0].equals("#ELSE")) {
				skip = ifs;
			} else if (str[0].equals("#ENDIF")) {
				skip = false;
				ifs = false;
			}
			if (!skip) {
				if (str[0].equals("#SETOPTION")) {
					int index = Integer.parseInt(str[1]);
					op.put(index, Integer.parseInt(str[2]) >= 1);
				}

				for (CommandWord cm : commands) {
					if (str[0].equals("#" + cm.str)) {
						cm.execute(str);
					}
				}
			}
		}
	}

	public Map<Integer, Boolean> getOption() {
		return op;
	}

	public abstract class CommandWord {

		public final String str;

		public CommandWord(String str) {
			this.str = str;
		}

		public abstract void execute(String[] values);
	}
}
