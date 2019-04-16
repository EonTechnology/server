package org.eontechnology.and.peer.core.crypto;

import java.io.Serializable;

/**
 * Base class for an object having an EDS.
 */
public class SignedObject implements Serializable {
    private static final long serialVersionUID = -5465601245276980043L;

    protected byte[] signature;

    private boolean verifiedState = false;

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public void setVerifiedState() {
        verifiedState = true;
    }

    public void resetVerifiedState() {
        verifiedState = false;
    }

    public boolean isVerified() {
        return verifiedState;
    }
}
