package com.exscudo.eon.bot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.exscudo.peer.core.blockchain.IBlockchainService;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ColoredCoinServiceTest {

    private ColoredCoinService service;
    private Account targetAccount;

    @Before
    public void setup() throws Exception {
        ExecutionContext mockStorage = mock(ExecutionContext.class);
        service = Mockito.spy(new ColoredCoinService(mockStorage,
                                                     mock(LedgerProvider.class),
                                                     mock(IBlockchainService.class)));

        doAnswer(new Answer<Account>() {
            @Override
            public Account answer(InvocationOnMock invocation) throws Throwable {
                return targetAccount;
            }
        }).when(service).getColoredAccount(anyString());
    }

    @Test
    public void getInformation_OK() throws Exception {
        AccountID id = new AccountID(1L);
        targetAccount = new Account(id);
        ColoredCoinProperty coloredCoin = new ColoredCoinProperty();
        coloredCoin.setMoneySupply(10000L);
        coloredCoin.setDecimalPoint(8);
        coloredCoin.setTimestamp(1);
        targetAccount = AccountProperties.setProperty(targetAccount, coloredCoin);

        ColoredCoinService.Info info = service.getInfo(id.toString());
        assertEquals(info.state, ColoredCoinService.State.OK);
        assertEquals(info.decimalPoint, Integer.valueOf(8));
        assertEquals(info.moneySupply, Long.valueOf(10000L));
        assertEquals(info.timestamp, Integer.valueOf(1));
    }

    @Test
    public void getInformation_not_found() throws Exception {
        AccountID id = new AccountID(1L);
        ColoredCoinService.Info info = service.getInfo(id.toString());
        assertEquals(info.state, ColoredCoinService.State.Unauthorized);
        assertNull(info.decimalPoint);
        assertNull(info.moneySupply);
    }

    @Test
    public void getInformation_illegal_state() throws Exception {
        AccountID id = new AccountID(1L);
        targetAccount = new Account(id);

        ColoredCoinService.Info info = service.getInfo(id.toString());
        assertEquals(info.state, ColoredCoinService.State.Unauthorized);
        assertNull(info.decimalPoint);
        assertNull(info.moneySupply);
    }
}
