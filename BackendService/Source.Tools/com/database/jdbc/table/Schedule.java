package com.database.jdbc.table;

import java.util.Date;

import com.database.jdbc.annotation.DatabaseColumn;
import com.database.jdbc.annotation.DatabaseTable;

@DatabaseTable( tableName = "Schedule")
public class Schedule {

	@DatabaseColumn(name = "scheduleId", pk = true)
	public String scheduleId;
	
	@DatabaseColumn(name = "desci")
	public String desci;
	
	@DatabaseColumn(name = "processClass")
	public String processClass;
	
	@DatabaseColumn(name = "initial")
	public Date initial;
	
	@DatabaseColumn(name = "intervalTime")
	public Integer intervalTime;
	
	@DatabaseColumn(name = "config")
	public String config;
	
	@DatabaseColumn(name = "lockTime")
	public Date lockTime;
}
