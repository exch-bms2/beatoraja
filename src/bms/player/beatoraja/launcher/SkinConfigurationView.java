package bms.player.beatoraja.launcher;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinHeader.*;
import bms.player.beatoraja.skin.json.JSONSkinLoader;
import bms.player.beatoraja.skin.lr2.LR2SkinHeaderLoader;
import bms.player.beatoraja.skin.lua.LuaSkinLoader;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * スキンコンフィグ
 *
 * @author exch
 */
public class SkinConfigurationView implements Initializable {

	@FXML
	private ComboBox<SkinType> skintypeSelector;
	@FXML
	private ComboBox<SkinHeader> skinheaderSelector;
	@FXML
	private ScrollPane skinconfig;

	private PlayerConfig player;
	private SkinType mode = null;

	/**
	 * 検出されたスキン定義ファイル
	 */
	private List<SkinHeader> skinheader = new ArrayList<SkinHeader>();

	private SkinHeader selected = null;
	private Map<CustomOption, ComboBox<String>> optionbox = new HashMap<CustomOption, ComboBox<String>>();
	private Map<CustomFile, ComboBox<String>> filebox = new HashMap<CustomFile, ComboBox<String>>();
	private Map<CustomOffset, Spinner<Integer>[]> offsetbox = new HashMap<CustomOffset, Spinner<Integer>[]>();

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		skintypeSelector.setCellFactory((param) -> new SkinTypeCell());
		skintypeSelector.setButtonCell(new SkinTypeCell());
		skintypeSelector.getItems().addAll(SkinType.values());

