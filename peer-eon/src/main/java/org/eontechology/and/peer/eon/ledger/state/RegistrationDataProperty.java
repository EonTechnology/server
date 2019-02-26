package org.eontechology.and.peer.eon.ledger.state;

public class RegistrationDataProperty {

    private final byte[] publicKey;

    public RegistrationDataProperty(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }
}
