package com.exscudo.eon.peer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.exscudo.eon.StringConstant;
import com.exscudo.eon.exceptions.DecodeException;
import com.exscudo.eon.utils.Crypto;
import com.exscudo.eon.utils.Format;

public class Hallmark {

	final byte[] publicKey;
	final String host;
	final int date;

	private Hallmark(byte[] publicKey, String host, int date) {

		this.publicKey = publicKey;
		this.host = host;
		this.date = date;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public String getHost() {
		return host;
	}

	public int getDate() {
		return date;
	}

	public static Hallmark parse(String hallmark) throws DecodeException {

		try {

			byte[] hallmarkBytes = Format.convert(hallmark);
			ByteBuffer buffer = ByteBuffer.wrap(hallmarkBytes);
			buffer.order(ByteOrder.LITTLE_ENDIAN);

			byte[] publicKey = new byte[32];
			buffer.get(publicKey);

			byte[] hostBytes = new byte[buffer.getShort()];
			buffer.get(hostBytes);
			String host = new String(hostBytes, StringConstant.messageEncoding);
			if (host == null || host.length() == 0 || host.length() > 100) {
				throw new Exception("Invalid host.");
			}

			int date = buffer.getInt();

			byte[] signature = new byte[64];
			buffer.get(signature);

			byte[] data = new byte[hallmarkBytes.length - 64];
			System.arraycopy(hallmarkBytes, 0, data, 0, data.length);
			if (!Crypto.verify(signature, data, publicKey)) {
				throw new Exception("Invalid signature.");
			}

			return new Hallmark(publicKey, host, date);

		} catch (Exception e) {
			throw new DecodeException(hallmark, e);
		}
	}
}
