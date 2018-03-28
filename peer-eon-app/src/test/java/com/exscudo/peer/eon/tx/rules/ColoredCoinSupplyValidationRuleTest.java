package com.exscudo.peer.eon.tx.rules;

import java.util.HashMap;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.ed25519.Ed25519Signer;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ColoredCoinID;
import com.exscudo.peer.eon.PropertyType;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.tx.ITransactionParser;
import com.exscudo.peer.eon.tx.builders.ColoredCoinSupplyBuilder;
import com.exscudo.peer.eon.tx.builders.TransactionBuilder;
import com.exscudo.peer.eon.tx.parsers.ColoredCoinSupplyParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ColoredCoinSupplyValidationRuleTest extends AbstractParserTest {
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

        sender = new Ed25519Signer(SENDER);

        senderAccount = Mockito.spy(new DefaultAccount(new AccountID(sender.getPublicKey())));
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
        expectedException.expectMessage("Attachment of unknown type.");

        HashMap<String, Object> map = new HashMap<>();
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinSupply, map).build(sender);
        validate(tx);
    }

    @Test
    public void money_supply_invalid_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The 'moneySupply' field value has a unsupported format.");

        HashMap<String, Object> map = new HashMap<>();
        map.put("moneySupply", "moneySupply");
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinSupply, map).build(sender);
        validate(tx);
    }

    @Test
    public void money_supply_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The 'moneySupply' field value is out of range.");

        HashMap<String, Object> map = new HashMap<>();
        map.put("moneySupply", -1);
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinSupply, map).build(sender);
        validate(tx);
    }

    @Test
    public void illegal_account_state() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Colored coin is not associated with an account.");

        Mockito.when(senderAccount.getProperty(PropertyType.COLORED_COIN)).thenReturn(null);

        Transaction tx = ColoredCoinSupplyBuilder.createNew(10000L).build(sender);
        validate(tx);
    }

    @Test
    public void money_supply_reset() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Value already set.");

        Transaction tx = ColoredCoinSupplyBuilder.createNew(50000L).build(sender);
        validate(tx);
    }

    @Test
    public void insufficient_balance() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Insufficient number of colored coins on the balance.");

        Transaction tx = ColoredCoinSupplyBuilder.createNew(12000L).build(sender);
        validate(tx);
    }

    @Test
    public void insufficient_balance_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Insufficient number of colored coins on the balance.");

        Mockito.when(senderAccount.getProperty(PropertyType.COLORED_BALANCE)).thenReturn(null);

        Transaction tx = ColoredCoinSupplyBuilder.createNew(12000L).build(sender);
        validate(tx);
    }

    @Test
    public void illegal_set_to_zero() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The entire amount of funds must be on the balance.");

        Transaction tx = ColoredCoinSupplyBuilder.createNew(0L).build(sender);
        validate(tx);
    }

    @Test
    public void success() throws Exception {
        Transaction tx = ColoredCoinSupplyBuilder.createNew(40000L).build(sender);
        validate(tx);
    }

    @Test
    public void success_1() throws Exception {
        Transaction tx = ColoredCoinSupplyBuilder.createNew(60000L).build(sender);
        validate(tx);
    }

    @Test
    public void success_2() throws Exception {
        ColoredBalanceProperty coloredBalance = new ColoredBalanceProperty();
        coloredBalance.setBalance(50000L, new ColoredCoinID(senderAccount.getID()));
        AccountProperties.setProperty(senderAccount, coloredBalance);

        Transaction tx = ColoredCoinSupplyBuilder.createNew(0L).build(sender);
        validate(tx);
    }
}
