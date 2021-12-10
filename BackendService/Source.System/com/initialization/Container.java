package com.initialization;

import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import com.database.jdbc.DatabaseUpgrade;
import com.schedule.ScheduleManager;
import com.system.information.SystemProtocol;

public class Container extends HttpServlet {

	private static final long serialVersionUID = 2207189384084619739L;

	public Logger log = Logger.getLogger(this.getClass().getSimpleName());

	public final static Long envirStartTime = System.currentTimeMillis();
	
	public enum Service{
		
	}

	public void init() {
		try {
			Global.getLogger.info("System initialization begins");

			Global.init(this);
			new DatabaseUpgrade().schemaPrepare();
//			SystemProtocol.init();
//			ScheduleManager.init();
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		} finally {
			Global.getLogger.info("System initialization completed");
		}
	}
}