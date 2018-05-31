package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.Texture;

public class addCharaFactory {
	public static final int BACKGROUND = 1;
	public static final int NAME = 2;
	public static final int FACE_UPPER = 3;
	public static final int FACE_ALL = 4;
	public static final int SELECT_CG = 5;
	
	private static addCharaFactory factory;
	private addCharaFactory() {};
	public static addCharaFactory instance() {
		if(factory == null) factory = new addCharaFactory();
		return factory;
	}
	public SkinImage getAddChara(Skin skin, int color, Texture[] CharBMP,int[][] Position, int type) {
		SkinImage skinImage = null;
		switch(type) {
		case BACKGROUND : skinImage =  new addCharaBackGround().addChara(skin, color, CharBMP, Position); break;
		case NAME : skinImage = new addCharaName().addChara(skin, color, CharBMP, Position); break;
		case FACE_UPPER : skinImage = new addCharaFaceUpper().addChara(skin, color, CharBMP, Position); break;
		case FACE_ALL : skinImage = new addCharaFaceAll().addChara(skin, color, CharBMP, Position); break;
		case SELECT_CG : skinImage = new addCharaSelectCG().addChara(skin, color, CharBMP, Position); break;
		}

		return skinImage;
	}
}
