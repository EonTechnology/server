package org.eontechnology.and.eon.app.IT;

import org.eontechnology.and.TestSigner;
import org.eontechnology.and.eon.app.api.bot.AccountBotService;
import org.eontechnology.and.eon.app.api.bot.ColoredCoinBotService;
import org.eontechnology.and.peer.core.common.TimeProvider;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechnology.and.peer.eon.midleware.parsers.ColoredCoinPaymentParser;
import org.eontechnology.and.peer.eon.midleware.parsers.ColoredCoinRegistrationParserV1;
import org.eontechnology.and.peer.eon.midleware.parsers.ColoredCoinRegistrationParserV2;
import org.eontechnology.and.peer.eon.midleware.parsers.ColoredCoinRemoveParser;
import org.eontechnology.and.peer.eon.midleware.parsers.ColoredCoinSupplyParserV1;
import org.eontechnology.and.peer.eon.midleware.parsers.ColoredCoinSupplyParserV2;
import org.eontechnology.and.peer.eon.midleware.parsers.PaymentParser;
import org.eontechnology.and.peer.eon.midleware.parsers.RegistrationParser;
import org.eontechnology.and.peer.tx.ColoredCoinID;
import org.eontechnology.and.peer.tx.TransactionType;
import org.eontechnology.and.peer.tx.midleware.builders.ColoredCoinRegistrationBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.ColoredCoinRemoveBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.ColoredCoinSupplyBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.ColoredPaymentBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.PaymentBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.RegistrationBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@RunWith(Enclosed.class)
public class ColoredCoinsTestIT {

  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  public static class V1 {

    private static final String GENERATOR =
        "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static final String COLORED_COIN =
        "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    private TimeProvider mockTimeProvider;

    private PeerContext ctx;

    private ISigner coloredCoinSigner;

    @Before
    public void setUp() throws Exception {
      mockTimeProvider = Mockito.mock(TimeProvider.class);

      ctx =
          new PeerContext(
              PeerStarterFactory.create()
                  .route(TransactionType.Payment, new PaymentParser())
                  .route(TransactionType.Registration, new RegistrationParser())
                  .route(
                      TransactionType.ColoredCoinRegistration,
                      new ColoredCoinRegistrationParserV1())
                  .route(TransactionType.ColoredCoinPayment, new ColoredCoinPaymentParser())
                  .route(TransactionType.ColoredCoinSupply, new ColoredCoinSupplyParserV1())
                  .seed(GENERATOR)
                  .build(mockTimeProvider));

      coloredCoinSigner = new TestSigner(COLORED_COIN);

      create_colored_coin();
    }

    private void create_colored_coin() throws Exception {

      AccountID accountID = new AccountID(coloredCoinSigner.getPublicKey());
      Block lastBlock = ctx.blockExplorerService.getLastBlock();

      // registration
      int timestamp = lastBlock.getTimestamp();
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

      Transaction tx1 =
          RegistrationBuilder.createNew(coloredCoinSigner.getPublicKey())
              .validity(timestamp, 3600)
              .build(ctx.getNetworkID(), ctx.getSigner());

      ctx.transactionBotService.putTransaction(tx1);

      ctx.generateBlockForNow();

      // payment
      timestamp = mockTimeProvider.get();
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

      Transaction tx2 =
          PaymentBuilder.createNew(1000000L, accountID)
              .validity(timestamp, 3600)
              .build(ctx.getNetworkID(), ctx.getSigner());
      ctx.transactionBotService.putTransaction(tx2);

      ctx.generateBlockForNow();

      // colored account
      timestamp = mockTimeProvider.get();
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

      Transaction tx3 =
          ColoredCoinRegistrationBuilder.createNew(10000L, 1)
              .validity(timestamp, 3600)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx3);

      ctx.generateBlockForNow();
    }

    @Test
    public void step_1_new_colored_coin() throws Exception {

      AccountID accountID = new AccountID(coloredCoinSigner.getPublicKey());

      ColoredCoinBotService.Info info =
          ctx.coloredCoinService.getInfo(new ColoredCoinID(accountID).toString());
      Assert.assertTrue(10000L == info.supply);

      AccountBotService.EONBalance balance = ctx.accountBotService.getBalance(accountID.toString());
      Assert.assertEquals(
          balance.coloredCoins.get(new ColoredCoinID(accountID).toString()), Long.valueOf(10000L));
    }

