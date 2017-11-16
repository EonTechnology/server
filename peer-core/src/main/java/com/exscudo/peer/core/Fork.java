package com.exscudo.peer.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.ITransactionHandler;

/**
 * Hard fork is a pre-planned network update point. At that point in time, new
 * functionality is introduced into the network.
 */
public class Fork {

	private final LinkedList<Item> items;
	private final long genesisBlockID;

	public Fork(long genesisBlockID, List<Item> items) {
		this.genesisBlockID = genesisBlockID;

		this.items = new LinkedList<>(items);
		this.items.sort(new ItemComparator());
	}

	/**
	 * Returns genesis-block ID.
	 * <p>
	 * Genesis block can be considered as a network identifier.
	 */
	public long getGenesisBlockID() {
		return genesisBlockID;
	}

	/**
	 * Checks whether the hard-fork has been expired for the specified
	 * {@code timestamp}.
	 *
	 * @param timestamp
	 *            for which a check is made (unix timestamp)
	 * @return true if time not in fork, otherwise false
	 */
	public boolean isPassed(int timestamp) {
		return items.getLast().isPassed(timestamp);
	}

	/**
	 * Checks whether the hard-fork started at the specified {@code timestamp}.
	 * <p>
	 * The moment that the fork was started at the specified time, does not
	 * guarantee that it was not already completed
	 *
	 * @param timestamp
	 *            for which a check is made (unix timestamp)
	 * @return true if the hard-fork was started, otherwise - false.
	 */
	public boolean isCome(int timestamp) {
		return items.getLast().isCome(timestamp);
	}

	/**
	 * Returns hard-fork number.
	 * <p>
	 * Hard-forks are numbered sequentially in ascending order.
	 *
	 * @param timestamp
	 *            on which it is necessary to calculate the number of the hard-fork
	 *            (unix timestamp)
	 * @return hard-fork number
	 */
	public int getNumber(int timestamp) {
		Item item = getItem(timestamp);
		return (item == null) ? -1 : item.number;
	}

	/**
	 * Returns the transaction handler used for the specified time or null.
	 *
	 * @param timestamp
	 *            for which a handler will be returned (unix timestamp)
	 * @return transaction handler or null
	 */
	public ITransactionHandler getTransactionExecutor(int timestamp) {
		Item item = getItem(timestamp);
		return (item == null) ? null : item.handler;
	}

	public boolean isSupportedTran(Transaction tx, int timestamp) {
		Item item = getItem(timestamp);
		return (item != null) && Arrays.binarySearch(item.targetTranVersions, tx.getVersion()) >= 0;
	}

	/**
	 * Returns the block version used for the specified time.
	 *
	 * @param timestamp
	 *            for which a block version will be returned (unix timestamp)
	 * @return block version or -1
	 */
	public int getBlockVersion(int timestamp) {
		Item item = getItem(timestamp);
		return (item == null) ? -1 : item.blockVersion;
	}

	private Item getItem(int timestamp) {
		if (!items.getFirst().isCome(timestamp)) {
			return null;
		}
		for (Item item : items) {
			if (item.isCome(timestamp) && !item.isPassed(timestamp)) {
				return item;
			}
		}
		return items.getLast();
	}

	private static class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item o1, Item o2) {
			return Integer.compare(o1.number, o2.number);
		}
	}

	public static class Item {

		final long begin;
		final long end;
		final int number;
		final int[] targetTranVersions;
		final ITransactionHandler handler;
		final int blockVersion;

		public Item(int number, long begin, long end, int[] targetTranVersions, ITransactionHandler handler,
				int blockVersion) {

			this.number = number;
			this.begin = begin;
			this.end = end;

			this.targetTranVersions = Arrays.copyOf(targetTranVersions, targetTranVersions.length);
			Arrays.sort(this.targetTranVersions);

			this.handler = handler;
			this.blockVersion = blockVersion;

		}

		boolean isCome(int timestamp) {
			return timestamp * 1000L > begin;
		}

		boolean isPassed(int timestamp) {
			return timestamp * 1000L > end;
		}
	}
}
