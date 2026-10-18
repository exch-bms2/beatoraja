package bms.player.beatoraja.skin.lua;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.function.Supplier;

import org.luaj.vm2.LuaError;

final class SkinLuaPathResolver {

	private final Supplier<Path> skinDirectorySupplier;

	SkinLuaPathResolver(Supplier<Path> skinDirectorySupplier) {
		this.skinDirectorySupplier = skinDirectorySupplier;
	}

	Path resolve(String pathText) {
		Path root = skinDirectorySupplier.get();
		if(root == null) {
			throw new LuaError("skin directory is not specified");
		}
		root = root.toAbsolutePath().normalize();
		Path path = Path.of(pathText);
		Path resolved;
		if(path.isAbsolute()) {
			resolved = path.normalize();
		} else {
			Path workingDirectoryPath = path.toAbsolutePath().normalize();
			resolved = workingDirectoryPath.startsWith(root)
					? workingDirectoryPath
					: root.resolve(path).toAbsolutePath().normalize();
		}
		if(!resolved.startsWith(root)) {
			throw new LuaError("skin file access denied: " + pathText);
		}
		// Check every component, including dangling links and links below missing directories.
		Path component = root;
		if (Files.isSymbolicLink(component)) throw new LuaError("skin symbolic link access denied");
		for (Path name : root.relativize(resolved)) {
			component = component.resolve(name);
			if (Files.isSymbolicLink(component)) throw new LuaError("skin symbolic link access denied");
		}
		return resolved;
	}
}
