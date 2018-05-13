package com.exscudo.eon.app.IT;

import com.exscudo.TestSigner;
import com.exscudo.eon.app.api.bot.AccountBotService;
import com.exscudo.eon.app.api.bot.ColoredCoinBotService;
import com.exscudo.eon.app.cfg.PeerStarter;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.tx.ColoredCoinID;
import com.exscudo.peer.tx.midleware.builders.ColoredCoinRegistrationBuilder;
import com.exscudo.peer.tx.midleware.builders.ColoredCoinSupplyBuilder;
import com.exscudo.peer.tx.midleware.builders.ColoredPaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
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
public class ColoredCoinsTestIT {
    private static final String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static final String COLORED_COIN = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    private TimeProvider mockTimeProvider;

    private PeerContext ctx;

    private ISigner coloredCoinSigner;

    @Before
    public void setUp() throws Exception {
        mockTimeProvider = Mockito.mock(TimeProvider.class);

        PeerStarter peerStarter = PeerStarterFactory.create(GENERATOR, mockTimeProvider);
        peerStarter.setFork(Utils.createFork(peerStarter.getStorage()));

        ctx = new PeerContext(peerStarter);

        coloredCoinSigner = new TestSigner(COLORED_COIN);

        create_colored_coin();
    }

    private void create_colored_coin() throws Exception {

        AccountID accountID = new AccountID(coloredCoinSigner.getPublicKey());
        Block lastBlock = ctx.blockExplorerService.getLastBlock();

        // registration
        int timestamp = lastBlock.getTimestamp();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

        Transaction tx1 = RegistrationBuilder.createNew(coloredCoinSigner.getPublicKey())
                                             .validity(timestamp, 3600)
                                             .build(ctx.getNetworkID(), ctx.getSigner());

        ctx.transactionBotService.putTransaction(tx1);

        ctx.generateBlockForNow();

        // payment
        timestamp = mockTimeProvider.get();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

        Transaction tx2 = PaymentBuilder.createNew(1000000L, accountID)
                                        .validity(timestamp, 3600)
                                        .build(ctx.getNetworkID(), ctx.getSigner());
        ctx.transactionBotService.putTransaction(tx2);

        ctx.generateBlockForNow();

        // colored account
        timestamp = mockTimeProvider.get();
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

        Transaction tx3 = ColoredCoinRegistrationBuilder.createNew(10000L, 1)
                                                        .validity(timestamp, 3600)
                                                        .build(ctx.getNetworkID(), coloredCoinSigner);
        ctx.transactionBotService.putTransaction(tx3);

        ctx.generateBlockForNow();
    }

    @Test
    public void step_1_new_colored_coin() throws Exception {

        AccountID accountID = new AccountID(coloredCoinSigner.getPublicKey());

        ColoredCoinBotService.Info info = ctx.coloredCoinService.getInfo(new ColoredCoinID(accountID).toString());
        Assert.assertTrue(10000L == info.supply);

        AccountBotService.EONBalance balance = ctx.accountBotService.getBalance(accountID.toString());
        Assert.assertEquals(balance.coloredCoins.get(new ColoredCoinID(accountID).toString()), Long.valueOf(10000L));
    }

    @Test
    public void step_2_colored_payment() throws Exception {

        AccountID senderID = new AccountID(coloredCoinSigner.getPublicKey());
        ColoredCoinID coloredCoinID = new ColoredCoinID(senderID);
        AccountID recipientID = new AccountID(ctx.getSigner().getPublicKey());

        AccountBotService.EONBalance senderBalance = ctx.accountBotService.getBalance(senderID.toString());
        AccountBotService.EONBalance recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());

        Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
        Assert.assertNull(recepientBalance.coloredCoins);

