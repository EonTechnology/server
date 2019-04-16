package org.eontechnology.and.eon.app.api.peer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eontechnology.and.eon.app.api.DefaultBacklog;
import org.eontechnology.and.peer.core.IFork;
import org.eontechnology.and.peer.core.backlog.IBacklog;
import org.eontechnology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechnology.and.peer.core.common.TimeProvider;
import org.eontechnology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.data.identifier.TransactionID;
import org.eontechnology.and.peer.core.env.ExecutionContext;
import org.junit.Before;
import org.junit.Test;

public class SyncTransactionServiceTest {

    private BlockID lastBlockIdLong = new BlockID(8597994084945064665L);
    private String lastBlockIdStr = "EON-B-TQWY2-R54QK-NGR3K";
    private TransactionID ignoreTrId = new TransactionID(-8775790525992972917L);
    private String ignoreTrIdStr = "EON-T-DEM8H-3F23K-FEAHV";
    private TransactionID unconfTrId1 = new TransactionID(7983673130013968962L);
    private TransactionID unconfTrId2 = new TransactionID(6300362882568955079L);
    private Transaction ignoreTr;
    private Transaction unconfTr1;
    private Transaction unconfTr2;

    private IBacklog backlog;

    private ExecutionContext mockContext;
    private IBlockchainProvider blockchain;
    private IFork mockFork;
    private SyncTransactionService sts;

    @Before
    public void setup() throws Exception {

        unconfTr1 = mock(Transaction.class);
        when(unconfTr1.getID()).thenReturn(unconfTrId1);
        unconfTr2 = mock(Transaction.class);
        when(unconfTr2.getID()).thenReturn(unconfTrId2);
        ignoreTr = mock(Transaction.class);
        when(ignoreTr.getID()).thenReturn(ignoreTrId);

        backlog = new DefaultBacklog();
        backlog.put(unconfTr1);
        backlog.put(unconfTr2);
        backlog.put(ignoreTr);

        Block mockBlock = mock(Block.class);
        when(mockBlock.getID()).thenReturn(lastBlockIdLong);

        blockchain = mock(IBlockchainProvider.class);
        when(blockchain.getLastBlock()).thenReturn(mockBlock);

        mockFork = mock(IFork.class);
        when(mockFork.isPassed(anyInt())).thenReturn(false);
        when(mockFork.getGenesisBlockID()).thenReturn(new BlockID(12345L));

        mockContext = mock(ExecutionContext.class);

        sts = new SyncTransactionService(mockFork, mock(TimeProvider.class), backlog, blockchain);
    }

    @Test(expected = RemotePeerException.class)
    public void getTransactions_with_wrong_lastblockid_should_throw() throws Exception {
        sts.getTransactions("xxx", new String[0]);
    }

    @Test(expected = RemotePeerException.class)
    public void getTransactions_with_bad_ignorelist_should_throw() throws Exception {
        sts.getTransactions(lastBlockIdStr, new String[] {"bad_tr_id"});
    }

    @Test
    public void getTransactions_with_notlast_lastblockid_should_return_no_trs() throws Exception {
        Transaction[] trs = sts.getTransactions("EON-B-NA7Z7-YSK86-7BWKU", new String[0]);
        assertEquals(0, trs.length);
    }

    @Test
    public void getTransactions_should_return_unconfirmed_trs() throws Exception {
        Transaction[] trs = sts.getTransactions(lastBlockIdStr, new String[0]);
        assertEquals(3, trs.length);
        assertEquals(unconfTr1, trs[0]);
        assertEquals(unconfTr2, trs[1]);
        assertEquals(ignoreTr, trs[2]);
    }

    @Test
    public void getTransactions_shouldnt_return_ignore_trs() throws Exception {
        Transaction[] trs = sts.getTransactions(lastBlockIdStr, new String[] {ignoreTrIdStr});
        assertEquals(2, trs.length);
        assertEquals(unconfTr1, trs[0]);
        assertEquals(unconfTr2, trs[1]);
    }
}