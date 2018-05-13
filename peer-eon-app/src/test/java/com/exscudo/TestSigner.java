package com.exscudo;

import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.Signer;
import com.exscudo.peer.core.data.identifier.BlockID;

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
