package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class addCharaFaceAll extends addCharaPart{
	 public void EachSet(int setColor, Texture[] CharBMP, int[][] Position ) {
		setBMP = setColor == 2 && CharBMP[CharFaceIndex + 1] != null ? CharBMP[CharFaceIndex + 1] : CharBMP[CharFaceIndex];
		image[0] = new TextureRegion(setBMP, charFaceAllXywh[0], charFaceAllXywh[1], charFaceAllXywh[2], charFaceAllXywh[3]);
	}
}
