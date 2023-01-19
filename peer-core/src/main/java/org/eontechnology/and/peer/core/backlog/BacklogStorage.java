package org.eontechnology.and.peer.core.backlog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.eontechnology.and.peer.core.common.TransactionComparator;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.TransactionID;

/** Storage for unconfirmed transactions */
class BacklogStorage {

  private volatile ConcurrentHashMap<TransactionID, Transaction> transactions =
      new ConcurrentHashMap<>();
  private volatile ConcurrentSkipListSet<TransactionID> keys =
      new ConcurrentSkipListSet<>(new LongComparator(transactions));

  public synchronized boolean put(Transaction transaction) throws ValidateException {
    TransactionID id = transaction.getID();
    transactions.put(id, transaction);
    keys.add(id);
    return true;
  }

  public synchronized Transaction remove(TransactionID id) {
    if (transactions.containsKey(id)) {
      keys.remove(id);
      return transactions.remove(id);
    }
    return null;
  }

  public Transaction get(TransactionID id) {
    return transactions.get(id);
  }

  public boolean contains(TransactionID id) {
    return transactions.containsKey(id);
  }

  public Iterator<TransactionID> iterator() {
    return keys.iterator();
  }

  public synchronized List<Transaction> copyAndClear() {
    ArrayList<Transaction> copy = new ArrayList<>(transactions.values());
    keys.clear();
    transactions.clear();

    return copy;
  }

  public int size() {
    return transactions.size();
  }

  private static class LongComparator implements Comparator<TransactionID> {

    private volatile ConcurrentHashMap<TransactionID, Transaction> transactions;
    private transient Comparator<Transaction> comparatorTran = new TransactionComparator();

    LongComparator(ConcurrentHashMap<TransactionID, Transaction> transactions) {

      this.transactions = transactions;
    }

    @Override
    public int compare(TransactionID a, TransactionID b) {
      Transaction aTx = this.transactions.get(a);
      Transaction bTx = this.transactions.get(b);
      return comparatorTran.compare(aTx, bTx);
    }
  }
}
