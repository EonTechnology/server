package com.exscudo.eon.bot;

public class AccountServiceTest {
	//
	// private Blockchain ds;
	// private ILedger ls;
	// private Storage cn;
	// private BacklogBuffer bl;
	// private IAccount existingAccount;
	// private AccountBalance existingAccountBalance;
	// private AccountDeposit existingAccountDeposit;
	// private Transaction processingTr;
	//
	// private String existingAccountIdStr;
	// private long existingAccountIdLong;
	// private final long existingAccountBalanceValue = 100500;
	// private final long existingAccountDepositValue = 666999;
	// private String processingAccountIdStr;
	// private long processingAccountIdLong;
	// private Long processingTrId;
	// private ArrayList<Long> processingTrIds;
	//
	// private final String notExistingAccountIdStr = "EON-78XV6-PTFBB-3G76K";
	//
	// @Before
	// public void setup() {
	// ds = mock(Blockchain.class);
	// ls = mock(ILedger.class);
	// cn = mock(Storage.class);
	// bl = mock(BacklogBuffer.class);
	//
	// existingAccount = mock(IAccount.class);
	// existingAccountIdStr = "EON-6MBZW-3LNVV-VWJ3Z";
	// existingAccountIdLong = 1040185985462150756L;
	// existingAccountBalance = new AccountBalance(existingAccountIdLong,
	// existingAccountBalanceValue);
	// existingAccountDeposit = new AccountDeposit(existingAccountIdLong,
	// existingAccountDepositValue, 1);
	//
	// processingAccountIdStr = "EON-CCGUV-VZMB6-6P9PN";
	// processingAccountIdLong = 8831710189422721354L;
	// processingTr = mock(Transaction.class);
	// processingTrId = 1L;
	// processingTrIds = new ArrayList<>();
	// processingTrIds.add(processingTrId);
	//
	// when(ds.getState()).thenReturn(ls);
	// when(ls.existAccount(existingAccountIdLong)).thenReturn(true);
	// when(ls.getAccount(existingAccountIdLong)).thenReturn(existingAccount);
	// when(ls.getAccount(processingAccountIdLong)).thenReturn(null);
	// when(existingAccount.getBalance()).thenReturn(existingAccountBalance);
	// when(existingAccount.getDeposit()).thenReturn(existingAccountDeposit);
	// when(ds.getConnector()).thenReturn(cn);
	// when(cn.getBacklog()).thenReturn(bl);
	// when(bl.findAll()).thenReturn(processingTrIds.iterator());
	// when(bl.get(processingTrId)).thenReturn(processingTr);
	// when(processingTr.getSenderID()).thenReturn(existingAccountIdLong);
	// when(processingTr.getType()).thenReturn(TransactionType.AccountRegistration.getType());
	// when(processingTr.getSubType()).thenReturn(TransactionType.AccountRegistration.getSubType());
	// when(processingTr.getData()).thenReturn(new HashMap<String, Object>() {
	// {
	// put(processingAccountIdStr, "");
	// }
	// });
	// }
	//
	// @Test
	// public void getState_for_existing_account_should_return_OK() throws Exception
	// {
	// AccountService ac = new AccountService(ds);
	// assertEquals(AccountService.State.OK, ac.getState(existingAccountIdStr));
	// }
	//
	// @Test
	// public void getState_for_processing_account_should_return_Processing() throws
	// Exception {
	// AccountService ac = new AccountService(ds);
	// assertEquals(AccountService.State.Processing,
	// ac.getState(processingAccountIdStr));
	// }
	//
	// @Test
	// public void getState_for_notexisting_account_should_return_NotFound() throws
	// Exception {
	// AccountService ac = new AccountService(ds);
	// assertEquals(AccountService.State.NotFound,
	// ac.getState(notExistingAccountIdStr));
	// }
	//
	// @Test
	// public void
	// getInformation_for_existing_account_should_return_correct_balances() throws
	// Exception {
	// AccountService ac = new AccountService(ds);
	// AccountService.Info info = ac.getInformation(existingAccountIdStr);
	// assertEquals(AccountService.State.OK, info.state);
	// assertEquals(existingAccountBalanceValue, info.amount);
	// assertEquals(existingAccountDepositValue, info.deposit);
	// }
	//
	// @Test
	// public void
	// getInformation_for_notexisting_account_should_return_null_balances() throws
	// Exception {
	// AccountService ac = new AccountService(ds);
	// AccountService.Info info = ac.getInformation(notExistingAccountIdStr);
	// assertEquals(AccountService.State.NotFound, info.state);
	// assertEquals(0, info.amount);
	// assertEquals(0, info.deposit);
	// }
	//
	// @Test
	// public void
	// getInformation_for_processing_account_should_return_null_balances() throws
	// Exception {
	// AccountService ac = new AccountService(ds);
	// AccountService.Info info = ac.getInformation(processingAccountIdStr);
	// assertEquals(AccountService.State.Processing, info.state);
	// assertEquals(0, info.amount);
	// assertEquals(0, info.deposit);
	// }

}