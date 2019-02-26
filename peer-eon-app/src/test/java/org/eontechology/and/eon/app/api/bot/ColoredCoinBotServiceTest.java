package org.eontechology.and.eon.app.api.bot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.eontechology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.ledger.LedgerProvider;
import org.eontechology.and.peer.eon.ledger.AccountProperties;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ColoredCoinBotServiceTest {

    private ColoredCoinBotService service;
    private Account targetAccount;

    @Before
    public void setup() throws Exception {

        service = Mockito.spy(new ColoredCoinBotService(mock(LedgerProvider.class), mock(IBlockchainProvider.class)));

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
        coloredCoin.setEmitMode(ColoredCoinEmitMode.PRESET);
        coloredCoin.setMoneySupply(10000L);
        coloredCoin.setAttributes(new ColoredCoinProperty.Attributes(8, 1));
        targetAccount = AccountProperties.setProperty(targetAccount, coloredCoin);

        ColoredCoinBotService.Info info = service.getInfo(id.toString());
        assertEquals(info.state, ColoredCoinBotService.State.OK);
        assertEquals(info.decimal, Integer.valueOf(8));
        assertEquals(info.supply, Long.valueOf(10000L));
        assertEquals(info.timestamp, Integer.valueOf(1));
        assertFalse(info.auto);
    }

    @Test
    public void getInformation_not_found() throws Exception {
        AccountID id = new AccountID(1L);
        ColoredCoinBotService.Info info = service.getInfo(id.toString());
        assertEquals(info.state, ColoredCoinBotService.State.Unauthorized);
        assertNull(info.decimal);
        assertNull(info.supply);
    }

    @Test
    public void getInformation_illegal_state() throws Exception {
        AccountID id = new AccountID(1L);
        targetAccount = new Account(id);

        ColoredCoinBotService.Info info = service.getInfo(id.toString());
        assertEquals(info.state, ColoredCoinBotService.State.Unauthorized);
        assertNull(info.decimal);
        assertNull(info.supply);
    }
}
