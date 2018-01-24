package com.exscudo.eon.IT;

import java.time.Instant;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import com.exscudo.eon.bot.AccountService;
import com.exscudo.eon.bot.ColoredCoinService;
import com.exscudo.eon.cfg.Fork;
import com.exscudo.eon.cfg.ForkInitializer;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TimeProvider;
import com.exscudo.peer.eon.crypto.Ed25519Signer;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.transactions.builders.*;
import com.exscudo.peer.eon.utils.ColoredCoinId;
import com.exscudo.peer.store.sqlite.Storage;

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

		Storage storage = Utils.createStorage();
		long time = Utils.getLastBlock(storage).getTimestamp();
		String begin = Instant.ofEpochMilli((time - 1) * 1000).toString();
		String end = Instant.ofEpochMilli((time + 10 * 180 * 1000) * 1000).toString();
		IFork fork = new Fork(Utils.getGenesisBlockID(storage),
				new Fork.Item[]{new Fork.Item(1, begin, end, ForkInitializer.items[3].handler, 2)});
		ctx = new PeerContext(GENERATOR, mockTimeProvider, storage, fork);

		coloredCoinSigner = new Ed25519Signer(COLORED_COIN);

		create_colored_coin();
	}

	private void create_colored_coin() throws Exception {

		long accountID = Format.MathID.pick(coloredCoinSigner.getPublicKey());
		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		// registration
		int timestamp = lastBlock.getTimestamp();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180 + 1);

		Transaction tx1 = AccountRegistrationBuilder.createNew(coloredCoinSigner.getPublicKey())
				.validity(timestamp, 3600).forFee(1L).build(ctx.getSigner());

		ctx.transactionBotService.putTransaction(tx1);

		ctx.generateBlockForNow();

		// payment
		timestamp = mockTimeProvider.get();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

		Transaction tx2 = PaymentBuilder.createNew(1000000L, accountID).validity(timestamp, 3600)
				.build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx2);

		ctx.generateBlockForNow();

		// colored account
		timestamp = mockTimeProvider.get();
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

		Transaction tx3 = ColoredCoinRegistrationBuilder.createNew(10000L, 1).validity(timestamp, 3600)
				.build(coloredCoinSigner);
		ctx.transactionBotService.putTransaction(tx3);

		ctx.generateBlockForNow();

	}

	@Test
	public void step_1_new_colored_coin() throws Exception {

		long accountID = Format.MathID.pick(coloredCoinSigner.getPublicKey());

		ColoredCoinService.Info info = ctx.coloredCoinService.getInfo(ColoredCoinId.convert(accountID),
				Integer.MAX_VALUE);
		Assert.assertTrue(10000L == info.moneySupply);

		AccountService.EONBalance balance = ctx.accountBotService.getBalance(Format.ID.accountId(accountID));
		Assert.assertEquals(balance.coloredCoins.get(ColoredCoinId.convert(accountID)), Long.valueOf(10000L));
	}

	@Test
	public void step_2_colored_payment() throws Exception {

		long senderID = Format.MathID.pick(coloredCoinSigner.getPublicKey());
		String coloredCoinID = ColoredCoinId.convert(senderID);
		long recipientID = Format.MathID.pick(ctx.getSigner().getPublicKey());

		AccountService.EONBalance senderBalance = ctx.accountBotService.getBalance(Format.ID.accountId(senderID));
		AccountService.EONBalance recepientBalance = ctx.accountBotService.getBalance(Format.ID.accountId(recipientID));

		Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID), Long.valueOf(10000L));
		Assert.assertNull(recepientBalance.coloredCoins);

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);

		int timestamp = mockTimeProvider.get();

		Transaction tx = ColoredPaymentBuilder.createNew(4000L, senderID, recipientID).validity(timestamp, 60 * 60)
				.build(coloredCoinSigner);
		ctx.transactionBotService.putTransaction(tx);

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
		ctx.generateBlockForNow();

		senderBalance = ctx.accountBotService.getBalance(Format.ID.accountId(senderID));
		recepientBalance = ctx.accountBotService.getBalance(Format.ID.accountId(recipientID));

		Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID), Long.valueOf(6000L));
		Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID), Long.valueOf(4000L));

	}

	@Test
	public void step_3_change_money_supply() throws Exception {

		long senderID = Format.MathID.pick(coloredCoinSigner.getPublicKey());
		long recipientID = Format.MathID.pick(ctx.getSigner().getPublicKey());
		String coloredCoinID = ColoredCoinId.convert(senderID);

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);

		// payment

		int timestamp = mockTimeProvider.get();
		Transaction tx = ColoredPaymentBuilder.createNew(10000L, senderID, recipientID).validity(timestamp, 60 * 60)
				.build(coloredCoinSigner);
		ctx.transactionBotService.putTransaction(tx);

		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
		ctx.generateBlockForNow();

		AccountService.EONBalance senderBalance = ctx.accountBotService.getBalance(Format.ID.accountId(senderID));
		AccountService.EONBalance recepientBalance = ctx.accountBotService.getBalance(Format.ID.accountId(recipientID));
		ColoredCoinService.Info coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID, Integer.MAX_VALUE);
		Assert.assertNull(senderBalance.coloredCoins);
		Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID), Long.valueOf(10000L));
		Assert.assertEquals(coloredCoinInfo.moneySupply, Long.valueOf(10000L));

		// increase money supply

		timestamp = mockTimeProvider.get();
		Transaction tx1 = ColoredCoinSupplyBuilder.createNew(11000L).validity(timestamp, 60 * 60)
				.build(coloredCoinSigner);
		ctx.transactionBotService.putTransaction(tx1);
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

		ctx.generateBlockForNow();

		senderBalance = ctx.accountBotService.getBalance(Format.ID.accountId(senderID));
		recepientBalance = ctx.accountBotService.getBalance(Format.ID.accountId(recipientID));
		coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID, Integer.MAX_VALUE);
		Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID), Long.valueOf(1000L));
		Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID), Long.valueOf(10000L));
		Assert.assertEquals(coloredCoinInfo.moneySupply, Long.valueOf(11000L));

		// try to decrease money supply

		timestamp = mockTimeProvider.get();
		Transaction tx2 = ColoredCoinSupplyBuilder.createNew(3000L).validity(timestamp, 60 * 60)
				.build(coloredCoinSigner);
		try {
			ctx.transactionBotService.putTransaction(tx2);
		} catch (Exception ignore) {
		}

		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);

		ctx.generateBlockForNow();

		senderBalance = ctx.accountBotService.getBalance(Format.ID.accountId(senderID));
		recepientBalance = ctx.accountBotService.getBalance(Format.ID.accountId(recipientID));
		coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID, Integer.MAX_VALUE);
		Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID), Long.valueOf(1000L));
		Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID), Long.valueOf(10000L));
		Assert.assertEquals(coloredCoinInfo.moneySupply, Long.valueOf(11000L));

		// payment

		timestamp = mockTimeProvider.get();
		Transaction tx3 = ColoredPaymentBuilder.createNew(7000L, senderID, senderID).validity(timestamp, 60 * 60)
				.build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx3);

		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
		ctx.generateBlockForNow();

		senderBalance = ctx.accountBotService.getBalance(Format.ID.accountId(senderID));
		recepientBalance = ctx.accountBotService.getBalance(Format.ID.accountId(recipientID));
		coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID, Integer.MAX_VALUE);
		Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID), Long.valueOf(8000L));
		Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID), Long.valueOf(3000L));
		Assert.assertEquals(coloredCoinInfo.moneySupply, Long.valueOf(11000L));

		// decrease money supply

		timestamp = mockTimeProvider.get();
		Transaction tx4 = ColoredCoinSupplyBuilder.createNew(4000L).validity(timestamp, 60 * 60)
				.build(coloredCoinSigner);
		ctx.transactionBotService.putTransaction(tx4);
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
		ctx.generateBlockForNow();

		senderBalance = ctx.accountBotService.getBalance(Format.ID.accountId(senderID));
		recepientBalance = ctx.accountBotService.getBalance(Format.ID.accountId(recipientID));
		coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID, Integer.MAX_VALUE);
		Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID), Long.valueOf(1000L));
		Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID), Long.valueOf(3000L));
		Assert.assertEquals(coloredCoinInfo.moneySupply, Long.valueOf(4000L));

	}

	@Test
	public void step_4_delete_colored_coin() throws Exception {

		long senderID = Format.MathID.pick(coloredCoinSigner.getPublicKey());
		long recipientID = Format.MathID.pick(ctx.getSigner().getPublicKey());
		String coloredCoinID = ColoredCoinId.convert(senderID);

		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);

		// payment

		int timestamp = mockTimeProvider.get();
		Transaction tx = ColoredPaymentBuilder.createNew(10000L, senderID, recipientID).validity(timestamp, 60 * 60)
				.build(coloredCoinSigner);
		ctx.transactionBotService.putTransaction(tx);

		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
		ctx.generateBlockForNow();

		AccountService.EONBalance senderBalance = ctx.accountBotService.getBalance(Format.ID.accountId(senderID));
		AccountService.EONBalance recepientBalance = ctx.accountBotService.getBalance(Format.ID.accountId(recipientID));
		ColoredCoinService.Info coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID, Integer.MAX_VALUE);
		Assert.assertNull(senderBalance.coloredCoins);
		Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID), Long.valueOf(10000L));
		Assert.assertEquals(coloredCoinInfo.moneySupply, Long.valueOf(10000L));

		// try to delete colored coin

		timestamp = mockTimeProvider.get();
		Transaction tx1 = ColoredCoinSupplyBuilder.createNew().validity(timestamp, 60 * 60).build(coloredCoinSigner);
		try {
			ctx.transactionBotService.putTransaction(tx1);
		} catch (Exception e) {

		}
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
		ctx.generateBlockForNow();

		senderBalance = ctx.accountBotService.getBalance(Format.ID.accountId(senderID));
		recepientBalance = ctx.accountBotService.getBalance(Format.ID.accountId(recipientID));
		coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID, Integer.MAX_VALUE);
		Assert.assertNull(senderBalance.coloredCoins);
		Assert.assertEquals(recepientBalance.coloredCoins.get(coloredCoinID), Long.valueOf(10000L));
		Assert.assertEquals(coloredCoinInfo.moneySupply, Long.valueOf(10000L));

		// payment

		timestamp = mockTimeProvider.get();
		Transaction tx3 = ColoredPaymentBuilder.createNew(10000L, senderID, senderID).validity(timestamp, 60 * 60)
				.build(ctx.getSigner());
		ctx.transactionBotService.putTransaction(tx3);

		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
		ctx.generateBlockForNow();

		senderBalance = ctx.accountBotService.getBalance(Format.ID.accountId(senderID));
		recepientBalance = ctx.accountBotService.getBalance(Format.ID.accountId(recipientID));
		coloredCoinInfo = ctx.coloredCoinService.getInfo(coloredCoinID, Integer.MAX_VALUE);
		Assert.assertEquals(senderBalance.coloredCoins.get(coloredCoinID), Long.valueOf(10000L));
		Assert.assertNull(recepientBalance.coloredCoins);
		Assert.assertEquals(coloredCoinInfo.moneySupply, Long.valueOf(10000L));

		// delete colored coin

		timestamp = mockTimeProvider.get();
		Transaction tx4 = ColoredCoinSupplyBuilder.createNew().validity(timestamp, 60 * 60).build(coloredCoinSigner);
		ctx.transactionBotService.putTransaction(tx4);
		Mockito.when(mockTimeProvider.get()).thenReturn(timestamp + 180);
		ctx.generateBlockForNow();

		Assert.assertNull(ctx.accountBotService.getBalance(Format.ID.accountId(senderID)).coloredCoins);
		Assert.assertNull(ctx.accountBotService.getBalance(Format.ID.accountId(recipientID)).coloredCoins);
		Assert.assertNull(ctx.accountBotService.getInformation(Format.ID.accountId(senderID)).coloredCoin);

	}

}
