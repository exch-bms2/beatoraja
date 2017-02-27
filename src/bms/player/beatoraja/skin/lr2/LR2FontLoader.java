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
	 * 読み込んだTextureリスト
	 */
	private List<Texture> imagelist = new ArrayList<Texture>();
	/**
	 * 生成するテキストイメージソース
	 */
	private SkinTextImage.SkinTextImageSource textimage;
	/**
	 * lr2fontファイルパス
	 */
	private Path path;

	public LR2FontLoader() {
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
					try {
						// File cim = new File(imagefile.getPath().substring(0,
						// imagefile.getPath().lastIndexOf('.')) + ".cim");
						// if(cim.exists()) {
						// long l = System.nanoTime();
						// imagefile = cim;
						// Pixmap pixmap =
						// PixmapIO.readCIM(Gdx.files.internal(imagefile.getPath()));
						// imagelist.add(new Texture(pixmap));
						// System.out.println(imagefile.getPath() + " -> " +
						// (System.nanoTime() - l));
						// pixmap.dispose();
						// } else {
						// Pixmap pixmap = new
						// Pixmap(Gdx.files.internal(imagefile.getPath()));
						// PixmapIO.writeCIM(Gdx.files.absolute(imagefile.getAbsolutePath().substring(0,
						// imagefile.getPath().lastIndexOf('.')) + ".cim"),
						// pixmap);
						// long l = System.nanoTime();
						// imagelist.add(new Texture(pixmap));
						// System.out.println(imagefile.getPath() + " -> " +
						// (System.nanoTime() - l));
						// pixmap.dispose();
						// }
						// ファイル読み込みが最も処理時間が長い(80%)
						Pixmap pixmap = new Pixmap(Gdx.files.internal(imagefile.getPath()));
						imagelist.add(new Texture(pixmap));
						pixmap.dispose();

					} catch (GdxRuntimeException e) {
						imagelist.add(null);
						e.printStackTrace();
					}
				}
			}
		});
		// reference
		addCommandWord(new CommandWord("R") {
			@Override
			public void execute(String[] str) {
				try {
					int[] values = parseInt(str);
					if (values[2] < imagelist.size() && imagelist.get(values[2]) != null) {
						// System.out.println("Font loaded : " + values[1]);
						textimage.setImage(mapCode(values[1]), new TextureRegion(imagelist.get(values[2]), values[3],
								values[4], values[5], values[6]));
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		});

	}

	private int mapCode(int code) {
		int sjiscode = code;
		byte[] sjisbyte;
		if (code >= 128) {
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
			return utfcode;
		} catch (UnsupportedEncodingException e) {
		}
		return 0;
	}

	protected SkinTextImage.SkinTextImageSource loadFont(Path p) throws IOException {
		textimage = new SkinTextImage.SkinTextImageSource();
		this.path = p;

		long l = System.nanoTime();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(p.toFile()), "MS932"));
		String line;
		while ((line = br.readLine()) != null) {
			processLine(line, null);
		}
		br.close();

		System.out.println(p.toString() + " -> " + (System.nanoTime() - l));

		return textimage;
	}

	protected int[] parseInt(String[] s) {
		int[] result = new int[22];
		for (int i = 1; i < s.length; i++) {
			try {
				result[i] = Integer.parseInt(s[i].replace('!', '-').replaceAll(" ", ""));
			} catch (Exception e) {

			}
		}
		return result;
	}
}
