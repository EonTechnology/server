package org.eontechnology.and.peer.core.common.exceptions;

/**
 * The class {@code RemotePeerException} and its subclasses indicates an error
 * occurred while accessing to the services env.
 */
public class RemotePeerException extends IllegalArgumentException {
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
     * @param cause Cause if the error is associated with a exception. The null value
     *              is permitted and indicates that the cause is nonexistent or
     *              unknown.
     */
    public RemotePeerException(Throwable cause) {
        super("Unexpected error has occurred.", cause);
    }

    /**
     * Constructor.
     *
     * @param message The reason for throwing.
     */
    public RemotePeerException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message The reason for throwing.
     * @param cause   Cause if the error is associated with a exception. The null value
     *                is permitted and indicates that the cause is nonexistent or
     *                unknown.
     */
    public RemotePeerException(String message, Throwable cause) {
        super(message, cause);
    }
}
