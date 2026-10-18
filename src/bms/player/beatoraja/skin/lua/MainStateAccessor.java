package bms.player.beatoraja.skin.lua;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import bms.player.beatoraja.MainState;
import org.luaj.vm2.LuaTable;

/**
 * 実行時にスキンからMainStateの数値などにアクセスできる関数を提供する
 */
public class MainStateAccessor implements com.badlogic.gdx.utils.Disposable {

	private final List<LuaApiExporter> exporters;

	public MainStateAccessor(MainState state) {
		this(state, () -> null);
	}

	public MainStateAccessor(MainState state, Supplier<Path> skinDirectorySupplier) {
		this(state, skinDirectorySupplier, true);
	}

	MainStateAccessor(MainState state, Supplier<Path> skinDirectorySupplier, boolean allowExternalEffects) {
		SkinLuaPathResolver pathResolver = new SkinLuaPathResolver(skinDirectorySupplier);
		exporters = List.of(
				new MainStatePropertyLuaApiExporter(state),
				new SkinFileLuaApiExporter(pathResolver, allowExternalEffects),
				new SkinRuntimeLuaApiExporter(allowExternalEffects),
				new SkinHttpLuaApiExporter(),
				new SkinAudioLuaApiExporter(state, pathResolver)
		);
	}

	public void export(LuaTable table) {
		for(LuaApiExporter exporter : exporters) {
			exporter.export(table);
		}
	}

	@Override public void dispose() {
		for (LuaApiExporter exporter : exporters) {
			if (exporter instanceof com.badlogic.gdx.utils.Disposable disposable) disposable.dispose();
		}
	}
}
