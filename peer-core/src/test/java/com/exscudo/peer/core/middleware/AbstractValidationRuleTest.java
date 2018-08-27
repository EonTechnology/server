package com.exscudo.peer.core.middleware;

import static org.mockito.Mockito.spy;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.IAccountHelper;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.ILedger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public abstract class AbstractValidationRuleTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected TimeProvider timeProvider = spy(new TimeProvider());
    protected int timestamp;
    protected IFork fork;
    protected IAccountHelper accountHelper;
    protected ILedger ledger = Mockito.spy(new TestLedger());

    protected BlockID networkID = new BlockID(0L);

    protected abstract IValidationRule getValidationRule();

    @Before
    public void setUp() throws Exception {

        accountHelper = Mockito.mock(IAccountHelper.class);
        fork = Mockito.mock(IFork.class);
        Mockito.when(fork.getGenesisBlockID()).thenReturn(networkID);
        Mockito.when(accountHelper.verifySignature(ArgumentMatchers.any(),
                                                   ArgumentMatchers.any(),
                                                   ArgumentMatchers.any(),
                                                   ArgumentMatchers.anyInt())).thenReturn(true);

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
