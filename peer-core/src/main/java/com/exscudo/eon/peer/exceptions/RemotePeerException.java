package com.exscudo.eon.peer.exceptions;

/**
 * The class <code>RemotePeerException</code> and its subclasses indicates an
 * error occurred while accessing to the remote node.
 */
public class RemotePeerException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public RemotePeerException() {
		super("Unexpected error has occurred.");
	}

	/**
	 * Constructor.
	 * 
	 * @param cause
	 *            Cause if the error is associated with a exception.
	 */
	public RemotePeerException(Throwable cause) {
		super("Unexpected error has occurred.", cause);
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            The reason for throwing.
	 */
	public RemotePeerException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            The reason for throwing.
	 * @param cause
	 *            Cause if the error is associated with a exception.
	 */
	public RemotePeerException(String message, Throwable cause) {
		super(message, cause);
	}
}
