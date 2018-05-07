package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class addCharaFaceUpper extends addCharaPart {

	public void EachSet(int setColor, Texture[] CharBMP, int[][] Position ) {
		setBMP = setColor == 2 && CharBMP[CharFaceIndex + 1] != null ? CharBMP[CharFaceIndex + 1]
				: CharBMP[CharFaceIndex];
		image[0] = new TextureRegion(setBMP, charFaceUpperXywh[0], charFaceUpperXywh[1], charFaceUpperXywh[2],
				charFaceUpperXywh[3]);
	}
}
