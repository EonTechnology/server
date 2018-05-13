package com.exscudo.peer.core.crypto.signatures;

import java.util.Collections;
import java.util.Map;

import com.exscudo.peer.core.common.CachedHashMap;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.crypto.ISignature;
import com.iwebpp.crypto.TweetNaclFast;

public class Ed25519Signature implements ISignature {
    private final Map<String, TweetNaclFast.Signature> cache = Collections.synchronizedMap(new CachedHashMap<>(5000));

    @Override
    public String getName() {
        return "Ed25519";
    }

    @Override
    public KeyPair getKeyPair(byte[] seed) {

        TweetNaclFast.Signature.KeyPair pair = TweetNaclFast.Signature.keyPair_fromSeed(seed);
        return new KeyPair(pair.getSecretKey(), pair.getPublicKey());
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

    @Override
    public byte[] sign(byte[] messageToSign, byte[] secretKey) {

        TweetNaclFast.Signature sign = new TweetNaclFast.Signature(null, secretKey);
        return sign.detached(messageToSign);
    }
}
