package org.eontechnology.and.peer.core.crypto;

public interface ISignature {

    /**
     * Returns the signature algorithm name.
     *
     * @return name
     */
    String getName();

    /**
     * @param seed
     * @return
     */
    KeyPair getKeyPair(byte[] seed);

    /**
     * Check the digital signature for the specified {@code message}
     *
     * @param message   for which {@code signature} verification is performed
     * @param signature for the {@code message}
     * @param publicKey for verifying the {@code signature}
     * @return true if verification succeeded, otherwise - false.
     * @throws NullPointerException     if the {@code message} or {@code signature} or {@code publicKey}
     *                                  is null
     * @throws IllegalArgumentException
     */
    boolean verify(byte[] message, byte[] signature, byte[] publicKey);

    /**
     * @param messageToSign
     * @param secretKey
     * @return
     */
    byte[] sign(byte[] messageToSign, byte[] secretKey);

    class KeyPair {

        public final byte[] secretKey;
        public final byte[] publicKey;

        public KeyPair(byte[] secretKey, byte[] publicKey) {
            this.secretKey = new byte[secretKey.length];
            this.publicKey = new byte[publicKey.length];

            System.arraycopy(secretKey, 0, this.secretKey, 0, secretKey.length);
            System.arraycopy(publicKey, 0, this.publicKey, 0, publicKey.length);
        }
    }
}
