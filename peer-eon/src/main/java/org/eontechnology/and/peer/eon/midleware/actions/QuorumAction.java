package org.eontechnology.and.peer.eon.midleware.actions;

import java.util.ArrayList;

import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.LedgerActionContext;
import org.eontechnology.and.peer.eon.ledger.AccountProperties;
import org.eontechnology.and.peer.eon.ledger.state.ValidationModeProperty;
import org.eontechnology.and.peer.eon.midleware.Resources;

public class QuorumAction implements ILedgerAction {
    private final ArrayList<Item> quorumTyped = new ArrayList<>();
    private final AccountID accountID;
    private final int quorum;

    public QuorumAction(AccountID accountID, int quorum) {
        this.accountID = accountID;
        this.quorum = quorum;
    }

    private void ensureValidState(ILedger ledger) throws ValidateException {
        Account sender = ledger.getAccount(accountID);
        if (sender == null) {
            throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
        }

        int maxWeight = ValidationModeProperty.MAX_WEIGHT;
        ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);
        if (validationMode.isMultiFactor()) {
            maxWeight = validationMode.getMaxWeight();
        }

        if (maxWeight < quorum) {
            throw new ValidateException(Resources.QUORUM_CAN_NOT_BE_CHANGED);
        }
        for (Item item : quorumTyped) {
            if (maxWeight < item.quorum) {
                throw new ValidateException(Resources.QUORUM_FOR_TYPE_CAN_NOT_BE_CHANGED);
            }
        }
    }

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

        ensureValidState(ledger);

        Account account = ledger.getAccount(accountID);
        ValidationModeProperty validationMode = AccountProperties.getValidationMode(account);

        validationMode.setQuorum(quorum);
        for (Item qi : quorumTyped) {
            validationMode.setQuorum(qi.type, qi.quorum);
        }

        validationMode.setTimestamp(context.getTimestamp());
        account = AccountProperties.setProperty(account, validationMode);

        return ledger.putAccount(account);
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
