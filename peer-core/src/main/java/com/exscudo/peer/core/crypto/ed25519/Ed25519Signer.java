package com.exscudo.peer.core.crypto.ed25519;

import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.crypto.Algorithms;
import com.exscudo.peer.core.crypto.ISigner;
import com.iwebpp.crypto.TweetNaclFast;

/**
 * {@code ISigner} interface implementation using ed25519 signatures.
 */
public class Ed25519Signer implements ISigner {

    private final TweetNaclFast.Signature.KeyPair pair;

    public Ed25519Signer(String seedStr) {
        byte[] seed = Format.convert(seedStr);
        pair = TweetNaclFast.Signature.keyPair_fromSeed(seed);
    }

    public static Ed25519Signer createNew(String seedStr) {
        Ed25519Signer signer = null;
        try {
            signer = new Ed25519Signer(seedStr);
        } catch (Throwable t) {
            Loggers.warning(Ed25519Signer.class, "Wrong seed string. ISigner not created.", t);
        }

        return signer;
    }

    @Override
    public String getName() {
        return Algorithms.Ed25519;
    }

    @Override
    public boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
        TweetNaclFast.Signature sign = new TweetNaclFast.Signature(publicKey, null);
        return sign.detached_verify(message, signature);
    }

    @Override
    public byte[] getPublicKey() {
        return pair.getPublicKey();
    }

    @Override
    public byte[] sign(byte[] messageToSign) {
        TweetNaclFast.Signature sign = new TweetNaclFast.Signature(null, pair.getSecretKey());
        return sign.detached(messageToSign);
    }
}
