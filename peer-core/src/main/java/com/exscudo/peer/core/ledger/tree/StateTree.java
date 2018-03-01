package com.exscudo.peer.core.ledger.tree;

import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

public class StateTree<TValue> implements ITreeNodeCollection {
    private final ITreeNodeCollection nodeCollection;
    private final IValueConverter<TValue> valueConverter;
    private final Map<String, TreeNode> writeAheadMap = new ConcurrentHashMap<>();

    public StateTree(ITreeNodeCollection nodeCollection, IValueConverter<TValue> valueConverter) {
        this.nodeCollection = nodeCollection;
        this.valueConverter = valueConverter;
    }

    @Override
    public TreeNode get(TreeNodeID id) {
        if (this.writeAheadMap.containsKey(id.getKey())) {
            return this.writeAheadMap.get(id.getKey());
        }
        return nodeCollection.get(id);
    }

    @Override
    public void add(TreeNode node) {
//        TreeNode n = get(node.getID());
//        if (n != null) {
//            return;
//        }
        this.writeAheadMap.put(node.getID().getKey(), node);
    }

    public State<TValue> getState(String name) {
        TreeNodeID id = new TreeNodeID(name);
        TreeNode root = get(id);
        return new StateTree.State<TValue>(valueConverter, this, root);
    }

    public State<TValue> newState(State<TValue> state, long id, TValue value, int timestamp) {

        TreeNode newLeaf = new TreeNode(TreeNode.LEAF, timestamp, id, 0, null, null, valueConverter.convert(value));
        return newState(state, newLeaf);
    }

    private State<TValue> newState(State<TValue> state, TreeNode newNode) {

        if (newNode.getType() != TreeNode.LEAF) {
            throw new UnsupportedOperationException("Invalid type of the node.");
        }
        add(newNode);

        TreeNode newRoot;
        if (state == null) {
            newRoot = newNode;
        } else {
            newRoot = createNewRoot(state.rootNode, newNode);
        }

        return new State<>(valueConverter, this, newRoot);
    }

    private TreeNode createNewRoot(TreeNode rootNode, TreeNode leaf) {

        if (rootNode.getType() == TreeNode.LEAF && rootNode.getMask() == leaf.getMask()) {
            return leaf;
        }
        if (rootNode.getType() == TreeNode.ROOT && TreeNode.isChild(rootNode, leaf)) {

            TreeNode left = get(rootNode.getLeftNodeID());
            TreeNode right = get(rootNode.getRightNodeID());

            int nextMaskLength = rootNode.getMaskLength() + 1;
            boolean isLeftBranch = TreeNode.hasIntersection(left, leaf, nextMaskLength);
            boolean isRightBranch = TreeNode.hasIntersection(right, leaf, nextMaskLength);
            if (isLeftBranch == isRightBranch) {
                throw new IllegalStateException("panic");
            }

            TreeNode newLeft = left;
            TreeNode newRight = right;
            if (isLeftBranch) {
                newLeft = createNewRoot(left, leaf);
            }
            if (isRightBranch) {
                newRight = createNewRoot(right, leaf);
            }

            TreeNode newRoot = TreeNode.union(newLeft, newRight, leaf.getTimestamp());
            add(newRoot);
            return newRoot;
        }

        TreeNode unionNode = TreeNode.union(leaf, rootNode, leaf.getTimestamp());
        add(unionNode);
        return unionNode;
    }

    public void saveState(State<TValue> state) {
        AnalyzeIterator iterator = new AnalyzeIterator(state.rootNode);
        while (iterator.hasNext()) {
            nodeCollection.add(iterator.next());
        }
    }

    public static class State<T> implements Iterable<T> {
        final TreeNode rootNode;
        private final IValueConverter<T> valueConverter;
        private final ITreeNodeCollection nodes;

        private State(IValueConverter<T> valueConverter, ITreeNodeCollection nodes, TreeNode rootNode) {
            this.valueConverter = valueConverter;
            this.nodes = nodes;
            this.rootNode = rootNode;
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
            return valueConverter.convert(node.getValues());
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
            private final IValueConverter<V> valueConverter;
            private final ITreeNodeCollection nodes;
            private final Stack<TreeNode> stack = new Stack<>();
            private TreeNode next;

            TreeNodeIterator(ITreeNodeCollection nodes, TreeNode rootNode, IValueConverter<V> valueConverter) {
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
                V value = valueConverter.convert(next.getValues());
                if (stack.size() == 0) {
                    next = null;
                } else {

                    TreeNode p = stack.pop();
                    TreeNode r = nodes.get(p.getRightNodeID());
                    if (r.getType() == TreeNode.ROOT) {
                        next = walkDown(r);
                    } else {
                        next = r;
                    }
                }
                return value;
            }
        }
    }

    private class AnalyzeIterator implements Iterator<TreeNode> {
        private final Stack<TreeNode> stack = new Stack<>();

        private AnalyzeIterator(TreeNode parent) {
            walkDown(parent);
        }

        private void walkDown(TreeNode parent) {
            TreeNode c = parent;
            while (c != null) {
                stack.push(c);
                TreeNodeID id = c.getLeftNodeID();
                c = (id == null) ? null : writeAheadMap.get(id.getKey());
            }
        }

        @Override
        public boolean hasNext() {
            return !stack.empty();
        }

        @Override
        public TreeNode next() {

            TreeNode next = stack.pop();
            if (next.getRightNodeID() != null) {
                TreeNode r = writeAheadMap.get(next.getRightNodeID().getKey());
                if (r != null) {
                    walkDown(r);
                }
            }
            return next;
        }
    }
}
