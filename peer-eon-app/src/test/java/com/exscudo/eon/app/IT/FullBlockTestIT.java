package com.exscudo.eon.app.IT;

import com.exscudo.TestSigner;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class FullBlockTestIT {

    private static final int REPEAT_COUNT = 400;

    protected static String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    protected PeerContext ctx;
    protected TimeProvider mockTimeProvider;
    private String[] seedSet = new String[] {
            "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e",
            "9e641020d3803008bf4e8a15ad05f84fb8eb3220037322ebc5fa58b70c3f1bd1",
            "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c",
            "d1bca0c433abc6fa3e418c53b7b723cea11ec97eb4494b403400052a73f2183a",
            "391b34d7f878c7f327fd244370edb9d521472e36816a36299341d0220662e0c2",
            "51e183f4b1ea5d4852bb727beae87e7b18503209d70d45d70e8d6a937209162f",
            "2806d51f6bcf1054a4ab484e3e3a30c33c441c9d9141f31b44e60cb798e9fa7d",
            "4bf315601c15a75bec0af369bc9cafb1fbbeadc41ee69696cd4e6781c5506c5f",
            "ec941f556e5bfc5e803140e496a1a972a9764a64d8ede972993e4f6f818f1210",
            "c14726d547ba633139908cefd6ee95268fa53b6bb606015a9dbc0508981a271a",
            };

    @Before
    public void setUp() throws Exception {
        mockTimeProvider = Mockito.mock(TimeProvider.class);
        ctx = new PeerContext(PeerStarterFactory.create(GENERATOR, mockTimeProvider));

        Block lastBlock = ctx.blockExplorerService.getLastBlock();
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);
    }

    @Test
    public void test1() throws Exception {
        Thread[] threads = new Thread[seedSet.length];

        for (int i = 0; i < seedSet.length; i++) {

            ISigner signer = new TestSigner(seedSet[i % seedSet.length]);
            AccountID to = new AccountID(new TestSigner(seedSet[(i + 1) % seedSet.length]).getPublicKey());

            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int k = 0; k < REPEAT_COUNT; k++) {
                            Transaction tx = PaymentBuilder.createNew(1000, to)
                                                           .validity(mockTimeProvider.get() - k, 3600)
                                                           .build(ctx.getNetworkID(), signer);
                            ctx.transactionBotService.putTransaction(tx);
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
        }
        for (int i = 0; i < seedSet.length; i++) {
            threads[i].start();
        }
        for (int i = 0; i < seedSet.length; i++) {
            threads[i].join();
        }

        Block lastBlock = ctx.blockExplorerService.getLastBlock();
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
        ctx.generateBlockForNow();

        Block newLastBlock = ctx.blockExplorerService.getLastBlock();
        Assert.assertNotEquals(lastBlock.getID(), newLastBlock.getID());
        Assert.assertNotEquals(0, newLastBlock.getTransactions().size());

        Assert.assertNotEquals(ctx.backlogExplorerService.getLatest(1).size(), 0);
    }
}
