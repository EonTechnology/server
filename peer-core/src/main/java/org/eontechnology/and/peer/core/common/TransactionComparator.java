package org.eontechnology.and.peer.core.common;

import java.util.Comparator;
import org.eontechnology.and.peer.core.data.Transaction;

/**
 * Performs a transaction comparison
 *
 * <p>
 *
 * <p>The main criterion for comparison is the value calculated based on the transaction fee and the
 * data length (conditionally, the cost per kilobyte of data). At the second stage, the transactions
 * time stamp is compared. Transactions created earlier have a higher priority. The last criterion
 * is the transaction identifier.
 */
public class TransactionComparator implements Comparator<Transaction> {
  private static final long SIZE_UNIT = 1024L;

  @Override
  public int compare(Transaction a, Transaction b) {

    // Comparison by fee of byte
    int aBytesLen = a.getLength();
    int bBytesLen = b.getLength();

    long aRate = a.getFee() * SIZE_UNIT / aBytesLen;
    long bRate = b.getFee() * SIZE_UNIT / bBytesLen;

    // Reversed - bigger is better and is earlier in the queue
    int res = Long.compare(aRate, bRate) * -1;

    // Comparison by time stamp
    if (res == 0) {
      res = Integer.compare(a.getTimestamp(), b.getTimestamp());
    }

    // Comparison by id
    if (res == 0) {
      res = Long.compare(a.getID().getValue(), b.getID().getValue());
    }

    return res;
  }
}
