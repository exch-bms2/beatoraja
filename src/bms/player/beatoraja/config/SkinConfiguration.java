package bms.player.beatoraja.config;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;
import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.lr2.LR2SkinHeaderLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SkinConfiguration extends MainState {

	private SkinConfigurationSkin skin;
	private SkinType type;
	private SkinConfig config;
	private List<SkinHeader> allSkins;
	private List<SkinHeader> availableSkins;
	private int selectedSkinIndex;
	private SkinHeader selectedSkinHeader;
	private Skin selectedSkin;

	public SkinConfiguration(MainController main) {
		super(main);
	}

	public void create() {
		loadSkin(SkinType.SKIN_SELECT);
		skin = (SkinConfigurationSkin) getSkin();
		loadAllSkins();
		changeSkinType(SkinType.getSkinTypeById(skin.getDefaultSkinType()));
	}

	public void render() {

		if (main.getInputProcessor().isExitPressed()) {
			main.getInputProcessor().setExitPressed(false);
			main.saveConfig();
			main.changeState(MainController.STATE_SELECTMUSIC);
		}
	}

	public int getImageIndex(int id) {
		if (SkinPropertyMapper.isSkinSelectTypeId(id)) {
			SkinType t = SkinPropertyMapper.getSkinSelectType(id);
			return type == t ? 1 : 0;
		}
		return super.getImageIndex(id);
	}

	public String getTextValue(int id) {
		switch (id) {
		case STRING_SKIN_NAME:
			return selectedSkinHeader != null ? selectedSkinHeader.getName() : "";
		case STRING_SKIN_AUTHOR:
			return selectedSkinHeader != null ? "" : "";
		default:
			if (SkinPropertyMapper.isSkinCustomizeCategory(id)) {
				int index = SkinPropertyMapper.getSkinCustomizeCategoryIndex(id);
				return "";
			}
			if (SkinPropertyMapper.isSkinCustomizeItem(id)) {
				int index = SkinPropertyMapper.getSkinCustomizeItemIndex(id);
				return "";
			}
		}
		return super.getTextValue(id);
	}

	public void executeClickEvent(int id) {
		switch (id) {
		case BUTTON_CHANGE_SKIN:
			setNextSkin();
			break;
		default:
			if (SkinPropertyMapper.isSkinSelectTypeId(id)) {
				SkinType t = SkinPropertyMapper.getSkinSelectType(id);
				changeSkinType(t);
			}
		}
	}

	private void changeSkinType(SkinType type) {
		this.type = type != null ? type : SkinType.PLAY_7KEYS;
		this.config = main.getPlayerConfig().getSkin()[this.type.getId()];
		availableSkins = new ArrayList<>();
		for (SkinHeader header : allSkins) {
			if (header.getSkinType() == type) {
				availableSkins.add(header);
			}
		}
		if (this.config != null) {
			int index = -1;
			for (int i = 0; i < availableSkins.size(); i++) {
				SkinHeader header = availableSkins.get(i);
				if (header != null && header.getPath().equals(Paths.get(config.getPath()))) {
					index = i;
				}
			}
			selectSkin(index);
		} else {
			selectSkin(-1);
		}
	}

	private void setNextSkin() {
		if (availableSkins.isEmpty()) {
			Logger.getGlobal().warning("利用可能なスキンがありません");
			return;
		}

		int index = (selectedSkinIndex + 1) % availableSkins.size();
		//config.setPath();
		selectSkin(index);
	}

	private void selectSkin(int index) {
		selectedSkinIndex = index;
		if (index >= 0) {
			selectedSkinHeader = availableSkins.get(selectedSkinIndex);
		} else {
			selectedSkinHeader = null;
			if (availableSkins.size() > 0) {
			} else {
			}
		}
	}

	private void loadAllSkins() {
		allSkins = new ArrayList<SkinHeader>();
		List<Path> skinPaths = new ArrayList<>();
		scanSkins(Paths.get("skin"), skinPaths);
		for (Path path : skinPaths) {
			if (path.toString().toLowerCase().endsWith(".json")) {
				JSONSkinLoader loader = new JSONSkinLoader();
				SkinHeader header = loader.loadHeader(path);
				if (header != null) {
					allSkins.add(header);
				}
			} else {
				LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
				try {
					SkinHeader header = loader.loadSkin(path, null);
					allSkins.add(header);
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
						allSkins.add(header);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void scanSkins(Path path, List<Path> paths) {
		if (Files.isDirectory(path)) {
			try (Stream<Path> sub = Files.list(path)) {
				sub.forEach((Path t) -> {
					scanSkins(t, paths);
				});
			} catch (IOException e) {
			}
		} else if (path.getFileName().toString().toLowerCase().endsWith(".lr2skin")
				|| path.getFileName().toString().toLowerCase().endsWith(".json")) {
			paths.add(path);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
	}
}
