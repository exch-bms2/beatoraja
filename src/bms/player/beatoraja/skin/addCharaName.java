package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class addCharaName extends addCharaPart {
	 public void EachSet(int setColor, Texture[] CharBMP, int[][] Position ) {
			setBMP = CharBMP[CharBMPIndex + setColor - 1];
			image[0] = new TextureRegion(setBMP, Position[0][0], Position[0][1], Position[0][2], Position[0][3]);
	}
}