        Block lastBlock = ctx.blockExplorerService.getLastBlock();
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);

        int timestamp = mockTimeProvider.get();

        Transaction tx = ColoredPaymentBuilder.createNew(4000L, coloredCoinID, recipientID)
                                              .validity(timestamp, 60 * 60)
                                              .build(ctx.getNetworkID(), coloredCoinSigner);
        ctx.transactionBotService.putTransaction(tx);

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
        ctx.generateBlockForNow();

        senderBalance = ctx.accountBotService.getBalance(senderID.toString());
        recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());

        Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(6000L));
        Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(4000L));
    }

    @Test
    public void step_3_change_money_supply() throws Exception {

        AccountID senderID = new AccountID(coloredCoinSigner.getPublicKey());
        AccountID recipientID = new AccountID(ctx.getSigner().getPublicKey());
        ColoredCoinID coloredCoinID = new ColoredCoinID(senderID);

        Block lastBlock = ctx.blockExplorerService.getLastBlock();
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);

        // payment

        int timestamp = mockTimeProvider.get();
        Transaction tx = ColoredPaymentBuilder.createNew(10000L, coloredCoinID, recipientID)
                                              .validity(timestamp, 60 * 60)
                                              .build(ctx.getNetworkID(), coloredCoinSigner);
        ctx.transactionBotService.putTransaction(tx);

        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
        ctx.generateBlockForNow();

        AccountBotService.EONBalance senderBalance = ctx.accountBotService.getBalance(senderID.toString());
        AccountBotService.EONBalance recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());
        ColoredCoinBotService.Info coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID.toString());
        Assert.assertNull(senderBalance.coloredCoins);
        Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
        Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(10000L));

        // increase money supply

        timestamp = mockTimeProvider.get();
        Transaction tx1 = ColoredCoinSupplyBuilder.createNew(11000L)
                                                  .validity(timestamp, 60 * 60)
                                                  .build(ctx.getNetworkID(), coloredCoinSigner);
        ctx.transactionBotService.putTransaction(tx1);
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

        ctx.generateBlockForNow();

        senderBalance = ctx.accountBotService.getBalance(senderID.toString());
        recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());
        coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID.toString());
        Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(1000L));
        Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
        Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(11000L));

        // try to decrease money supply

        timestamp = mockTimeProvider.get();
        Transaction tx2 = ColoredCoinSupplyBuilder.createNew(3000L)
                                                  .validity(timestamp, 60 * 60)
                                                  .build(ctx.getNetworkID(), coloredCoinSigner);
        try {
            ctx.transactionBotService.putTransaction(tx2);
        } catch (Exception ignore) {
        }

        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

        ctx.generateBlockForNow();

        senderBalance = ctx.accountBotService.getBalance(senderID.toString());
        recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());
        coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID.toString());
        Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(1000L));
        Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
        Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(11000L));

        // payment

        timestamp = mockTimeProvider.get();
        Transaction tx3 = ColoredPaymentBuilder.createNew(7000L, coloredCoinID, senderID)
                                               .validity(timestamp, 60 * 60)
                                               .build(ctx.getNetworkID(), ctx.getSigner());
        ctx.transactionBotService.putTransaction(tx3);

        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
        ctx.generateBlockForNow();

        senderBalance = ctx.accountBotService.getBalance(senderID.toString());
        recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());
        coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID.toString());
        Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(8000L));
        Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(3000L));
        Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(11000L));

        // decrease money supply

        timestamp = mockTimeProvider.get();
        Transaction tx4 = ColoredCoinSupplyBuilder.createNew(4000L)
                                                  .validity(timestamp, 60 * 60)
                                                  .build(ctx.getNetworkID(), coloredCoinSigner);
        ctx.transactionBotService.putTransaction(tx4);
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
        ctx.generateBlockForNow();

        senderBalance = ctx.accountBotService.getBalance(senderID.toString());
        recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());
        coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID.toString());
        Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(1000L));
        Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(3000L));
        Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(4000L));
    }

    @Test
    public void step_4_delete_colored_coin() throws Exception {

        AccountID senderID = new AccountID(coloredCoinSigner.getPublicKey());
        AccountID recipientID = new AccountID(ctx.getSigner().getPublicKey());
        ColoredCoinID coloredCoinID = new ColoredCoinID(senderID);

        Block lastBlock = ctx.blockExplorerService.getLastBlock();
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);

        // payment

        int timestamp = mockTimeProvider.get();
        Transaction tx = ColoredPaymentBuilder.createNew(10000L, coloredCoinID, recipientID)
                                              .validity(timestamp, 60 * 60)
                                              .build(ctx.getNetworkID(), coloredCoinSigner);
        ctx.transactionBotService.putTransaction(tx);

        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
        ctx.generateBlockForNow();

        AccountBotService.EONBalance senderBalance = ctx.accountBotService.getBalance(senderID.toString());
        AccountBotService.EONBalance recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());
        ColoredCoinBotService.Info coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID.toString());
        Assert.assertNull(senderBalance.coloredCoins);
        Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
        Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(10000L));

        // try to delete colored coin

        timestamp = mockTimeProvider.get();
        Transaction tx1 = ColoredCoinSupplyBuilder.createNew()
                                                  .validity(timestamp, 60 * 60)
                                                  .build(ctx.getNetworkID(), coloredCoinSigner);
        try {
            ctx.transactionBotService.putTransaction(tx1);
        } catch (Exception ignored) {
        }
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
        ctx.generateBlockForNow();

        senderBalance = ctx.accountBotService.getBalance(senderID.toString());
        recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());
        coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID.toString());
        Assert.assertNull(senderBalance.coloredCoins);
        Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
        Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(10000L));

        // payment

        timestamp = mockTimeProvider.get();
        Transaction tx3 = ColoredPaymentBuilder.createNew(10000L, coloredCoinID, senderID)
                                               .validity(timestamp, 60 * 60)
                                               .build(ctx.getNetworkID(), ctx.getSigner());
        ctx.transactionBotService.putTransaction(tx3);

        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
        ctx.generateBlockForNow();

        senderBalance = ctx.accountBotService.getBalance(senderID.toString());
        recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());
        coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID.toString());
        Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
        Assert.assertNull(recepientBalance.coloredCoins);
        Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(10000L));

        // delete colored coin

        timestamp = mockTimeProvider.get();
        Transaction tx4 = ColoredCoinSupplyBuilder.createNew()
                                                  .validity(timestamp, 60 * 60)
                                                  .build(ctx.getNetworkID(), coloredCoinSigner);
        ctx.transactionBotService.putTransaction(tx4);
        Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
        ctx.generateBlockForNow();

        Assert.assertNull(ctx.accountBotService.getBalance(senderID.toString()).coloredCoins);
        Assert.assertNull(ctx.accountBotService.getBalance(recipientID.toString()).coloredCoins);
        Assert.assertNull(ctx.accountBotService.getInformation(senderID.toString()).coloredCoin);
    }
}
