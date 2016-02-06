package bms.table;

import java.util.*;

/**
 * 表の要素
 * 
 * @author exch
 */
public abstract class BMSTableElement {
	
	private Map<String, Object> values = new HashMap<String, Object>();

	/**
	 * タイトル
	 */
	private String title = "";
	/**
	 * MD5
	 */
	private String md5 = "";
	/**
	 * アーティスト
	 */
	private String artist = "";
	
	public BMSTableElement() {
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getURL1() {
		return (String)values.get("url");
	}

	public void setURL1(String url1) {
		values.put("url", url1);
	}

	public String getURL1name() {
		return artist;
	}

	public void setURL1name(String url1name) {
		this.artist = url1name;
	}

	public String getHash() {
		return md5;
	}

	public void setHash(String hash) {
		this.md5 = hash;
	}

	public List<String> getParentHash() {
		Object o = values.get("org_md5");
		if(o instanceof String) {
			List<String> result = new ArrayList<String>();
			result.add((String) o);
			return result;
		}
		if(o instanceof List) {
			return (List<String>) o;
		}
		return null;
	}
	
	public void setParentHash(List<String> hashes) {
		if(hashes == null || hashes.size() == 0) {
			values.remove("org_md5");
		} else {
			values.put("org_md5", hashes);
		}		
	}
	
	public Map<String, Object> getValues() {
		values.put("title", getTitle());
		values.put("md5", getHash());
		values.put("artist", getURL1name());
		return values;
	}

	public void setValues(Map<String, Object> values) {
		this.values = values;
		title = (String) values.get("title");
		md5 = (String) values.get("md5");
		artist= (String) values.get("artist");
	}	
}
