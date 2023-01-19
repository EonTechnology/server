package org.eontechnology.and.peer.core;

import java.security.SecureRandom;
import java.util.Random;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.identifier.BlockID;

public class Signer implements ISigner {
  private Random random = new SecureRandom();
  private String seed;
  private byte[] publicKey;

  public Signer(String seed) {
    this.seed = seed;
    this.publicKey = generate(32);
  }

  private byte[] generate(int length) {
    byte[] bytes = new byte[length];
    random.nextBytes(bytes);
    return bytes;
  }

  @Override
  public byte[] getPublicKey() {
    return publicKey;
  }

  @Override
  public byte[] sign(Object obj, BlockID networkID) {
    return generate(64);
  }
}
