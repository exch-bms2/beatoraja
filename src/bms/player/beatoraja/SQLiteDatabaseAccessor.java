package bms.player.beatoraja;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

/**
 * SQLiteデータベースアクセス用抽象クラス
 * 
 * @author exch
 */
public abstract class SQLiteDatabaseAccessor {

	private final ResultSetHandler<List<Column>> columnhandler = new BeanListHandler<Column>(Column.class);

	private final Table[] tables;
	
	public SQLiteDatabaseAccessor(Table... tables) {
		this.tables = tables;
	}

	/**
	 * 指定のカラムを持つテーブルを作成する。 テーブルやカラムが存在しない場合、作成する。
	 * 
	 * @param qr
	 *            QueryRunner
	 * @throws SQLException
	 */
	public void validate(QueryRunner qr) throws SQLException {
		
		for(Table table : tables) {
			List<Column> pk = new ArrayList<Column>();
			if (qr.query("SELECT * FROM sqlite_master WHERE name = ? and type='table';", new MapListHandler(), table.getName())
					.size() == 0) {
				StringBuilder sql = new StringBuilder("CREATE TABLE [" + table.getName() + "] (");
				boolean comma = false;
				for (Column column : table.getColumn()) {
					sql.append(comma ? "," : "").append('[').append(column.getName()).append("] ").append(column.getType())
							.append(column.getNotnull() == 1 ? " NOT NULL" : "").append(column.getDefaultval() != null && column.getDefaultval().length() > 0 ? " DEFAULT " + column.getDefaultval() : "");
					comma = true;
					if (column.getPk() == 1) {
						pk.add(column);
					}
				}

				if (pk.size() > 0) {
					sql.append(",PRIMARY KEY(");
					comma = false;
					for (Column column : pk) {
						sql.append(comma ? "," : "").append(column.getName());
						comma = true;
					}
					sql.append(")");
				}
				sql.append(");");
				qr.update(sql.toString());
			}

			List<Column> adds = new ArrayList<Column>(Arrays.asList(table.getColumn()));
			for (Column songcolumn : qr.query("PRAGMA table_info('" + table.getName() + "');",
					columnhandler)) {
				final String name = (String) songcolumn.getName();
				for (int i = 0; i < adds.size(); i++) {
					if (adds.get(i).getName().equals(name)) {
						adds.remove(i);
						break;
					}
				}
			}
			for (Column add : adds) {
				qr.update("ALTER TABLE " + table.getName() + " ADD COLUMN [" + add.getName() + "] " + add.getType()
						+ (add.getNotnull() == 1 ? " NOT NULL" : "") + (add.getDefaultval() != null && add.getDefaultval().length() > 0 ? " DEFAULT " + add.getDefaultval() : ""));
			}			
		}

	}

	protected void insert(QueryRunner qr, String tablename,
			Object entity) throws SQLException {
		insert(qr, null, tablename, entity);
	}

	protected void insert(QueryRunner qr, Connection con, String tablename,
			Object entity) throws SQLException {
		Column[] columns = null;
		for(Table table : tables) {
			if(table.getName().equals(tablename)) {
				columns = table.getColumn();
				break;
			}
		}
		if(columns == null) {
			return;
		}
		
		StringBuilder sql = new StringBuilder("INSERT OR REPLACE INTO " + tablename + " (");
		boolean comma = false;
		for (Column column : columns) {
			sql.append(comma ? "," : "").append(column.getName());
			comma = true;
		}
		sql.append(") VALUES(");

		Object[] params = new Object[columns.length];
		comma = false;
		for (int i = 0; i < columns.length; i++) {
			sql.append(comma ? ",?" : "?");
			comma = true;

			PropertyDescriptor pd;
			try {
				pd = new PropertyDescriptor(columns[i].getName(), entity.getClass());
				Method getterMethod = pd.getReadMethod();
				params[i] = getterMethod.invoke(entity);
			} catch (IntrospectionException | ReflectiveOperationException | IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		sql.append(");");

		if(con != null) {
			qr.update(con, sql.toString(), params);
		} else {
			qr.update(sql.toString(), params);			
		}
	}
	
	/**
	 * SQLiteテーブル
	 * 
	 * @author exch
	 */
	public static class Table {

		/**
		 * テーブル名
		 */
		private String name;
		
		/**
		 * カラム
		 */
		private Column[] column;
		
		public Table(String name, Column... column) {
			this.name = name;
			this.column = column;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Column[] getColumn() {
			return column;
		}

		public void setColumn(Column[] column) {
			this.column = column;
		}
	}

	/**
	 * SQLiteカラム
	 * 
	 * @author exch
	 */
	public static class Column {

		/**
		 * カラム名
		 */
		private String name;
		/**
		 * 値の型式
		 */
		private String type;

		/**
		 * NOT NULL = 1
		 */
		private int notnull;

		/**
		 * PRIMAL KEY = 1
		 */
		private int pk;
		
		private String defaultval;
		
		public Column() {
			
		}

		public Column(String name, String type) {
			this(name, type, 0, 0);
		}

		public Column(String name, String type, int notnull, int pk) {
			this.name = name;
			this.type = type;
			this.notnull = notnull;
			this.pk = pk;
		}
		
		public Column(String name, String type, int notnull, int pk, String defaultval) {
			this.name = name;
			this.type = type;
			this.notnull = notnull;
			this.pk = pk;
			this.setDefaultval(defaultval);
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}
		
		public int getNotnull() {
			return notnull;
		}
		
		public void setNotnull(int notnull) {
			this.notnull = notnull;
		}
		
		public int getPk() {
			return pk;
		}
		
		public void setPk(int pk) {
			this.pk = pk;
		}

		public String getDefaultval() {
			return defaultval;
		}

		public void setDefaultval(String defaultval) {
			this.defaultval = defaultval;
		}
	}	
}
