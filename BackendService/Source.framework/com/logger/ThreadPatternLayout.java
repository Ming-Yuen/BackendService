package com.logger;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.PatternParser;

public class ThreadPatternLayout extends PatternLayout {

	public ThreadPatternLayout(String pattern) {
		super(pattern);
	}

	public ThreadPatternLayout() {
		super();
	}

	/**
	 * Rewote the CreatePatternParser method, return to the subclass of
	 * PatternParser
	 */
	@Override
	protected PatternParser createPatternParser(String pattern) {
		return new ThreadPatternParser(pattern);
	}
}