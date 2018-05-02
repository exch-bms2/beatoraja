package bms.player.beatoraja;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import static java.nio.file.StandardCopyOption.*;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

import bms.player.beatoraja.song.SongData;

/**
 * ipfsによる楽曲ダウンロードを行うクラス
 *
 * @author LNTakeshi
 */
public class MusicDownloadProcessor {

	private Deque<SongData> commands = new ConcurrentLinkedDeque<SongData>();
	private String ipfs = "";
	private DownloadDaemonThread daemon;
	private FreeTypeFontGenerator generator;
	BitmapFont downloadfont;
	private SpriteBatch sprite;
	private boolean daemonexists;

	public final MainController main;


    public MusicDownloadProcessor(MainController main) {
    	this.main = main;
         generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
          sprite = new SpriteBatch();
    }


    public void start(SongData song){
    	if((daemon == null || !daemon.isAlive())){
    		ipfs = main.getConfig().getIpfspath();
    		if(ipfs != null && Paths.get(ipfs).toFile().exists()){
				daemonexists = true;
    		}
			daemon = new DownloadDaemonThread();
			daemon.start();
    	}
    	if(song!= null){
    		commands.add(song);
    	}
    }

    public void dispose(){
    	if(daemon != null && daemon.isAlive()){
    		daemon.dispose = true;
    		try {
    			if(daemon.pc != null)daemon.pc.waitFor();
    			if(daemon.pd != null)daemon.pd.waitFor();
    		} catch (InterruptedException e) {
    			// TODO 自動生成された catch ブロック
    			e.printStackTrace();
    		}
    	}
    }

    public boolean isDownload(){
    	return daemon != null && daemon.download;
    }

    public boolean isAlive(){
    	return daemon != null && daemon.isAlive();
    }

    public void drawMessage(){
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		parameter.characters += daemon.message;
		if(downloadfont != null) {
			downloadfont.dispose();
		}
		downloadfont = generator.generateFont(parameter);
		sprite.begin();
		downloadfont.setColor(0,1,1,0.5f + (System.currentTimeMillis() % 750) / 1000.0f);
		downloadfont.draw(sprite, daemon.message, 100, main.getConfig().getResolution().height - 2);
		sprite.end();
    }

    public String getDownloadpath() {
		return daemon != null ? daemon.downloadpath : null;
	}

	public void setDownloadpath(String downloadpath) {
		if(daemon != null) daemon.downloadpath = downloadpath;
	}

    class DownloadDaemonThread extends Thread {

