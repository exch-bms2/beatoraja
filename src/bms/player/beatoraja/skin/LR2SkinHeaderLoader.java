package bms.player.beatoraja.skin;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.LR2SkinHeader.CustomFile;
import bms.player.beatoraja.skin.LR2SkinHeader.CustomOption;

/**
 * LR2スキンヘッダファイル(lr2skin)のローダー
 * 
 * @author exch
 */
public class LR2SkinHeaderLoader extends LR2SkinLoader {
	
	private LR2SkinHeader header = new LR2SkinHeader();
	private List<CustomFile> files = new ArrayList();
	private List<CustomOption> options = new ArrayList();

	public LR2SkinHeaderLoader() {
		
		addCommandWord(new CommandWord("INFORMATION") {
			@Override
			public void execute(String[] str) {
				header.setMode(Integer.parseInt(str[1]));
				header.setName(str[2]);
			}
		});
		addCommandWord(new CommandWord("CUSTOMOPTION") {
			@Override
			public void execute(String[] str) {
				List<String> contents = new ArrayList();
				for(int i = 3;i < str.length;i++) {
					if(str[i] != null && str[i].length() > 0) {
						contents.add(str[i]);
					}
				}
				options.add(new CustomOption(str[1], Integer.parseInt(str[2]), contents.toArray(new String[contents.size()])));
			}
		});
		addCommandWord(new CommandWord("CUSTOMFILE") {
			@Override
			public void execute(String[] str) {
				files.add(new CustomFile(str[1], str[2].replace("LR2files\\Theme", "skin").replace("\\", "/")));
			}
		});
		addCommandWord(new CommandWord("RESOLUTION") {
			@Override
			public void execute(String[] str) {
				header.setResolution(Integer.parseInt(str[1]));
			}
		});
		addCommandWord(new CommandWord("INCLUDE") {
			@Override
			public void execute(String[] str) {
			}
		});

	}
	
	public LR2SkinHeader loadSkin(Path f, MainState state) throws IOException {
		return this.loadSkin(f, state, new HashMap());
	}
	
	public LR2SkinHeader loadSkin(Path f, MainState state, Map<String, Object> property) throws IOException {
		header = new LR2SkinHeader();
		files.clear();
		options.clear();
		
		header.setPath(f);

		BufferedReader br = Files.newBufferedReader(f, Charset.forName("MS932"));
		String line = null;

		for(String key : property.keySet()) {
			if(property.get(key) != null && property.get(key) instanceof Integer) {
				op.add((Integer) property.get(key));
			}
		}

		while ((line = br.readLine()) != null) {
			processLine(line, state);
		}
		header.setCustomOptions(options.toArray(new CustomOption[options.size()]));
		header.setCustomFiles(files.toArray(new CustomFile[files.size()]));

		return header;
	}	
}
