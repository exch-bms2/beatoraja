package bms.player.beatoraja.skin;

import bms.player.beatoraja.Resolution;

import java.nio.file.Path;
import java.util.*;

/**
 * スキンのヘッダ情報
 * 
 * @author exch
 */
public class SkinHeader {

	/**
	 * スキンの種類
	 */
	private int type;

	public static final int TYPE_LR2SKIN = 0;
	public static final int TYPE_BEATORJASKIN = 1;
	/**
	 * スキンファイルのパス
	 */
	private Path path;
	/**
	 * スキンタイプ
	 */
	private SkinType mode;
	/**
	 * スキン名
	 */
	private String name;
	
	private CustomOption[] options = new CustomOption[0];
	private CustomFile[] files = new CustomFile[0];
	private CustomOffset[] offsets = new CustomOffset[0];
	/**
	 * スキン解像度
	 */
	private Resolution resolution = Resolution.SD;
	/**
	 * ランダムで選択されたオプション名と値
	 */
	private Map<String, Integer> randomSelectedOptions = new HashMap<>();

	public SkinType getSkinType() {
		return mode;
	}
	
	public void setSkinType(SkinType mode) {
		this.mode = mode;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public CustomOption[] getCustomOptions() {
		return options;
	}
	
	public void setCustomOptions(CustomOption[] options) {
		this.options = options;
	}

	public CustomFile[] getCustomFiles() {
		return files;
	}
	
	public void setCustomFiles(CustomFile[] files) {
		this.files = files;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	public Resolution getResolution() {
		return resolution;
	}

	public void setResolution(Resolution resolution) {
		this.resolution = resolution;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public CustomOffset[] getCustomOffsets() {
		return offsets;
	}

	public void setCustomOffsets(CustomOffset[] offsets) {
		this.offsets = offsets;
	}

	public static class CustomOption {
		public final String name;
		public final int[] option;
		public final String[] contents;
		public final String def;

		public CustomOption(String name, int[] option, String[] contents) {
			this.name = name;
			this.option = option;
			this.contents = contents;
			this.def = null;
		}

		public CustomOption(String name, int[] option, String[] contents, String def) {
			this.name = name;
			this.option = option;
			this.contents = contents;
			this.def = def;
		}

		public int getDefaultOption() {
			for (int i = 0; i < option.length; i++) {
				if (contents[i].equals(def))
					return option[i];
			}
			return option[0];
		}
	}

	public static class CustomFile {
		public final String name;
		public final String path;
		public final String def;
		
		public CustomFile(String name, String path, String def) {
			this.name = name;
			this.path = path;
			this.def = def;
		}
	}
	
	public static class CustomOffset {
		public final String name;
		public final int id;
		public final boolean x;
		public final boolean y;
		public final boolean w;
		public final boolean h;
		public final boolean r;
		public final boolean a;
		
		public CustomOffset(String name, int id, boolean x, boolean y, boolean w, boolean h,boolean r,boolean a) {
			this.name = name;
			this.id = id;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.r = r;
			this.a = a;
		}
	}

	public int getRandomSelectedOptions(String name) {
		if(randomSelectedOptions.containsKey(name)) return randomSelectedOptions.get(name);
		return -1;
	}

	public void setRandomSelectedOptions(String name, int value) {
		randomSelectedOptions.put(name, value);
	}
}
