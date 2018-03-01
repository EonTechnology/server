package com.exscudo.eon.IT;

import com.exscudo.eon.bot.AccountService;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.ed25519.Ed25519Signer;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.tx.builders.DepositBuilder;
import com.exscudo.peer.eon.tx.builders.PaymentBuilder;
import com.exscudo.peer.eon.tx.builders.RegistrationBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TransactionTestIT {
    private static final String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static final String GENERATOR2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";
    private static final String GENERATOR_NEW = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    private TimeProvider mockTimeProvider;

    @Before
    public void setUp() throws Exception {
        mockTimeProvider = Mockito.mock(TimeProvider.class);
    }

    @Test
    public void step_1_doubleSending() throws Exception {
        PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
        PeerContext ctx2 = new PeerContext(GENERATOR2, mockTimeProvider);
        ISigner signer = new Ed25519Signer(GENERATOR_NEW);

        Block lastBlock = ctx.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

        Transaction tx = RegistrationBuilder.createNew(signer.getPublicKey())
                                            .validity(lastBlock.getTimestamp() + 100, 3600)
                                            .build(ctx.getSigner());
        ctx.transactionBotService.putTransaction(tx);
        ctx.generateBlockForNow();

        Assert.assertEquals("Registration in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

        Transaction tx2 = PaymentBuilder.createNew(10000L, new AccountID(signer.getPublicKey()))
                                        .validity(lastBlock.getTimestamp() + 200, 3600)
                                        .build(ctx.getSigner());

        ctx.transactionBotService.putTransaction(tx2);
        ctx.generateBlockForNow();

        Assert.assertEquals("Payment in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());

        ctx2.setPeerToConnect(ctx);
        ctx.setPeerToConnect(ctx2);

        ctx2.fullBlockSync();

        Assert.assertEquals("Block synchronized",
                            ctx.blockchain.getLastBlock().getID(),
                            ctx2.blockchain.getLastBlock().getID());

        Transaction tx3 = PaymentBuilder.createNew(8000L, new AccountID(ctx.getSigner().getPublicKey()))
                                        .forFee(1000L)
                                        .validity(lastBlock.getTimestamp() + 200, 3600)
                                        .build(signer);
        Transaction tx4 = PaymentBuilder.createNew(8000L, new AccountID(ctx2.getSigner().getPublicKey()))
                                        .forFee(1000L)
                                        .validity(lastBlock.getTimestamp() + 200, 3600)
                                        .build(signer);

        ctx.transactionBotService.putTransaction(tx3);
        ctx2.transactionBotService.putTransaction(tx4);

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 3 + 1);

        ctx.syncTransactionListTask.run();
        ctx2.syncTransactionListTask.run();

        Assert.assertTrue(ctx.backlog.contains(tx3.getID()));
        Assert.assertFalse(ctx.backlog.contains(tx4.getID()));
        Assert.assertFalse(ctx2.backlog.contains(tx3.getID()));
        Assert.assertTrue(ctx2.backlog.contains(tx4.getID()));

        ctx.generateBlockForNow();
        ctx2.generateBlockForNow();

        Assert.assertNotEquals("Block different",
                               ctx.blockchain.getLastBlock().getID(),
                               ctx2.blockchain.getLastBlock().getID());
        Assert.assertEquals("Block TIMESTAMP equals",
                            ctx.blockchain.getLastBlock().getTimestamp(),
                            ctx2.blockchain.getLastBlock().getTimestamp());

        ctx.fullBlockSync();
        ctx2.fullBlockSync();

        Assert.assertEquals("Block synchronized",
                            ctx.blockchain.getLastBlock().getID(),
                            ctx2.blockchain.getLastBlock().getID());

        Assert.assertFalse(ctx.backlog.contains(tx3.getID()));
        Assert.assertFalse(ctx.backlog.contains(tx4.getID()));
        Assert.assertFalse(ctx2.backlog.contains(tx3.getID()));
        Assert.assertFalse(ctx2.backlog.contains(tx4.getID()));
    }

    @Test
    public void step_2_balances_checker() throws Exception {
        // Init peer and etc...
        PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
        ISigner signer = new Ed25519Signer(GENERATOR2);
        ISigner signerNew = new Ed25519Signer(GENERATOR_NEW);

        Block lastBlock = ctx.blockchain.getLastBlock();

        String signerID = new AccountID(signer.getPublicKey()).toString();
        String signerCtxID = new AccountID(ctx.getSigner().getPublicKey()).toString();
        String signerNewID = new AccountID(signerNew.getPublicKey()).toString();

        // Create new acc
        AccountService.EONBalance balance = ctx.accountBotService.getBalance(signerID);
        AccountService.EONBalance balanceCtx = ctx.accountBotService.getBalance(signerCtxID);
        AccountService.EONBalance balanceNew = ctx.accountBotService.getBalance(signerNewID);

        Assert.assertEquals(AccountService.State.NotFound, ctx.accountBotService.getState(signerNewID));
        Assert.assertEquals(AccountService.State.Unauthorized, balanceNew.state);

        Transaction tx = RegistrationBuilder.createNew(signerNew.getPublicKey())
                                            .validity(lastBlock.getTimestamp() + 100, 3600)
                                            .build(signer);
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
        ctx.transactionBotService.putTransaction(tx);

        balanceNew = ctx.accountBotService.getBalance(signerNewID);
        Assert.assertEquals(AccountService.State.Processing, ctx.accountBotService.getState(signerNewID));
        Assert.assertEquals(AccountService.State.Unauthorized, balanceNew.state);

        ctx.generateBlockForNow();

        Assert.assertEquals("Registration in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());
        Assert.assertEquals("Fee in generator balance",
                            balanceCtx.amount + tx.getFee(),
                            ctx.accountBotService.getBalance(signerCtxID).amount);
        Assert.assertEquals("Fee from sender",
                            balance.amount - tx.getFee(),
                            ctx.accountBotService.getBalance(signerID).amount);
        Assert.assertEquals("New acc created",
                            AccountService.State.OK,
                            ctx.accountBotService.getInformation(signerNewID).state);

        // Payment to new acc
        balance = ctx.accountBotService.getBalance(signerID);
        balanceCtx = ctx.accountBotService.getBalance(signerCtxID);
        balanceNew = ctx.accountBotService.getBalance(signerNewID);

        Transaction tx2 = PaymentBuilder.createNew(10000L, new AccountID(signerNew.getPublicKey()))
                                        .validity(lastBlock.getTimestamp() + 200, 3600)
                                        .build(signer);
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);
        ctx.transactionBotService.putTransaction(tx2);
        ctx.generateBlockForNow();

        Assert.assertEquals("Payment in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());
        Assert.assertEquals("Fee in generator balance",
                            balanceCtx.amount + tx2.getFee(),
                            ctx.accountBotService.getBalance(signerCtxID).amount);
        Assert.assertEquals("Fee and amount from sender",
                            balance.amount - (10000L + tx2.getFee()),
                            ctx.accountBotService.getBalance(signerID).amount);
        Assert.assertEquals("Amount to recipient",
                            balanceNew.amount + 10000L,
                            ctx.accountBotService.getBalance(signerNewID).amount);

        // Deposit in new acc
        balanceCtx = ctx.accountBotService.getBalance(signerCtxID);
        balanceNew = ctx.accountBotService.getBalance(signerNewID);
        AccountService.Info informationNew = ctx.accountBotService.getInformation(signerNewID);

        Transaction tx3 =
                DepositBuilder.createNew(100L).validity(lastBlock.getTimestamp() + 200, 3600).build(signerNew);
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 3 + 1);
        ctx.transactionBotService.putTransaction(tx3);
        ctx.generateBlockForNow();

        Assert.assertEquals("Deposit.refill in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());
        Assert.assertEquals("Fee in generator balance",
                            balanceCtx.amount + tx3.getFee(),
                            ctx.accountBotService.getBalance(signerCtxID).amount);
        Assert.assertEquals("Fee and amount from sender",
                            balanceNew.amount - (100L + tx3.getFee()),
                            ctx.accountBotService.getBalance(signerNewID).amount);

        Assert.assertTrue("Deposit in sender", ctx.accountBotService.getInformation(signerNewID).deposit == 100L);

        // Deposit from new acc
        balanceCtx = ctx.accountBotService.getBalance(signerCtxID);
        balanceNew = ctx.accountBotService.getBalance(signerNewID);
        informationNew = ctx.accountBotService.getInformation(signerNewID);

        Transaction tx4 = DepositBuilder.createNew(50L).validity(lastBlock.getTimestamp() + 200, 3600).build(signerNew);
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 4 + 1);
        ctx.transactionBotService.putTransaction(tx4);
        ctx.generateBlockForNow();

        Assert.assertEquals("Deposit.withdraw in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());
        Assert.assertEquals("Fee in generator balance",
                            balanceCtx.amount + tx4.getFee(),
                            ctx.accountBotService.getBalance(signerCtxID).amount);
        Assert.assertEquals("Fee and amount from sender",
                            balanceNew.amount + 50L - tx4.getFee(),
                            ctx.accountBotService.getBalance(signerNewID).amount);
        Assert.assertTrue("Deposit in sender", ctx.accountBotService.getInformation(signerNewID).deposit == 50L);
    }

    @Test
    public void step_3_transaction_duplicate() throws Exception {

        PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
        ISigner signerNew = new Ed25519Signer(GENERATOR_NEW);

        Block lastBlock = ctx.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);
        Transaction tx = RegistrationBuilder.createNew(signerNew.getPublicKey())
                                            .validity(mockTimeProvider.get(), 3600)
                                            .build(ctx.signer);
        Assert.assertTrue(ctx.backlog.put(tx));
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

        ctx.generateBlockForNow();
        Assert.assertEquals("Registration in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());

        Transaction tx1 = PaymentBuilder.createNew(100L, new AccountID(signerNew.getPublicKey()))
                                        .validity(mockTimeProvider.get(), 3600)
                                        .build(ctx.signer);
        Assert.assertTrue(ctx.backlog.put(tx1));
        Assert.assertFalse(ctx.backlog.put(tx1));
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 2 * 180 + 1);

        ctx.generateBlockForNow();
        Assert.assertEquals("Payment in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());

        Assert.assertTrue(ctx.backlog.put(tx1));
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 3 * 180 + 1);

        ctx.generateBlockForNow();
        Assert.assertEquals("Emprty block", 0, ctx.blockchain.getLastBlock().getTransactions().size());
    }

    @Test
    public void step_4_double_spending() throws Exception {

        PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
        ISigner signerNew = new Ed25519Signer(GENERATOR_NEW);

        Block lastBlock = ctx.blockchain.getLastBlock();
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);

        // registration
        Transaction tx = RegistrationBuilder.createNew(signerNew.getPublicKey())
                                            .validity(mockTimeProvider.get(), 3600)
                                            .build(ctx.signer);
        Assert.assertTrue(ctx.backlog.put(tx));

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
        ctx.generateBlockForNow();

        Assert.assertEquals("Registration in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());

        // payment
        Transaction tx1 = PaymentBuilder.createNew(100L, new AccountID(signerNew.getPublicKey()))
                                        .validity(mockTimeProvider.get(), 3600)
                                        .build(ctx.signer);
        Assert.assertTrue(ctx.backlog.put(tx1));

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 2 * 180 + 1);
        ctx.generateBlockForNow();

        Assert.assertEquals("Payment in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());

        // back payment
        Transaction tx2 = PaymentBuilder.createNew(50L, new AccountID(ctx.signer.getPublicKey()))
                                        .validity(mockTimeProvider.get(), 3600)
                                        .build(signerNew);
        Assert.assertTrue(ctx.backlog.put(tx2));
        Transaction tx3 = PaymentBuilder.createNew(70L, new AccountID(ctx.signer.getPublicKey()))
                                        .validity(mockTimeProvider.get(), 3600)
                                        .build(signerNew);
        try {
            ctx.backlog.put(tx3);
        } catch (ValidateException ignore) {

        }

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 3 * 180 + 1);
        ctx.generateBlockForNow();

        Assert.assertEquals("Payment in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());
    }
}
