package bms.player.beatoraja.launcher;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinHeader.CustomFile;
import bms.player.beatoraja.skin.SkinHeader.CustomOffset;
import bms.player.beatoraja.skin.SkinHeader.CustomOption;
import bms.player.beatoraja.skin.lr2.LR2SkinHeaderLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * スキンコンフィグ
 *
 * @author exch
 */
public class SkinConfigurationView {

	private List<SkinHeader> lr2skinheader = new ArrayList<SkinHeader>();

	private SkinHeader selected = null;
	private Map<CustomOption, ComboBox<String>> optionbox = new HashMap<CustomOption, ComboBox<String>>();
	private Map<CustomFile, ComboBox<String>> filebox = new HashMap<CustomFile, ComboBox<String>>();
	private Map<CustomOffset, Spinner<Integer>[]> offsetbox = new HashMap<CustomOffset, Spinner<Integer>[]>();

	public SkinConfigurationView() {
		List<Path> lr2skinpaths = new ArrayList<Path>();
		scan(Paths.get("skin"), lr2skinpaths);
		for (Path path : lr2skinpaths) {
			if (path.toString().toLowerCase().endsWith(".json")) {
				JSONSkinLoader loader = new JSONSkinLoader();
				SkinHeader header = loader.loadHeader(path);
				if (header != null) {
					lr2skinheader.add(header);
				}
			} else {
				LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
				try {
					SkinHeader header = loader.loadSkin(path, null);
					// System.out.println(path.toString() + " : " +
					// header.getName()
					// + " - " + header.getMode());
					lr2skinheader.add(header);
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
						lr2skinheader.add(header);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public VBox create(SkinHeader header, SkinConfig.Property property) {
		selected = header;
		if (header == null) {
			return null;
		}
		if(property == null) {
			property = new SkinConfig.Property();
		}
		VBox main = new VBox();
		optionbox.clear();
		for (CustomOption option : header.getCustomOptions()) {
			HBox hbox = new HBox();
			ComboBox<String> combo = new ComboBox<String>();
			combo.getItems().setAll(option.contents);
			combo.getSelectionModel().select(0);
			for(SkinConfig.Option o : property.getOption()) {
				if (o.name.equals(option.name)) {
					int i = o.value;
					for(int index = 0;index < option.option.length;index++) {
						if(option.option[index] == i) {
							combo.getSelectionModel().select(index);
							break;
						}
					}
					break;
				}
			}

			Label label = new Label(option.name);
			label.setMinWidth(250.0);
			hbox.getChildren().addAll(label, combo);
			optionbox.put(option, combo);
			main.getChildren().add(hbox);
		}
		filebox.clear();
		for (CustomFile file : header.getCustomFiles()) {
			String name = file.path.substring(file.path.lastIndexOf('/') + 1);
			final Path dirpath = Paths.get(file.path.substring(0, file.path.lastIndexOf('/')));
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
		offsetbox.clear();
		for (CustomOffset option : header.getCustomOffsets()) {
			final String[] values = {"x","y","w","h","r","a"};
			HBox hbox = new HBox();
			Label label = new Label(option.name);
			label.setMinWidth(250.0);
			hbox.getChildren().add(label);

			int[] v = new int[values.length];
			boolean[] b = new boolean[values.length];
			for(SkinConfig.Offset o : property.getOffset()) {
				if(o.name.equals(option.name)) {
					v[0] = o.x;
					v[1] = o.y;
					v[2] = o.w;
					v[3] = o.h;
					v[4] = o.r;
					v[5] = o.a;
					b[0] = option.x;
					b[1] = option.y;
					b[2] = option.w;
					b[3] = option.h;
					b[4] = option.r;
					b[5] = option.a;
					break;
				}
			}

			Spinner<Integer>[] spinner = new Spinner[values.length];
			for(int i = 0;i < spinner.length;i++) {
				spinner[i] = new Spinner(-9999,9999,0,1);
				spinner[i].setPrefWidth(80);
				spinner[i].getValueFactory().setValue(v[i]);
				if(b[i]) {
					hbox.getChildren().addAll(new Label(values[i]), spinner[i]);					
				}
			}
			offsetbox.put(option, spinner);
			main.getChildren().add(hbox);
		}

		return main;
	}

	private void scan(Path p, final List<Path> paths) {
		if (Files.isDirectory(p)) {
			try (Stream<Path> sub = Files.list(p)) {
				sub.forEach(new Consumer<Path>() {
					@Override
					public void accept(Path t) {
						scan(t, paths);
					}
				});
			} catch (IOException e) {
			}
		} else if (p.getFileName().toString().toLowerCase().endsWith(".lr2skin")
				|| p.getFileName().toString().toLowerCase().endsWith(".json")) {
			paths.add(p);
		}
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
				o.value = option.option[index];
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
		for (SkinHeader header : lr2skinheader) {
			if (header.getSkinType() == mode) {
				result.add(header);
			}
		}
		return result.toArray(new SkinHeader[result.size()]);
	}
}