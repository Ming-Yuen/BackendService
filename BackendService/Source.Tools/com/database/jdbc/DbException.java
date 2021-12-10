package com.database.jdbc;

import java.util.Arrays;

public class DbException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public DbException(String statement) {
		super(statement);
	}

	public DbException(Throwable statement) {
		super(statement);
	}
	
	public DbException(String statement, Throwable e) {
		super(statement, e);
	}
	
	public DbException(String statement, Object[] whereClause, Throwable arg1) {
		super(sql(statement, whereClause), arg1);
	}


	public static String sql(String statement, Object[] whereClause) {
		int start = statement.indexOf(":");
		if (start > -1) {
			statement = statement.substring(start + 1);
		}
		String containWhereClause = start == -1 ? " " + Arrays.toString(whereClause) : "";
		return statement + containWhereClause;
	}
}
