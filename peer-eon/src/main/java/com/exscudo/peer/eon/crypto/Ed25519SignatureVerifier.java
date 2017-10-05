package com.exscudo.peer.eon.crypto;

import com.exscudo.peer.core.crypto.ISignatureVerifier;
import com.iwebpp.crypto.TweetNaclFast;

/**
 * {@code ISignatureVerifier} interface implementation using Ed25519 signatures.
 *
 */
public class Ed25519SignatureVerifier implements ISignatureVerifier {

	@Override
	public String getName() {
		return "Ed25519";
	}

	@Override
	public boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
		TweetNaclFast.Signature sign = new TweetNaclFast.Signature(publicKey, null);
		return sign.detached_verify(message, signature);
	}

}
