package com.database.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.initialization.Global;
import com.mysql.cj.jdbc.ConnectionImpl;

public class DatabaseManager {
	
	public enum DbDriver{
		oracle, sqlserver, mysql 
	}
	
	private static DbDriver dbDriver = null;
	
	public static DbDriver getDbDriver() {
		if(dbDriver != null) {
			return dbDriver;
		}
		synchronized(DbConnection.class) {
			if(dbDriver != null) {
				return dbDriver;
			}
			for(DbDriver driver : DbDriver.values()) {
				if(DbConfig.getDriver().toLowerCase().contains(driver.name())) {
					dbDriver = driver;
					return dbDriver;
				}
			}
		}
		throw new DbException("No suitable driver");
	}
	
	public static Set<String> getContainTable(DbConnection dbConn) {
		Set<String> table = new HashSet<String>();
		try {
			DatabaseMetaData dbmd = dbConn.getDbConnection().getMetaData();
			ResultSet rs = dbmd.getTables(null, null, "%", new String[]{"TABLE"});
			while (rs.next()) {
				table.add(rs.getString("TABLE_NAME"));
			}
		}catch(SQLException e) {
			throw new DbException(e);
		}
		return table;
	}
	
	public List<String> getPrimaryKey(DbConnection dbConn, String tableName) {
		List<String> primaryKeyList = new ArrayList<String>();
		
		try {
			DatabaseMetaData dbmd = dbConn.getDbConnection().getMetaData();
			try (ResultSet primaryKeys = dbmd.getPrimaryKeys(null, null, tableName)) {
	            while (primaryKeys.next()) {
	                primaryKeyList.add(primaryKeys.getString("COLUMN_NAME"));
	                System.out.println(primaryKeys.getString("INDEX_NAME"));
	            }
	        }
		}catch(SQLException e) {
			throw new DbException(e);
		}
		return primaryKeyList;
	}
	
	public List<String> getIndexs(DbConnection dbConn, String tableName) {
		List<String> primaryKeyList = new ArrayList<String>();
		
		try {
			DatabaseMetaData dbmd = dbConn.getDbConnection().getMetaData();
			try (ResultSet primaryKeys = dbmd.getIndexInfo(dbConn.getDbConnection().getCatalog(), null, tableName, false, true)) {
	            while (primaryKeys.next()) {
	                System.out.println(primaryKeys.getString("INDEX_NAME"));
	                System.out.println(primaryKeys.getString("NON_UNIQUE"));
	            }
	        }
		}catch(SQLException e) {
			throw new DbException(e);
		}
		return primaryKeyList;
	}
	
	public String getDatabaseName(DbConnection dbConn) {
		try {
			if(dbConn.getDbConnection() instanceof ConnectionImpl) {
				return ((ConnectionImpl)dbConn.getDbConnection()).getDatabase();
			}
		}catch(Exception e) {
			new DbException(e);
		}
		return null;
	}
	
	public static class DbConfig{
		private static final String driver = Global.getConfig.getConfigValue("DatabaseClassName");
		private static final String url = Global.getConfig.getConfigValue("DatabaseConnectionUrl");
		private static final String userName = Global.getConfig.getConfigValue("DatabaseUserName");
		private static final String password = Global.getConfig.getConfigValue("DatabasePassword");
		
		public static String getDriver() {
			return driver;
		}
		
		public static String getUrl() {
			return url;
		}
		
		public static String getUsername() {
			return userName;
		}
		
		public static String getPassword() {
			return password;
		}
		
		public static String getDescription() {
			return String.join(", ", "driver : ".concat(driver), "url : ".concat(url), "user name : ".concat(userName), "password : ".concat(password));
		}
	}
}
