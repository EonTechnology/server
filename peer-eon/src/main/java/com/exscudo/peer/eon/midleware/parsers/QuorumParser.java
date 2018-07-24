package com.exscudo.peer.eon.midleware.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.actions.FeePaymentAction;
import com.exscudo.peer.eon.midleware.actions.QuorumAction;
import com.exscudo.peer.tx.TransactionType;

public class QuorumParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        Map<String, Object> data = transaction.getData();
        if (data == null) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        if (!(data.get("all") instanceof Long)) {
            throw new ValidateException(Resources.QUORUM_INVALID_FORMAT);
        }

        long quorum = (long) data.get("all");
        if (quorum < ValidationModeProperty.MIN_QUORUM || quorum > ValidationModeProperty.MAX_QUORUM) {
            throw new ValidateException(Resources.QUORUM_OUT_OF_RANGE);
        }

        QuorumAction action = new QuorumAction(transaction.getSenderID(), (int) quorum);
        for (Map.Entry<String, Object> entry : data.entrySet()) {

            if (entry.getKey().equals("all")) {
                continue;
            }

            int type;
            try {
                type = Integer.parseInt(String.valueOf(entry.getKey()));
                if (!TransactionType.contains(type)) {
                    throw new ValidateException(Resources.TRANSACTION_TYPE_UNKNOWN);
                }
            } catch (NumberFormatException e) {
                throw new ValidateException(Resources.TRANSACTION_TYPE_INVALID_FORMAT);
            }

            if (!(entry.getValue() instanceof Long)) {
                throw new ValidateException(Resources.QUORUM_INVALID_FORMAT);
            }

            long quorumTyped = (long) entry.getValue();
            if (quorumTyped < ValidationModeProperty.MIN_QUORUM || quorumTyped > ValidationModeProperty.MAX_QUORUM) {
                throw new ValidateException(Resources.QUORUM_OUT_OF_RANGE);
            }

            if (quorumTyped == quorum) {
                throw new ValidateException(Resources.QUORUM_ILLEGAL_USAGE);
            }
            action.setQuorum(type, (int) quorumTyped);
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()), action
        };
    }

    @Override
    public AccountID getRecipient(Transaction transaction) throws ValidateException {
        return null;
    }
}
