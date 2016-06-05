package bms.player.beatoraja.skin;

import java.io.File;
import java.io.IOException;

import bms.player.beatoraja.result.MusicResultSkin;

public class LR2ResultSkinLoader extends LR2SkinLoader {

	private MusicResultSkin skin;
	
	public MusicResultSkin loadResultSkin(File f, int[] option) throws IOException {

		skin = new MusicResultSkin();

		this.loadSkin(skin, f, option);
		
		System.out.println("result skin objects : " + skin.getSkinPart().length);
		System.out.println("result skin numbers : " + skin.getSkinNumbers().length);
		for(SkinNumber number : skin.getSkinNumbers()) {
//			System.out.println("result skin number id : " + number.getId());
			if(number.getId() == 106) {
				skin.setTotalnotes(number);
			}
			
			if(number.getId() == 152) {
				skin.setScore(2, number);
			}
		}

		return skin;
	}

}
