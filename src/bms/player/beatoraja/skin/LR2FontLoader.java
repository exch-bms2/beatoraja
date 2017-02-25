package bms.player.beatoraja.skin;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.play.bga.FFmpegProcessor;
import bms.player.beatoraja.play.bga.MovieProcessor;
import bms.player.beatoraja.play.bga.VLCMovieProcessor;
import bms.player.beatoraja.skin.LR2SkinHeader.CustomFile;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * LR2のスキン定義用csvファイルのローダー
 *
 * @author exch
 */
public class LR2FontLoader extends LR2SkinLoader {

    private List<Texture> imagelist = new ArrayList<Texture>();

    private SkinTextImage.SkinTextImageSource textimage;

    private Path path;

    public LR2FontLoader() {

        addCommandWord(new CommandWord("S") {
            @Override
            public void execute(String[] str) {
                textimage.setSize(Integer.parseInt(str[1]));
            }
        });
        addCommandWord(new CommandWord("M") {
            @Override
            public void execute(String[] str) {
                textimage.setMargin(Integer.parseInt(str[1]));
            }
        });
        addCommandWord(new CommandWord("T") {
            @Override
            public void execute(String[] str) {
                File imagefile = path.getParent().resolve(str[2]).toFile();
//                System.out.println("Font image loading : " + imagefile.getPath());
                if (imagefile.exists()) {
                    try {
                        imagelist.add(new Texture(Gdx.files.internal(imagefile.getPath())));
                    } catch (GdxRuntimeException e) {
                        imagelist.add(null);
                        e.printStackTrace();
                    }
                }
            }
        });

        addCommandWord(new CommandWord("R") {
            @Override
            public void execute(String[] str) {
                try {
                    int[] values = parseInt(str);
                    if (values[2] < imagelist.size() && imagelist.get(values[2]) != null) {
//                        System.out.println("Font loaded : " + values[1]);
                        textimage.getImages()[values[1]] = new TextureRegion(imagelist.get(values[2]),values[3], values[4], values[5], values[6]);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });

    }

    protected SkinTextImage.SkinTextImageSource loadFont(Path p) throws IOException {
        textimage = new SkinTextImage.SkinTextImageSource();
        this.path = p;

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(p.toFile()), "MS932"));
        String line;
        while ((line = br.readLine()) != null) {
            processLine(line, null);
        }
        br.close();

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
