package bms.player.beatoraja.skin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

public class LR2SkinHeaderLoader {
	
	private List<Integer> op = new ArrayList<Integer>();

	public LR2SkinHeader loadSkin(Path f, MainState state) throws IOException {
		return this.loadSkin(f, state, new HashMap());
	}
	
	public LR2SkinHeader loadSkin(Path f, MainState state, Map<String, Object> property) throws IOException {
		LR2SkinHeader header = new LR2SkinHeader();
		header.setPath(f);

		BufferedReader br = Files.newBufferedReader(f, Charset.forName("MS932"));
		String line = null;

		boolean skip = false;
		boolean ifs = false;
		
		List<CustomFile> files = new ArrayList();
		List<CustomOption> options = new ArrayList();
		for(String key : property.keySet()) {
			if(property.get(key) != null && property.get(key) instanceof Integer) {
				op.add((Integer) property.get(key));
			}
		}

		while ((line = br.readLine()) != null) {
			if (!line.startsWith("//")) {
				String[] str = line.split(",", -1);
				if (str.length > 0) {
					if (str[0].equals("#IF")) {
						for (int i = 1; i < str.length; i++) {
							try {
								int opt = Integer.parseInt(str[i]);
								for (Integer o : op) {
									if (o == opt) {
										ifs = true;
										break;
									}
								}
								if (!ifs && state != null) {
									ifs = state.getBooleanValue(opt);
								}
							} catch (NumberFormatException e) {
								break;
							}
						}

						skip = !ifs;
					}
					if (str[0].equals("#ELSEIF")) {
						if (ifs) {
							skip = true;
						} else {
							for (int i = 1; i < str.length; i++) {
								try {
									int opt = Integer.parseInt(str[i]);
									for (Integer o : op) {
										if (o == opt) {
											ifs = true;
											break;
										}
									}
									if (!ifs && state != null) {
										ifs = state.getBooleanValue(opt);
									}
								} catch (NumberFormatException e) {
									break;
								}
							}

							skip = !ifs;
						}
					}
					if (str[0].equals("#ELSE")) {
						skip = ifs;
					}
					if (str[0].equals("#ENDIF")) {
						skip = false;
						ifs = false;
					}
					if (!skip) {
						if (str[0].equals("#SETOPTION")) {
							int index = Integer.parseInt(str[1]);
							if (Integer.parseInt(str[2]) >= 1) {
								op.add(index);
							} else {
								for(int i = 0;i < op.size();i++) {
									if(op.get(i) == index) {
										op.remove(i);
										break;
									}
								}
							}
						}
						if (str[0].equals("#INFORMATION")) {
							header.setMode(Integer.parseInt(str[1]));
							header.setName(str[2]);
						}
						if (str[0].equals("#CUSTOMOPTION")) {
							List<String> contents = new ArrayList();
							for(int i = 3;i < str.length;i++) {
								if(str[i] != null && str[i].length() > 0) {
									contents.add(str[i]);
								}
							}
							options.add(new CustomOption(str[1], Integer.parseInt(str[2]), contents.toArray(new String[contents.size()])));
						}
						if (str[0].equals("#CUSTOMFILE")) {
							files.add(new CustomFile(str[1], str[2].replace("LR2files\\Theme", "skin").replace("\\", "/")));
						}	
						if (str[0].equals("#RESOLUTION")) {
							header.setResolution(Integer.parseInt(str[1]));
						}
						if (str[0].equals("#INCLUDE")) {
							header.setInclude(str[1].replace("LR2files\\Theme", "skin").replace("\\", "/"));
						}	
					}
				}
			}
		}
		header.setCustomOptions(options.toArray(new CustomOption[options.size()]));
		header.setCustomFiles(files.toArray(new CustomFile[files.size()]));
		
		return header;
	}
	
	public int[] getOption() {
		int[] result = new int[op.size()];
		for(int i = 0;i < op.size();i++) {
			result[i] = op.get(i);
		}
		return result;
	}
}
