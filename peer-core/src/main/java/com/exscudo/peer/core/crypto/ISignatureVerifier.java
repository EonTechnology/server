package com.exscudo.peer.core.crypto;

/**
 * Interface {@code ISignatureVerifier} represents an abstraction for an object
 * that performs an EDS validation.
 */
public interface ISignatureVerifier {

    /**
     * Returns the signature algorithm name.
     *
     * @return name
     */
    String getName();

    /**
     * Check the digital signature for the specified {@code message}
     *
     * @param message   for which signature verification is performed
     * @param signature for the {@code message}
     * @param publicKey for verifying the signature
     * @return returns true if verification succeeded, otherwise - false.
     * @throws NullPointerException if the specified parameters is null
     */
    boolean verify(byte[] message, byte[] signature, byte[] publicKey);
}
