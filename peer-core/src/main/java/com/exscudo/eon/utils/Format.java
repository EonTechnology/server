package com.exscudo.eon.utils;

import java.math.BigInteger;

import com.exscudo.eon.exceptions.DecodeException;

public class Format {

	/**
	 * 0x10000000000000000 = 2*0xFFFFFFFFFFFFFFFF
	 */
	public static final BigInteger two64 = new BigInteger("18446744073709551616");
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

			int number;
			string.append(Format.alphabet.charAt((number = bytes[i] & 0xFF) >> 4))
					.append(Format.alphabet.charAt(number & 0xF));

		}
		return string.toString();
	}

	public static String convert(long objectId) {

		BigInteger id = BigInteger.valueOf(objectId);
		if (objectId < 0) {
			id = id.add(Format.two64);
		}
		return id.toString();
	}

	private static final String ACCOUNT_PREFIX = "EON";
	private static final String BLOCK_PREFIX = "EON-B";
	private static final String TRANSACTION_PREFIX = "EON-T";
	private static final String BLANK_PREFIX = "EON-TB";

	public static long AccountIdDecode(String id) throws DecodeException {
		return UserFriendlyID.Decode(id, ACCOUNT_PREFIX);
	}

	public static String AccountIdEncode(long id) {
		return UserFriendlyID.Encode(id, ACCOUNT_PREFIX);
	}

	public static long BlockIdDecode(String id) throws DecodeException {
		return UserFriendlyID.Decode(id, BLOCK_PREFIX);
	}

	public static String BlockIdEncode(long id) {
		return UserFriendlyID.Encode(id, BLOCK_PREFIX);
	}

	public static long TransactionIdDecode(String id) throws DecodeException {
		return UserFriendlyID.Decode(id, TRANSACTION_PREFIX);
	}

	public static String TransactionIdEncode(long id) {
		return UserFriendlyID.Encode(id, TRANSACTION_PREFIX);
	}
	
	public static long TransactionBlankIdDecode(String id) throws DecodeException {
		return UserFriendlyID.Decode(id, BLANK_PREFIX);
	}
	
	public static String TransactionBlankIdEncode(long id) {
		return UserFriendlyID.Encode(id, BLANK_PREFIX);
	}

	static class UserFriendlyID {

		private static final String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

		static String Encode(long plain, final String prefix) {

			BigInteger id = BigInteger.valueOf(plain);
			if (plain < 0) {
				id = id.add(Format.two64);
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

		private final static int ID_LEN = 18;

		static long Decode(String idString, final String prefix) throws DecodeException {

			idString = idString.trim().toUpperCase();

			if (idString.length() != ID_LEN + prefix.length()) {
				throw new DecodeException(idString);
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
				throw new DecodeException(idString);
			}

			return id;
		}
	}

	
}
