package com.exscudo.peer.eon.crypto;

import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.core.utils.Loggers;
import com.iwebpp.crypto.TweetNaclFast;

/**
 * {@code ISigner} interface implementation using Ed25519 signatures.
 *
 */
public class Ed25519Signer implements ISigner {

	public static Ed25519Signer createNew(String seedStr) {
		Ed25519Signer signer = null;
		try {
			signer = new Ed25519Signer(seedStr);
		} catch (Throwable t) {
			Loggers.warning(Ed25519Signer.class, "Wrong seed string. ISigner not created.", t);
		}

		return signer;
	}

	private final TweetNaclFast.Signature.KeyPair pair;

	public Ed25519Signer(String seedStr) {
		byte[] seed = Format.convert(seedStr);
		pair = TweetNaclFast.Signature.keyPair_fromSeed(seed);
	}

	@Override
	public String getName() {
		return Algorithms.Ed25519;
	}

	@Override
	public byte[] getPublicKey() {
		return pair.getPublicKey();
	}

	@Override
	public byte[] sign(byte[] messageToSign) {
		TweetNaclFast.Signature sign = new TweetNaclFast.Signature(null, pair.getSecretKey());
		return sign.detached(messageToSign);
	}

	@Override
	public boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
		TweetNaclFast.Signature sign = new TweetNaclFast.Signature(publicKey, null);
		return sign.detached_verify(message, signature);
	}

}