		private String message = "";
    	private boolean dispose;
    	private Process pd = null;
    	private Process pc = null;
		private String ipfspath = "";
    	private String path = "";
    	private String diffpath = "";
    	private boolean download;
    	private String downloadpath;
    	private SongData song;
		public void run() {
			DownloadTarThread downloadtar = null;
        	dispose = false;
        	try{
				ProcessBuilder pbd = null;
				if (daemonexists) {
					pbd = new ProcessBuilder(ipfs, "init");
					pbd.inheritIO();
					pd = pbd.start();
					pd.waitFor();

					pbd = new ProcessBuilder(ipfs, "repo", "fsck");
					pbd.inheritIO();
					pd = pbd.start();
					pd.waitFor();

					pbd = new ProcessBuilder(ipfs, "daemon");
					pbd.inheritIO();
				}
        		while(!dispose){
					if (daemonexists && !pd.isAlive()) {
                		Logger.getGlobal().info("ipfs daemon開始");
        				pd = pbd.start();
        			}
        			if(!commands.isEmpty() && !download){
        				song = commands.removeFirst();

						ipfspath = song.getIpfs();
        				diffpath = song.getAppendIpfs();

						if (ipfspath.toLowerCase().startsWith("/ipfs/")) {
							ipfspath = path.substring(5);
        				}
						path = "[" + song.getArtist() + "]" + song.getTitle();
						path = "ipfs/" + path.replaceAll("[(\\\\|/|:|\\*|\\?|\"|<|>|\\|)]", "");
						if (diffpath != null && diffpath.toLowerCase().startsWith("/ipfs/")) {
							diffpath = diffpath.substring(5);
        				}

        				List<String> orgmd5 = song.getOrg_md5();
        				if(orgmd5 != null && orgmd5.size() != 0){
        					SongData[] s = main.getSongDatabase().getSongDatas(orgmd5.toArray(new String[orgmd5.size()]));
        					if(s.length != 0){
        						path = Paths.get(s[0].getPath()).getParent().toString();
        					}
        				}
						if (!Paths.get(path).toFile().exists() && daemonexists) {
							ProcessBuilder pbc = new ProcessBuilder(ipfs, "get", ipfspath, "-o=" + path);
        					pbc.inheritIO();
        					pc = pbc.start();
        					download = true;
        					message = "downloading:/"+path;
							Logger.getGlobal().info("ipfs BMS本体取得開始");
						} else if (!Paths.get(path).toFile().exists()) {
							downloadtar = new DownloadTarThread();
							downloadtar.ipfspath = ipfspath;
							downloadtar.start();
							download = true;
							message = "downloading:/" + path;
							Logger.getGlobal().info("BMS本体取得開始");
        				}else{
        					Logger.getGlobal().info(path+"は既に存在します（差分取得のみ）");
							ipfspath = "";
        					download = true;
        				}
        			}

					if (download
							&& ((pc != null && !pc.isAlive()) || (downloadtar != null && !downloadtar.isAlive())
									|| (pc == null && downloadtar == null))) {
						if (!daemonexists) {
							Path p = Paths.get("ipfs/bms.tar.gz").toAbsolutePath();
							if (Files.exists(p)) {
								TarInputStream tin = new TarInputStream(
										new GZIPInputStream(new FileInputStream(p.toFile())));
								for (TarEntry tarEnt = tin.getNextEntry(); tarEnt != null; tarEnt = tin
										.getNextEntry()) {
									if (tarEnt.isDirectory()) {
										new File("ipfs/" + tarEnt.getName()).mkdir();
									} else {
										FileOutputStream fos = new FileOutputStream(
												new File("ipfs/" + tarEnt.getName()));
										tin.copyEntryContents(fos);
										fos.close();
									}
								}
								tin.close();

							}
							Files.deleteIfExists(p);
							if (ipfspath != null && ipfspath.length() != 0) {
								Files.move(Paths.get("ipfs/" + ipfspath), Paths.get(path), ATOMIC_MOVE);
							}
						}
        				if(diffpath != null && diffpath.length() != 0){
							ipfspath = "";
							File f = Paths.get("ipfs/" + diffpath).toFile();
        					if(f.exists()){
        						if(f.isDirectory()){
        							for(File fs:f.listFiles()){
        								Files.move(fs.toPath(), Paths.get(path+"/"+fs.getName()));
        							}
        							f.delete();
        						}else{
        							Files.move(f.toPath(), Paths.get(path+"/"+diffpath.substring(5)+".bms"));
        						}
        						diffpath = "";
        					}else{
								if (daemonexists) {
									ProcessBuilder pbc = new ProcessBuilder(ipfs, "get", diffpath,
											"-o=ipfs/" + diffpath);
									pbc.inheritIO();
									pc = pbc.start();
								} else {
									downloadtar = new DownloadTarThread();
									downloadtar.ipfspath = diffpath;
									downloadtar.start();
								}
								message = "downloading diff:/" + diffpath;
								Logger.getGlobal().info("差分取得開始");
        					}
        				}else if(downloadpath == null){
        					Path p = Paths.get(path).toAbsolutePath();
        					downloadpath = p.toFile().exists() ? p.toString() : null;
        					download = false;
        				}
        			}
        			sleep(100);
        		}
        	} catch (Exception e) {
        		// TODO 自動生成された catch ブロック
        		e.printStackTrace();
        	}
        	Logger.getGlobal().info("daemon終了");
			if (pd != null && pd.isAlive())
				pd.destroy();
			if (pc != null && pc.isAlive())
				pc.destroy();
        	dispose = false;
        	download = false;
        }
    }

	class DownloadTarThread extends Thread {
		private String ipfspath = "";

		public void run() {
			URL url = null;
			try {
				url = new URL(
						"http://ipfs.io/api/v0/get?arg=" + ipfspath
								+ "&archive=true&compress=true");
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
			Path dlpath = Paths.get("ipfs/bms.tar.gz");
			try {
				Files.copy(url.openStream(), dlpath, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}