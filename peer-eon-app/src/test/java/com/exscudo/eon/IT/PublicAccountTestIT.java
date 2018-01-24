package com.exscudo.eon.IT;

import java.time.Instant;

import com.exscudo.eon.cfg.Fork;
import com.exscudo.eon.cfg.ForkInitializer;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TimeProvider;
import com.exscudo.peer.eon.crypto.Ed25519Signer;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.transactions.builders.AccountPublicationBuilder;
import com.exscudo.peer.eon.transactions.builders.AccountRegistrationBuilder;
import com.exscudo.peer.eon.transactions.builders.DelegateBuilder;
import com.exscudo.peer.eon.transactions.builders.PaymentBuilder;
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
public class PublicAccountTestIT {

	private static final String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
	private static final String DELEGATE_1 = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
	private static final String DELEGATE_2 = "112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00";

	private TimeProvider mockTimeProvider;
	private PeerContext ctx;

	private ISigner delegate_1;
	private ISigner delegate_2;

	@Before
	public void setUp() throws Exception {
		mockTimeProvider = Mockito.mock(TimeProvider.class);

		Storage storage = Utils.createStorage();
		long time = Utils.getLastBlock(storage).getTimestamp();
		String begin = Instant.ofEpochMilli((time - 1) * 1000).toString();
		String end = Instant.ofEpochMilli((time + 10 * 180 * 1000) * 1000).toString();
		IFork fork = new Fork(Utils.getGenesisBlockID(storage),
				new Fork.Item[] { new Fork.Item(1, begin, end, ForkInitializer.items[2].handler, 2) });
		ctx = new PeerContext(GENERATOR, mockTimeProvider, storage, fork);

		delegate_1 = new Ed25519Signer(DELEGATE_1);
		delegate_2 = new Ed25519Signer(DELEGATE_2);

		prepare_peer();
	}

	private void prepare_peer() throws Exception {

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		// registration
		int timestamp = lastBlock.getTimestamp();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

		Transaction tx1 = AccountRegistrationBuilder.createNew(delegate_1.getPublicKey()).validity(timestamp, 3600)
				.forFee(1L).build(ctx.getSigner());
		Transaction tx2 = AccountRegistrationBuilder.createNew(delegate_2.getPublicKey()).validity(timestamp, 3600)
				.forFee(1L).build(ctx.getSigner());

		ctx.transactionBotService.putTransaction(tx1);
		ctx.transactionBotService.putTransaction(tx2);

		ctx.generateBlockForNow();

		// delegate + payment
		timestamp = mockTimeProvider.get();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

		Transaction tx3 = DelegateBuilder.createNew(Format.MathID.pick(delegate_1.getPublicKey()), 100)
				.validity(timestamp, 3600).build(ctx.getSigner());
		Transaction tx4 = DelegateBuilder.createNew(Format.MathID.pick(delegate_2.getPublicKey()), 100)
				.validity(timestamp, 3600).build(ctx.getSigner());

		ctx.transactionBotService.putTransaction(tx3);
		ctx.transactionBotService.putTransaction(tx4);

		Transaction tx5 = PaymentBuilder.createNew(1000000L, Format.MathID.pick(delegate_1.getPublicKey()))
				.validity(timestamp, 3600).build(ctx.getSigner());
		Transaction tx6 = PaymentBuilder.createNew(1000000L, Format.MathID.pick(delegate_2.getPublicKey()))
				.validity(timestamp, 3600).build(ctx.getSigner());

		ctx.transactionBotService.putTransaction(tx5);
		ctx.transactionBotService.putTransaction(tx6);

		ctx.generateBlockForNow();

		// delegate self to 0
		timestamp = mockTimeProvider.get();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

		Transaction tx = DelegateBuilder.createNew(Format.MathID.pick(ctx.getSigner().getPublicKey()))
				.validity(timestamp, 3600).build(ctx.getSigner());

		ctx.transactionBotService.putTransaction(tx);

		ctx.generateBlockForNow();
	}

	@Test
	public void step_1_PublicAccNormal() throws Exception {

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();
		int timestamp = lastBlock.getTimestamp();

		// Generate 1 day
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * Constant.BLOCK_IN_DAY + 1);

		ctx.generateBlockForNow();

		// Publication acc
		timestamp = mockTimeProvider.get();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

		Transaction tx = AccountPublicationBuilder.createNew(GENERATOR).validity(timestamp, 3600).build(ctx.getSigner(),
				new ISigner[] { delegate_1 });
		ctx.transactionBotService.putTransaction(tx);

