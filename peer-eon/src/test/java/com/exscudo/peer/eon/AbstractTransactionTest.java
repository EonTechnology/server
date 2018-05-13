package com.exscudo.peer.eon;

import static org.mockito.Mockito.spy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import com.exscudo.peer.TestLedger;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.core.middleware.LedgerActionContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public abstract class AbstractTransactionTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    protected TimeProvider timeProvider;
    protected ILedger ledger;
    protected IFork fork;
    protected BlockID networkID;

    protected abstract ITransactionParser getParser();

    @Before
    public void setUp() throws Exception {

        timeProvider = spy(new TimeProvider());
        ledger = spy(new TestLedger());
        networkID = new BlockID(0L);

        fork = Mockito.mock(IFork.class);
        Mockito.when(fork.getGenesisBlockID()).thenReturn(networkID);
        Mockito.when(fork.getDifficulty(ArgumentMatchers.any(Transaction.class), ArgumentMatchers.anyInt()))
               .thenAnswer(new Answer<Integer>() {
                   @Override
                   public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                       Transaction tx = invocationOnMock.getArgument(0);

                       try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                           try (ObjectOutput out = new ObjectOutputStream(stream)) {
                               out.writeObject(tx);
                               out.flush();
                               return stream.toByteArray().length;
                           }
                       } catch (IOException e) {
                           throw new RuntimeException(e);
                       }
                   }
               });
    }

    protected void validate(Transaction tx) throws Exception {

        ILedgerAction[] actions = getParser().parse(tx);

        ILedger newLedger = ledger;
        for (ILedgerAction action : actions) {
            newLedger = action.run(newLedger, new LedgerActionContext(timeProvider.get(), fork));
        }
    }
}
