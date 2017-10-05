package com.exscudo.peer.core.data;

import com.exscudo.peer.core.crypto.SignedObject;
import com.exscudo.peer.core.utils.Format;

/**
 * The class {@code SignedMessage} provides base implementation for data message
 * signed by the sender of the object
 */
public class SignedMessage extends SignedObject {
	private static final long serialVersionUID = 1897455959474176742L;

	protected long senderID;
	protected int timestamp;
	protected long id = 0L;

	/**
	 * Returns the ID.
	 *
	 * @return
	 */
	public long getID() {
		if (id == 0L) {
			id = Format.MathID.pick(getSignature(), getTimestamp());
		}
		return id;
	}

	/**
	 * Returns the sender IAccount ID.
	 *
	 * @return IAccount ID
	 */
	public long getSenderID() {
		return senderID;
	}

	/**
	 * Sets sender
	 *
	 * @param senderID
	 *            sender id
	 */
	public void setSenderID(long senderID) {
		this.senderID = senderID;
	}

	/**
	 * Returns the time of the creation.
	 *
	 * @return unix timestamp
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Sets timestamp
	 *
	 * @param timestamp
	 *            unix timestamp
	 */
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

}
