package bms.player.beatoraja.select;

import static bms.player.beatoraja.SystemSoundManager.SoundType.*;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import bms.player.beatoraja.select.bar.*;
import bms.player.beatoraja.song.SongData;

import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.graphics.Color;

public enum MusicSelectCommand {

	RESET_REPLAY(selector -> {
		if (selector.getBarManager().getSelected() instanceof SelectableBar) {
			final SelectableBar bar = (SelectableBar) selector.getBarManager().getSelected();
			for (int i = 0; i < MusicSelector.REPLAY; i++) {
				if (bar.existsReplay(i)) {
					selector.setSelectedReplay(i);
					return;
				}
			}
		}
		selector.setSelectedReplay(-1);
	}),
	NEXT_REPLAY(selector -> {
		if (selector.getBarManager().getSelected() instanceof SelectableBar) {
			final SelectableBar bar = (SelectableBar) selector.getBarManager().getSelected();
			for (int i = 1; i < MusicSelector.REPLAY; i++) {
				final int selectedreplay = selector.getSelectedReplay();
				if (bar.existsReplay((i + selectedreplay) % MusicSelector.REPLAY)) {
					selector.setSelectedReplay((i + selectedreplay) % MusicSelector.REPLAY);
					selector.play(OPTION_CHANGE);
					break;
				}
			}
		}
	}),
	PREV_REPLAY(selector -> {
		if (selector.getBarManager().getSelected() instanceof SelectableBar) {
			final SelectableBar bar = (SelectableBar) selector.getBarManager().getSelected();
			for (int i = 1; i < MusicSelector.REPLAY; i++) {
				final int selectedreplay = selector.getSelectedReplay();
				if (bar.existsReplay((selectedreplay + MusicSelector.REPLAY - i) % MusicSelector.REPLAY)) {
					selector.setSelectedReplay((selectedreplay + MusicSelector.REPLAY - i) % MusicSelector.REPLAY);
					selector.play(OPTION_CHANGE);
					break;
				}
			}
		}
	}),
	/**
	 * 楽曲ファイルの場所をOS既定のファイルブラウザーで開く
	 */
	OPEN_WITH_EXPLORER(selector -> {
		Bar current = selector.getBarManager().getSelected();
		try {
			if (Desktop.isDesktopSupported()) {
				if (current instanceof SongBar) {
					final SongBar songbar = (SongBar) current;
					if (songbar.existsSong()) {
						Desktop.getDesktop().open(Paths.get(songbar.getSongData().getPath()).getParent().toFile());
					} else if (songbar.getSongData() != null && songbar.getSongData().getOrg_md5() != null) {
						String[] md5 = songbar.getSongData().getOrg_md5()
								.toArray(new String[songbar.getSongData().getOrg_md5().size()]);
						SongData[] songdata = selector.getSongDatabase().getSongDatas(md5);
						for (SongData sd : songdata) {
							if (sd.getPath() != null) {
								Desktop.getDesktop().open(Paths.get(sd.getPath()).getParent().toFile());
								break;
							}
						}
					} else {
						Matcher m = Pattern.compile(".[^\\(\\[～~]*").matcher(current.getTitle());
						if (m.find()) {
							SongData[] songdata = selector.getSongDatabase().getSongDatasByText(m.group());
							for (SongData sd : songdata) {
								if (sd.getPath() != null) {
									Desktop.getDesktop().open(Paths.get(sd.getPath()).getParent().toFile());
									break;
								}
							}
						}
					}
				} else if (current instanceof FolderBar) {
					Desktop.getDesktop().open(Paths.get(((FolderBar) current).getFolderData().getPath()).toFile());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}),
	/**
	 * 譜面のMD5ハッシュをクリップボードにコピーする
	 */
	COPY_MD5_HASH(selector -> {
		Bar current = selector.getBarManager().getSelected();
		if (current instanceof SongBar) {
			final SongData song = ((SongBar) current).getSongData();
			if (song != null) {
				String hash = song.getMd5();
				if (hash != null && hash.length() > 0) {
					StringSelection stringSelection = new StringSelection(hash);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(stringSelection, null);
					selector.main.getMessageRenderer().addMessage("MD5 hash copied : " + hash, 2000, Color.GOLD, 0);
				}
			}
		}
	}),
	/**
	 * 譜面のMD5ハッシュをクリップボードにコピーする
	 */
	COPY_SHA256_HASH(selector -> {
		Bar current = selector.getBarManager().getSelected();
		if (current instanceof SongBar) {
			final SongData song = ((SongBar) current).getSongData();
			if (song != null) {
				String hash = song.getSha256();
				if (hash != null && hash.length() > 0) {
					StringSelection stringSelection = new StringSelection(hash);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(stringSelection, null);
					selector.main.getMessageRenderer().addMessage("SHA256 hash copied : " + hash, 2000, Color.GOLD, 0);
				}
			}
		}
	}),
	/**
	 * 楽曲ファイルのDLサイトをOS既定のブラウザーで開く
	 */
	OPEN_DOWNLOAD_SITE(selector -> {
		Bar current = selector.getBarManager().getSelected();
		if (current instanceof SongBar) {
			final SongData song = ((SongBar) current).getSongData();
			if (song != null) {
				if (song.getUrl() != null && song.getUrl().length() > 0) {
					try {
						URI uri = new URI(song.getUrl());
						Desktop.getDesktop().browse(uri);
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
				if (song.getAppendurl() != null && song.getAppendurl().length() > 0
						&& !song.getAppendurl().equals(song.getUrl())) {
					try {
						URI uri = new URI(song.getAppendurl());
						Desktop.getDesktop().browse(uri);
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}
		}
	}), 
	DOWNLOAD_IPFS(selector -> {
		Queue<DirectoryBar> dir = selector.getBarManager().getDirectory();
		String[] acceptdomain = { "lnt.softether.net", "www.ribbit.xyz", "rattoto10.jounin.jp",
				"flowermaster.web.fc2.com", "stellawingroad.web.fc2.com", "pmsdifficulty.xxxxxxxx.jp", "walkure.net",
				"stellabms.xyz", "dpbmsdelta.web.fc2.com", "cgi.geocities.jp/asahi3jpn", "nekokan.dyndns.info" };
		boolean startdownload = false;
		for (DirectoryBar d : dir) {
			if (d instanceof TableBar) {
				String selecturl = ((TableBar) d).getUrl();
				if (selecturl == null)
					break;
				for (String url : acceptdomain) {
					if (selecturl.startsWith("http://" + url) || selecturl.startsWith("https://" + url)) {
						Bar current = selector.getBarManager().getSelected();
						if (current instanceof SongBar) {
							final SongData song = ((SongBar) current).getSongData();
							if (song != null && song.getIpfs() != null) {
								selector.main.getMusicDownloadProcessor().start(song);
								startdownload = true;
							}
						}
						break;
					}
				}
				if (!startdownload) {
					Logger.getGlobal().info("ダウンロードは開始されませんでした。");
				}
				break;
			}

		}
	}),
	/**
	 * 楽曲フォルダ/難易度表を更新する
	 */
	UPDATE_FOLDER(selector -> {
		Bar selected = selector.getBarManager().getSelected();
		if (selected instanceof FolderBar) {
			selector.main.updateSong(((FolderBar) selected).getFolderData().getPath());
		} else if (selected instanceof TableBar) {
			selector.main.updateTable((TableBar) selected);
		} else if (selected instanceof SongBar) {
			final String path = ((SongBar) selected).getSongData().getPath();
			if (path != null) {
				selector.main.updateSong(Paths.get(path).getParent().toString());
			}
		}
	}),
	/**
	 * 同一フォルダにある譜面を全て表示する．コースの場合は構成譜面を全て表示する
	 */
	SHOW_SONGS_ON_SAME_FOLDER(selector -> {
		final BarManager bar = selector.getBarManager();
		Bar current = selector.getBarManager().getSelected();
		if (current instanceof SongBar && ((SongBar) current).existsSong()
				&& (bar.getDirectory().size == 0 || !(bar.getDirectory().last() instanceof SameFolderBar))) {
			SongData sd = ((SongBar) current).getSongData();
			bar.updateBar(new SameFolderBar(selector, sd.getFullTitle(), sd.getFolder()));
			selector.play(FOLDER_OPEN);
		} else if (current instanceof GradeBar) {
			List<Bar> songbars = Arrays.asList(((GradeBar) current).getSongDatas()).stream().distinct()
					.map(SongBar::new).collect(Collectors.toList());
			bar.updateBar(new ContainerBar(current.getTitle(), songbars.toArray(new Bar[songbars.size()])));
			selector.play(FOLDER_OPEN);
		}
	});

	public final Consumer<MusicSelector> function;

	private MusicSelectCommand(Consumer<MusicSelector> function) {
		this.function = function;
	}
}
