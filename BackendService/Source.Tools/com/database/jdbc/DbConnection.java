package com.database.jdbc;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;

import com.database.jdbc.DatabaseRecordCache.DataRecordCache;
import com.database.jdbc.DatabaseRecordCache.Execute;
import com.initialization.Global;

public class DbConnection implements AutoCloseable{

	private final Connection dbConnection;
	private static final Integer sqlMaxUpdateSize = Integer.valueOf(1000);
	private static final Integer sqlMaxRows = 50000000;	
	
	public DbConnection() {
		this.dbConnection = DbConnectionPoolManager.getConntion();
	}	

	public Connection getDbConnection() {
		return dbConnection;
	}
	
	public void rollback() {
		if(dbConnection != null) {
			try {
				dbConnection.rollback();
			} catch (SQLException e) {
				throw new DbException("Database rollback failed, ", e);
			}
		}
	}
	
	public void commit() {
		try {
			dbConnection.commit();
		} catch (SQLException e) {
			throw new DbException("Database commit failed", e);
		}
	}
	
	public void release() {
        if (dbConnection!=null){
            try {
            	dbConnection.close();
            }catch (Exception e){
                throw new DbException("Database release failed", e);
            }
        }
	}
	
	public ArrayList<ArrayList<Object>> query(String statement) {
        return query(statement, null, sqlMaxRows);
    }
	
	public ArrayList<ArrayList<Object>> query(String statement, int maxRows) {
        return query(statement, null, maxRows);
    }
    
	public ArrayList<ArrayList<Object>> query(String statement, Object[] whereClause){
		return query(statement, whereClause, sqlMaxRows);
	}
	
    private ArrayList<ArrayList<Object>> query(String statement, Object[] whereClause, int maxRows) {
        ArrayList<ArrayList<Object>> result = new ArrayList<ArrayList<Object>>();
        PreparedStatement stmt = null;
        ResultSet resuleset = null;
        
        Long start = System.currentTimeMillis();
        Long end = start;
        
        try {
            stmt = dbConnection.prepareStatement(statement);
            stmt.setMaxRows(maxRows);
            setParameterValue(stmt, whereClause);
            statement = stmt.toString();
            
            resuleset = stmt.executeQuery();
            ResultSetMetaData rsmd = resuleset.getMetaData();
            while (resuleset.next()) {
                ArrayList<Object> row = new ArrayList<Object>();
                for (int index = 1; index <= rsmd.getColumnCount(); index++) {
                    row.add(resuleset.getObject(index));
                }
                result.add(row);
            }
            end = System.currentTimeMillis();
            log(statement, whereClause, result, end - start);
        }catch (SQLException e) {
            throw new DbException(statement, whereClause, e);
        }finally {
        	dbCloseable(stmt, resuleset);
        };
        return result;
    }
    
    private void log(String statement, Object[] whereClause, ArrayList<ArrayList<Object>> result, long time) {
    	int start = statement.indexOf(":");
    	if(start > -1) {
    		statement = statement.substring(start + 1);
    	}
    	
    	String containWhereClause = start == -1 ? " " + Arrays.toString(whereClause) : "";
    	if(Global.getLogger.log4j.getLevel() == Level.TRACE || Global.getLogger.log4j.getLevel() == Level.ALL) {
    		Global.getLogger.debug(this.getClass().getSimpleName(), statement + containWhereClause + " [" + result.size() +" rows " + time + "ms] " + result);
    	}else {
    		Global.getLogger.debug(this.getClass().getSimpleName(), statement + containWhereClause + " [" + result.size() +" rows " + time + "ms]");
    	}	
    }
    
    public int update(String statement) {
    	return update(statement, new ArrayList<Object[]>());
    }
    
    public int update(String statement, Object[] whereClause) {
    	List<Object[]> list = new ArrayList<Object[]>();
    	list.add(whereClause);
    	return update(statement, list);
    }
    
    public int update(String statement, List<Object[]> whereClause) {
        PreparedStatement stmt = null;
        Long start = System.currentTimeMillis();
        Long end = start;
        try {
            stmt = dbConnection.prepareStatement(statement);
            int rowCount = 0;
            if(whereClause == null || whereClause.isEmpty()) {
            	rowCount += stmt.executeUpdate();
            }
            for(int index = 0; whereClause != null && index < whereClause.size(); index++) {
            	setParameterValue(stmt, whereClause.get(index));
            	if(index % sqlMaxUpdateSize == 0 || index == whereClause.size() - 1) {
            		rowCount += stmt.executeUpdate();
            		stmt.clearBatch();
            	}
            }
            end = System.currentTimeMillis();
            updateLog(statement, whereClause, rowCount, end - start);
            return rowCount;
        }catch (SQLException e) {
            throw new DbException(statement, e);
        }finally {
        	dbCloseable(stmt);
        }
    }
    
