package com.exscudo.eon.IT;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.blockchain.storage.DbBlock;
import com.exscudo.peer.core.blockchain.storage.DbTransaction;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.tx.builders.PaymentBuilder;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BlockCleanerTestIT {

    private static int BLOCK_COUNT = 10;
    private static int LIMIT_BLOCK_COUNT = Constant.STORAGE_FRAME_BLOCK + 10;

    private static String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static String GENERATOR2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";
    private TimeProvider mockTimeProvider;

    private PeerContext ctx1;
    private PeerContext ctx2;

    @Before
    public void setUp() throws Exception {
        mockTimeProvider = Mockito.mock(TimeProvider.class);

        ctx1 = new PeerContext(GENERATOR, mockTimeProvider, true);
        ctx2 = new PeerContext(GENERATOR2, mockTimeProvider, false);

        ctx1.syncBlockPeerService = Mockito.spy(ctx1.syncBlockPeerService);
        ctx2.syncBlockPeerService = Mockito.spy(ctx2.syncBlockPeerService);

        ctx1.setPeerToConnect(ctx2);
        ctx2.setPeerToConnect(ctx1);

        // Init peer with snapshot sync
        ctx2.syncSnapshotTask.run();
    }

    @Test
    public void step_1_cleaning() throws Exception {

        AccountID recipient1 = new AccountID(ctx1.getSigner().getPublicKey());
        AccountID recipient2 = new AccountID(ctx2.getSigner().getPublicKey());

        for (int i = 0; i < BLOCK_COUNT; i++) {

            Block lastBlock = ctx1.blockExplorerService.getLastBlock();
            Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

            Transaction tx1 = PaymentBuilder.createNew(10000L, recipient2)
                                            .validity(mockTimeProvider.get(), 3600)
                                            .build(ctx1.getSigner());
            Transaction tx2 = PaymentBuilder.createNew(10000L, recipient1)
                                            .validity(mockTimeProvider.get(), 3600)
                                            .build(ctx1.getSigner());

            ctx1.transactionBotService.putTransaction(tx1);
            ctx2.transactionBotService.putTransaction(tx2);

            ctx1.syncTransactionListTask.run();
            ctx2.syncTransactionListTask.run();

            ctx1.generateBlockForNow();
            ctx2.generateBlockForNow();

            Assert.assertNotEquals("Blockchain different",
                                   ctx1.blockExplorerService.getLastBlock().getID(),
                                   ctx2.blockExplorerService.getLastBlock().getID());

            ctx1.fullBlockSync();
            ctx2.fullBlockSync();

            Assert.assertEquals("Blockchain synchronized",
                                ctx1.blockExplorerService.getLastBlock().getID(),
                                ctx2.blockExplorerService.getLastBlock().getID());
        }

        ctx1.syncTransactionListTask.run();
        ctx2.syncTransactionListTask.run();

        Dao<DbTransaction, Long> txDao1 = DaoManager.createDao(ctx1.storage.getConnectionSource(), DbTransaction.class);
        Dao<DbTransaction, Long> txDao2 = DaoManager.createDao(ctx2.storage.getConnectionSource(), DbTransaction.class);
        Dao<DbBlock, Long> blockDao1 = DaoManager.createDao(ctx1.storage.getConnectionSource(), DbBlock.class);
        Dao<DbBlock, Long> blockDao2 = DaoManager.createDao(ctx2.storage.getConnectionSource(), DbBlock.class);

        long txC1 = txDao1.countOf();
        long txC2 = txDao2.countOf();
        long txB1 = blockDao1.countOf();
        long txB2 = blockDao2.countOf();

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();
        int timestamp = lastBlock.getTimestamp() + 180 * LIMIT_BLOCK_COUNT + 1;
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp);

        ctx1.generateBlockForNow();
        ctx2.fullBlockSync();

        ctx1.branchesCleanupTask.run();
        ctx2.branchesCleanupTask.run();

        Assert.assertNotEquals(txC1, txDao1.countOf());
        Assert.assertNotEquals(txC2, txDao2.countOf());
        Assert.assertNotEquals(txB1, blockDao1.countOf());
        Assert.assertNotEquals(txB2, blockDao2.countOf());

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());
        Assert.assertEquals("Blockchain synchronized and not empty",
                            ctx1.blockExplorerService.getByHeight(LIMIT_BLOCK_COUNT).getID(),
                            ctx2.blockExplorerService.getByHeight(LIMIT_BLOCK_COUNT).getID());

        Assert.assertNotNull("Block not deleted", ctx1.blockchain.getBlockByHeight(5));
        Assert.assertNull("Block deleted", ctx2.blockchain.getBlockByHeight(5));

        // Full history
        // BLOCK_COUNT transactions sent to each feast
        Assert.assertEquals(BLOCK_COUNT * 2, txDao1.countOf());
        // (BLOCK_IN_DAY + BLOCK_COUNT + 1) blocks after, but in DB zero block and genesis block
        Assert.assertEquals(BLOCK_COUNT + LIMIT_BLOCK_COUNT, blockDao1.countOf() - 2);

        // Cleaned history
        Assert.assertEquals(0, txDao2.countOf());
        Assert.assertEquals(Constant.STORAGE_FRAME_BLOCK, blockDao2.countOf() - 3);
    }
}
