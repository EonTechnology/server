package com.exscudo.eon.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.exscudo.eon.StringConstant;
import com.iwebpp.crypto.TweetNaclFast;

public class Crypto {

	public static byte[] getPublicKey(String secretPhrase) {

		try {

			byte[] digest512 = TweetNaclFast.Hash.sha512(secretPhrase.getBytes(StringConstant.messageEncoding));

			byte[] seed = new byte[32];
			for (int i = 0; i < 32; i++) {
				seed[i] = digest512[i + 32];
			}

			TweetNaclFast.Signature.KeyPair keyPairAcc = TweetNaclFast.Signature.keyPair_fromSeed(seed);
			return keyPairAcc.getPublicKey();

		} catch (UnsupportedEncodingException e) {

			throw new RuntimeException(e);

		}
	}

	public static byte[] sign(byte[] message, String secretPhrase) {

		try {
			
			byte[] digest512 = TweetNaclFast.Hash.sha512(secretPhrase.getBytes(StringConstant.messageEncoding));

			byte[] seed = new byte[32];
			for (int i = 0; i < 32; i++) {
				seed[i] = digest512[i + 32];
			}

			TweetNaclFast.Signature.KeyPair keyPairAcc = TweetNaclFast.Signature.keyPair_fromSeed(seed);
			TweetNaclFast.Signature sign = new TweetNaclFast.Signature(null, keyPairAcc.getSecretKey());

			return sign.detached(message);
			
		} catch (UnsupportedEncodingException e) {

			throw new RuntimeException(e);

		}
	}

	public static boolean verify(byte[] signature, byte[] message, byte[] publicKey) {

		TweetNaclFast.Signature sign = new TweetNaclFast.Signature(publicKey, null);

		return sign.detached_verify(message, signature);

	}

	public static Long getID(byte[] data) {

		try {

			byte[] hash = MessageDigest.getInstance(StringConstant.messageDigestAlgorithm).digest(data);
			BigInteger bigInteger = new BigInteger(1,
					new byte[] { hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0] });
			return bigInteger.longValue();

		} catch (NoSuchAlgorithmException e) {

			throw new RuntimeException(e);

		}
	}
}