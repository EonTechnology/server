package com.exscudo.eon.peer.exceptions;

/**
 * The class <code>UnknownObjectException</code> and its subclasses indicates an
 * error occurred when accessing an unknown object
 *
 */
public class UnknownObjectException extends ValidateException {
	private static final long serialVersionUID = 1L;

	/**
	 * /** Constructor.
	 * 
	 * @param message
	 *            The reason for throwing.
	 */
	public UnknownObjectException(String message) {
		super(message);
	}
}