		ctx.generateBlockForNow();

		Assert.assertEquals(GENERATOR, ctx.accountBotService
				.getInformation(Format.ID.accountId(Format.MathID.pick(ctx.getSigner().getPublicKey()))).seed);

		// Delegate to me
		timestamp = mockTimeProvider.get();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);
		Transaction tx2 = DelegateBuilder.createNew(Format.MathID.pick(ctx.getSigner().getPublicKey()), 100)
				.validity(timestamp, 3600).build(delegate_1);

		try {
			ctx.transactionBotService.putTransaction(tx2);
			Assert.assertTrue(false);
		} catch (Exception ignored) {
			Assert.assertTrue(true);
		}

		// Weight up
		Transaction tx3 = DelegateBuilder.createNew(Format.MathID.pick(ctx.getSigner().getPublicKey()), 80)
				.validity(timestamp, 3600).build(ctx.getSigner(), new ISigner[] { delegate_1 });

		try {
			ctx.transactionBotService.putTransaction(tx3);
			Assert.assertTrue(false);
		} catch (Exception ignored) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void step_2_PublicAccEarly() throws Exception {

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();
		int timestamp = lastBlock.getTimestamp();

		// Generate 0.5 days
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * Constant.BLOCK_IN_DAY / 2 + 1);

		ctx.generateBlockForNow();

		// Publication acc
		timestamp = mockTimeProvider.get();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

		Transaction tx = AccountPublicationBuilder.createNew(GENERATOR).validity(timestamp, 3600).build(ctx.getSigner(),
				new ISigner[] { delegate_1 });

		try {
			ctx.transactionBotService.putTransaction(tx);
			Assert.assertTrue(false);
		} catch (Exception ignored) {
		}

		ctx.generateBlockForNow();

		Assert.assertNull(ctx.accountBotService
				.getInformation(Format.ID.accountId(Format.MathID.pick(ctx.getSigner().getPublicKey()))).seed);

	}

	@Test
	public void step_3_DelegatedAcc() throws Exception {

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();
		int timestamp = lastBlock.getTimestamp();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

		// Delegate
		Transaction tx = DelegateBuilder.createNew(Format.MathID.pick(ctx.getSigner().getPublicKey()), 100)
				.validity(timestamp, 3600).build(delegate_1);

		ctx.transactionBotService.putTransaction(tx);

		ctx.generateBlockForNow();

		// Generate 1 day
		timestamp = mockTimeProvider.get();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * Constant.BLOCK_IN_DAY + 1);

		ctx.generateBlockForNow();

		// Publication acc
		timestamp = mockTimeProvider.get();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

		Transaction tx2 = AccountPublicationBuilder.createNew(GENERATOR).validity(timestamp, 3600)
				.build(ctx.getSigner(), new ISigner[] { delegate_1 });
		try {
			ctx.transactionBotService.putTransaction(tx2);
			Assert.assertTrue(false);
		} catch (Exception ignored) {
		}

		ctx.generateBlockForNow();

		Assert.assertNull(ctx.accountBotService
				.getInformation(Format.ID.accountId(Format.MathID.pick(ctx.getSigner().getPublicKey()))).seed);

	}

	@Test
	public void step_4_SignWeightExist() throws Exception {

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();
		int timestamp = lastBlock.getTimestamp();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

		// Delegate Weight to me
		Transaction tx = DelegateBuilder.createNew(Format.MathID.pick(ctx.getSigner().getPublicKey()), 80)
				.validity(timestamp, 3600).build(ctx.getSigner(), new ISigner[] { delegate_1 });

		ctx.transactionBotService.putTransaction(tx);

		ctx.generateBlockForNow();

		// Generate 1 day
		timestamp = mockTimeProvider.get();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 * Constant.BLOCK_IN_DAY + 1);

		ctx.generateBlockForNow();

		// Publication acc
		timestamp = mockTimeProvider.get();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

		Transaction tx2 = AccountPublicationBuilder.createNew(GENERATOR).validity(timestamp, 3600)
				.build(ctx.getSigner(), new ISigner[] { delegate_1 });
		try {
			ctx.transactionBotService.putTransaction(tx2);
			Assert.assertTrue(false);
		} catch (Exception ignored) {
		}

		ctx.generateBlockForNow();

		Assert.assertNull(ctx.accountBotService
				.getInformation(Format.ID.accountId(Format.MathID.pick(ctx.getSigner().getPublicKey()))).seed);

	}
}
