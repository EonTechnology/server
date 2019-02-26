package org.eontechology.and.peer.core.common.exceptions;

/**
 * The class {@code DataAccessException} and its subclasses indicates an error
 * occurred during accession to the data into Data Connector implementation.
 */
public class DataAccessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public DataAccessException() {
        super("Data access error.");
    }

    /**
     * Constructor.
     *
     * @param message The reason for throwing.
     */
    public DataAccessException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param cause Cause if the error is associated with a exception. The null value
     *              is permitted and indicates that the cause is nonexistent or
     *              unknown.
     */
    public DataAccessException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     *
     * @param message the detail message.
     * @param cause   the cause. The null value is permitted and indicates that the
     *                cause is nonexistent or unknown.
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
