package com.exscudo.peer.eon.state;

public class RegistrationData {

	private byte[] publicKey;

	public RegistrationData(byte[] publicKey) {
		this.publicKey = publicKey;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

}
