package org.eontechology.and.peer.core.common.exceptions;

/**
 * Thrown to indicate that the parameters that determine the lifetime of the
 * object are not valid on the current node.
 */
public class LifecycleException extends ValidateException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public LifecycleException() {
        this("Invalid timestamp or other params for set the time.");
    }

    /**
     * Constructor.
     *
     * @param message The reason for throwing.
     */
    public LifecycleException(String message) {
        super(message);
    }
}
