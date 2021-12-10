package com.logger;

import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LoggingEvent;

public class ThreadPatternParser extends PatternParser {

	public ThreadPatternParser(String pattern) {
		super(pattern);
	}

	/**
	 * Rewote FinalizeConverter, process the specific placeholders, T represent
	 * thread ID placeholder
	 */
	@Override
	protected void finalizeConverter(char c) {
		if (c == 'T') {
			this.addConverter(new ExPatternConverter(this.formattingInfo));
		} else {
			super.finalizeConverter(c);
		}
	}

	private static class ExPatternConverter extends PatternConverter {

		public ExPatternConverter(FormattingInfo fi) {
			super(fi);
		}

		/**
		 * When you need to display the thread ID, return the ID of the current calling
		 * thread
		 */
		@Override
		protected String convert(LoggingEvent event) {
			return String.valueOf("Thread-" + Thread.currentThread().getId());
		}
	}
}