package org.eontechnology.and.peer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.crypto.ISignature;

public class TestSignature implements ISignature {
    @Override
    public String getName() {
        return "test";
    }

    @Override
    public KeyPair getKeyPair(byte[] seed) {
        Signer s = new Signer(Format.convert(seed));
        return new KeyPair(seed, s.getPublicKey());
    }

    @Override
    public boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
        try {
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            sha512.update(publicKey);
            sha512.update(message);
            return Arrays.equals(sha512.digest(), signature);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    @Override
    public byte[] sign(byte[] messageToSign, byte[] secretKey) {
        Signer s = new Signer(Format.convert(secretKey));
        return s.sign(messageToSign);
    }
}
