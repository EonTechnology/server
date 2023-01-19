package org.eontechnology.and.peer.core.ledger.tree;

import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

public class BufferedTreeNodeCollection implements ITreeNodeCollection {
  private final ITreeNodeCollection nodeCollection;
  private final Map<String, TreeNode> writeAheadMap = new ConcurrentHashMap<>();

  public BufferedTreeNodeCollection(ITreeNodeCollection nodeCollection) {
    this.nodeCollection = nodeCollection;
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

  public void flushBranch(TreeNode rootNode) {
    AnalyzeIterator iterator = new AnalyzeIterator(rootNode);
    while (iterator.hasNext()) {
      nodeCollection.add(iterator.next());
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
