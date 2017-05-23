package com.exscudo.eon.transactions;

import javax.naming.NamingException;

import com.exscudo.eon.peer.data.Transaction;

/**
 * Known transaction types
 *
 */
public enum TransactionType {

	None((byte) 0, (byte) 0);

	private byte type;
	private byte subtype;

	private TransactionType(byte type, byte subtype) {
		this.type = type;
		this.subtype = subtype;
	}

	public byte getType() {
		return type;
	}

	public byte getSubType() {
		return subtype;
	}

	public int getKey() {
		return (getType() * 256) | getSubType();
	}

	public static TransactionType typeOf(Transaction transaction) throws NamingException {

		for (TransactionType t : TransactionType.values()) {
			if (t.getType() == transaction.getType() && t.getSubType() == transaction.getSubType()) {
				return t;
			}
		}
		throw new NamingException("Transaction type not registered.");

	}

}
