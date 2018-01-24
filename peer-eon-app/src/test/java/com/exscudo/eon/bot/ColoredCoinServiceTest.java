package com.exscudo.eon.bot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.Account;
import com.exscudo.peer.eon.state.ColoredCoin;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import com.exscudo.peer.store.sqlite.Storage;

public class ColoredCoinServiceTest {

	private ColoredCoinService service;
	private IAccount targetAccount;

	@Before
	public void setup() throws Exception {
		Storage mockStorage = mock(Storage.class);
		service = Mockito.spy(new ColoredCoinService(mockStorage));

		doAnswer(new Answer<IAccount>() {
			@Override
			public IAccount answer(InvocationOnMock invocation) throws Throwable {
				return targetAccount;
			}
		}).when(service).getColoredAccount(anyString());
	}

	@Test
	public void getInformation_OK() throws Exception {
		targetAccount = new Account(1L);
		ColoredCoin coloredCoin = new ColoredCoin();
		coloredCoin.setMoneySupply(10000L);
		coloredCoin.setDecimalPoint(8);
		AccountProperties.setColoredCoinRegistrationData(targetAccount, coloredCoin);

		ColoredCoinService.Info info = service.getInfo(Format.ID.accountId(1L), Integer.MAX_VALUE);
		assertEquals(info.state, ColoredCoinService.State.OK);
		assertEquals(info.decimalPoint, Integer.valueOf(8));
		assertEquals(info.moneySupply, Long.valueOf(10000L));
	}

	@Test
	public void getInformation_not_found() throws Exception {
		ColoredCoinService.Info info = service.getInfo(Format.ID.accountId(1L), Integer.MAX_VALUE);
		assertEquals(info.state, ColoredCoinService.State.Unauthorized);
		assertNull(info.decimalPoint);
		assertNull(info.moneySupply);
	}

	@Test
	public void getInformation_old_info() throws Exception {
		targetAccount = new Account(1L);
		ColoredCoin coloredCoin = new ColoredCoin();
		coloredCoin.setMoneySupply(10000L);
		coloredCoin.setDecimalPoint(8);
		coloredCoin.setTimestamp(1);
		AccountProperties.setColoredCoinRegistrationData(targetAccount, coloredCoin);

		ColoredCoinService.Info info = service.getInfo(Format.ID.accountId(1L), 0);
		assertEquals(info.state, ColoredCoinService.State.Unauthorized);
		assertNull(info.decimalPoint);
		assertNull(info.moneySupply);
	}

	@Test
	public void getInformation_illegal_state() throws Exception {
		targetAccount = new Account(1L);

		ColoredCoinService.Info info = service.getInfo(Format.ID.accountId(1L), Integer.MAX_VALUE);
		assertEquals(info.state, ColoredCoinService.State.Unauthorized);
		assertNull(info.decimalPoint);
		assertNull(info.moneySupply);
	}

}
