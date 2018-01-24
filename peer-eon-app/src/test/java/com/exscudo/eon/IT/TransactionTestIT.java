package com.exscudo.eon.IT;

import com.exscudo.eon.bot.AccountService;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TimeProvider;
import com.exscudo.peer.eon.crypto.Ed25519Signer;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.transactions.builders.AccountRegistrationBuilder;
import com.exscudo.peer.eon.transactions.builders.DepositRefillBuilder;
import com.exscudo.peer.eon.transactions.builders.DepositWithdrawBuilder;
import com.exscudo.peer.eon.transactions.builders.PaymentBuilder;
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

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

		Transaction tx = AccountRegistrationBuilder.createNew(signer.getPublicKey())
				.validity(lastBlock.getTimestamp() + 100, 3600).forFee(1L).build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx);
		ctx.generateBlockForNow();

		Assert.assertEquals("Registration in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

		Transaction tx2 = PaymentBuilder.createNew(10000L, Format.MathID.pick(signer.getPublicKey())).forFee(1L)
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

		Transaction tx3 = PaymentBuilder.createNew(8000L, Format.MathID.pick(ctx.getSigner().getPublicKey()))
				.forFee(1000L).validity(lastBlock.getTimestamp() + 200, 3600).build(signer);
		Transaction tx4 = PaymentBuilder.createNew(8000L, Format.MathID.pick(ctx2.getSigner().getPublicKey()))
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
		ISigner signer = new Ed25519Signer(GENERATOR2);
		ISigner signerNew = new Ed25519Signer(GENERATOR_NEW);

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		String signerID = Format.ID.accountId(Format.MathID.pick(signer.getPublicKey()));
		String signerCtxID = Format.ID.accountId(Format.MathID.pick(ctx.getSigner().getPublicKey()));
		String signerNewID = Format.ID.accountId(Format.MathID.pick(signerNew.getPublicKey()));

		// Create new acc
		AccountService.EONBalance balance = ctx.accountBotService.getBalance(signerID);
		AccountService.EONBalance balanceCtx = ctx.accountBotService.getBalance(signerCtxID);
		AccountService.EONBalance balanceNew = ctx.accountBotService.getBalance(signerNewID);

		Assert.assertEquals(AccountService.State.NotFound, ctx.accountBotService.getState(signerNewID));
		Assert.assertEquals(AccountService.State.Unauthorized, balanceNew.state);

		Transaction tx = AccountRegistrationBuilder.createNew(signerNew.getPublicKey())
				.validity(lastBlock.getTimestamp() + 100, 3600).forFee(1L).build(signer);
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
		ctx.transactionBotService.putTransaction(tx);

		balanceNew = ctx.accountBotService.getBalance(signerNewID);
		Assert.assertEquals(AccountService.State.Processing, ctx.accountBotService.getState(signerNewID));
		Assert.assertEquals(AccountService.State.Unauthorized, balanceNew.state);

		ctx.generateBlockForNow();

		Assert.assertEquals("Registration in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());
		Assert.assertEquals("Fee in generator balance", balanceCtx.amount + 1L,
				ctx.accountBotService.getBalance(signerCtxID).amount);
		Assert.assertEquals("Fee from sender", balance.amount - 1L, ctx.accountBotService.getBalance(signerID).amount);
		Assert.assertEquals("New acc created", AccountService.State.OK,
				ctx.accountBotService.getInformation(signerNewID).state);

		// Payment to new acc
		balance = ctx.accountBotService.getBalance(signerID);
		balanceCtx = ctx.accountBotService.getBalance(signerCtxID);
		balanceNew = ctx.accountBotService.getBalance(signerNewID);

		Transaction tx2 = PaymentBuilder.createNew(10000L, Format.MathID.pick(signerNew.getPublicKey())).forFee(1L)
				.validity(lastBlock.getTimestamp() + 200, 3600).build(signer);
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);
		ctx.transactionBotService.putTransaction(tx2);
		ctx.generateBlockForNow();

		Assert.assertEquals("Payment in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());
		Assert.assertEquals("Fee in generator balance", balanceCtx.amount + 1L,
				ctx.accountBotService.getBalance(signerCtxID).amount);
		Assert.assertEquals("Fee and amount from sender", balance.amount - (10000L + 1L),
				ctx.accountBotService.getBalance(signerID).amount);
		Assert.assertEquals("Amount to recipient", balanceNew.amount + 10000L,
				ctx.accountBotService.getBalance(signerNewID).amount);

		// Deposit in new acc
		balanceCtx = ctx.accountBotService.getBalance(signerCtxID);
		balanceNew = ctx.accountBotService.getBalance(signerNewID);
		AccountService.Info informationNew = ctx.accountBotService.getInformation(signerNewID);

		Transaction tx3 = DepositRefillBuilder.createNew(100L).validity(lastBlock.getTimestamp() + 200, 3600)
				.build(signerNew);
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 3 + 1);
		ctx.transactionBotService.putTransaction(tx3);
		ctx.generateBlockForNow();

		Assert.assertEquals("Deposit.refill in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());
		Assert.assertEquals("Fee in generator balance",
				balanceCtx.amount + DepositRefillBuilder.DEPOSIT_TRANSACTION_FEE,
				ctx.accountBotService.getBalance(signerCtxID).amount);
		Assert.assertEquals("Fee and amount from sender",
				balanceNew.amount - (100L + DepositRefillBuilder.DEPOSIT_TRANSACTION_FEE),
				ctx.accountBotService.getBalance(signerNewID).amount);

		Assert.assertTrue("Deposit in sender", ctx.accountBotService.getInformation(signerNewID).deposit == 100L);

		// Deposit from new acc
		balanceCtx = ctx.accountBotService.getBalance(signerCtxID);
		balanceNew = ctx.accountBotService.getBalance(signerNewID);
		informationNew = ctx.accountBotService.getInformation(signerNewID);

		Transaction tx4 = DepositWithdrawBuilder.createNew(50L).validity(lastBlock.getTimestamp() + 200, 3600)
				.build(signerNew);
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 4 + 1);
		ctx.transactionBotService.putTransaction(tx4);
		ctx.generateBlockForNow();

		Assert.assertEquals("Deposit.withdraw in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());
		Assert.assertEquals("Fee in generator balance",
				balanceCtx.amount + DepositRefillBuilder.DEPOSIT_TRANSACTION_FEE,
				ctx.accountBotService.getBalance(signerCtxID).amount);
		Assert.assertEquals("Fee and amount from sender",
				balanceNew.amount + 50L - DepositRefillBuilder.DEPOSIT_TRANSACTION_FEE,
				ctx.accountBotService.getBalance(signerNewID).amount);
		Assert.assertTrue("Deposit in sender", ctx.accountBotService.getInformation(signerNewID).deposit == 50L);
	}

	@Test
	public void step_3_transaction_duplicate() throws Exception {

		PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
		ISigner signerNew = new Ed25519Signer(GENERATOR_NEW);

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);
		Transaction tx = AccountRegistrationBuilder.createNew(signerNew.getPublicKey()).validity(mockTimeProvider.get(), (short) 60, 1)
				.forFee(1L).build(ctx.signer);
		Assert.assertTrue(ctx.context.getInstance().getBacklogService().put(tx));
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

		ctx.generateBlockForNow();
		Assert.assertEquals("Registration in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		Transaction tx1 = PaymentBuilder.createNew(100L, Format.MathID.pick(signerNew.getPublicKey()))
				.validity(mockTimeProvider.get(), (short) 60, 1).build(ctx.signer);
		Assert.assertTrue(ctx.context.getInstance().getBacklogService().put(tx1));
		Assert.assertFalse(ctx.context.getInstance().getBacklogService().put(tx1));
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 2 * 180 + 1);

		ctx.generateBlockForNow();
		Assert.assertEquals("Payment in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		Assert.assertTrue(ctx.context.getInstance().getBacklogService().put(tx1));
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 3 * 180 + 1);

		ctx.generateBlockForNow();
		Assert.assertEquals("Emprty block", 0,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

	}

	@Test
	public void step_4_double_spending() throws Exception {

		PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
		ISigner signerNew = new Ed25519Signer(GENERATOR_NEW);

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);

		// registration
		Transaction tx = AccountRegistrationBuilder.createNew(signerNew.getPublicKey()).validity(mockTimeProvider.get(), (short) 60, 1)
				.forFee(1L).build(ctx.signer);
		Assert.assertTrue(ctx.context.getInstance().getBacklogService().put(tx));

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
		ctx.generateBlockForNow();

		Assert.assertEquals("Registration in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		// payment
		Transaction tx1 = PaymentBuilder.createNew(100L, Format.MathID.pick(signerNew.getPublicKey()))
				.validity(mockTimeProvider.get(), (short) 60, 1).build(ctx.signer);
		Assert.assertTrue(ctx.context.getInstance().getBacklogService().put(tx1));

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 2 * 180 + 1);
		ctx.generateBlockForNow();

		Assert.assertEquals("Payment in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		// back payment
		Transaction tx2 = PaymentBuilder.createNew(50L, Format.MathID.pick(ctx.signer.getPublicKey())).forFee(1L)
				.validity(mockTimeProvider.get(), (short) 60, 1).build(signerNew);
		Assert.assertTrue(ctx.context.getInstance().getBacklogService().put(tx2));
		Transaction tx3 = PaymentBuilder.createNew(70L, Format.MathID.pick(ctx.signer.getPublicKey())).forFee(3L)
				.validity(mockTimeProvider.get(), (short) 60, 1).build(signerNew);
		try {
			ctx.context.getInstance().getBacklogService().put(tx3);
		} catch(ValidateException ignore){

		}

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 3 * 180 + 1);
		ctx.generateBlockForNow();

		Assert.assertEquals("Payment in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

	}

}
