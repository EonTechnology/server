package org.eontechnology.and.peer.eon;

import java.util.Arrays;
import java.util.HashMap;

import org.eontechnology.and.peer.Signer;
import org.eontechnology.and.peer.TestAccount;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.eon.ledger.AccountProperties;
import org.eontechnology.and.peer.eon.ledger.state.BalanceProperty;
import org.eontechnology.and.peer.eon.ledger.state.ColoredBalanceProperty;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechnology.and.peer.eon.ledger.state.RegistrationDataProperty;
import org.eontechnology.and.peer.eon.midleware.Resources;
import org.eontechnology.and.peer.eon.midleware.parsers.ColoredCoinSupplyParserV1;
import org.eontechnology.and.peer.eon.midleware.parsers.ColoredCoinSupplyParserV2;
import org.eontechnology.and.peer.tx.ColoredCoinID;
import org.eontechnology.and.peer.tx.TransactionType;
import org.eontechnology.and.peer.tx.midleware.builders.ColoredCoinSupplyBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

@RunWith(Enclosed.class)
public class ColoredCoinSupplyTransactionTest {

    @RunWith(Parameterized.class)
    public static class ParametrizedPart extends AbstractTransactionTest {
        private static final String SENDER = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
        private ITransactionParser parser;
        private ISigner sender;
        private Account senderAccount;

        public ParametrizedPart(ITransactionParser parser) {
            this.parser = parser;
        }

        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> parsers() {
            return Arrays.asList(new Object[][] {
                    {new ColoredCoinSupplyParserV2()}, {new ColoredCoinSupplyParserV1()}
            });
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
            coloredCoin.setAttributes(new ColoredCoinProperty.Attributes(0, 0));
            coloredCoin.setMoneySupply(50000L);
            coloredCoin.setEmitMode(ColoredCoinEmitMode.PRESET);
            AccountProperties.setProperty(senderAccount, coloredCoin);
            ColoredBalanceProperty coloredBalance = new ColoredBalanceProperty();
            coloredBalance.setBalance(10000L, new ColoredCoinID(senderAccount.getID()));
            AccountProperties.setProperty(senderAccount, coloredBalance);
            ledger.putAccount(senderAccount);
        }

        @Test
        public void invalid_nested_transaction() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

            Transaction innerTx = new TransactionBuilder(1).build(networkID, sender);
            Transaction tx = ColoredCoinSupplyBuilder.createNew(0L).addNested(innerTx).build(networkID, sender);

            validate(parser, tx);
        }

        @Test
        public void invalid_attach() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

