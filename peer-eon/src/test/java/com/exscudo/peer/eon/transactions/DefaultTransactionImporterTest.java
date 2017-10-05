package com.exscudo.peer.eon.transactions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.exscudo.peer.DefaultBacklog;
import com.exscudo.peer.MockSigner;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.ForkProvider;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.exceptions.LifecycleException;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.*;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.*;
import com.exscudo.peer.eon.transactions.handlers.AccountRegistrationHandler;
import com.exscudo.peer.eon.transactions.handlers.DepositRefillHandler;
import com.exscudo.peer.eon.transactions.handlers.DepositWithdrawHandler;
import com.exscudo.peer.eon.transactions.handlers.OrdinaryPaymentHandler;
import com.exscudo.peer.eon.transactions.utils.AccountAttributes;
import com.exscudo.peer.eon.transactions.utils.AccountBalance;
import com.exscudo.peer.eon.transactions.utils.AccountDeposit;

public class DefaultTransactionImporterTest {
	private TimeProvider timeProvider = new TimeProvider();

	private ILedger ledger;
	private DefaultAccount signerAccount;
	private MockSigner signer = new MockSigner();

	private IBacklogService backlogBuffer;
	private IBacklogService importer;
	private IBlockchainService blockchain;

	@Before
	public void setUp() throws Exception {

		ITransactionHandler txHandler = new TransactionHandlerDecorator() {
			{
				bind(TransactionType.AccountRegistration, new AccountRegistrationHandler());
				bind(TransactionType.OrdinaryPayment, new OrdinaryPaymentHandler());
				bind(TransactionType.DepositWithdraw, new DepositWithdrawHandler());
				bind(TransactionType.DepositRefill, new DepositRefillHandler());
			}
		};

		ledger = spy(new DefaultLedger());
		DefaultAccount da = (DefaultAccount) ledger.newAccount(Format.MathID.pick(signer.getPublicKey()));
		AccountAttributes.setPublicKey(da, signer.getPublicKey());
		AccountBalance.refill(da, 1000L);
		signerAccount = spy(da);

		ISandbox sandbox = new DefaultSandbox(txHandler, ledger);

		LinkedBlock linkedBlock = mock(LinkedBlock.class);
		when(linkedBlock.createSandbox(any())).thenReturn(sandbox);
		when(linkedBlock.getState()).thenReturn(ledger);
		when(linkedBlock.getTimestamp()).thenReturn(timeProvider.get());

		blockchain = mock(IBlockchainService.class);
		when(blockchain.getLastBlock()).thenReturn(linkedBlock);
		when(blockchain.transactionMapper()).thenReturn(mock(ITransactionMapper.class));

		backlogBuffer = spy(new DefaultBacklog());

		importer = new BacklogDecorator(backlogBuffer, blockchain, txHandler);

		CryptoProvider.getInstance().addProvider(signer);
		CryptoProvider.getInstance().setDefaultProvider("test");

		Fork mockFork = mock(Fork.class);
		when(mockFork.isPassed(anyInt())).thenReturn(false);
		when(mockFork.getGenesisBlockID()).thenReturn(12345L);
		ForkProvider.init(mockFork);

	}

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void exist_unconfirmed_transaction() throws Exception {
		when(backlogBuffer.contains(anyLong())).thenReturn(true);

		DefaultAccount da = spy(DefaultAccount.create(new byte[32]));
		da.setBalance(1000L);

		when(ledger.getAccount(anyLong())).thenReturn(da);
		when(ledger.getAccount(Format.MathID.pick(new byte[32]))).thenReturn(null);
		when(ledger.getAccount(Format.MathID.pick(signer.getPublicKey()))).thenReturn(signerAccount);

		Transaction tx = Registration.newAccount(new byte[32]).validity(timeProvider.get(), (short) 60).forFee(1L)
				.build(signer);
		assertFalse(importer.put(tx));
	}

