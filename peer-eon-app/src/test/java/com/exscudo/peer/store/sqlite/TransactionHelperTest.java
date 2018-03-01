package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.*;

import java.util.List;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.core.storage.utils.AccountHelper;
import com.exscudo.peer.core.storage.utils.TransactionHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// TODO: review
public class TransactionHelperTest {
    private Storage storage;
    private TransactionHelper transactionHelper;
    private AccountHelper accountHelper;

    @Before
    public void setUp() throws Exception {
        storage = ConnectionUtils.create("/com/exscudo/peer/store/sqlite/transactions_test.sql");
        transactionHelper = storage.getTransactionHelper();
        accountHelper = storage.getAccountHelper();
    }

    @After
    public void after() throws Exception {
        transactionHelper = null;
        accountHelper = null;
        storage.destroy();
    }

    @Test
    public void get() throws Exception {

        Transaction tx = transactionHelper.get(new TransactionID(4381492506058027276L));
        assertEquals(tx.getSenderID().toString(), "EON-GKQXZ-7DMS8-QL65R");
        assertEquals(tx.getDeadline(), 60);
        assertEquals(tx.getFee(), 5);
        assertEquals(tx.getData().get("recipient"), "EON-WEUCY-TPM29-EK53X");
        assertEquals(tx.getData().get("amount"), 50L);
        assertNull(tx.getReference());
        assertEquals(tx.getType(), 2);
        assertEquals(tx.getTimestamp(), 1503654156);
    }

    @Test
    public void getNonExistent() throws Exception {
        assertNull(transactionHelper.get(new TransactionID(-1)));
    }

    @Test
    public void contains() throws Exception {
        assertTrue(transactionHelper.contains(new TransactionID(2641518845407277113L)));
        assertFalse(transactionHelper.contains(new TransactionID(-1)));
    }

    @Test
    public void findByAccount() throws Exception {
        AccountID accountID = new AccountID(4085011828883941788L);
        List<Transaction> list = accountHelper.getTransactions(accountID, 0, 100);
        assertEquals(3, list.size());

        assertEquals(4381492506058027276L, list.get(0).getID().getValue());
        assertEquals(6265336003274207499L, list.get(1).getID().getValue());
        assertEquals(2641518845407277113L, list.get(2).getID().getValue());

        assertTrue(list.get(0).getTimestamp() > list.get(1).getTimestamp());
        assertTrue(list.get(1).getTimestamp() > list.get(2).getTimestamp());
    }

    @Test
    public void findByAccountLimit() throws Exception {
        AccountID accountID = new AccountID(4085011828883941788L);
        List<Transaction> list = accountHelper.getTransactions(accountID, 0, 2);
        assertEquals(2, list.size());

        assertEquals(4381492506058027276L, list.get(0).getID().getValue());
        assertEquals(6265336003274207499L, list.get(1).getID().getValue());

        assertTrue(list.get(0).getTimestamp() > list.get(1).getTimestamp());
    }

    @Test
    public void findByAccountLimitFrom() throws Exception {
        AccountID accountID = new AccountID(4085011828883941788L);
        List<Transaction> list = accountHelper.getTransactions(accountID, 1, 2);
        assertEquals(2, list.size());

        assertEquals(6265336003274207499L, list.get(0).getID().getValue());
        assertEquals(2641518845407277113L, list.get(1).getID().getValue());

        assertTrue(list.get(0).getTimestamp() > list.get(1).getTimestamp());
    }
}
