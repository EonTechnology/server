package com.exscudo.peer.eon.transactions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.exscudo.peer.DefaultBacklog;
import com.exscudo.peer.MockSigner;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.mapper.crypto.SignedObjectMapper;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.*;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.*;
import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.state.GeneratingBalance;
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.transactions.handlers.*;
import com.exscudo.peer.eon.transactions.rules.*;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class DefaultTransactionImporterTest {
	private TimeProvider timeProvider = new TimeProvider();

	private ILedger ledger;
	private IAccount signerAccount;
	private MockSigner signer = new MockSigner();

	private IBacklogService backlogBuffer;
	private IBacklogService importer;
	private IBlockchainService blockchain;

	@Before
	public void setUp() throws Exception {

		ledger = spy(new DefaultLedger());
		IAccount da = new Account(Format.MathID.pick(signer.getPublicKey()));
		AccountProperties.setPublicKey(da, signer.getPublicKey());
		AccountProperties.balanceRefill(da, 1000L);
		ledger.putAccount(da);
		signerAccount = spy(da);

		Block block = mock(Block.class);
		when(block.getTimestamp()).thenReturn(timeProvider.get());

		blockchain = mock(IBlockchainService.class);
		when(blockchain.getLastBlock()).thenReturn(block);
		when(blockchain.transactionMapper()).thenReturn(mock(ITransactionMapper.class));
		when(blockchain.getState(any())).thenReturn(ledger);

		backlogBuffer = spy(new DefaultBacklog());

		CryptoProvider cryptoProvider = new CryptoProvider(new SignedObjectMapper(12345L));
		cryptoProvider.addProvider(signer);
		cryptoProvider.setDefaultProvider("test");
		CryptoProvider.init(cryptoProvider);

		IFork mockFork = mock(IFork.class);
		when(mockFork.isPassed(anyInt())).thenReturn(false);
		when(mockFork.getTransactionExecutor(anyInt())).thenReturn(new DefaultTransactionHandler());
		importer = new BacklogDecorator(backlogBuffer, blockchain, mockFork);

	}

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void exist_unconfirmed_transaction() throws Exception {
		when(backlogBuffer.contains(anyLong())).thenReturn(true);

		IAccount da = new Account(Format.MathID.pick(new byte[32]));
		AccountProperties.setRegistrationData(da, new RegistrationData(new byte[32]));
		AccountProperties.setBalance(da, new Balance(1000L));

		when(ledger.getAccount(anyLong())).thenReturn(da);
		when(ledger.getAccount(Format.MathID.pick(new byte[32]))).thenReturn(null);
		when(ledger.getAccount(Format.MathID.pick(signer.getPublicKey()))).thenReturn(signerAccount);

		Transaction tx = Registration.newAccount(new byte[32]).validity(timeProvider.get(), (short) 60, 1).forFee(1L)
				.build(signer);
		assertFalse(importer.put(tx));
	}

	@Test
	public void exist_confirmed_transaction() throws Exception {

		when(blockchain.transactionMapper().containsTransaction(anyLong())).thenReturn(true);

		IAccount da = new Account(Format.MathID.pick(new byte[32]));
		AccountProperties.setRegistrationData(da, new RegistrationData(new byte[32]));
		AccountProperties.setBalance(da, new Balance(1000L));

		when(ledger.getAccount(anyLong())).thenReturn(da);
		when(ledger.getAccount(Format.MathID.pick(new byte[32]))).thenReturn(null);
		when(ledger.getAccount(Format.MathID.pick(signer.getPublicKey()))).thenReturn(signerAccount);

		Transaction tx = Registration.newAccount(new byte[32]).validity(timeProvider.get(), (short) 60, 1).forFee(1L)
				.build(signer);
		assertTrue(importer.put(tx));
	}

	@Test()
	public void put_transaction() throws Exception {

		when(blockchain.transactionMapper().containsTransaction(anyLong())).thenReturn(false);

		IAccount da = new Account(Format.MathID.pick(new byte[32]));
		AccountProperties.setRegistrationData(da, new RegistrationData(new byte[32]));
		AccountProperties.setBalance(da, new Balance(1000L));

		when(ledger.getAccount(anyLong())).thenReturn(da);
		when(ledger.getAccount(Format.MathID.pick(new byte[32]))).thenReturn(null);
		when(ledger.getAccount(Format.MathID.pick(signer.getPublicKey()))).thenReturn(signerAccount);

		Transaction tx = Registration.newAccount(new byte[32]).validity(timeProvider.get(), (short) 60, 1).forFee(1L)
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

		Transaction tx = new Transaction();
		tx.setType(10);
		tx.setVersion(1);
		tx.setTimestamp(timeProvider.get());
		tx.setDeadline((short) 60);
		tx.setReference(0);
		tx.setSenderID(Format.MathID.pick(signer.getPublicKey()));
		tx.setFee(1L);
		tx.setData(new HashMap<>());
		tx.setSignature(new byte[0]);

		tx.setSignature(signer.sign(tx.getBytes()));
		importer.put(tx);
	}

	//
	// Conditions
	//
	private long recipientID = 12345L;

	@Test
	public void payment() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Not enough funds.");

		Transaction payment = Payment.newPayment(50L, recipientID).forFee(1L)
				.validity(timeProvider.get(), (short) 60, 1).build(signer);
		backlogBuffer.put(payment);

		Transaction tx = Payment.newPayment(70L, recipientID).forFee(3L).validity(timeProvider.get(), (short) 60, 1)
				.build(signer);

		AccountProperties.setBalance(signerAccount, new Balance(100L));
		when(ledger.getAccount(eq(recipientID))).thenReturn(mock(IAccount.class));

		importer.put(tx);
	}

	@Test
	public void payment_after_open_deposit() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Not enough funds.");

		long depositAmount = 999;
		Transaction deposit = Deposit.refill(depositAmount).validity(timeProvider.get(), (short) 60, 1).build(signer);
		backlogBuffer.put(deposit);

		Transaction tx = Payment.newPayment(70L, 12345L).forFee(3L).validity(timeProvider.get(), (short) 60, 1)
				.build(signer);

		AccountProperties.setBalance(signerAccount, new Balance(depositAmount + Deposit.DEPOSIT_TRANSACTION_FEE + 60L));
		AccountProperties.setDeposit(signerAccount, new GeneratingBalance(0L, 0));

		when(ledger.getAccount(eq(recipientID))).thenReturn(mock(IAccount.class));

		importer.put(tx);
	}

	@Test
	public void payment_after_close_deposit() throws Exception {

		long depositAmount = 999;
		Transaction deposit = Deposit.withdraw(depositAmount).validity(timeProvider.get(), (short) 60, 1).build(signer);
		backlogBuffer.put(deposit);

		Transaction tx = Payment.newPayment(70L, 12345L).forFee(3L).validity(timeProvider.get(), (short) 60, 1)
				.build(signer);

		AccountProperties.setBalance(signerAccount, new Balance(14L));
		AccountProperties.setDeposit(signerAccount, new GeneratingBalance(1000L, 0));
		when(ledger.getAccount(eq(recipientID))).thenReturn(mock(IAccount.class));

		assertTrue(importer.put(tx));
	}

	private static class DefaultLedger implements ILedger {
		private Map<Long, IAccount> accounts = new HashMap<>();

		@Override
		public IAccount getAccount(long accountID) {
			return accounts.get(accountID);
		}

		@Override
		public void putAccount(IAccount account) {
			accounts.put(account.getID(), account);
		}

		@Override
		public byte[] getHash() {
			throw new UnsupportedOperationException();
		}

	}

	public class DefaultTransactionHandler extends TransactionHandler {

		public DefaultTransactionHandler() {
			super(new HashMap<Integer, ITransactionHandler>() {
				private static final long serialVersionUID = 1L;

				{
					put(TransactionType.AccountRegistration, new AccountRegistrationHandler());
					put(TransactionType.OrdinaryPayment, new OrdinaryPaymentHandler());
					put(TransactionType.DepositWithdraw, new DepositWithdrawHandler());
					put(TransactionType.DepositRefill, new DepositRefillHandler());
					put(TransactionType.Delegate, new DelegateHandler());
					put(TransactionType.Quorum, new QuorumHandler());
					put(TransactionType.Rejection, new RejectionHandler());
				}
			}, new IValidationRule[]{new BaseValidationRuleV1_2(), new SenderValidationRule(),
					new ConfirmationsValidationRule(),
					new AttachmentValidationRule(new HashMap<Integer, IValidationRule>() {
						private static final long serialVersionUID = 1L;

						{
							put(TransactionType.AccountRegistration, new AccountRegistrationValidationRule());
							put(TransactionType.OrdinaryPayment, new OrdinaryPaymentValidationRule());
							put(TransactionType.DepositWithdraw, new DepositWithdrawValidationRule());
							put(TransactionType.DepositRefill, new DepositRefillValidationRule());
							put(TransactionType.Delegate, new DelegateValidationRule());
							put(TransactionType.Quorum, new QuorumValidationRule());
							put(TransactionType.Rejection, new RejectionValidationRule());
						}
					})});
		}

	}

}
