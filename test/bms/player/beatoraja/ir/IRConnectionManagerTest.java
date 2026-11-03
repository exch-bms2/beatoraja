package bms.player.beatoraja.ir;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import bms.player.beatoraja.ir.mocha.MissingNameIRConnection;
import bms.player.beatoraja.ir.mocha.dto.LoginRequest;
import bms.player.beatoraja.ir.mocha.service.IllegalBmsProvider;

/** Regression tests for IRConnection class discovery. */
public final class IRConnectionManagerTest {

	private static final String IR_RESOURCE = "bms/player/beatoraja/ir";
	private static final Class<?>[] FIXTURE_CLASSES = {
			FooIRConnection.class,
			TestIRConnectionBase.class,
			LoginRequest.class,
			IllegalBmsProvider.class,
			MissingNameIRConnection.class
	};
	private static final Set<String> FIXTURE_CLASS_NAMES = java.util.Arrays.stream(FIXTURE_CLASSES)
			.map(Class::getName)
			.collect(java.util.stream.Collectors.toUnmodifiableSet());

	private IRConnectionManagerTest() {
	}

	public static void main(String[] args) throws Exception {
		testClassNameConversion();
		testDirectoryDiscovery();
		testJarDiscovery();
	}

	private static void testClassNameConversion() throws Exception {
		assertEquals("bms.player.beatoraja.ir.FooIRConnection",
				toClassName("bms/player/beatoraja/ir/FooIRConnection.class"));
		assertEquals("bms.player.beatoraja.ir.mocha.dto.LoginRequest",
				toClassName("bms/player/beatoraja/ir/mocha/dto/LoginRequest.class"));
		assertEquals("bms.player.beatoraja.ir.mocha.service.IllegalBmsProvider",
				toClassName("bms\\player\\beatoraja\\ir\\mocha\\service\\IllegalBmsProvider.class"));
		assertNull(toClassName("bms/player/beatoraja/ir/mocha/Outer$Inner.class"));
		assertNull(toClassName("bms/player/beatoraja/ir/mocha/package-info.class"));
	}

	private static void testDirectoryDiscovery() throws Exception {
		Path root = Files.createTempDirectory("ir-connection-directory-test");
		try {
			for(Class<?> fixtureClass : FIXTURE_CLASSES) {
				copyClass(root, fixtureClass);
			}
			try(URLClassLoader classLoader = new FixtureClassLoader(new URL[] { root.toUri().toURL() },
					IRConnectionManagerTest.class.getClassLoader())) {
				assertDiscoveredConnections(loadFromDirectory(classLoader, root.resolve(IR_RESOURCE).toUri().toURL()));
			}
		} finally {
			deleteTree(root);
		}
	}

	private static void testJarDiscovery() throws Exception {
		Path archive = Files.createTempFile("ir-connection-jar-test", ".jar");
		try {
			try(JarOutputStream output = new JarOutputStream(Files.newOutputStream(archive))) {
				output.putNextEntry(new JarEntry(IR_RESOURCE + "/"));
				output.closeEntry();
				for(Class<?> fixtureClass : FIXTURE_CLASSES) {
					writeClass(output, fixtureClass);
				}
			}
			try(URLClassLoader classLoader = new FixtureClassLoader(new URL[] { archive.toUri().toURL() },
					IRConnectionManagerTest.class.getClassLoader())) {
				assertDiscoveredConnections(loadFromJar(classLoader,
						new URL("jar:" + archive.toUri().toURL() + "!/" + IR_RESOURCE + "/")));
			}
		} finally {
			Files.deleteIfExists(archive);
		}
	}

	private static String toClassName(String path) throws Exception {
		Method method = IRWorkerDiscovery.class.getDeclaredMethod("toClassName", String.class);
		method.setAccessible(true);
		return (String) method.invoke(null, path);
	}

	private static List<Class<IRConnection>> loadFromDirectory(ClassLoader classLoader, URL url) throws Exception {
		return load("loadFromDirectory", classLoader, url);
	}

	private static List<Class<IRConnection>> loadFromJar(ClassLoader classLoader, URL url) throws Exception {
		return load("loadFromJar", classLoader, url);
	}

	private static List<Class<IRConnection>> load(String methodName, ClassLoader classLoader, URL url) throws Exception {
		List<Class<IRConnection>> connections = new ArrayList<>();
		Method method = IRWorkerDiscovery.class.getDeclaredMethod(methodName, ClassLoader.class, URL.class, List.class);
		method.setAccessible(true);
		try {
			method.invoke(null, classLoader, url, connections);
		} catch (InvocationTargetException e) {
			throwAsException(e.getCause());
		}
		return connections;
	}

	private static void assertDiscoveredConnections(List<Class<IRConnection>> connections) {
		Set<String> names = connections.stream().map(Class::getName).collect(java.util.stream.Collectors.toSet());
		assertEquals(Set.of(FooIRConnection.class.getName()), names);
	}

	private static void copyClass(Path root, Class<?> type) throws IOException {
		String resource = type.getName().replace('.', '/') + ".class";
		Path destination = root.resolve(resource);
		Files.createDirectories(destination.getParent());
		try(InputStream input = type.getClassLoader().getResourceAsStream(resource)) {
			if(input == null) {
				throw new IOException("Test fixture class was not found: " + resource);
			}
			Files.copy(input, destination);
		}
	}

	private static void writeClass(JarOutputStream output, Class<?> type) throws IOException {
		String resource = type.getName().replace('.', '/') + ".class";
		output.putNextEntry(new JarEntry(resource));
		try(InputStream input = type.getClassLoader().getResourceAsStream(resource)) {
			if(input == null) {
				throw new IOException("Test fixture class was not found: " + resource);
			}
			input.transferTo(output);
		}
		output.closeEntry();
	}

	private static void deleteTree(Path root) throws IOException {
		try(var paths = Files.walk(root)) {
			for(Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}

	private static void throwAsException(Throwable throwable) throws Exception {
		if(throwable instanceof Exception exception) {
			throw exception;
		}
		if(throwable instanceof Error error) {
			throw error;
		}
		throw new RuntimeException(throwable);
	}

	private static void assertEquals(Object expected, Object actual) {
		if(!expected.equals(actual)) {
			throw new AssertionError("Expected " + expected + " but was " + actual);
		}
	}

	private static void assertNull(Object actual) {
		if(actual != null) {
			throw new AssertionError("Expected null but was " + actual);
		}
	}

	private static final class FixtureClassLoader extends URLClassLoader {

		private FixtureClassLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}

		@Override
		protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if(!FIXTURE_CLASS_NAMES.contains(name)) {
				return super.loadClass(name, resolve);
			}
			Class<?> type = findLoadedClass(name);
			if(type == null) {
				type = findClass(name);
			}
			if(resolve) {
				resolveClass(type);
			}
			return type;
		}
	}
}
