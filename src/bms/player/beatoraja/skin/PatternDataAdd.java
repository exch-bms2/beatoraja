package bms.player.beatoraja.skin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PatternDataAdd {
	public static final int NO_VALUE = -1;
	List<String> listA = new ArrayList<String>();
	HashMap<String, Integer> map = new HashMap<String, Integer>();
	public PatternDataAdd() {
		listA.add("#Patern");
		listA.add("#Pattern");
		listA.add("#Texture");
		listA.add("#Layer");
		
		map.put("#Patern", 0);
		map.put("#Pattern", 0);
		map.put("#Texture", 1);
		map.put("#Layer", 2);
	}
	public int addType(String type) {
		for(int i=0;i<listA.size();i++) {
			if(type.equalsIgnoreCase(listA.get(i).toString())){
				return map.get(type);
			}
		}
		return NO_VALUE;
	}
	
	public boolean getType(String type) {
		for(int i=0;i<listA.size();i++) {
			if(type.equalsIgnoreCase(listA.get(i).toString())){
				return true;
			}
		}
		return false;
	}
}
