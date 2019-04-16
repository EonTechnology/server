package org.eontechnology.and.peer.core.blockchain.storage.converters;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import org.eontechnology.and.peer.core.blockchain.storage.DbTransaction;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.storage.Storage;

/**
 * Implementing a block with lazy loading of transactions.
 */
class LazyBlock extends Block {
    private final Storage storage;
    private boolean txLoaded = false;

    private QueryBuilder<DbTransaction, Long> getTransactionsBuilder = null;
    private ArgumentHolder vBlockID = new ThreadLocalSelectArg();

    LazyBlock(Storage storage) {
        super();
        this.storage = storage;
    }

    @Override
    public Collection<Transaction> getTransactions() {
        if (!txLoaded) {
            try {

                List<Transaction> transactions = new ArrayList<>();
                for (DbTransaction dbtx : loadTransactions(getID())) {
                    transactions.add(DTOConverter.convert(dbtx));
                }

                setTransactions(transactions);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return super.getTransactions();
    }

    @Override
    public void setTransactions(Collection<Transaction> transactions) {
        txLoaded = true;
        super.setTransactions(transactions);
    }

    private List<DbTransaction> loadTransactions(BlockID blockID) throws SQLException {

        if (getTransactionsBuilder == null) {

            Dao<DbTransaction, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);

            getTransactionsBuilder = dao.queryBuilder();
            getTransactionsBuilder.where().eq("block_id", vBlockID);
        }

        vBlockID.setValue(blockID.getValue());
        return getTransactionsBuilder.query();
    }
}