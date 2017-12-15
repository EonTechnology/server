package com.exscudo.eon.bot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.state.GeneratingBalance;
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.state.ValidationMode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.mapper.crypto.SignedObjectMapper;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.Account;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.crypto.Ed25519SignatureVerifier;
import com.exscudo.peer.eon.crypto.Ed25519Signer;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.transactions.Registration;
import com.exscudo.peer.eon.transactions.utils.*;
import com.exscudo.peer.store.sqlite.Backlog;
import com.exscudo.peer.store.sqlite.Storage;

public class AccountServiceTest {

	private AccountService service;
	private Ledger ledger;
	private Backlog backlog;

	@Before
	public void setup() throws Exception {
		ledger = new Ledger();

		backlog = new Backlog();

		Storage mockStorage = mock(Storage.class);
		when(mockStorage.getBacklog()).thenReturn(backlog);
		service = Mockito.spy(new AccountService(mockStorage));
		doAnswer(new Answer<IAccount>() {
			@Override
			public IAccount answer(InvocationOnMock invocation) throws Throwable {
				long id = Format.ID.accountId(invocation.getArgument(0));
				return ledger.getAccount(id);
			}
		}).when(service).getAccount(anyString());

		Ed25519SignatureVerifier signatureVerifier = new Ed25519SignatureVerifier();
		CryptoProvider cryptoProvider = new CryptoProvider(new SignedObjectMapper(0L));
		cryptoProvider.addProvider(signatureVerifier);
		cryptoProvider.setDefaultProvider(signatureVerifier.getName());
		CryptoProvider.init(cryptoProvider);
	}

	@Test
	public void getState_for_existing_account_should_return_OK() throws Exception {
		ledger.putAccount(new Account(12345L));

		assertEquals(AccountService.State.OK, service.getState(Format.ID.accountId(12345L)));
	}

	@Test
	public void getState_for_processing_account() throws Exception {
		ISigner sender = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
		ISigner newAccount = new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
		Transaction tx = Registration.newAccount(newAccount.getPublicKey()).build(sender);
		backlog.put(tx);

		long id = Format.MathID.pick(newAccount.getPublicKey());
		assertEquals(AccountService.State.Processing, service.getState(Format.ID.accountId(id)));
	}

	@Test
	public void getState_for_notexisting_account() throws Exception {
		assertEquals(AccountService.State.NotFound, service.getState(Format.ID.accountId(12345L)));
	}

	@Test
	public void getBalance_for_existing_account() throws Exception {
		Account account = new Account(12345L);
		AccountProperties.setBalance(account, new Balance(100L));
		ledger.putAccount(account);

		AccountService.EONBalance balance = service.getBalance(Format.ID.accountId(12345L));
		assertEquals(AccountService.State.OK, balance.state);
		assertEquals(100L, balance.amount);
	}

	@Test
	public void getBalance_for_notexisting_account() throws Exception {
		AccountService.EONBalance balance = service.getBalance(Format.ID.accountId(12345L));
		assertEquals(AccountService.State.Unauthorized, balance.state);
		assertEquals(0, balance.amount);
	}

	@Test
	public void getInformation_for_existing_account() throws Exception {
		ISigner signer = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");

		long id = Format.MathID.pick(signer.getPublicKey());
		Account account = new Account(id);
		AccountProperties.setRegistrationData(account, new RegistrationData(signer.getPublicKey()));
		ValidationMode validationMode = new ValidationMode();
		validationMode.setBaseWeight(100);
		AccountProperties.setValidationMode(account, validationMode);
		AccountProperties.setDeposit(account, new GeneratingBalance(1000L, 0));
		ledger.putAccount(account);

		AccountService.Info balance = service.getInformation(Format.ID.accountId(id));

		assertEquals(AccountService.State.OK, balance.state);
		assertEquals(Format.convert(signer.getPublicKey()), balance.publicKey);
		assertTrue(1000L == balance.deposit);
		assertEquals(null, balance.votingRights);
		assertEquals(null, balance.quorum);
		assertEquals(AccountService.SignType.Normal, balance.signType);
	}

	@Test
	public void getInformation_for_public_account() throws Exception {
		String seed = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
		ISigner signer = new Ed25519Signer(seed);

		long id = Format.MathID.pick(signer.getPublicKey());
		Account account = new Account(id);
		AccountProperties.setRegistrationData(account, new RegistrationData(signer.getPublicKey()));
		ValidationMode validationMode = new ValidationMode();
		validationMode.setPublicMode(seed);
		validationMode.setWeightForAccount(1L, 70);
		validationMode.setWeightForAccount(2L, 50);
		AccountProperties.setValidationMode(account, validationMode);

		ledger.putAccount(account);

		AccountService.Info balance = service.getInformation(Format.ID.accountId(id));

		assertEquals(AccountService.State.OK, balance.state);
		assertEquals(AccountService.SignType.Public, balance.signType);
		assertEquals(seed, balance.seed);
		assertEquals(Format.convert(signer.getPublicKey()), balance.publicKey);
		assertEquals(0L, balance.deposit);
		assertEquals(0L, balance.amount);
		assertEquals(null, balance.quorum);
		assertEquals(null, balance.votingRights.weight);
		assertTrue(balance.votingRights.delegates.get(Format.ID.accountId(1L)) == 70);
		assertTrue(balance.votingRights.delegates.get(Format.ID.accountId(2L)) == 50);
		assertTrue(balance.votingRights.delegates.size() == 2);
	}

	@Test
	public void getInformation_for_mfa_account() throws Exception {
		ISigner signer = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");

		long id = Format.MathID.pick(signer.getPublicKey());
		Account account = new Account(id);
		AccountProperties.setRegistrationData(account, new RegistrationData(signer.getPublicKey()));
		ValidationMode validationMode = new ValidationMode();
		validationMode.setBaseWeight(70);
		validationMode.setWeightForAccount(1L, 40);
		validationMode.setQuorum(40);
		validationMode.setQuorum(TransactionType.OrdinaryPayment, 90);
		AccountProperties.setValidationMode(account, validationMode);

		ledger.putAccount(account);

		AccountService.Info balance = service.getInformation(Format.ID.accountId(id));

		assertEquals(AccountService.State.OK, balance.state);
		assertEquals(AccountService.SignType.MFA, balance.signType);
		assertEquals(Format.convert(signer.getPublicKey()), balance.publicKey);
		assertEquals(0L, balance.deposit);
		assertEquals(0L, balance.amount);
		assertTrue(balance.quorum.quorum == 40);
		assertTrue(balance.quorum.quorumByTypes.get(TransactionType.OrdinaryPayment) == 90);
		assertTrue(balance.quorum.quorumByTypes.size() == 1);
		assertTrue(balance.votingRights.weight == 70);
		assertTrue(balance.votingRights.delegates.get(Format.ID.accountId(1L)) == 40);
		assertTrue(balance.votingRights.delegates.size() == 1);
	}

	@Test
	public void getInformation_for_notexisting_account() throws Exception {
		AccountService.Info balance = service.getInformation(Format.ID.accountId(12345L));

		assertEquals(AccountService.State.Unauthorized, balance.state);
		assertEquals(null, balance.publicKey);
		assertEquals(0L, balance.deposit);
		assertEquals(0L, balance.amount);
		assertEquals(null, balance.signType);
		assertEquals(null, balance.quorum);
		assertEquals(null, balance.votingRights);
	}

	static class Ledger implements ILedger {
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
}