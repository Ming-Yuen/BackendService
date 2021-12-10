package com.database.jdbc;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.database.jdbc.DatabaseManager.DbDriver;
import com.file.FileManager;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.initialization.Global;

public class DatabaseUpgrade {
	
	public final String schemaFolder = String.join(File.separator, Global.getRealPath(), "SQLSchema");
	
	String procName = "Database update schema";
	
	public enum KEYProcess{
		CREATE, SELECT, INSERT, ALTER, TABLE, TRIGGER;
	}

	public void schemaPrepare() {
		try {
			List<File> files = getSchemaFileList();
			try(DbConnection dbConn = new DbConnection()){
				for(int index = 0; index < files.size(); index++) {
					List<String> sqlContainner = getStatement(files.get(index));
					DbInfoCache structure = new DbInfoCache(dbConn);
					
					for(String sql : sqlContainner) {
						sql = sql.trim().toUpperCase();
						createProcess(dbConn, structure, sql);
						alterProcess(dbConn, sql);
					}
				}
			}
		} catch (Exception e) {
			Global.getLogger.error(procName, e.getMessage(), e);
		}
	}
	
	private void alterProcess(DbConnection dbConn, String sql) throws SQLException{
		if(!sql.startsWith(KEYProcess.ALTER.toString())) {
			return;
		}
		dbConn.update(sql);
	}
	
	private void createProcess(DbConnection dbConn, DbInfoCache structure, String sql) throws SQLException{
		if(!sql.startsWith(KEYProcess.CREATE.toString())) {
			return;
		}
		
		Integer index_table = sql.indexOf(KEYProcess.TABLE.name());
		if(index_table != -1) {
			String table = StringUtils.substringBetween(sql, KEYProcess.TABLE.name(), "(");
			  
			if(!structure.tables.contains(table)) {
				dbConn.update(sql);
			}
			return;
		}
		
		Integer index_trigger = sql.indexOf(KEYProcess.TRIGGER.name());
		if(index_trigger != -1) {
			String trigger = StringUtils.substringBetween(sql, KEYProcess.TRIGGER.name(), " ");
			  
			if(!structure.tables.contains(trigger)) {
				dbConn.update(sql);
			}
			return;
		}
	}
	public enum TriggerFormat{
		createOrAlert(DbDriver.oracle, "CREATE OR ALTER TRIGGER"), create(DbDriver.oracle, "CREATE TRIGGER"), ;

		TriggerFormat(DbDriver oracle, String string) {
			// TODO Auto-generated constructor stub
		}
	}
	
	private Set<String> triggerFormat = new HashSet<String>();
	public boolean isTrigger(String sql) {
		sql = sql.replace(" ", "").toUpperCase();
		if(triggerFormat.isEmpty()) {
			DbDriver driver = DatabaseManager.getDbDriver();
			if(DbDriver.sqlserver == driver) {
				// CREATE OR ALTER TRIGGER
				triggerFormat.add("CREATEORALTERTRIGGER");
				// CREATE TRIGGER
				triggerFormat.add("CREATETRIGGER");
			}
		}
		return triggerFormat.contains(sql);
	}
	
	public List<String> getStatement(File schema) throws IOException {
		List<String> sqlContainner = new ArrayList<String>();
		List<String> lines = FileUtils.readLines(schema, StandardCharsets.UTF_8);
		Global.getLogger.info(procName, "Reading database schema : " + schema.getPath());
		boolean multiLineComment = false;
		
		StringBuilder sqlAppend = new StringBuilder();
		boolean isTrigger = false;
		for(int line = 0; line < lines.size(); line++) {
			char[] characters = lines.get(line).toCharArray();
			isTrigger = isTrigger(sqlAppend.toString());
			
			for(int charIndex  = 0; charIndex < characters.length; charIndex++) {
				// comment current line
				if(isCommentProcess("-- ", charIndex, characters)) {
					break;
				}
				
				// Multiple-line comment start
				if(isCommentProcess("/*", charIndex, characters)) {
					charIndex++;// next char is *
					multiLineComment = true;
					continue;
				}
				
				// Multiple-line comment end
				if(isCommentProcess("*/", charIndex, characters)) {
					charIndex++;// next char is /
					multiLineComment = false;
					continue;
				}
				
				if(!multiLineComment && characters[charIndex] == ';') {
					sqlContainner.add(sqlAppend.toString());
					sqlAppend.setLength(0);
					continue;
				}
				
				if(!multiLineComment) {
					sqlAppend.append(characters[charIndex]);
				}
			}
		}
		return sqlContainner;
	}
	
	public boolean isCommentProcess(String comment, int index, char[] characters) {
		return characters.length >= (index + comment.length()) && String.valueOf(characters, index, comment.length()).equals(comment);
	}
	
	private List<File> getSchemaFileList() throws IOException{
		File schemaDriverFolder = new File(schemaFolder);
		if(!schemaDriverFolder.exists()) {
			Files.createDirectories(schemaDriverFolder.toPath());
		}		
		
		//check database drive get schema file
		List<File> sqlFolders = Arrays.stream(schemaDriverFolder.listFiles()).filter(x->x.isDirectory() && x.getName().equalsIgnoreCase(DatabaseManager.getDbDriver().toString())).collect(Collectors.toList());
		List<File> sqlFiles = FileManager.getAllFile("sql", sqlFolders);
		
		if(sqlFiles.isEmpty() || Arrays.stream(schemaDriverFolder.listFiles()).filter(x->x.isDirectory()).findAny().equals(Optional.empty())) {
			File schemaFolder = new File(String.join(File.separator, schemaDriverFolder.getPath(), DatabaseManager.getDbDriver().toString()));
			Files.createDirectories(schemaFolder.toPath());
			Global.getLogger.info(procName, "No file need to prepare schema in " + schemaFolder.getPath());
			return new ArrayList<File>();
		}

		return sqlFiles;
	}
	
	public static class DbInfoCache{
		public final Set<String> tables;
		public final Multimap<String, String> indexs = ArrayListMultimap.create();
		public final Multimap<String, String> primaryKey = ArrayListMultimap.create();
		
		public DbInfoCache(DbConnection dbConn) {
			tables = DatabaseManager.getContainTable(dbConn);
		}
	}
}
