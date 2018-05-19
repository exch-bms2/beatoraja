package bms.tool.mdprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

/**
 * ipfsによる楽曲ダウンロードを行うクラス
 *
 * @author LNTakeshi
 */
public class MusicDownloadProcessor {

	private Deque<IpfsInformation> commands = new ConcurrentLinkedDeque<IpfsInformation>();
	private String ipfs = "";
	private DownloadDaemonThread daemon;
	private boolean daemonexists;
	private String message = "";

	public final MusicDatabaseAccessor main;

    public MusicDownloadProcessor(String ipfs, MusicDatabaseAccessor main) {
    	this.ipfs = ipfs;
    	this.main = main;
    }

    public void start(IpfsInformation song){
    	if((daemon == null || !daemon.isAlive())){
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
				daemon.join();
			} catch (InterruptedException e) {
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

    public String getDownloadpath() {
		return daemon != null ? daemon.downloadpath : null;
	}

	public void setDownloadpath(String downloadpath) {
		if(daemon != null) daemon.downloadpath = downloadpath;
	}

    public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	class DownloadDaemonThread extends Thread {

    	private boolean dispose;
    	private Process pd = null;
		private String ipfspath = "";
    	private String path = "";
    	private String diffpath = "";
		private Path orgbms;
    	private boolean download;
    	private String downloadpath;
    	private IpfsInformation song;

		public void run() {
			DownloadIpfsThread downloadipfs = null;
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
						orgbms = null;
        				if(orgmd5 != null && orgmd5.size() != 0){
							String[] s = main.getMusicPaths(orgmd5.toArray(new String[orgmd5.size()]));
							if (s.length != 0) {
								for (String bms : s) {
									Path bmspath = Paths.get(bms);
									if (bmspath.toFile().exists()) {
										orgbms = bmspath;
										path = bmspath.getParent().toString();
										break;
									}
								}
        					}
        				}
						if (ipfspath != null && ipfspath.length() != 0 && orgbms == null) {
							downloadipfs = new DownloadIpfsThread(ipfspath, path);
							downloadipfs.start();
        					download = true;
							Logger.getGlobal().info("BMS本体取得開始");
						} else if (ipfspath != null && ipfspath.length() != 0 && diffpath != null
								&& diffpath.length() != 0) {
        					Logger.getGlobal().info(path+"は既に存在します（差分取得のみ）");
        					download = true;
        				}
        			}

					if (download && (downloadipfs == null || !downloadipfs.isAlive())) {
        				if(diffpath != null && diffpath.length() != 0){
							File f = Paths.get("ipfs/" + diffpath).toFile();
							if (ipfspath == null || ipfspath.length() == 0) {
								if (f.exists() && f.isDirectory()) {
        							for(File fs:f.listFiles()){
										Files.move(fs.toPath(), Paths.get(path + "/" + fs.getName()), REPLACE_EXISTING);
        							}
        							f.delete();
								} else if (f.exists()) {
									Files.move(f.toPath(), Paths.get(path + "/" + diffpath + ".bms"),
											REPLACE_EXISTING);
        						}
        						diffpath = "";
        					}else{
								downloadipfs = new DownloadIpfsThread(diffpath, "ipfs/" + diffpath);
								ipfspath = "";
								downloadipfs.start();
								Logger.getGlobal().info("差分取得開始");
        					}
        				}else if(downloadpath == null){
        					Path p = Paths.get(path).toAbsolutePath();
        					downloadpath = p.toFile().exists() ? p.toString() : null;
        					download = false;
							ipfspath = "";
        				}
        			}
        			sleep(100);
        		}
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        	Logger.getGlobal().info("daemon終了");
			if (pd != null && pd.isAlive())
				pd.destroy();
			if (downloadipfs != null && downloadipfs.isAlive()) {
				downloadipfs.interrupt();
			}
        	dispose = false;
        	download = false;
        }
    }

	class DownloadIpfsThread extends Thread {
		private String ipfspath = "";
		private String path = "";

		public DownloadIpfsThread(String ipfspath, String path) {
			setMessage("downloading:" + path);
			this.ipfspath = ipfspath;
			this.path = path;
		}

		public void run() {
			if (daemonexists) {
				ProcessBuilder pbc = new ProcessBuilder(ipfs, "get", ipfspath, "-o=" + path);
				pbc.inheritIO();
				Process pc = null;
				try {
					pc = pbc.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
				while (pc != null && pc.isAlive()) {
					if (daemon.dispose) {
						pc.destroy();
					}
					try {
						sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} else {
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
				} catch (Exception e) {
					e.printStackTrace();
				}
				Path p = Paths.get("ipfs/bms.tar.gz").toAbsolutePath();
				if (Files.exists(p)) {
					try (TarInputStream tin = new TarInputStream(
							new GZIPInputStream(new FileInputStream(p.toFile())))) {
						for (TarEntry tarEnt = tin.getNextEntry(); tarEnt != null; tarEnt = tin.getNextEntry()) {
							File file = new File("ipfs/" + tarEnt.getName());
							if (tarEnt.isDirectory()) {
								file.mkdir();
							} else {
								if (!file.getParentFile().exists()) {
									file.getParentFile().mkdirs();
								}
								try (FileOutputStream fos = new FileOutputStream(file)) {
									tin.copyEntryContents(fos);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						tin.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				try {
					Files.deleteIfExists(p);
				} catch (IOException e) {
					e.printStackTrace();
				}
				Path dir = Paths.get("ipfs/" + ipfspath);
				if (ipfspath != null && ipfspath.length() != 0 && Files.exists(dir)) {
					if (Files.isDirectory(dir)) {
						if (!Paths.get(path).toFile().exists())
							Paths.get(path).toFile().mkdirs();
						File d = dir.toFile();
						for (File f : d.listFiles()) {
							try {
								Files.move(f.toPath(), Paths.get(path + "/" + f.toPath().getFileName().toString()),
										REPLACE_EXISTING);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					} else if (!Paths.get(path).toFile().exists()) {
						try {
							Files.move(dir, Paths.get(path), REPLACE_EXISTING);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					try {
						Files.deleteIfExists(dir);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}
		}
	}
}