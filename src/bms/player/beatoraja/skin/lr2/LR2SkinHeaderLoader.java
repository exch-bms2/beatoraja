package bms.player.beatoraja.skin.lr2;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.badlogic.gdx.utils.Array;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinProperty;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.skin.lr2.LR2SkinLoader.Command;
import bms.player.beatoraja.skin.SkinHeader.*;

import static bms.player.beatoraja.Resolution.*;

/**
 * LR2スキンヘッダファイル(lr2skin)のローダー
 * 
 * @author exch
 */
public class LR2SkinHeaderLoader extends LR2SkinLoader {
	
	SkinHeader header = new SkinHeader();
	Array<CustomFile> files = new Array<CustomFile>();
	Array<CustomOption> options = new Array<CustomOption>();
	Array<CustomOffset> offsets = new Array<CustomOffset>();
	
	final String skinpath;

	public LR2SkinHeaderLoader(Config c) {
		addCommandWord(HeaderCommand.values());
		skinpath = c.getSkinpath();
	}
	
	public SkinHeader loadSkin(Path f, MainState state) throws IOException {
		return this.loadSkin(f, state, new SkinConfig.Property());
	}
	
	public SkinHeader loadSkin(Path f, MainState state, SkinConfig.Property property) throws IOException {
		// TODO header読み込みに失敗したらnullを返す
		header = new SkinHeader();
		files.clear();
		options.clear();
		offsets.clear();

		header.setPath(f);

		try (Stream<String> lines = Files.lines(f, Charset.forName("MS932"))) {
			lines.forEach(line -> {
				try {
					processLine(line, state);				
				} catch(Throwable e) {
					e.printStackTrace();
				}
			});
		};
		header.setCustomOptions(options.toArray(CustomOption.class));
		header.setCustomFiles(files.toArray(CustomFile.class));
		header.setCustomOffsets(offsets.toArray(CustomOffset.class));
		
		header.setSkinConfigProperty(property);
		
		for (CustomOption option : header.getCustomOptions()) {
			for(int i = 0;i < option.option.length;i++) {
				op.put(option.option[i], option.getSelectedOption() == option.option[i] ? 1 : 0);
			}
		}

		return header;
	}	
}

enum HeaderCommand implements Command<LR2SkinHeaderLoader> {

	INFORMATION ((loader, str) -> {
		loader.header.setSkinType(SkinType.getSkinTypeById(Integer.parseInt(str[1])));
		loader.header.setName(str[2]);
		loader.header.setAuthor(str[3]);
		switch (loader.header.getSkinType()) {
			case PLAY_5KEYS:
			case PLAY_7KEYS:
			case PLAY_9KEYS:
			case PLAY_10KEYS:
			case PLAY_14KEYS:
			case PLAY_24KEYS:
			case PLAY_24KEYS_DOUBLE:
				loader.options.add(new CustomOption("BGA Size", new int[]{30,31}, new String[]{"Normal", "Extend"}));
				loader.options.add(new CustomOption("Ghost", new int[]{34,35,36,37}, new String[]{"Off", "Type A", "Type B", "Type C"}));
				loader.options.add(new CustomOption("Score Graph", new int[]{38,39}, new String[]{"Off", "On"}));
				loader.options.add(new CustomOption("Judge Detail", new int[]{1997,1998,1999}, new String[]{"Off", "EARLY/LATE", "+-ms"}));

				loader.offsets.add(new CustomOffset("All offset(%)", SkinProperty.OFFSET_ALL, true, true, true, true, false, false));
				loader.offsets.add(new CustomOffset("Notes offset", SkinProperty.OFFSET_NOTES_1P, false, false, false, true, false, false));
				loader.offsets.add(new CustomOffset("Judge offset", SkinProperty.OFFSET_JUDGE_1P, true, true, true, true, false, true));
				loader.offsets.add(new CustomOffset("Judge Detail offset", SkinProperty.OFFSET_JUDGEDETAIL_1P, true, true, true, true, false, true));
		}
	}),
	RESOLUTION ((loader, str) -> {
		final Resolution res[] = {SD, HD, FULLHD, ULTRAHD};
		loader.header.setResolution(res[Integer.parseInt(str[1])]);
	}),
	CUSTOMOPTION ((loader, str) -> {
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
		loader.options.add(new CustomOption(str[1], op, contents.toArray(new String[contents.size()])));
	}),
	CUSTOMFILE ((loader, str) -> {
		loader.files.add(new CustomFile(str[1], str[2].replace("LR2files\\Theme", loader.skinpath).replace("\\", "/"), str.length >= 4 ? str[3] : null));
	}),
	CUSTOMOFFSET ((loader, str) -> {
		boolean[] op = new boolean[6];
		Arrays.fill(op, true);
		for(int i = 0;i < op.length && i + 3 < str.length;i++) {
			op[i] = Integer.parseInt(str[i + 3]) > 0;
		}
		loader.offsets.add(new CustomOffset(str[1], Integer.parseInt(str[2]), op[0], op[1], op[2], op[3], op[4], op[5]));
	}),
	CUSTOMOPTION_ADDITION_SETTING ((loader, str) -> {
		//#CUSTOMOPTION_ADDITION_SETTING, BGA Size, Ghost, Score Graph, Judge Detail
		//0 = No Add, 1 = Add
		CustomOption[] addition = new CustomOption[4];
		String[] additionName = {"BGA Size", "Ghost", "Score Graph", "Judge Detail"};
		for(CustomOption co : loader.options) {
			for(int i = 0; i < additionName.length; i++) {
				if(co.name.equals(additionName[i])) addition[i] = co;
			}
		}
		for(int i = 0; i < addition.length; i++) {
			if(str[i + 1].replaceAll("[^0-9-]", "").equals("0") && addition[i] != null) {
				loader.options.removeValue(addition[i], true);
			}
		}
	}),
	INCLUDE ((loader, str) -> {
	});
	
	public final BiConsumer<LR2SkinHeaderLoader, String[]> function;
	
	private HeaderCommand(BiConsumer<LR2SkinHeaderLoader, String[]> function) {
		this.function = function;
	}
	
	public void execute(LR2SkinHeaderLoader loader, String[] str) {
		function.accept(loader, str);
	}
}
