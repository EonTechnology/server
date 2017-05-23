package com.exscudo.eon.peer.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

import com.exscudo.eon.StringConstant;
import com.exscudo.eon.utils.Crypto;

public class BlockFactory {

	public static Builder newBlock() {
		return new Builder();
	}

	public static class Builder {
		private final Block block = new Block();

		Builder() {
			block.version = 1;
		}

		public Builder timestamp(int timestamp) {

			block.timestamp = timestamp;
			return this;

		}

		/**
		 * 
		 * @param payload
		 * @return
		 * @throws IllegalArgumentException
		 *             thrown if can not find transaction.
		 */
		public Builder withPayload(Map<Long, Transaction> payload) {

			int payloadLength = 0;
			long[] ids = new long[payload.size()];

			int i = 0;
			for (Map.Entry<Long, Transaction> entry : payload.entrySet()) {

				Transaction tx = entry.getValue();
				if (tx == null) {
					throw new IllegalArgumentException();
				}
				payloadLength += tx.getBytes().length;
				ids[i++] = entry.getKey();

			}

			Arrays.sort(ids);
			block.setTransactions(ids);

			MessageDigest digest;
			try {
				digest = MessageDigest.getInstance(StringConstant.messageDigestAlgorithm);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			for (i = 0; i < payload.size(); i++) {
				digest.update(payload.get(ids[i]).getBytes());
			}

			block.payload = new Block.Payload(ids.length, payloadLength, digest.digest());
			return this;

		}

		public Block build(Block previousBlock, String secretPhrase) {

			if (previousBlock == null) {
				throw new NullPointerException("previousBlock");
			}

			if (secretPhrase == null) {
				throw new NullPointerException("secretPhrase");
			}

			byte[] generationSignature = Crypto.sign(previousBlock.getGenerationSignature(), secretPhrase);

			byte[] data = block.getBytes();
			byte[] data2 = new byte[data.length - 64];
			System.arraycopy(data, 0, data2, 0, data2.length);

			block.generationSignature = generationSignature;
			block.previousBlock = previousBlock.getID();
			block.generatorPublicKey = Crypto.getPublicKey(secretPhrase);
			block.blockSignature = Crypto.sign(data2, secretPhrase);

			return block;

		}
	}
}
