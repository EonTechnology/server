package org.eontechology.and.peer.core.data;

/**
 * The base object for calculating the signature generation
 */
public class Generation {
    public final byte[] prevSignature;

    public Generation(byte[] prevSignature) {
        this.prevSignature = prevSignature;
    }
}
