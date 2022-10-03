package bms.player.beatoraja.ir;

import java.io.File;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * IRConnectionの管理用クラス
 * 
 * @author exch
 */
public class IRConnectionManager {
	
	/**
	 * 検出されたIRConnection
	 */
	private static Class<IRConnection>[] irconnections;


	/**
	 * 利用可能な全てのIRConnectionの名称を返す
	 * 
	 * @return IRConnectionの名称
	 */
	public static String[] getAllAvailableIRConnectionName() {
		Class<IRConnection>[] irclass = getAllAvailableIRConnection();
		String[] names = new String[irclass.length];
		for (int i = 0; i < names.length; i++) {
			try {
				names[i] = irclass[i].getField("NAME").get(null).toString();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return names;
	}

	/**
	 * 名称に対応したIRConnectionインスタンスを返す
	 * 
	 * @param name
	 *            IRCOnnectionの名称
	 * @return 対応するIRConnectionインスタンス。存在しない場合はnull
	 */
	public static IRConnection getIRConnection(String name) {
		Class<IRConnection> irclass = getIRConnectionClass(name);
		if(irclass != null) {
			try {
				return (IRConnection) irclass.getDeclaredConstructor().newInstance();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static Class<IRConnection> getIRConnectionClass(String name) {
		if (name == null || name.length() == 0) {
			return null;
		}
		Class<IRConnection>[] irclass = getAllAvailableIRConnection();
		for (int i = 0; i < irclass.length; i++) {
			try {
				if (name.equals(irclass[i].getField("NAME").get(null).toString())) {
					return irclass[i];
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static Class<IRConnection>[] getAllAvailableIRConnection() {
		if(irconnections != null) {
			return irconnections;
		}
		List<Class<IRConnection>> classes = new ArrayList<Class<IRConnection>>();

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Enumeration<URL> urls = cl.getResources("bms/player/beatoraja/ir");
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				if (url.getProtocol().equals("jar")) {
					JarURLConnection jarUrlConnection = (JarURLConnection) url.openConnection();
					try (JarFile jarFile = jarUrlConnection.getJarFile()){
						Enumeration<JarEntry> jarEnum = jarFile.entries();

						while (jarEnum.hasMoreElements()) {
							JarEntry jarEntry = jarEnum.nextElement();
							String path = jarEntry.getName();
							if (path.startsWith("bms/player/beatoraja/ir/") && path.endsWith(".class")) {
								Class c = cl.loadClass("bms.player.beatoraja.ir."
										+ path.substring(path.lastIndexOf("/") + 1, path.length() - 6));
								for (Class inf : c.getInterfaces()) {
									if (inf == IRConnection.class) {
										for (Field f : c.getFields()) {
											if (f.getName().equals("NAME")) {
												classes.add(c);
											}
										}
										break;
									}
								}
							}
						}
					} catch(Throwable e) {
						Logger.getGlobal().warning("Jarファイル読み込み失敗 - " + url.toString() + " : " + e.getMessage());
					}
				}
				if (url.getProtocol().equals("file")) {
					File dir = new File(url.getPath());
					for (String path : dir.list()) {
						if (path.endsWith(".class")) {
							Class c = cl.loadClass("bms.player.beatoraja.ir." + path.substring(0, path.length() - 6));
							for (Class inf : c.getInterfaces()) {
								if (inf == IRConnection.class) {
									for (Field f : c.getFields()) {
										if (f.getName().equals("NAME")) {
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
		irconnections = classes.toArray(new Class[classes.size()]);
		return irconnections;
	}

	/**
	 * IRのホームURLを取得する
	 * @param name IR名
	 * @return IRのホームURL。存在しない場合はnull
	 */
	public static String getHomeURL(String name) {
		Class irclass = getIRConnectionClass(name);
		if(irclass != null) {
			try {
				Object result = irclass.getField("HOME").get(null);
				if(result != null) {
					return result.toString();
				}
			} catch (Throwable e) {
			}
		}
		return null;
	}

}
