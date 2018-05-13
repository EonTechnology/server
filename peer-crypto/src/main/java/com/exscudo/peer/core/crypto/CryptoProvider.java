package com.exscudo.peer.core.crypto;

import java.util.Objects;

import com.exscudo.peer.core.common.BencodeFormatter;
import com.exscudo.peer.core.common.IFormatter;
import com.exscudo.peer.core.crypto.mapper.ObjectMapper;
import com.exscudo.peer.core.crypto.signatures.Ed25519Signature;
import com.exscudo.peer.core.data.identifier.BlockID;

public class CryptoProvider {
    private final ISignature signature;

    public CryptoProvider(ISignature signature) {
        this.signature = signature;
    }

    public static CryptoProvider getInstance() {
        return new CryptoProvider(new Ed25519Signature());
    }

    public ISignature getSignature() {
        return signature;
    }

    /**
     * Check the digital signature for the specified {@code obj}
     *
     * @param obj
     * @param signature
     * @param publicKey
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public boolean verify(Object obj, BlockID networkID, byte[] signature, byte[] publicKey) {

        ObjectMapper mapper = new ObjectMapper(Objects.requireNonNull(networkID));
        IFormatter formatter = new BencodeFormatter();

        byte[] message;
        try {
            message = formatter.getBytes(mapper.convert(obj));
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        return this.signature.verify(message, signature, publicKey);
    }
}
