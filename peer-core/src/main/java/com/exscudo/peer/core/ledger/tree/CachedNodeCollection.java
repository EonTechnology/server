package com.exscudo.peer.core.ledger.tree;

import java.sql.SQLException;
import java.util.Map;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.ledger.storage.DbNode;
import com.exscudo.peer.core.ledger.storage.DbNodeCache;
import com.exscudo.peer.core.ledger.storage.DbNodeHelper;
import com.exscudo.peer.core.storage.CacheManager;
import com.j256.ormlite.support.ConnectionSource;

public class CachedNodeCollection implements ITreeNodeCollection {
    private final ConnectionSource connectionSource;
    private DbNodeCache cache;
    private DbNodeHelper dbNodeHelper;

    public CachedNodeCollection(ConnectionSource connectionSource) {
        this.connectionSource = connectionSource;
        this.cache = null;

        this.dbNodeHelper = new DbNodeHelper(connectionSource);
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
        if (cache == null) {
            cache = CacheManager.lookupCache(connectionSource, DbNodeCache.class);
        }
        return cache;
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
        if (find(node.getID()) != null) {
            return;
        }
        DbNode dbn = convert(node);
        try {

            dbNodeHelper.put(dbn);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }

        DbNodeCache dbCache = getCache();
        if (dbCache != null) {
            dbCache.put(dbn.getIndex(), dbn);
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
