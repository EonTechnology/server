package org.eontechnology.and.peer.core.data;

/** The base object for calculating the signature generation */
public class Generation {
  public final byte[] prevSignature;
  public final String salt;

  public Generation(byte[] prevSignature, String salt) {
    this.prevSignature = prevSignature;
    this.salt = salt;
  }
}
