package bms.player.beatoraja;

import bms.player.beatoraja.skin.SkinType;

/**
 * スキンコンフィグ
 * 
 * @author exch
 */
public class SkinConfig {
	/**
	 * ファイルパス
	 */
	private String path;
	/**
	 * 設定項目
	 */
	private Property properties;

	public SkinConfig() {

	}

	public SkinConfig(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Property getProperties() {
		return properties;
	}

	public void setProperties(Property property) {
		this.properties = property;
	}
	
	public void validate() {
		if(properties == null) {
			properties = new Property();
		}
		properties.validate();
	}
	
	public static SkinConfig getDefault(int id) {
		SkinConfig skin = new SkinConfig();
		Default dskin = Default.get(SkinType.getSkinTypeById(id));
		if(dskin != null) {
			skin.setPath(dskin.path);
			skin.validate();
		}
		return skin;
	}

	public static class Property {
		private Option[] option = new Option[0];
		private FilePath[] file = new FilePath[0];
		private Offset[] offset = new Offset[0];

		public Option[] getOption() {
			return option;
		}

		public void setOption(Option[] option) {
			this.option = option;
		}

		public FilePath[] getFile() {
			return file;
		}

		public void setFile(FilePath[] file) {
			this.file = file;
		}

		public Offset[] getOffset() {
			return offset;
		}

		public void setOffset(Offset[] offset) {
			this.offset = offset;
		}
		
		public void validate() {
			if(option == null) {
				option = new Option[0];
			}
			if(file == null) {
				file = new FilePath[0];
			}
			if(offset == null) {
				offset = new Offset[0];
			}			
		}
	}

	public static class Option {
		public String name;
		public int value;
	}

	public static class FilePath {
		public String name;
		public String path;
	}

	public static class Offset {
		public String name;
		public int x;
		public int y;
		public int w;
		public int h;
		public int r;
		public int a;
	}

	public enum Default {
		PLAY7(SkinType.PLAY_7KEYS, "skin/default/play/play7.luaskin"),
		PLAY5(SkinType.PLAY_5KEYS, "skin/default/play5.json"),
		PLAY14(SkinType.PLAY_14KEYS, "skin/default/play14.json"),
		PLAY10(SkinType.PLAY_10KEYS, "skin/default/play10.json"),
		PLAY9(SkinType.PLAY_9KEYS, "skin/default/play9.json"),
		SELECT(SkinType.MUSIC_SELECT, "skin/default/select.json"),
		DECIDE(SkinType.DECIDE, "skin/default/decide/decide.luaskin"),
		RESULT(SkinType.RESULT, "skin/default/result/result.luaskin"),
		COURSERESULT(SkinType.COURSE_RESULT, "skin/default/graderesult.json"),
		PLAY24(SkinType.PLAY_24KEYS, "skin/default/play24.json"),
		PLAY24DOUBLE(SkinType.PLAY_24KEYS_DOUBLE, "skin/default/play24double.json"),
		KEYCONFIG(SkinType.KEY_CONFIG, "skin/default/keyconfig/keyconfig.luaskin"),
		SKINSELECT(SkinType.SKIN_SELECT, "skin/default/skinselect/skinselect.luaskin"),
		;

		public final SkinType type;
		public final String path;

		private Default(SkinType type, String path) {
			this.type = type;
			this.path = path;
		}

		public static Default get(SkinType type) {
			for(Default skin : values()) {
				if(skin.type == type) {
					return skin;
				}
			}
			return null;
		}
	}
}