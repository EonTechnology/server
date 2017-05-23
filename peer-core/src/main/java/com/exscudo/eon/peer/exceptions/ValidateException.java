package com.exscudo.eon.peer.exceptions;

/**
 * The class <code>ValidateException</code> and its subclasses indicates an
 * error occurred during validation.
 *
 */
public class ValidateException extends Exception {
	private static final long serialVersionUID = 1L;

	public ValidateException() {
		super("Invalid object.");
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 */
	public ValidateException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            The reason for throwing validation error.
	 * @param cause
	 *            Cause if the error is associated with a exception.
	 */
	public ValidateException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 * 
	 * @param cause
	 *            Cause if the error is associated with a exception.
	 */
	public ValidateException(Throwable cause) {
		super("Invalid object.", cause);
	}

}
