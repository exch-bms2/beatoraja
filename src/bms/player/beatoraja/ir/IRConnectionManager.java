package bms.player.beatoraja.ir;

import java.io.File;
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

	private static final String IR_PACKAGE = "bms.player.beatoraja.ir";
	private static final String IR_RESOURCE = IR_PACKAGE.replace('.', '/');
	
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
		Class<IRConnection>[] irClasses = getAllAvailableIRConnection();
		String[] names = new String[irClasses.length];
		for(int i = 0;i < names.length;i++) {
			names[i] = getConnectionName(irClasses[i]);
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
		return getIsolatedIRConnection(name);
	}

	public static IRConnection getIsolatedIRConnection(String name) {
		return getIRConnectionClass(name) != null ? new IsolatedIRConnection(name) : null;
	}

	static IRConnection getIRConnectionInProcess(String name) {
		Class<IRConnection> irClass = getIRConnectionClass(name);
		if(irClass != null) {
			try {
				return irClass.getDeclaredConstructor().newInstance();
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
		for(Class<IRConnection> irClass : getAllAvailableIRConnection()) {
			if(name.equals(getConnectionName(irClass))) {
				return irClass;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static synchronized Class<IRConnection>[] getAllAvailableIRConnection() {
		if(irconnections != null) {
			return irconnections;
		}
		List<Class<IRConnection>> classes = new ArrayList<>();

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Enumeration<URL> urls = cl.getResources(IR_RESOURCE);
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				switch(url.getProtocol()) {
				case "jar" -> loadFromJar(cl, url, classes);
				case "file" -> loadFromDirectory(cl, url, classes);
				default -> {
				}
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		irconnections = classes.toArray(new Class[classes.size()]);
		return irconnections;
	}

	private static void loadFromJar(ClassLoader classLoader, URL url, List<Class<IRConnection>> classes) {
		try {
			JarURLConnection jarUrlConnection = (JarURLConnection) url.openConnection();
			try(JarFile jarFile = jarUrlConnection.getJarFile()) {
				Enumeration<JarEntry> jarEntries = jarFile.entries();
				while(jarEntries.hasMoreElements()) {
					String className = toClassName(jarEntries.nextElement().getName());
					if(className != null) {
						addConnectionClass(classLoader, className, classes);
					}
				}
			}
		} catch(Throwable e) {
			Logger.getGlobal().warning("Jarファイル読み込み失敗 - " + url + " : " + e.getMessage());
		}
	}

	private static void loadFromDirectory(ClassLoader classLoader, URL url, List<Class<IRConnection>> classes) {
		File dir = new File(url.getPath());
		String[] paths = dir.list();
		if(paths == null) {
			return;
		}
		for(String path : paths) {
			if(path.endsWith(".class")) {
				addConnectionClass(classLoader, IR_PACKAGE + "." + path.substring(0, path.length() - 6), classes);
			}
		}
	}

	private static String toClassName(String path) {
		if(!path.startsWith(IR_RESOURCE + "/") || !path.endsWith(".class")) {
			return null;
		}
		String simpleName = path.substring(path.lastIndexOf("/") + 1, path.length() - 6);
		return IR_PACKAGE + "." + simpleName;
	}

	@SuppressWarnings("unchecked")
	private static void addConnectionClass(ClassLoader classLoader, String className, List<Class<IRConnection>> classes) {
		try {
			Class<?> type = classLoader.loadClass(className);
			if(isAvailableConnection(type)) {
				classes.add((Class<IRConnection>) type);
			}
		} catch(Throwable e) {
			Logger.getGlobal().warning("IRConnection読み込み失敗 - " + className + " : " + e.getMessage());
		}
	}

	private static boolean isAvailableConnection(Class<?> type) {
		return type != IRConnection.class && IRConnection.class.isAssignableFrom(type) && getConnectionName(type) != null;
	}

	private static String getConnectionName(Class<?> irClass) {
		try {
			Object name = irClass.getField("NAME").get(null);
			return name != null ? name.toString() : null;
		} catch(Throwable e) {
			return null;
		}
	}

	/**
	 * IRのホームURLを取得する
	 * @param name IR名
	 * @return IRのホームURL。存在しない場合はnull
	 */
	public static String getHomeURL(String name) {
		Class<IRConnection> irClass = getIRConnectionClass(name);
		if(irClass != null) {
			try {
				Object result = irClass.getField("HOME").get(null);
				if(result != null) {
					return result.toString();
				}
			} catch (Throwable e) {
			}
		}
		return null;
	}

}
