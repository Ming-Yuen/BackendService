package com.database.jdbc;

import com.initialization.Global;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class DatabaseHelper{

	private static final Integer sqlMaxLogResultSize = Integer.valueOf(10000);

	private static final Integer sqlMaxUpdateSize = Integer.valueOf(1000);
	

	public ArrayList<ArrayList<Object>> query(DbConnection dbConn, String sqlStatement) {
        return query(dbConn, sqlStatement, null, false);
    }
    
    public static ArrayList<ArrayList<Object>> query(DbConnection dbConn, String sqlStatement, List<Object> whereCluase) {
        return query(dbConn, sqlStatement, whereCluase.toArray(), false);
    }
    
    public static ArrayList<ArrayList<Object>> query(DbConnection dbConn, String sqlStatement, Object[] whereCluase) {
        return query(dbConn, sqlStatement, whereCluase, false);
    }
    
    public static ArrayList<ArrayList<Object>> queryIncludeColumnName(DbConnection dbConn, String sqlStatement) {
        return query(dbConn, sqlStatement, null, true);
    }
    
    public static ArrayList<ArrayList<Object>> queryIncludeColumnName(DbConnection dbConn, String sqlStatement, Object[] whereCluase) {
        return query(dbConn, sqlStatement, whereCluase, true);
    }
    
    private static ArrayList<ArrayList<Object>> query(DbConnection dbConn, String sqlStatement, Object[] whereCluase, boolean includeColName) {
        ArrayList<ArrayList<Object>> resultSetList = new ArrayList<ArrayList<Object>>();
        PreparedStatement stmt = null;
        ResultSet resuleset = null;
        try {
            Date queryStartTime = new Date();
            stmt = dbConn.getDbConnection().prepareStatement(sqlStatement);
            databaseSetValue(stmt, whereCluase);
            resuleset = stmt.executeQuery();
            ResultSetMetaData rsmd = resuleset.getMetaData();
            boolean isFirstRow = true;
            while (resuleset.next()) {
                ArrayList<Object> colNameRow = new ArrayList<Object>();
                ArrayList<Object> row = new ArrayList<Object>();
                for (int index = 1; index <= rsmd.getColumnCount(); index++) {
                    if (includeColName && isFirstRow) {
                        colNameRow.add(rsmd.getColumnLabel(index));
                        isFirstRow = false;
                    }
                    row.add(resuleset.getObject(index));
                }
                if (includeColName && isFirstRow) {
                    resultSetList.add(colNameRow);
                }
                resultSetList.add(row);
            }
            sqlSelectLog(stmt, resultSetList, new Date().getTime() - queryStartTime.getTime());
        }catch (SQLException e) {
            Global.getLogger.debug(DatabaseHelper.class.getName(), sqlStatement + (whereCluase == null ? "" : ", " + Arrays.toString(whereCluase)));
            throw new DbException(e);
        }finally {
        	dbCloseable(stmt, resuleset);
        };
        return resultSetList;
    }
    
    public static void databaseSetValue(PreparedStatement stmt, Object[] whereCluase) throws SQLException {
        if (whereCluase != null && whereCluase.length > 0) {
            for (int index = 1; index <= whereCluase.length; ++index) {
                Object value = whereCluase[index - 1];
                if (value instanceof Date) {
                    stmt.setTimestamp(index, new Timestamp(((Date)value).getTime()));
                }else {
                    stmt.setObject(index, value);
                }
            }
            stmt.addBatch();
        }
    }
    
    public static int update(Connection dbConn, String sqlStatement, Object[] whereCluaseArr) {
        PreparedStatement stmt = null;
        Date queryStartTime = new Date();
        try {
            stmt = dbConn.prepareStatement(sqlStatement);
            if (whereCluaseArr != null) {
                databaseSetValue(stmt, whereCluaseArr);
            }
            int row = stmt.executeUpdate();
            sqlUpdateLog(stmt, row, new Date().getTime() - queryStartTime.getTime());
            return row;
        }catch (SQLException e) {
            throw new DbException(e);
        }finally {
        	dbCloseable(stmt);
        }
    }
    
    public static int update(Connection dbConn, String[] sqlStatementArr, Object[][] whereCluaseArr) {
        PreparedStatement stmt = null;
        int ttlUpdateRows = 0;
        Date queryStartTime = new Date();
        try {
            for (int index = 0; index <= sqlStatementArr.length; ++index) {
                queryStartTime = new Date();
                final String sql = sqlStatementArr[index];
                stmt = dbConn.prepareStatement(sql);
                if (whereCluaseArr != null) {
                    databaseSetValue(stmt, whereCluaseArr[index]);
                }
                stmt.addBatch();
                if (index % DatabaseHelper.sqlMaxUpdateSize == 0) {
                    ttlUpdateRows += Arrays.stream(stmt.executeBatch()).sum();
                    stmt.clearBatch();
                }
            }
            ttlUpdateRows += Arrays.stream(stmt.executeBatch()).sum();
            sqlUpdateLog(stmt, ttlUpdateRows, new Date().getTime() - queryStartTime.getTime());
            return ttlUpdateRows;
        }catch (SQLException e) {
            throw new DbException(e);
        }finally {
        	dbCloseable(stmt);
        }
    }
    
    public static <T> T getValue(ArrayList<ArrayList<Object>> dbResultList, int row, int column) {
        if (row > dbResultList.size()) {
            throw new IndexOutOfBoundsException("dbResultList max row of " + dbResultList.size() + ", cannot get " + row + " index row");
        }
        Object dbValue = dbResultList.get(row).get(column);
        if (dbValue == null) {
            return null;
        }
        return (T)dbValue;
    }
    
    private static void sqlSelectLog(PreparedStatement stmt, ArrayList<ArrayList<Object>> resultSetList, long processTime) {
        sqlUpdateLog(stmt, resultSetList.size(), processTime);
        if (!resultSetList.isEmpty()) {
            Global.getLogger.info(String.join(",", resultSetList.stream().map(x -> x.toString()).collect(Collectors.toList())));
        }
    }
    
    public static void sqlUpdateLog(final Statement stmt, final int updateRows, final long processTime) {
        String sqlStatement = stmt.toString();
        String sqlLog = String.valueOf(StringUtils.abbreviate(sqlStatement.substring(sqlStatement.indexOf(":") + 1), (int)DatabaseHelper.sqlMaxLogResultSize)) + " [" + updateRows + " rows, " + processTime + "ms]";
        Global.getLogger.debug("SQL :", sqlLog.toString());
    }
    
    @SuppressWarnings("unchecked")
	public static <T extends AutoCloseable> void dbCloseable(T... objArray) {
    	try {
			for(AutoCloseable obj : objArray) {
				if(obj != null) {
					obj.close();
				}
			}
		} catch (Exception e) {
			throw new DbException(e);
		}
    }
}