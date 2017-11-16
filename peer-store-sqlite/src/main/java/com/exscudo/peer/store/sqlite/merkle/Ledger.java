package com.exscudo.peer.store.sqlite.merkle;

import java.util.Map;

import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.store.sqlite.ConnectionProxy;
import com.exscudo.peer.store.sqlite.ISegment;
import com.exscudo.peer.store.sqlite.utils.NodeHelper;

/**
 * Implementation of {@code ILedger} interface using by Merkle Tree.
 * <p>
 * Instead of returning IAccount in {@link ILedger#getAccount}
 *
 * @see ILedger
 * @see IAccount
 */
public class Ledger implements ILedger {
	private final Tree state;

	Ledger(ConnectionProxy connection, byte[] rootNode) {
		this.state = new Tree(new Segment(connection), rootNode == null ? null : Format.convert(rootNode));
	}

	@Override
	public IAccount getAccount(long accountID) {
		Map<String, Object> map = state.get(accountID);
		if (map != null) {
			return AccountMapper.convert(map);
		}
		return null;
	}

	@Override
	public void putAccount(IAccount account) {
		state.put(account.getID(), AccountMapper.convert(account));
	}

	@Override
	public byte[] getHash() {
		return Format.convert(state.getRootNode().getHash());
	}

	private static class Segment implements ISegment<String, TreeNode> {

		private final ConnectionProxy connection;

		public Segment(ConnectionProxy connection) {
			this.connection = connection;
		}

		@Override
		public void put(String key, TreeNode node) {
			NodeHelper.put(connection, key, node);
		}

		@Override
		public TreeNode get(String key) {
			return NodeHelper.get(connection, key);
		}

		@Override
		public boolean contains(String key) {
			return NodeHelper.contains(connection, key);
		}

		@Override
		public void remove(String key) {
			// NodeHelper.remove(connection, key);
		}

	}
}
