package com.exscudo.eon;

public class SecretPhraseProvider {
	private static String secretPhrase = null;

	public static synchronized String getSecretPhrase() {

		if (secretPhrase != null) {
			return secretPhrase;
		}

		//
		// ATTENTION. skip...
		//
		throw new RuntimeException();
	}

}
