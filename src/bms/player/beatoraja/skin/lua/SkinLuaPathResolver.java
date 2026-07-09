package bms.player.beatoraja.skin.lua;

import java.nio.file.Path;
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
		return resolved;
	}
}
