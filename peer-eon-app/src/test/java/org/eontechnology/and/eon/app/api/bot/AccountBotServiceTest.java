package org.eontechnology.and.eon.app.api.bot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eontechnology.and.TestSigner;
import org.eontechnology.and.eon.app.api.DefaultBacklog;
import org.eontechnology.and.peer.core.backlog.IBacklog;
import org.eontechnology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.ledger.LedgerProvider;
import org.eontechnology.and.peer.eon.ledger.AccountProperties;
import org.eontechnology.and.peer.eon.ledger.state.BalanceProperty;
import org.eontechnology.and.peer.eon.ledger.state.ColoredBalanceProperty;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechnology.and.peer.eon.ledger.state.GeneratingBalanceProperty;
import org.eontechnology.and.peer.eon.ledger.state.RegistrationDataProperty;
import org.eontechnology.and.peer.eon.ledger.state.ValidationModeProperty;
import org.eontechnology.and.peer.tx.ColoredCoinID;
import org.eontechnology.and.peer.tx.TransactionType;
import org.eontechnology.and.peer.tx.midleware.builders.RegistrationBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class AccountBotServiceTest {

    private AccountBotService service;
    private ILedger ledger;
    private IBacklog backlog;

    @Before
    public void setup() throws Exception {
        ledger = new Ledger();

        backlog = new DefaultBacklog();
        service = Mockito.spy(new AccountBotService(backlog,
                                                    mock(LedgerProvider.class),
                                                    mock(IBlockchainProvider.class)));

        doAnswer(new Answer<Account>() {
            @Override
            public Account answer(InvocationOnMock invocation) throws Throwable {
                AccountID id = new AccountID(invocation.getArgument(0).toString());
                return ledger.getAccount(id);
            }
        }).when(service).getAccount(anyString());
    }

    @Test
    public void getState_for_existing_account_should_return_OK() throws Exception {
        AccountID id = new AccountID(12345L);
        ledger = ledger.putAccount(new Account(id));

        assertEquals(AccountBotService.State.OK, service.getState(id.toString()));
    }

    @Test
    public void getState_for_processing_account() throws Exception {
        ISigner sender = new TestSigner("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        ISigner newAccount = new TestSigner("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
        Transaction tx = RegistrationBuilder.createNew(newAccount.getPublicKey()).build(new BlockID(0L), sender);
        backlog.put(tx);

        AccountID id = new AccountID(newAccount.getPublicKey());
        assertEquals(AccountBotService.State.Processing, service.getState(id.toString()));
    }

    @Test
    public void getState_for_notexisting_account() throws Exception {
        assertEquals(AccountBotService.State.NotFound, service.getState(new AccountID(12345L).toString()));
    }

    @Test
    public void getBalance_for_existing_account() throws Exception {
        AccountID id = new AccountID(12345L);
        Account account = new Account(id);
        account = AccountProperties.setProperty(account, new BalanceProperty(100L));
        ledger = ledger.putAccount(account);

        AccountBotService.EONBalance balance = service.getBalance(id.toString());
        assertEquals(AccountBotService.State.OK, balance.state);
        assertEquals(100L, balance.amount);
        assertNull(balance.coloredCoins);
    }

    @Test
    public void getBalance_for_notexisting_account() throws Exception {
        AccountBotService.EONBalance balance = service.getBalance(new AccountID(12345L).toString());
        assertEquals(AccountBotService.State.Unauthorized, balance.state);
        assertEquals(0, balance.amount);
        assertNull(balance.coloredCoins);
    }

    @Test
    public void getBalance_for_existing_account_with_colored_coins() throws Exception {
        AccountID id = new AccountID(12345L);
        Account account = new Account(id);
        account = AccountProperties.setProperty(account, new BalanceProperty(100L));
        ColoredBalanceProperty coloredBalance = new ColoredBalanceProperty();
        coloredBalance.setBalance(100L, new ColoredCoinID(1L));
        coloredBalance.setBalance(200L, new ColoredCoinID(2L));
        account = AccountProperties.setProperty(account, coloredBalance);
        ledger = ledger.putAccount(account);

        AccountBotService.EONBalance balance = service.getBalance(id.toString());
        assertEquals(AccountBotService.State.OK, balance.state);
        assertEquals(100L, balance.amount);
        assertNotNull(balance.coloredCoins);
        assertTrue(balance.coloredCoins.size() == 2);
        assertEquals(balance.coloredCoins.get(new ColoredCoinID(1L).toString()), Long.valueOf(100L));
        assertEquals(balance.coloredCoins.get(new ColoredCoinID(2L).toString()), Long.valueOf(200L));
    }

    @Test
    public void getInformation_for_existing_account() throws Exception {
        ISigner signer = new TestSigner("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");

        AccountID id = new AccountID(signer.getPublicKey());
        Account account = new Account(id);
        account = AccountProperties.setProperty(account, new RegistrationDataProperty(signer.getPublicKey()));
        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(100);
        account = AccountProperties.setProperty(account, validationMode);
        account = AccountProperties.setProperty(account, new GeneratingBalanceProperty(1000L, 0));
        ledger = ledger.putAccount(account);

        AccountBotService.Info info = service.getInformation(id.toString());

        assertEquals(AccountBotService.State.OK, info.state);
        Assert.assertEquals(Format.convert(signer.getPublicKey()), info.publicKey);
        assertTrue(1000L == info.deposit);
        assertNull(info.votingRights);
        assertNull(info.quorum);
        assertEquals(AccountBotService.SignType.Normal, info.signType);
        assertNull(info.coloredCoin);
    }

    @Test
    public void getInformation_for_existing_colored_coin() throws Exception {
        ISigner signer = new TestSigner("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");

        AccountID id = new AccountID(signer.getPublicKey());
        Account account = new Account(id);
        account = AccountProperties.setProperty(account, new RegistrationDataProperty(signer.getPublicKey()));
        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(100);
        account = AccountProperties.setProperty(account, validationMode);
        account = AccountProperties.setProperty(account, new GeneratingBalanceProperty(1000L, 0));
        ColoredCoinProperty coloredCoin = new ColoredCoinProperty();
        coloredCoin.setEmitMode(ColoredCoinEmitMode.PRESET);
        coloredCoin.setAttributes(new ColoredCoinProperty.Attributes(2, 0));
        coloredCoin.setMoneySupply(50000L);
        account = AccountProperties.setProperty(account, coloredCoin);
        ledger = ledger.putAccount(account);

        AccountBotService.Info info = service.getInformation(id.toString());

        assertEquals(AccountBotService.State.OK, info.state);
        assertEquals(Format.convert(signer.getPublicKey()), info.publicKey);
        assertTrue(1000L == info.deposit);
        assertNull(info.votingRights);
        assertNull(info.quorum);
        assertEquals(AccountBotService.SignType.Normal, info.signType);
        Assert.assertEquals(info.coloredCoin, new ColoredCoinID(id).toString());
    }

    @Test
    public void getInformation_for_public_account() throws Exception {
        String seed = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
        ISigner signer = new TestSigner(seed);

        AccountID id = new AccountID(signer.getPublicKey());
        Account account = new Account(id);
        account = AccountProperties.setProperty(account, new RegistrationDataProperty(signer.getPublicKey()));
        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setPublicMode(seed);
        validationMode.setWeightForAccount(new AccountID(1L), 70);
        validationMode.setWeightForAccount(new AccountID(2L), 50);
        validationMode.setTimestamp(0);
        account = AccountProperties.setProperty(account, validationMode);

        ledger = ledger.putAccount(account);

        AccountBotService.Info info = service.getInformation(id.toString());

        assertEquals(AccountBotService.State.OK, info.state);
        assertEquals(AccountBotService.SignType.Public, info.signType);
        assertEquals(seed, info.seed);
        assertEquals(Format.convert(signer.getPublicKey()), info.publicKey);
        assertEquals(0L, info.deposit);
        assertEquals(0L, info.amount);
        assertNull(info.quorum);
        assertNull(info.votingRights.weight);
        assertTrue(info.votingRights.delegates.get(new AccountID(1L).toString()) == 70);
        assertTrue(info.votingRights.delegates.get(new AccountID(2L).toString()) == 50);
        assertTrue(info.votingRights.delegates.size() == 2);
        assertNull(info.coloredCoin);
    }

    @Test
    public void getInformation_for_mfa_account() throws Exception {
        ISigner signer = new TestSigner("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");

        AccountID id = new AccountID(signer.getPublicKey());
        Account account = new Account(id);
        account = AccountProperties.setProperty(account, new RegistrationDataProperty(signer.getPublicKey()));
        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(70);
        validationMode.setWeightForAccount(new AccountID(1L), 40);
        validationMode.setQuorum(40);
        validationMode.setQuorum(TransactionType.Payment, 90);
        validationMode.setTimestamp(0);
        account = AccountProperties.setProperty(account, validationMode);

        ledger = ledger.putAccount(account);

        AccountBotService.Info info = service.getInformation(id.toString());

        assertEquals(AccountBotService.State.OK, info.state);
        assertEquals(AccountBotService.SignType.MFA, info.signType);
        assertEquals(Format.convert(signer.getPublicKey()), info.publicKey);
        assertEquals(0L, info.deposit);
        assertEquals(0L, info.amount);
        assertTrue(info.quorum.quorum == 40);
        assertTrue(info.quorum.quorumByTypes.get(TransactionType.Payment) == 90);
        assertTrue(info.quorum.quorumByTypes.size() == 1);
        assertTrue(info.votingRights.weight == 70);
        assertTrue(info.votingRights.delegates.get(new AccountID(1L).toString()) == 40);
        assertTrue(info.votingRights.delegates.size() == 1);
        assertNull(info.coloredCoin);
    }

    @Test
    public void getInformation_for_notexisting_account() throws Exception {
        AccountBotService.Info info = service.getInformation(new AccountID(12345L).toString());

        assertEquals(AccountBotService.State.Unauthorized, info.state);
        assertNull(info.publicKey);
        assertEquals(0L, info.deposit);
        assertEquals(0L, info.amount);
        assertNull(info.signType);
        assertNull(info.quorum);
        assertNull(info.votingRights);
        assertNull(info.coloredCoin);
    }

    static class Ledger implements ILedger {
        private Map<AccountID, Account> accounts = new HashMap<>();

        public Ledger() {
        }

        private Ledger(Map<AccountID, Account> accounts) {
            this.accounts = accounts;
        }

        @Override
        public Account getAccount(AccountID accountID) {
            return accounts.get(accountID);
        }

        @Override
        public ILedger putAccount(Account account) {
            HashMap<AccountID, Account> newAccounts = new HashMap<>(accounts);
            newAccounts.put(account.getID(), account);
            return spy(new Ledger(newAccounts));
        }

        @Override
        public String getHash() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save() {
        }

        @Override
        public Iterator<Account> iterator() {
            return accounts.values().iterator();
        }
    }
}