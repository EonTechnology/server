package org.eontechnology.and.peer.core.ledger.tree;

import java.util.Iterator;
import java.util.Stack;

public class StateTree<T> implements Iterable<T> {
    final TreeNode rootNode;
    private final ITreeNodeConverter<T> valueConverter;
    private final ITreeNodeCollection nodes;

    public StateTree(ITreeNodeConverter<T> valueConverter, ITreeNodeCollection nodes, TreeNode rootNode) {
        this.valueConverter = valueConverter;
        this.nodes = nodes;
        this.rootNode = rootNode;
    }

    public static <V> StateTree<V> createNew(StateTree<V> state, Iterator<V> values) {

        ITreeNodeConverter<V> converter = state.valueConverter;
        BufferedTreeNodeCollection treeNodeCollection = new BufferedTreeNodeCollection(state.nodes);

        TreeNode rootNode = state.rootNode;
        while (values.hasNext()) {
            TreeNode newLeaf = converter.convert(values.next());
            rootNode = addLeaf(treeNodeCollection, rootNode, newLeaf);
        }

        treeNodeCollection.flushBranch(rootNode);
        return new StateTree<>(state.valueConverter, state.nodes, rootNode);
    }

    public static <V> StateTree<V> createNew(StateTree<V> state, V value) {

        TreeNode newRoot = addLeaf(state.nodes, state.rootNode, state.valueConverter.convert(value));
        return new StateTree<V>(state.valueConverter, state.nodes, newRoot);
    }

    private static TreeNode addLeaf(ITreeNodeCollection nodes, TreeNode rootNode, TreeNode newLeaf) {

        if (newLeaf.getType() != TreeNode.LEAF) {
            throw new UnsupportedOperationException("Invalid type of the node.");
        }
        nodes.add(newLeaf);

        TreeNode newRoot;
        if (rootNode == null) {
            newRoot = newLeaf;
        } else {
            newRoot = createNewRoot(nodes, rootNode, newLeaf);
        }

        return newRoot;
    }

    private static TreeNode createNewRoot(ITreeNodeCollection nodes, TreeNode rootNode, TreeNode newNode) {

        if (rootNode.getType() == TreeNode.LEAF && rootNode.getMask() == newNode.getMask()) {
            return newNode;
        }
        if (rootNode.getType() == TreeNode.ROOT && TreeNode.isChild(rootNode, newNode)) {

            TreeNode left = nodes.get(rootNode.getLeftNodeID());
            TreeNode right = nodes.get(rootNode.getRightNodeID());

            int nextMaskLength = rootNode.getMaskLength() + 1;
            boolean isLeftBranch = TreeNode.hasIntersection(left, newNode, nextMaskLength);
            boolean isRightBranch = TreeNode.hasIntersection(right, newNode, nextMaskLength);
            if (isLeftBranch == isRightBranch) {
                throw new IllegalStateException("panic");
            }

            TreeNode newLeft = left;
            TreeNode newRight = right;
            if (isLeftBranch) {
                newLeft = createNewRoot(nodes, left, newNode);
            }
            if (isRightBranch) {
                newRight = createNewRoot(nodes, right, newNode);
            }

            TreeNode newRoot = TreeNode.union(newLeft, newRight, newNode.getTimestamp());
            nodes.add(newRoot);
            return newRoot;
        }

        TreeNode unionNode = TreeNode.union(newNode, rootNode, newNode.getTimestamp());
        nodes.add(unionNode);
        return unionNode;
    }

    public String getName() {
        if (rootNode == null) {
            return null;
        }
        return rootNode.getID().getKey();
    }

    public T get(long id) {
        if (rootNode == null) {
            return null;
        }
        TreeNode node = getNode(rootNode, id);
        if (node == null) {
            return null;
        }
        return valueConverter.convert(node);
    }

    private TreeNode getNode(TreeNode node, long path) {

        if (node.getType() == TreeNode.LEAF) {
            if (node.getMask() == path) {
                return node;
            }
            return null;
        }

        if (!TreeNode.hasIntersection(node, path)) {
            return null;
        }

        TreeNode lNode = nodes.get(node.getLeftNodeID());
        TreeNode item = getNode(lNode, path);
        if (item != null) {
            return item;
        }

        TreeNode rNode = nodes.get(node.getRightNodeID());
        return getNode(rNode, path);
    }

    @Override
    public Iterator<T> iterator() {
        return new TreeNodeIterator<>(nodes, rootNode, valueConverter);
    }

    public static class TreeNodeIterator<V> implements Iterator<V> {
        private final ITreeNodeConverter<V> valueConverter;
        private final ITreeNodeCollection nodes;
        private final Stack<TreeNode> stack = new Stack<>();
        private TreeNode next;

        TreeNodeIterator(ITreeNodeCollection nodes, TreeNode rootNode, ITreeNodeConverter<V> valueConverter) {
            this.valueConverter = valueConverter;
            this.nodes = nodes;

            this.next = walkDown(rootNode);
        }

        private TreeNode walkDown(TreeNode node) {
            TreeNode c = node;
            while (c != null && c.getType() == TreeNode.ROOT) {
                stack.push(c);
                c = nodes.get(c.getLeftNodeID());
            }
            // the left and right leaf always exist so method returns null only if it is passed
            return c;
        }

        @Override
        public boolean hasNext() {
            return (next != null);
        }

        @Override
        public V next() {
            if (next == null) {
                return null;
            }
            V value = valueConverter.convert(next);
            if (stack.size() == 0) {
                next = null;
            } else {

                TreeNode p = stack.pop();
                TreeNode r = nodes.get(p.getRightNodeID());

                next = walkDown(r);
            }
            return value;
        }
    }
}

