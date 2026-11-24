package bms.player.beatoraja.ir;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.stream.Stream;

/** Reflection-based legacy plugin discovery. Entry points are restricted to the worker JVM. */
final class IRWorkerDiscovery {

	private static boolean worker;

	static void enterWorker() { worker = true; }
	static boolean isWorker() { return worker; }

	private static void requireWorker() {
		if (!worker) throw new IllegalStateException("IR implementation classes are only available inside the IR worker");
	}

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
		requireWorker();
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
		requireWorker();
		if(irconnections != null) {
			return irconnections;
		}
		List<Class<IRConnection>> classes = new ArrayList<>();

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			for (Path file : IRProcessSupport.classpaths().plugins()) {
				if (Files.isDirectory(file)) {
					Path directory = file.resolve(IR_RESOURCE);
					if (Files.isDirectory(directory)) loadFromDirectory(cl, directory.toUri().toURL(), classes);
				} else if (Files.isRegularFile(file)) {
					loadFromJar(cl, file, classes);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		irconnections = classes.toArray(new Class[classes.size()]);
		return irconnections;
	}

	/**
	 * Open the archive by file path. A "jar:" URL treats "!/" as the archive separator, so a
	 * directory whose name ends with '!' made the archive unreadable (#539).
	 */
	private static void loadFromJar(ClassLoader classLoader, Path jar, List<Class<IRConnection>> classes) {
		try {
			try(JarFile jarFile = new JarFile(jar.toFile())) {
				Enumeration<JarEntry> jarEntries = jarFile.entries();
				while(jarEntries.hasMoreElements()) {
					String className = toClassName(jarEntries.nextElement().getName());
					if(className != null) {
						addConnectionClass(classLoader, className, classes);
					}
				}
			}
		} catch(Throwable e) {
			Logger.getGlobal().warning("Jarファイル読み込み失敗 - " + jar + " : " + e.getMessage());
		}
	}

	private static void loadFromDirectory(ClassLoader classLoader, URL url, List<Class<IRConnection>> classes) {
		try {
			Path directory = Path.of(url.toURI());
			try(Stream<Path> paths = Files.walk(directory)) {
				paths.filter(Files::isRegularFile)
						.map(directory::relativize)
						.map(Path::toString)
						.map(path -> toClassName(IR_RESOURCE + "/" + path))
						.filter(className -> className != null)
						.forEach(className -> addConnectionClass(classLoader, className, classes));
			}
		} catch (IOException | URISyntaxException e) {
			Logger.getGlobal().warning("ディレクトリ読み込み失敗 - " + url + " : " + e.getMessage());
		}
	}

	private static String toClassName(String path) {
		String normalizedPath = path.replace('\\', '/');
		if(!normalizedPath.startsWith(IR_RESOURCE + "/") || !normalizedPath.endsWith(".class")) {
			return null;
		}
		String className = normalizedPath.substring(0, normalizedPath.length() - 6).replace('/', '.');
		String simpleName = className.substring(className.lastIndexOf('.') + 1);
		if(simpleName.equals("module-info") || simpleName.equals("package-info") || simpleName.indexOf('$') >= 0) {
			return null;
		}
		return className;
	}

	@SuppressWarnings("unchecked")
	private static void addConnectionClass(ClassLoader classLoader, String className, List<Class<IRConnection>> classes) {
		try {
			Class<?> type = classLoader.loadClass(className);
			if(isAvailableConnection(type)) {
				if (!classes.contains(type)) classes.add((Class<IRConnection>) type);
			}
		} catch(Throwable e) {
			Logger.getGlobal().warning("IRConnection読み込み失敗 - " + className + " : " + e.getMessage());
		}
	}

	private static boolean isAvailableConnection(Class<?> type) {
		return type != IRConnection.class && IRConnection.class.isAssignableFrom(type)
				&& !java.lang.reflect.Modifier.isAbstract(type.getModifiers()) && getConnectionName(type) != null;
	}

	private static String getConnectionName(Class<?> irClass) {
		return getStaticString(irClass, "NAME");
	}

	/**
	 * Read a static string such as NAME or HOME. Besides a Java static field (also Kotlin
	 * {@code const val} / {@code @JvmField}), accept a static getter (Kotlin {@code @JvmStatic}) and
	 * a getter on the Kotlin companion object, where a plain {@code val} is not a public field (#800).
	 */
	static String getStaticString(Class<?> irClass, String property) {
		String getter = "get" + property;
		try {
			return string(irClass.getField(property).get(null));
		} catch(Throwable ignored) {
		}
		try {
			Method method = irClass.getMethod(getter);
			if (Modifier.isStatic(method.getModifiers())) return string(method.invoke(null));
		} catch(Throwable ignored) {
		}
		try {
			Object companion = irClass.getField("Companion").get(null);
			if (companion != null) return string(companion.getClass().getMethod(getter).invoke(companion));
		} catch(Throwable ignored) {
		}
		return null;
	}

	private static String string(Object value) {
		return value != null ? value.toString() : null;
	}

	/**
	 * IRのホームURLを取得する
	 * @param name IR名
	 * @return IRのホームURL。存在しない場合はnull
	 */
	public static String getHomeURL(String name) {
		Class<IRConnection> irClass = getIRConnectionClass(name);
		return irClass != null ? getStaticString(irClass, "HOME") : null;
	}

}
