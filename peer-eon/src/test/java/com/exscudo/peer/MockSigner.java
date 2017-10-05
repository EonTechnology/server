package com.exscudo.peer;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import com.exscudo.peer.eon.crypto.ISigner;

public class MockSigner implements ISigner {
	private Random _random = new SecureRandom();

	private final LinkedList<Item> items = new LinkedList<>();
	public byte[] publicKey;

	public MockSigner() {
		publicKey = generate(32);
	}

	public MockSigner(long seed) {
		_random = new Random(seed);
		publicKey = generate(32);
	}

	private byte[] generate(int length) {
		byte[] bytes = new byte[length];
		_random.nextBytes(bytes);
		return bytes;
	}

	@Override
	public String getName() {
		return "test";
	}

	@Override
	public byte[] getPublicKey() {
		return publicKey;
	}

	@Override
	public boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
		byte[] sig = getSignature(message);
		if (sig == null) {
			return false;
		}
		return Arrays.equals(sig, signature);
	}

	@Override
	public byte[] sign(byte[] message) {
		byte[] signature = getSignature(message);
		if (signature == null) {
			signature = generate(64);
			items.add(new Item(message, signature));
		}
		return signature;
	}

	private byte[] getSignature(byte[] message) {
		for (Item i : items) {
			if (Arrays.equals(i.message, message))
				return i.signature;
		}
		return null;
	}

	private static class Item {
		public final byte[] message;
		public final byte[] signature;

		public Item(byte[] message, byte[] signature) {
			this.message = message;
			this.signature = signature;
		}
	}
}
