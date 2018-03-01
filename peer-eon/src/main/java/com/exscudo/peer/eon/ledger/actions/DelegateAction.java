package com.exscudo.peer.eon.ledger.actions;

import java.util.ArrayList;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.ledger.state.VotePollsProperty;

public class DelegateAction implements ILedgerAction {

    private final ArrayList<Item> delegates = new ArrayList<>();
    private final AccountID accountID;

    public DelegateAction(AccountID accountID) {
        this.accountID = accountID;
    }

    public void addDelegate(AccountID id, int weight) {
        delegates.add(new Item(id, weight));
    }

    private ValidationResult canRun(ILedger ledger) {

        Account sender = ledger.getAccount(accountID);
        if (sender == null) {
            return ValidationResult.error("Unknown account.");
        }

        ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);

        int delegatesCount = validationMode.delegatesEntrySet().size();
        int deltaWeight = 0;
        for (Item di : delegates) {

            if (di.id.equals(accountID)) {

                // cancellation of rights is not possible
                if (validationMode.isPublic()) {
                    return ValidationResult.error("Changing rights is prohibited.");
                }

                if (di.weight == validationMode.getBaseWeight()) {
                    return ValidationResult.error("Value already set.");
                }

                deltaWeight += di.weight - validationMode.getBaseWeight();
            } else {

                Account account = ledger.getAccount(di.id);
                if (account == null) {
                    return ValidationResult.error("Unknown account " + di.id.toString());
                }
                ValidationModeProperty accountValidationMode = AccountProperties.getValidationMode(account);
                if (accountValidationMode.isPublic()) {
                    return ValidationResult.error("A public account can not act as a delegate.");
                }

                if (validationMode.containWeightForAccount(di.id)) {
                    int oldWeight = validationMode.getWeightForAccount(di.id);

                    if (di.weight == oldWeight) {
                        return ValidationResult.error("Value already set.");
                    }
                    if (di.weight == 0) {
                        delegatesCount--;
                    }

                    deltaWeight += di.weight - oldWeight;
                } else {
                    if (di.weight == 0) {
                        return ValidationResult.error("Value already set.");
                    }

                    delegatesCount++;
                    deltaWeight += di.weight;
                }
            }
        }

        if ((validationMode.getMaxWeight() + deltaWeight) < ValidationModeProperty.MAX_QUORUM) {
            return ValidationResult.error("Incorrect distribution of votes.");
        }

        if (delegatesCount > ValidationModeProperty.MAX_DELEGATES) {
            return ValidationResult.error("The number of delegates has reached the limit.");
        }

        return ValidationResult.success;
    }

    @Override
    public ILedger run(ILedger ledger, TransactionContext context) throws ValidateException {
        ValidationResult r = canRun(ledger);
        if (r.hasError) {
            throw r.cause;
        }

        ILedger newLedger = ledger;

        Account sender = newLedger.getAccount(accountID);
        ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);

        for (Item delegate : delegates) {

            if (delegate.id.equals(sender.getID())) {
                validationMode.setBaseWeight(delegate.weight);
            } else {

                Account target = newLedger.getAccount(delegate.id);
                VotePollsProperty targetVoter = AccountProperties.getVoter(target);

                targetVoter.setPoll(accountID, delegate.weight);
                targetVoter.setTimestamp(context.getTimestamp());
                target = AccountProperties.setProperty(target, targetVoter);
                newLedger = newLedger.putAccount(target);

                validationMode.setWeightForAccount(delegate.id, delegate.weight);
            }
        }

        validationMode.setTimestamp(context.getTimestamp());
        sender = AccountProperties.setProperty(sender, validationMode);
        newLedger = newLedger.putAccount(sender);

        return newLedger;
    }

    static class Item {
        public final AccountID id;
        public final int weight;

        public Item(AccountID id, int weight) {
            this.id = id;
            this.weight = weight;
        }
    }
}
