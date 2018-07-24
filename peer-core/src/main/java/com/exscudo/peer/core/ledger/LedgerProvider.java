package com.exscudo.peer.core.ledger;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.Function;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.ledger.storage.DbNode;
import com.exscudo.peer.core.ledger.tree.BufferedTreeNodeCollection;
import com.exscudo.peer.core.ledger.tree.ITreeNodeConverter;
import com.exscudo.peer.core.ledger.tree.StateTree;
import com.exscudo.peer.core.ledger.tree.TreeNode;
import com.exscudo.peer.core.ledger.tree.TreeNodeID;
import com.exscudo.peer.core.storage.Storage;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;

public class LedgerProvider {
    private final Storage storage;

    public LedgerProvider(Storage storage) {
        this.storage = storage;
    }

    /**
     * Returns the status of accounts corresponding to the specified block.
     *
     * @param block target block
     * @return {@code ILedger} object or null
     */
    public ILedger getLedger(Block block) {
        return getLedger(block.getSnapshot(), block.getTimestamp() + Constant.BLOCK_PERIOD);
    }

    public ILedger getLedger(String snapshot, int timestamp) {

        BufferedTreeNodeCollection collection = new BufferedTreeNodeCollection(new CachedNodeCollection(storage));

        TreeNode root = null;
        if (snapshot != null) {
            TreeNodeID id = new TreeNodeID(snapshot);
            root = collection.get(id);
        }
        StateTree<Account> state = new StateTree(new AccountConverter(timestamp), collection, root);
        Function<StateTree<Account>, Void> callback = new Function<StateTree<Account>, Void>() {
            BufferedTreeNodeCollection nodes = collection;

            @Override
            public Void apply(StateTree<Account> s) {
                String name = s.getName();
                if (name != null) {
                    TreeNode newRoot = nodes.get(new TreeNodeID(name));
                    nodes.flushBranch(newRoot);
                }
                return null;
            }
        };

        return new Ledger(state, callback);
    }

    public Void addLedger(ILedger ledger) {
        return storage.callInTransaction(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                ledger.save();
                return null;
            }
        });
    }

    public void truncate(int timestamp) {
        storage.callInTransaction(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Dao<DbNode, ?> dao = DaoManager.createDao(storage.getConnectionSource(), DbNode.class);
                DeleteBuilder<DbNode, ?> builder = dao.deleteBuilder();
                builder.where().gt("timestamp", timestamp);
                builder.delete();

                return null;
            }
        });
    }

    private static class AccountConverter implements ITreeNodeConverter<Account> {
        private final int timestamp;

        private AccountConverter(int timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public Account convert(TreeNode treeNode) {

            Map<String, Object> map = treeNode.getValues();

            ArrayList<AccountProperty> properties = new ArrayList<>();
            if (map.containsKey("properties")) {
                Map<String, Object> p = (Map<String, Object>) map.get("properties");

                for (Map.Entry<String, Object> o : p.entrySet()) {

                    AccountProperty property =
                            new AccountProperty(String.valueOf(o.getKey()), (Map<String, Object>) o.getValue());
                    properties.add(property);
                }

                return new Account(new AccountID(map.get("id").toString()), properties.toArray(new AccountProperty[0]));
            }

            return null;
        }

        @Override
        public TreeNode convert(Account value) {
            Map<String, Object> properties = new TreeMap<>();
            for (AccountProperty property : value.getProperties()) {
                properties.put(property.getType(), property.getData());
            }
            Map<String, Object> map = new TreeMap<>();
            map.put("id", value.getID().toString());
            map.put("properties", properties);

            return new TreeNode(TreeNode.LEAF, timestamp, value.getID().getValue(), 0, null, null, map);
        }
    }
}