    private void updateLog(String statement, List<Object[]> whereClause, int updateRows, long time) {
		StringBuffer buf = new StringBuffer();
		if(whereClause != null && !whereClause.isEmpty()) {
			if(whereClause.size() <= 2 || Global.getLogger.log4j.getLevel() == Level.TRACE || Global.getLogger.log4j.getLevel() == Level.ALL) {
	    		//print all update record log
	    		buf.append(StringUtils.join(whereClause.stream().map(x->Arrays.toString(x)).collect(Collectors.toList()), ""));
	    	}else {
	    		//print first and last update record log
	    		buf.append(Arrays.toString(whereClause.get(0)))
	    		   .append("...")
	    		   .append(Arrays.toString(whereClause.get(whereClause.size() - 1)));
	    		
	    	}
		}
    	Global.getLogger.debug(this.getClass().getSimpleName(), statement + " " + buf + " [" + updateRows +" rows " + time + "ms]");
    }
    
    private static void setParameterValue(PreparedStatement stmt, Object[] whereClause) throws SQLException {
        if (whereClause != null && whereClause.length > 0) {
            for (int index = 1; index <= whereClause.length; ++index) {
                Object value = whereClause[index - 1];
                if (value instanceof Date) {
                    stmt.setTimestamp(index, new Timestamp(((Date)value).getTime()));
                }else {
                    stmt.setObject(index, value);
                }
            }
            stmt.addBatch();
        }
    }
    
    @SuppressWarnings("unchecked")
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
    
    @SuppressWarnings("unchecked")
	private static <T extends AutoCloseable> void dbCloseable(T... objArray) {
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
    
    public <T> ArrayList<T> query(Class<T> record) {
        return query(record, null, null);
    }
    
    public <T> ArrayList<T> query(Class<T> clazz, String whereClause, Object[] values) {
        ArrayList<T> sqlRecordList = new ArrayList<T>();
        DatabaseRecordCache.checkRecordAnnotation(clazz);
        DatabaseRecordCache.DataRecordCache cache = DatabaseRecordCache.getCacheRecord(clazz);
        ArrayList<ArrayList<Object>> sqlResult = query(cache.getSelectCmd(whereClause), values);
        Map<String, Field> columnList = cache.getAllColumnField();
        try {
            for (int sqlRow = 0; sqlRow < sqlResult.size(); ++sqlRow) {
                final T sqlRecord = clazz.newInstance();
                for (int sqlCol = 0; sqlCol < sqlResult.get(sqlRow).size(); ++sqlCol) {
                    if (DatabaseHelper.getValue(sqlResult, sqlRow, sqlCol) != null) {
                        final Field field = columnList.get(cache.getAllColumn().get(sqlCol));
                        if (field.getType() == String.class) {
                            field.set(sqlRecord, DatabaseHelper.getValue(sqlResult, sqlRow, sqlCol).toString());
                        }
                        else {
                            field.set(sqlRecord, DatabaseHelper.getValue(sqlResult, sqlRow, sqlCol));
                        }
                    }
                }
                sqlRecordList.add(sqlRecord);
            }
        }catch (InstantiationException | SecurityException | IllegalAccessException ex2) {
            throw new DbException(ex2);
        }
        return sqlRecordList;
    }
    
    @SuppressWarnings("unchecked")
	public <T> Integer insert(T... data) {
        return execute(Execute.insert, data);
    }
    
    @SuppressWarnings("unchecked")
	public <T> Integer update(T... data) {
        return execute(Execute.update, data);
    }
    
    @SuppressWarnings("unchecked")
	public <T> Integer execute(boolean isInsert, T... data) {
        return execute(isInsert ? Execute.insert : Execute.update, data);
    }
    
    @SuppressWarnings("unchecked")
	private <T> Integer execute(Execute execute, T... data) {
        Map<Class<?>, List<T>> recordMapping = Arrays.stream(data).collect(Collectors.groupingBy(x->x.getClass()));

        int updateCount = 0;
        for(Class<?> clazz : recordMapping.keySet()) {
        	List<T> batch = recordMapping.get(clazz);
        	DatabaseRecordCache.DataRecordCache cache = DatabaseRecordCache.getCacheRecord(batch.get(0).getClass());
        	
        	updateCount += update(cache.getCmd(execute), getRecordValues(cache, execute, batch));
        }
        return updateCount;
    }
    
    private <T> List<Object[]> getRecordValues(DataRecordCache cache, Execute execute, List<T> batch) {
        List<String> columnName = new ArrayList<String>();
        switch (execute) {
            case insert: {
                columnName.addAll(cache.getAllColumn());
                break;
            }
            case update: {
                columnName.addAll(cache.getUpdateColumn());
                columnName.addAll(cache.getPrimaryKeyColumn());
                break;
            }
            case delete: {
                columnName.addAll(cache.getPrimaryKeyColumn());
                break;
            }
		default:
			throw new IllegalArgumentException("Unsupport " + execute.toString());
        }
        List<Object[]> parameter = new ArrayList<Object[]>();
        for(Object record : batch) {
        	ArrayList<Object> rowParam = new ArrayList<Object>();
	        for (String name : columnName) {
	            Field field = cache.getAllColumnField().get(name);
	            try {
	            	rowParam.add(field.get(record));
	            }catch (IllegalArgumentException | IllegalAccessException e) {
	                throw new DbException(record.getClass() + "." + field.getName() + " get value error");
	            }
	        }
            parameter.add(rowParam.toArray());
        }
        return parameter;
    }

	@Override
	public void close() {
		release();
	}
}
