package com.exscudo.peer.store.sqlite.merkle;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import com.dampcake.bencode.Bencode;
import com.exscudo.peer.core.utils.Format;

public class TreeNode implements Serializable {
	public static final int ROOT = 1;
	public static final int LEAF = 2;

	private int type;
	private long id;
	private Map<String, Object> data;

	private String hashStr = null;

	public TreeNode(int type, long id, Map<String, Object> data) {
		this.type = type;
		this.id = id;
		this.data = data;
	}

	private TreeNode() {
	}

	public int getType() {
		return type;
	}

	public long getId() {
		return id;
	}

	public Object getValue(String name) {
		return data.get(name);
	}

	public Map<String, Object> getValues() {
		return data;
	}

	public String getHash() {
		try {

			if (hashStr == null) {

				Bencode bencode = new Bencode();
				byte[] bytes = bencode.encode(this.data);
				byte[] hash = MessageDigest.getInstance("SHA-512").digest(bytes);

				hashStr = Format.convert(hash);
			}
			return hashStr;

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static TreeNode union(TreeNode node1, TreeNode node2) {

		long id1 = getNodeMask(node1);
		long id2 = getNodeMask(node2);

		long base = 0L;
		for (int i = 0; i < 64; i++) {

			base = (base << 1) + 1L;
			long mask1 = id1 & base;
			long mask2 = id2 & base;

			if (mask1 != mask2) {

				Map<String, Object> map = new HashMap<>();
				map.put("height", new Long(i));
				map.put("mask", (id1 & (base >>> 1)));

				long d1 = (id1 >>> i) & 1;
				long d2 = (id2 >>> i) & 1;

				if (d1 > d2) {
					map.put("left", node2.getHash());
					map.put("right", node1.getHash());
				} else {
					map.put("left", node1.getHash());
					map.put("right", node2.getHash());
				}

				TreeNode node = new TreeNode();
				node.id = node1.getId() ^ node2.getId();
				node.type = TreeNode.ROOT;
				node.data = map;
				return node;
			}
		}

		throw new UnsupportedOperationException();
	}

	private static long getNodeMask(TreeNode node) {
		if (node.getType() == TreeNode.LEAF) {
			return node.getId();
		} else if (node.getType() == TreeNode.ROOT) {
			return (Long) node.getValue("mask");
		} else {
			throw new UnsupportedOperationException("Unknown type.");
		}
	}

	public static boolean isPrefixEquals(TreeNode aNode, TreeNode bNode, int height) {

		long base = (1L << height) - 1L;

		long mask = TreeNode.getNodeMask(aNode) & base;
		long test = TreeNode.getNodeMask(bNode) & base;

		return test == mask;

	}

	public static boolean isParent(TreeNode node, long id) {

		int height = ((Long) node.getValue("height")).intValue();

		long base = (1L << height) - 1L;
		long mask = TreeNode.getNodeMask(node) & base;
		long test = id & base;
		return test == mask;
	}

}
