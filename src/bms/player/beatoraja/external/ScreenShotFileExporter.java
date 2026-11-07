package bms.player.beatoraja.external;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.BufferUtils;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.external.discord.DiscordWebhook;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.property.IntegerPropertyFactory;
import bms.player.beatoraja.skin.property.StringPropertyFactory;

public class ScreenShotFileExporter implements ScreenShotExporter {

	@Override
	public boolean send(MainState currentState, byte[]  pixels) {
		return send(currentState, pixels, DiscordWebhook.capture(currentState));
	}

	public boolean send(MainState currentState, byte[] pixels, DiscordWebhook.Post webhook) {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String stateName = "";
		if(currentState instanceof MusicSelector) {
			stateName = "_Music_Select";
		} else if(currentState instanceof MusicDecide) {
			stateName = "_Decide";
		} if(currentState instanceof BMSPlayer) {
			final String tablelevel = StringPropertyFactory.getStringProperty(STRING_TABLE_LEVEL).get(currentState);
			if(tablelevel.length() > 0){
				stateName = "_Play_" + tablelevel;
			}else{
				stateName = "_Play_LEVEL" + IntegerPropertyFactory.getIntegerProperty(NUMBER_PLAYLEVEL).get(currentState);
			}
			final String fulltitle = StringPropertyFactory.getStringProperty(STRING_FULLTITLE).get(currentState);
			if(fulltitle.length() > 0) {
				stateName += " " + fulltitle;
			}
		} else if(currentState instanceof MusicResult || currentState instanceof CourseResult) {
			if(currentState instanceof MusicResult){
				final String tablelevel = StringPropertyFactory.getStringProperty(STRING_TABLE_LEVEL).get(currentState);
				if(tablelevel.length() > 0){
					stateName += "_" + tablelevel + " ";
				}else{
					stateName += "_LEVEL" + IntegerPropertyFactory.getIntegerProperty(NUMBER_PLAYLEVEL).get(currentState) + " ";
				}
			}else{
				stateName += "_";
			}
			final String fulltitle = StringPropertyFactory.getStringProperty(STRING_FULLTITLE).get(currentState);
			if(fulltitle.length() > 0) stateName += fulltitle;
			stateName += " " + ScreenShotExporter.getClearTypeName(currentState);
			stateName += " " + ScreenShotExporter.getRankTypeName(currentState);
		} else if(currentState instanceof KeyConfiguration) {
			stateName = "_Config";
		}
		stateName = stateName.replace("\\", "￥").replace("/", "／").replace(":", "：").replace("*", "＊").replace("?", "？").replace("\"", "”").replace("<", "＜").replace(">", "＞").replace("|", "｜").replace("\t", " ");

		String name = sdf.format(Calendar.getInstance().getTime()) + stateName;
		Path path;
		Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
		try {
			BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
			path = savePng(name, pixmap);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			pixmap.dispose();
		}
		if (webhook != null) webhook.send(path);
		Logger.getGlobal().info("スクリーンショット保存:" + path);
		currentState.main.getMessageRenderer().addMessage("Screen shot saved : " + path, 2000, Color.GOLD, 0);
		this.sendClipboard(currentState, path);
		return true;
	}
	
	private static Path savePng(String name, Pixmap pixmap) throws IOException {
		Path directory = Files.createDirectories(Path.of("screenshot"));
		PixmapIO.PNG encoder = new PixmapIO.PNG((int) (pixmap.getWidth() * pixmap.getHeight() * 1.5f));
		encoder.setFlipY(false);
		try {
			// A queued upload must keep its own file when two screenshots have the same timestamp.
			for (int suffix = 0; ; suffix++) {
				Path path = directory.resolve(name + (suffix == 0 ? "" : "_" + suffix) + ".png");
				try (OutputStream output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
					encoder.write(output, pixmap);
					return path;
				} catch (FileAlreadyExistsException collision) {
					// Try the next suffix.
				}
			}
		} finally {
			encoder.dispose();
		}
	}

	private void sendClipboard(MainState currentState, Path path) {
		if (!currentState.resource.getConfig().isSetClipboardWhenScreenshot()) {
			// スクショのクリップボードコピーが有効でないなら終わる
			return;
		}
		if (DesktopActions.copyImage(path)) {
			Logger.getGlobal().info("スクリーンショット保存: Clipboard");
			currentState.main.getMessageRenderer().addMessage("Screen shot saved : Clipboard", 2000, Color.GOLD, 0);
		}
	}
}
