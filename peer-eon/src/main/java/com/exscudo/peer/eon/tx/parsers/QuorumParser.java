package com.exscudo.peer.eon.tx.parsers;

import java.util.Map;

import com.exscudo.peer.core.TransactionType;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.actions.FeePaymentAction;
import com.exscudo.peer.eon.ledger.actions.QuorumAction;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.tx.ITransactionParser;

public class QuorumParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {
        Map<String, Object> data = transaction.getData();
        if (data == null) {
            throw new ValidateException("Attachment of unknown type.");
        }

        int quorum;
        try {
            quorum = Integer.parseInt(String.valueOf(data.get("all")));
        } catch (Exception e) {
            throw new ValidateException("Attachment of unknown type.");
        }
        if (quorum < ValidationModeProperty.MIN_QUORUM || quorum > ValidationModeProperty.MAX_QUORUM) {
            throw new ValidateException("Illegal quorum.");
        }

        QuorumAction action = new QuorumAction(transaction.getSenderID(), quorum);
        for (Map.Entry<String, Object> entry : data.entrySet()) {

            if (entry.getKey().equals("all")) {
                continue;
            }

            int type;
            int quorumTyped;
            try {
                type = Integer.parseInt(String.valueOf(entry.getKey()));
                quorumTyped = Integer.parseInt(String.valueOf(entry.getValue()));
            } catch (Exception e) {
                throw new ValidateException("Attachment of unknown type.");
            }

            if (!TransactionType.contains(type)) {
                throw new ValidateException("Unknown transaction type " + entry.getKey());
            }
            if (quorumTyped < ValidationModeProperty.MIN_QUORUM || quorumTyped > ValidationModeProperty.MAX_QUORUM) {
                throw new ValidateException("Illegal quorum for transaction type " + entry.getKey());
            }
            if (quorumTyped == quorum) {
                throw new ValidateException("Use all quorum for transaction type " + entry.getKey());
            }
            action.setQuorum(type, quorumTyped);
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()), action
        };
    }
}
