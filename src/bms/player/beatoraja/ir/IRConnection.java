package bms.player.beatoraja.ir;

import java.io.File;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import bms.model.BMSModel;
import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.PlayerInformation;
import bms.player.beatoraja.TableData;

/**
 * IR接続用インターフェイス
 * 
 * @author exch
 */
public interface IRConnection {
	
	public IRResponse<Object> register(String id, String pass, String name);

	/**
	 * IRにログインする。起動時に呼び出される
	 * @param id ユーザーID
	 * @param pass パスワード
	 */
	public IRResponse<Object> login(String id, String pass);

	/**
	 * ライバルデータを収録する
	 * @return ライバルデータ
	 */
	public IRResponse<PlayerInformation[]> getRivals();

	/**
	 * IRに設定されている表データを収録する
	 * @return IRで取得可能な表データ
	 */
	public IRResponse<TableData[]> getTableDatas();

	/**
	 * スコアデータを取得する
	 * @param id ユーザーID。譜面に登録されているスコアデータを全取得する場合はnullを入れる
	 * @param model スコアデータを取得する譜面。ユーザーIDのスコアデータを全取得する場合はnullを入れる
	 * @return
	 */
	public IRResponse<IRScoreData[]> getPlayData(String id, BMSModel model);

	/**
	 * スコアデータを送信する
	 * @param model
	 * @param score
	 */
	public IRResponse<Object> sendPlayData(BMSModel model, IRScoreData score);
	
	/**
	 * 楽曲のURLを取得する
	 * @param model 譜面データ
	 * @return
	 */
	public String getSongURL(BMSModel model);

	/**
	 * プレイヤーURLを取得する
	 * @param id ユーザーID
	 * @return
	 */
	public String getPlayerURL(String id);
	
	public static String[] getAllAvailableIRConnectionName() {
		Class[] irclass = getAllAvailableIRConnection();
		String[] names = new String[irclass.length];
		for(int i = 0;i < names.length;i++) {
			try {
				names[i] = irclass[i].getField("NAME").get(null).toString();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return names;
	}
	
	public static IRConnection getIRConnection(String name) {
		Class[] irclass = getAllAvailableIRConnection();
		for(int i = 0;i < irclass.length;i++) {
			try {
				if(name.equals(irclass[i].getField("NAME").get(null).toString())) {
					return (IRConnection) irclass[i].newInstance();
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	static Class[] getAllAvailableIRConnection() {
		List<Class> classes = new ArrayList();

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Enumeration<URL> urls = cl.getResources("bms/player/beatoraja/ir");
			while(urls.hasMoreElements()) {
				URL url  = urls.nextElement();
				if(url.getProtocol().equals("jar")) {
			        JarURLConnection jarUrlConnection = (JarURLConnection)url.openConnection();
			        JarFile jarFile = null;
			        try {
			            jarFile = jarUrlConnection.getJarFile();
			            Enumeration<JarEntry> jarEnum = jarFile.entries();

			            while (jarEnum.hasMoreElements()) {
			                JarEntry jarEntry = jarEnum.nextElement();
			                String path = jarEntry.getName();
			                if (path.startsWith("bms/player/beatoraja/ir/") && path.endsWith(".class")) {
			                    Class c = cl.loadClass("bms.player.beatoraja.ir." + path.substring(path.lastIndexOf("/") + 1, path.length() - 6));
								for(Class inf : c.getInterfaces()) {
									if(inf == IRConnection.class) {
										for(Field f : c.getFields()) {
											if(f.getName().equals("NAME")) {
												Object irname = c.getField("NAME").get(null);
												classes.add(c);										
											}
										}
										break;
									}
								}
			                }
			            }
			        } finally {
			            if (jarFile != null) {
			                jarFile.close();
			            }
			        }
				}
				if(url.getProtocol().equals("file")) {
					File dir = new File(url.getPath());
		            for (String path : dir.list()) {
		                if (path.endsWith(".class")) {
		                    Class c = cl.loadClass("bms.player.beatoraja.ir." + path.substring(0, path.length() - 6));
							for(Class inf : c.getInterfaces()) {
								if(inf == IRConnection.class) {
									for(Field f : c.getFields()) {
										if(f.getName().equals("NAME")) {
											Object irname = c.getField("NAME").get(null);
											classes.add(c);										
										}
									}
									break;
								}
							}
		                }
		            }
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}		
		return classes.toArray(new Class[classes.size()]);
	}
}
