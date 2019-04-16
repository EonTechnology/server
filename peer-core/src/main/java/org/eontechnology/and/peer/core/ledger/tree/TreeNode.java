package org.eontechnology.and.peer.core.ledger.tree;

import java.util.HashMap;
import java.util.Map;

import com.dampcake.bencode.Bencode;

public class TreeNode {
    public static final int ROOT = 1;
    public static final int LEAF = 2;

    private TreeNodeID id = null;

    private int type;
    private int timestamp;
    private long mask;
    private int maskLength;
    private TreeNodeID rightNodeID;
    private TreeNodeID leftNodeID;

    private Map<String, Object> data;

    public TreeNode(int type,
                    int timestamp,
                    long mask,
                    int maskLength,
                    TreeNodeID leftNodeID,
                    TreeNodeID rightNodeID,
                    Map<String, Object> map) {

        this.type = type;
        this.timestamp = timestamp;
        this.mask = mask;
        this.maskLength = maskLength;
        this.leftNodeID = leftNodeID;
        this.rightNodeID = rightNodeID;
        this.data = map;
    }

    public static boolean isChild(TreeNode parent, TreeNode child) {
        long base = (1L << parent.getMaskLength()) - 1L;

        long parentMask = parent.getMask() & base;
        long childMask = child.getMask() & base;

        return childMask == parentMask;
    }

    public static int intersectAt(TreeNode aNode, TreeNode bNode) {
        long id1 = aNode.getMask();
        long id2 = bNode.getMask();

        long base = 0L;
        for (int i = 0; i < 64; i++) {
            base = (base << 1) + 1L;
            if ((id1 & base) != (id2 & base)) {
                return i;
            }
        }

        return -1;
    }

    public static boolean hasIntersection(TreeNode parent, long mask) {

        if (parent.getType() == TreeNode.LEAF) {
            return parent.getMask() == mask;
        }

        long base = (1L << parent.getMaskLength()) - 1L;

        long parentMask = parent.getMask() & base;
        long childMask = mask & base;

        return childMask == parentMask;
    }

    public static boolean hasIntersection(TreeNode aNode, TreeNode bNode, int level) {
        long base = (1L << level) - 1L;

        long aMask = aNode.getMask() & base;
        long bMask = bNode.getMask() & base;

        return bMask == aMask;
    }

    public static TreeNode union(TreeNode aNode, TreeNode bNode, int timestamp) {

        int idx = TreeNode.intersectAt(aNode, bNode);
        if (idx == -1) {
            throw new UnsupportedOperationException();
        }

        long base = (1L << (idx + 1)) - 1L;
        long d1 = (aNode.getMask() >>> idx) & 1;
        long d2 = (bNode.getMask() >>> idx) & 1;

        TreeNode lNode = aNode;
        TreeNode rNode = bNode;
        if (d1 > d2) {
            lNode = bNode;
            rNode = aNode;
        }

        return new TreeNode(TreeNode.ROOT,
                            timestamp,
                            (aNode.getMask() & (base >>> 1)),
                            idx,
                            lNode.getID(),
                            rNode.getID(),
                            null);
    }

    public int getType() {
        return type;
    }

    public Map<String, Object> getValues() {
        return data;
    }

    public long getMask() {
        return mask;
    }

    public int getMaskLength() {
        return maskLength;
    }

    public TreeNodeID getRightNodeID() {
        return rightNodeID;
    }

    public TreeNodeID getLeftNodeID() {
        return leftNodeID;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public TreeNodeID getID() {
        if (id == null) {
            id = calculateID();
        }
        return id;
    }

    private TreeNodeID calculateID() {

        Map<String, Object> map = null;
        if (getType() == TreeNode.LEAF) {
            map = new HashMap<>(data);
        } else {
            map = new HashMap<>();
            map.put("height", getMaskLength());
            map.put("mask", getMask());
            map.put("left", getLeftNodeID().getKey());
            map.put("right", getRightNodeID().getKey());
        }

        Bencode bencode = new Bencode();
        byte[] bytes = bencode.encode(map);
        return new TreeNodeID(bytes);
    }
}
