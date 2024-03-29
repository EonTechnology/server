package org.eontechnology.and.peer.eon;

import static org.mockito.Mockito.spy;

import org.eontechnology.and.peer.TestLedger;
import org.eontechnology.and.peer.core.IFork;
import org.eontechnology.and.peer.core.common.TimeProvider;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.core.middleware.LedgerActionContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public abstract class AbstractTransactionTest {
  @Rule public ExpectedException expectedException = ExpectedException.none();
  protected TimeProvider timeProvider;
  protected ILedger ledger;
  protected IFork fork;
  protected BlockID networkID;

  @Before
  public void setUp() throws Exception {

    timeProvider = spy(new TimeProvider());
    ledger = spy(new TestLedger());
    networkID = new BlockID(0L);

    fork = Mockito.mock(IFork.class);
    Mockito.when(fork.getGenesisBlockID()).thenReturn(networkID);
  }

  protected void validate(ITransactionParser parser, Transaction tx) throws Exception {

    ILedgerAction[] actions = parser.parse(tx);

    ILedger newLedger = ledger;
    for (ILedgerAction action : actions) {
      newLedger = action.run(newLedger, new LedgerActionContext(timeProvider.get()));
    }
  }
}
