package com.exscudo.peer.core.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import com.dampcake.bencode.BencodeOutputStream;
import com.exscudo.peer.core.crypto.mapper.ObjectMapper;

/**
 * Converting an object to Bencode message.
 */
class BencodeMessage {

	public static byte[] getBytes(Object object) {
		Objects.requireNonNull(object);
		try {
			Map<String, Object> map = ObjectMapper.getBytes(object, true);
			return convertToBencode(map);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static int getLength(Object object) {
		Objects.requireNonNull(object);
		try {
			Map<String, Object> map = ObjectMapper.getBytes(object, false);
			byte[] bytes = convertToBencode(map);
			return bytes.length;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] convertToBencode(Map<String, Object> map) throws IOException {

		try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {

			try (BencodeOutputStream bencodeStream = new BencodeOutputStream(outStream)) {
				bencodeStream.writeDictionary(map);
			}

			String str = outStream.toString();
			return str.toUpperCase().getBytes();

		}

	}

}
