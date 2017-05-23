package com.exscudo.eon.peer.data;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.exscudo.eon.utils.Crypto;

public class Block implements Serializable {
	private static final long serialVersionUID = -8705643178030126672L;
	public static final int BLOCK_HEADER_LENGTH = 4 + 4 + 72 + 8 + 64 + 32 + 64; // 248
																					// bytes

	public static class Payload implements Serializable {
		private static final long serialVersionUID = 3635322440392533089L;
		
		private int numberOfTransactions;
		private int length;
		private byte[] hash;

		public Payload(int numberOfTransactions, int length, byte[] payload) {
			this.numberOfTransactions = numberOfTransactions;
			this.length = length;
			this.hash = payload;
		}

		/**
		 * Returns the number of transactions.
		 * 
		 * @return
		 */
		public int numberOfTransactions() {
			return numberOfTransactions;
		}

		/**
		 * Returns the buffer length, in bytes, required to download all
		 * transactions
		 * 
		 * @return
		 */
		public int length() {
			return length;
		}

		/**
		 * Returns Transactions Hash
		 * 
		 * <code>
		 * MessageDigest digest;
		 * for (Transaction tx : collection ) {
		 * 		digest.update(tx.getBytes());
		 * }
		 * hash = digest.digest();
		 * </code>
		 * 
		 * @return
		 */
		public byte[] hash() {
			return hash;
		}
	}

	int version;
	int timestamp;
	Payload payload;
	long previousBlock;
	byte[] generationSignature;
	byte[] generatorPublicKey;
	byte[] blockSignature;

	private int height;
	private long nextBlock;
	private long[] transactions;
	private BigInteger cumulativeDifficulty;

	Block() {
	}

	public Block(int version, int timestamp, Payload payload, long previousBlock, byte[] generatorPublicKey,
			byte[] generationSignature, byte[] blockSignature) {

		this.version = version;
		this.timestamp = timestamp;
		this.payload = payload;
		this.previousBlock = previousBlock;
		this.generatorPublicKey = generatorPublicKey;
		this.generationSignature = generationSignature;
		this.blockSignature = blockSignature;
	}

	/**
	 * Returns the version of the block.
	 * 
	 * @return
	 */
	public int getBlockVersion() {
		return version;
	}

	/**
	 * Returns the time of the creation of the block.
	 * 
	 * @return
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Returns true if the creation time of the transaction is in the future.
	 * 
	 * @param timestamp
	 *            Point in time for which the check is carried out.
	 * @return
	 */
	public boolean isFuture(int timestamp) {
		return getTimestamp() > timestamp;
	}

	/**
	 * 
	 * @return
	 */
	public Payload getPayload() {
		return payload;
	}

	/**
	 * Returns the ID of the previous block.
	 * 
	 * @return
	 */
	public long getPreviousBlock() {
		return previousBlock;
	}

	/**
	 * Returns the special field "signature generation" in order to prove
	 * possession address
	 *
	 * <code>
	 * Crypto.sign(prevBlock.getGenerationSignature(), secretPhrase);
	 * </code>
	 * 
	 * @return
	 */
	public byte[] getGenerationSignature() {
		return generationSignature;
	}

	/**
	 * Returns the public key of the creator.
	 *
	 * @return
	 */
	public byte[] getGeneratorPublicKey() {
		return generatorPublicKey;
	}

	/**
	 * Returns the block signature.
	 *
	 * @return
	 */
	public byte[] getSignature() {
		return blockSignature;
	}

	/**
	 * Returns the block data as a byte array.
	 * 
	 * @return
	 */
	public byte[] getBytes() {

		ByteBuffer buffer = ByteBuffer.allocate(BLOCK_HEADER_LENGTH);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(version);
		buffer.putInt(timestamp);
		buffer.putLong(previousBlock);
		buffer.put(generationSignature);
		buffer.putInt(payload.numberOfTransactions());
		buffer.putInt(payload.length());
		buffer.put(payload.hash());
		buffer.put(generatorPublicKey);
		buffer.put(blockSignature);

		return buffer.array();
	}

	/**
	 * Returns the transaction ID.
	 * 
	 * @return
	 */
	public long getID() {

		return Crypto.getID(getBytes());
	}

	/**
	 * 
	 * @return
	 */
	public boolean validateSignature() {

		byte[] data = getBytes();
		byte[] data2 = new byte[data.length - 64];
		System.arraycopy(data, 0, data2, 0, data2.length);

		return Crypto.verify(getSignature(), data2, getGeneratorPublicKey());
	}

	/**
	 * Returns the block number. Not used.
	 *
	 * @return
	 */
	public int getIndex() {

		// TODO:
		return 0;
	}

	public void setIndex(int value) {
		// TODO:
	}

	/**
	 * Returns the block position in the chain.
	 * 
	 * @return
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Sets the position in the chain.
	 *
	 * @param height
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * Returns a list of transaction IDs included in the block.
	 *
	 * @return
	 */
	public long[] getTransactions() {
		if (transactions == null) {
			throw new UnsupportedOperationException();
		}
		return transactions;
	}

	/**
	 * 
	 * @param ids
	 */
	public void setTransactions(long[] ids) {
		this.transactions = ids;
		// Arrays.sort(this.transactions);
	}

	/**
	 * Returns the ID of the next block.
	 *
	 * @return
	 */
	public long getNextBlock() {
		return nextBlock;
	}

	/**
	 * Sets the ID of the next block.
	 */
	public void setNextBlock(long blockId) {
		nextBlock = blockId;
	}

	/**
	 * Returns the Cumulative Difficulty of the current block.
	 *
	 * @return
	 */
	public BigInteger getCumulativeDifficulty() {
		return cumulativeDifficulty;
	}

	/**
	 * 
	 * @param cumulativeDifficulty
	 */
	public void setCumulativeDifficulty(BigInteger cumulativeDifficulty) {
		this.cumulativeDifficulty = cumulativeDifficulty;
	}

}