	@Test
	public void exist_confirmed_transaction() throws Exception {

		when(blockchain.transactionMapper().containsTransaction(anyLong())).thenReturn(true);

		DefaultAccount da = spy(DefaultAccount.create(new byte[32]));
		da.setBalance(1000L);

		when(ledger.getAccount(anyLong())).thenReturn(da);
		when(ledger.getAccount(Format.MathID.pick(new byte[32]))).thenReturn(null);
		when(ledger.getAccount(Format.MathID.pick(signer.getPublicKey()))).thenReturn(signerAccount);

		Transaction tx = Registration.newAccount(new byte[32]).validity(timeProvider.get(), (short) 60).forFee(1L)
				.build(signer);
		assertTrue(importer.put(tx));
	}

	@Test()
	public void put_transaction() throws Exception {

		when(blockchain.transactionMapper().containsTransaction(anyLong())).thenReturn(false);

		DefaultAccount da = spy(DefaultAccount.create(new byte[32]));
		da.setBalance(1000L);

		when(ledger.getAccount(anyLong())).thenReturn(da);
		when(ledger.getAccount(Format.MathID.pick(new byte[32]))).thenReturn(null);
		when(ledger.getAccount(Format.MathID.pick(signer.getPublicKey()))).thenReturn(signerAccount);

		Transaction tx = Registration.newAccount(new byte[32]).validity(timeProvider.get(), (short) 60).forFee(1L)
				.build(signer);
		assertTrue(importer.put(tx));

	}

	//
	// Common properties
	//

	@Test
	public void unknown_transaction_type() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Invalid transaction type.");
		IAccount mockAccount = mock(IAccount.class);
		when(ledger.getAccount(anyLong())).thenReturn(mockAccount);

		Transaction tx = new Transaction();
		tx.setType(10);
		tx.setTimestamp(0);
		tx.setDeadline((short) 1);
		tx.setReference(0);
		tx.setSenderID(12345L);
		tx.setFee(0);
		tx.setData(new HashMap<>());
		tx.setSignature(new byte[0]);

