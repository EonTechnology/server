package org.eontechology.and;

import org.eontechology.and.peer.core.crypto.ISigner;
import org.eontechology.and.peer.core.crypto.Signer;
import org.eontechology.and.peer.core.data.identifier.BlockID;

public class TestSigner implements ISigner {
    private Signer signer;

    public TestSigner(String seed) {
        signer = Signer.createNew(seed);
    }

    @Override
    public byte[] getPublicKey() {
        return signer.getPublicKey();
    }

    @Override
    public byte[] sign(Object obj, BlockID networkID) {
        return signer.sign(obj, networkID);
    }
}
