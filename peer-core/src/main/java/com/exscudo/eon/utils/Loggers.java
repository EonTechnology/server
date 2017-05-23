package com.exscudo.eon.utils;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.LogManager;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.Marker;
//import org.slf4j.MarkerFactory;

public enum Loggers {

	NOTICE("notice"), VERBOSE("verbose"), DIAGNOSTIC("diagnostic"), STREAM("stream");

	private Marker marker;

	private Loggers(String name) {
		marker = MarkerManager.getMarker(name);
	}

	private Marker getMarker() {
		return marker;
	}

	public void error(Class<?> clazz, Throwable t) {

		final Logger logger = LogManager.getLogger(clazz);
		logger.error(getMarker(), "Unexpected error has occurred. {}", t);
	}

	public void error(Class<?> clazz, String message, Throwable t) {

		final Logger logger = LogManager.getLogger(clazz);
		logger.error(getMarker(), "{} {}", message, t);
	}

	public void debug(Class<?> clazz, String msg, Throwable t) {

		final Logger logger = LogManager.getLogger(clazz);
		if (logger.isDebugEnabled()) {
			logger.debug(getMarker(), msg, t);
		}
	}

	public void debug(Class<?> clazz, String msg, Object... args) {

		final Logger logger = LogManager.getLogger(clazz);
		if (logger.isDebugEnabled()) {
			logger.debug(getMarker(), msg, args);
		}
	}

	public void trace(Class<?> clazz, String msg, Throwable t) {

		final Logger logger = LogManager.getLogger(clazz);
		if (logger.isTraceEnabled()) {
			logger.trace(getMarker(), msg, t);
		}

	}

	public void trace(Class<?> clazz, String msg, Object... args) {

		final Logger logger = LogManager.getLogger(clazz);
		if (logger.isTraceEnabled()) {
			logger.trace(getMarker(), msg, args);
		}

	}

	public void info(Class<?> clazz, String message) {

		final Logger logger = LogManager.getLogger(clazz);
		if (logger.isInfoEnabled())
			logger.info(getMarker(), message);
	}

	public void info(Class<?> clazz, String message, Object... args) {

		final Logger logger = LogManager.getLogger(clazz);
		if (logger.isInfoEnabled())
			logger.info(getMarker(), message, args);
	}

	public void warning(Class<?> clazz, String message, Object... args) {

		final Logger logger = LogManager.getLogger(clazz);
		if (logger.isWarnEnabled())
			logger.warn(getMarker(), message, args);
	}
}
