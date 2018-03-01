package com.exscudo.peer.eon.tx.rules;

import com.exscudo.peer.core.common.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ledger.AccountProperties;

public class SignatureValidationRule implements IValidationRule {

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

        Account sender = ledger.getAccount(tx.getSenderID());
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }

        if (!tx.verifySignature(AccountProperties.getRegistration(sender).getPublicKey())) {
            return ValidationResult.error(new IllegalSignatureException());
        }

        return ValidationResult.success;
    }
}
