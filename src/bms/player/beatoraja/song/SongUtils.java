package bms.player.beatoraja.song;

import java.nio.file.Paths;

public class SongUtils {

	private static final int Polynomial = 0xEDB88320;

	public static String crc32(String path, String[] rootdirs, String bmspath) {
		for (String s : rootdirs) {
			if (Paths.get(s).toAbsolutePath().getParent().toString().equals(path)) {
				return "e2977170";
			}
		}

		if (path.startsWith(bmspath)) {
			path = path.substring(bmspath.length() + 1);
		}
		final int previousCrc32 = 0;
		int crc = ~previousCrc32; // same as previousCrc32 ^ 0xFFFFFFFF

		for (byte b : (path + "\\\0").getBytes()) {
			crc ^= b;
			for (int j = 0; j < 8; j++)
				if ((crc & 1) != 0)
					crc = (crc >>> 1) ^ Polynomial;
				else
					crc = crc >>> 1;
		}
		return Integer.toHexString(~crc); // same as crc ^ 0xFFFFFFFF
	}
}
