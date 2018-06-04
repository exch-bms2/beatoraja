package bms.player.beatoraja.skin;

import java.io.File;

public class chpFileFactory {
	private static chpFileFactory factory;

	private chpFileFactory() {
	};

	public static chpFileFactory instance() {
		if (factory == null)
			factory = new chpFileFactory();
		return factory;
	}

	public File getFile(File imagefile, File chpdir, int FileState) {
		File chp = null;
		if(FileState == 1) {
			chp = new File(imagefile.getPath());
		}
		else if(FileState == 2) {
			chpdir = new File(imagefile.getPath().substring(0, Math.max(imagefile.getPath().lastIndexOf('\\'), imagefile.getPath().lastIndexOf('/')) + 1));
		}
		else if(FileState == 3) {
			chp = new File(imagefile.getPath()+"/");
		}
		else {
			chpdir = new File(imagefile.getPath());
		}
		
		if(chp == null && chpdir != null) {
			File[] filename = chpdir.listFiles();
			for(int i = 0; i < filename.length; i++) {
				if (filename[i].getPath().substring(filename[i].getPath().length()-4,filename[i].getPath().length()).equalsIgnoreCase(".chp")) {
					chp = new File(filename[i].getPath());
					break;
				}
			}
		}	
		return chp ;
	}
}
