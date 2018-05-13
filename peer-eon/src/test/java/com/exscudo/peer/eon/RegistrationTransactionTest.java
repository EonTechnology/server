package com.exscudo.peer.eon;

import com.exscudo.peer.Signer;
import com.exscudo.peer.TestAccount;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.parsers.RegistrationParser;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.RegistrationBuilder;
import com.exscudo.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RegistrationTransactionTest extends AbstractTransactionTest {
    private RegistrationParser parser = new RegistrationParser();

    private ISigner senderSigner = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private Account sender;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        sender = Mockito.spy(new TestAccount(new AccountID(senderSigner.getPublicKey())));
        AccountProperties.setProperty(sender, new RegistrationDataProperty(senderSigner.getPublicKey()));

        ledger.putAccount(sender);
    }

    @Test
    public void account_re_registration() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ACCOUNT_ALREADY_EXISTS);

        AccountProperties.setProperty(sender, new BalanceProperty(1000L));
        Transaction tx = RegistrationBuilder.createNew(senderSigner.getPublicKey()).build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void account_invalid_attachment() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

        Transaction tx = new TransactionBuilder(TransactionType.Registration).build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void account_invalid_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ACCOUNT_ID_INVALID_FORMAT);

        Transaction tx = new TransactionBuilder(TransactionType.Registration).withParam("tx_id", "tx")
                                                                             .build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void registered_account_has_invalid_pk() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ACCOUNT_ID_NOT_MATCH_DATA);

        Transaction tx = new TransactionBuilder(TransactionType.Registration).withParam(sender.getID().toString(),
                                                                                        Format.convert(new byte[32]))
                                                                             .build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void registered_account_has_invalid_pk_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ACCOUNT_ID_NOT_MATCH_DATA);

        Transaction tx = new TransactionBuilder(TransactionType.Registration).withParam(sender.getID().toString(),
                                                                                        Format.convert(new byte[31]))
                                                                             .build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void invalid_nested_transaction() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

        Transaction tx = RegistrationBuilder.createNew(senderSigner.getPublicKey())
                                            .addNested(new TransactionBuilder(1).build(networkID, senderSigner))
                                            .build(networkID, senderSigner);

        validate(tx);
    }

    @Test
    public void success() throws Exception {

        byte[] pk = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff").getPublicKey();

        AccountProperties.setProperty(sender, new BalanceProperty(1000L));
        Transaction tx = RegistrationBuilder.createNew(pk).build(networkID, senderSigner);

        validate(tx);
        Assert.assertNotNull(ledger.getAccount(new AccountID(pk)));
    }
}