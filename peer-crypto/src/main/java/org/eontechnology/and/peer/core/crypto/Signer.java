package org.eontechnology.and.peer.core.crypto;

import java.util.Objects;
import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.common.Loggers;
import org.eontechnology.and.peer.core.crypto.mapper.ObjectMapper;
import org.eontechnology.and.peer.core.crypto.signatures.Ed25519Signature;
import org.eontechnology.and.peer.core.data.identifier.BlockID;

public class Signer implements ISigner {
  private final ISignature signature;
  private final ISignature.KeyPair keyPair;
  private final IFormatter formatter;

  public Signer(ISignature signature, byte[] seed) {
    this.signature = signature;
    this.keyPair = signature.getKeyPair(seed);
    this.formatter = new BencodeFormatter();
  }

  public static Signer createNew(String seed) {
    Signer signer = null;
    try {
      byte[] bytes = Format.convert(seed);
      signer = new Signer(new Ed25519Signature(), bytes);
    } catch (Throwable t) {
      Loggers.warning(Signer.class, "Wrong seed string. ISigner not created.");
    }

    return signer;
  }

  @Override
  public byte[] getPublicKey() {
    return keyPair.publicKey;
  }

  @Override
  public byte[] sign(Object obj, BlockID networkID) {

    ObjectMapper mapper = new ObjectMapper(Objects.requireNonNull(networkID));

    byte[] message;
    try {
      message = formatter.getBytes(mapper.convert(obj));
    } catch (Exception e) {
      throw new IllegalArgumentException();
    }

    return this.signature.sign(message, keyPair.secretKey);
  }
}
