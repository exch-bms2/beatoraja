package bms.player.beatoraja.audio;

import bms.player.beatoraja.Config;

import java.nio.file.Path;

public class GdxAudioDeviceDriver extends AbstractAudioDriver {

	public GdxAudioDeviceDriver(Config config) {
		super(config.getSongResourceGen());
	}

	@Override
	protected Object getKeySound(Path p) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Object getKeySound(PCM pcm) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void disposeKeySound(Object pcm) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void play(Object wav, int channel, float volume, float pitch) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void play(AudioElement id, float volume, boolean loop) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void setVolume(AudioElement id, float volume) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean isPlaying(Object id) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	protected void stop(Object id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void stop(Object id, int channel) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setVolume(Object id, int channel, float volume) {
		// TODO Auto-generated method stub
		
	}
}
