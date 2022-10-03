package bms.player.beatoraja;

import bms.player.beatoraja.skin.SkinType;

/**
 * スキンコンフィグ
 * 
 * @author exch
 */
public class SkinConfig implements Validatable {
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
	
	public boolean validate() {
		if(path == null || path.length() == 0) {
			return false;
		}
		if(properties == null) {
			properties = new Property();
		}
		properties.validate();
		return true;
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

	/**
	 * スキンの各種設定項目
	 * 
	 * @author exch
	 */
	public static class Property implements Validatable {
		/**
		 * 設定項目名-数値のセット
		 */
		private Option[] option = new Option[0];
		/**
		 * 設定項目名-ファイルパスのセット
		 */
		private FilePath[] file = new FilePath[0];
		/**
		 * 設定項目名-オフセットのセット
		 */
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
		
		public boolean validate() {
			if(option == null) {
				option = new Option[0];
			}
			option = Validatable.removeInvalidElements(option);
			
			if(file == null) {
				file = new FilePath[0];
			}
			file = Validatable.removeInvalidElements(file);
			
			if(offset == null) {
				offset = new Offset[0];
			}
			offset = Validatable.removeInvalidElements(offset);
			
			return true;
		}
	}

	public static class Option implements Validatable {
		public String name;
		public int value;

		@Override
		public boolean validate() {
			return name != null && name.length() > 0;
		}
	}

	public static class FilePath implements Validatable {
		public String name;
		public String path;
		
		@Override
		public boolean validate() {
			return name != null && name.length() > 0 && path != null && path.length() > 0;
		}
	}

	public static class Offset implements Validatable {
		public String name;
		public int x;
		public int y;
		public int w;
		public int h;
		public int r;
		public int a;
		
		@Override
		public boolean validate() {
			return name != null && name.length() > 0;
		}
	}

	/**
	 * デフォルトスキンのパス
	 * 
	 * @author exch
	 */
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