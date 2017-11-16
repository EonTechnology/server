package com.exscudo.peer.eon.stubs;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exscudo.peer.DefaultBacklog;
import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.ForkProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.services.ITransactionMapper;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.Instance;
import org.junit.Before;
import org.junit.Test;

public class SyncTransactionServiceTest {

	private Long lastBlockIdLong = 8597994084945064665L;
	private String lastBlockIdStr = "EON-B-TQWY2-R54QK-NGR3K";
	private long ignoreTrId = -8775790525992972917L;
	private String ignoreTrIdStr = "EON-T-DEM8H-3F23K-FEAHV";
	private long unconfTrId1 = 7983673130013968962L;
	private long unconfTrId2 = 6300362882568955079L;
	private Transaction ignoreTr;
	private Transaction unconfTr1;
	private Transaction unconfTr2;

	private IBacklogService backlog;

	private ExecutionContext mockContext;

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

		IBlockchainService blockchain = mock(IBlockchainService.class);
		when(blockchain.getLastBlock()).thenReturn(mockBlock);
		when(blockchain.transactionMapper()).thenReturn(mock(ITransactionMapper.class));

		Fork mockFork = mock(Fork.class);
		when(mockFork.isPassed(anyInt())).thenReturn(false);
		when(mockFork.getGenesisBlockID()).thenReturn(12345L);
		ForkProvider.init(mockFork);

		Instance peer = mock(Instance.class);
		when(peer.getBacklogService()).thenReturn(backlog);
		when(peer.getBlockchainService()).thenReturn(blockchain);

		mockContext = mock(ExecutionContext.class);
		when(mockContext.getInstance()).thenReturn(peer);
		when(mockContext.getCurrentFork()).thenReturn(mockFork);
	}

	@Test(expected = RemotePeerException.class)
	public void getTransactions_with_wrong_lastblockid_should_throw() throws Exception {
		SyncTransactionService sts = new SyncTransactionService(mockContext);
		sts.getTransactions("xxx", new String[0]);
	}

	@Test(expected = RemotePeerException.class)
	public void getTransactions_with_bad_ignorelist_should_throw() throws Exception {
		SyncTransactionService sts = new SyncTransactionService(mockContext);
		sts.getTransactions(lastBlockIdStr, new String[] { "bad_tr_id" });
	}

	@Test
	public void getTransactions_with_notlast_lastblockid_should_return_no_trs() throws Exception {
		SyncTransactionService sts = new SyncTransactionService(mockContext);
		Transaction[] trs = sts.getTransactions("EON-B-NA7Z7-YSK86-7BWKU", new String[0]);
		assertEquals(0, trs.length);
	}

	@Test
	public void getTransactions_should_return_unconfirmed_trs() throws Exception {
		SyncTransactionService sts = new SyncTransactionService(mockContext);
		Transaction[] trs = sts.getTransactions(lastBlockIdStr, new String[0]);
		assertEquals(3, trs.length);
		assertEquals(unconfTr1, trs[0]);
		assertEquals(unconfTr2, trs[1]);
		assertEquals(ignoreTr, trs[2]);
	}

	@Test
	public void getTransactions_shouldnt_return_ignore_trs() throws Exception {
		SyncTransactionService sts = new SyncTransactionService(mockContext);
		Transaction[] trs = sts.getTransactions(lastBlockIdStr, new String[] { ignoreTrIdStr });
		assertEquals(2, trs.length);
		assertEquals(unconfTr1, trs[0]);
		assertEquals(unconfTr2, trs[1]);
	}
}