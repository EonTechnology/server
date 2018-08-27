package com.exscudo.peer.core.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.dampcake.bencode.BencodeOutputStream;

/**
 * Converting a Map to Bencode message.
 */
class BencodeFormatter implements IFormatter {
    private final Locale locale;

    public BencodeFormatter() {
        this(Locale.ENGLISH);
    }

    public BencodeFormatter(Locale locale) {
        this.locale = locale;
    }

    @Override
    public byte[] getBytes(Map<String, Object> map) {
        Objects.requireNonNull(map);
        try {

            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {

                try (BencodeOutputStream bencodeStream = new BencodeOutputStream(outStream, Charset.forName("UTF-8"))) {
                    bencodeStream.writeDictionary(map);
                }

                String str = outStream.toString("UTF-8");
                return str.toUpperCase(locale).getBytes();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
