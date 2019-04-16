package org.eontechnology.and.eon.app.IT;

import org.eontechnology.and.TestSigner;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.common.TimeProvider;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.eon.midleware.parsers.DelegateParser;
import org.eontechnology.and.peer.eon.midleware.parsers.PaymentParser;
import org.eontechnology.and.peer.eon.midleware.parsers.PublicationParser;
import org.eontechnology.and.peer.eon.midleware.parsers.RegistrationParser;
import org.eontechnology.and.peer.tx.TransactionType;
import org.eontechnology.and.peer.tx.midleware.builders.DelegateBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.PaymentBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.PublicationBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.RegistrationBuilder;
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

        ctx = new PeerContext(PeerStarterFactory.create()
                                                .route(TransactionType.Payment, new PaymentParser())
                                                .route(TransactionType.Registration, new RegistrationParser())
                                                .route(TransactionType.Delegate, new DelegateParser())
                                                .route(TransactionType.Publication, new PublicationParser())
                                                .seed(GENERATOR)
                                                .build(mockTimeProvider));

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

        Transaction tx = DelegateBuilder.createNew(new AccountID(ctx.getSigner().getPublicKey()), 0)
                                        .validity(timestamp, 3600)
                                        .build(ctx.getNetworkID(), ctx.getSigner());

        ctx.transactionBotService.putTransaction(tx);

        ctx.generateBlockForNow();
    }

    @Test
    public void step_0_PrePublicAccCanNotSign() throws Exception {

        Block lastBlock = ctx.blockExplorerService.getLastBlock();
        int timestamp = lastBlock.getTimestamp();

        // Pre-public account in fee payer not allowed
        AccountID publicAccId = new AccountID(ctx.getSigner().getPublicKey());
        Transaction tx5 = PaymentBuilder.createNew(10, publicAccId)
                                        .validity(timestamp, 3600)
                                        .payedBy(publicAccId)
                                        .build(ctx.getNetworkID(), delegate_1, new ISigner[] {ctx.getSigner()});

        try {
            ctx.transactionBotService.putTransaction(tx5);
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertEquals("Account " +
                                        new AccountID(ctx.getSigner().getPublicKey()) +
                                        " can not confirm a transaction.", ex.getCause().getMessage());
        }
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
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertEquals("Action is forbidden for public account.", ex.getCause().getMessage());
        }

        // Weight up
        Transaction tx3 = DelegateBuilder.createNew(new AccountID(ctx.getSigner().getPublicKey()), 80)
                                         .validity(timestamp, 3600)
                                         .build(ctx.getNetworkID(), ctx.getSigner(), new ISigner[] {delegate_1});

        try {
            ctx.transactionBotService.putTransaction(tx3);
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertEquals("Action is forbidden for public account.", ex.getCause().getMessage());
        }

        // Public account in fee payer not allowed
        AccountID publicAccId = new AccountID(ctx.getSigner().getPublicKey());
        Transaction tx5 = PaymentBuilder.createNew(10, publicAccId)
                                        .validity(timestamp, 3600)
                                        .payedBy(publicAccId)
                                        .build(ctx.getNetworkID(), delegate_1, new ISigner[] {ctx.getSigner()});

        try {
            ctx.transactionBotService.putTransaction(tx5);
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertEquals("Account " +
                                        new AccountID(ctx.getSigner().getPublicKey()) +
                                        " can not confirm a transaction.", ex.getCause().getMessage());
        }

        // Check tx normal processing
        Transaction tx4 = PaymentBuilder.createNew(10L, new AccountID(delegate_1.getPublicKey()))
                                        .validity(timestamp, 3600)
                                        .build(ctx.getNetworkID(), ctx.getSigner(), new ISigner[] {delegate_1});

        ctx.transactionBotService.putTransaction(tx4);

        ctx.generateBlockForNow();
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
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertEquals(
                    "The confirmation mode has been changed less than the 24 hours period. Do not use this seed more for personal operations.",
                    ex.getCause().getMessage());
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
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertEquals(
                    "A public account must not confirm transactions of other accounts. Do not use this seed more for personal operations.",
                    ex.getCause().getMessage());
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
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertEquals("Illegal validation mode. Do not use this seed more for personal operations.",
                                ex.getCause().getMessage());
        }

        ctx.generateBlockForNow();

        Assert.assertNull(ctx.accountBotService.getInformation(new AccountID(ctx.getSigner()
                                                                                .getPublicKey()).toString()).seed);
    }
}
