package org.eontechology.and.eon.app.IT;

import java.util.Random;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import org.eontechology.and.TestSigner;
import org.eontechology.and.eon.app.api.bot.AccountBotService;
import org.eontechology.and.peer.core.Constant;
import org.eontechology.and.peer.core.common.TimeProvider;
import org.eontechology.and.peer.core.crypto.ISigner;
import org.eontechology.and.peer.core.data.Block;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.ledger.storage.DbNode;
import org.eontechology.and.peer.eon.midleware.parsers.PaymentParser;
import org.eontechology.and.peer.tx.TransactionType;
import org.eontechology.and.peer.tx.midleware.builders.PaymentBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NodeCleanerTestIT {

    private static String GENERATOR1 = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static String GENERATOR2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";
    private TimeProvider mockTimeProvider1;
    private TimeProvider mockTimeProvider2;

    private PeerContext ctx1;
    private PeerContext ctx2;

    private String[] seeds = new String[] {
            "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e",
            "9e641020d3803008bf4e8a15ad05f84fb8eb3220037322ebc5fa58b70c3f1bd1",
            "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c",
            "d1bca0c433abc6fa3e418c53b7b723cea11ec97eb4494b403400052a73f2183a",
            "391b34d7f878c7f327fd244370edb9d521472e36816a36299341d0220662e0c2",
            "51e183f4b1ea5d4852bb727beae87e7b18503209d70d45d70e8d6a937209162f",
            "2806d51f6bcf1054a4ab484e3e3a30c33c441c9d9141f31b44e60cb798e9fa7d",
            "4bf315601c15a75bec0af369bc9cafb1fbbeadc41ee69696cd4e6781c5506c5f",
            "ec941f556e5bfc5e803140e496a1a972a9764a64d8ede972993e4f6f818f1210",
            "c14726d547ba633139908cefd6ee95268fa53b6bb606015a9dbc0508981a271a"
    };

    @Before
    public void setUp() throws Exception {
        mockTimeProvider1 = Mockito.mock(TimeProvider.class);
        mockTimeProvider2 = Mockito.mock(TimeProvider.class);

        ctx1 = new PeerContext(PeerStarterFactory.create()
                                                 .seed(GENERATOR1)
                                                 .route(TransactionType.Payment, new PaymentParser())
                                                 .build(mockTimeProvider1));

        ctx2 = new PeerContext(PeerStarterFactory.create()
                                                 .seed(GENERATOR2)
                                                 .route(TransactionType.Payment, new PaymentParser())
                                                 .build(mockTimeProvider2));

        ctx1.setPeerToConnect(ctx2);
        ctx2.setPeerToConnect(ctx1);
    }

    @Test
    public void step_1_cleaning() throws Exception {

        // change state
        changeState(new Random());

        // generate the blocks for a day
        Block lastBlock = ctx1.blockExplorerService.getLastBlock();
        int timestamp = lastBlock.getTimestamp() + 180 * (Constant.BLOCK_IN_DAY) + 1;
        Mockito.when(mockTimeProvider1.get()).thenReturn(timestamp);
        Mockito.when(mockTimeProvider2.get()).thenReturn(timestamp);

        ctx1.generateBlockForNow();
        ctx2.fullBlockSync();

        // change state
        changeState(new Random());

        // generate the blocks for a day
        lastBlock = ctx1.blockExplorerService.getLastBlock();
        timestamp = lastBlock.getTimestamp() + 180 * (2 * Constant.BLOCK_IN_DAY + 1) + 1;
        Mockito.when(mockTimeProvider1.get()).thenReturn(timestamp);
        Mockito.when(mockTimeProvider2.get()).thenReturn(timestamp);

        ctx1.generateBlockForNow();
        ctx2.fullBlockSync();

        // clear
        ctx1.nodesCleanupTask.run();
        ctx2.nodesCleanupTask.run();

        // 19 nodes for 10 accounts
        Dao<DbNode, Long> nodeDao1 = DaoManager.createDao(ctx1.storage.getConnectionSource(), DbNode.class);
        Dao<DbNode, Long> nodeDao2 = DaoManager.createDao(ctx2.storage.getConnectionSource(), DbNode.class);
        Assert.assertEquals(nodeDao1.countOf(), 19);
        Assert.assertEquals(nodeDao2.countOf(), 19);

        checkContext(ctx1);
        checkContext(ctx2);
    }

    private void checkContext(PeerContext ctx) throws Exception {
        for (String seed : seeds) {
            AccountBotService.State state =
                    ctx.accountBotService.getState(new AccountID(new TestSigner(seed).getPublicKey()).toString());
            Assert.assertEquals(state, AccountBotService.State.OK);
        }
    }

    private void changeState(Random random) throws Exception {
        Block lastBlock = ctx1.blockExplorerService.getLastBlock();
        Mockito.when(mockTimeProvider1.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
        Mockito.when(mockTimeProvider2.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

        AccountID recipient = new AccountID(new TestSigner(seeds[random.nextInt(seeds.length - 1)]).getPublicKey());

        ISigner signer = new TestSigner(seeds[random.nextInt(seeds.length - 1)]);
        Transaction tx = PaymentBuilder.createNew(100L, recipient)
                                       .validity(mockTimeProvider1.get(), 3600)
                                       .build(ctx1.getNetworkID(), signer);
        ctx1.transactionBotService.putTransaction(tx);

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

        ctx1.syncTransactionListTask.run();
        ctx2.syncTransactionListTask.run();
    }
}
