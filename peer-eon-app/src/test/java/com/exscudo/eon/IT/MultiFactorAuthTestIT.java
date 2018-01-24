package com.exscudo.eon.IT;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;

import com.exscudo.eon.bot.AccountService;
import com.exscudo.eon.cfg.Fork;
import com.exscudo.eon.cfg.ForkInitializer;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TimeProvider;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.crypto.Ed25519Signer;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.transactions.builders.AccountRegistrationBuilder;
import com.exscudo.peer.eon.transactions.builders.DelegateBuilder;
import com.exscudo.peer.eon.transactions.builders.PaymentBuilder;
import com.exscudo.peer.eon.transactions.builders.QuorumBuilder;
import com.exscudo.peer.eon.transactions.builders.RejectionBuilder;
import com.exscudo.peer.store.sqlite.Storage;
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
	private static final String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
	private static final String DELEGATE_1 = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
	private static final String DELEGATE_2 = "112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00";
	private static final String DELEGATE_NEW = "2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0000";

	private TimeProvider mockTimeProvider;
	private PeerContext ctx;

	@Before
	public void setUp() throws Exception {
		mockTimeProvider = Mockito.mock(TimeProvider.class);

		Storage storage = Utils.createStorage();
		long timestamp = Utils.getLastBlock(storage).getTimestamp();
		String begin = Instant.ofEpochMilli((timestamp - 1) * 1000).toString();
		String end = Instant.ofEpochMilli((timestamp + 10 * 180 * 1000) * 1000).toString();
		IFork fork = new Fork(Utils.getGenesisBlockID(storage),
				new Fork.Item[] { new Fork.Item(1, begin, end, ForkInitializer.items[2].handler, 2) });
		ctx = new PeerContext(GENERATOR, mockTimeProvider, storage, fork);

	}

	// TODO: check account state
	@Test
	public void step_1_mfa() throws Exception {

		ISigner delegate_1 = new Ed25519Signer(DELEGATE_1);
		ISigner delegate_2 = new Ed25519Signer(DELEGATE_2);

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();
		int timestamp = lastBlock.getTimestamp();

		// registration

		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

		Transaction tx1 = AccountRegistrationBuilder.createNew(delegate_1.getPublicKey()).validity(timestamp + 1, 3600)
				.forFee(1L).build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx1);
		Transaction tx2 = AccountRegistrationBuilder.createNew(delegate_2.getPublicKey()).validity(timestamp + 1, 3600)
				.forFee(1L).build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx2);

		ctx.generateBlockForNow();
		Assert.assertEquals("Registration in block", 2,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		// payments
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 2 * 180 + 1);

		Transaction tx3 = PaymentBuilder.createNew(1000L, Format.MathID.pick(delegate_1.getPublicKey())).forFee(1L)
				.validity(timestamp + 180 + 1, 3600).build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx3);
		Transaction tx4 = PaymentBuilder.createNew(1000L, Format.MathID.pick(delegate_2.getPublicKey())).forFee(1L)
				.validity(timestamp + 180 + 1, 3600).build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx4);

		ctx.generateBlockForNow();
		Assert.assertEquals("Payments in block", 2,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		// set quorum and delegates
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 3 * 180 + 1);

		Transaction tx6 = DelegateBuilder.createNew(Format.MathID.pick(delegate_1.getPublicKey()), 30)
				.validity(timestamp + 2 * 180 + 1, 3600).build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx6);
		Transaction tx7 = DelegateBuilder.createNew(Format.MathID.pick(delegate_2.getPublicKey()), 20)
				.validity(timestamp + 2 * 180 + 1, 3600).build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx7);
		Transaction tx8 = QuorumBuilder.createNew(50).quorumForType(TransactionType.OrdinaryPayment, 85)
				.validity(timestamp + 2 * 180 + 1, 3600).build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx8);

		ctx.generateBlockForNow();
		Assert.assertEquals("Transactions in block", 3,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		String id1 = Format.ID.accountId(Format.MathID.pick(delegate_1.getPublicKey()));
		AccountService.Info info1 = ctx.accountBotService.getInformation(id1);
		assertTrue(info1.voter.get(Format.ID.accountId(Format.MathID.pick(ctx.getSigner().getPublicKey()))) == 30);

		String id2 = Format.ID.accountId(Format.MathID.pick(delegate_2.getPublicKey()));
		AccountService.Info info2 = ctx.accountBotService.getInformation(id2);
		assertTrue(info2.voter.get(Format.ID.accountId(Format.MathID.pick(ctx.getSigner().getPublicKey()))) == 20);

		// enable mfa
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 4 * 180 + 1);

		Transaction tx5 = DelegateBuilder.createNew(Format.MathID.pick(ctx.getSigner().getPublicKey()), 50)
				.validity(timestamp + 3 * 180 + 1, 3600).build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx5);

		ctx.generateBlockForNow();
		Assert.assertEquals("Transactions in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		// try put transaction
		ISigner delegate_new = new Ed25519Signer(DELEGATE_NEW);
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * 5 + 1);

		Transaction tx9 = AccountRegistrationBuilder.createNew(delegate_new.getPublicKey())
				.validity(timestamp + 4 * 180 + 1, 3600).build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx9);
		Transaction tx10 = PaymentBuilder.createNew(100L, Format.MathID.pick(delegate_1.getPublicKey()))
				.validity(timestamp + 4 * 180 + 1, 3600).build(ctx.getSigner());
		try {
			ctx.transactionBotService.putTransaction(tx10);
		} catch (Exception ignore) {

		}

		ctx.generateBlockForNow();
		Assert.assertEquals("Transactions in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		// try put transaction
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * 6 + 1);

		Transaction tx11 = PaymentBuilder.createNew(100L, Format.MathID.pick(delegate_1.getPublicKey()))
				.validity(timestamp + 180 * 5 + 1, 3600).build(ctx.getSigner(), new ISigner[] { delegate_1 });
		try {
			ctx.transactionBotService.putTransaction(tx11);
		} catch (Exception ignore) {

		}

		ctx.generateBlockForNow();
		Assert.assertEquals("Transactions in block", 0,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		// put transaction
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * 7 + 1);

		Transaction tx12 = PaymentBuilder.createNew(100L, Format.MathID.pick(delegate_1.getPublicKey()))
				.validity(timestamp + 180 * 6 + 1, 3600)
				.build(ctx.getSigner(), new ISigner[] { delegate_1, delegate_2 });
		ctx.transactionBotService.putTransaction(tx12);

		ctx.generateBlockForNow();
		Assert.assertEquals("Transactions in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		// reject
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * 8 + 1);

		Transaction tx13 = RejectionBuilder.createNew(Format.MathID.pick(ctx.getSigner().getPublicKey()))
				.validity(timestamp + 180 * 7 + 1, 3600).build(delegate_2);
		ctx.transactionBotService.putTransaction(tx13);

		ctx.generateBlockForNow();
		Assert.assertEquals("Transactions in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());
		info2 = ctx.accountBotService.getInformation(id2);
		assertNull(info2.voter);

		// put transaction
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * 9 + 1);
		ctx.transactionBotService.putTransaction(tx11);
		ctx.generateBlockForNow();
		Assert.assertEquals("Transactions in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());
	}
}
