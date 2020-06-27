package bms.player.beatoraja;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;

/**
 * SQLiteデータベースアクセス用抽象クラス
 * 
 * @author exch
 */
public abstract class SQLiteDatabaseAccessor {

	/**
	 * 指定のカラムを持つテーブルを作成する。 テーブルやカラムが存在しない場合、作成する。
	 * 
	 * @param qr
	 *            QueryRunner
	 * @param tablename
	 *            テーブル名
	 * @param columns
	 *            テーブルカラム
	 * @throws SQLException
	 */
	protected void createTable(QueryRunner qr, String tablename, TableColumn[] columns) throws SQLException {
		List<TableColumn> pk = new ArrayList<TableColumn>();

		if (qr.query("SELECT * FROM sqlite_master WHERE name = ? and type='table';", new MapListHandler(), tablename)
				.size() == 0) {
			StringBuilder sql = new StringBuilder("CREATE TABLE [" + tablename + "] (");
			boolean comma = false;
			for (TableColumn column : columns) {
				sql.append(comma ? "," : "").append('[').append(column.name()).append("] ").append(column.type())
						.append(column.notnull() ? " NOT NULL" : "");
				comma = true;
				if (column.pk()) {
					pk.add(column);
				}
			}

			if (pk.size() > 0) {
				sql.append(",PRIMARY KEY(");
				comma = false;
				for (TableColumn column : pk) {
					sql.append(comma ? "," : "").append(column.name());
					comma = true;
				}
				sql.append(")");
			}
			sql.append(");");
			qr.update(sql.toString());
		}

		List<TableColumn> adds = new ArrayList<TableColumn>(Arrays.asList(columns));
		for (Map<String, Object> songcolumn : qr.query("PRAGMA table_info('" + tablename + "');",
				new MapListHandler())) {
			final String name = (String) songcolumn.get("name");
			for (int i = 0; i < adds.size(); i++) {
				if (adds.get(i).name().equals(name)) {
					adds.remove(i);
					break;
				}
			}
		}
		for (TableColumn add : adds) {
			qr.update("ALTER TABLE " + tablename + " ADD COLUMN [" + add.name() + "] " + add.type()
					+ (add.notnull() ? " NOT NULL" : ""));
		}
	}

	public interface TableColumn {

		public String name();

		public String type();

		public boolean notnull();

		public boolean pk();
	}

}
