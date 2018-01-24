package com.exscudo.eon.bot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

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
import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.state.ColoredBalance;
import com.exscudo.peer.eon.state.ColoredCoin;
import com.exscudo.peer.eon.state.GeneratingBalance;
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.transactions.builders.AccountRegistrationBuilder;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import com.exscudo.peer.eon.utils.ColoredCoinId;
import com.exscudo.peer.store.sqlite.Backlog;
import com.exscudo.peer.store.sqlite.Storage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
		Transaction tx = AccountRegistrationBuilder.createNew(newAccount.getPublicKey()).build(sender);
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
		assertNull(balance.coloredCoins);
	}

	@Test
	public void getBalance_for_notexisting_account() throws Exception {
		AccountService.EONBalance balance = service.getBalance(Format.ID.accountId(12345L));
		assertEquals(AccountService.State.Unauthorized, balance.state);
		assertEquals(0, balance.amount);
		assertNull(balance.coloredCoins);
	}

	@Test
	public void getBalance_for_existing_account_with_colored_coins() throws Exception {
		Account account = new Account(12345L);
		AccountProperties.setBalance(account, new Balance(100L));
		ColoredBalance coloredBalance = new ColoredBalance();
		coloredBalance.setBalance(100L, 1L);
		coloredBalance.setBalance(200L, 2L);
		AccountProperties.setColoredBalance(account, coloredBalance);
		ledger.putAccount(account);

		AccountService.EONBalance balance = service.getBalance(Format.ID.accountId(12345L));
		assertEquals(AccountService.State.OK, balance.state);
		assertEquals(100L, balance.amount);
		assertNotNull(balance.coloredCoins);
		assertTrue(balance.coloredCoins.size() == 2);
		assertEquals(balance.coloredCoins.get(ColoredCoinId.convert(1L)), Long.valueOf(100L));
		assertEquals(balance.coloredCoins.get(ColoredCoinId.convert(2L)), Long.valueOf(200L));
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

		AccountService.Info info = service.getInformation(Format.ID.accountId(id));

		assertEquals(AccountService.State.OK, info.state);
		assertEquals(Format.convert(signer.getPublicKey()), info.publicKey);
		assertTrue(1000L == info.deposit);
		assertNull(info.votingRights);
		assertNull(info.quorum);
		assertEquals(AccountService.SignType.Normal, info.signType);
		assertNull(info.coloredCoin);
	}

	@Test
	public void getInformation_for_existing_colored_coin() throws Exception {
		ISigner signer = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");

		long id = Format.MathID.pick(signer.getPublicKey());
		Account account = new Account(id);
		AccountProperties.setRegistrationData(account, new RegistrationData(signer.getPublicKey()));
		ValidationMode validationMode = new ValidationMode();
		validationMode.setBaseWeight(100);
		AccountProperties.setValidationMode(account, validationMode);
		AccountProperties.setDeposit(account, new GeneratingBalance(1000L, 0));
		ColoredCoin coloredCoin = new ColoredCoin();
		coloredCoin.setDecimalPoint(2);
		coloredCoin.setMoneySupply(50000L);
		AccountProperties.setColoredCoinRegistrationData(account, coloredCoin);
		ledger.putAccount(account);

		AccountService.Info info = service.getInformation(Format.ID.accountId(id));

		assertEquals(AccountService.State.OK, info.state);
		assertEquals(Format.convert(signer.getPublicKey()), info.publicKey);
		assertTrue(1000L == info.deposit);
		assertNull(info.votingRights);
		assertNull(info.quorum);
		assertEquals(AccountService.SignType.Normal, info.signType);
		assertEquals(info.coloredCoin, ColoredCoinId.convert(id));
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

		AccountService.Info info = service.getInformation(Format.ID.accountId(id));

		assertEquals(AccountService.State.OK, info.state);
		assertEquals(AccountService.SignType.Public, info.signType);
		assertEquals(seed, info.seed);
		assertEquals(Format.convert(signer.getPublicKey()), info.publicKey);
		assertEquals(0L, info.deposit);
		assertEquals(0L, info.amount);
		assertNull(info.quorum);
		assertNull(info.votingRights.weight);
		assertTrue(info.votingRights.delegates.get(Format.ID.accountId(1L)) == 70);
		assertTrue(info.votingRights.delegates.get(Format.ID.accountId(2L)) == 50);
		assertTrue(info.votingRights.delegates.size() == 2);
		assertNull(info.coloredCoin);
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

		AccountService.Info info = service.getInformation(Format.ID.accountId(id));

		assertEquals(AccountService.State.OK, info.state);
		assertEquals(AccountService.SignType.MFA, info.signType);
		assertEquals(Format.convert(signer.getPublicKey()), info.publicKey);
		assertEquals(0L, info.deposit);
		assertEquals(0L, info.amount);
		assertTrue(info.quorum.quorum == 40);
		assertTrue(info.quorum.quorumByTypes.get(TransactionType.OrdinaryPayment) == 90);
		assertTrue(info.quorum.quorumByTypes.size() == 1);
		assertTrue(info.votingRights.weight == 70);
		assertTrue(info.votingRights.delegates.get(Format.ID.accountId(1L)) == 40);
		assertTrue(info.votingRights.delegates.size() == 1);
		assertNull(info.coloredCoin);
	}

	@Test
	public void getInformation_for_notexisting_account() throws Exception {
		AccountService.Info info = service.getInformation(Format.ID.accountId(12345L));

		assertEquals(AccountService.State.Unauthorized, info.state);
		assertNull(info.publicKey);
		assertEquals(0L, info.deposit);
		assertEquals(0L, info.amount);
		assertNull(info.signType);
		assertNull(info.quorum);
		assertNull(info.votingRights);
		assertNull(info.coloredCoin);
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