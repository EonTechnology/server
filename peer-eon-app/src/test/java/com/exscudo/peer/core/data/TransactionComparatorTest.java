package com.exscudo.peer.core.data;

import java.util.Arrays;

import com.exscudo.peer.MockSigner;
import com.exscudo.peer.core.common.TransactionComparator;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.crypto.mapper.SignedObjectMapper;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.eon.tx.builders.DepositBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TransactionComparatorTest {

    MockSigner signer;

    @Before
    public void setUp() throws Exception {
        CryptoProvider cryptoProvider = new CryptoProvider(new SignedObjectMapper(new BlockID(0L)));
        CryptoProvider.init(cryptoProvider);
        signer = new MockSigner(123L);
    }

    @Test
    public void check_fee() throws Exception {
        Transaction tx1 = DepositBuilder.createNew(123L).forFee(1L).validity(12345, 3600).build(signer);
        Transaction tx2 = DepositBuilder.createNew(321L).forFee(2L).validity(12345, 3600).build(signer);

        Transaction[] transactions = new Transaction[] {tx1, tx2};

        Arrays.sort(transactions, new TransactionComparator());

        Assert.assertEquals(tx2, transactions[0]);
    }

    @Test
    public void check_timestamp() throws Exception {
        Transaction tx1 = DepositBuilder.createNew(123L).forFee(1L).validity(12345, 3600).build(signer);
        Transaction tx2 = DepositBuilder.createNew(321L).forFee(1L).validity(12000, 3600).build(signer);

        Transaction[] transactions = new Transaction[] {tx1, tx2};

        Arrays.sort(transactions, new TransactionComparator());

        Assert.assertEquals(tx2, transactions[0]);
    }

    @Test
    public void check_id() throws Exception {
        Transaction tx1 = DepositBuilder.createNew(123L).forFee(1L).validity(12345, 3600).build(signer);
        Transaction tx2 = DepositBuilder.createNew(321L).forFee(1L).validity(12345, 3600).build(signer);

        Transaction[] transactions = new Transaction[] {tx1, tx2};

        Arrays.sort(transactions, new TransactionComparator());

        if (tx1.getID().getValue() > tx2.getID().getValue()) {
            Assert.assertEquals(tx2, transactions[0]);
        } else {
            Assert.assertEquals(tx1, transactions[0]);
        }
    }
}
