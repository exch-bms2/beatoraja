package bms.player.beatoraja.external;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.BufferUtils;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.property.IntegerPropertyFactory;
import bms.player.beatoraja.skin.property.StringPropertyFactory;

public class ScreenShotFileExporter implements ScreenShotExporter {

	@Override
	public boolean send(MainState currentState, byte[]  pixels) {
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

		Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
		try {
			String path = "screenshot/" + sdf.format(Calendar.getInstance().getTime()) + stateName +".png";
			BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
			PixmapIO.writePNG(new FileHandle(path), pixmap);
			Logger.getGlobal().info("スクリーンショット保存:" + path);
			pixmap.dispose();
			currentState.main.getMessageRenderer().addMessage("Screen shot saved : " + path, 2000, Color.GOLD, 0);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		pixmap.dispose();
		return false;
	}

}
