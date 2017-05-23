package com.exscudo.eon.exceptions;

public class DecodeException extends Exception {
	private static final long serialVersionUID = 1L;

	public DecodeException() {
		super("Unknown format or data corrupted.");
	}

	public DecodeException(String value) {
		super("Unknown format or data corrupted. " + value);
	}

	public DecodeException(String value, Throwable t) {
		super("Unknown format or data corrupted. " + value, t);
	}
}
