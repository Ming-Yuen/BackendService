package com.schedule;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import com.database.jdbc.DbConnection;
import com.database.jdbc.table.SystemInit;
import com.initialization.Container;
import com.system.information.SystemProtocol;

public class SystemSignOn {
	
	public final static AtomicBoolean prepareInit = new AtomicBoolean(true);
	
	public void init() {
		
	}

	public void run() {
		SystemInit init 	= new SystemInit();
		init.Systemstartup  = new Date(Container.envirStartTime);
		init.SystemEndTime	= new Date();
		init.IPHost			= SystemProtocol.getIPv4Host();
		try(DbConnection dbConn = new DbConnection()){
			dbConn.execute(prepareInit.get(), init);
			prepareInit.set(false);
			dbConn.commit();
		}
	}
}
