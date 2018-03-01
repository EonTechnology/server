package com.exscudo.peer.core.ledger.tree;

public interface ITreeNodeCollection {

    TreeNode get(TreeNodeID id);

    void add(TreeNode node);
}
