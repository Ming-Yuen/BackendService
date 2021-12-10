package com.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

public class FileManager {
	
	public static List<File> getAllFile(String prefix, String... folders){
		return getAllFile(prefix, Arrays.stream(folders).map(x->new File(x)).toArray(File[] :: new));
	}
	
	public static List<File> getAllFile(String prefix, List<File> folders){
		return getAllFile(prefix, folders.toArray(new File[folders.size()]));
	}

	public static List<File> getAllFile(String prefix, File... folders) {
		prefix = prefix == null ? "" : prefix.toUpperCase();

		List<File> fileList = new ArrayList<File>();
		for(File folder : folders) {
			List<File> source = new ArrayList<File>(Arrays.asList(folder.listFiles()));
			int defaultSourceCount = source.size();
			for (int index = 0; index < defaultSourceCount; index++) {
				File file = source.get(index);
				if (file.isDirectory()) {
					source.add(file);
					continue;
				}

				if (FilenameUtils.getExtension(file.getName().toUpperCase()).equalsIgnoreCase(prefix)) {
					fileList.add(file);
				}
			}
		}
		return fileList;
	}
}
