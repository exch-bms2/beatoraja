package bms.tool.mdprocessor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;
import static java.nio.file.StandardCopyOption.*;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * ipfsによる楽曲ダウンロードを行うクラス
 *
 * @author LNTakeshi
 */
public class MusicDownloadProcessor {

	private Deque<IpfsInformation> commands = new ConcurrentLinkedDeque<IpfsInformation>();
	private String ipfs = "";
	private DownloadDaemonThread daemon;
	private String message = "";

	public final MusicDatabaseAccessor main;

    public MusicDownloadProcessor(String ipfs, MusicDatabaseAccessor main) {
    	this.ipfs = ipfs;
    	this.main = main;
    }

    public void start(IpfsInformation song){
    	if((daemon == null || !daemon.isAlive())){
			if (ipfs == null || ipfs.length() == 0) {
				ipfs = "https://gateway.ipfs.io/";
			}
			if (!ipfs.endsWith("/")) {
				ipfs = ipfs + "/";
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
        		while(!dispose){
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
								downloadipfs = new DownloadIpfsThread(diffpath, "ipfs" + File.separator + diffpath);
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
			Logger.getGlobal().info("IPFS Thread終了");
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
			URL url = null;
			InputStream in = null;
			OutputStream out = null;
			try {
				url = new URL(
						ipfs + "api/v0/get?arg=" + ipfspath
								+ "&archive=true&compress=true");
				Files.deleteIfExists(Paths.get("ipfs/bms.tar.gz"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				URLConnection connection = url.openConnection();
				connection.connect();
				in = new BufferedInputStream(url.openStream());
				out = new FileOutputStream("ipfs/bms.tar.gz");
			} catch (Exception e) {
				if (url != null) {
					Logger.getGlobal().info("URL:" + url.toString() + "に接続失敗。");
				}
			}
			byte data[] = new byte[1024 * 512];
			long total = 0;
			int count;
			if (in != null && out != null) {
				try {
					while ((count = in.read(data)) != -1) {
						total += count;
						setMessage("downloading:" + path + " " + total / 1024 / 1024 + "MB");
						out.write(data, 0, count);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				if (out != null) {
					out.flush();
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			String gz = "ipfs/bms.tar.gz";
			String tar = "ipfs/bms.tar";
			try (FileInputStream fis = new FileInputStream(gz);
					GzipCompressorInputStream archive = new GzipCompressorInputStream(fis);
					FileOutputStream fos = new FileOutputStream(tar)) {
				int size = 0;
				byte[] buf = new byte[1048576];
				while ((size = archive.read(buf)) > 0) {
					fos.write(buf, 0, size);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try (TarArchiveInputStream tin = new TarArchiveInputStream(new FileInputStream(tar))) {
				ArchiveEntry entry = null;
				while ((entry = tin.getNextEntry()) != null) {
					File file = new File("ipfs/" + entry.getName());
					if (entry.isDirectory()) {
						file.mkdirs();
						continue;
					}
					if (!file.getParentFile().exists()) {
						file.getParentFile().mkdirs();
					}
					try (FileOutputStream fos = new FileOutputStream(file)) {
						IOUtils.copy(tin, fos);
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Files.deleteIfExists(Paths.get(gz));
				Files.deleteIfExists(Paths.get(tar));
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			Path dir = Paths.get("ipfs" + File.separator + ipfspath);
			if (ipfspath != null && ipfspath.length() != 0 && !dir.toString().equals(path.toString())
					&& Files.exists(dir)) {
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