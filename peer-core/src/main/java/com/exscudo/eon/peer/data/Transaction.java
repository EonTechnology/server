package com.exscudo.eon.peer.data;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.exscudo.eon.peer.Constant;
import com.exscudo.eon.utils.Crypto;

/**
 * The class {@code Transaction} is used to indicate signed by the sender of the
 * data packet, which contains a description of some action - atomic change
 * system state.
 */
public class Transaction implements Comparable<Transaction>, Serializable {
	private static final long serialVersionUID = 2622024570224422971L;

	// in seconds
	private static final int DEADLINE_TIME_UNIT = 60;

	private static final int HEAD_LEN = 1 + 1 + 4 + 2 + 8 + 8 + 8 + 32;
	private static final int HEAD_LEN_SIGN = HEAD_LEN + 64;

	private byte type;
	private byte subtype;
	private int timestamp;
	private short deadline;
	private long referencedTransaction;
	private long recipient;
	private byte[] senderPublicKey;
	private long fee;
	private byte[] data;
	private byte[] signature;

	private volatile long block;
	private volatile int height;

	public Transaction(byte type, byte subtype, int timestamp, short deadline, long referencedTransaction,
			byte[] senderPublicKey, long recipient, long fee, byte[] data, byte[] signature) {

		this.type = type;
		this.subtype = subtype;
		this.timestamp = timestamp;
		this.deadline = deadline;
		this.referencedTransaction = referencedTransaction;
		this.senderPublicKey = senderPublicKey;
		this.recipient = recipient;
		this.fee = fee;
		this.signature = signature;

		this.data = data;

		height = Integer.MAX_VALUE;
		block = 0;
	}

	/**
	 * Return the type of the transaction.
	 * 
	 * @return
	 */
	public byte getType() {
		return type;
	}

	/**
	 * Returns the transaction subtype.
	 * 
	 * @return
	 */
	public byte getSubType() {
		return subtype;
	}

	/**
	 * Returns the time of the creation of the transaction.
	 * 
	 * @return
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Returns the lifetime of the transaction. Unit is assumed equal to
	 * <code>DEADLINE_UNIT</code> seconds.
	 * 
	 * @return
	 */
	public short getDeadline() {
		return deadline;
	}

	/**
	 * Returns true if transaction lifetime has expired, otherwise - false.
	 * 
	 * @param timestamp
	 *            Point in time for which the check is carried out
	 * @return
	 */
	public boolean isExpired(int timestamp) {

		return (getTimestamp() + getDeadline() * DEADLINE_TIME_UNIT <= timestamp);
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
	 * Returns the ID of the linked transaction. Linked transaction must be
	 * added to the block before the current transaction.
	 * 
	 * @return
	 */
	public long getReferencedTransaction() {
		return referencedTransaction;
	}

	/**
	 * Returns the recipient Account ID.
	 *
	 * @return
	 */
	public long getRecipientID() {
		return recipient;
	}

	/**
	 * Returns the sender public key.
	 * 
	 * @return
	 */
	public byte[] getSenderPublicKey() {
		return senderPublicKey;
	}

	/**
	 * Returns the fee for the implementation of the transaction.
	 * 
	 * @return
	 */
	public long getFee() {
		return fee;
	}

	/**
	 * Return the data packet
	 * 
	 * @return
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Returns the transaction signature.
	 * 
	 * @return
	 */
	public byte[] getSignature() {
		return signature;
	}

	/**
	 * Returns block ID where a transaction was placed.
	 *
	 * @return
	 */
	public long getBlock() {
		return block;
	}

	/**
	 * Sets the block ID where a transaction was placed.
	 *
	 * @param blockId
	 */
	public void setBlock(long blockId) {
		block = blockId;
	}

	/**
	 * Returns the block height. Not used
	 *
	 * @return
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Sets the block height where a transaction was placed.
	 * 
	 * @param height
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * Returns the transaction number. Not used.
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
	 * Returns the transaction data as a byte array.
	 * 
	 * @return
	 */
	public byte[] getBytes() {

		byte[] data = getData();
		int dataLength = 0;
		if (data != null)
			dataLength = data.length;

		ByteBuffer buffer = ByteBuffer.allocate(HEAD_LEN_SIGN + dataLength);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(getType());
		buffer.put(getSubType());
		buffer.putInt(getTimestamp());
		buffer.putShort(getDeadline());
		buffer.putLong(getReferencedTransaction());
		buffer.putLong(getFee());
		buffer.putLong(getRecipientID());
		buffer.put(getSenderPublicKey());
		buffer.put(getSignature());

		if (data != null)
			buffer.put(data);

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

	@Override
	public int compareTo(Transaction o) {

		if (getHeight() < o.getHeight()) {

			return -1;

		} else if (getHeight() > o.getHeight()) {

			return 1;

		} else {

			if (getFee() * Constant.TX_MAX_PAYLOAD_LENGTH / getBytes().length > o.getFee()
					* Constant.TX_MAX_PAYLOAD_LENGTH / o.getBytes().length) {

				return -1;

			} else if (getFee() * Constant.TX_MAX_PAYLOAD_LENGTH / getBytes().length < o.getFee()
					* Constant.TX_MAX_PAYLOAD_LENGTH / o.getBytes().length) {

				return 1;

			} else {

				if (getTimestamp() < o.getTimestamp()) {

					return -1;

				} else if (getTimestamp() > o.getTimestamp()) {

					return 1;

				} else {

					if (getIndex() < o.getIndex()) {

						return -1;

					} else if (getIndex() > o.getIndex()) {

						return 1;

					} else {

						try {

							long id = getID();
							long oid = o.getID();

							if (id < oid) {

								return -1;

							} else if (id > oid) {

								return 1;
							}

						} catch (Exception ignore) {
						}

						final byte[] signature = getSignature();
						final byte[] osignature = o.getSignature();
						int length = Math.min(signature.length, osignature.length);

						for (int i = 0; i < length; i++) {

							if (signature[i] < osignature[i]) {

								return -1;

							} else if (signature[i] > osignature[i]) {

								return 1;
							}
						}

						if (signature.length < osignature.length) {

							return -1;

						} else if (signature.length > osignature.length) {

							return 1;
						}

						return 0;
					}
				}

			}
		}
	}

	public static boolean validateSignature(Transaction tx) {

		byte[] data = tx.getBytes();
		for (int i = HEAD_LEN; i < HEAD_LEN_SIGN; i++) {
			data[i] = 0;
		}
		return Crypto.verify(tx.getSignature(), data, tx.getSenderPublicKey());

	}

	public static void sign(Transaction tx, String secretPhrase) {
		if (secretPhrase == null) {
			throw new NullPointerException("secretPhrase");
		}
		tx.signature = Crypto.sign(tx.getBytes(), secretPhrase);
	}

}