		skinheaderSelector.setCellFactory((param) -> new SkinListCell());
		skinheaderSelector.setButtonCell(new SkinListCell());
	}

	public SkinHeader getSelectedHeader() {
		return selected;
	}

	public SkinConfig.Property getProperty() {
		SkinConfig.Property property = new SkinConfig.Property();

		List<SkinConfig.Option> options = new ArrayList<>();
		for (CustomOption option : selected.getCustomOptions()) {
			if (optionbox.get(option) != null) {
				int index = optionbox.get(option).getSelectionModel().getSelectedIndex();
				SkinConfig.Option o = new SkinConfig.Option();
				o.name = option.name;
				if(index != optionbox.get(option).getItems().size() - 1) {
					o.value = option.option[index];
				} else {
					o.value = OPTION_RANDOM_VALUE;
				}
				options.add(o);
			}
		}
		property.setOption(options.toArray(new SkinConfig.Option[options.size()]));

		List<SkinConfig.FilePath> files = new ArrayList<>();
		for (CustomFile file : selected.getCustomFiles()) {
			if (filebox.get(file) != null) {
				SkinConfig.FilePath o = new SkinConfig.FilePath();
				o.name = file.name;
				o.path = filebox.get(file).getValue();
				files.add(o);
			}
		}
		property.setFile(files.toArray(new SkinConfig.FilePath[files.size()]));

		List<SkinConfig.Offset> offsets = new ArrayList<>();
		for (CustomOffset offset : selected.getCustomOffsets()) {
			if (offsetbox.get(offset) != null) {
				SkinConfig.Offset o = new SkinConfig.Offset();
				Spinner<Integer>[] spinner = offsetbox.get(offset);
				int[] values = new int[spinner.length];
				for(int i = 0;i < values.length;i++) {
					spinner[i].getValueFactory()
					.setValue(spinner[i].getValueFactory().getConverter().fromString(spinner[i].getEditor().getText()));
					values[i] = spinner[i].getValue();
				}
				o.name = offset.name;
				o.x = spinner[0].getValue();
				o.y = spinner[1].getValue();
				o.w = spinner[2].getValue();
				o.h = spinner[3].getValue();
				o.r = spinner[4].getValue();
				o.a = spinner[5].getValue();
				offsets.add(o);
			}
		}
		property.setOffset(offsets.toArray(new SkinConfig.Offset[offsets.size()]));

		return property;
	}

	public SkinHeader[] getSkinHeader(SkinType mode) {
		List<SkinHeader> result = new ArrayList<SkinHeader>();
		for (SkinHeader header : skinheader) {
			if (header.getSkinType() == mode) {
				result.add(header);
			}
		}
		return result.toArray(new SkinHeader[result.size()]);
	}
	
    @FXML
	public void changeSkinType() {
    	commitSkinType();
    	updateSkinType(skintypeSelector.getValue());    	
	}
    
	public void updateSkinType(SkinType type) {
		mode = type;

		skinheaderSelector.getItems().clear();
		SkinHeader[] headers = getSkinHeader(type);
		skinheaderSelector.getItems().addAll(headers);
		if (player.getSkin()[type.getId()] != null) {
			SkinConfig skinconf = player.getSkin()[type.getId()];
			if (skinconf != null) {
				for (SkinHeader header : skinheaderSelector.getItems()) {
					if (header != null && header.getPath().equals(Paths.get(skinconf.getPath()))) {
						skinheaderSelector.setValue(header);
						skinconfig.setContent(create(skinheaderSelector.getValue(), skinconf.getProperties()));
						break;
					}
				}
			} else {
				skinheaderSelector.getSelectionModel().select(0);
			}
		}
	}
    
    @FXML
	public void commitSkinType() {
		if(player == null) {
			return;
		}
		
		if (selected != null) {
			SkinConfig skin = new SkinConfig(selected.getPath().toString());
			skin.setProperties(getProperty());
			player.getSkin()[selected.getSkinType().getId()] = skin;
		} else if (mode != null) {
			player.getSkin()[mode.getId()] = null;
		}
	}
    
    public void update(Config config) {
		List<Path> skinpaths = new ArrayList<Path>();
		scan(Paths.get(config.getSkinpath()), skinpaths);
		for (Path path : skinpaths) {
			String pathString = path.toString().toLowerCase();
			if (pathString.endsWith(".json")) {
				JSONSkinLoader loader = new JSONSkinLoader();
				SkinHeader header = loader.loadHeader(path);
				if (header != null) {
					skinheader.add(header);
				}
			} else if (pathString.endsWith(".luaskin")) {
				LuaSkinLoader loader = new LuaSkinLoader();
				SkinHeader header = loader.loadHeader(path);
				if (header != null) {
					skinheader.add(header);
				}
			} else {
				LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader(config);
				try {
					SkinHeader header = loader.loadSkin(path, null);
					// System.out.println(path.toString() + " : " +
					// header.getName()
					// + " - " + header.getMode());
					skinheader.add(header);
					// 7/14key skinは5/10keyにも加える
					if(header.getType() == SkinHeader.TYPE_LR2SKIN &&
							(header.getSkinType() == SkinType.PLAY_7KEYS || header.getSkinType() == SkinType.PLAY_14KEYS)) {
						header = loader.loadSkin(path, null);

						if(header.getSkinType() == SkinType.PLAY_7KEYS && !header.getName().toLowerCase().contains("7key")) {
							header.setName(header.getName() + " (7KEYS) ");
						} else if(header.getSkinType() == SkinType.PLAY_14KEYS && !header.getName().toLowerCase().contains("14key")) {
							header.setName(header.getName() + " (14KEYS) ");
						}
						header.setSkinType(header.getSkinType() == SkinType.PLAY_7KEYS ? SkinType.PLAY_5KEYS : SkinType.PLAY_10KEYS);
						skinheader.add(header);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
    }
    
	private void scan(Path p, final List<Path> paths) {
		if (Files.isDirectory(p)) {
			try (Stream<Path> sub = Files.list(p)) {
				sub.forEach((t) -> {
						scan(t, paths);
				});
			} catch (IOException e) {
			}
		} else if (p.getFileName().toString().toLowerCase().endsWith(".lr2skin")
				|| p.getFileName().toString().toLowerCase().endsWith(".luaskin")
				|| p.getFileName().toString().toLowerCase().endsWith(".json")) {
			paths.add(p);
		}
	}

    public void update(PlayerConfig player) {
    	this.player = player;
    	skintypeSelector.setValue(SkinType.PLAY_7KEYS);
		updateSkinType(SkinType.PLAY_7KEYS);
    }

    public void commit() {
    	commitSkinType();
    }
    
    @FXML
	public void changeSkinHeader() {
    	commitSkinHeader();
		updateSkinHeader(skinheaderSelector.getValue());
	}
    
    public void updateSkinHeader(SkinHeader header) {
		// historyからconfig抽出
		SkinConfig.Property property = null;
		if(header != null) {
			for(SkinConfig skinc : player.getSkinHistory()) {
				if(skinc.getPath().equals(header.getPath().toString())) {
					property = skinc.getProperties();
					break;
				}
			}
		}
		skinconfig.setContent(create(header, property));
    	
    }
    
    public void commitSkinHeader() {
		// history保存
		if(selected != null) {
			SkinConfig.Property property = getProperty();
			int index = -1;
			for(int i = 0;i < player.getSkinHistory().length;i++) {
				if(player.getSkinHistory()[i].getPath().equals(selected.getPath().toString())) {
					index = i;
					break;
				}
			}

			SkinConfig sc = new SkinConfig();
			sc.setPath(selected.getPath().toString());
			sc.setProperties(property);
			if(index >= 0) {
				player.getSkinHistory()[index] = sc;
			} else {
				SkinConfig[] history = Arrays.copyOf(player.getSkinHistory(), player.getSkinHistory().length + 1);
				history[history.length - 1] = sc;
				player.setSkinHistory(history);
			}
		}
    }

	private VBox create(SkinHeader header, SkinConfig.Property property) {
		selected = header;
		if (header == null) {
			return null;
		}
		if(property == null) {
			property = new SkinConfig.Property();
		}
		VBox main = new VBox();
		
		// Option項目生成
		optionbox.clear();
		for (CustomOption option : header.getCustomOptions()) {
			HBox hbox = new HBox();
			ComboBox<String> combo = new ComboBox<String>();
			combo.getItems().setAll(option.contents);
			combo.getItems().add("Random");
			combo.getSelectionModel().select(0);
			int selection = -1;
			for(SkinConfig.Option o : property.getOption()) {
				if (o.name.equals(option.name)) {
					int i = o.value;
					if(i != OPTION_RANDOM_VALUE) {
						for(int index = 0;index < option.option.length;index++) {
							if(option.option[index] == i) {
								selection = index;
								break;
							}
						}
					} else {
						selection = combo.getItems().size() - 1;
					}
					break;
				}
			}
			if (selection < 0 && option.def != null) {
				for (int index = 0; index < option.option.length; index++) {
					if (option.contents[index].equals(option.def)) {
						selection = index;
					}
				}
			}
			if (selection >= 0) {
				combo.getSelectionModel().select(selection);
			}

			Label label = new Label(option.name);
			label.setMinWidth(250.0);
			hbox.getChildren().addAll(label, combo);
			optionbox.put(option, combo);
			main.getChildren().add(hbox);
		}
		
		// File項目生成
		filebox.clear();
		for (CustomFile file : header.getCustomFiles()) {
			String name = file.path.substring(file.path.lastIndexOf('/') + 1);
			if(file.path.contains("|")) {
				if(file.path.length() > file.path.lastIndexOf('|') + 1) {
					name = file.path.substring(file.path.lastIndexOf('/') + 1, file.path.indexOf('|')) + file.path.substring(file.path.lastIndexOf('|') + 1);
				} else {
					name = file.path.substring(file.path.lastIndexOf('/') + 1, file.path.indexOf('|'));
				}
			}
			
			final int slashindex = file.path.lastIndexOf('/');
			final Path dirpath = slashindex != -1 ? Paths.get(file.path.substring(0, slashindex)) : Paths.get(file.path);
			if (!Files.exists(dirpath)) {
				continue;
			}
			try (DirectoryStream<Path> paths = Files.newDirectoryStream(dirpath,
					"{" + name.toLowerCase() + "," + name.toUpperCase() + "}")) {
				HBox hbox = new HBox();
				ComboBox<String> combo = new ComboBox<String>();
				for (Path p : paths) {
					combo.getItems().add(p.getFileName().toString());
				}
				combo.getItems().add("Random");

				String selection = null;
				for(SkinConfig.FilePath f : property.getFile()) {
					if(f.name.equals(file.name)) {
						selection = f.path;
						break;
					}
				}
				if (selection == null && file.def != null) {
					// デフォルト値のファイル名またはそれに拡張子を付けたものが存在すれば使用する
					for (String item : combo.getItems()) {
						if (item.equalsIgnoreCase(file.def)) {
							selection = item;
							break;
						}
						int point = item.lastIndexOf('.');
						if (point != -1 && item.substring(0, point).equalsIgnoreCase(file.def)) {
							selection = item;
							break;
						}
					}
				}
				if (selection != null) {
					combo.setValue(selection);
				} else {
					combo.getSelectionModel().select(0);
				}

				Label label = new Label(file.name);
				label.setMinWidth(250.0);
				hbox.getChildren().addAll(label, combo);
				filebox.put(file, combo);
				main.getChildren().add(hbox);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Offset項目生成
		offsetbox.clear();
		for (CustomOffset option : header.getCustomOffsets()) {
			final String[] values = {"x","y","w","h","r","a"};
			HBox hbox = new HBox();
			Label label = new Label(option.name);
			label.setMinWidth(250.0);
			hbox.getChildren().add(label);

			final boolean[] b = {option.x, option.y, option.w, option.h, option.r, option.a};
			SkinConfig.Offset offset = null;
			for(SkinConfig.Offset o : property.getOffset()) {
				if(o.name.equals(option.name)) {
					offset = o;
					break;
				}
			}
			final int[] v = offset != null ? new int[]{offset.x, offset.y, offset.w, offset.h, offset.r, offset.a} : new int[values.length];

			Spinner<Integer>[] spinner = new Spinner[values.length];
			for(int i = 0;i < spinner.length;i++) {
				spinner[i] = new NumericSpinner();
				spinner[i].setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-9999,9999,v[i],1));
				spinner[i].setPrefWidth(80);
				spinner[i].setEditable(true);
				if(b[i]) {
					hbox.getChildren().addAll(new Label(values[i]), spinner[i]);					
				}
			}
			offsetbox.put(option, spinner);
			main.getChildren().add(hbox);
		}

		return main;
	}
	
	static class SkinTypeCell extends ListCell<SkinType> {

		@Override
		protected void updateItem(SkinType arg0, boolean arg1) {
			super.updateItem(arg0, arg1);
			setText(arg0 != null ? arg0.getName() : "");
		}
	}

	static class SkinListCell extends ListCell<SkinHeader> {

		@Override
		protected void updateItem(SkinHeader arg0, boolean arg1) {
			super.updateItem(arg0, arg1);
			setText(arg0 != null ? arg0.getName() + (arg0.getType() == SkinHeader.TYPE_BEATORJASKIN ? "" : " (LR2 Skin)") : "");
		}
	}
}