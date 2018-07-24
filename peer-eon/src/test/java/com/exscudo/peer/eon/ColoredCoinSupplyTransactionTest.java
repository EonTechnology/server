package com.exscudo.peer.eon;

import java.util.HashMap;

import com.exscudo.peer.Signer;
import com.exscudo.peer.TestAccount;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinSupplyParser;
import com.exscudo.peer.tx.ColoredCoinID;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.ColoredCoinSupplyBuilder;
import com.exscudo.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ColoredCoinSupplyTransactionTest extends AbstractTransactionTest {
    private static final String SENDER = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    private ColoredCoinSupplyParser parser = new ColoredCoinSupplyParser();
    private ISigner sender;
    private Account senderAccount;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        sender = new Signer(SENDER);

        senderAccount = Mockito.spy(new TestAccount(new AccountID(sender.getPublicKey())));
        AccountProperties.setProperty(senderAccount, new RegistrationDataProperty(sender.getPublicKey()));
        AccountProperties.setProperty(senderAccount, new BalanceProperty(100L));
        ColoredCoinProperty coloredCoin = new ColoredCoinProperty();
        coloredCoin.setMoneySupply(50000L);
        AccountProperties.setProperty(senderAccount, coloredCoin);
        ColoredBalanceProperty coloredBalance = new ColoredBalanceProperty();
        coloredBalance.setBalance(10000L, new ColoredCoinID(senderAccount.getID()));
        AccountProperties.setProperty(senderAccount, coloredBalance);
        ledger.putAccount(senderAccount);
    }

    @Test
    public void invalid_attach() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

        HashMap<String, Object> map = new HashMap<>();
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinSupply, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void money_supply_invalid_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.MONEY_SUPPLY_INVALID_FORMAT);

        HashMap<String, Object> map = new HashMap<>();
        map.put("supply", "supply");
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinSupply, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void money_supply_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.MONEY_SUPPLY_OUT_OF_RANGE);

        HashMap<String, Object> map = new HashMap<>();
        map.put("supply", -1L);
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinSupply, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void illegal_account_state() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_NOT_EXISTS);

        Mockito.when(senderAccount.getProperty(PropertyType.COLORED_COIN)).thenReturn(null);

        Transaction tx = ColoredCoinSupplyBuilder.createNew(10000L).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void money_supply_reset() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.VALUE_ALREADY_SET);

        Transaction tx = ColoredCoinSupplyBuilder.createNew(50000L).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void insufficient_balance() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_NOT_ENOUGH_FUNDS);

        Transaction tx = ColoredCoinSupplyBuilder.createNew(12000L).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void insufficient_balance_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_NOT_ENOUGH_FUNDS);

        Mockito.when(senderAccount.getProperty(PropertyType.COLORED_BALANCE)).thenReturn(null);

        Transaction tx = ColoredCoinSupplyBuilder.createNew(12000L).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void illegal_set_to_zero() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_INCOMPLETE_MONEY_SUPPLY);

        Transaction tx = ColoredCoinSupplyBuilder.createNew(0L).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void invalid_nested_transaction() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

        Transaction innerTx = new TransactionBuilder(1).build(networkID, sender);
        Transaction tx = ColoredCoinSupplyBuilder.createNew(0L).addNested(innerTx).build(networkID, sender);

        validate(tx);
    }

    @Test
    public void success() throws Exception {
        Transaction tx = ColoredCoinSupplyBuilder.createNew(40000L).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void success_1() throws Exception {
        Transaction tx = ColoredCoinSupplyBuilder.createNew(60000L).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void success_2() throws Exception {
        ColoredBalanceProperty coloredBalance = new ColoredBalanceProperty();
        coloredBalance.setBalance(50000L, new ColoredCoinID(senderAccount.getID()));
        AccountProperties.setProperty(senderAccount, coloredBalance);

        Transaction tx = ColoredCoinSupplyBuilder.createNew(0L).build(networkID, sender);
        validate(tx);
    }
}
