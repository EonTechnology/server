package com.exscudo.eon.IT;

import java.util.Iterator;

import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.ledger.ILedger;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LedgerIteratorTestIT {

    private static String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";

    @Test
    public void test_1() throws Exception {

        TimeProvider mockTimeProvider = Mockito.mock(TimeProvider.class);
        PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);

        ILedger ledger = ctx.ledgerProvider.getLedger(ctx.blockchain.getLastBlock());

        int accCount = 0;
        for (Account ignored : ledger) {
            accCount++;
        }

        Assert.assertEquals("Account count in test genesis block", 10, accCount);

        for (int i = 0; i < accCount - 1; i++) {

            Iterator<Account> iterator = ledger.iterator();

            Account acc = iterator.next();
            for (int k = 0; k < i; k++) {
                acc = iterator.next();
            }

            Iterator<Account> iterator2 = ledger.iterator(acc.getID());

            Account acc2 = iterator2.next();
            System.out.println(acc.getID().getValue() + " - " + acc2.getID().getValue());
            //Assert.assertEquals(acc.getID().getValue(), acc2.getID().getValue());

            while (iterator.hasNext() && iterator2.hasNext()) {
                acc = iterator.next();
                acc2 = iterator2.next();
                System.out.println(acc.getID().getValue() + " - " + acc2.getID().getValue());
                //  Assert.assertEquals(acc.getID().getValue(), acc2.getID().getValue());
            }

            //Assert.assertFalse(iterator.hasNext());
            //Assert.assertFalse(iterator2.hasNext());

            System.out.println();
        }
    }
}
