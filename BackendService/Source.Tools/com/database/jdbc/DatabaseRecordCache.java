package com.database.jdbc;
import org.apache.commons.lang3.StringUtils;

import com.database.jdbc.annotation.DatabaseColumn;
import com.database.jdbc.annotation.DatabaseTable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DatabaseRecordCache{
	
	protected enum Execute{
	    select, insert, update, delete;
	}
	
    private static final HashMap<Class<?>, DatabaseRecordCache.DataRecordCache> DatabaseRecordListCache = new HashMap<Class<?>, DatabaseRecordCache.DataRecordCache>();
    
    public static class DataRecordCache{
        private Class<?> format;
        private String tableName;
        private String selectCmd;
        private String insertCmd;
        private String updateCmd;
        private String deleteCmd;
        private Map<String, Field> allColumnField = new HashMap<String, Field>();
        
        private List<String> primaryKeyColumn 	= new ArrayList<String>();
        private List<String> updateColumn 		= new ArrayList<String>();
        private List<String> allColumn 			= new ArrayList<String>();
        
        public Class<?> getFormat() {
            return this.format;
        }
        
        public String getTableName() {
            return this.tableName;
        }
        
        public String getCmd(Execute execute) {
            String cmd = null;
            switch (execute) {
                case select: {
                    cmd = this.getSelectCmd();
                    break;
                }
                case update: {
                    cmd = this.getUpdateCmd();
                    break;
                }
                case insert: {
                    cmd = this.getInsertCmd();
                    break;
                }
                case delete: {
                    cmd = this.getDeleteCmd();
                    break;
                }
            }
            return cmd;
        }
        
        public String getSelectCmd() {
            return this.getSelectCmd(null);
        }
        
        public String getSelectCmd(String whereClause) {
            this.selectCmd = this.selectCmd != null ? this.selectCmd : DatabaseRecordCache.getSqlCmd(this, Execute.select).toString();
            return this.selectCmd + (whereClause == null || whereClause.trim().isEmpty() ? "" : " where " + whereClause);
        }
        
        public String getInsertCmd() {
            return this.insertCmd = this.insertCmd != null ? this.insertCmd : DatabaseRecordCache.getSqlCmd(this, Execute.insert).toString();
        }
        
        public String getUpdateCmd() {
            return this.updateCmd;
        }
        
        public String getUpdateCmd(String whereClause) {
            this.updateCmd = ((this.updateCmd != null) ? this.updateCmd : DatabaseRecordCache.getSqlCmd(this, Execute.update).toString());
            return this.updateCmd + (whereClause == null || whereClause.trim().isEmpty() ? "" : " and " + whereClause);
        }
        
        public String getDeleteCmd() {
            return this.deleteCmd;
        }
        
        public String getDeleteCmd(String whereClause) {
            this.deleteCmd = this.deleteCmd != null ? this.deleteCmd : DatabaseRecordCache.getSqlCmd(this, Execute.delete).toString();
            return this.deleteCmd + (whereClause == null || whereClause.trim().isEmpty() ? "" : " and " + whereClause);
        }
        
        public Map<String, Field> getAllColumnField() {
            return this.allColumnField;
        }
        
        public List<String> getPrimaryKeyColumn() {
            return this.primaryKeyColumn;
        }
        
        public List<String> getUpdateColumn() {
            return this.updateColumn;
        }
        
        public List<String> getAllColumn() {
            return this.allColumn;
        }
    }
    
    protected static void checkRecordAnnotation(final Class<?> clazz) {
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
    
    public static DatabaseRecordCache.DataRecordCache getCacheRecord(Class<?> clazz) {
        if (DatabaseRecordListCache.containsKey(clazz)) {
            return DatabaseRecordListCache.get(clazz);
        }
        synchronized (clazz) {
            if (DatabaseRecordListCache.containsKey(clazz)) {
                return DatabaseRecordListCache.get(clazz);
            }
            boolean isValidate = false;
            DataRecordCache cache = new DataRecordCache();
            DatabaseTable table = clazz.getAnnotation(DatabaseTable.class);
            if (table == null) {
                throw new DbException(clazz.getName() + " annotation " + DatabaseTable.class.getSimpleName() + " not found");
            }
            
            cache.format 	= clazz;
            cache.tableName =  table.tableName();
            
            Field[] fields = clazz.getFields();
            for (int index = 0; index < fields.length; index++) {
                Field field = fields[index];
                DatabaseColumn column = field.getAnnotation(DatabaseColumn.class);
                if (column != null) {
                	cache.allColumn.add(column.name().toUpperCase());
                	cache.allColumnField.put(column.name().toUpperCase(), field);
                    if (column.pk()) {
                    	cache.primaryKeyColumn.add(column.name().toUpperCase());
                    }else {
                    	cache.updateColumn.add(column.name().toUpperCase());
                    }
                    isValidate = true;
                }
            }
            if (!isValidate) {
                throw new DbException(String.valueOf(clazz.getName()) + " annotation " + DatabaseColumn.class.getSimpleName() + " not found");
            }
            return cache;
        }
    }
    
    private static StringBuffer getSqlCmd(DataRecordCache cacheRecod, Execute execute) {
        synchronized (cacheRecod) {
            final StringBuffer sqlStatement = new StringBuffer();
            switch (execute) {
                case select: {
                    if (cacheRecod.selectCmd != null) {
                        sqlStatement.append(cacheRecod.selectCmd);
                        break;
                    }
                    sqlStatement.append("SELECT ").append(String.join(",", cacheRecod.getAllColumn())).append(" FROM ").append(cacheRecod.getTableName());
                    break;
                }
                case update: {
                    if (cacheRecod.updateCmd != null) {
                        sqlStatement.append(cacheRecod.updateCmd);
                        break;
                    }
                    sqlStatement.append("update ").append(cacheRecod.getTableName()).append(" set ").append(String.join(" = ?, ", cacheRecod.getUpdateColumn())).append(" = ?").append(" where ").append(String.join(" = ? and ", cacheRecod.getPrimaryKeyColumn())).append(" = ?");
                    break;
                }
                case insert: {
                    if (cacheRecod.insertCmd != null) {
                        sqlStatement.append(cacheRecod.insertCmd);
                        break;
                    }
                    sqlStatement.append("insert into ").append(cacheRecod.getTableName()).append(" (").append(String.join(", ", cacheRecod.getAllColumn())).append(")").append(" values (").append(StringUtils.repeat("?", ",", cacheRecod.getAllColumn().size())).append(")");
                    break;
                }
                case delete: {
                    if (cacheRecod.deleteCmd != null) {
                        sqlStatement.append(cacheRecod.deleteCmd);
                        break;
                    }
                    sqlStatement.append("delete from ").append(cacheRecod.getTableName()).append(" where ").append(String.join(" = ?", cacheRecod.getPrimaryKeyColumn())).append(" = ?");
                    break;
                }
            }
            return sqlStatement;
        }
    }
}