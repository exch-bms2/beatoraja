package bms.player.beatoraja.video;

import com.badlogic.gdx.utils.Array;

public enum VideoFormat {

	MPEG4(".mp4",".m4v"),
	WMV(".wmv"),
	WEBM(".webm"),
	MPEG(".mpg",".mpeg"),
	MPEG1VIDEO(".m1v"),
	MPEG2VIDEO(".m2v"),
	AVI(".avi");
	
	private final String[] extensions;
	
	private static VideoFormat[] values = VideoFormat.values();
	private static String[] allExtensions = null;
	
	private VideoFormat(String... extensions) {
		this.extensions = extensions;
	}
	
	public static VideoFormat getFormat(String filename) {
		for(VideoFormat format : values) {
			for(String extension : format.extensions) {
				if(filename.toLowerCase().endsWith(extension)) {
					return format;
				}
			}
		}
		return null;
	}
	
	public static String[] getAllExtensions() {
		if(allExtensions == null) {
			Array<String> l = new Array();
			for(VideoFormat format : values) {
				for(String extension : format.extensions) {
					if(!l.contains(extension, false)) {
						l.add(extension.substring(1));
					}
				}			
			}
			allExtensions = l.toArray(String.class);
		}
		return allExtensions;
	}
}
