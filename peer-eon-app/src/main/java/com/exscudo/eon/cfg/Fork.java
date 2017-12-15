package com.exscudo.eon.cfg;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedList;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.services.ITransactionHandler;

/**
 * Basic implementation of the {@code IFork} interface.
 */
public class Fork implements IFork {

	private final LinkedList<Item> items;
	private final long genesisBlockID;

	public Fork(long genesisBlockID, Item[] items) {
		this.genesisBlockID = genesisBlockID;

		this.items = new LinkedList<>();
		for (Item i : items) {
			this.items.add(i);
		}
		this.items.sort(new ItemComparator());
	}

	@Override
	public long getGenesisBlockID() {
		return genesisBlockID;
	}

	@Override
	public boolean isPassed(int timestamp) {
		return items.getLast().isPassed(timestamp);
	}

	@Override
	public boolean isCome(int timestamp) {
		return items.getLast().isCome(timestamp);
	}

	@Override
	public int getNumber(int timestamp) {
		Item item = getItem(timestamp);
		return (item == null) ? -1 : item.number;
	}

	@Override
	public ITransactionHandler getTransactionExecutor(int timestamp) {
		Item item = getItem(timestamp);
		return (item == null) ? null : item.handler;
	}

	@Override
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

		public final long begin;
		public final long end;
		public final int number;
		public final ITransactionHandler handler;
		public final int blockVersion;

		public Item(int number, String begin, String end, ITransactionHandler handler, int blockVersion) {

			this.number = number;
			this.begin = Instant.parse(begin).toEpochMilli();
			this.end = Instant.parse(end).toEpochMilli();

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
