package com.exscudo.peer.core.data;

import java.util.Map;

import com.exscudo.peer.core.data.mapper.transport.TransactionMapper;
import com.exscudo.peer.core.crypto.BencodeFormatter;

/**
 * Class {@code Transaction} is used to indicate some data signed by the sender,
 * which contains a description of some action - an atomic change of the system
 * state.
 */
public class Transaction extends SignedMessage {
	private static final long serialVersionUID = 2622024570224422971L;

	private int version;
	private int type;
	private int deadline;
	private long referencedTransaction;
	private long fee;
	private Map<String, Object> data;
	private Map<String, Object> confirmations;

	private volatile long block;
	private volatile int height = Integer.MAX_VALUE;

	private int length = 0;

	/**
	 * Returns the type of the transaction.
	 *
	 * @return
	 */
	public int getType() {
		return type;
	}

	/**
	 * Sets the type of the transaction.
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Returns the lifetime of the transaction.
	 *
	 * @return
	 */
	public int getDeadline() {
		return deadline;
	}

	/**
	 * Sets the lifetime of the transaction. The unit (assumed) is equal to
	 * <code>DEADLINE_UNIT</code> seconds.
	 *
	 * @param deadline
	 */
	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}

	/**
	 * Returns true if transaction lifetime has expired, otherwise - false.
	 *
	 * @param timestamp
	 *            Point in time for which the check is carried out
	 * @return
	 */
	public boolean isExpired(int timestamp) {

		return (getTimestamp() + getDeadline() <= timestamp);
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
	 * Returns the ID of the linked transaction. Linked transaction must be added to
	 * the block before the current transaction.
	 *
	 * @return
	 */
	public long getReference() {
		return referencedTransaction;
	}

	/**
	 * Sets the ID of the linked transaction. Linked transaction must be added to
	 * the block before the current transaction.
	 *
	 * @param reference
	 */
	public void setReference(long reference) {
		this.referencedTransaction = reference;
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
	 * Sets the fee for the implementation of the transaction.
	 *
	 * @param fee
	 */
	public void setFee(long fee) {
		this.fee = fee;
	}

	/**
	 * Returns the data packet
	 *
	 * @return
	 */
	public Map<String, Object> getData() {
		return data;
	}

	/**
	 * Sets the data packet
	 *
	 * @param data
	 */
	public void setData(Map<String, Object> data) {
		this.data = data;
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
	 * Returns the transaction version.
	 *
	 * @return
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * Sets the transaction version.
	 *
	 * @param version
	 */
	public void setVersion(int version) {
		this.version = version;
	}

	/**
	 * Returns the list of confirmations for the transaction.
	 *
	 * @return map which contains a list of accounts that have confirmed the
	 *         transaction.
	 *         <p>
	 *         The key is the friendly ID of account. To get an id call
	 *         {@link com.exscudo.peer.core.utils.Format.ID#accountId(String)}
	 *         <p>
	 *         The value is the signature. To convert to an array of bytes
	 *         {@link com.exscudo.peer.core.utils.Format#convert}
	 */
	public Map<String, Object> getConfirmations() {
		return confirmations;
	}

	/**
	 * Sets the list of confirmations for the transaction.
	 *
	 * @param map
	 *            contains a list of accounts that have confirmed the transaction.
	 *            <p>
	 *            The key is the friendly ID of account. To get an id call
	 *            {@link com.exscudo.peer.core.utils.Format.ID#accountId(String)}
	 *            <p>
	 *            The value is the signature. To convert to an array of bytes
	 *            {@link com.exscudo.peer.core.utils.Format#convert}
	 */
	public void setConfirmations(Map<String, Object> map) {
		this.confirmations = map;
	}

	/**
	 * Get object length
	 *
	 * @return length
	 */
	public int getLength() {
		if (this.length == 0) {
			byte[] bytes = BencodeFormatter.getBytes(TransactionMapper.convert(this));
			this.length = bytes.length;
		}
		return this.length;
	}
}
