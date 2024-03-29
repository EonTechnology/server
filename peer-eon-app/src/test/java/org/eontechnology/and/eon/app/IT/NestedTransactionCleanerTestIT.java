package org.eontechnology.and.eon.app.IT;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import org.eontechnology.and.TestSigner;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.blockchain.storage.DbNestedTransaction;
import org.eontechnology.and.peer.core.common.TimeProvider;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.eon.midleware.parsers.ComplexPaymentParserV1;
import org.eontechnology.and.peer.eon.midleware.parsers.PaymentParser;
import org.eontechnology.and.peer.eon.midleware.parsers.RegistrationParser;
import org.eontechnology.and.peer.tx.TransactionType;
import org.eontechnology.and.peer.tx.midleware.builders.ComplexPaymentBuilder;
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
public class NestedTransactionCleanerTestIT {

  private static final String ACCOUNT_SEED1 =
      "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
  private static final String NEW_ACCOUNT =
      "2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0000";
  private TimeProvider timeProvider;
  private PeerContext ctx;

  @Before
  public void setUp() throws Exception {
    timeProvider = Mockito.mock(TimeProvider.class);
    ctx =
        new PeerContext(
            PeerStarterFactory.create()
                .route(TransactionType.Payment, new PaymentParser())
                .route(TransactionType.Registration, new RegistrationParser())
                .route(TransactionType.ComplexPayment, new ComplexPaymentParserV1())
                .seed(ACCOUNT_SEED1)
                .build(timeProvider));
  }

  @Test
  public void step_1_cleaning() throws Exception {

    ISigner newAccountSigner = new TestSigner(NEW_ACCOUNT);
    AccountID newAccountID = new AccountID(newAccountSigner.getPublicKey());

    AccountID accountID = new AccountID(ctx.getSigner().getPublicKey());

    Block lastBlock = ctx.blockExplorerService.getLastBlock();
    Mockito.when(timeProvider.get()).thenReturn(lastBlock.getTimestamp());

    // registration

    Transaction tx1 =
        RegistrationBuilder.createNew(newAccountSigner.getPublicKey())
            .validity(timeProvider.get(), 3600)
            .build(ctx.getNetworkID(), ctx.getSigner());
    ctx.transactionBotService.putTransaction(tx1);

    Mockito.when(timeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
    ctx.generateBlockForNow();

    // payment

    lastBlock = ctx.blockExplorerService.getLastBlock();

    Transaction tx2 =
        PaymentBuilder.createNew(1000L, newAccountID)
            .validity(timeProvider.get(), 3600)
            .build(ctx.getNetworkID(), ctx.getSigner());
    ctx.transactionBotService.putTransaction(tx2);

    Mockito.when(timeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
    ctx.generateBlockForNow();

    // put complex transaction to backlog

    lastBlock = ctx.blockExplorerService.getLastBlock();
    Transaction nestedTx1 =
        PaymentBuilder.createNew(100L, newAccountID)
            .validity(timeProvider.get() - 1, 3600)
            .forFee(0L)
            .build(ctx.getNetworkID(), ctx.getSigner());
    Transaction nestedTx2 =
        PaymentBuilder.createNew(1L, accountID)
            .validity(timeProvider.get() - 1, 3600)
            .forFee(0L)
            .refBy(nestedTx1.getID())
            .build(ctx.getNetworkID(), newAccountSigner);

    Transaction tx4 =
        ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2})
            .validity(timeProvider.get(), 3600)
            .forFee(30)
            .build(ctx.getNetworkID(), newAccountSigner);

    ctx.transactionBotService.putTransaction(tx4);
    Mockito.when(timeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
    ctx.generateBlockForNow();

    // check

    Dao<DbNestedTransaction, Long> dao =
        DaoManager.createDao(ctx.storage.getConnectionSource(), DbNestedTransaction.class);
    Assert.assertEquals(dao.countOf(), 2);

    lastBlock = ctx.blockExplorerService.getLastBlock();
    Mockito.when(timeProvider.get())
        .thenReturn(lastBlock.getTimestamp() + Constant.SECONDS_IN_DAY + 1);
    ctx.generateBlockForNow();

    ctx.nestedTransactionCleanupTask.run();
    Assert.assertEquals(dao.countOf(), 2);

    lastBlock = ctx.blockExplorerService.getLastBlock();
    Mockito.when(timeProvider.get())
        .thenReturn(lastBlock.getTimestamp() + Constant.SECONDS_IN_DAY + 180 + 1);
    ctx.generateBlockForNow();

    ctx.nestedTransactionCleanupTask.run();
    Assert.assertEquals(dao.countOf(), 0);
  }
}
