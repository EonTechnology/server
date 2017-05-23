package com.exscudo.eon.peer.exceptions;

/**
 * Thrown to indicate an attempt to double spending.
 *
 */
public class DoubleSpendingException extends ValidateException {
	private static final long serialVersionUID = 1L;

	public DoubleSpendingException() {
		this("Double spending.");
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            The reason for throwing.
	 */
	public DoubleSpendingException(String message) {
		super(message);
	}
}
