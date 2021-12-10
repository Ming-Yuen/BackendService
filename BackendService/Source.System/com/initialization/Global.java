package com.initialization;

import javax.servlet.ServletContext;

import com.configuration.Config;
import com.database.jdbc.DatabaseManager;
import com.logger.Logger;

public class Global {
	public static Logger getLogger = new Logger();
	public static Config getConfig = new Config();
	
	public static ServletContext context;

	public static void init(Container container) {
		context = container.getServletContext();
		infomation();
	}	
	
	private static final StringBuffer realPath = new StringBuffer(); 
	public static String getRealPath() {
		if(realPath.length() == 0) {
			realPath.append(context.getRealPath("").toCharArray());
		}
		return realPath.toString();
	}
	
	public static void infomation() {
		String catalina = System.getProperty("catalina.base");
		Global.getLogger.info("System base directory : " + catalina);
		Global.getLogger.info("Database connection " + DatabaseManager.DbConfig.getDescription());
	}
}
