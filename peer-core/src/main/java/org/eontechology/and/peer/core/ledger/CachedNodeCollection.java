package org.eontechology.and.peer.core.ledger;

import java.sql.SQLException;
import java.util.Map;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import org.eontechology.and.peer.core.common.exceptions.DataAccessException;
import org.eontechology.and.peer.core.ledger.storage.DbNode;
import org.eontechology.and.peer.core.ledger.storage.DbNodeCache;
import org.eontechology.and.peer.core.ledger.storage.DbNodeHelper;
import org.eontechology.and.peer.core.ledger.tree.ITreeNodeCollection;
import org.eontechology.and.peer.core.ledger.tree.TreeNode;
import org.eontechology.and.peer.core.ledger.tree.TreeNodeID;
import org.eontechology.and.peer.core.storage.Storage;

public class CachedNodeCollection implements ITreeNodeCollection {
    private final Storage storage;
    private DbNodeHelper dbNodeHelper;

    public CachedNodeCollection(Storage storage) {
        this.storage = storage;

        this.dbNodeHelper = new DbNodeHelper(storage.getConnectionSource());
    }

    private static DbNode convert(TreeNode node) {

        String value = null;
        if (node.getValues() != null) {
            Bencode bencode = new Bencode();
            value = new String(bencode.encode(node.getValues()), bencode.getCharset());
        }
        String lNodeID = null;
        if (node.getLeftNodeID() != null) {
            lNodeID = node.getLeftNodeID().getKey();
        }
        String rNodeID = null;
        if (node.getRightNodeID() != null) {
            rNodeID = node.getRightNodeID().getKey();
        }
        TreeNodeID id = node.getID();

        DbNode dbNode = new DbNode();
        dbNode.setKey(id.getKey());
        dbNode.setIndex(id.getIndex());
        dbNode.setTimestamp(node.getTimestamp());
        dbNode.setMask(node.getMask());
        dbNode.setMaskLength(node.getMaskLength());
        dbNode.setType(node.getType());
        dbNode.setLeftNode(lNodeID);
        dbNode.setRightNode(rNodeID);
        dbNode.setValue(value);

        return dbNode;
    }

    private static TreeNode convert(DbNode dbNode) {

        Map<String, Object> map = null;

        String value = dbNode.getValue();
        if (value != null && value.length() != 0) {
            Bencode bencode = new Bencode();
            map = bencode.decode(value.getBytes(bencode.getCharset()), Type.DICTIONARY);
        }

        return new TreeNode(dbNode.getType(),
                            dbNode.getTimestamp(),
                            dbNode.getMask(),
                            dbNode.getMaskLength(),
                            TreeNodeID.valueOf(dbNode.getLeftNode()),
                            TreeNodeID.valueOf(dbNode.getRightNode()),
                            map);
    }

    private DbNodeCache getCache() {
        return storage.getDbNodeCache();
    }

    @Override
    public TreeNode get(TreeNodeID id) {
        DbNode dbNode = find(id);
        if (dbNode == null) {
            return null;
        }
        return convert(dbNode);
    }

    @Override
    public void add(TreeNode node) {
        try {
            DbNode dbn = find(node.getID());
            if (dbn != null) {
                if (dbn.getTimestamp() < node.getTimestamp()) {
                    if (dbNodeHelper.updateTimestamp(node.getID().getKey(),
                                                     node.getID().getIndex(),
                                                     node.getTimestamp())) {
                        dbn.setTimestamp(node.getTimestamp());
                        return;
                    }
                } else {
                    return;
                }
            }

            dbn = convert(node);
            dbNodeHelper.put(dbn);

            DbNodeCache dbCache = getCache();
            if (dbCache != null) {
                dbCache.put(dbn.getIndex(), dbn);
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    private DbNode find(TreeNodeID id) {
        DbNodeCache dbCache = getCache();

        DbNode node;
        if (dbCache != null) {
            node = dbCache.get(id.getIndex());
            if (node != null && node.getKey().equals(id.getKey())) {
                return node;
            }
        }

        try {
            node = dbNodeHelper.get(id.getKey(), id.getIndex());
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }

        if (dbCache != null && node != null) {
            dbCache.put(node.getIndex(), node);
        }

        return node;
    }
}
