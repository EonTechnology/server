package com.exscudo.peer.core.middleware;

import static org.mockito.Mockito.spy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.ILedger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public abstract class AbstractValidationRuleTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected TimeProvider timeProvider = spy(new TimeProvider());
    protected int timestamp;
    protected IFork fork;
    protected ILedger ledger = Mockito.spy(new TestLedger());

    protected BlockID networkID = new BlockID(0L);

    protected abstract IValidationRule getValidationRule();

    @Before
    public void setUp() throws Exception {

        fork = Mockito.mock(IFork.class);
        Mockito.when(fork.getMaxNoteLength(ArgumentMatchers.anyInt())).thenReturn(Constant.TRANSACTION_NOTE_MAX_LENGTH);
        Mockito.when(fork.getGenesisBlockID()).thenReturn(networkID);
        Mockito.when(fork.verifySignature(ArgumentMatchers.any(),
                                          ArgumentMatchers.any(),
                                          ArgumentMatchers.any(),
                                          ArgumentMatchers.anyInt())).thenReturn(true);
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

        timestamp = timeProvider.get();
        Mockito.when(timeProvider.get()).thenReturn(timestamp);
    }

    protected void validate(Transaction tx) throws Exception {
        ValidationResult r = getValidationRule().validate(tx, ledger);
        if (r.hasError) {
            throw r.cause;
        }
    }
}
