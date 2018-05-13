package com.exscudo.eon.app.IT;

import com.exscudo.TestSigner;
import com.exscudo.eon.app.cfg.PeerStarter;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.tx.midleware.builders.DelegateBuilder;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.PublicationBuilder;
import com.exscudo.peer.tx.midleware.builders.RegistrationBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PublicAccountTestIT {

    private static final String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static final String DELEGATE_1 = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    private static final String DELEGATE_2 = "112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00";

    private TimeProvider mockTimeProvider;
    private PeerContext ctx;

    private ISigner delegate_1;
    private ISigner delegate_2;

    @Before
    public void setUp() throws Exception {
        mockTimeProvider = Mockito.mock(TimeProvider.class);

        PeerStarter peerStarter = PeerStarterFactory.create(GENERATOR, mockTimeProvider);
        IFork fork = Utils.createFork(peerStarter.getStorage());
        peerStarter.setFork(fork);

        ctx = new PeerContext(peerStarter);

        delegate_1 = new TestSigner(DELEGATE_1);
        delegate_2 = new TestSigner(DELEGATE_2);

        prepare_peer();
    }

    private void prepare_peer() throws Exception {

        Block lastBlock = ctx.blockExplorerService.getLastBlock();

        // registration
        int timestamp = lastBlock.getTimestamp();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

        Transaction tx1 = RegistrationBuilder.createNew(delegate_1.getPublicKey())
                                             .validity(timestamp, 3600)
                                             .build(ctx.getNetworkID(), ctx.getSigner());
        Transaction tx2 = RegistrationBuilder.createNew(delegate_2.getPublicKey())
                                             .validity(timestamp, 3600)
                                             .build(ctx.getNetworkID(), ctx.getSigner());

        ctx.transactionBotService.putTransaction(tx1);
        ctx.transactionBotService.putTransaction(tx2);

        ctx.generateBlockForNow();

        // delegate + payment
        timestamp = mockTimeProvider.get();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

        Transaction tx3 = DelegateBuilder.createNew(new AccountID(delegate_1.getPublicKey()), 100)
                                         .validity(timestamp, 3600)
                                         .build(ctx.getNetworkID(), ctx.getSigner());
        Transaction tx4 = DelegateBuilder.createNew(new AccountID(delegate_2.getPublicKey()), 100)
                                         .validity(timestamp, 3600)
                                         .build(ctx.getNetworkID(), ctx.getSigner());

        ctx.transactionBotService.putTransaction(tx3);
        ctx.transactionBotService.putTransaction(tx4);

        Transaction tx5 = PaymentBuilder.createNew(1000000L, new AccountID(delegate_1.getPublicKey()))
                                        .validity(timestamp, 3600)
                                        .build(ctx.getNetworkID(), ctx.getSigner());
        Transaction tx6 = PaymentBuilder.createNew(1000000L, new AccountID(delegate_2.getPublicKey()))
                                        .validity(timestamp, 3600)
                                        .build(ctx.getNetworkID(), ctx.getSigner());

        ctx.transactionBotService.putTransaction(tx5);
        ctx.transactionBotService.putTransaction(tx6);

        ctx.generateBlockForNow();

        // delegate self to 0
        timestamp = mockTimeProvider.get();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

        Transaction tx = DelegateBuilder.createNew(new AccountID(ctx.getSigner().getPublicKey()))
                                        .validity(timestamp, 3600)
                                        .build(ctx.getNetworkID(), ctx.getSigner());

        ctx.transactionBotService.putTransaction(tx);

        ctx.generateBlockForNow();
    }

    @Test
    public void step_1_PublicAccNormal() throws Exception {

        Block lastBlock = ctx.blockExplorerService.getLastBlock();
        int timestamp = lastBlock.getTimestamp();

        // Generate 1 day
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * Constant.BLOCK_IN_DAY + 1);

        ctx.generateBlockForNow();

        // Publication acc
        timestamp = mockTimeProvider.get();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

        Transaction tx = PublicationBuilder.createNew(GENERATOR)
                                           .validity(timestamp, 3600)
                                           .build(ctx.getNetworkID(), ctx.getSigner(), new ISigner[] {delegate_1});
        ctx.transactionBotService.putTransaction(tx);

        ctx.generateBlockForNow();

        Assert.assertEquals(GENERATOR,
                            ctx.accountBotService.getInformation(new AccountID(ctx.getSigner()
                                                                                  .getPublicKey()).toString()).seed);

        // Delegate to me
        timestamp = mockTimeProvider.get();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);
        Transaction tx2 = DelegateBuilder.createNew(new AccountID(ctx.getSigner().getPublicKey()), 100)
                                         .validity(timestamp, 3600)
                                         .build(ctx.getNetworkID(), delegate_1);

        try {
            ctx.transactionBotService.putTransaction(tx2);
            Assert.assertTrue(false);
        } catch (Exception ignored) {
            Assert.assertTrue(true);
        }

        // Weight up
        Transaction tx3 = DelegateBuilder.createNew(new AccountID(ctx.getSigner().getPublicKey()), 80)
                                         .validity(timestamp, 3600)
                                         .build(ctx.getNetworkID(), ctx.getSigner(), new ISigner[] {delegate_1});

        try {
            ctx.transactionBotService.putTransaction(tx3);
            Assert.assertTrue(false);
        } catch (Exception ignored) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void step_2_PublicAccEarly() throws Exception {

        Block lastBlock = ctx.blockExplorerService.getLastBlock();
        int timestamp = lastBlock.getTimestamp();

        // Generate 0.5 days
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * Constant.BLOCK_IN_DAY / 2 + 1);

        ctx.generateBlockForNow();

        // Publication acc
        timestamp = mockTimeProvider.get();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

        Transaction tx = PublicationBuilder.createNew(GENERATOR)
                                           .validity(timestamp, 3600)
                                           .build(ctx.getNetworkID(), ctx.getSigner(), new ISigner[] {delegate_1});

        try {
            ctx.transactionBotService.putTransaction(tx);
            Assert.assertTrue(false);
        } catch (Exception ignored) {
        }

        ctx.generateBlockForNow();

        Assert.assertNull(ctx.accountBotService.getInformation(new AccountID(ctx.getSigner()
                                                                                .getPublicKey()).toString()).seed);
    }

    @Test
    public void step_3_DelegatedAcc() throws Exception {

        Block lastBlock = ctx.blockExplorerService.getLastBlock();
        int timestamp = lastBlock.getTimestamp();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

        // Delegate
        Transaction tx = DelegateBuilder.createNew(new AccountID(ctx.getSigner().getPublicKey()), 100)
                                        .validity(timestamp, 3600)
                                        .build(ctx.getNetworkID(), delegate_1);

        ctx.transactionBotService.putTransaction(tx);

        ctx.generateBlockForNow();

        // Generate 1 day
        timestamp = mockTimeProvider.get();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * Constant.BLOCK_IN_DAY + 1);

        ctx.generateBlockForNow();

        // Publication acc
        timestamp = mockTimeProvider.get();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

        Transaction tx2 = PublicationBuilder.createNew(GENERATOR)
                                            .validity(timestamp, 3600)
                                            .build(ctx.getNetworkID(), ctx.getSigner(), new ISigner[] {delegate_1});
        try {
            ctx.transactionBotService.putTransaction(tx2);
            Assert.assertTrue(false);
        } catch (Exception ignored) {
        }

        ctx.generateBlockForNow();

        Assert.assertNull(ctx.accountBotService.getInformation(new AccountID(ctx.getSigner()
                                                                                .getPublicKey()).toString()).seed);
    }

    @Test
    public void step_4_SignWeightExist() throws Exception {

        Block lastBlock = ctx.blockExplorerService.getLastBlock();
        int timestamp = lastBlock.getTimestamp();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

        // Delegate Weight to me
        Transaction tx = DelegateBuilder.createNew(new AccountID(ctx.getSigner().getPublicKey()), 80)
                                        .validity(timestamp, 3600)
                                        .build(ctx.getNetworkID(), ctx.getSigner(), new ISigner[] {delegate_1});

        ctx.transactionBotService.putTransaction(tx);

        ctx.generateBlockForNow();

        // Generate 1 day
        timestamp = mockTimeProvider.get();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * Constant.BLOCK_IN_DAY + 1);

        ctx.generateBlockForNow();

        // Publication acc
        timestamp = mockTimeProvider.get();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

        Transaction tx2 = PublicationBuilder.createNew(GENERATOR)
                                            .validity(timestamp, 3600)
                                            .build(ctx.getNetworkID(), ctx.getSigner(), new ISigner[] {delegate_1});
        try {
            ctx.transactionBotService.putTransaction(tx2);
            Assert.assertTrue(false);
        } catch (Exception ignored) {
        }

        ctx.generateBlockForNow();

        Assert.assertNull(ctx.accountBotService.getInformation(new AccountID(ctx.getSigner()
                                                                                .getPublicKey()).toString()).seed);
    }
}
