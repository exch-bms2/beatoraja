package bms.player.beatoraja.song;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteConfig.SynchronousMode;

import bms.player.beatoraja.SQLiteDatabaseAccessor;

/**
 * ユーザーの楽曲評価データベースへのアクセスクラス
 *
 *
 * @author exch
 */
public class SongReviewAccessor extends SQLiteDatabaseAccessor {

	private final SQLiteDataSource ds;

	private final ResultSetHandler<List<SongReview>> songhandler = new BeanListHandler<SongReview>(
			SongReview.class);

	private final QueryRunner qr;

	public SongReviewAccessor(String filepath) throws ClassNotFoundException {
		super(new Table("review", 
				new Column("sha256", "TEXT",1,1),
				new Column("tag", "TEXT"),
				new Column("favorite", "INTEGER"),
				new Column("levelreview", "REAL"),
				new Column("comment", "TEXT")
				));
		Class.forName("org.sqlite.JDBC");
		try {
			Path parent = Paths.get(filepath).toAbsolutePath().getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
		} catch (IOException e) {
			Logger.getGlobal().severe("楽曲評価データベースディレクトリ作成中の例外:" + e.getMessage());
		}
		SQLiteConfig conf = new SQLiteConfig();
		conf.setSharedCache(true);
		conf.setSynchronous(SynchronousMode.OFF);
		// conf.setJournalMode(JournalMode.MEMORY);
		ds = new SQLiteDataSource(conf);
		ds.setUrl("jdbc:sqlite:" + filepath);
		qr = new QueryRunner(ds);
		try {
			validate(qr);
		} catch (SQLException e) {
			Logger.getGlobal().severe("楽曲データベース初期化中の例外:" + e.getMessage());
		}
	}

	public Map<String, SongReview> getSongReviews() {
		Map<String, SongReview> reviews = new HashMap<>();
		try {
			for (SongReview review : qr.query("SELECT * FROM review", songhandler)) {
				if (review.getSha256() != null && review.getSha256().length() > 0) {
					reviews.put(review.getSha256(), review);
				}
			}
		} catch (SQLException e) {
			Logger.getGlobal().severe("楽曲評価データベース取得中の例外:" + e.getMessage());
		}
		return reviews;
	}

	public SongReview getSongReview(String sha256) {
		if (sha256 == null || sha256.length() == 0) {
			return null;
		}
		try {
			List<SongReview> reviews = qr.query("SELECT * FROM review WHERE sha256 = ?", songhandler, sha256);
			return reviews.size() > 0 ? reviews.get(0) : null;
		} catch (SQLException e) {
			Logger.getGlobal().severe("楽曲評価データベース取得中の例外:" + e.getMessage());
		}
		return null;
	}

	public void setSongReview(SongReview review) {
		if (review == null || review.getSha256() == null || review.getSha256().length() == 0) {
			return;
		}
		try {
			insert(qr, "review", review);
		} catch (SQLException e) {
			Logger.getGlobal().severe("楽曲評価データベース更新中の例外:" + e.getMessage());
		}
	}

	public void setSongReviews(SongData[] songs) {
		if (songs == null || songs.length == 0) {
			return;
		}
		Map<String, SongReview> existingReviews = getSongReviews();
		try (Connection conn = qr.getDataSource().getConnection()) {
			conn.setAutoCommit(false);
			for (SongData song : songs) {
				if (song == null || song.getSha256() == null || song.getSha256().length() == 0) {
					continue;
				}
				SongReview review = existingReviews.get(song.getSha256());
				if (review == null) {
					review = new SongReview();
					review.setSha256(song.getSha256());
				}
				SongReview songReview = song.getSongReview();
				review.setTag(songReview.getTag() != null ? songReview.getTag() : "");
				review.setFavorite(songReview.getFavorite());
				insert(qr, conn, "review", review);
			}
			conn.commit();
		} catch (SQLException e) {
			Logger.getGlobal().severe("楽曲評価データベース更新中の例外:" + e.getMessage());
		}
	}

}
