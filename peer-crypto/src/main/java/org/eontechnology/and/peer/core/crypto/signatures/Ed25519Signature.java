package org.eontechnology.and.peer.core.crypto.signatures;

import org.eontechnology.and.peer.core.crypto.ISignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ed25519Signature implements ISignature {
  private static final Logger logger = LoggerFactory.getLogger(Ed25519Signature.class);
  private static final ISignature signer;

  static {
    ISignature signerTmp;
    try {
      signerTmp = new Ed25519SignatureFast();
      signerTmp.verify(new byte[0], new byte[0], new byte[0]);
    } catch (Throwable ex) {
      signerTmp = new Ed25519SignatureNative();
      logger.error(
          "Ed25519SignatureFast not loaded. Used Ed25519SignatureNative. {}", ex.getMessage());
    }
    signer = signerTmp;
  }

  @Override
  public String getName() {
    return "Ed25519";
  }

  @Override
  public KeyPair getKeyPair(byte[] seed) {
    return signer.getKeyPair(seed);
  }

  @Override
  public boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
    return signer.verify(message, signature, publicKey);
  }

  @Override
  public byte[] sign(byte[] messageToSign, byte[] secretKey) {
    return signer.sign(messageToSign, secretKey);
  }
}
