package com.file;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FileConnection {
	
	public static class Mandatory{
		private String path;
		private Class<?> schema;
	}
	
	public static class Selective{
		private Charset charset = StandardCharsets.UTF_8;
	}
	
	private static class SchemaDataRecord{
		
	}
	
	private static final Map<Class<?>, SchemaDataRecord> schemaDataRecordCache = new HashMap<Class<?>, SchemaDataRecord>();
	
	public void getFileContent(Mandatory mandatory, Selective selective) {
//		FileInputStream input = new FileInputStream();
	}
	
	public void getFileRecord(Mandatory mandatory, Selective selective) {
		
	}
}
