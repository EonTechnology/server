package com.exscudo.peer.store.sqlite.merkle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.store.sqlite.ConnectionProxy;
import com.exscudo.peer.store.sqlite.ISegment;
import com.exscudo.peer.store.sqlite.utils.NodeHelper;

/**
 * Simple implementation of {@code ILedger} interface using by Merkle Tree and
 * data caching.
 * <p>
 * Changes are saved when call {@link CachedLedger#commit()}
 *
 * @see ILedger
 * @see IAccount
 */
public class CachedLedger implements ILedger {
	private final Tree state;
	private Segment cache;

	CachedLedger(ConnectionProxy connection, byte[] rootNode) {
		cache = new Segment(connection);
		this.state = new Tree(cache, rootNode == null ? null : Format.convert(rootNode));
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

	public void commit() {
		cache.commit();
	}

	public void analyze() {
		cache.analyze(Format.convert(state.getRootNode().getHash()));
	}

	private static class Segment implements ISegment<String, TreeNode> {

		private final ConnectionProxy connection;
		private Map<String, TreeNode> saved = new ConcurrentHashMap<>();
		private HashMap<String, TreeNode> treeInBlock = new HashMap<>();

		public Segment(ConnectionProxy connection) {
			this.connection = connection;
		}

		@Override
		public TreeNode get(String id) {

			Objects.requireNonNull(id);
			if (this.saved.containsKey(id)) {
				return this.saved.get(id);
			}
			return NodeHelper.get(connection, id);

		}

		@Override
		public void put(String id, TreeNode value) {
			Objects.requireNonNull(id);
			Objects.requireNonNull(value);
			saved.put(id, value);
		}

		@Override
		public void remove(String id) {
			Objects.requireNonNull(id);
			saved.remove(id);
		}

		public void commit() {
			for (Map.Entry<String, TreeNode> entry : saved.entrySet()) {
				NodeHelper.put(connection, entry.getKey(), entry.getValue());
			}
		}

		public void analyze(byte[] root) {
			saveTree(treeInBlock, Format.convert(root));
			saved = new HashMap<>(treeInBlock);
		}

		private void saveTree(HashMap<String, TreeNode> newNodeSet, String key) {

			TreeNode node = saved.get(key);

			if (node == null) {
				return;
			}

			newNodeSet.put(node.getHash(), node);

			if (node.getType() == TreeNode.ROOT) {

				String left = node.getValue("left").toString();
				String right = node.getValue("right").toString();

				saveTree(newNodeSet, left);
				saveTree(newNodeSet, right);
			}

		}

	}

}
