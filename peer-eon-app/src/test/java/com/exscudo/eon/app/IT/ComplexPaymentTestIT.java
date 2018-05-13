package com.exscudo.eon.app.IT;

import com.exscudo.TestSigner;
import com.exscudo.eon.app.api.bot.AccountBotService;
import com.exscudo.eon.app.cfg.PeerStarter;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.tx.ColoredCoinID;
import com.exscudo.peer.tx.midleware.builders.ColoredCoinRegistrationBuilder;
import com.exscudo.peer.tx.midleware.builders.ColoredPaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.ComplexPaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.RegistrationBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ComplexPaymentTestIT {

    private static final String ACCOUNT_SEED1 = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static final String NEW_ACCOUNT = "2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0000";
    private TimeProvider timeProvider;
    private PeerContext ctx;

    @Before
    public void setUp() throws Exception {
        timeProvider = Mockito.mock(TimeProvider.class);

        PeerStarter peerStarter = PeerStarterFactory.create(ACCOUNT_SEED1, timeProvider);
        IFork fork = Utils.createFork(peerStarter.getStorage());
        peerStarter.setFork(fork);
        ctx = new PeerContext(peerStarter);
    }

    @Test
    public void step1_complex_payment() throws Exception {

        ISigner newAccountSigner = new TestSigner(NEW_ACCOUNT);
        AccountID newAccountID = new AccountID(newAccountSigner.getPublicKey());

        AccountID accountID = new AccountID(ctx.getSigner().getPublicKey());

        Block lastBlock = ctx.blockExplorerService.getLastBlock();
        Mockito.when(timeProvider.get()).thenReturn(lastBlock.getTimestamp());

        // preliminary check

        AccountBotService.EONBalance accountBalance = ctx.accountBotService.getBalance(accountID.toString());
        AccountBotService.EONBalance newAccountBalance = ctx.accountBotService.getBalance(newAccountID.toString());
        Assert.assertEquals(accountBalance.state, AccountBotService.State.OK);
        Assert.assertEquals(newAccountBalance.state, AccountBotService.State.Unauthorized);

        // registration

        Transaction tx1 = RegistrationBuilder.createNew(newAccountSigner.getPublicKey())
                                             .validity(timeProvider.get(), 3600)
                                             .build(ctx.getNetworkID(), ctx.getSigner());
        ctx.transactionBotService.putTransaction(tx1);

        Mockito.when(timeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
        ctx.generateBlockForNow();

        // payment

        lastBlock = ctx.blockExplorerService.getLastBlock();

        Transaction tx2 = PaymentBuilder.createNew(1000L, newAccountID)
                                        .validity(timeProvider.get(), 3600)
                                        .build(ctx.getNetworkID(), ctx.getSigner());
        ctx.transactionBotService.putTransaction(tx2);

        Mockito.when(timeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
        ctx.generateBlockForNow();

        // colored account

        lastBlock = ctx.blockExplorerService.getLastBlock();

        Transaction tx3 = ColoredCoinRegistrationBuilder.createNew(1000L, 1)
                                                        .validity(timeProvider.get(), 3600)
                                                        .build(ctx.getNetworkID(), newAccountSigner);
        ctx.transactionBotService.putTransaction(tx3);

        Mockito.when(timeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
        ctx.generateBlockForNow();

        // check state

        accountBalance = ctx.accountBotService.getBalance(accountID.toString());
        Assert.assertEquals(accountBalance.amount, 999999010L);
        Assert.assertNull(accountBalance.coloredCoins);
        newAccountBalance = ctx.accountBotService.getBalance(newAccountID.toString());
        Assert.assertEquals(newAccountBalance.amount, 990L);
        Assert.assertEquals(newAccountBalance.coloredCoins.get((new ColoredCoinID(newAccountID)).toString()),
                            (Long) 1000L);

        // put complex transaction to backlog

        lastBlock = ctx.blockExplorerService.getLastBlock();
        Transaction nestedTx1 = PaymentBuilder.createNew(100L, newAccountID)
                                              .validity(timeProvider.get() - 1, 3600)
                                              .forFee(0L)
                                              .build(ctx.getNetworkID(), ctx.getSigner());
        Transaction nestedTx2 = ColoredPaymentBuilder.createNew(10L,
                                                                new ColoredCoinID(newAccountID),
                                                                new AccountID(ctx.getSigner().getPublicKey()))
                                                     .validity(timeProvider.get() - 1, 3600)
                                                     .forFee(0L)
                                                     .refBy(nestedTx1.getID())
                                                     .build(ctx.getNetworkID(), newAccountSigner);
        Transaction nestedTx3 = PaymentBuilder.createNew(1L, accountID)
                                              .validity(timeProvider.get() - 1, 3600)
                                              .forFee(0L)
                                              .refBy(nestedTx1.getID())
                                              .build(ctx.getNetworkID(), newAccountSigner);

        Transaction tx4 = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2, nestedTx3})
                                               .validity(timeProvider.get(), 3600)
                                               .forFee(30)
                                               .build(ctx.getNetworkID(), newAccountSigner);

        ctx.transactionBotService.putTransaction(tx4);

        // try to add another complex transaction with a common nested transaction

        Transaction nestedTx4 = PaymentBuilder.createNew(99L, newAccountID)
                                              .validity(timeProvider.get() - 1, 3600)
                                              .forFee(0L)
                                              .build(ctx.getNetworkID(), ctx.getSigner());
        Transaction tx5 = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx4, nestedTx2})
                                               .validity(timeProvider.get(), 3600)
                                               .forFee(30)
                                               .build(ctx.getNetworkID(), newAccountSigner);
        try {
            ctx.transactionBotService.putTransaction(tx5);
        } catch (Exception ignore) {

        }
        Assert.assertNull(ctx.backlogExplorerService.get(tx5.getID()));

        // apply complex payment

        Mockito.when(timeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
        ctx.generateBlockForNow();

        // check state

        accountBalance = ctx.accountBotService.getBalance(accountID.toString());
        Assert.assertEquals(accountBalance.amount, 999998941L);
        Assert.assertEquals(accountBalance.coloredCoins.get((new ColoredCoinID(newAccountID)).toString()), (Long) 10L);
        newAccountBalance = ctx.accountBotService.getBalance(newAccountID.toString());
        Assert.assertEquals(newAccountBalance.amount, 1059L);
        Assert.assertEquals(newAccountBalance.coloredCoins.get((new ColoredCoinID(newAccountID)).toString()),
                            (Long) 990L);

        // try to add transaction with a nested transaction which in blockchain

        try {
            ctx.transactionBotService.putTransaction(tx5);
        } catch (Exception ignore) {

        }
        Assert.assertNull(ctx.backlogExplorerService.get(tx5.getID()));
    }
}