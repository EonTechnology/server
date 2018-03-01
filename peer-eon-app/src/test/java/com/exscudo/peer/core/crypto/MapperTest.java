package com.exscudo.peer.core.crypto;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import com.exscudo.peer.MockSigner;
import com.exscudo.peer.core.crypto.mapper.SignedObjectMapper;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.eon.tx.builders.DepositBuilder;
import com.exscudo.peer.eon.tx.builders.PaymentBuilder;
import com.exscudo.peer.eon.tx.builders.QuorumBuilder;
import com.exscudo.peer.eon.tx.builders.RegistrationBuilder;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("SpellCheckingInspection")
public class MapperTest {

    @Before
    public void setUp() throws Exception {
        CryptoProvider cryptoProvider = new CryptoProvider(new SignedObjectMapper(new BlockID(0L)));
        CryptoProvider.init(cryptoProvider);
    }

    @Test
    public void transaction_bencode() throws Exception {

        Transaction tran = new Transaction();
        tran.setSenderID(new AccountID(13245L));
        tran.setVersion(1);
        byte[] bytes = tran.getBytes();
        String s = new String(bytes);

        assertEquals(
                "D10:ATTACHMENTDE8:DEADLINEI0E3:FEEI0E7:NETWORK23:EON-B-22222-22222-2222J6:SENDER21:EON-XXE22-22222-22JSY9:TIMESTAMPI0E4:TYPEI0E7:VERSIONI1EE",
                s);
    }

    @Test
    public void transaction_v2_bencode() throws Exception {

        Transaction tran = new Transaction();
        tran.setSenderID(new AccountID(13245L));
        tran.setVersion(2);
        byte[] bytes = tran.getBytes();
        String s = new String(bytes);

        assertEquals(
                "D10:ATTACHMENTDE8:DEADLINEI0E3:FEEI0E7:NETWORK23:EON-B-22222-22222-2222J6:SENDER21:EON-XXE22-22222-22JSY9:TIMESTAMPI0E4:TYPEI0E7:VERSIONI2EE",
                s);
    }

    @Test
    public void transaction_bencode_payment() throws Exception {
        MockSigner signer = new MockSigner(123L);

        Transaction tran = PaymentBuilder.createNew(100L, new AccountID(12345L)).validity(12345, 3600).build(signer);

        byte[] bytes = tran.getBytes();
        String s = new String(bytes);

        assertEquals(
                "D10:ATTACHMENTD6:AMOUNTI100E9:RECIPIENT21:EON-T3E22-22222-22JUJE8:DEADLINEI3600E3:FEEI10E7:NETWORK23:EON-B-22222-22222-2222J6:SENDER21:EON-RMNF4-KLGQ7-9Y65X9:TIMESTAMPI12345E4:TYPEI200E7:VERSIONI1EE",
                s);
    }

    @Test
    public void transaction_bencode_register() throws Exception {
        MockSigner signer = new MockSigner(123L);

        Transaction tran =
                RegistrationBuilder.createNew(signer.getPublicKey()).validity(12345 + 60, 3600).build(signer);

        byte[] bytes = tran.getBytes();
        String s = new String(bytes);

        assertEquals(
                "D10:ATTACHMENTD21:EON-RMNF4-KLGQ7-9Y65X64:DDF121B99504BC3CD18CABFDAAF0334D16D1D7403226FA924557DA9B3A0F4642E8:DEADLINEI3600E3:FEEI10E7:NETWORK23:EON-B-22222-22222-2222J6:SENDER21:EON-RMNF4-KLGQ7-9Y65X9:TIMESTAMPI12405E4:TYPEI100E7:VERSIONI1EE",
                s);
    }

    @Test
    public void transaction_bencode_deposit() throws Exception {
        MockSigner signer = new MockSigner(123L);

        Transaction tran = DepositBuilder.createNew(999L).validity(12345, 3600).build(signer);

        byte[] bytes = tran.getBytes();
        String s = new String(bytes);

        assertEquals(
                "D10:ATTACHMENTD6:AMOUNTI999EE8:DEADLINEI3600E3:FEEI10E7:NETWORK23:EON-B-22222-22222-2222J6:SENDER21:EON-RMNF4-KLGQ7-9Y65X9:TIMESTAMPI12345E4:TYPEI300E7:VERSIONI1EE",
                s);
    }

    @Test
    public void transaction_quorum() throws Exception {
        MockSigner signer = new MockSigner(123L);

        Transaction tran = QuorumBuilder.createNew(50).quorumForType(200, 70).validity(12345, 3600).build(signer);

        byte[] bytes = tran.getBytes();
        String s = new String(bytes);

        assertEquals(
                "D10:ATTACHMENTD3:200I70E3:ALLI50EE8:DEADLINEI3600E3:FEEI10E7:NETWORK23:EON-B-22222-22222-2222J6:SENDER21:EON-RMNF4-KLGQ7-9Y65X9:TIMESTAMPI12345E4:TYPEI410E7:VERSIONI1EE",
                s);
    }

    @Test
    public void block_bencode() throws Exception {

        Block bl = new Block();
        bl.setSenderID(new AccountID(12345L));
        bl.setGenerationSignature(new byte[] {4, 5, 6});
        bl.setPreviousBlock(new BlockID(0));

        List<Transaction> rxSet = new ArrayList<>();
        for (byte i = 0; i < 10; i++) {
            Transaction tx = new Transaction();
            tx.setSignature(new byte[] {i});

            rxSet.add(tx);
        }
        bl.setTransactions(rxSet);
        bl.setSnapshot("");

        byte[] bytes = bl.getBytes();
        String s = new String(bytes);

        assertEquals(
                "D19:GENERATIONSIGNATURE6:0405069:GENERATOR21:EON-T3E22-22222-22JUJ4:PREV23:EON-B-22222-22222-2222J8:SNAPSHOT0:9:TIMESTAMPI0E12:TRANSACTIONSL23:EON-T-22222-226WV-563ZX23:EON-T-22222-22GQB-DHWAN23:EON-T-22222-22WV9-QRF2Q23:EON-T-22222-26T7T-3MANT23:EON-T-22222-2AJP7-W332U23:EON-T-22222-2AME2-NM8DJ23:EON-T-22222-2N95H-BFPXL23:EON-T-22222-2NLH7-VLEQL23:EON-T-22222-2SD5F-7W6FX23:EON-T-22222-2WCBD-PKT6SE7:VERSIONI0EE",
                s);
    }
}
