package com.exscudo.eon.cfg;

import java.util.HashMap;

import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.TransactionHandler;
import com.exscudo.peer.eon.transactions.handlers.AccountPublicationHandler;
import com.exscudo.peer.eon.transactions.handlers.AccountRegistrationHandler;
import com.exscudo.peer.eon.transactions.handlers.DelegateHandler;
import com.exscudo.peer.eon.transactions.handlers.DepositRefillHandler;
import com.exscudo.peer.eon.transactions.handlers.DepositWithdrawHandler;
import com.exscudo.peer.eon.transactions.handlers.OrdinaryPaymentHandler;
import com.exscudo.peer.eon.transactions.handlers.QuorumHandler;
import com.exscudo.peer.eon.transactions.handlers.RejectionHandler;
import com.exscudo.peer.eon.transactions.rules.AccountPublicationValidationRule;
import com.exscudo.peer.eon.transactions.rules.AccountRegistrationValidationRule;
import com.exscudo.peer.eon.transactions.rules.AttachmentValidationRule;
import com.exscudo.peer.eon.transactions.rules.BaseValidationRuleV1;
import com.exscudo.peer.eon.transactions.rules.BaseValidationRuleV1_2;
import com.exscudo.peer.eon.transactions.rules.ConfirmationsValidationRule;
import com.exscudo.peer.eon.transactions.rules.DelegateValidationRule;
import com.exscudo.peer.eon.transactions.rules.DepositRefillValidationRule;
import com.exscudo.peer.eon.transactions.rules.DepositWithdrawValidationRule;
import com.exscudo.peer.eon.transactions.rules.IValidationRule;
import com.exscudo.peer.eon.transactions.rules.OrdinaryPaymentValidationRule;
import com.exscudo.peer.eon.transactions.rules.QuorumValidationRule;
import com.exscudo.peer.eon.transactions.rules.RejectionValidationRule;
import com.exscudo.peer.eon.transactions.rules.SenderValidationRule;
import com.exscudo.peer.store.sqlite.Storage;
import com.exscudo.peer.store.sqlite.utils.SettingHelper;
import com.exscudo.peer.store.sqlite.utils.SettingName;

/**
 * Initializing the current fork
 */
public class ForkInitializer {

	public static final Fork.Item[] items = new Fork.Item[] {

			new Fork.Item(1, "2017-10-04T12:00:00.00Z", "2017-11-15T12:00:00.00Z",
					new TransactionHandler(new HashMap<Integer, ITransactionHandler>() {
						private static final long serialVersionUID = 0L;

						{
							put(TransactionType.AccountRegistration, new AccountRegistrationHandler());
							put(TransactionType.OrdinaryPayment, new OrdinaryPaymentHandler());
							put(TransactionType.DepositWithdraw, new DepositWithdrawHandler());
							put(TransactionType.DepositRefill, new DepositRefillHandler());
						}
					}, new IValidationRule[] { new BaseValidationRuleV1(), new SenderValidationRule(),
							new AttachmentValidationRule(new HashMap<Integer, IValidationRule>() {
								private static final long serialVersionUID = 0L;

								{
									put(TransactionType.AccountRegistration, new AccountRegistrationValidationRule());
									put(TransactionType.OrdinaryPayment, new OrdinaryPaymentValidationRule());
									put(TransactionType.DepositWithdraw, new DepositWithdrawValidationRule());
									put(TransactionType.DepositRefill, new DepositRefillValidationRule());
								}
							}) }),
					1),

			new Fork.Item(2, "2017-11-15T12:00:00.00Z", "2017-12-15T12:00:00.00Z",
					new TransactionHandler(new HashMap<Integer, ITransactionHandler>() {
						private static final long serialVersionUID = 0L;

						{
							put(TransactionType.AccountRegistration, new AccountRegistrationHandler());
							put(TransactionType.OrdinaryPayment, new OrdinaryPaymentHandler());
							put(TransactionType.DepositWithdraw, new DepositWithdrawHandler());
							put(TransactionType.DepositRefill, new DepositRefillHandler());
						}
					}, new IValidationRule[] { new BaseValidationRuleV1_2(), new SenderValidationRule(),
							new AttachmentValidationRule(new HashMap<Integer, IValidationRule>() {
								private static final long serialVersionUID = 1L;

								{
									put(TransactionType.AccountRegistration, new AccountRegistrationValidationRule());
									put(TransactionType.OrdinaryPayment, new OrdinaryPaymentValidationRule());
									put(TransactionType.DepositWithdraw, new DepositWithdrawValidationRule());
									put(TransactionType.DepositRefill, new DepositRefillValidationRule());
								}
							}) }),
					2),

			new Fork.Item(3, "2017-12-15T12:00:00.00Z", "2018-01-25T12:00:00.00Z",
					new TransactionHandler(new HashMap<Integer, ITransactionHandler>() {
						private static final long serialVersionUID = 1L;

						{
							put(TransactionType.AccountRegistration, new AccountRegistrationHandler());
							put(TransactionType.OrdinaryPayment, new OrdinaryPaymentHandler());
							put(TransactionType.DepositWithdraw, new DepositWithdrawHandler());
							put(TransactionType.DepositRefill, new DepositRefillHandler());
							put(TransactionType.Delegate, new DelegateHandler());
							put(TransactionType.Quorum, new QuorumHandler());
							put(TransactionType.Rejection, new RejectionHandler());
							put(TransactionType.AccountPublication, new AccountPublicationHandler());
						}
					}, new IValidationRule[] { new BaseValidationRuleV1_2(), new SenderValidationRule(),
							new ConfirmationsValidationRule(),
							new AttachmentValidationRule(new HashMap<Integer, IValidationRule>() {
								private static final long serialVersionUID = 1L;

								{
									put(TransactionType.AccountRegistration, new AccountRegistrationValidationRule());
									put(TransactionType.OrdinaryPayment, new OrdinaryPaymentValidationRule());
									put(TransactionType.DepositWithdraw, new DepositWithdrawValidationRule());
									put(TransactionType.DepositRefill, new DepositRefillValidationRule());
									put(TransactionType.Delegate, new DelegateValidationRule());
									put(TransactionType.Quorum, new QuorumValidationRule());
									put(TransactionType.Rejection, new RejectionValidationRule());
									put(TransactionType.AccountPublication, new AccountPublicationValidationRule());
								}
							}) }),
					2) };

	public static Fork init(Storage storage) {
		long genesisBlockID = Long
				.parseLong(SettingHelper.getValue(storage.getConnection(), SettingName.genesisBlockID), 10);
		Fork fork = new Fork(genesisBlockID, items);
		return fork;
	}

}