    @Test
    public void step_2_colored_payment() throws Exception {

      AccountID senderID = new AccountID(coloredCoinSigner.getPublicKey());
      ColoredCoinID coloredCoinID = new ColoredCoinID(senderID);
      AccountID recipientID = new AccountID(ctx.getSigner().getPublicKey());

      AccountBotService.EONBalance senderBalance =
          ctx.accountBotService.getBalance(senderID.toString());
      AccountBotService.EONBalance recepientBalance =
          ctx.accountBotService.getBalance(recipientID.toString());

      Assert.assertEquals(
          senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
      Assert.assertNull(recepientBalance.coloredCoins);

      Block lastBlock = ctx.blockExplorerService.getLastBlock();
      Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);

      int timestamp = mockTimeProvider.get();

      Transaction tx =
          ColoredPaymentBuilder.createNew(4000L, coloredCoinID, recipientID)
              .validity(timestamp, 60 * 60)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx);

      Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
      ctx.generateBlockForNow();

      senderBalance = ctx.accountBotService.getBalance(senderID.toString());
      recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());

      Assert.assertEquals(
          senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(6000L));
      Assert.assertEquals(
          recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(4000L));
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
      Transaction tx =
          ColoredPaymentBuilder.createNew(10000L, coloredCoinID, recipientID)
              .validity(timestamp, 60 * 60)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx);

      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
      ctx.generateBlockForNow();

      AccountBotService.EONBalance senderBalance =
          ctx.accountBotService.getBalance(senderID.toString());
      AccountBotService.EONBalance recepientBalance =
          ctx.accountBotService.getBalance(recipientID.toString());
      ColoredCoinBotService.Info coloredCoinInfo =
          ctx.coloredCoinService.getInfo(coloredCoinID.toString());
      Assert.assertNull(senderBalance.coloredCoins);
      Assert.assertEquals(
          recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
      Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(10000L));

      // increase money supply

      timestamp = mockTimeProvider.get();
      Transaction tx1 =
          ColoredCoinSupplyBuilder.createNew(11000L)
              .validity(timestamp, 60 * 60)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx1);
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

      ctx.generateBlockForNow();

      senderBalance = ctx.accountBotService.getBalance(senderID.toString());
      recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());
      coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID.toString());
      Assert.assertEquals(
          senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(1000L));
      Assert.assertEquals(
          recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
      Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(11000L));

      // try to decrease money supply

      timestamp = mockTimeProvider.get();
      Transaction tx2 =
          ColoredCoinSupplyBuilder.createNew(3000L)
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
      Assert.assertEquals(
          senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(1000L));
      Assert.assertEquals(
          recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
      Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(11000L));

      // payment

      timestamp = mockTimeProvider.get();
      Transaction tx3 =
          ColoredPaymentBuilder.createNew(7000L, coloredCoinID, senderID)
              .validity(timestamp, 60 * 60)
              .build(ctx.getNetworkID(), ctx.getSigner());
      ctx.transactionBotService.putTransaction(tx3);

      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
      ctx.generateBlockForNow();

      senderBalance = ctx.accountBotService.getBalance(senderID.toString());
      recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());
      coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID.toString());
      Assert.assertEquals(
          senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(8000L));
      Assert.assertEquals(
          recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(3000L));
      Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(11000L));

      // decrease money supply

      timestamp = mockTimeProvider.get();
      Transaction tx4 =
          ColoredCoinSupplyBuilder.createNew(4000L)
              .validity(timestamp, 60 * 60)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx4);
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
      ctx.generateBlockForNow();

      senderBalance = ctx.accountBotService.getBalance(senderID.toString());
      recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());
      coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID.toString());
      Assert.assertEquals(
          senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(1000L));
      Assert.assertEquals(
          recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(3000L));
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
      Transaction tx =
          ColoredPaymentBuilder.createNew(10000L, coloredCoinID, recipientID)
              .validity(timestamp, 60 * 60)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx);

      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
      ctx.generateBlockForNow();

      AccountBotService.EONBalance senderBalance =
          ctx.accountBotService.getBalance(senderID.toString());
      AccountBotService.EONBalance recepientBalance =
          ctx.accountBotService.getBalance(recipientID.toString());
      ColoredCoinBotService.Info coloredCoinInfo =
          ctx.coloredCoinService.getInfo(coloredCoinID.toString());
      Assert.assertNull(senderBalance.coloredCoins);
      Assert.assertEquals(
          recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
      Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(10000L));

      // try to delete colored coin

      timestamp = mockTimeProvider.get();
      Transaction tx1 =
          ColoredCoinSupplyBuilder.createNew()
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
      Assert.assertEquals(
          recepientBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
      Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(10000L));

      // payment

      timestamp = mockTimeProvider.get();
      Transaction tx3 =
          ColoredPaymentBuilder.createNew(10000L, coloredCoinID, senderID)
              .validity(timestamp, 60 * 60)
              .build(ctx.getNetworkID(), ctx.getSigner());
      ctx.transactionBotService.putTransaction(tx3);

      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
      ctx.generateBlockForNow();

      senderBalance = ctx.accountBotService.getBalance(senderID.toString());
      recepientBalance = ctx.accountBotService.getBalance(recipientID.toString());
      coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID.toString());
      Assert.assertEquals(
          senderBalance.coloredCoins.get(coloredCoinID.toString()), Long.valueOf(10000L));
      Assert.assertNull(recepientBalance.coloredCoins);
      Assert.assertEquals(coloredCoinInfo.supply, Long.valueOf(10000L));

      // delete colored coin

      timestamp = mockTimeProvider.get();
      Transaction tx4 =
          ColoredCoinSupplyBuilder.createNew()
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

  public static class V2 {
    private static final String GENERATOR =
        "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static final String COLORED_COIN =
        "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    private TimeProvider mockTimeProvider;

    private PeerContext ctx;

    private ISigner coloredCoinSigner;

    @Before
    public void setUp() throws Exception {
      mockTimeProvider = Mockito.mock(TimeProvider.class);

      ctx =
          new PeerContext(
              PeerStarterFactory.create()
                  .route(TransactionType.Payment, new PaymentParser())
                  .route(TransactionType.Registration, new RegistrationParser())
                  .route(
                      TransactionType.ColoredCoinRegistration,
                      new ColoredCoinRegistrationParserV2())
                  .route(TransactionType.ColoredCoinPayment, new ColoredCoinPaymentParser())
                  .route(TransactionType.ColoredCoinSupply, new ColoredCoinSupplyParserV2())
                  .route(TransactionType.ColoredCoinRemove, new ColoredCoinRemoveParser())
                  .seed(GENERATOR)
                  .build(mockTimeProvider));
      coloredCoinSigner = new TestSigner(COLORED_COIN);
      create_colored_coin_account();
    }

    private void create_colored_coin_account() throws Exception {
      AccountID accountID = new AccountID(coloredCoinSigner.getPublicKey());
      Block lastBlock = ctx.blockExplorerService.getLastBlock();

      // registration
      int timestamp = lastBlock.getTimestamp();
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

      Transaction tx1 =
          RegistrationBuilder.createNew(coloredCoinSigner.getPublicKey())
              .validity(timestamp, 3600)
              .build(ctx.getNetworkID(), ctx.getSigner());

      ctx.transactionBotService.putTransaction(tx1);

      ctx.generateBlockForNow();

      // payment
      timestamp = mockTimeProvider.get();
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

      Transaction tx2 =
          PaymentBuilder.createNew(1000000L, accountID)
              .validity(timestamp, 3600)
              .build(ctx.getNetworkID(), ctx.getSigner());
      ctx.transactionBotService.putTransaction(tx2);

      ctx.generateBlockForNow();
    }

    @Test
    public void create_colored_coin() throws Exception {

      // create colored coin

      int timestamp = mockTimeProvider.get();
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

      Transaction tx1 =
          ColoredCoinRegistrationBuilder.createNew(10000L, 1)
              .validity(timestamp, 3600)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx1);
      ctx.generateBlockForNow();

      AccountID accountID = new AccountID(coloredCoinSigner.getPublicKey());
      ColoredCoinBotService.Info info =
          ctx.coloredCoinService.getInfo(new ColoredCoinID(accountID).toString());
      Assert.assertFalse(info.auto);
      Assert.assertTrue(10000L == info.supply);

      AccountBotService.EONBalance balance = ctx.accountBotService.getBalance(accountID.toString());
      Assert.assertEquals(
          balance.coloredCoins.get(new ColoredCoinID(accountID).toString()), Long.valueOf(10000L));

      // colored payment

      AccountID senderID = new AccountID(coloredCoinSigner.getPublicKey());
      AccountID recipientID = new AccountID(ctx.getSigner().getPublicKey());
      Transaction tx2 =
          ColoredPaymentBuilder.createNew(4000L, new ColoredCoinID(senderID), recipientID)
              .validity(timestamp, 60 * 60)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx2);

      timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      ColoredCoinID id = new ColoredCoinID(senderID);
      AccountBotService.EONBalance senderBalance =
          ctx.accountBotService.getBalance(senderID.toString());
      AccountBotService.EONBalance recipientBalance =
          ctx.accountBotService.getBalance(recipientID.toString());

      Assert.assertEquals(senderBalance.coloredCoins.get(id.toString()), Long.valueOf(6000L));
      Assert.assertEquals(recipientBalance.coloredCoins.get(id.toString()), Long.valueOf(4000L));

      info = ctx.coloredCoinService.getInfo(id.toString());
      Assert.assertFalse(info.auto);
      Assert.assertTrue(10000L == info.supply);
    }

    @Test
    public void create_colored_coin_auto_emission_mode() throws Exception {

      // create colored coin
      int timestamp = mockTimeProvider.get();
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

      Transaction tx1 =
          ColoredCoinRegistrationBuilder.createNew(1)
              .validity(timestamp, 3600)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx1);
      ctx.generateBlockForNow();

      AccountID accountID = new AccountID(coloredCoinSigner.getPublicKey());
      ColoredCoinBotService.Info info =
          ctx.coloredCoinService.getInfo(new ColoredCoinID(accountID).toString());
      Assert.assertTrue(info.auto);
      Assert.assertTrue(0 == info.supply);

      AccountBotService.EONBalance balance = ctx.accountBotService.getBalance(accountID.toString());
      Assert.assertNull(balance.coloredCoins);

      // colored payment

      AccountID senderID = new AccountID(coloredCoinSigner.getPublicKey());
      AccountID recipientID = new AccountID(ctx.getSigner().getPublicKey());
      Transaction tx2 =
          ColoredPaymentBuilder.createNew(4000L, new ColoredCoinID(senderID), recipientID)
              .validity(timestamp, 60 * 60)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx2);

      timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      ColoredCoinID id = new ColoredCoinID(senderID);
      AccountBotService.EONBalance senderBalance =
          ctx.accountBotService.getBalance(senderID.toString());
      AccountBotService.EONBalance recipientBalance =
          ctx.accountBotService.getBalance(recipientID.toString());

      Assert.assertNull(senderBalance.coloredCoins);
      Assert.assertEquals(recipientBalance.coloredCoins.get(id.toString()), Long.valueOf(4000L));

      info = ctx.coloredCoinService.getInfo(id.toString());
      Assert.assertTrue(info.auto);
      Assert.assertTrue(4000L == info.supply);
    }

    @Test
    public void delete_colored_coin_preset_mode() throws Exception {

      // create colored coin
      Transaction tx1 =
          ColoredCoinRegistrationBuilder.createNew(10000L, 1)
              .validity(mockTimeProvider.get(), 3600)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx1);

      int timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      ColoredCoinID id = new ColoredCoinID(new AccountID(coloredCoinSigner.getPublicKey()));
      ColoredCoinBotService.Info info = ctx.coloredCoinService.getInfo(id.toString());
      Assert.assertFalse(info.auto);
      Assert.assertTrue(10000L == info.supply);

      // remove colored coin

      Transaction tx2 =
          ColoredCoinRemoveBuilder.createNew()
              .validity(mockTimeProvider.get(), 3600)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx2);

      timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      info = ctx.coloredCoinService.getInfo(id.toString());
      Assert.assertEquals(info.state, ColoredCoinBotService.State.Unauthorized);
    }

    @Test
    public void delete_colored_coin_auto_emission_mode() throws Exception {

      // create colored coin
      Transaction tx1 =
          ColoredCoinRegistrationBuilder.createNew(1)
              .validity(mockTimeProvider.get(), 3600)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx1);

      int timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      AccountID accountID = new AccountID(coloredCoinSigner.getPublicKey());
      ColoredCoinBotService.Info info =
          ctx.coloredCoinService.getInfo(new ColoredCoinID(accountID).toString());
      Assert.assertTrue(info.auto);
      Assert.assertTrue(0 == info.supply);

      AccountBotService.EONBalance balance = ctx.accountBotService.getBalance(accountID.toString());
      Assert.assertNull(balance.coloredCoins);

      // remove colored coin

      Transaction tx2 =
          ColoredCoinRemoveBuilder.createNew()
              .validity(mockTimeProvider.get(), 3600)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx2);

      timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      info = ctx.coloredCoinService.getInfo(new ColoredCoinID(accountID).toString());
      Assert.assertEquals(info.state, ColoredCoinBotService.State.Unauthorized);
    }

    @Test
    public void toggling_to_auto_emission_mode() throws Exception {

      // create colored coin
      Transaction tx1 =
          ColoredCoinRegistrationBuilder.createNew(10000L, 1)
              .validity(mockTimeProvider.get(), 3600)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx1);

      int timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      AccountID senderID = new AccountID(coloredCoinSigner.getPublicKey());
      ColoredCoinID id = new ColoredCoinID(senderID);
      ColoredCoinBotService.Info info = ctx.coloredCoinService.getInfo(id.toString());
      Assert.assertFalse(info.auto);
      Assert.assertTrue(10000L == info.supply);
      AccountBotService.EONBalance senderBalance =
          ctx.accountBotService.getBalance(senderID.toString());
      Assert.assertTrue(senderBalance.coloredCoins.get(id.toString()) == 10000L);

      // payment

      AccountID recipientID = new AccountID(ctx.getSigner().getPublicKey());
      Transaction tx2 =
          ColoredPaymentBuilder.createNew(4000L, new ColoredCoinID(senderID), recipientID)
              .validity(mockTimeProvider.get(), 60 * 60)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx2);

      timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      // toggling

      Transaction tx3 =
          ColoredCoinSupplyBuilder.createNew(ColoredCoinEmitMode.AUTO.toString())
              .validity(mockTimeProvider.get(), 60 * 60)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx3);
      timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      info = ctx.coloredCoinService.getInfo(id.toString());
      Assert.assertTrue(info.auto);
      Assert.assertTrue(4000L == info.supply);
      senderBalance = ctx.accountBotService.getBalance(senderID.toString());
      Assert.assertNull(senderBalance.coloredCoins);
    }

    @Test
    public void toggling_to_preset_mode() throws Exception {

      // create colored coin

      Transaction tx1 =
          ColoredCoinRegistrationBuilder.createNew(1)
              .validity(mockTimeProvider.get(), 3600)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx1);

      int timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      AccountID senderID = new AccountID(coloredCoinSigner.getPublicKey());
      ColoredCoinID id = new ColoredCoinID(senderID);
      ColoredCoinBotService.Info info = ctx.coloredCoinService.getInfo(id.toString());
      Assert.assertTrue(info.auto);
      Assert.assertTrue(0 == info.supply);
      AccountBotService.EONBalance senderBalance =
          ctx.accountBotService.getBalance(senderID.toString());
      Assert.assertNull(senderBalance.coloredCoins);

      // payment

      AccountID recipientID = new AccountID(ctx.getSigner().getPublicKey());
      Transaction tx2 =
          ColoredPaymentBuilder.createNew(4000L, new ColoredCoinID(senderID), recipientID)
              .validity(mockTimeProvider.get(), 60 * 60)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx2);

      timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      // toggling

      Transaction tx3 =
          ColoredCoinSupplyBuilder.createNew(10000)
              .validity(mockTimeProvider.get(), 60 * 60)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx3);
      timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      info = ctx.coloredCoinService.getInfo(id.toString());
      Assert.assertFalse(info.auto);
      Assert.assertTrue(10000L == info.supply);
      senderBalance = ctx.accountBotService.getBalance(senderID.toString());
      Assert.assertTrue(senderBalance.coloredCoins.get(id.toString()) == 6000L);
    }

    @Test
    public void change_money_supply() throws Exception {

      // create colored coin

      Transaction tx1 =
          ColoredCoinRegistrationBuilder.createNew(10000L, 1)
              .validity(mockTimeProvider.get(), 3600)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx1);

      int timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      AccountID senderID = new AccountID(coloredCoinSigner.getPublicKey());
      ColoredCoinID id = new ColoredCoinID(senderID);
      ColoredCoinBotService.Info info = ctx.coloredCoinService.getInfo(id.toString());
      Assert.assertFalse(info.auto);
      Assert.assertTrue(10000L == info.supply);
      AccountBotService.EONBalance senderBalance =
          ctx.accountBotService.getBalance(senderID.toString());
      Assert.assertTrue(senderBalance.coloredCoins.get(id.toString()) == 10000L);

      // change money supply

      Transaction tx2 =
          ColoredCoinSupplyBuilder.createNew(6000)
              .validity(mockTimeProvider.get(), 60 * 60)
              .build(ctx.getNetworkID(), coloredCoinSigner);
      ctx.transactionBotService.putTransaction(tx2);
      timestamp = ctx.blockExplorerService.getLastBlock().getTimestamp() + 180;
      Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 1);
      ctx.generateBlockForNow();

      info = ctx.coloredCoinService.getInfo(id.toString());
      Assert.assertFalse(info.auto);
      Assert.assertTrue(6000L == info.supply);
      senderBalance = ctx.accountBotService.getBalance(senderID.toString());
      Assert.assertTrue(senderBalance.coloredCoins.get(id.toString()) == 6000L);
    }
  }
}
