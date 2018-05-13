package com.exscudo.peer.core.middleware.rules;

import java.util.Map;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.ITimeProvider;
import com.exscudo.peer.core.common.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class SignatureValidationRule implements IValidationRule {
    private final IFork fork;
    private final ITimeProvider timeProvider;

    public SignatureValidationRule(IFork fork, ITimeProvider timeProvider) {
        this.fork = fork;
        this.timeProvider = timeProvider;
    }

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {

        Account sender = ledger.getAccount(tx.getSenderID());
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }

        if (!tx.isVerified()) {

            if (!fork.verifySignature(tx, tx.getSignature(), sender, timeProvider.get())) {
                return ValidationResult.error(new IllegalSignatureException());
            }

            if (tx.getConfirmations() != null) {

                for (Map.Entry<String, Object> entry : tx.getConfirmations().entrySet()) {

                    AccountID id;
                    byte[] signature;
                    try {
                        id = new AccountID(entry.getKey());
                        signature = Format.convert(String.valueOf(entry.getValue()));
                    } catch (Exception e) {
                        return ValidationResult.error("Invalid format.");
                    }

                    if (tx.getSenderID().equals(id)) {
                        return ValidationResult.error("Duplicates sender signature.");
                    }

                    Account account = ledger.getAccount(id);
                    if (account == null) {
                        return ValidationResult.error("Unknown account " + id.toString());
                    }

                    if (!fork.verifySignature(tx, signature, account, timeProvider.get())) {
                        return ValidationResult.error(new IllegalSignatureException(id.toString()));
                    }
                }
            }

            tx.setVerifiedState();
        }

        return ValidationResult.success;
    }
}
