package com.exscudo.eon.peer.exceptions;

/**
 * Thrown if the behavior of the remote node does not match expectations.
 *
 */
public class ProtocolException extends ValidateException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            The reason for throwing .
	 */
	public ProtocolException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * 
	 * @param cause
	 *            Cause if the error is associated with a exception.
	 */
	public ProtocolException(Throwable cause) {
		super(cause);
	}

	@Override
	public String getMessage() {

		String message = super.getMessage();
		if (message != null) {
			if (message.length() > 0)
				return message;
		}

		if (super.getCause() != null) {
			return super.getCause().getMessage();
		}

		return "Protocol exception.";

	}
}
