package com.exscudo.peer.core.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import com.dampcake.bencode.BencodeOutputStream;

/**
 * Converting a Map to Bencode message.
 */
public class BencodeFormatter implements IFormatter {

    public byte[] getBytes(Map<String, Object> map) {
        Objects.requireNonNull(map);
        try {

            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {

                try (BencodeOutputStream bencodeStream = new BencodeOutputStream(outStream)) {
                    bencodeStream.writeDictionary(map);
                }

                String str = outStream.toString();
                return str.toUpperCase().getBytes();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
