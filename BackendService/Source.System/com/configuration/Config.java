package com.configuration;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

import com.initialization.Global;

public class Config {
	private static final Properties prop = new Properties();
	private static final String configProperties = "config.properties";

	public String getConfigValue(String configName) {
		try {
			if (!prop.contains(configName)) {
				try (InputStream inputStream = Config.class.getClassLoader().getResourceAsStream(configProperties);) {
					if (inputStream != null) {
						prop.load(inputStream);
					} else {
						throw new FileNotFoundException("property file '" + configProperties + "' not found in the classpath");
					}
				}
			}
			return prop.getProperty(configName);
		} catch (Exception e) {
			Global.getLogger.error(Config.class.getName(), e);
		}
		throw new RuntimeException("Config " + configName + " not found");
	}

	public static void writeConfigValue(String configName, String configValue) {
		try {
			try (InputStream inputStream = Config.class.getClassLoader().getResourceAsStream(configProperties);) {
				prop.load(inputStream);
			}
			prop.setProperty(configName, configValue);
			try (FileOutputStream fos = new FileOutputStream(
					(Config.class.getResource("/") + configProperties).substring(6));) {
				prop.store(fos, null);
			}
			Global.getLogger.debug("update config" + configName + " success in " + configProperties);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public Properties getProperties() {
		return prop;
	}
}