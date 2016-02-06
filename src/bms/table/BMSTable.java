package bms.table;

import java.util.*;

/**
 * 表
 * 
 * @author exch
 */
public abstract class BMSTable<T> {
	
	/**
	 * 票の名称
	 */
	private String name = "";
	/**
	 * 難易度表のマーク
	 */
	private String id = "";
	/**
	 * 難易度表のタグ付加時のマーク
	 */
	private String tag = "";
	/**
	 * 難易度表のソースURL
	 */
	private String sourceURL = "";
	/**
	 * 難易度表のヘッダURL
	 */
	private String headURL = "";
	/**
	 * 難易度表のデータURL
	 */
	private String[] dataURL = new String[0];
	
	private boolean autoUpdate = true;
	/**
	 * 統合時のレベルマッピング.。key:元のレベル表記-value:統合時のレベル表記に変換する。
	 * value=""の場合、そのレベルは統合時に除外する。nullの場合は元のレベル表記=統合時のレベル表記とする。
	 */
	Map<String, Map<String, String>> mergeConfigurations = new HashMap<String, Map<String, String>>();

	/**
	 * 最終更新時間(ms)。終了時に保存しない
	 */
	private long lastupdate = 0;
	/**
	 * 表固有の属性値-名称のマップ
	 */
	private Map<String, String> attrmap = new HashMap<String, String>();
	/**
	 * 表の要素
	 */
	private List<T> models = new ArrayList<T>();
	
	private boolean editable = false;
	/**
	 * アクセス回数
	 */
	private int accessCount = 0;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getID() {
		return id;
	}

	public void setID(String id) {
		this.id = id;
	}

	public String[] getDataURL() {
		return dataURL;
	}

	public void setDataURL(String[] datas) {
		this.dataURL = datas;
	}
	
	public Map<String, Map<String, String>> getMergeConfigurations() {
		return mergeConfigurations;
	}

	public void setMergeConfigurations(
			Map<String, Map<String, String>> mergeConfigurations) {
		this.mergeConfigurations = mergeConfigurations;
	}


	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getSourceURL() {
		return sourceURL;
	}

	public void setSourceURL(String sourceURL) {
		this.sourceURL = sourceURL;
	}
	
	public List<T> getModels() {
		return models;
	}

	public void setModels(List<T> models) {
		this.models.clear();
		this.models.addAll(models);
	}

	public void addElement(T dte) {
		models.add(dte);
		lastupdate = System.currentTimeMillis();
	}

	public void removeElement(T dte) {
		models.remove(dte);
	}

	public void removeAllElements() {
		models.clear();
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public Map<String, String> getAttrmap() {
		return attrmap;
	}

	public void setAttrmap(Map<String, String> attrmap) {
		this.attrmap = attrmap;
	}

	public String getHeadURL() {
		return headURL;
	}

	public void setHeadURL(String headURL) {
		this.headURL = headURL;
	}

	public long getLastupdate() {
		return lastupdate;
	}

	public int getAccessCount() {
		return accessCount;
	}

	public void setAccessCount(int accessCount) {
		this.accessCount = accessCount;
	}

	public boolean isAutoUpdate() {
		return autoUpdate;
	}

	public void setAutoUpdate(boolean autoupdate) {
		this.autoUpdate = autoupdate;
	}
}
