package bms.player.beatoraja;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
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

	protected void insertOrReplace(QueryRunner qr, Connection con, String tablename, TableColumn[] columns,
			Object entity) throws IntrospectionException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, SQLException {
		StringBuilder sql = new StringBuilder("INSERT OR REPLACE INTO " + tablename + " (");
		boolean comma = false;
		for (TableColumn column : columns) {
			sql.append(comma ? "," : "").append(column.name());
			comma = true;
		}
		sql.append(") VALUES(");

		Object[] params = new Object[columns.length];
		comma = false;
		for (int i = 0; i < columns.length; i++) {
			sql.append(comma ? ",?" : "?");
			comma = true;

			PropertyDescriptor pd = new PropertyDescriptor(columns[i].name(), entity.getClass());
			Method getterMethod = pd.getReadMethod();
			params[i] = getterMethod.invoke(entity);
		}
		sql.append(");");

		qr.update(con, sql.toString(), params);
	}

	public interface TableColumn {

		public String name();

		public String type();

		public boolean notnull();

		public boolean pk();
	}

}
