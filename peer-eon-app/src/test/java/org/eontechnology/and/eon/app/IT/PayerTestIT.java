package org.eontechnology.and.eon.app.IT;

import static org.junit.Assert.assertEquals;

import org.eontechnology.and.TestSigner;
import org.eontechnology.and.eon.app.api.bot.AccountBotService;
import org.eontechnology.and.peer.core.common.TimeProvider;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.eon.midleware.parsers.PaymentParser;
import org.eontechnology.and.peer.eon.midleware.parsers.RegistrationParser;
import org.eontechnology.and.peer.tx.TransactionType;
import org.eontechnology.and.peer.tx.midleware.builders.PaymentBuilder;
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
public class PayerTestIT {
  private static final String GENERATOR =
      "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
  private static final String SENDER =
      "9e641020d3803008bf4e8a15ad05f84fb8eb3220037322ebc5fa58b70c3f1bd1";
  private static final String DELEGATE_1 =
      "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
  private static final String DELEGATE_2 =
      "112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00";

  private TimeProvider mockTimeProvider;
  private PeerContext ctx;

  @Before
  public void setUp() throws Exception {
    mockTimeProvider = Mockito.mock(TimeProvider.class);

    ctx =
        new PeerContext(
            PeerStarterFactory.create()
                .route(TransactionType.Registration, new RegistrationParser())
                .route(TransactionType.Payment, new PaymentParser())
                .seed(GENERATOR)
                .build(mockTimeProvider));
  }

  @Test
  public void step_1() throws Exception {

    ISigner sender = new TestSigner(SENDER);
    ISigner delegate_1 = new TestSigner(DELEGATE_1);
    ISigner delegate_2 = new TestSigner(DELEGATE_2);

    Block lastBlock = ctx.blockExplorerService.getLastBlock();
    BlockID lastBlockID = lastBlock.getID();
    int timestamp = lastBlock.getTimestamp();

    // registration

    Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

    Transaction tx1 =
        RegistrationBuilder.createNew(delegate_1.getPublicKey())
            .validity(timestamp + 1, 3600)
            .build(ctx.getNetworkID(), ctx.getSigner());
    ctx.transactionBotService.putTransaction(tx1);
    Transaction tx2 =
        RegistrationBuilder.createNew(delegate_2.getPublicKey())
            .validity(timestamp + 1, 3600)
            .build(ctx.getNetworkID(), ctx.getSigner());
    ctx.transactionBotService.putTransaction(tx2);

    ctx.generateBlockForNow();

    Assert.assertNotEquals(
        "Block generated", lastBlockID, ctx.blockExplorerService.getLastBlock().getID());
    lastBlockID = ctx.blockExplorerService.getLastBlock().getID();

    Assert.assertEquals(
        "Registration in block",
        2,
        ctx.blockExplorerService.getLastBlock().getTransactions().size());

    // payments
    long amount = 1000L;
    long fee = 1000L;

    Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 2 * 180 + 1);

    AccountID delegate_1_id = new AccountID(delegate_1.getPublicKey());
    Transaction tx3 =
        PaymentBuilder.createNew(amount, delegate_1_id)
            .forFee(fee)
            .validity(timestamp + 180 + 1, 3600)
            .build(ctx.getNetworkID(), sender);
    ctx.transactionBotService.putTransaction(tx3);

    ctx.generateBlockForNow();

    Assert.assertNotEquals(
        "Block generated", lastBlockID, ctx.blockExplorerService.getLastBlock().getID());
    lastBlockID = ctx.blockExplorerService.getLastBlock().getID();

    Assert.assertEquals(
        "Payment in block", 1, ctx.blockExplorerService.getLastBlock().getTransactions().size());

    // use payer field
    Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 3 * 180 + 1);

    AccountID delegate_2_id = new AccountID(delegate_2.getPublicKey());
    Transaction tx4 =
        PaymentBuilder.createNew(amount, delegate_2_id)
            .payedBy(delegate_1_id)
            .forFee(fee)
            .validity(timestamp + 180 + 1, 3600)
            .build(ctx.getNetworkID(), sender, new ISigner[] {delegate_1});
    ctx.transactionBotService.putTransaction(tx4);

    ctx.generateBlockForNow();

    Assert.assertNotEquals(
        "Block generated", lastBlockID, ctx.blockExplorerService.getLastBlock().getID());
    lastBlockID = ctx.blockExplorerService.getLastBlock().getID();

    Assert.assertEquals(
        "Payment in block", 1, ctx.blockExplorerService.getLastBlock().getTransactions().size());

    // check balances
    String id1 = delegate_1_id.toString();
    AccountBotService.Info info1 = ctx.accountBotService.getInformation(id1);
    assertEquals(amount - fee, info1.amount);

    String id2 = delegate_2_id.toString();
    AccountBotService.Info info2 = ctx.accountBotService.getInformation(id2);
    assertEquals(amount, info2.amount);

    AccountID senderID = new AccountID(sender.getPublicKey());
    String id3 = senderID.toString();
    AccountBotService.Info info3 = ctx.accountBotService.getInformation(id3);
    assertEquals(1000000000L - (amount * 2 + fee), info3.amount);

    // sync new peer
    PeerContext ctx2 =
        new PeerContext(
            PeerStarterFactory.create()
                .route(TransactionType.Registration, new RegistrationParser())
                .route(TransactionType.Payment, new PaymentParser())
                .seed("")
                .build(mockTimeProvider));

    ctx2.setPeerToConnect(ctx);
    ctx2.fullBlockSync();

    Assert.assertEquals(
        "Blocks synchronized",
        ctx.blockchain.getLastBlock().getID(),
        ctx2.blockchain.getLastBlock().getID());

    // check tx history
    assertEquals(
        "Delegate1 tx count",
        3,
        ctx2.transactionExplorerService.getByAccountId(delegate_1_id, 0).size());

    assertEquals(
        "Delegate2 tx count",
        2,
        ctx2.transactionExplorerService.getByAccountId(delegate_2_id, 0).size());

    assertEquals(
        "Sender tx count", 2, ctx2.transactionExplorerService.getByAccountId(senderID, 0).size());
  }
}
