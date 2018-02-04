package bms.player.beatoraja.skin.lr2;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinProperty;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.skin.SkinHeader.*;

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
	private List<CustomOffset> offsets = new ArrayList<CustomOffset>();

	public LR2SkinHeaderLoader() {
		
		addCommandWord(new CommandWord("INFORMATION") {
			@Override
			public void execute(String[] str) {
				header.setSkinType(SkinType.getSkinTypeById(Integer.parseInt(str[1])));
				header.setName(str[2]);
				switch (header.getSkinType()) {
					case PLAY_5KEYS:
					case PLAY_7KEYS:
					case PLAY_9KEYS:
					case PLAY_10KEYS:
					case PLAY_14KEYS:
					case PLAY_24KEYS:
					case PLAY_24KEYS_DOUBLE:
						options.add(new CustomOption("BGA Size", new int[]{30,31}, new String[]{"Normal", "Extend"}));
						options.add(new CustomOption("Ghost", new int[]{34,35,36,37}, new String[]{"Off", "Type A", "Type B", "Type C"}));
						options.add(new CustomOption("Score Graph", new int[]{38,39}, new String[]{"Off", "On"}));
						options.add(new CustomOption("Judge Detail", new int[]{1997,1998,1999}, new String[]{"Off", "EARLY/LATE", "+-ms"}));

						offsets.add(new CustomOffset("Notes offset", SkinProperty.OFFSET_NOTES_1P, false, false, false, true, false, false));
						offsets.add(new CustomOffset("Judge offset", SkinProperty.OFFSET_JUDGE_1P, true, true, true, true, false, true));
						offsets.add(new CustomOffset("Judge Detail offset", SkinProperty.OFFSET_JUDGEDETAIL_1P, true, true, true, true, false, true));
				}
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
		addCommandWord(new CommandWord("CUSTOMOFFSET") {
			@Override
			public void execute(String[] str) {
				List<String> contents = new ArrayList<String>();
				for(int i = 3;i < str.length;i++) {
					if(str[i] != null && str[i].length() > 0) {
						contents.add(str[i]);
					}
				}
				boolean[] op = new boolean[6];
				Arrays.fill(op, true);
				for(int i = 0;i < op.length && i + 3 < str.length;i++) {
					op[i] = Integer.parseInt(str[i + 3]) > 0;
				}
				offsets.add(new CustomOffset(str[1], Integer.parseInt(str[2]), op[0], op[1], op[2], op[3], op[4], op[5]));
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
		return this.loadSkin(f, state, new SkinConfig.Property());
	}
	
	public SkinHeader loadSkin(Path f, MainState state, SkinConfig.Property property) throws IOException {
		header = new SkinHeader();
		files.clear();
		options.clear();
		offsets.clear();

		header.setPath(f);

		BufferedReader br = Files.newBufferedReader(f, Charset.forName("MS932"));
		String line = null;

		for(SkinConfig.Option opt : property.getOption()) {
			op.put(opt.value, true);
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
		header.setCustomOffsets(offsets.toArray(new CustomOffset[offsets.size()]));

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
