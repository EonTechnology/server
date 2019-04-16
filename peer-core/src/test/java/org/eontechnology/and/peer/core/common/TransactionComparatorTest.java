package org.eontechnology.and.peer.core.common;

import java.util.Arrays;

import org.eontechnology.and.peer.core.Builder;
import org.eontechnology.and.peer.core.Signer;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TransactionComparatorTest {

    protected TimeProvider timeProvider;
    private ISigner signer;
    private BlockID networkID = new BlockID(0L);

    @Before
    public void setUp() throws Exception {

        timeProvider = Mockito.spy(new TimeProvider());
        signer = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    }

    @Test
    public void check_fee() throws Exception {

        int length = 100;

        Transaction tx1 = Builder.newTransaction(timeProvider).forFee(1L).build(networkID, signer);
        tx1.setLength(length);

        Transaction tx2 = Builder.newTransaction(timeProvider).forFee(2L).build(networkID, signer);
        tx2.setLength(length);

        Transaction[] transactions = new Transaction[] {tx1, tx2};

        Arrays.sort(transactions, new TransactionComparator());

        Assert.assertEquals(tx2, transactions[0]);
    }

    @Test
    public void check_timestamp() throws Exception {

        int length1 = 100;

        Mockito.when(timeProvider.get()).thenReturn(12345);
        Transaction tx1 = Builder.newTransaction(timeProvider).forFee(1L).build(networkID, signer);
        tx1.setLength(length1);

        Mockito.when(timeProvider.get()).thenReturn(12000);
        Transaction tx2 = Builder.newTransaction(timeProvider).forFee(1L).build(networkID, signer);
        tx2.setLength(length1 + 1);

        Transaction[] transactions = new Transaction[] {tx1, tx2};

        Arrays.sort(transactions, new TransactionComparator());

        Assert.assertEquals(tx2, transactions[0]);
    }

    @Test
    public void check_id() throws Exception {

        int length = 100;
        Transaction tx1 = Builder.newTransaction(timeProvider).forFee(1L).build(networkID, signer);
        tx1.setLength(length);

        Transaction tx2 = Builder.newTransaction(timeProvider).forFee(1L).build(networkID, signer);
        tx2.setLength(length);

        Transaction[] transactions = new Transaction[] {tx1, tx2};

        Arrays.sort(transactions, new TransactionComparator());

        if (tx1.getID().getValue() > tx2.getID().getValue()) {
            Assert.assertEquals(tx2, transactions[0]);
        } else {
            Assert.assertEquals(tx1, transactions[0]);
        }
    }
}
