package com.database.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DbConnectionPoolManager {

	public enum ConnectionPool {
		dbcp2, jdbc, HikariCP;

		public static ConnectionPool connectionPool = ConnectionPool.dbcp2;

		public static ConnectionPool getConnectionPool() {
			return connectionPool;
		}
	}

	public static Connection getConntion() {
		switch (ConnectionPool.getConnectionPool()) {
		case dbcp2:
			return DbcpManager.getConnection();
		case jdbc:
			return JDBCManager.getConnection();
		case HikariCP:
			return HikariCPManager.getConnection();
		default:
			throw new RuntimeException();
		}
	}

	public static class HikariCPManager {
		private static DataSource datasource;

		static {
			HikariConfig config = new HikariConfig();
			config.setDriverClassName(DatabaseManager.DbConfig.getDriver());
			config.setJdbcUrl(DatabaseManager.DbConfig.getUrl());
			config.setUsername(DatabaseManager.DbConfig.getUsername());
			config.setPassword(DatabaseManager.DbConfig.getPassword());

			config.setMaximumPoolSize(10);
			config.setAutoCommit(false);
			config.addDataSourceProperty("cachePrepStmts", "true");
			config.addDataSourceProperty("prepStmtCacheSize", "250");
			config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

			datasource = new HikariDataSource(config);
		}

		public static Connection getConnection() {
			try {
				return datasource.getConnection();
			} catch (SQLException e) {
				throw new DbException(e);
			}
		}
	}

	public static class JDBCManager {
		public static Connection getConnection() {
			Connection conn = null;
			
			try {
				Class.forName(DatabaseManager.DbConfig.getDriver());
				conn = DriverManager.getConnection(DatabaseManager.DbConfig.getUrl(), DatabaseManager.DbConfig.getUsername(), DatabaseManager.DbConfig.getPassword());
				conn.setAutoCommit(false);
			} catch (ClassNotFoundException | SQLException e) {
				throw new DbException(e);
			}
			return conn;
		}
	}

	public static class DbcpManager {

		private static BasicDataSource dataSource = new BasicDataSource();

		static {
			DataSourceConfig();
		}

		private static void DataSourceConfig() {
			dataSource.setDriverClassName(DatabaseManager.DbConfig.getDriver());
			dataSource.setUrl(DatabaseManager.DbConfig.getUrl());
			dataSource.setUsername(DatabaseManager.DbConfig.getUsername());
			dataSource.setPassword(DatabaseManager.DbConfig.getPassword());
			dataSource.setInitialSize(20);
			dataSource.setMaxTotal(80);
			dataSource.setMaxIdle(40);
			dataSource.setMinIdle(20);
			dataSource.setMaxWaitMillis(6000);
			dataSource.setDefaultAutoCommit(false);
			dataSource.setEnableAutoCommitOnReturn(false);
			dataSource.setMaxOpenPreparedStatements(100);
		}

		public static Connection getConnection() {
			try {
				return dataSource.getConnection();
			} catch (SQLException e) {
				throw new DbException(e);
			}
		}
	}
	
	
}
