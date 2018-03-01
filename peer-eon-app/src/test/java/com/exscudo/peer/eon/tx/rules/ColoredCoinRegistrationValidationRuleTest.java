package com.exscudo.peer.eon.tx.rules;

import java.util.HashMap;

import com.exscudo.peer.core.TransactionType;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.ed25519.Ed25519Signer;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.tx.ITransactionParser;
import com.exscudo.peer.eon.tx.builders.ColoredCoinRegistrationBuilder;
import com.exscudo.peer.eon.tx.builders.TransactionBuilder;
import com.exscudo.peer.eon.tx.parsers.ColoredCoinRegistrationParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ColoredCoinRegistrationValidationRuleTest extends AbstractParserTest {
    private ColoredCoinRegistrationParser parser = new ColoredCoinRegistrationParser();

    private ISigner sender = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private Account senderAccount;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        senderAccount = Mockito.spy(new DefaultAccount(new AccountID(sender.getPublicKey())));
        AccountProperties.setProperty(senderAccount, new RegistrationDataProperty(sender.getPublicKey()));
        AccountProperties.setProperty(senderAccount, new BalanceProperty(5000L));

        ledger.putAccount(senderAccount);
    }

    @Test
    public void invalid_attachment() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Attachment of unknown type.");

        HashMap<String, Object> map = new HashMap<>();
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(sender);
        validate(tx);
    }

    @Test
    public void invalid_emission_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The 'emission' field value has a unsupported format.");

        HashMap<String, Object> map = new HashMap<>();
        map.put("emission", "werwerwer");
        map.put("decimalPoint", 2);
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(sender);
        validate(tx);
    }

    @Test
    public void invalid_emission_format_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The 'emission' field value has a unsupported format.");

        HashMap<String, Object> map = new HashMap<>();
        map.put("emission", "18446744073709551616");
        map.put("decimalPoint", 2);
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(sender);
        validate(tx);
    }

    @Test
    public void emission_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The 'emission' field value out of range.");

        HashMap<String, Object> map = new HashMap<>();
        map.put("emission", -1);
        map.put("decimalPoint", 2);
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(sender);
        validate(tx);
    }

    @Test
    public void decimal_point_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The 'decimalPoint' field value is out of range.");

        HashMap<String, Object> map = new HashMap<>();
        map.put("emission", 100L);
        map.put("decimalPoint", 100L);
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(sender);
        validate(tx);
    }

    @Test
    public void decimal_point_not_specified() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The 'decimalPoint' field value has a unsupported format.");

        HashMap<String, Object> map = new HashMap<>();
        map.put("emission", 100L);
        map.put("field", 100L);
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(sender);
        validate(tx);
    }

    @Test
    public void re_enable() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Account is already associated with a color coin.");

        ColoredCoinProperty coloredCoin = new ColoredCoinProperty();
        coloredCoin.setMoneySupply(1000L);
        AccountProperties.setProperty(senderAccount, coloredCoin);

        Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(sender);
        validate(tx);
    }

    @Test
    public void success() throws Exception {
        Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(sender);
        validate(tx);
    }
}
