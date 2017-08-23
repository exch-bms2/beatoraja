package bms.player.beatoraja.skin.lr2;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.skin.SkinHeader.CustomFile;
import bms.player.beatoraja.skin.SkinHeader.CustomOption;

import static bms.player.beatoraja.Resolution.*;

/**
 * LR2スキンヘッダファイル(lr2skin)のローダー
 * 
 * @author exch
 */
public class LR2SkinHeaderLoader extends LR2SkinLoader {
	
	private SkinHeader header = new SkinHeader();
	private List<CustomFile> files = new ArrayList<CustomFile>();
	private List<CustomOption> options = new ArrayList<CustomOption>();

	public LR2SkinHeaderLoader() {
		
		addCommandWord(new CommandWord("INFORMATION") {
			@Override
			public void execute(String[] str) {
				header.setSkinType(SkinType.getSkinTypeById(Integer.parseInt(str[1])));
				header.setName(str[2]);
			}
		});
		addCommandWord(new CommandWord("CUSTOMOPTION") {
			@Override
			public void execute(String[] str) {
				List<String> contents = new ArrayList<String>();
				for(int i = 3;i < str.length;i++) {
					if(str[i] != null && str[i].length() > 0) {
						contents.add(str[i]);
					}
				}
				int[] op = new int[contents.size()];
				for(int i = 0;i < op.length;i++) {
					op[i] = Integer.parseInt(str[2]) + i;
				}
				options.add(new CustomOption(str[1], op, contents.toArray(new String[contents.size()])));
			}
		});
		addCommandWord(new CommandWord("CUSTOMFILE") {
			@Override
			public void execute(String[] str) {
				files.add(new CustomFile(str[1], str[2].replace("LR2files\\Theme", "skin").replace("\\", "/"), str.length >= 4 ? str[3] : null));
			}
		});
		addCommandWord(new CommandWord("RESOLUTION") {

			final Resolution res[] = {SD, HD, FULLHD, ULTRAHD};
			@Override
			public void execute(String[] str) {
				header.setResolution(res[Integer.parseInt(str[1])]);
			}
		});
		addCommandWord(new CommandWord("INCLUDE") {
			@Override
			public void execute(String[] str) {
			}
		});

	}
	
	public SkinHeader loadSkin(Path f, MainState state) throws IOException {
		return this.loadSkin(f, state, new HashMap());
	}
	
	public SkinHeader loadSkin(Path f, MainState state, Map<String, Object> property) throws IOException {
		header = new SkinHeader();
		files.clear();
		options.clear();
		
		header.setPath(f);

		BufferedReader br = Files.newBufferedReader(f, Charset.forName("MS932"));
		String line = null;

		for(String key : property.keySet()) {
			if(property.get(key) != null && property.get(key) instanceof Integer) {
				op.put((Integer) property.get(key), true);
			}
		}

		while ((line = br.readLine()) != null) {
			try {
				processLine(line, state);				
			} catch(Throwable e) {
				e.printStackTrace();
			}
		}
		header.setCustomOptions(options.toArray(new CustomOption[options.size()]));
		header.setCustomFiles(files.toArray(new CustomFile[files.size()]));
		
		for(CustomOption co : options) {
			for(int i = 0;i < co.contents.length;i++) {
				if(!op.containsKey(co.option[i])) {
					op.put(co.option[i], false);
				}
			}
		}

		return header;
	}	
}
