package com.exscudo.peer.core.crypto.ed25519;

import java.util.Collections;
import java.util.Map;

import com.exscudo.peer.core.common.CachedHashMap;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.crypto.Algorithms;
import com.exscudo.peer.core.crypto.ISignatureVerifier;
import com.iwebpp.crypto.TweetNaclFast;

/**
 * {@code ISignatureVerifier} interface implementation using ed25519 signatures.
 */
public class Ed25519SignatureVerifier implements ISignatureVerifier {

    private Map<String, TweetNaclFast.Signature> cache = Collections.synchronizedMap(new CachedHashMap<>(5000));

    @Override
    public String getName() {
        return Algorithms.Ed25519;
    }

    @Override
    public boolean verify(byte[] message, byte[] signature, byte[] publicKey) {

        String key = Format.convert(publicKey);
        TweetNaclFast.Signature sign = cache.get(key);
        if (sign == null) {
            sign = new TweetNaclFast.Signature(publicKey, null);
            cache.put(key, sign);
        }
        return sign.detached_verify(message, signature);
    }
}
