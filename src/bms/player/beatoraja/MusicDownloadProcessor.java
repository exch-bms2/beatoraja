package bms.player.beatoraja;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

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
    			daemon = new DownloadDaemonThread();
    			daemon.start();
    		}else{
    			Logger.getGlobal().info("ipfsの実行ファイルが指定されていないため、ダウンロードは開始されません");
    		}
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
    	private String path = "";
    	private String diffpath = "";
    	private boolean download;
    	private String downloadpath;
    	private SongData song;
		public void run() {
        	dispose = false;
        	try{
        		ProcessBuilder pbd = new ProcessBuilder(ipfs,"init");
        		pbd.inheritIO();
        		pd = pbd.start();
        		pd.waitFor();

        		pbd = new ProcessBuilder(ipfs,"repo","fsck");
        		pbd.inheritIO();
        		pd = pbd.start();
        		pd.waitFor();

        		pbd = new ProcessBuilder(ipfs,"daemon");
        		pbd.inheritIO();
        		while(!dispose){
        			if(!pd.isAlive()){
                		Logger.getGlobal().info("ipfs daemon開始");
        				pd = pbd.start();
        			}
        			if(!commands.isEmpty() && !download){
        				song = commands.removeFirst();
        				path = song.getIpfs();
        				diffpath = song.getAppendIpfs();
        				if(path.toLowerCase().startsWith("/ipfs")){
        					path = path.substring(1);
        				}else{
        					path = "ipfs/" + path;
        				}
        				if(diffpath != null &&diffpath.toLowerCase().startsWith("/ipfs")){
        					diffpath = diffpath.substring(1);
        				}else if(diffpath != null){
        					diffpath = "ipfs/" + diffpath;
        				}
        				List<String> orgmd5 = song.getOrg_md5();
        				if(orgmd5 != null && orgmd5.size() != 0){
        					SongData[] s = main.getSongDatabase().getSongDatas(orgmd5.toArray(new String[orgmd5.size()]));
        					if(s.length != 0){
        						path = Paths.get(s[0].getPath()).getParent().toString();
        					}
        				}

        				if(!Paths.get(path).toFile().exists()){
        					ProcessBuilder pbc = new ProcessBuilder(ipfs,"get","/" +path,"-o="+path);
        					pbc.inheritIO();
        					pc = pbc.start();
        					download = true;

        					message = "downloading:/"+path;
        					Logger.getGlobal().info("ipfs client本体取得開始");
        				}else{
        					Logger.getGlobal().info(path+"は既に存在します（差分取得のみ）");
        					download = true;
        				}
        			}

        			if(download && !( pc != null &&pc.isAlive())){
        				if(diffpath.length() != 0){
        					File f = Paths.get(diffpath).toFile();
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
        						Logger.getGlobal().info(diffpath);
        						ProcessBuilder pbc = new ProcessBuilder(ipfs,"get","/" +diffpath,"-o="+diffpath);
        						pbc.inheritIO();
        						pc = pbc.start();
        						message = "downloading diff:/"+path;
        						Logger.getGlobal().info("ipfs client差分取得開始");
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
        	pd.destroy();
        	if(pc != null &&pc.isAlive()){
        		pc.destroy();
        	}
        	dispose = false;
        	download = false;
        }
    }
}