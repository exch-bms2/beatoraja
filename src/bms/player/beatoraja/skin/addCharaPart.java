package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

abstract public class addCharaPart {

	TextureRegion[] image = new TextureRegion[1];
	SkinImage PMcharaPart = null;
	Texture setBMP;
	int[] charFaceUpperXywh = { 0, 0, 256, 256 };
	int[] charFaceAllXywh = { 320, 0, 320, 480 };
	int CharBMPIndex = 0;
	int CharTexIndex = 2;
	int CharFaceIndex = 4;
	int SelectCGIndex = 6;
	public addCharaPart() {
		
	}
	public SkinImage addChara(Skin skin, int setColor, Texture[] CharBMP,int[][] Position) {;
		image = new TextureRegion[1];
		EachSet(setColor,CharBMP,Position);
		PMcharaPart = new SkinImage(image, 0, 0);
		skin.add(PMcharaPart);
		return PMcharaPart;
	}
	
	abstract public void EachSet(int setColor, Texture[] CharBMP, int[][] Position);
}
