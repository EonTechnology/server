package org.eontechnology.and.peer.core.common;

import java.math.BigInteger;

public class Format {
    /**
     * 0x10000000000000000 = 0xFFFFFFFFFFFFFFFF + 1
     */
    public static final BigInteger TWO64 = new BigInteger("18446744073709551616");
    public static final String alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";

    public static byte[] convert(String string) {
        byte[] bytes = new byte[string.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(string.substring(i * 2, i * 2 + 2), 16);
        }

        return bytes;
    }

    public static String convert(byte[] bytes) {
        StringBuilder string = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {

            int number = bytes[i] & 0xFF;
            string.append(Format.alphabet.charAt(number >> 4)).append(Format.alphabet.charAt(number & 0xF));
        }
        return string.toString();
    }
}
