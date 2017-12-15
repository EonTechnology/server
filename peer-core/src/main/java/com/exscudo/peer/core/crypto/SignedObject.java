package com.exscudo.peer.core.crypto;

import java.io.Serializable;

/**
 * Base class for an object having an EDS.
 *
 */
public class SignedObject implements Serializable {
	private static final long serialVersionUID = -5465601245276980043L;

	protected byte[] signature;
	private boolean signatureOK = false;

	public byte[] getSignature() {
		return signature;
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	/**
	 * Verifies a EDS using an algorithm defined in {@link CryptoProvider}
	 * 
	 * @param publicKey
	 *            for verifying the signature
	 * @return true if signature valid, otherwise - false
	 */
	public boolean verifySignature(byte[] publicKey) {

		if (!this.signatureOK) {
			this.signatureOK = CryptoProvider.getInstance().verifySignature(this, publicKey);
		}
		return this.signatureOK;

	}

	/**
	 * Convert object to byte array
	 *
	 * @return bytes
	 */
	public byte[] getBytes() {
		return CryptoProvider.getInstance().getBytes(this);
	}

	@Override
	public String toString() {
		return new String(CryptoProvider.getInstance().getBytes(this));
	}

}
