package org.eontechnology.and.peer.core.common.exceptions;

/** Thrown if the signature is invalid. */
public class IllegalSignatureException extends ValidateException {
  private static final long serialVersionUID = 1L;

  /** Constructor. */
  public IllegalSignatureException() {
    super("Illegal signature.");
  }

  /**
   * Constructor.
   *
   * @param message The reason for throwing.
   */
  public IllegalSignatureException(String message) {
    super(message);
  }
}
