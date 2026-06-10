package bms.player.beatoraja.external;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.MainStateListener;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;

public class DiscordListener implements MainStateListener {

	private Discord discord;
	
	public DiscordListener() {
		discord = new Discord();
		discord.startup();
	}
	
	@Override
	public void update(MainState state, int status) {
		if(state instanceof MusicSelector) {
			discord.update("In Music Select Menu", "");
		}
		if(state instanceof MusicDecide) {
			discord.update("Decide Screen", "");
		}
		if(state instanceof BMSPlayer) {
			final PlayerResource resource = state.main.getPlayerResource();
			discord.update(resource.getSongdata().getFullTitle(), resource.getSongdata().getArtist(), resource.getSongdata().getMode());
		}
		if(state instanceof MusicResult) {
			discord.update("Result Screen", "");
		}
		if(state instanceof CourseResult) {
			discord.update("Course Result Screen", "");
		}
	}

	public static class Discord {
	    public final DiscordRichPresence presence = new DiscordRichPresence();

	    private final DiscordRPC lib = DiscordRPC.INSTANCE;

	    private final String APPLICATIONID = "876968973126746182"; // DISCORD APPLICATION ID   (https://discord.com/developers/applications)

	    public void startup() {
	        String steamId = "";
	        DiscordEventHandlers handlers = new DiscordEventHandlers();
	        handlers.ready = (user) -> System.out.println("Discord RPC Ready!");
	        lib.Discord_Initialize(APPLICATIONID, handlers, true, steamId);
	        DiscordRichPresence presence = new DiscordRichPresence();
	        lib.Discord_UpdatePresence(presence);
	        // in a worker thread
	        new Thread(() -> {
	            while (!Thread.currentThread().isInterrupted()) {
	                lib.Discord_RunCallbacks();
	                try {
	                    Thread.sleep(2000);
	                } catch (InterruptedException ignored) {
	                }
	            }
	        }, "RPC-Callback-Handler").start();

	    }

	    public void update(String fulltitle, String artist, int mode) {
	        update("Playing: " + mode + "Keys", fulltitle + " / " + artist);	    	
	    }

	    public void update(String state, String details) {
	        presence.details = details;
	        presence.state = state;
	        presence.startTimestamp = System.currentTimeMillis() / 1000;
	        presence.largeImageKey = "bms";
	        lib.Discord_UpdatePresence(presence);
	    }

	}
}
