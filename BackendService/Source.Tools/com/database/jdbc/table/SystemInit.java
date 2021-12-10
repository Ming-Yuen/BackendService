package com.database.jdbc.table;

import java.util.Date;

import com.database.jdbc.annotation.DatabaseColumn;
import com.database.jdbc.annotation.DatabaseTable;

@DatabaseTable( tableName = "SystemInit")
public class SystemInit {

	@DatabaseColumn(name = "Systemstartup", pk = true)
	public Date Systemstartup;
	
	@DatabaseColumn(name = "SystemEndTime")
	public Date SystemEndTime;
	
	@DatabaseColumn(name = "IPHost", pk = true)
	public String IPHost;
}
