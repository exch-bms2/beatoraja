package bms.player.beatoraja.skin.lr2;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.GdxRuntimeException;

import bms.player.beatoraja.skin.SkinLoader;
import bms.player.beatoraja.skin.SkinTextImage;
import bms.player.beatoraja.skin.SkinTextImage.SkinTextImageSource;
import bms.player.beatoraja.skin.lr2.LR2SkinLoader.CommandWord;

/**
 * LR2のスキン定義用csvファイルのローダー
 *
 * @author exch
 */
public class LR2FontLoader extends LR2SkinLoader {
	/**
	 * 生成するテキストイメージソース
	 */
	private SkinTextImage.SkinTextImageSource textimage;
	/**
	 * lr2fontファイルパス
	 */
	private Path path;

	private final boolean usecim;
	
	public LR2FontLoader(boolean usecim) {
		this.usecim = usecim;
		// size
		addCommandWord(new CommandWord("S") {
			@Override
			public void execute(String[] str) {
				textimage.setSize(Integer.parseInt(str[1]));
			}
		});
		// margin
		addCommandWord(new CommandWord("M") {
			@Override
			public void execute(String[] str) {
				textimage.setMargin(Integer.parseInt(str[1]));
			}
		});
		// texture
		addCommandWord(new CommandWord("T") {
			@Override
			public void execute(String[] str) {
				File imagefile = path.getParent().resolve(str[2]).toFile();
				// System.out.println("Font image loading : " +
				// imagefile.getPath());
				if (imagefile.exists()) {
					textimage.setPath(Integer.parseInt(str[1]),imagefile.getPath());
				}
			}
		});
		// reference
		addCommandWord(new CommandWord("R") {
			@Override
			public void execute(String[] str) {
				try {
					int[] values = parseInt(str);
					if (textimage.getPath(values[2]) != null) {
						// System.out.println("Font loaded : " + values[1]);
                        for(int code : mapCode(values[1])) {
                            textimage.setImage(code, values[2], values[3], values[4], values[5], values[6]);
                        }
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		});

	}

	private int[] mapCode(int code) {
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

	protected SkinTextImage.SkinTextImageSource loadFont(Path p) throws IOException {
		textimage = new SkinTextImage.SkinTextImageSource(usecim);
		this.path = p;

//		long l = System.nanoTime();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(p.toFile()), "MS932"));
		String line;
		while ((line = br.readLine()) != null) {
			processLine(line, null);
		}
		br.close();

//		System.out.println(p.toString() + " -> " + (System.nanoTime() - l));

		return textimage;
	}

	protected int[] parseInt(String[] s) {
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
