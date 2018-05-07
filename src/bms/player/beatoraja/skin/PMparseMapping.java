package bms.player.beatoraja.skin;

import java.util.HashMap;

public class PMparseMapping {
	HashMap<String, Integer> map = new HashMap<String, Integer>();
	public PMparseMapping(int CharBMPIndex, int CharTexIndex, int CharFaceIndex, int SelectCGIndex) {
		map.put("#CharBMP", CharBMPIndex);
		map.put("#CharBMP2P", CharBMPIndex+1);
		map.put("#CharTex", CharTexIndex);
		map.put("#CharTex2P", CharTexIndex+1);
		map.put("#CharFace", CharFaceIndex);
		map.put("#CharFace2P", CharFaceIndex+1);
		map.put("#SelectCG", SelectCGIndex);
		map.put("#SelectCG2P", SelectCGIndex+1);
	}
	public int getIndex(String CharString) {
		return map.get(CharString);
	}
}
