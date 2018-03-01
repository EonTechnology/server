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

public class QuorumAction implements ILedgerAction {
    private final ArrayList<Item> quorumTyped = new ArrayList<>();
    private final AccountID accountID;
    private final int quorum;

    public QuorumAction(AccountID accountID, int quorum) {
        this.accountID = accountID;
        this.quorum = quorum;
    }

    private ValidationResult canRun(ILedger ledger) {
        Account sender = ledger.getAccount(accountID);
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }

        int maxWeight = ValidationModeProperty.MAX_WEIGHT;
        ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);
        if (validationMode.isMultiFactor()) {
            maxWeight = validationMode.getMaxWeight();
        }

        if (maxWeight < quorum) {
            return ValidationResult.error("Unable to set quorum.");
        }
        for (Item item : quorumTyped) {
            if (maxWeight < item.quorum) {
                return ValidationResult.error("Unable to set quorum for transaction type " + item.type);
            }
        }

        return ValidationResult.success;
    }

    @Override
    public ILedger run(ILedger ledger, TransactionContext context) throws ValidateException {
        ValidationResult r = canRun(ledger);
        if (r.hasError) {
            throw r.cause;
        }

        Account account = ledger.getAccount(accountID);
        ValidationModeProperty validationMode = AccountProperties.getValidationMode(account);

        validationMode.setQuorum(quorum);
        for (Item qi : quorumTyped) {
            validationMode.setQuorum(qi.type, qi.quorum);
        }

        validationMode.setTimestamp(context.getTimestamp());
        account = AccountProperties.setProperty(account, validationMode);

        ILedger newLedger = ledger.putAccount(account);

        return newLedger;
    }

    public void setQuorum(int type, int quorum) {
        quorumTyped.add(new Item(type, quorum));
    }

    static class Item {
        public final int type;
        public final int quorum;

        public Item(int type, int quorum) {
            this.type = type;
            this.quorum = quorum;
        }
    }
}
