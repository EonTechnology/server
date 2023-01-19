package org.eontechnology.and.eon.app.IT;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eontechnology.and.TestSigner;
import org.eontechnology.and.eon.app.api.bot.AccountBotService;
import org.eontechnology.and.peer.core.common.TimeProvider;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.eon.midleware.parsers.DelegateParser;
import org.eontechnology.and.peer.eon.midleware.parsers.PaymentParser;
import org.eontechnology.and.peer.eon.midleware.parsers.QuorumParser;
import org.eontechnology.and.peer.eon.midleware.parsers.RegistrationParser;
import org.eontechnology.and.peer.eon.midleware.parsers.RejectionParser;
import org.eontechnology.and.peer.tx.TransactionType;
import org.eontechnology.and.peer.tx.midleware.builders.DelegateBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.PaymentBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.QuorumBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.RegistrationBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.RejectionBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MultiFactorAuthTestIT {
  private static final String GENERATOR =
      "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
  private static final String DELEGATE_1 =
      "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
  private static final String DELEGATE_2 =
      "112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00";
  private static final String DELEGATE_NEW =
      "2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0000";

  private TimeProvider mockTimeProvider;
  private PeerContext ctx;

  @Before
  public void setUp() throws Exception {
    mockTimeProvider = Mockito.mock(TimeProvider.class);

    ctx =
        new PeerContext(
            PeerStarterFactory.create()
                .route(TransactionType.Payment, new PaymentParser())
                .route(TransactionType.Registration, new RegistrationParser())
                .route(TransactionType.Delegate, new DelegateParser())
                .route(TransactionType.Quorum, new QuorumParser())
                .route(TransactionType.Rejection, new RejectionParser())
                .seed(GENERATOR)
                .build(mockTimeProvider));
  }

  @Test
  public void step_1_mfa() throws Exception {

    ISigner delegate_1 = new TestSigner(DELEGATE_1);
    ISigner delegate_2 = new TestSigner(DELEGATE_2);

    Block lastBlock = ctx.blockExplorerService.getLastBlock();
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
    Assert.assertEquals(
        "Registration in block",
        2,
        ctx.blockExplorerService.getLastBlock().getTransactions().size());

    // payments
    Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 2 * 180 + 1);

    AccountID delegate_1_id = new AccountID(delegate_1.getPublicKey());
    Transaction tx3 =
        PaymentBuilder.createNew(1000L, delegate_1_id)
            .validity(timestamp + 180 + 1, 3600)
            .build(ctx.getNetworkID(), ctx.getSigner());
    ctx.transactionBotService.putTransaction(tx3);
    AccountID delegate_2_id = new AccountID(delegate_2.getPublicKey());
    Transaction tx4 =
        PaymentBuilder.createNew(1000L, delegate_2_id)
            .validity(timestamp + 180 + 1, 3600)
            .build(ctx.getNetworkID(), ctx.getSigner());
    ctx.transactionBotService.putTransaction(tx4);

    ctx.generateBlockForNow();
    Assert.assertEquals(
        "Payments in block", 2, ctx.blockExplorerService.getLastBlock().getTransactions().size());

    // set quorum and delegates
    Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 3 * 180 + 1);

    Transaction tx6 =
        DelegateBuilder.createNew(delegate_1_id, 30)
            .validity(timestamp + 2 * 180 + 1, 3600)
            .build(ctx.getNetworkID(), ctx.getSigner());
    ctx.transactionBotService.putTransaction(tx6);
    Transaction tx7 =
        DelegateBuilder.createNew(delegate_2_id, 20)
            .validity(timestamp + 2 * 180 + 1, 3600)
            .build(ctx.getNetworkID(), ctx.getSigner());
    ctx.transactionBotService.putTransaction(tx7);
    Transaction tx8 =
        QuorumBuilder.createNew(50)
            .quorumForType(TransactionType.Payment, 85)
            .validity(timestamp + 2 * 180 + 1, 3600)
            .build(ctx.getNetworkID(), ctx.getSigner());
    ctx.transactionBotService.putTransaction(tx8);

    ctx.generateBlockForNow();
    Assert.assertEquals(
        "Transactions in block",
        3,
        ctx.blockExplorerService.getLastBlock().getTransactions().size());

    String id1 = delegate_1_id.toString();
    AccountBotService.Info info1 = ctx.accountBotService.getInformation(id1);
    AccountID signer_id = new AccountID(ctx.getSigner().getPublicKey());
    assertTrue(info1.voter.get(signer_id.toString()) == 30);

    String id2 = delegate_2_id.toString();
    AccountBotService.Info info2 = ctx.accountBotService.getInformation(id2);
    assertTrue(info2.voter.get(signer_id.toString()) == 20);

    // enable mfa
    Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 4 * 180 + 1);

    Transaction tx5 =
        DelegateBuilder.createNew(signer_id, 50)
            .validity(timestamp + 3 * 180 + 1, 3600)
            .build(ctx.getNetworkID(), ctx.getSigner());
    ctx.transactionBotService.putTransaction(tx5);

    ctx.generateBlockForNow();
    Assert.assertEquals(
        "Transactions in block",
        1,
        ctx.blockExplorerService.getLastBlock().getTransactions().size());

    // try put transaction
    ISigner delegate_new = new TestSigner(DELEGATE_NEW);
    Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * 5 + 1);

    Transaction tx9 =
        RegistrationBuilder.createNew(delegate_new.getPublicKey())
            .validity(timestamp + 4 * 180 + 1, 3600)
            .build(ctx.getNetworkID(), ctx.getSigner());
    ctx.transactionBotService.putTransaction(tx9);
    Transaction tx10 =
        PaymentBuilder.createNew(100L, delegate_1_id)
            .validity(timestamp + 4 * 180 + 1, 3600)
            .build(ctx.getNetworkID(), ctx.getSigner());
    try {
      ctx.transactionBotService.putTransaction(tx10);
    } catch (Exception ignore) {

    }

    ctx.generateBlockForNow();
    Assert.assertEquals(
        "Transactions in block",
        1,
        ctx.blockExplorerService.getLastBlock().getTransactions().size());

    // try put transaction
    Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * 6 + 1);

    Transaction tx11 =
        PaymentBuilder.createNew(100L, delegate_1_id)
            .validity(timestamp + 180 * 5 + 1, 3600)
            .build(ctx.getNetworkID(), ctx.getSigner(), new ISigner[] {delegate_1});
    try {
      ctx.transactionBotService.putTransaction(tx11);
    } catch (Exception ignore) {

    }

    ctx.generateBlockForNow();
    Assert.assertEquals(
        "Transactions in block",
        0,
        ctx.blockExplorerService.getLastBlock().getTransactions().size());

    // put transaction
    Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * 7 + 1);

    Transaction tx12 =
        PaymentBuilder.createNew(100L, delegate_1_id)
            .validity(timestamp + 180 * 6 + 1, 3600)
            .build(ctx.getNetworkID(), ctx.getSigner(), new ISigner[] {delegate_1, delegate_2});
    ctx.transactionBotService.putTransaction(tx12);

    ctx.generateBlockForNow();
    Assert.assertEquals(
        "Transactions in block",
        1,
        ctx.blockExplorerService.getLastBlock().getTransactions().size());

    // reject
    Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * 8 + 1);

    Transaction tx13 =
        RejectionBuilder.createNew(signer_id)
            .validity(timestamp + 180 * 7 + 1, 3600)
            .build(ctx.getNetworkID(), delegate_2);
    ctx.transactionBotService.putTransaction(tx13);

    ctx.generateBlockForNow();
    Assert.assertEquals(
        "Transactions in block",
        1,
        ctx.blockExplorerService.getLastBlock().getTransactions().size());
    info2 = ctx.accountBotService.getInformation(id2);
    assertNull(info2.voter);

    // put transaction
    Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * 9 + 1);
    ctx.transactionBotService.putTransaction(tx11);
    ctx.generateBlockForNow();
    Assert.assertEquals(
        "Transactions in block",
        1,
        ctx.blockExplorerService.getLastBlock().getTransactions().size());
  }
}
