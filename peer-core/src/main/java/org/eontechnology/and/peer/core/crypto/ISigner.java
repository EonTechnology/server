package org.eontechnology.and.peer.core.crypto;

import org.eontechnology.and.peer.core.data.identifier.BlockID;

/** Provides an abstract for an object that allows you to sign messages. */
public interface ISigner {

  /**
   * Returns the public key.
   *
   * @return
   */
  byte[] getPublicKey();

  /**
   * Signs the message using the secret key and returns a signed message.
   *
   * @param obj to be signed
   * @param networkID
   * @return signature
   */
  byte[] sign(Object obj, BlockID networkID);
}
