package com.exscudo.peer.core.crypto;

import java.util.HashMap;
import java.util.Objects;

/**
 * Provide cryptography algorithms
 */
public class CryptoProvider {
	private static CryptoProvider instance = new CryptoProvider();

	private HashMap<String, ISignatureVerifier> verifierSet;
	private String defaultName;

	private CryptoProvider() {
		verifierSet = new HashMap<>();
	}

	public static CryptoProvider getInstance() {
		return instance;
	}

	/**
	 * Set the algorithm used by default to verify the signature.
	 *
	 * @param name
	 *            of the algorithm used by default
	 */
	public void setDefaultProvider(String name) {
		defaultName = name;
	}

	/**
	 * Adds or updates a implementation of the algorithm to verify the signature.
	 *
	 * @param provider
	 *            to add to the list of known. can not be null.
	 */
	public void addProvider(ISignatureVerifier provider) {
		Objects.requireNonNull(provider);
		verifierSet.put(provider.getName(), provider);
	}

	/**
	 * Finds and returns the {@code ISignatureVerifier} instance by name.
	 *
	 * @param name
	 *            to search
	 * @return {@code ISignatureVerifier} instance or null if not exist
	 */
	public ISignatureVerifier getProvider(String name) {
		return verifierSet.get(name);
	}

	/**
	 * Returns default provider
	 *
	 * @return return verifier or null if not exist
	 */
	public ISignatureVerifier getProvider() {
		return verifierSet.get(defaultName);
	}

	/**
	 * Check the digital signature for the specified {@code message}
	 *
	 * @param algorithm
	 *            used to sign {@code messages}
	 * @param message
	 *            for which {@code signature} verification is performed
	 * @param signature
	 *            for the {@code message}
	 * @param publicKey
	 *            for verifying the {@code signature}
	 * @return true if verification succeeded, otherwise - false.
	 * @throws NullPointerException
	 *             if the {@code message} or {@code signature} or {@code publicKey}
	 *             is null
	 * @throws UnsupportedOperationException
	 *             if the specified {@code  algorithm} is not supported
	 */
	public boolean verifySignature(String algorithm, byte[] message, byte[] signature, byte[] publicKey) {

		ISignatureVerifier verifier = this.getProvider(algorithm);
		if (verifier == null) {
			throw new UnsupportedOperationException();
		}
		return verifier.verify(message, signature, publicKey);

	}

	/**
	 * Check the digital signature for the specified {@code message}
	 *
	 * @param message
	 *            for which {@code signature} verification is performed
	 * @param signature
	 *            for the {@code message}
	 * @param publicKey
	 *            for verifying the {@code signature}
	 * @return true if verification succeeded, otherwise - false.
	 * @throws NullPointerException
	 *             if the {@code message} or {@code signature} or {@code publicKey}
	 *             is null
	 * @throws UnsupportedOperationException
	 *             if the default algorithm is not specified
	 */
	public boolean verifySignature(byte[] message, byte[] signature, byte[] publicKey) {
		return verifySignature(defaultName, message, signature, publicKey);
	}

	/**
	 * Check the digital signature for the specified {@code object}.
	 *
	 * @param algorithm
	 *            used to sign
	 * @param object
	 *            for which a verification is performed
	 * @param publicKey
	 *            used by the signer
	 * @return true if verification succeeded, otherwise - false.
	 * @throws NullPointerException
	 *             if the {@code object} or {@code publicKey} is null. Also, if
	 *             there the signature field is not defined.
	 * @throws UnsupportedOperationException
	 *             if the specified {@code  algorithm} is not supported
	 */
	public boolean verifySignature(String algorithm, SignedObject object, byte[] publicKey) {

		Objects.requireNonNull(object);
		ISignatureVerifier verifier = this.getProvider(algorithm);
		byte[] data = getBytes(object);
		byte[] signature = object.getSignature();
		return verifier.verify(data, signature, publicKey);

	}

	/**
	 * Check the digital signature for the specified {@code object}.
	 *
	 * @param object
	 *            for which a verification is performed
	 * @param publicKey
	 *            used by the signer
	 * @return true if verification succeeded, otherwise - false.
	 * @throws NullPointerException
	 *             if the {@code object} or {@code publicKey} is null. Also, if
	 *             there the signature field is not defined.
	 * @throws UnsupportedOperationException
	 *             if the default algorithm is not specified
	 */
	public boolean verifySignature(SignedObject object, byte[] publicKey) {
		return verifySignature(defaultName, object, publicKey);
	}

	/**
	 * Convert object to byte array
	 * 
	 * @param obj
	 *            object to convert
	 * @return bytes
	 */
	byte[] getBytes(SignedObject obj) {
		return BencodeMessage.getBytes(obj);
	}

	/**
	 * Get object length
	 * 
	 * @param obj
	 *            object
	 * @return length
	 */
	int getLength(SignedObject obj) {
		return BencodeMessage.getLength(obj);
	}
}
