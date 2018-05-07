package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class addCharaBackGround extends addCharaPart{
	 public void EachSet(int setColor, Texture[] CharBMP, int[][] Position ) {
			setBMP = CharBMP[CharBMPIndex + setColor-1];
			image[0] = new TextureRegion(setBMP, Position[1][0], Position[1][1], Position[1][2], Position[1][3]);
	}
}
