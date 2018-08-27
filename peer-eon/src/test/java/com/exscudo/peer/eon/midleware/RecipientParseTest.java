package com.exscudo.peer.eon.midleware;

import java.util.Collections;
import java.util.HashSet;

import com.exscudo.peer.Signer;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinPaymentParser;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinRegistrationParserV1;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinRegistrationParserV2;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinRemoveParser;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinSupplyParserV1;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinSupplyParserV2;
import com.exscudo.peer.eon.midleware.parsers.ComplexPaymentParserV1;
import com.exscudo.peer.eon.midleware.parsers.ComplexPaymentParserV2;
import com.exscudo.peer.eon.midleware.parsers.DelegateParser;
import com.exscudo.peer.eon.midleware.parsers.DepositParser;
import com.exscudo.peer.eon.midleware.parsers.PaymentParser;
import com.exscudo.peer.eon.midleware.parsers.PublicationParser;
import com.exscudo.peer.eon.midleware.parsers.QuorumParser;
import com.exscudo.peer.eon.midleware.parsers.RegistrationParser;
import com.exscudo.peer.eon.midleware.parsers.RejectionParser;
import com.exscudo.peer.tx.ColoredCoinID;
import com.exscudo.peer.tx.midleware.builders.ColoredCoinRegistrationBuilder;
import com.exscudo.peer.tx.midleware.builders.ColoredCoinRemoveBuilder;
import com.exscudo.peer.tx.midleware.builders.ColoredCoinSupplyBuilder;
import com.exscudo.peer.tx.midleware.builders.ColoredPaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.ComplexPaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.DelegateBuilder;
import com.exscudo.peer.tx.midleware.builders.DepositBuilder;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.PublicationBuilder;
import com.exscudo.peer.tx.midleware.builders.QuorumBuilder;
import com.exscudo.peer.tx.midleware.builders.RegistrationBuilder;
import com.exscudo.peer.tx.midleware.builders.RejectionBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RecipientParseTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private ISigner signer = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private BlockID networkID = new BlockID(100500L);

    @Test
    public void colored_payment() throws Exception {
        Transaction tx =
                ColoredPaymentBuilder.createNew(9999L, new ColoredCoinID(1L), new AccountID(signer.getPublicKey()))
                                     .build(networkID, signer);

        ColoredCoinPaymentParser parser = new ColoredCoinPaymentParser();
        Assert.assertEquals(parser.getDependencies(tx), Collections.singleton(new AccountID(signer.getPublicKey())));
    }

    @Test
    public void colored_payment_error() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.RECIPIENT_INVALID_FORMAT);

        Transaction tx =
                ColoredPaymentBuilder.createNew(9999L, new ColoredCoinID(1L), new AccountID(signer.getPublicKey()))
                                     .build(networkID, signer);

        tx.getData().put("recipient", "EON-ID-ERROR");

        new ColoredCoinPaymentParser().getDependencies(tx);
    }

    @Test
    public void colored_payment_error2() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.RECIPIENT_INVALID_FORMAT);

        Transaction tx =
                ColoredPaymentBuilder.createNew(9999L, new ColoredCoinID(1L), new AccountID(signer.getPublicKey()))
                                     .build(networkID, signer);

        tx.getData().remove("recipient");

        new ColoredCoinPaymentParser().getDependencies(tx);
    }

    @Test
    public void colored_coin_registration_v1() throws Exception {
        Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, signer);
        ColoredCoinRegistrationParserV1 parser = new ColoredCoinRegistrationParserV1();
        Assert.assertNull(parser.getDependencies(tx));
    }

    @Test
    public void colored_coin_registration_v2() throws Exception {
        Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, signer);
        ColoredCoinRegistrationParserV2 parser = new ColoredCoinRegistrationParserV2();
        Assert.assertNull(parser.getDependencies(tx));
    }

    @Test
    public void colored_coin_supply_v1() throws Exception {
        Transaction tx = ColoredCoinSupplyBuilder.createNew(10000L).build(networkID, signer);
        ColoredCoinSupplyParserV1 parser = new ColoredCoinSupplyParserV1();
        Assert.assertNull(parser.getDependencies(tx));
    }

    @Test
    public void colored_coin_supply_v2() throws Exception {
        Transaction tx = ColoredCoinSupplyBuilder.createNew(10000L).build(networkID, signer);
        ColoredCoinSupplyParserV2 parser = new ColoredCoinSupplyParserV2();
        Assert.assertNull(parser.getDependencies(tx));
    }

    @Test
    public void colored_coin_remove() throws Exception {
        Transaction tx = ColoredCoinRemoveBuilder.createNew().build(networkID, signer);
        ColoredCoinRemoveParser parser = new ColoredCoinRemoveParser();
        Assert.assertNull(parser.getDependencies(tx));
    }

    @Test
    public void complex_payment_v1() throws Exception {
        Signer signer1 = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");

        Transaction nestedTx1 = PaymentBuilder.createNew(100L, new AccountID(signer.getPublicKey()))
                                              .forFee(0L)
                                              .build(networkID, signer1);
        Transaction nestedTx2 =
                ColoredPaymentBuilder.createNew(10L, new ColoredCoinID(0L), new AccountID(signer1.getPublicKey()))
                                     .refBy(nestedTx1.getID())
                                     .build(networkID, signer);
        Transaction nestedTx3 = PaymentBuilder.createNew(1L, new AccountID(signer1.getPublicKey()))
                                              .refBy(nestedTx1.getID())
                                              .build(networkID, signer);
        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2, nestedTx3})
                                              .build(networkID, signer);

        ComplexPaymentParserV1 parser = new ComplexPaymentParserV1();
        Assert.assertEquals(parser.getDependencies(tx), Collections.singleton(new AccountID(signer1.getPublicKey())));
    }

    @Test
    public void complex_payment_error_v1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_SEQUENCE_NOT_FOUND);

        Signer signer1 = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");

        Transaction nestedTx1 = PaymentBuilder.createNew(100L, new AccountID(signer.getPublicKey()))
                                              .forFee(0L)
                                              .build(networkID, signer1);
        Transaction nestedTx2 =
                ColoredPaymentBuilder.createNew(10L, new ColoredCoinID(0L), new AccountID(signer1.getPublicKey()))
                                     .refBy(nestedTx1.getID())
                                     .build(networkID, signer);
        Transaction nestedTx3 = PaymentBuilder.createNew(1L, new AccountID(signer1.getPublicKey()))
                                              .refBy(nestedTx1.getID())
                                              .build(networkID, signer);
        Transaction tx =
                ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx2, nestedTx3}).build(networkID, signer);

        ComplexPaymentParserV1 parser = new ComplexPaymentParserV1();
        Assert.assertEquals(parser.getDependencies(tx), Collections.singleton(new AccountID(signer1.getPublicKey())));
    }

    @Test
    public void complex_payment_v2() throws Exception {
        Signer signer1 = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        Signer signer2 = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
        Signer signer3 = new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");
        Signer signer4 = new Signer("33445566778899aabbccddeeff00112233445566778899aabbccddeeff001122");

        Transaction nestedTx1 = PaymentBuilder.createNew(100L, new AccountID(signer3.getPublicKey()))
                                              .forFee(0L)
                                              .payedBy(new AccountID(signer1.getPublicKey()))
                                              .build(networkID, signer2);
        Transaction nestedTx2 = PaymentBuilder.createNew(100L, new AccountID(signer4.getPublicKey()))
                                              .forFee(0L)
                                              .refBy(nestedTx1.getID())
                                              .build(networkID, signer2);
        Transaction tx =
                ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2}).build(networkID, signer);

        ComplexPaymentParserV2 parser = new ComplexPaymentParserV2();
        Assert.assertEquals(parser.getDependencies(tx), new HashSet<AccountID>() {{
            add(new AccountID(signer2.getPublicKey()));
            add(new AccountID(signer3.getPublicKey()));
            add(new AccountID(signer4.getPublicKey()));
        }});
    }

    @Test
    public void delegate() throws Exception {
        AccountID id = new AccountID(0L);
        Transaction tx = DelegateBuilder.createNew(id, 50).build(networkID, signer);
        DelegateParser parser = new DelegateParser();
        Assert.assertEquals(parser.getDependencies(tx), Collections.singleton(id));
    }

    @Test
    public void delegate_error() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ACCOUNT_ID_INVALID_FORMAT);

        AccountID id = new AccountID(0L);
        Transaction tx = DelegateBuilder.createNew(id, 50).build(networkID, signer);
        tx.getData().put("EON-ID-ERROR", 50);

        new DelegateParser().getDependencies(tx);
    }

    @Test
    public void deposit() throws Exception {
        Transaction tx = DepositBuilder.createNew(999L).build(networkID, signer);
        DepositParser parser = new DepositParser();
        Assert.assertNull(parser.getDependencies(tx));
    }

    @Test
    public void payment() throws Exception {
        AccountID recipient = new AccountID(0L);
        Transaction tx = PaymentBuilder.createNew(100L, recipient).forFee(5L).build(networkID, signer);
        PaymentParser parser = new PaymentParser();
        Assert.assertEquals(parser.getDependencies(tx), Collections.singleton(recipient));
    }

    @Test
    public void payment_error() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.RECIPIENT_INVALID_FORMAT);

        AccountID recipient = new AccountID(0L);
        Transaction tx = PaymentBuilder.createNew(100L, recipient).forFee(5L).build(networkID, signer);

        tx.getData().put("recipient", "EON-ID-ERROR");

        new PaymentParser().getDependencies(tx);
    }

    @Test
    public void payment_error2() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.RECIPIENT_INVALID_FORMAT);

        AccountID recipient = new AccountID(0L);
        Transaction tx = PaymentBuilder.createNew(100L, recipient).forFee(5L).build(networkID, signer);

        tx.getData().remove("recipient");

        new PaymentParser().getDependencies(tx);
    }

    @Test
    public void publication() throws Exception {
        Transaction tx =
                PublicationBuilder.createNew("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00")
                                  .build(networkID, signer);
        PublicationParser parser = new PublicationParser(CryptoProvider.getInstance().getSignature());
        Assert.assertNull(parser.getDependencies(tx));
    }

    @Test
    public void quorum() throws Exception {
        Transaction tx = QuorumBuilder.createNew(70).build(networkID, signer);
        QuorumParser parser = new QuorumParser();
        Assert.assertNull(parser.getDependencies(tx));
    }

    @Test
    public void registration() throws Exception {
        Signer newSigner = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        Transaction tx = RegistrationBuilder.createNew(newSigner.getPublicKey()).build(networkID, signer);
        RegistrationParser parser = new RegistrationParser();
        Assert.assertEquals(parser.getDependencies(tx), Collections.singleton(new AccountID(newSigner.getPublicKey())));
    }

    @Test
    public void registration_error() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ACCOUNT_ID_INVALID_FORMAT);

        Signer newSigner = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        Transaction tx = RegistrationBuilder.createNew(newSigner.getPublicKey()).build(networkID, signer);
        tx.getData().put("EON-ID-ERROR", Format.convert(newSigner.getPublicKey()));

        new RegistrationParser().getDependencies(tx);
    }

    @Test
    public void rejection() throws Exception {
        AccountID id = new AccountID(100L);
        Transaction tx = RejectionBuilder.createNew(id).build(networkID, signer);

        RejectionParser parser = new RejectionParser();
        Assert.assertEquals(parser.getDependencies(tx), Collections.singleton(id));
    }

    @Test
    public void rejection_error() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ACCOUNT_ID_INVALID_FORMAT);

        AccountID id = new AccountID(100L);
        Transaction tx = RejectionBuilder.createNew(id).build(networkID, signer);

        tx.getData().put("account", "EON-ID-ERROR");

        new RejectionParser().getDependencies(tx);
    }

    @Test
    public void rejection_error2() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ACCOUNT_ID_INVALID_FORMAT);

        AccountID id = new AccountID(100L);
        Transaction tx = RejectionBuilder.createNew(id).build(networkID, signer);

        tx.getData().remove("account");

        new RejectionParser().getDependencies(tx);
    }
}
