package com.exscudo.peer.eon.midleware;

import com.exscudo.peer.Signer;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinPaymentParser;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinRegistrationParser;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinSupplyParser;
import com.exscudo.peer.eon.midleware.parsers.ComplexPaymentParser;
import com.exscudo.peer.eon.midleware.parsers.DelegateParser;
import com.exscudo.peer.eon.midleware.parsers.DepositParser;
import com.exscudo.peer.eon.midleware.parsers.PaymentParser;
import com.exscudo.peer.eon.midleware.parsers.PublicationParser;
import com.exscudo.peer.eon.midleware.parsers.QuorumParser;
import com.exscudo.peer.eon.midleware.parsers.RegistrationParser;
import com.exscudo.peer.eon.midleware.parsers.RejectionParser;
import com.exscudo.peer.tx.ColoredCoinID;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.ColoredCoinRegistrationBuilder;
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
import org.junit.Test;

public class RecipientParseTest {
    private ISigner signer = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private BlockID networkID = new BlockID(100500L);

    @Test
    public void colored_payment() throws Exception {
        Transaction tx =
                ColoredPaymentBuilder.createNew(9999L, new ColoredCoinID(1L), new AccountID(signer.getPublicKey()))
                                     .build(networkID, signer);

        PaymentParser parser = new PaymentParser();
        Assert.assertEquals(parser.getRecipient(tx), new AccountID(signer.getPublicKey()));
    }

    @Test
    public void colored_coin_registration() throws Exception {
        Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(networkID, signer);
        ColoredCoinRegistrationParser parser = new ColoredCoinRegistrationParser();
        Assert.assertNull(parser.getRecipient(tx));
    }

    @Test
    public void colored_coin_supply() throws Exception {
        Transaction tx = ColoredCoinSupplyBuilder.createNew(10000L).build(networkID, signer);
        ColoredCoinSupplyParser parser = new ColoredCoinSupplyParser();
        Assert.assertNull(parser.getRecipient(tx));
    }

    @Test
    public void complex_payment() throws Exception {
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

        ComplexPaymentParser parser = new ComplexPaymentParser(CompositeTransactionParser.create()
                                                                                         .addParser(TransactionType.Payment,
                                                                                                    new PaymentParser())
                                                                                         .addParser(TransactionType.ColoredCoinPayment,
                                                                                                    new ColoredCoinPaymentParser())
                                                                                         .build());
        Assert.assertEquals(parser.getRecipient(tx), new AccountID(signer1.getPublicKey()));
    }

    @Test
    public void delegate() throws Exception {
        Transaction tx = DelegateBuilder.createNew(new AccountID(0L), 50).build(networkID, signer);
        DelegateParser parser = new DelegateParser();
        Assert.assertNull(parser.getRecipient(tx));
    }

    @Test
    public void deposit() throws Exception {
        Transaction tx = DepositBuilder.createNew(999L).build(networkID, signer);
        DepositParser parser = new DepositParser();
        Assert.assertNull(parser.getRecipient(tx));
    }

    @Test
    public void payment() throws Exception {
        AccountID recipient = new AccountID(0L);
        Transaction tx = PaymentBuilder.createNew(100L, recipient).forFee(5L).build(networkID, signer);
        PaymentParser parser = new PaymentParser();
        Assert.assertEquals(parser.getRecipient(tx), recipient);
    }

    @Test
    public void publication() throws Exception {
        Transaction tx =
                PublicationBuilder.createNew("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00")
                                  .build(networkID, signer);
        PublicationParser parser = new PublicationParser();
        Assert.assertNull(parser.getRecipient(tx));
    }

    @Test
    public void quorum() throws Exception {
        Transaction tx = QuorumBuilder.createNew(70).build(networkID, signer);
        QuorumParser parser = new QuorumParser();
        Assert.assertNull(parser.getRecipient(tx));
    }

    @Test
    public void registration() throws Exception {
        Signer newSigner = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        Transaction tx = RegistrationBuilder.createNew(newSigner.getPublicKey()).build(networkID, signer);
        RegistrationParser parser = new RegistrationParser();
        Assert.assertEquals(parser.getRecipient(tx), new AccountID(newSigner.getPublicKey()));
    }

    @Test
    public void rejection() throws Exception {
        Transaction tx = RejectionBuilder.createNew(new AccountID(100L)).build(networkID, signer);

        RejectionParser parser = new RejectionParser();
        Assert.assertNull(parser.getRecipient(tx));
    }
}
