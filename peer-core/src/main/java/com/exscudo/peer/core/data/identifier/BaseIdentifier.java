package com.exscudo.peer.core.data.identifier;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.exscudo.peer.core.common.Format;

public class BaseIdentifier implements Serializable {

    private final String scheme;
    private long id;

    public BaseIdentifier(long id, String scheme) {
        this.id = id;
        this.scheme = scheme;
    }

    public BaseIdentifier(String id, String scheme) {
        this.id = UserFriendlyID.Decode(id, scheme);
        this.scheme = scheme;
    }

    public BaseIdentifier(byte[] bytes, String scheme) {
        this.id = MathID.pick(bytes);
        this.scheme = scheme;
    }

    public BaseIdentifier(byte[] bytes, int timestamp, String scheme) {
        this.id = MathID.pick(bytes, timestamp);
        this.scheme = scheme;
    }

    public static <T extends BaseIdentifier> long getValueOrRef(T id) {
        if (id == null) {
            return 0L;
        }
        return id.getValue();
    }

    public long getValue() {
        return id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof BaseIdentifier) {
            BaseIdentifier t = (BaseIdentifier) obj;
            return scheme.equals(t.scheme) && id == t.id;
        }

        return false;
    }

    @Override
    public String toString() {
        return UserFriendlyID.Encode(id, scheme);
    }

    static class MathID {
        private static final String MessageDigestAlgorithm = "SHA-512";

        public static long pick(byte[] bytes, int timestamp) {
            try {
                byte[] hash = MessageDigest.getInstance(MessageDigestAlgorithm).digest(bytes);

                BigInteger bigInteger = BigInteger.ZERO;
                for (int i = 0; i < hash.length; i += 4) {
                    BigInteger bi = new BigInteger(1, new byte[] {hash[i + 3], hash[i + 2], hash[i + 1], hash[i]});
                    bigInteger = bigInteger.xor(bi);
                }

                long val = ((long) bigInteger.intValue() << 32) | ((long) timestamp & 0xFFFFFFFFL);

                return val;
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        public static long pick(byte[] bytes) {
            try {
                byte[] hash = MessageDigest.getInstance(MessageDigestAlgorithm).digest(bytes);

                BigInteger bigInteger = BigInteger.ZERO;
                for (int i = 0; i < hash.length; i += 8) {
                    BigInteger bi = new BigInteger(1, new byte[] {
                            hash[i + 7],
                            hash[i + 6],
                            hash[i + 5],
                            hash[i + 4],
                            hash[i + 3],
                            hash[i + 2],
                            hash[i + 1],
                            hash[i]
                    });
                    bigInteger = bigInteger.xor(bi);
                }

                return bigInteger.longValue();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class UserFriendlyID {

        private static final String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        private final static int ID_LEN = 18;

        static String Encode(long plain, final String prefix) {

            BigInteger id = BigInteger.valueOf(plain);
            if (plain < 0) {
                id = id.add(Format.TWO64);
            }

            BigInteger chs = BigInteger.ZERO;
            BigInteger andVal = BigInteger.valueOf(0x3FF);
            BigInteger tmp = id;
            while (tmp.compareTo(BigInteger.ZERO) > 0) {
                chs = chs.xor(tmp.and(andVal));
                tmp = tmp.shiftRight(10);
            }
            id = id.or(chs.shiftLeft(64));
            id = id.setBit(74);

            StringBuilder idStr = new StringBuilder(prefix);
            andVal = BigInteger.valueOf(0x1F);

            for (int i = 0; i < 15; i++) {
                if ((i % 5) == 0) {
                    idStr.append('-');
                }

                idStr.append(alphabet.charAt(id.and(andVal).intValue()));
                id = id.shiftRight(5);
            }

            return idStr.toString();
        }

        static long Decode(String idString, final String prefix) throws IllegalArgumentException {

            idString = idString.trim().toUpperCase();

            if (idString.length() != ID_LEN + prefix.length()) {
                throw new IllegalArgumentException(idString);
            }

            BigInteger idB = BigInteger.ZERO;
            for (int i = ID_LEN + prefix.length() - 1; i > prefix.length(); i--) {
                int p = alphabet.indexOf(idString.charAt(i));
                if (p >= 0) {

                    idB = idB.shiftLeft(5);
                    idB = idB.add(BigInteger.valueOf(p));
                }
            }
            for (int i = 64; i < 75; i++) {
                idB = idB.clearBit(i);
            }

            long id = idB.longValue();

            if (!idString.equals(Encode(id, prefix))) {
                throw new IllegalArgumentException(idString);
            }

            return id;
        }
    }
}
