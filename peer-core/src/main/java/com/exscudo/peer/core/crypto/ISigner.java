package com.exscudo.peer.core.crypto;

import java.util.Objects;

/**
 * Provides an abstract for an object that allows you to sign messages.
 */
public interface ISigner extends ISignatureVerifier {

    /**
     * Returns the public key.
     *
     * @return
     */
    byte[] getPublicKey();

    /**
     * Signs the message using the secret key and returns a signed message.
     *
     * @param message to be signed
     * @return signature
     */
    byte[] sign(byte[] message);

    /**
     * Verifies the signature for the message.
     *
     * @param message   for which {@code signature} verification is performed
     * @param signature for the {@code message}
     * @return returns true if verification succeeded, otherwise - false.
     */
    default boolean verify(byte[] message, byte[] signature) {
        byte[] publicKey = getPublicKey();
        Objects.requireNonNull(publicKey, "The publick key has not been initialized.");
        return this.verify(message, signature, publicKey);
    }
}
