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
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinRegistrationParser;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.ColoredCoinRegistrationBuilder;
import com.exscudo.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ColoredCoinRegistrationTransactionTest extends AbstractTransactionTest {
    private ColoredCoinRegistrationParser parser = new ColoredCoinRegistrationParser();

    private ISigner sender = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private Account senderAccount;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        senderAccount = Mockito.spy(new TestAccount(new AccountID(sender.getPublicKey())));
        AccountProperties.setProperty(senderAccount, new RegistrationDataProperty(sender.getPublicKey()));
        AccountProperties.setProperty(senderAccount, new BalanceProperty(5000L));

        ledger.putAccount(senderAccount);
    }

    @Test
    public void invalid_attachment() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

        HashMap<String, Object> map = new HashMap<>();
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void invalid_emission_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.EMISSION_INVALID_FORMAT);

        HashMap<String, Object> map = new HashMap<>();
        map.put("emission", "werwerwer");
        map.put("decimal", 2);
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void invalid_emission_format_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.EMISSION_INVALID_FORMAT);

        HashMap<String, Object> map = new HashMap<>();
        map.put("emission", "18446744073709551616");
        map.put("decimal", 2);
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void emission_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.EMISSION_OUT_OF_RANGE);

        HashMap<String, Object> map = new HashMap<>();
        map.put("emission", -1);
        map.put("decimal", 2);
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void decimal_point_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.DECIMAL_POINT_OUT_OF_RANGE);

        HashMap<String, Object> map = new HashMap<>();
        map.put("emission", 100L);
        map.put("decimal", 100L);
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void decimal_point_not_specified() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.DECIMAL_POINT_INVALID_FORMAT);

        HashMap<String, Object> map = new HashMap<>();
        map.put("emission", 100L);
        map.put("field", 100L);
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void re_enable() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_ALREADY_EXISTS);

        ColoredCoinProperty coloredCoin = new ColoredCoinProperty();
        coloredCoin.setMoneySupply(1000L);
        AccountProperties.setProperty(senderAccount, coloredCoin);

        Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void invalid_nested_transaction() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

        Transaction innerTx = new TransactionBuilder(1).build(networkID, sender);
        Transaction tx =
                ColoredCoinRegistrationBuilder.createNew(1000000L, 1).addNested(innerTx).build(networkID, sender);

        validate(tx);
    }

    @Test
    public void success() throws Exception {
        Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, sender);
        validate(tx);
    }
}
