package org.eontechnology.and.peer.core.common.exceptions;

/**
 * Thrown {@code ProtocolException} if the behavior of the services node does
 * not match expectations.
 */
public class ProtocolException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param message The reason for throwing.
     */
    public ProtocolException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param cause Cause if the error is associated with a exception. The null value
     *              is permitted and indicates that the cause is nonexistent or
     *              unknown.
     */
    public ProtocolException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {

        String message = super.getMessage();
        if (message != null && message.length() > 0) {
            return message;
        }

        if (super.getCause() != null) {
            return super.getCause().getMessage();
        }

        return "Protocol exception.";
    }
}
