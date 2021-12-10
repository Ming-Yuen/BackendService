package com.logger;

public class Logger {
	public final org.apache.log4j.Logger log4j = org.apache.log4j.Logger.getLogger("");

	public void all(String procName, String message) {
		log4j.debug(procName + "\t" + message);
	}

	public void trace(String procName, String message) {
		log4j.trace(procName + "\t" + message);
	}

	public void debug(String message) {
		log4j.debug(message);
	}
	
	public void debug(String procName, String message) {
		log4j.debug(procName + "\t" + message);
	}

	public void info(String message) {
		log4j.info(message);
	}
	
	public void info(String procName, String message) {
		log4j.info(procName + "\t" + message);
	}

	public void info(String procName, String methodsName, String message) {
		log4j.info("Method:" + procName + "." + methodsName + "\t" + message);
	}

	public void warn(String procName, String message) {
		log4j.warn(procName + "\t" + message);
	}

	public void error(String procName, String message) {
		log4j.error(procName + "\t" + message);
	}
	
	public void error(String message, Throwable t) {
		log4j.error(message, t);
	}

	public void error(String procName, String message, Throwable t) {
		log4j.error(procName + "\t" + message, t);
	}
}