            HashMap<String, Object> map = new HashMap<>();
            Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinSupply, map).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void illegal_account_state() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.COLORED_COIN_NOT_EXISTS);

            Mockito.when(senderAccount.getProperty(PropertyType.COLORED_COIN)).thenReturn(null);

            Transaction tx = ColoredCoinSupplyBuilder.createNew(10000L).build(networkID, sender);
            validate(parser, tx);
        }
    }

    public static class V1 extends AbstractTransactionTest {
        private static final String SENDER = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
        private ITransactionParser parser = new ColoredCoinSupplyParserV1();

        private ISigner sender;
        private Account senderAccount;

        @Before
        @Override
        public void setUp() throws Exception {
            super.setUp();

            sender = new Signer(SENDER);

            senderAccount = Mockito.spy(new TestAccount(new AccountID(sender.getPublicKey())));
            AccountProperties.setProperty(senderAccount, new RegistrationDataProperty(sender.getPublicKey()));
            AccountProperties.setProperty(senderAccount, new BalanceProperty(100L));
            ColoredCoinProperty coloredCoin = new ColoredCoinProperty();
            coloredCoin.setAttributes(new ColoredCoinProperty.Attributes(0, 0));
            coloredCoin.setMoneySupply(50000L);
            coloredCoin.setEmitMode(ColoredCoinEmitMode.PRESET);
            AccountProperties.setProperty(senderAccount, coloredCoin);
            ColoredBalanceProperty coloredBalance = new ColoredBalanceProperty();
            coloredBalance.setBalance(10000L, new ColoredCoinID(senderAccount.getID()));
            AccountProperties.setProperty(senderAccount, coloredBalance);
            ledger.putAccount(senderAccount);
        }

        @Test
        public void money_supply_out_of_range() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.MONEY_SUPPLY_OUT_OF_RANGE);

            HashMap<String, Object> map = new HashMap<>();
            map.put("supply", -1L);
            Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinSupply, map).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void money_supply_reset() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.VALUE_ALREADY_SET);

            Transaction tx = ColoredCoinSupplyBuilder.createNew(50000L).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void insufficient_balance() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.COLORED_COIN_NOT_ENOUGH_FUNDS);

            Transaction tx = ColoredCoinSupplyBuilder.createNew(12000L).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void insufficient_balance_1() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.COLORED_COIN_NOT_ENOUGH_FUNDS);

            Mockito.when(senderAccount.getProperty(PropertyType.COLORED_BALANCE)).thenReturn(null);

            Transaction tx = ColoredCoinSupplyBuilder.createNew(12000L).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void illegal_set_to_zero() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.COLORED_COIN_INCOMPLETE_MONEY_SUPPLY);

            Transaction tx = ColoredCoinSupplyBuilder.createNew(0L).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void success() throws Exception {
            Transaction tx = ColoredCoinSupplyBuilder.createNew(40000L).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void success_1() throws Exception {
            Transaction tx = ColoredCoinSupplyBuilder.createNew(60000L).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void success_2() throws Exception {
            ColoredBalanceProperty coloredBalance = new ColoredBalanceProperty();
            coloredBalance.setBalance(50000L, new ColoredCoinID(senderAccount.getID()));
            AccountProperties.setProperty(senderAccount, coloredBalance);

            Transaction tx = ColoredCoinSupplyBuilder.createNew(0L).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void money_supply_invalid_format() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.MONEY_SUPPLY_INVALID_FORMAT);

            HashMap<String, Object> map = new HashMap<>();
            map.put("supply", "supply");
            Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinSupply, map).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void supply_error_null() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.MONEY_SUPPLY_INVALID_FORMAT);

            Transaction tx = ColoredCoinSupplyBuilder.createNew(100L).build(networkID, sender);

            tx.getData().put("supply", null);
            validate(parser, tx);
        }

        @Test
        public void supply_error_string() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.MONEY_SUPPLY_INVALID_FORMAT);

            Transaction tx = ColoredCoinSupplyBuilder.createNew(100L).build(networkID, sender);

            tx.getData().put("supply", "100");
            validate(parser, tx);
        }

        @Test
        public void supply_error_decimal() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.MONEY_SUPPLY_INVALID_FORMAT);

            Transaction tx = ColoredCoinSupplyBuilder.createNew(100L).build(networkID, sender);

            tx.getData().put("supply", 100.001);
            validate(parser, tx);
        }
    }

    public static class V2 extends AbstractTransactionTest {
        private static final String SENDER = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
        private ITransactionParser parser = new ColoredCoinSupplyParserV2();
        private ISigner sender;
        private Account senderAccount;

        @Before
        @Override
        public void setUp() throws Exception {
            super.setUp();

            sender = new Signer(SENDER);

            senderAccount = Mockito.spy(new TestAccount(new AccountID(sender.getPublicKey())));
            AccountProperties.setProperty(senderAccount, new RegistrationDataProperty(sender.getPublicKey()));
            AccountProperties.setProperty(senderAccount, new BalanceProperty(100L));
            ledger.putAccount(senderAccount);
        }

        @Test
        public void unknown_mode() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.MONEY_SUPPLY_INVALID_FORMAT);

            Transaction tx = ColoredCoinSupplyBuilder.createNew("preset").build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void toggle_to_auto_emission_mode() throws Exception {
            ColoredCoinID id = new ColoredCoinID(senderAccount.getID());

            AccountProperties.setProperty(senderAccount,
                                          new ColoredCoinProperty().setAttributes(new ColoredCoinProperty.Attributes(0,
                                                                                                                     0))
                                                                   .setMoneySupply(50000L)
                                                                   .setEmitMode(ColoredCoinEmitMode.PRESET));
            AccountProperties.setProperty(senderAccount, new ColoredBalanceProperty().setBalance(10000L, id));

            Transaction tx = ColoredCoinSupplyBuilder.createNew("auto").build(networkID, sender);
            validate(parser, tx);

            ColoredCoinProperty newColoredCoin = AccountProperties.getColoredCoin(senderAccount);
            Assert.assertTrue(newColoredCoin.isIssued());
            Assert.assertEquals(newColoredCoin.getEmitMode(), ColoredCoinEmitMode.AUTO);
            Assert.assertEquals(newColoredCoin.getMoneySupply(), 40000L);
            ColoredBalanceProperty newBalance = AccountProperties.getColoredBalance(senderAccount);
            Assert.assertEquals(newBalance.getBalance(id), 0L);
        }

        @Test
        public void re_enable_to_auto_emission_mode() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.VALUE_ALREADY_SET);

            AccountProperties.setProperty(senderAccount,
                                          new ColoredCoinProperty().setAttributes(new ColoredCoinProperty.Attributes(0,
                                                                                                                     0))
                                                                   .setEmitMode(ColoredCoinEmitMode.AUTO));

            Transaction tx = ColoredCoinSupplyBuilder.createNew("auto").build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void toggle_to_preset_mode() throws Exception {
            ColoredCoinID id = new ColoredCoinID(senderAccount.getID());

            AccountProperties.setProperty(senderAccount,
                                          new ColoredCoinProperty().setAttributes(new ColoredCoinProperty.Attributes(0,
                                                                                                                     0))
                                                                   .setMoneySupply(40000L)
                                                                   .setEmitMode(ColoredCoinEmitMode.AUTO));
            Transaction tx = ColoredCoinSupplyBuilder.createNew(50000L).build(networkID, sender);
            validate(parser, tx);

            ColoredCoinProperty newColoredCoin = AccountProperties.getColoredCoin(senderAccount);
            Assert.assertTrue(newColoredCoin.isIssued());
            Assert.assertEquals(newColoredCoin.getEmitMode(), ColoredCoinEmitMode.PRESET);
            Assert.assertEquals(newColoredCoin.getMoneySupply(), 50000L);
            ColoredBalanceProperty newBalance = AccountProperties.getColoredBalance(senderAccount);
            Assert.assertEquals(newBalance.getBalance(id), 10000L);
        }

        @Test
        public void toggle_to_preset_mode_supply_invalid_value() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.MONEY_SUPPLY_INVALID_FORMAT);

            Transaction tx = ColoredCoinSupplyBuilder.createNew("0").build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void toggle_to_preset_mode_supply_invalid_format() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.MONEY_SUPPLY_INVALID_FORMAT);

            Transaction tx = ColoredCoinSupplyBuilder.createNew("unknown").build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void toggle_to_preset_mode_supply_out_of_range() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.COLORED_COIN_NOT_ENOUGH_FUNDS);

            AccountProperties.setProperty(senderAccount,
                                          new ColoredCoinProperty().setAttributes(new ColoredCoinProperty.Attributes(0,
                                                                                                                     0))
                                                                   .setMoneySupply(40000L)
                                                                   .setEmitMode(ColoredCoinEmitMode.AUTO));
            Transaction tx = ColoredCoinSupplyBuilder.createNew(30000L).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void change_money_supply_repeat() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.VALUE_ALREADY_SET);

            AccountProperties.setProperty(senderAccount,
                                          new ColoredCoinProperty().setAttributes(new ColoredCoinProperty.Attributes(0,
                                                                                                                     0))
                                                                   .setMoneySupply(50000L)
                                                                   .setEmitMode(ColoredCoinEmitMode.PRESET));
            Transaction tx = ColoredCoinSupplyBuilder.createNew(50000L).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void change_money_supply_insufficient_balance() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.COLORED_COIN_NOT_ENOUGH_FUNDS);

            AccountProperties.setProperty(senderAccount,
                                          new ColoredCoinProperty().setAttributes(new ColoredCoinProperty.Attributes(0,
                                                                                                                     0))
                                                                   .setMoneySupply(50000L)
                                                                   .setEmitMode(ColoredCoinEmitMode.PRESET));
            AccountProperties.setProperty(senderAccount,
                                          new ColoredBalanceProperty().setBalance(10000L,
                                                                                  new ColoredCoinID(senderAccount.getID())));

            Transaction tx = ColoredCoinSupplyBuilder.createNew(39000L).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void change_money_supply() throws Exception {
            AccountProperties.setProperty(senderAccount,
                                          new ColoredCoinProperty().setAttributes(new ColoredCoinProperty.Attributes(0,
                                                                                                                     0))
                                                                   .setMoneySupply(50000L)
                                                                   .setEmitMode(ColoredCoinEmitMode.PRESET));
            AccountProperties.setProperty(senderAccount,
                                          new ColoredBalanceProperty().setBalance(10000L,
                                                                                  new ColoredCoinID(senderAccount.getID())));

            Transaction tx = ColoredCoinSupplyBuilder.createNew(40000L).build(networkID, sender);
            validate(parser, tx);

            ColoredCoinProperty newColoredCoin = AccountProperties.getColoredCoin(senderAccount);
            Assert.assertTrue(newColoredCoin.isIssued());
            Assert.assertEquals(newColoredCoin.getEmitMode(), ColoredCoinEmitMode.PRESET);
            Assert.assertEquals(newColoredCoin.getMoneySupply(), 40000L);
            ColoredBalanceProperty newBalance = AccountProperties.getColoredBalance(senderAccount);
            Assert.assertEquals(newBalance.getBalance(new ColoredCoinID(senderAccount.getID())), 0L);
        }
    }
}
