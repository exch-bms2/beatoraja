package bms.table;

import java.util.*;

/**
 * 表管理用クラス
 * 
 * @author exch
 */
public class BMSTableManager {
	/**
	 * 表リスト
	 */
	private List<BMSTable> tableList = new ArrayList<BMSTable>();

	private List<BMSTableManagerListener> listener = new ArrayList<BMSTableManagerListener>();

	private Map<String, List<DifficultyTableElement>> userList = new HashMap<String, List<DifficultyTableElement>>();

	private Map<String, String> memoMap = new HashMap<String, String>();

	public BMSTableManager() {
	}

	public void addListener(
			BMSTableManagerListener l) {
		listener.add(l);
	}

	public void fireModelChanged() {
		for (int i = 0; i < listener.size(); i++) {
			listener.get(i).modelChanged();
		}
	}

	/**
	 * 難易度表を追加する
	 * 
	 * @param dt
	 *            追加する難易度表
	 */
	public void addBMSTable(BMSTable dt) {
		tableList.add(dt);
		this.fireModelChanged();
	}

	/**
	 * 難易度表を削除する
	 * 
	 * @param dt
	 *            削除する難易度表
	 */
	public void removeBMSTable(BMSTable dt) {
		tableList.remove(dt);
		this.fireModelChanged();
	}

	/**
	 * 難易度表リストを取得する
	 * 
	 * @return 難易度表リスト
	 */
	public BMSTable[] getBMSTables() {
		return tableList.toArray(new BMSTable[0]);
	}

	public List<BMSTable> getTableList() {
		return tableList;
	}

	public Map<String, List<DifficultyTableElement>> getUserList() {
		return userList;
	}
	
	public void setUserList(Map<String, List<DifficultyTableElement>> userList) {
		this.userList = userList;
	}
	
	public Map<String, String> getMemoMap() {
		return memoMap;
	}
	
	public void setMemoMap(Map<String, String> memoMap) {
		this.memoMap = memoMap;
	}
	
	public List<DifficultyTableElement> getUserDifficultyTableElements(String name) {
		if(userList.get(name) == null) {
			userList.put(name, new ArrayList<DifficultyTableElement>());
		}
		return userList.get(name);
	}

	public void setTableList(List<BMSTable> tableList) {
		this.tableList = tableList;
	}

	public void clearAllTableElements() {
		for (int i = 0; i < tableList.size(); i++) {
			tableList.get(i).removeAllElements();
		}
	}
}
