package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.math.MathUtils;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

final class SkinAudioLuaApiExporter implements LuaApiExporter {

	private final MainState state;
	private final SkinLuaPathResolver pathResolver;

	SkinAudioLuaApiExporter(MainState state, SkinLuaPathResolver pathResolver) {
		this.state = state;
		this.pathResolver = pathResolver;
	}

	@Override
	public void export(LuaTable table) {
		table.set("audio_play", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue path, LuaValue volume) {
				play(path.tojstring(), volume, false);
				return LuaBoolean.TRUE;
			}
		});
		table.set("audio_loop", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue path, LuaValue volume) {
				play(path.tojstring(), volume, true);
				return LuaBoolean.TRUE;
			}
		});
		table.set("audio_preload", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				state.main.getAudioProcessor().play(resolve(path), 0f, false);
				return LuaBoolean.TRUE;
			}
		});
		table.set("audio_stop", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				state.main.getAudioProcessor().stop(resolve(path));
				return LuaBoolean.TRUE;
			}
		});
		table.set("audio_dispose", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				state.main.getAudioProcessor().dispose(resolve(path));
				return LuaBoolean.TRUE;
			}
		});
	}

	private void play(String path, LuaValue volume, boolean loop) {
		float vol = MathUtils.clamp(volume.isnil() ? 1f : volume.tofloat(), 0.0f, 2.0f);
		state.main.getAudioProcessor().play(resolve(path), state.main.getConfig().getAudioConfig().getSystemvolume() * vol, loop);
	}

	private String resolve(LuaValue path) {
		return resolve(path.tojstring());
	}

	private String resolve(String path) {
		return pathResolver.resolve(path).toString();
	}
}
