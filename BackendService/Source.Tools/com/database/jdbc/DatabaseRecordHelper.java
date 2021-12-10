package com.database.jdbc;

import java.util.Iterator;
import java.util.Collection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import com.database.jdbc.DatabaseRecordCache.Execute;
import com.database.jdbc.annotation.DatabaseColumn;
import com.database.jdbc.annotation.DatabaseTable;

import java.util.ArrayList;
import java.sql.Connection;
import java.lang.reflect.Field;
import java.util.HashSet;

public class DatabaseRecordHelper{
	
    private static void checkRecordAnnotation(final Class<?> clazz) {
        final DatabaseTable type = clazz.getAnnotation(DatabaseTable.class);
        if (type == null) {
            throw new DbException(String.valueOf(clazz.getName()) + " not found " + DatabaseTable.class.getSimpleName() + " annotation");
        }
        if (type.tableName().isEmpty()) {
            throw new DbException(String.valueOf(clazz.getName()) + " " + DatabaseTable.class.getSimpleName() + " tableName cannot empty");
        }
        boolean primary_key_exists = false;
        final HashSet<String> column_name_list = new HashSet<String>();
        Field[] fields;
        for (int length = (fields = clazz.getFields()).length, i = 0; i < length; ++i) {
            final Field field = fields[i];
            final DatabaseColumn column_name = field.getAnnotation(DatabaseColumn.class);
            if (column_name != null) {
                if (column_name.name().isEmpty()) {
                    throw new DbException(String.valueOf(clazz.getName()) + field.getName() + " annotation column name cannot empty");
                }
                if (column_name_list.contains(column_name.name().toUpperCase())) {
                    throw new DbException(String.valueOf(clazz.getName()) + " dulipcate sql column name : " + column_name.name());
                }
                column_name_list.add(column_name.name().toUpperCase());
                if (column_name.pk()) {
                    primary_key_exists = true;
                }
            }
        }
        if (!primary_key_exists) {
            throw new DbException(String.valueOf(clazz.getName()) + " not found primary key 'pk' annotation");
        }
        if (column_name_list.isEmpty()) {
            throw new DbException(String.valueOf(clazz.getName()) + " not found column annotation, annotation : " + DatabaseColumn.class.getSimpleName());
        }
    }
    
    public static synchronized <T> ArrayList<T> query(final Connection dbConn, final Class<T> record) {
        return query(dbConn, record, null, null);
    }
    
    public static synchronized <T> ArrayList<T> query(final Connection dbConn, final Class<T> clazz, final String whereClause, final Object[] values) {
        final ArrayList<T> sqlRecordList = new ArrayList<T>();
        checkRecordAnnotation(clazz);
        final DatabaseRecordCache.DataRecordCache cache = DatabaseRecordCache.getCacheRecord(clazz);
        final ArrayList<ArrayList<Object>> sqlResult = null ;//= DatabaseHelper.query(dbConn, cache.getSelectCmd(whereClause), values);
        final Map<String, Field> columnList = cache.getAllColumnField();
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
        }
        catch (InstantiationException | SecurityException | IllegalAccessException ex2) {
            throw new DbException(ex2);
        }
        return sqlRecordList;
    }
    
    public static <T> Integer insert(final Connection dbConn, final T record) {
        return execute(dbConn, Arrays.asList(record), Execute.insert);
    }
    
    public static <T> Integer insert(final Connection dbConn, final List<T> record) {
        return execute(dbConn, record, Execute.insert);
    }
    
    public static <T> Integer update(final Connection dbConn, final T record) {
        return execute(dbConn, Arrays.asList(record), Execute.update);
    }
    
    public static <T> Integer update(final Connection dbConn, final List<T> record) {
        return execute(dbConn, record, Execute.update);
    }
    
    public static <T> Integer execute(final Connection dbConn, final T record, Execute execute) {
        return execute(dbConn, Arrays.asList(record), execute);
    }
    
    public static <T> Integer execute(final Connection dbConn, final List<T> recordList, Execute execute) {
        final Date queryStartTime = new Date();
        final DatabaseRecordCache.DataRecordCache cache = DatabaseRecordCache.getCacheRecord(recordList.get(0).getClass());
        PreparedStatement stmt = null;
        try {
            stmt = dbConn.prepareStatement(cache.getCmd(execute));
            for (int index = 0; index < recordList.size(); ++index) {
                final T record = recordList.get(index);
                final List<Object> columnValues = getSqlFieldValues(cache, record, execute);
                DatabaseHelper.databaseSetValue(stmt, columnValues.toArray());
            }
            if (stmt == null) {
                throw new DbException("PreparedStatement is null");
            }
            final int[] updateRows = stmt.executeBatch();
            final int ttlUpdateRows = Arrays.stream(updateRows).sum();
            DatabaseHelper.sqlUpdateLog(stmt, ttlUpdateRows, new Date().getTime() - queryStartTime.getTime());
            return ttlUpdateRows;
        }
        catch (SQLException e) {
            throw new DbException((Throwable)e);
        }
        finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            }
            catch (SQLException e2) {
                throw new DbException((Throwable)e2);
            }
        }
    }
    
    private static List<Object> getSqlFieldValues(final DatabaseRecordCache.DataRecordCache cache, final Object record, Execute execute) {
        final List<String> columnName = new ArrayList<String>();
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
        }
        final List<Object> parameter = new ArrayList<Object>();
        for (final String name : columnName) {
            final Field field = cache.getAllColumnField().get(name);
            try {
                parameter.add(field.get(record));
            }
            catch (IllegalAccessException e) {
                throw new DbException(record.getClass() + "." + field.getName() + " get value error");
            }
        }
        return parameter;
    }
}