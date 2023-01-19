package org.eontechnology.and.peer.core.crypto.signatures;

import jnr.ffi.byref.LongLongByReference;
import org.abstractj.kalium.NaCl;
import org.eontechnology.and.peer.core.crypto.ISignature;

public class Ed25519SignatureFast implements ISignature {
  private final NaCl.Sodium sodium = NaCl.sodium();

  @Override
  public String getName() {
    return "Ed25519";
  }

  @Override
  public KeyPair getKeyPair(byte[] seed) {
    byte[] sk = new byte[64];
    byte[] pk = new byte[32];
    sodium.crypto_sign_ed25519_seed_keypair(pk, sk, seed);

    return new KeyPair(sk, pk);
  }

  @Override
  public boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
    int state =
        sodium.crypto_sign_ed25519_verify_detached(signature, message, message.length, publicKey);
    return state == 0;
  }

  @Override
  public byte[] sign(byte[] messageToSign, byte[] secretKey) {
    byte[] sig = new byte[64];
    sodium.crypto_sign_ed25519_detached(
        sig, new LongLongByReference(), messageToSign, messageToSign.length, secretKey);
    return sig;
  }
}
