package com.exscudo.peer.eon.transactions;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.exscudo.peer.MockSigner;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.data.mapper.TransactionMapper;
import com.exscudo.peer.eon.ColoredCoinID;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.tx.builders.ColoredCoinRegistrationBuilder;
import com.exscudo.peer.eon.tx.builders.ColoredCoinSupplyBuilder;
import com.exscudo.peer.eon.tx.builders.ColoredPaymentBuilder;
import com.exscudo.peer.eon.tx.builders.DelegateBuilder;
import com.exscudo.peer.eon.tx.builders.DepositBuilder;
import com.exscudo.peer.eon.tx.builders.PaymentBuilder;
import com.exscudo.peer.eon.tx.builders.PublicationBuilder;
import com.exscudo.peer.eon.tx.builders.QuorumBuilder;
import com.exscudo.peer.eon.tx.builders.RegistrationBuilder;
import com.exscudo.peer.eon.tx.builders.RejectionBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TransactionConverterTest {
    private MockSigner signer;
    private Bencode bencode = new Bencode();

    @Before
    public void setUp() {
        signer = new MockSigner(123L);
        CryptoProvider cryptoProvider = Mockito.mock(CryptoProvider.class);
        CryptoProvider.init(cryptoProvider);
    }

    @Test
    public void transaction_payment_referenced() throws Exception {
        Transaction tran =
                PaymentBuilder.createNew(100L, new AccountID(12345L)).forFee(1L).validity(12345, 3600).build(signer);
        tran.setReference(new TransactionID(-1));

        checkTransaction(tran,
                         "d10:attachmentd6:amounti100e9:recipient21:EON-T3E22-22222-22JUJe8:deadlinei3600e3:feei1e21:referencedTransaction23:EON-T-ZZZZZ-ZZZZZ-ZZZ9J6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei200e7:versioni1ee");
    }

    @Test
    public void transaction_confirmations() throws Exception {
        Transaction tran = DepositBuilder.createNew(999L).validity(12345, 3600).build(signer, new ISigner[] {signer});

        checkTransaction(tran,
                         "d10:attachmentd6:amounti999ee13:confirmationsd21:EON-RMNF4-KLGQ7-9Y65X128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c57437e8:deadlinei3600e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei300e7:versioni1ee");
    }

    //
    // Transaction Types
    //

    @Test
    public void transaction_payment() throws Exception {
        Transaction tran = PaymentBuilder.createNew(100L, new AccountID(12345L)).validity(12345, 3600).build(signer);

        checkTransaction(tran,
                         "d10:attachmentd6:amounti100e9:recipient21:EON-T3E22-22222-22JUJe8:deadlinei3600e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei200e7:versioni1ee");
    }

    @Test
    public void transaction_payment_v2() throws Exception {
        Transaction tran = PaymentBuilder.createNew(100L, new AccountID(12345L)).validity(12345, 3600).build(signer);

        checkTransaction(tran,
                         "d10:attachmentd6:amounti100e9:recipient21:EON-T3E22-22222-22JUJe8:deadlinei3600e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei200e7:versioni1ee");
    }

    @Test
    public void transaction_account_registration() throws Exception {
        Transaction tran =
                RegistrationBuilder.createNew(signer.getPublicKey()).validity(12345 + 60, 3600).build(signer);

        checkTransaction(tran,
                         "d10:attachmentd21:EON-RMNF4-KLGQ7-9Y65X64:ddf121b99504bc3cd18cabfdaaf0334d16d1d7403226fa924557da9b3a0f4642e8:deadlinei3600e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12405e4:typei100e7:versioni1ee");
    }

    @Test
    public void transaction_deposit() throws Exception {
        Transaction tran = DepositBuilder.createNew(999L).validity(12345, 3600).build(signer);

        checkTransaction(tran,
                         "d10:attachmentd6:amounti999ee8:deadlinei3600e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei300e7:versioni1ee");
    }

    @Test
    public void transaction_quorum() throws Exception {
        Transaction tx = QuorumBuilder.createNew(80)
                                      .quorumForType(TransactionType.Quorum, 70)
                                      .validity(12345, 3600)
                                      .build(signer);

        checkTransaction(tx,
                         "d10:attachmentd3:410i70e3:alli80ee8:deadlinei3600e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei410e7:versioni1ee");
    }

    @Test
    public void transaction_delegate() throws Exception {
        Transaction tx = DelegateBuilder.createNew(new AccountID(1L), 10).validity(12345, 3600).build(signer);

        checkTransaction(tx,
                         "d10:attachmentd21:EON-32222-22222-22J2Ji10ee8:deadlinei3600e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei400e7:versioni1ee");
    }

    @Test
    public void transaction_rejection() throws Exception {
        Transaction tx = RejectionBuilder.createNew(new AccountID(1L)).validity(12345, 3600).build(signer);

        checkTransaction(tx,
                         "d10:attachmentd7:account21:EON-32222-22222-22J2Je8:deadlinei3600e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei420e7:versioni1ee");
    }

    @Test
    public void transaction_account_publication() throws Exception {
        Transaction tx =
                PublicationBuilder.createNew("112233445566778899qqwweerrttyyuu").validity(12345, 3600).build(signer);

        checkTransaction(tx,
                         "d10:attachmentd4:seed32:112233445566778899qqwweerrttyyuue8:deadlinei3600e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei430e7:versioni1ee");
    }

    @Test
    public void transaction_colored_coin_registration() throws Exception {
        Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000L, 2).validity(12345, 3600).build(signer);

        checkTransaction(tx,
                         "d10:attachmentd12:decimalPointi2e8:emissioni1000ee8:deadlinei3600e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei500e7:versioni1ee");
    }

    @Test
    public void transaction_colored_coin_payment() throws Exception {
        Transaction tx = ColoredPaymentBuilder.createNew(100L, new ColoredCoinID(9999999L), new AccountID(1111L))
                                              .validity(12345, 3600)
                                              .build(signer);

        checkTransaction(tx,
                         "d10:attachmentd6:amounti100e5:color23:EON-C-ZM7KB-22222-22JBK9:recipient21:EON-R4322-22222-222DKe8:deadlinei3600e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei510e7:versioni1ee");
    }

    @Test
    public void transaction_colored_coin_supply() throws Exception {
        Transaction tx = ColoredCoinSupplyBuilder.createNew(2000L).validity(12345, 3600).build(signer);

        checkTransaction(tx,
                         "d10:attachmentd11:moneySupplyi2000ee8:deadlinei3600e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei520e7:versioni1ee");
    }

    private void checkTransaction(Transaction tran, String data) throws IllegalArgumentException {
        Map<String, Object> map = TransactionMapper.convert(tran);
        byte[] bytes = bencode.encode(map);
        String s = new String(bytes);

        assertEquals(data, s);

        Map<String, Object> decoded = bencode.decode(data.getBytes(), Type.DICTIONARY);

        Transaction tx = TransactionMapper.convert(decoded);

        Assert.assertEquals(tran.getType(), tx.getType());
        Assert.assertEquals(tran.getTimestamp(), tx.getTimestamp());
        Assert.assertEquals(tran.getDeadline(), tx.getDeadline());
        Assert.assertEquals(tran.getReference(), tx.getReference());
        Assert.assertEquals(tran.getSenderID(), tx.getSenderID());
        Assert.assertEquals(tran.getFee(), tx.getFee());
        Assert.assertEquals(Format.convert(bencode.encode(tran.getData())),
                            Format.convert(bencode.encode(tx.getData())));
        Assert.assertEquals(Format.convert(tran.getSignature()), Format.convert(tx.getSignature()));
        Assert.assertEquals(tran.getID(), tx.getID());
        Assert.assertEquals(tran.getLength(), tx.getLength());
        Assert.assertEquals(tran.getVersion(), tx.getVersion());
        Assert.assertEquals(tran.getConfirmations(), tx.getConfirmations());
    }
}
