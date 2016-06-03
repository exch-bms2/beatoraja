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
			
			if(number.getId() == 105) {
				skin.setMaxcombo(0, number);
			}
			if(number.getId() == 175) {
				skin.setMaxcombo(1, number);
			}
			
			if(number.getId() == 101) {
				skin.setScore(0, number);
			}
			if(number.getId() == 170) {
				skin.setScore(1, number);
			}
			if(number.getId() == 152) {
				skin.setScore(2, number);
			}
			if(number.getId() == 177) {
				skin.setMisscount(0, number);
			}
			if(number.getId() == 178) {
				skin.setMisscount(1, number);
			}
			
		}

		return skin;
	}

}
