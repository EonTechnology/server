package com.exscudo.eon.IT;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.storage.tasks.Cleaner;
import com.exscudo.peer.core.storage.utils.DbBlock;
import com.exscudo.peer.core.storage.utils.DbTransaction;
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

    private static long BLOCK_COUNT = 10;

    private static String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static String GENERATOR2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";
    private TimeProvider mockTimeProvider1;
    private TimeProvider mockTimeProvider2;

    private PeerContext ctx1;
    private PeerContext ctx2;

    private Cleaner cleaner1;
    private Cleaner cleaner2;

    @Before
    public void setUp() throws Exception {
        mockTimeProvider1 = Mockito.mock(TimeProvider.class);
        mockTimeProvider2 = Mockito.mock(TimeProvider.class);

        ctx1 = new PeerContext(GENERATOR, mockTimeProvider1);
        ctx2 = new PeerContext(GENERATOR2, mockTimeProvider2);

        ctx1.syncBlockPeerService = Mockito.spy(ctx1.syncBlockPeerService);
        ctx2.syncBlockPeerService = Mockito.spy(ctx2.syncBlockPeerService);

        ctx1.setPeerToConnect(ctx2);
        ctx2.setPeerToConnect(ctx1);

        cleaner1 = new Cleaner(ctx1.blockchain, ctx1.storage);
        cleaner2 = new Cleaner(ctx2.blockchain, ctx2.storage);
    }

    @Test
    public void step_1_cleaning() throws Exception {

        AccountID recipient1 = new AccountID(ctx1.getSigner().getPublicKey());
        AccountID recipient2 = new AccountID(ctx2.getSigner().getPublicKey());

        for (int i = 0; i < BLOCK_COUNT; i++) {

            Block lastBlock = ctx1.blockchain.getLastBlock();
            Mockito.when(mockTimeProvider1.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
            Mockito.when(mockTimeProvider2.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

            Transaction tx1 = PaymentBuilder.createNew(10000L, recipient2)
                                            .validity(mockTimeProvider1.get(), 3600)
                                            .build(ctx1.getSigner());
            Transaction tx2 = PaymentBuilder.createNew(10000L, recipient1)
                                            .validity(mockTimeProvider2.get(), 3600)
                                            .build(ctx1.getSigner());

            ctx1.transactionBotService.putTransaction(tx1);
            ctx2.transactionBotService.putTransaction(tx2);

            ctx1.syncTransactionListTask.run();
            ctx2.syncTransactionListTask.run();

            ctx1.generateBlockForNow();
            ctx2.generateBlockForNow();

            Assert.assertNotEquals("Blockchain different",
                                   ctx1.blockchain.getLastBlock().getID(),
                                   ctx2.blockchain.getLastBlock().getID());

            ctx1.fullBlockSync();
            ctx2.fullBlockSync();

            Assert.assertEquals("Blockchain synchronized",
                                ctx1.blockchain.getLastBlock().getID(),
                                ctx2.blockchain.getLastBlock().getID());
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

        Block lastBlock = ctx1.blockchain.getLastBlock();
        Mockito.when(mockTimeProvider1.get())
               .thenReturn(lastBlock.getTimestamp() + 180 * (Constant.BLOCK_IN_DAY + 1) + 1);
        Mockito.when(mockTimeProvider2.get())
               .thenReturn(lastBlock.getTimestamp() + 180 * (Constant.BLOCK_IN_DAY + 1) + 1);

        ctx1.generateBlockForNow();
        ctx2.fullBlockSync();

        cleaner1.run();
        cleaner2.run();

        Assert.assertNotEquals(txC1, txDao1.countOf());
        Assert.assertNotEquals(txC2, txDao2.countOf());
        Assert.assertNotEquals(txB1, blockDao1.countOf());
        Assert.assertNotEquals(txB2, blockDao2.countOf());

        // BLOCK_COUNT transactions sent to each feast
        Assert.assertEquals(BLOCK_COUNT * 2, txDao1.countOf());
        Assert.assertEquals(BLOCK_COUNT * 2, txDao2.countOf());

        // (BLOCK_IN_DAY + BLOCK_COUNT + 1) blocks after, but in DB zero block and genesis block
        Assert.assertEquals(Constant.BLOCK_IN_DAY + BLOCK_COUNT + 1, blockDao1.countOf() - 2);
        Assert.assertEquals(Constant.BLOCK_IN_DAY + BLOCK_COUNT + 1, blockDao2.countOf() - 2);
    }
}
