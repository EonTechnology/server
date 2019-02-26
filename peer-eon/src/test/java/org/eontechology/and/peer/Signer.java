package org.eontechology.and.peer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eontechology.and.peer.core.crypto.ISigner;
import org.eontechology.and.peer.core.data.identifier.BlockID;

public class Signer implements ISigner {
    private byte[] publicKey;

    public Signer(String seed) {
        if (seed.length() != 64) {
            throw new IllegalArgumentException();
        }
        try {
            publicKey = MessageDigest.getInstance("SHA-256").digest(seed.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] getPublicKey() {
        return publicKey;
    }

    @Override
    public byte[] sign(Object obj, BlockID networkID) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            try (ObjectOutput out = new ObjectOutputStream(stream)) {
                out.writeObject(obj);
                out.flush();
                return sign(stream.toByteArray());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] sign(byte[] message) {
        try {
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            sha512.update(publicKey);
            sha512.update(message);
            return sha512.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