		tx.setSignature(signer.sign(tx.getBytes()));
		importer.put(tx);
	}

	@Test
	public void expired() throws Exception {
		expectedException.expect(LifecycleException.class);
		IAccount mockAccount = mock(IAccount.class);
		when(ledger.getAccount(anyLong())).thenReturn(mockAccount);

		Transaction tx = Registration.newAccount(new byte[0]).validity(timeProvider.get() - 61 * 60, (short) 60)
				.build(signer);
		importer.put(tx);
	}

	@Test
	public void invalid_deadline() throws Exception {
		expectedException.expect(LifecycleException.class);
		IAccount mockAccount = mock(IAccount.class);
		when(ledger.getAccount(anyLong())).thenReturn(mockAccount);

		Transaction tx = Registration.newAccount(new byte[0]).validity(timeProvider.get(), (short) 0).build(signer);
		importer.put(tx);
	}

	@Test
	public void invalid_deadline_() throws Exception {
		expectedException.expect(LifecycleException.class);
		IAccount mockAccount = mock(IAccount.class);
		when(ledger.getAccount(anyLong())).thenReturn(mockAccount);

		Transaction tx = Registration.newAccount(new byte[0])
				.validity(timeProvider.get(), (short) (EonConstant.TRANSACTION_MAX_LIFETIME + 1)).build(signer);
		importer.put(tx);
	}

	@Test
	public void invalid_signature() throws Exception {
		expectedException.expect(IllegalSignatureException.class);

		DefaultAccount da = spy(DefaultAccount.create(new byte[32]));
		da.setBalance(1000L);
		when(ledger.getAccount(anyLong())).thenReturn(da);

		Transaction tx = spy(
				Registration.newAccount(new byte[0]).validity(timeProvider.get(), (short) 60).forFee(1L).build(signer));
		when(tx.getSenderID()).thenReturn(12345L);
		importer.put(tx);
	}

	//
	// IAccount registration
	//

	@Test
	public void account_invalid_fee() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Invalid fee.");

		Transaction tx = spy(Registration.newAccount(new byte[32]).validity(timeProvider.get(), (short) 60).forFee(0L)
				.build(signer));

		importer.put(tx);
	}

	@Test
	public void account_re_registration() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("IAccount can not be to created.");

		Transaction tx = Registration.newAccount(signer.getPublicKey()).validity(timeProvider.get(), (short) 60)
				.forFee(1L).build(signer);

		DefaultAccount da = spy(DefaultAccount.create(new byte[32]));
		da.setBalance(1000L);
		when(ledger.getAccount(eq(tx.getSenderID()))).thenReturn(da);

		importer.put(tx);
	}

	@Test
	public void account_invalid_attachment() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		Transaction tx = spy(
				Registration.newAccount(new byte[0]).validity(timeProvider.get(), (short) 60).forFee(1L).build(signer));

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("data", "test");
		when(tx.getData()).thenReturn(map);

		updateSignature(tx);

		importer.put(tx);
	}

	//
	// Ordinary payments
	//

	@Test
	public void payment_invalid_sender() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown sender.");

		Transaction tx = Payment.newPayment(100L).forFee(1L).to(12345L).validity(timeProvider.get(), (short) 60)
				.build(signer);

		when(ledger.getAccount(eq(tx.getSenderID()))).thenReturn(null);

		importer.put(tx);
	}

	@Test
	public void payment_invalid_recipient() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown recipient.");

		Transaction tx = Payment.newPayment(100L).forFee(1L).to(12345L).validity(timeProvider.get(), (short) 60)
				.build(signer);

		importer.put(tx);
	}

	@Test
	public void payment_invalid_attachment() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		Transaction tx = spy(
				Payment.newPayment(100L).forFee(1L).to(12345L).validity(timeProvider.get(), (short) 60).build(signer));

		when(tx.getData()).thenReturn(new HashMap<>());

		updateSignature(tx);

		importer.put(tx);
	}

	@Test
	public void payment_invalid_fee() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Invalid fee.");

		Transaction tx = new Transaction();
		tx.setType(TransactionType.OrdinaryPayment);
		tx.setTimestamp(timeProvider.get());
		tx.setDeadline((short) 60);
		tx.setReference(0);
		tx.setSenderID(Format.MathID.pick(signer.getPublicKey()));
		tx.setFee(0L);

		HashMap<String, Object> hashMap = new HashMap<>();
		hashMap.put("amount", 1L);
		hashMap.put("recipient", Format.ID.accountId(12345L));
		tx.setData(hashMap);

		byte[] bytes = tx.getBytes();
		byte[] signature = signer.sign(bytes);
		tx.setSignature(signature);

		when(ledger.getAccount(eq(12345L))).thenReturn(signerAccount);

		importer.put(tx);
	}

	@Test
	public void payment_invalid_balance() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Not enough funds.");

		long recipientID = 12345L;
		Transaction tx = Payment.newPayment(100L).forFee(5L).to(recipientID).validity(timeProvider.get(), (short) 60)
				.build(signer);

		DefaultAccount mockSender = spy(DefaultAccount.create(signer.getPublicKey()));
		mockSender.setBalance(103L);

		IAccount mockRecipient = mock(IAccount.class);

		when(ledger.getAccount(eq(tx.getSenderID()))).thenReturn(mockSender);
		when(ledger.getAccount(eq(recipientID))).thenReturn(mockRecipient);

		importer.put(tx);
	}

	//
	// Deposit refill
	//

	@Test
	public void deposit_refill_with_invalid_fee_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The field value Fee is not valid.");

		Transaction tx = spy(Deposit.refill(999L).validity(timeProvider.get(), (short) 60).build(signer));

		when(tx.getFee()).thenReturn(5L);

		updateSignature(tx);

		importer.put(tx);
	}

	@Test
	public void deposit_refill_with_arbitrary_amount_is_ok() throws Exception {

		when(blockchain.transactionMapper().containsTransaction(anyLong())).thenReturn(true);

		long refillAmount = 900;
		long balanceAmount = refillAmount + Deposit.DEPOSIT_TRANSACTION_FEE;
		long depositAmount = 0;

		Transaction tx = Deposit.refill(refillAmount).validity(timeProvider.get(), (short) 60).build(signer);

		signerAccount.setDeposit(depositAmount, 1);
		signerAccount.setBalance(balanceAmount);

		importer.put(tx);

	}

	@Test
	public void deposit_refill_with_invalid_attachment_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		Transaction tx = spy(Deposit.refill(999L).validity(timeProvider.get(), (short) 60).build(signer));

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("amount", "test");
		when(tx.getData()).thenReturn(map);

		updateSignature(tx);

		importer.put(tx);
	}

	@Test
	public void deposit_refill_with_positive_deposit_is_ok() throws Exception {

		when(blockchain.transactionMapper().containsTransaction(anyLong())).thenReturn(true);

		long refillAmount = 100500;
		long balanceAmount = refillAmount + Deposit.DEPOSIT_TRANSACTION_FEE;
		long depositAmount = 1;

		Transaction tx = Deposit.refill(refillAmount).validity(timeProvider.get(), (short) 60).build(signer);

		signerAccount.setDeposit(depositAmount, 0);
		signerAccount.setBalance(balanceAmount);

		importer.put(tx);

	}

	@Test
	public void deposit_refill_with_unknown_sender_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown sender.");

		Transaction tx = Deposit.refill(1L).validity(timeProvider.get(), (short) 60).build(signer);
		when(ledger.getAccount(eq(tx.getSenderID()))).thenReturn(null);
		importer.put(tx);
	}

	@Test
	public void deposit_refill_with_low_balance_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Not enough funds.");

		long depositAmount = 999L;
		Transaction tx = Deposit.refill(depositAmount).validity(timeProvider.get(), (short) 60).build(signer);

		signerAccount.setDeposit(0, 0);
		signerAccount.setBalance(depositAmount + Deposit.DEPOSIT_TRANSACTION_FEE - 1);

		importer.put(tx);
	}

	//
	// Deposit withdraw
	//

	@Test
	public void deposit_withdraw_with_invalid_fee_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The field value Fee is not valid.");

		Transaction tx = spy(Deposit.withdraw(100500L).validity(timeProvider.get(), (short) 60).build(signer));

		when(tx.getFee()).thenReturn(5L);

		updateSignature(tx);

		importer.put(tx);
	}

	@Test
	public void deposit_withdraw_with_invalid_attachment_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		Transaction tx = spy(Deposit.withdraw(100500L).validity(timeProvider.get(), (short) 60).build(signer));

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("date", "test");
		when(tx.getData()).thenReturn(map);

		updateSignature(tx);

		importer.put(tx);
	}

	@Test
	public void deposit_withdraw_with_unknown_sender_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown sender.");

		Transaction tx = Deposit.withdraw(100500L).validity(timeProvider.get(), (short) 60).build(signer);
		when(ledger.getAccount(eq(tx.getSenderID()))).thenReturn(null);
		importer.put(tx);
	}

	@Test
	public void deposit_withdraw_with_low_deposit_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Not enough funds on deposit.");

		long depositAmount = 2;
		long withdrawAmount = 3;
		long balanceAmount = withdrawAmount + Deposit.DEPOSIT_TRANSACTION_FEE + 1;

		Transaction tx = Deposit.withdraw(withdrawAmount).validity(timeProvider.get(), (short) 60).build(signer);

		signerAccount.setDeposit(depositAmount, 1);
		signerAccount.setBalance(balanceAmount);

		importer.put(tx);
	}

	@Test
	public void deposit_withdraw_with_low_balance_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Not enough funds.");

		Transaction tx = Deposit.withdraw(1000L).validity(timeProvider.get(), (short) 60).build(signer);

		signerAccount.setDeposit(1000L, 0);
		signerAccount.setBalance(Deposit.DEPOSIT_TRANSACTION_FEE - 1);

		importer.put(tx);
	}

	private void updateSignature(Transaction tx) {
		byte[] bytes = tx.getBytes();
		byte[] signature = signer.sign(bytes);
		tx.setSignature(signature);
	}

	//
	// Conditions
	//
	private long recipientID = 12345L;

	@Test
	public void payment() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Not enough funds.");

		Transaction payment = Payment.newPayment(50L).forFee(1L).to(recipientID)
				.validity(timeProvider.get(), (short) 60).build(signer);
		backlogBuffer.put(payment);

		Transaction tx = Payment.newPayment(70L).forFee(3L).to(recipientID).validity(timeProvider.get(), (short) 60)
				.build(signer);

		signerAccount.setBalance(100L);
		when(ledger.getAccount(eq(recipientID))).thenReturn(mock(IAccount.class));

		importer.put(tx);
	}

	@Test
	public void payment_after_open_deposit() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Not enough funds.");

		long depositAmount = 999;
		Transaction deposit = Deposit.refill(depositAmount).validity(timeProvider.get(), (short) 60).build(signer);
		backlogBuffer.put(deposit);

		Transaction tx = Payment.newPayment(70L).forFee(3L).to(12345L).validity(timeProvider.get(), (short) 60)
				.build(signer);

		signerAccount.setDeposit(0L, 0);
		signerAccount.setBalance(depositAmount + Deposit.DEPOSIT_TRANSACTION_FEE + 60L);

		when(ledger.getAccount(eq(recipientID))).thenReturn(mock(IAccount.class));

		importer.put(tx);
	}

	@Test
	public void payment_after_close_deposit() throws Exception {

		long depositAmount = 999;
		Transaction deposit = Deposit.withdraw(depositAmount).validity(timeProvider.get(), (short) 60).build(signer);
		backlogBuffer.put(deposit);

		Transaction tx = Payment.newPayment(70L).forFee(3L).to(12345L).validity(timeProvider.get(), (short) 60)
				.build(signer);

		signerAccount.setDeposit(1000L, 0);
		signerAccount.setBalance(14L);
		when(ledger.getAccount(eq(recipientID))).thenReturn(mock(IAccount.class));

		assertTrue(importer.put(tx));
	}

	private static class DefaultAccount implements IAccount {

		private long id;
		private Map<UUID, AccountProperty> properties = new HashMap<>();

		public DefaultAccount(long id) {
			this.id = id;
		}

		public static DefaultAccount create(byte[] publicKey) {
			DefaultAccount account = new DefaultAccount(Format.MathID.pick(publicKey));
			AccountAttributes.setPublicKey(account, publicKey);
			return account;
		}

		void setDeposit(long value, int height) {
			properties.put(AccountDeposit.ID, new AccountDeposit(id, value, height).asProperty());
		}

		void setBalance(long value) {
			properties.put(AccountBalance.ID, new AccountBalance(id, value).asProperty());
		}

		@Override
		public long getID() {
			return id;
		}

		@Override
		public AccountProperty getProperty(UUID id) {
			return properties.get(id);
		}

		@Override
		public void putProperty(AccountProperty property) {
			properties.put(property.getType(), property);
		}

		@Override
		public Collection<? extends AccountProperty> getProperties() {
			return properties.values();
		}

	}

	private static class DefaultSandbox extends AbstractSandbox {
		private final ILedger ledger;

		public DefaultSandbox(ITransactionHandler txHandler, ILedger ledger) {
			super(txHandler);
			this.ledger = ledger;
		}

		@Override
		public AccountProperty[] getProperties() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected ILedger getLedger() {
			return ledger;
		}

	}

	private static class DefaultLedger implements ILedger {
		private Map<Long, DefaultAccount> accounts = new HashMap<>();
		{
			accounts.put(Constant.DUMMY_ACCOUNT_ID, new DefaultAccount(Format.MathID.pick(new byte[32])));
		}

		@Override
		public IAccount getAccount(long accountID) {
			DefaultAccount impl = accounts.get(accountID);
			if (impl != null) {
				return impl;
			}
			return null;
		}

		@Override
		public IAccount newAccount(long id) {
			DefaultAccount account = new DefaultAccount(id);
			accounts.put(account.id, account);
			return account;
		}

	}

}
