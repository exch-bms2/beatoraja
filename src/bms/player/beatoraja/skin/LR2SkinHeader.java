package bms.player.beatoraja.skin;

import java.nio.file.Path;

public class LR2SkinHeader {

	private Path path;
	
	private int mode;

	public static final int MODE_7KEYS = 0;
	public static final int MODE_5KEYS = 1;
	public static final int MODE_14KEYS = 2;
	public static final int MODE_10KEYS = 3;
	public static final int MODE_9KEYS = 4;
	public static final int MODE_MUSICSELECT = 5;
	public static final int MODE_DECIDE = 6;
	public static final int MODE_RESULT = 7;
	public static final int MODE_KEYCONFIG = 8;
	public static final int MODE_SKINSELECT = 9;
	public static final int MODE_SOUNDSET = 10;
	public static final int MODE_THEME = 11;
	public static final int MODE_7KEYSBATTLE = 12;
	public static final int MODE_5KEYSBATTLE = 13;
	public static final int MODE_9KEYSBATTLE = 14;

	private String name;
	
	private CustomOption[] options = new CustomOption[0];
	private CustomFile[] files = new CustomFile[0];
	
	private String include;
	
	private int resolution;

	public int getMode() {
		return mode;
	}
	
	public void setMode(int mode) {
		this.mode = mode;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getInclude() {
		return include;
	}
	
	public void setInclude(String include) {
		this.include = include;
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

	public int getResolution() {
		return resolution;
	}

	public void setResolution(int resolution) {
		this.resolution = resolution;
	}

	public static class CustomOption {
		public final String name;
		public final int option;
		public final String[] contents;
		
		public CustomOption(String name, int option, String[] contents) {
			this.name = name;
			this.option = option;
			this.contents = contents;
		}
	}

	public static class CustomFile {
		public final String name;
		public final String path;
		
		public CustomFile(String name, String path) {
			this.name = name;
			this.path = path;
		}
	}
}
