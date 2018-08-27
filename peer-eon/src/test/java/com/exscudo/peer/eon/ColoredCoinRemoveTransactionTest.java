package com.exscudo.peer.eon;

import java.util.HashMap;

import com.exscudo.peer.Signer;
import com.exscudo.peer.TestAccount;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredCoinEmitMode;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinRemoveParser;
import com.exscudo.peer.tx.ColoredCoinID;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.ColoredCoinRemoveBuilder;
import com.exscudo.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ColoredCoinRemoveTransactionTest extends AbstractTransactionTest {
    private static final String SENDER = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    private ISigner sender;
    private ColoredCoinRemoveParser parser = new ColoredCoinRemoveParser();

    private Account account;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        sender = new Signer(SENDER);

        byte[] publicKey = sender.getPublicKey();

        account = Mockito.spy(new TestAccount(new AccountID(publicKey)));
        AccountProperties.setProperty(account, new RegistrationDataProperty(publicKey));
        AccountProperties.setProperty(account, new BalanceProperty(10L));
        ledger.putAccount(account);
    }

    @Test
    public void invalid_nested_transaction() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

        Transaction innerTx = new TransactionBuilder(1).build(networkID, sender);
        Transaction tx = ColoredCoinRemoveBuilder.createNew().addNested(innerTx).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void invalid_attach() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

        HashMap<String, Object> map = new HashMap<>();
        map.put("key", "value");
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRemove, map).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void unknown_sender() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.SENDER_ACCOUNT_NOT_FOUND);

        Mockito.when(ledger.getAccount(account.getID())).thenReturn(null);

        Transaction tx = ColoredCoinRemoveBuilder.createNew().build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void colored_coin_not_associated_with_sender() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_NOT_EXISTS);

        Transaction tx = ColoredCoinRemoveBuilder.createNew().build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void auto_emit_incomplete_money_supply_on_target_balance() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_INCOMPLETE_MONEY_SUPPLY);

        ColoredCoinProperty coin = new ColoredCoinProperty();
        coin.setAttributes(new ColoredCoinProperty.Attributes(0, 0));
        coin.setEmitMode(ColoredCoinEmitMode.AUTO);
        coin.setMoneySupply(1L);
        AccountProperties.setProperty(account, coin);

        Transaction tx = ColoredCoinRemoveBuilder.createNew().build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void preset_incomplete_money_supply_on_target_balance() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_INCOMPLETE_MONEY_SUPPLY);

        ColoredCoinProperty coin = new ColoredCoinProperty();
        coin.setAttributes(new ColoredCoinProperty.Attributes(0, 0));
        coin.setEmitMode(ColoredCoinEmitMode.PRESET);
        coin.setMoneySupply(1000L);
        AccountProperties.setProperty(account, coin);

        ColoredBalanceProperty balance = new ColoredBalanceProperty();
        balance.setBalance(999L, new ColoredCoinID(account.getID()));
        AccountProperties.setProperty(account, balance);

        Transaction tx = ColoredCoinRemoveBuilder.createNew().build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void auto_emit_mode_success() throws Exception {
        ColoredCoinProperty coin = new ColoredCoinProperty();
        coin.setAttributes(new ColoredCoinProperty.Attributes(0, 0));
        coin.setEmitMode(ColoredCoinEmitMode.AUTO);
        coin.setMoneySupply(0L);
        AccountProperties.setProperty(account, coin);

        Assert.assertTrue(AccountProperties.getColoredCoin(account).isIssued());

        Transaction tx = ColoredCoinRemoveBuilder.createNew().build(networkID, sender);
        validate(parser, tx);

        Assert.assertFalse(AccountProperties.getColoredCoin(account).isIssued());
        Assert.assertEquals(AccountProperties.getBalance(account).getValue(), 0);
    }

    @Test
    public void preset_mode_success() throws Exception {

        ColoredCoinProperty coin = new ColoredCoinProperty();
        coin.setAttributes(new ColoredCoinProperty.Attributes(0, 0));
        coin.setEmitMode(ColoredCoinEmitMode.PRESET);
        coin.setMoneySupply(1000L);
        AccountProperties.setProperty(account, coin);

        ColoredBalanceProperty balance = new ColoredBalanceProperty();
        balance.setBalance(1000L, new ColoredCoinID(account.getID()));
        AccountProperties.setProperty(account, balance);

        Assert.assertTrue(AccountProperties.getColoredCoin(account).isIssued());

        Transaction tx = ColoredCoinRemoveBuilder.createNew().build(networkID, sender);
        validate(parser, tx);

        Assert.assertFalse(AccountProperties.getColoredCoin(account).isIssued());
        Assert.assertEquals(AccountProperties.getBalance(account).getValue(), 0);
    }
}
