package bms.player.beatoraja.skin.lr2;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import bms.player.beatoraja.skin.SkinTextImage;
import bms.player.beatoraja.skin.lr2.LR2SkinLoader.Command;

/**
 * LR2のスキン定義用csvファイルのローダー
 *
 * @author exch
 */
public class LR2FontLoader extends LR2SkinLoader {
	/**
	 * 生成するテキストイメージソース
	 */
	SkinTextImage.SkinTextImageSource textimage;
	/**
	 * lr2fontファイルパス
	 */
	Path path;

	private final boolean usecim;
	
	public LR2FontLoader(boolean usecim) {
		this.usecim = usecim;
		addCommandWord(FontCommand.values());
	}

	protected SkinTextImage.SkinTextImageSource loadFont(Path p) throws IOException {
		textimage = new SkinTextImage.SkinTextImageSource(usecim);
		this.path = p;

//		long l = System.nanoTime();
		try (Stream<String> lines = Files.lines(p, Charset.forName("MS932"))) {
			lines.forEach(line -> {
				processLine(line, null);				
			});
		};
//		System.out.println(p.toString() + " -> " + (System.nanoTime() - l));

		return textimage;
	}

	protected static int[] parseInt(String[] s) {
		int[] result = new int[22];
		for (int i = 1; i < result.length && i < s.length; i++) {
			try {
				result[i] = Integer.parseInt(s[i].replace('!', '-').replaceAll(" ", ""));
			} catch (Exception e) {

			}
		}
		return result;
	}
}

enum FontCommand implements Command<LR2FontLoader> {
	// size
	S ((loader, str) -> {
		loader.textimage.setSize(Integer.parseInt(str[1]));
	}),
	// margin
	M ((loader, str) -> {
		loader.textimage.setMargin(Integer.parseInt(str[1]));
	}),
	// texture
	T ((loader, str) -> {
		File imagefile = loader.path.getParent().resolve(str[2]).toFile();
		// System.out.println("Font image loading : " +
		// imagefile.getPath());
		if (imagefile.exists()) {
			loader.textimage.setPath(Integer.parseInt(str[1]),imagefile.getPath());
		}
	}),
	// reference
	R ((loader, str) -> {
		try {
			int[] values = LR2FontLoader.parseInt(str);
			if (loader.textimage.getPath(values[2]) != null) {
				// System.out.println("Font loaded : " + values[1]);
                for(int code : mapCode(values[1])) {
                	loader.textimage.setImage(code, values[2], values[3], values[4], values[5], values[6]);
                }
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	});
	
	private static int[] mapCode(int code) {
		int sjiscode = code;
		byte[] sjisbyte;
        if (code == 288) {
            return new int[]{0x0000301c, 0x0000ff5e};
//        } else if(code == 25318) {
//            return new int[]{0x0000e326, 0x00007dba};
        } else if (code >= 8127) {
            sjiscode = (char) (code + 49281);
            sjisbyte = new byte[2];
            sjisbyte[1] = (byte) (sjiscode & 0xff);
            sjisbyte[0] = (byte) ((sjiscode & 0xff00) >> 8);
        } else if (code >= 256) {
            sjiscode = (char) (code + 32832);
            sjisbyte = new byte[2];
            sjisbyte[1] = (byte) (sjiscode & 0xff);
            sjisbyte[0] = (byte) ((sjiscode & 0xff00) >> 8);
        } else {
			sjisbyte = new byte[1];
			sjisbyte[0] = (byte) (sjiscode & 0xff);
		}

		try {
			byte[] b = new String(sjisbyte, "Shift_JIS").getBytes("utf-16le");
			int utfcode = 0;
			for (int i = 0; i < b.length; i++) {
				utfcode |= (b[i] & 0xff) << (8 * i);
			}
			return new int[]{utfcode};
		} catch (UnsupportedEncodingException e) {
		}
		return new int[0];
	}
	
	public final BiConsumer<LR2FontLoader, String[]> function;

	private FontCommand(BiConsumer<LR2FontLoader, String[]> function) {
		this.function = function;
	}

	public void execute(LR2FontLoader loader, String[] str) {
		function.accept(loader, str);
	}

}
