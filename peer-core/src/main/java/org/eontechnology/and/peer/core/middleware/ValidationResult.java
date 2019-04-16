package org.eontechnology.and.peer.core.middleware;

import org.eontechnology.and.peer.core.common.exceptions.ValidateException;

public class ValidationResult {
    public static final ValidationResult success = new ValidationResult();

    public final boolean hasError;
    public final ValidateException cause;

    private ValidationResult() {
        this.hasError = false;
        this.cause = null;
    }

    private ValidationResult(ValidateException cause) {
        this.hasError = true;
        this.cause = cause;
    }

    public static ValidationResult error(String message) {
        return new ValidationResult(new ValidateException(message));
    }

    public static ValidationResult error(ValidateException cause) {
        return new ValidationResult(cause);
    }
}
