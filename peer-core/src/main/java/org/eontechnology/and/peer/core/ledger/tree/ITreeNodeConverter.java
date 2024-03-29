package org.eontechnology.and.peer.core.ledger.tree;

public interface ITreeNodeConverter<T> {

  /**
   * @return
   */
  T convert(TreeNode treeNode);

  /**
   * @param value
   * @return
   */
  TreeNode convert(T value);
}
