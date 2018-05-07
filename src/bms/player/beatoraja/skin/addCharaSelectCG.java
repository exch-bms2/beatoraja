package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class addCharaSelectCG extends addCharaPart{
	 public void EachSet(int setColor, Texture[] CharBMP, int[][] Position ) {
		 setBMP = setColor == 2 && CharBMP[SelectCGIndex + 1] != null ? CharBMP[SelectCGIndex + 1] : CharBMP[SelectCGIndex];
		image[0] = new TextureRegion(setBMP, 0, 0, setBMP.getWidth(), setBMP.getHeight());
	}

}
