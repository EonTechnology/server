package org.eontechology.and.peer.core.crypto;

import java.util.HashMap;

import org.eontechology.and.peer.core.crypto.mapper.ObjectMapper;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.data.identifier.BlockID;
import org.eontechology.and.peer.core.data.identifier.TransactionID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BencodeFormatterTest {

    private Transaction tx;
    private ObjectMapper mapper;
    private BencodeFormatter formatter;

    @Before
    public void setUp() throws Exception {
        tx = new Transaction();
        tx.setVersion(1);
        tx.setFee(10);
        tx.setTimestamp(1);
        tx.setDeadline(3600);
        tx.setType(100);
        tx.setSenderID(new AccountID(100L));

        mapper = new ObjectMapper(new BlockID(0L));
        formatter = new BencodeFormatter();
    }

    @Test
    public void test_reference() {

        tx.setReference(new TransactionID(132L));

        byte[] bytes = formatter.getBytes(mapper.convert(tx));
        Assert.assertEquals(
                "D10:ATTACHMENTDE8:DEADLINEI3600E3:FEEI10E7:NETWORK23:EON-B-22222-22222-2222J9:REFERENCE23:EON-T-66222-22222-2224L6:SENDER21:EON-65222-22222-222LK9:TIMESTAMPI1E4:TYPEI100E7:VERSIONI1EE",
                new String(bytes));
    }

    @Test
    public void test_attachment() {

        tx.setData(new HashMap<String, Object>() {
            {
                put("int", 123);
                put("str", "data");
            }
        });

        byte[] bytes = formatter.getBytes(mapper.convert(tx));
        Assert.assertEquals(
                "D10:ATTACHMENTD3:INTI123E3:STR4:DATAE8:DEADLINEI3600E3:FEEI10E7:NETWORK23:EON-B-22222-22222-2222J6:SENDER21:EON-65222-22222-222LK9:TIMESTAMPI1E4:TYPEI100E7:VERSIONI1EE",
                new String(bytes));
    }

    @Test
    public void test_note() {

        tx.setNote("test");

        byte[] bytes = formatter.getBytes(mapper.convert(tx));
        Assert.assertEquals(
                "D10:ATTACHMENTDE8:DEADLINEI3600E3:FEEI10E7:NETWORK23:EON-B-22222-22222-2222J4:NOTE4:TEST6:SENDER21:EON-65222-22222-222LK9:TIMESTAMPI1E4:TYPEI100E7:VERSIONI1EE",
                new String(bytes));
    }

    @Test
    public void test_nested() {
        Transaction tx2 = new Transaction();
        tx2.setVersion(1);
        tx2.setFee(10);
        tx2.setTimestamp(1);
        tx2.setDeadline(3600);
        tx2.setType(100);
        tx2.setSenderID(new AccountID(100L));
        tx2.setSignature(new byte[64]);

        tx.setNestedTransactions(new HashMap<String, Transaction>() {
            {
                put(tx2.getID().toString(), tx2);
            }
        });

        byte[] bytes = formatter.getBytes(mapper.convert(tx));
        Assert.assertEquals(
                "D10:ATTACHMENTDE5:BILLSD23:EON-T-32222-2NVPE-L3ZGVD10:ATTACHMENTDE8:DEADLINEI3600E3:FEEI10E7:NETWORK23:EON-B-22222-22222-2222J6:SENDER21:EON-65222-22222-222LK9:TIMESTAMPI1E4:TYPEI100E7:VERSIONI1EEE8:DEADLINEI3600E3:FEEI10E7:NETWORK23:EON-B-22222-22222-2222J6:SENDER21:EON-65222-22222-222LK9:TIMESTAMPI1E4:TYPEI100E7:VERSIONI1EE",
                new String(bytes));
    }

    @Test
    public void test_payer() {

        tx.setPayer(new AccountID(123L));

        byte[] bytes = formatter.getBytes(mapper.convert(tx));
        Assert.assertEquals(
                "D10:ATTACHMENTDE8:DEADLINEI3600E3:FEEI10E7:NETWORK23:EON-B-22222-22222-2222J5:PAYER21:EON-V5222-22222-22JXK6:SENDER21:EON-65222-22222-222LK9:TIMESTAMPI1E4:TYPEI100E7:VERSIONI1EE",
                new String(bytes));
    }
}
