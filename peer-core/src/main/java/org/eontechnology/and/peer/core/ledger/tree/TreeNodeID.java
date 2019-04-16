package org.eontechnology.and.peer.core.ledger.tree;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.eontechnology.and.peer.core.common.Format;

public class TreeNodeID {
    private final String key;
    private final long index;

    public TreeNodeID(String key) {
        this.key = key;
        this.index = getIndex(Format.convert(key));
    }

    public TreeNodeID(byte[] bytes) {

        byte[] hash = new byte[0];
        try {
            hash = MessageDigest.getInstance("SHA-512").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        key = Format.convert(hash);
        index = getIndex(hash);
    }

    public static TreeNodeID valueOf(String key) {
        if (key == null) {
            return null;
        }
        return new TreeNodeID(key);
    }

    public String getKey() {
        return key;
    }

    public long getIndex() {
        return index;
    }

    private long getIndex(byte[] bytes) {

        if (bytes.length % 8 != 0) {
            bytes = Arrays.copyOf(bytes, ((bytes.length % 8) + 1) * 4);
        }

        BigInteger bigInteger = BigInteger.ZERO;
        for (int i = 0; i < bytes.length; i += 8) {
            BigInteger bi = new BigInteger(1, new byte[] {
                    bytes[i + 7],
                    bytes[i + 6],
                    bytes[i + 5],
                    bytes[i + 4],
                    bytes[i + 3],
                    bytes[i + 2],
                    bytes[i + 1],
                    bytes[i]
            });
            bigInteger = bigInteger.xor(bi);
        }

        return bigInteger.longValue();
    }
}
