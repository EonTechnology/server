package com.exscudo.peer.core.crypto;

import java.util.Locale;

import com.exscudo.peer.core.crypto.mapper.ObjectMapper;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import org.junit.Assert;
import org.junit.Test;

public class BencodeFormatterTest {

    @Test
    public void test_bencode_locale() {

        Locale locale = Locale.getDefault();
        try {
            Locale trLocale = new Locale("tr", "TR");
            Locale.setDefault(trLocale);

            ObjectMapper mapper = new ObjectMapper(new BlockID(0L));
            BencodeFormatter formatter = new BencodeFormatter();

            Transaction tx = new Transaction();
            tx.setVersion(1);
            tx.setFee(10);
            tx.setTimestamp(1);
            tx.setDeadline(3600);
            tx.setType(100);
            tx.setSenderID(new AccountID(100L));

            byte[] bytes = formatter.getBytes(mapper.convert(tx));
            Assert.assertEquals(new String(bytes),
                                "D10:ATTACHMENTDE8:DEADLINEI3600E3:FEEI10E7:NETWORK23:EON-B-22222-22222-2222J6:SENDER21:EON-65222-22222-222LK9:TIMESTAMPI1E4:TYPEI100E7:VERSIONI1EE");
        } finally {
            Locale.setDefault(locale);
        }
    }
}
