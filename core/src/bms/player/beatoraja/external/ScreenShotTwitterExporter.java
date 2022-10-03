package bms.player.beatoraja.external;

import static bms.player.beatoraja.skin.SkinProperty.NUMBER_PLAYLEVEL;
import static bms.player.beatoraja.skin.SkinProperty.STRING_FULLTITLE;
import static bms.player.beatoraja.skin.SkinProperty.STRING_TABLE_LEVEL;
import static bms.player.beatoraja.skin.SkinProperty.STRING_TABLE_NAME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.BufferUtils;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.property.IntegerPropertyFactory;
import bms.player.beatoraja.skin.property.StringPropertyFactory;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.UploadedMedia;
import twitter4j.conf.ConfigurationBuilder;

public class ScreenShotTwitterExporter implements ScreenShotExporter {

	private String consumerKey;
	private String consumerSecret;
	private String accessToken;
	private String accessTokenSecret;
	
	public ScreenShotTwitterExporter(PlayerConfig player) {
		consumerKey = player.getTwitterConsumerKey();
		consumerSecret = player.getTwitterConsumerSecret();
		accessToken = player.getTwitterAccessToken();
		accessTokenSecret = player.getTwitterAccessTokenSecret();
	}
	
	@Override
	public boolean send(MainState currentState, byte[] pixels) {
		java.lang.StringBuilder builder = new java.lang.StringBuilder();
		if(currentState instanceof MusicSelector) {
			// empty
		} else if(currentState instanceof MusicDecide) {
			// empty
		} if(currentState instanceof BMSPlayer) {
			final String tablename = StringPropertyFactory.getStringProperty(STRING_TABLE_NAME).get(currentState);
			final String tablelevel = StringPropertyFactory.getStringProperty(STRING_TABLE_LEVEL).get(currentState);

			if(tablename.length() > 0){
				builder.append(tablelevel);
			}else{
				builder.append("LEVEL").append(IntegerPropertyFactory.getIntegerProperty(NUMBER_PLAYLEVEL).get(currentState));
			}
			final String fulltitle = StringPropertyFactory.getStringProperty(STRING_FULLTITLE).get(currentState);
			if(fulltitle.length() > 0) {
				builder.append(" ").append(fulltitle);
			}
		} else if(currentState instanceof MusicResult || currentState instanceof CourseResult) {
			if(currentState instanceof MusicResult) {
				final String tablename = StringPropertyFactory.getStringProperty(STRING_TABLE_NAME).get(currentState);
				final String tablelevel = StringPropertyFactory.getStringProperty(STRING_TABLE_LEVEL).get(currentState);
				if(tablename.length() > 0){
					builder.append(tablelevel);
				}else{
					builder.append("LEVEL").append(IntegerPropertyFactory.getIntegerProperty(NUMBER_PLAYLEVEL).get(currentState));
				}
			}
			final String fulltitle = StringPropertyFactory.getStringProperty(STRING_FULLTITLE).get(currentState);
			if(fulltitle.length() > 0) {
				builder.append(" ").append(fulltitle);
			}
			builder.append(" ");
			builder.append(ScreenShotExporter.getClearTypeName(currentState));
			builder.append(" ");
			builder.append(ScreenShotExporter.getRankTypeName(currentState));
		} else if(currentState instanceof KeyConfiguration) {
			// empty
		}
		String text = builder.toString();
		text = text.replace("\\", "￥").replace("/", "／").replace(":", "：").replace("*", "＊").replace("?", "？").replace("\"", "”").replace("<", "＜").replace(">", "＞").replace("|", "｜").replace("\t", " ");

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setOAuthConsumerKey(consumerKey)
		  .setOAuthConsumerSecret(consumerSecret)
		  .setOAuthAccessToken(accessToken)
		  .setOAuthAccessTokenSecret(accessTokenSecret);
		TwitterFactory twitterFactory = new TwitterFactory(cb.build());
		Twitter twitter = twitterFactory.getInstance();

		Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(),
				Pixmap.Format.RGBA8888);
        try {
			// create png byte stream
			BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
			ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
			PixmapIO.PNG png = new PixmapIO.PNG((int)(pixmap.getWidth() * pixmap.getHeight() * 1.5f));
			png.write(byteArrayOutputStream, pixmap);
			byte[] imageBytes=byteArrayOutputStream.toByteArray();
			ByteArrayInputStream byteArrayInputStream=new ByteArrayInputStream(imageBytes);

			// Upload Media and Post
			UploadedMedia mediastatus = twitter.uploadMedia("from beatoraja", byteArrayInputStream);
			Logger.getGlobal().info("Twitter Media Upload:" + mediastatus.toString());
			StatusUpdate update = new StatusUpdate(text);
			update.setMediaIds(new long[]{mediastatus.getMediaId()});
			Status status = twitter.updateStatus(update);
			Logger.getGlobal().info("Twitter Post:" + status.toString());
			pixmap.dispose();
			currentState.main.getMessageRenderer().addMessage( "Twitter Upload : " + text, 2000, Color.YELLOW, 0);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		pixmap.dispose();
		return false;
	}

}
