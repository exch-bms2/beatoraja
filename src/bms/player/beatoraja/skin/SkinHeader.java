package bms.player.beatoraja.skin;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.SkinConfig;

import static bms.player.beatoraja.skin.SkinProperty.OPTION_RANDOM_VALUE;

import java.io.File;
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
	/**
	 * スキン:LR2
	 */
	public static final int TYPE_LR2SKIN = 0;
	/**
	 * スキン:beatoraja
	 */
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
	/**
	 * スキン製作者名
	 */
	private String author;
	/**
	 * カスタムオプション
	 */
	private CustomOption[] options = CustomOption.EMPTY_ARRAY;
	/**
	 * カスタムファイル
	 */
	private CustomFile[] files = CustomFile.EMPTY_ARRAY;
	/**
	 * カスタムオフセット
	 */
	private CustomOffset[] offsets = CustomOffset.EMPTY_ARRAY;
	/**
	 * カスタムカテゴリー
	 */
	private CustomCategory[] categories = CustomCategory.EMPTY_ARRAY;
	/**
	 * スキン解像度
	 */
	private Resolution resolution = Resolution.SD;

	private Resolution sourceResolution;
	
	private Resolution destinationResolution;
	
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

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
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

	public CustomCategory[] getCustomCategories() {
		return categories;
	}

	public void setCustomCategories(CustomCategory[] categories) {
		this.categories = categories;
	}
	
	public void setSkinConfigProperty(SkinConfig.Property property) {
		for (SkinHeader.CustomOption customOption : getCustomOptions()) {
			int op = customOption.getDefaultOption();
			for (SkinConfig.Option option : property.getOption()) {
				if (option.name.equals(customOption.name)) {
					if (option.value != OPTION_RANDOM_VALUE) {
						op = option.value;
					} else {
						if (customOption.option.length > 0) {
							op = customOption.option[(int) (Math.random() * customOption.option.length)];
						}
					}
					break;
				}
			}
			for(int i = 0;i < customOption.option.length;i++) {
				if(customOption.option[i] == op) {
					customOption.selectedIndex = i;
				}
			}	
		}
		
		for (SkinHeader.CustomFile customFile : getCustomFiles()) {
			for (SkinConfig.FilePath file : property.getFile()) {
				if (customFile.name.equals(file.name)) {
					if (!file.path.equals("Random")) {
						customFile.filename = file.path;
					} else {
						String ext = customFile.path.substring(customFile.path.lastIndexOf("*") + 1);
						if (customFile.path.contains("|")) {
							if (customFile.path.length() > customFile.path.lastIndexOf('|') + 1) {
								ext = customFile.path.substring(customFile.path.lastIndexOf("*") + 1, customFile.path.indexOf('|')) + customFile.path.substring(customFile.path.lastIndexOf('|') + 1);
							} else {
								ext = customFile.path.substring(customFile.path.lastIndexOf("*") + 1, customFile.path.indexOf('|'));
							}
						}
						final int slashindex = customFile.path.lastIndexOf('/');
						File dir = slashindex != -1 ? new File(customFile.path.substring(0, slashindex)) : new File(customFile.path);
						if (dir.exists() && dir.isDirectory()) {
							List<File> l = new ArrayList<File>();
							for (File subfile : dir.listFiles()) {
								if (subfile.getPath().toLowerCase().endsWith(ext)) {
									l.add(subfile);
								}
							}
							if (l.size() > 0) {
								String filename = l.get((int) (Math.random() * l.size())).getName();
								customFile.filename = filename;
							}
						}
					}
				}
			}
		}
		
		for (SkinHeader.CustomOffset of : getCustomOffsets()) {
			SkinConfig.Offset off = null;
			for(SkinConfig.Offset off2 : property.getOffset()) {
				if (off2.name.equals(of.name)) {
					off = off2;
					break;
				}
			}
			if(off == null) {
				off = new SkinConfig.Offset();
				off.name = of.name;
			}
			of.offset = off;
		}
	}

	public Resolution getSourceResolution() {
		return sourceResolution;
	}

	public void setSourceResolution(Resolution sourceResolution) {
		this.sourceResolution = sourceResolution;
	}

	public Resolution getDestinationResolution() {
		return destinationResolution;
	}

	public void setDestinationResolution(Resolution destinationResolution) {
		this.destinationResolution = destinationResolution;
	}

	/**
	 * ユーザーが選択可能な項目
	 * 
	 * @author exch
	 */
	public static abstract class CustomItem {

		/**
		 * カスタムファイル名称
		 */
		public final String name;
		
		public CustomItem(String name) {
			this.name = name;
		}
	}
	
	/**
	 * 選択可能なオプション
	 * 
	 * @author exch
	 */
	public static class CustomOption extends CustomItem {

		public static final CustomOption[] EMPTY_ARRAY = new CustomOption[0];

		/**
		 * 各オプションID
		 */
		public final int[] option;
		/**
		 * 各オプション名
		 */
		public final String[] contents;
		/**
		 * デフォルトオプション名
		 */
		public final String def;
		
		private int selectedIndex = -1;

		public CustomOption(String name, int[] option, String[] contents) {
			super(name);
			this.option = option;
			this.contents = contents;
			this.def = null;
		}

		public CustomOption(String name, int[] option, String[] contents, String def) {
			super(name);
			this.option = option;
			this.contents = contents;
			this.def = def;
		}

		public int getDefaultOption() {
			for (int i = 0; i < option.length; i++) {
				if (contents[i].equals(def))
					return option[i];
			}
			return option.length > 0 ? option[0] : SkinProperty.OPTION_RANDOM_VALUE;
		}
		
		public int getSelectedOption() {
			return (selectedIndex >= 0 && selectedIndex < option.length) ? option[selectedIndex] : SkinProperty.OPTION_RANDOM_VALUE;
		}
	}

	/**
	 * 選択可能なファイル
	 * 
	 * @author exch
	 */
	public static class CustomFile extends CustomItem {

		public static final CustomFile[] EMPTY_ARRAY = new CustomFile[0];

		/**
		 * ファイルパス
		 */
		public final String path;
		/**
		 * デフォルトファイル名
		 */
		public final String def;
		
		private String filename;
		
		public CustomFile(String name, String path, String def) {
			super(name);
			this.path = path;
			this.def = def;
		}
		
		public String getSelectedFilename() {
			return filename;
		}
	}
	
	/**
	 * 選択可能なオフセット
	 * 
	 * @author exch
	 */
	public static class CustomOffset extends CustomItem {

		public static final CustomOffset[] EMPTY_ARRAY = new CustomOffset[0];

		/**
		 * オフセットID
		 */
		public final int id;
		
		/**
		 * それぞれの値の変更を許可するかどうか
		 */
		public final boolean x;
		public final boolean y;
		public final boolean w;
		public final boolean h;
		public final boolean r;
		public final boolean a;
		
		private SkinConfig.Offset offset;
		
		public CustomOffset(String name, int id, boolean x, boolean y, boolean w, boolean h,boolean r,boolean a) {
			super(name);
			this.id = id;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.r = r;
			this.a = a;
		}
		
		public SkinConfig.Offset getOffset() {
			return offset;
		}
	}
	
	/**
	 * カテゴリー
	 * 
	 * @author exch
	 */
	public static class CustomCategory {
	
		public static final CustomCategory[] EMPTY_ARRAY = new CustomCategory[0];
		
		/**
		 * カテゴリー名
		 */
		public final String name;
		/**
		 * カテゴリーのカスタムアイテム
		 */
		public final CustomItem[] items;
		
		public CustomCategory(String name, CustomItem[] items) {
			this.name = name;
			this.items = items;
		}

	}
}
