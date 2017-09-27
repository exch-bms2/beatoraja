package bms.player.beatoraja.skin;

import bms.player.beatoraja.Resolution;

import java.nio.file.Path;

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
		
		public CustomOption(String name, int[] option, String[] contents) {
			this.name = name;
			this.option = option;
			this.contents = contents;
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
		
		public CustomOffset(String name, int id) {
			this.name = name;
			this.id = id;
		}
	}
}
