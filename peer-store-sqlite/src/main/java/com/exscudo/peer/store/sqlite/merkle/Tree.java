package com.exscudo.peer.store.sqlite.merkle;

import java.util.Map;

import com.exscudo.peer.store.sqlite.ISegment;

public class Tree {

	private ISegment<String, TreeNode> storage;
	private TreeNode rootNode;

	public Tree(ISegment<String, TreeNode> storage, String root) {
		this.storage = storage;
		if (root != null) {
			rootNode = storage.get(root);
		}
	}

	public TreeNode getRootNode() {
		return rootNode;
	}

	public Map<String, Object> get(long id) {

		if (rootNode == null) {
			return null;
		}
		TreeNode node = get(id, rootNode);
		if (node != null) {
			return node.getValues();
		}
		return null;

	}

	public void put(long id, Map<String, Object> data) {

		TreeNode newLeaf = new TreeNode(TreeNode.LEAF, id, data);
		storage.put(newLeaf.getHash(), newLeaf);

		if (rootNode == null || (rootNode.getType() == TreeNode.LEAF && rootNode.getId() == id)) {
			rootNode = newLeaf;
			return;
		}
		rootNode = addNode(rootNode, newLeaf);

	}

	private TreeNode get(long id, TreeNode root) {

		if (root.getType() == TreeNode.LEAF) {
			if (root.getId() == id) {
				return root;
			}
			return null;
		}

		if (!TreeNode.isParent(root, id)) {
			return null;
		}

		String targetLeft = root.getValue("left").toString();
		String targetRight = root.getValue("right").toString();
		TreeNode left = storage.get(targetLeft);
		TreeNode right = storage.get(targetRight);

		TreeNode item = get(id, left);
		if (item == null) {
			item = get(id, right);
		}

		return item;
	}

	private TreeNode addNode(TreeNode root, TreeNode leaf) {

		if (leaf.getType() != TreeNode.LEAF) {
			throw new UnsupportedOperationException();
		}

		if (root.getType() == TreeNode.ROOT) {

			int height = ((Long) root.getValue("height")).intValue();
			if (TreeNode.isPrefixEquals(root, leaf, height)) {

				String targetLeft = root.getValue("left").toString();
				String targetRight = root.getValue("right").toString();
				TreeNode left = storage.get(targetLeft);
				TreeNode right = storage.get(targetRight);

				boolean testLeft = TreeNode.isPrefixEquals(left, leaf, height + 1);
				boolean testRight = TreeNode.isPrefixEquals(right, leaf, height + 1);

				TreeNode newLeft = left;
				TreeNode newRight = right;
				if (testLeft == testRight) {
					throw new IllegalStateException();
				}

				if (testLeft) {
					newLeft = addNode(left, leaf);
				}

				if (testRight) {
					newRight = addNode(right, leaf);
				}

				TreeNode newRoot = TreeNode.union(newLeft, newRight);

				storage.remove(root.getHash());
				storage.put(newRoot.getHash(), newRoot);
				return newRoot;

			}
		}

		if (root.getType() == TreeNode.LEAF && root.getId() == leaf.getId()) {
			return leaf;
		}

		TreeNode unionNode = TreeNode.union(leaf, root);
		storage.put(unionNode.getHash(), unionNode);
		return unionNode;

	}

}
