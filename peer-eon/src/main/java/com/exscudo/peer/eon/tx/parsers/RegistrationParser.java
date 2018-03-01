package com.exscudo.peer.eon.tx.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.actions.FeePaymentAction;
import com.exscudo.peer.eon.ledger.actions.RegistrationAction;
import com.exscudo.peer.eon.tx.ITransactionParser;

public class RegistrationParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {
        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 1) {
            throw new ValidateException("Attachment of unknown type.");
        }

        RegistrationAction registration = new RegistrationAction();
        for (Map.Entry<String, Object> entry : data.entrySet()) {

            AccountID id;
            try {
                id = new AccountID(entry.getKey());
            } catch (Exception e) {
                throw new ValidateException("Attachment of unknown type.");
            }

            byte[] publicKey;
            try {
                publicKey = Format.convert(String.valueOf(entry.getValue()));
                if (publicKey.length != 32) {
                    throw new IllegalArgumentException();
                }
                if (!id.equals(new AccountID(publicKey))) {
                    throw new IllegalArgumentException();
                }
            } catch (Exception e) {
                throw new ValidateException("Attachment of unknown type.");
            }

            registration.addAccount(id, publicKey);
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()), registration
        };
    }
}
