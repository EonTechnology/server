package com.exscudo.peer.core.crypto;

import java.util.Objects;

import com.exscudo.peer.core.common.BencodeFormatter;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.IFormatter;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.crypto.mapper.ObjectMapper;
import com.exscudo.peer.core.crypto.signatures.Ed25519Signature;
import com.exscudo.peer.core.data.identifier.BlockID;

public class Signer implements ISigner {
    private final ISignature signature;
    private final ISignature.KeyPair keyPair;

    public Signer(ISignature signature, byte[] seed) {
        this.signature = signature;
        this.keyPair = signature.getKeyPair(seed);
    }

    public static Signer createNew(String seed) {
        Signer signer = null;
        try {
            byte[] bytes = Format.convert(seed);
            signer = new Signer(new Ed25519Signature(), bytes);
        } catch (Throwable t) {
            Loggers.warning(Signer.class, "Wrong seed string. ISigner not created.");
        }

        return signer;
    }

    @Override
    public byte[] getPublicKey() {
        return keyPair.publicKey;
    }

    @Override
    public byte[] sign(Object obj, BlockID networkID) {

        ObjectMapper mapper = new ObjectMapper(Objects.requireNonNull(networkID));
        IFormatter formatter = new BencodeFormatter();

        byte[] message;
        try {
            message = formatter.getBytes(mapper.convert(obj));
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        return this.signature.sign(message, keyPair.secretKey);
    }
}