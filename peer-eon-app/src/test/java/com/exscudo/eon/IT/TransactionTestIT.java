package com.exscudo.eon.IT;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import com.exscudo.eon.bot.AccountService;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TimeProvider;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.transactions.Deposit;
import com.exscudo.peer.eon.transactions.Payment;
import com.exscudo.peer.eon.transactions.Registration;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TransactionTestIT {
	private static final String GENERATOR = "55373380ff77987646b816450824310fb377c1a14b6f725b94382af3cf7b788a";
	private static final String GENERATOR2 = "dd6403d520afbfadeeff0b1bb49952440b767663454ab1e5f1a358e018cf9c73";
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
		ISigner signer = (new PeerContext(GENERATOR_NEW, mockTimeProvider)).getSigner();

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

		Transaction tx = Registration.newAccount(signer.getPublicKey())
				.validity(lastBlock.getTimestamp() + 100, 3600).forFee(1L).build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx);
		ctx.generateBlockForNow();

		Assert.assertEquals("Registration in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

		Transaction tx2 = Payment.newPayment(10000L, Format.MathID.pick(signer.getPublicKey())).forFee(1L)
				.validity(lastBlock.getTimestamp() + 200, 3600).build(ctx.getSigner());

		ctx.transactionBotService.putTransaction(tx2);
		ctx.generateBlockForNow();

		Assert.assertEquals("Payment in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		ctx2.setPeerToConnect(ctx);
		ctx.setPeerToConnect(ctx2);

		ctx2.fullBlockSync();

		Assert.assertEquals("Block synchronized",
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());

		Transaction tx3 = Payment.newPayment(8000L, Format.MathID.pick(ctx.getSigner().getPublicKey())).forFee(1000L)
				.validity(lastBlock.getTimestamp() + 200, 3600).build(signer);
		Transaction tx4 = Payment.newPayment(8000L, Format.MathID.pick(ctx2.getSigner().getPublicKey()))
				.forFee(1000L).validity(lastBlock.getTimestamp() + 200, 3600).build(signer);

		ctx.transactionBotService.putTransaction(tx3);
		ctx2.transactionBotService.putTransaction(tx4);

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 3 + 1);

		ctx.syncTransactionListTask.run();
		ctx2.syncTransactionListTask.run();

		Assert.assertTrue(ctx.context.getInstance().getBacklogService().contains(tx3.getID()));
		Assert.assertFalse(ctx.context.getInstance().getBacklogService().contains(tx4.getID()));
		Assert.assertFalse(ctx2.context.getInstance().getBacklogService().contains(tx3.getID()));
		Assert.assertTrue(ctx2.context.getInstance().getBacklogService().contains(tx4.getID()));

		ctx.generateBlockForNow();
		ctx2.generateBlockForNow();

		Assert.assertNotEquals("Block different",
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());
		Assert.assertEquals("Block TIMESTAMP equals",
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTimestamp(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getTimestamp());

		ctx.fullBlockSync();
		ctx2.fullBlockSync();

		Assert.assertEquals("Block synchronized",
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());

		Assert.assertFalse(ctx.context.getInstance().getBacklogService().contains(tx3.getID()));
		Assert.assertFalse(ctx.context.getInstance().getBacklogService().contains(tx4.getID()));
		Assert.assertFalse(ctx2.context.getInstance().getBacklogService().contains(tx3.getID()));
		Assert.assertFalse(ctx2.context.getInstance().getBacklogService().contains(tx4.getID()));
	}

	@Test
	public void step_2_balances_checker() throws Exception {
		// Init peer and etc...
		PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
		ISigner signer = (new PeerContext(GENERATOR2, mockTimeProvider)).getSigner();
		ISigner signerNew = (new PeerContext(GENERATOR_NEW, mockTimeProvider)).getSigner();

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		String signerID = Format.ID.accountId(Format.MathID.pick(signer.getPublicKey()));
		String signerCtxID = Format.ID.accountId(Format.MathID.pick(ctx.getSigner().getPublicKey()));
		String signerNewID = Format.ID.accountId(Format.MathID.pick(signerNew.getPublicKey()));

		// Create new acc
		AccountService.Info information = ctx.accountBotService.getInformation(signerID);
		AccountService.Info informationCtx = ctx.accountBotService.getInformation(signerCtxID);
		AccountService.Info informationNew = ctx.accountBotService.getInformation(signerNewID);

		Assert.assertEquals(AccountService.State.NotFound, informationNew.state);

		Transaction tx = Registration.newAccount(signerNew.getPublicKey())
				.validity(lastBlock.getTimestamp() + 100, 3600).forFee(1L).build(signer);
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
		ctx.transactionBotService.putTransaction(tx);

		informationNew = ctx.accountBotService.getInformation(signerNewID);
		Assert.assertEquals(AccountService.State.Processing, informationNew.state);

		ctx.generateBlockForNow();

		Assert.assertEquals("Registration in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());
		Assert.assertEquals("Fee in generator balance", informationCtx.amount + 1L,
				ctx.accountBotService.getInformation(signerCtxID).amount);
		Assert.assertEquals("Fee from sender", information.amount - 1L,
				ctx.accountBotService.getInformation(signerID).amount);
		Assert.assertEquals("New acc created", AccountService.State.OK,
				ctx.accountBotService.getInformation(signerNewID).state);

		// Payment to new acc
		information = ctx.accountBotService.getInformation(signerID);
		informationCtx = ctx.accountBotService.getInformation(signerCtxID);
		informationNew = ctx.accountBotService.getInformation(signerNewID);

		Transaction tx2 = Payment.newPayment(10000L, Format.MathID.pick(signerNew.getPublicKey())).forFee(1L)
				.validity(lastBlock.getTimestamp() + 200, 3600).build(signer);
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);
		ctx.transactionBotService.putTransaction(tx2);
		ctx.generateBlockForNow();

		Assert.assertEquals("Payment in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());
		Assert.assertEquals("Fee in generator balance", informationCtx.amount + 1L,
				ctx.accountBotService.getInformation(signerCtxID).amount);
		Assert.assertEquals("Fee and amount from sender", information.amount - (10000L + 1L),
				ctx.accountBotService.getInformation(signerID).amount);
		Assert.assertEquals("Amount to recipient", informationNew.amount + 10000L,
				ctx.accountBotService.getInformation(signerNewID).amount);

		// Deposit in new acc
		informationCtx = ctx.accountBotService.getInformation(signerCtxID);
		informationNew = ctx.accountBotService.getInformation(signerNewID);

		Transaction tx3 = Deposit.refill(100L).validity(lastBlock.getTimestamp() + 200, 3600).build(signerNew);
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 3 + 1);
		ctx.transactionBotService.putTransaction(tx3);
		ctx.generateBlockForNow();

		Assert.assertEquals("Deposit.refill in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());
		Assert.assertEquals("Fee in generator balance", informationCtx.amount + Deposit.DEPOSIT_TRANSACTION_FEE,
				ctx.accountBotService.getInformation(signerCtxID).amount);
		Assert.assertEquals("Fee and amount from sender",
				informationNew.amount - (100L + Deposit.DEPOSIT_TRANSACTION_FEE),
				ctx.accountBotService.getInformation(signerNewID).amount);
		Assert.assertEquals("Deposit in sender", informationNew.deposit + 100L,
				ctx.accountBotService.getInformation(signerNewID).deposit);

		// Deposit from new acc
		informationCtx = ctx.accountBotService.getInformation(signerCtxID);
		informationNew = ctx.accountBotService.getInformation(signerNewID);

		Transaction tx4 = Deposit.withdraw(50L).validity(lastBlock.getTimestamp() + 200, 3600).build(signerNew);
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 4 + 1);
		ctx.transactionBotService.putTransaction(tx4);
		ctx.generateBlockForNow();

		Assert.assertEquals("Deposit.withdraw in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());
		Assert.assertEquals("Fee in generator balance", informationCtx.amount + Deposit.DEPOSIT_TRANSACTION_FEE,
				ctx.accountBotService.getInformation(signerCtxID).amount);
		Assert.assertEquals("Fee and amount from sender", informationNew.amount + 50L - Deposit.DEPOSIT_TRANSACTION_FEE,
				ctx.accountBotService.getInformation(signerNewID).amount);
		Assert.assertEquals("Deposit in sender", informationNew.deposit - 50L,
				ctx.accountBotService.getInformation(signerNewID).deposit);
	}

}
