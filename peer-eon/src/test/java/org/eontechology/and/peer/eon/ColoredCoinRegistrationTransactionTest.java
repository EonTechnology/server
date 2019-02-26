package org.eontechology.and.peer.eon;

import java.util.Arrays;
import java.util.HashMap;

import org.eontechology.and.peer.Signer;
import org.eontechology.and.peer.TestAccount;
import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.crypto.ISigner;
import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.middleware.ITransactionParser;
import org.eontechology.and.peer.eon.ledger.AccountProperties;
import org.eontechology.and.peer.eon.ledger.state.BalanceProperty;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechology.and.peer.eon.ledger.state.RegistrationDataProperty;
import org.eontechology.and.peer.eon.midleware.Resources;
import org.eontechology.and.peer.eon.midleware.parsers.ColoredCoinRegistrationParserV1;
import org.eontechology.and.peer.eon.midleware.parsers.ColoredCoinRegistrationParserV2;
import org.eontechology.and.peer.tx.ColoredCoinID;
import org.eontechology.and.peer.tx.TransactionType;
import org.eontechology.and.peer.tx.midleware.builders.ColoredCoinRegistrationBuilder;
import org.eontechology.and.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

@RunWith(Enclosed.class)
public class ColoredCoinRegistrationTransactionTest {

    @RunWith(Parameterized.class)
    public static class ParametrizedPart extends AbstractTransactionTest {

        private ISigner sender = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        private Account senderAccount;
        private ITransactionParser parser;

        public ParametrizedPart(ITransactionParser parser) {
            this.parser = parser;
        }

        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> parsers() {
            return Arrays.asList(new Object[][] {
                    {new ColoredCoinRegistrationParserV2()}, {new ColoredCoinRegistrationParserV1()}
            });
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
            Transaction tx =
                    new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void invalid_nested_transaction() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

            Transaction innerTx = new TransactionBuilder(1).build(networkID, sender);
            Transaction tx =
                    ColoredCoinRegistrationBuilder.createNew(1000000L, 1).addNested(innerTx).build(networkID, sender);

            validate(parser, tx);
        }

        @Test
        public void decimal_point_out_of_range() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.DECIMAL_POINT_OUT_OF_RANGE);

            HashMap<String, Object> map = new HashMap<>();
            map.put("emission", 100L);
            map.put("decimal", 100L);
            Transaction tx =
                    new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void decimal_point_not_specified() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.DECIMAL_POINT_INVALID_FORMAT);

            HashMap<String, Object> map = new HashMap<>();
            map.put("emission", 100L);
            map.put("field", 100L);
            Transaction tx =
                    new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void invalid_emission_format() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.EMISSION_INVALID_FORMAT);

            HashMap<String, Object> map = new HashMap<>();
            map.put("emission", "18446744073709551616");
            map.put("decimal", 2L);
            Transaction tx =
                    new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void emission_out_of_range() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.EMISSION_OUT_OF_RANGE);

            HashMap<String, Object> map = new HashMap<>();
            map.put("emission", -1L);
            map.put("decimal", 2L);
            Transaction tx =
                    new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void re_enable() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.COLORED_COIN_ALREADY_EXISTS);

            ColoredCoinProperty coloredCoin = new ColoredCoinProperty();
            coloredCoin.setAttributes(new ColoredCoinProperty.Attributes(0, 0));
            coloredCoin.setEmitMode(ColoredCoinEmitMode.PRESET);
            coloredCoin.setMoneySupply(1000L);
            AccountProperties.setProperty(senderAccount, coloredCoin);

            Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void emission_error_null() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.EMISSION_INVALID_FORMAT);

            Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, sender);

            tx.getData().put("emission", null);
            validate(parser, tx);
        }

        @Test
        public void emission_error_string() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.EMISSION_INVALID_FORMAT);

            Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, sender);

            tx.getData().put("emission", "100");
            validate(parser, tx);
        }

        @Test
        public void emission_error_decimal() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.EMISSION_INVALID_FORMAT);

            Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, sender);

            tx.getData().put("emission", 100.001);
            validate(parser, tx);
        }

        @Test
        public void decimal_error_null() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.DECIMAL_POINT_INVALID_FORMAT);

            Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, sender);

            tx.getData().put("decimal", null);
            validate(parser, tx);
        }

        @Test
        public void decimal_error_string() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.DECIMAL_POINT_INVALID_FORMAT);

            Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, sender);

            tx.getData().put("decimal", "5");
            validate(parser, tx);
        }

        @Test
        public void decimal_error_decimal() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.DECIMAL_POINT_INVALID_FORMAT);

            Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, sender);

            tx.getData().put("decimal", 5.001);
            validate(parser, tx);
        }

        @Test
        public void decimal_error_over() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.DECIMAL_POINT_OUT_OF_RANGE);

            Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, sender);

            tx.getData().put("decimal", 5 + 0xFFFFFFFFL);
            validate(parser, tx);
        }
    }

    public static class V1 extends AbstractTransactionTest {

        private ISigner sender = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        private Account senderAccount;
        private ITransactionParser parser = new ColoredCoinRegistrationParserV1();

        @Test
        public void invalid_emission_format() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.EMISSION_INVALID_FORMAT);

            HashMap<String, Object> map = new HashMap<>();
            map.put("emission", "werwerwer");
            map.put("decimal", 2);
            Transaction tx =
                    new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void success() throws Exception {
            Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, sender);
            validate(parser, tx);
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
    }

    public static class V2 extends AbstractTransactionTest {
        private ISigner sender = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        private Account senderAccount;
        private ITransactionParser parser = new ColoredCoinRegistrationParserV2();

        @Test
        public void unknown_mode() throws Exception {
            expectedException.expect(ValidateException.class);
            expectedException.expectMessage(Resources.EMISSION_INVALID_FORMAT);

            HashMap<String, Object> map = new HashMap<>();
            map.put("emission", "preset");
            map.put("decimal", 2L);
            Transaction tx =
                    new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(networkID, sender);
            validate(parser, tx);
        }

        @Test
        public void auto_emission_mode() throws Exception {
            Transaction tx = ColoredCoinRegistrationBuilder.createNew(2).build(networkID, sender);
            validate(parser, tx);

            Assert.assertTrue(AccountProperties.getColoredCoin(senderAccount).isIssued());
            Assert.assertEquals(AccountProperties.getColoredCoin(senderAccount).getMoneySupply(), 0L);
        }

        @Test
        public void preset_emission_mode() throws Exception {
            Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, sender);
            validate(parser, tx);

            Assert.assertTrue(AccountProperties.getColoredCoin(senderAccount).isIssued());
            Assert.assertEquals(AccountProperties.getColoredCoin(senderAccount).getMoneySupply(), 1000000L);
            Assert.assertEquals(AccountProperties.getColoredBalance(senderAccount)
                                                 .getBalance(new ColoredCoinID(senderAccount.getID())), 1000000L);
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
    }
}
