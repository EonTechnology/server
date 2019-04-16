package org.eontechnology.and.peer.core.middleware.rules;

import java.util.Map;

import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.common.IAccountHelper;
import org.eontechnology.and.peer.core.common.ITimeProvider;
import org.eontechnology.and.peer.core.common.exceptions.IllegalSignatureException;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.IValidationRule;
import org.eontechnology.and.peer.core.middleware.ValidationResult;

public class SignatureValidationRule implements IValidationRule {
    private final ITimeProvider timeProvider;
    private final IAccountHelper accountHelper;

    public SignatureValidationRule(ITimeProvider timeProvider, IAccountHelper accountHelper) {
        this.timeProvider = timeProvider;
        this.accountHelper = accountHelper;
    }

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {

        Account sender = ledger.getAccount(tx.getSenderID());
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }

        if (!tx.isVerified()) {

            if (!accountHelper.verifySignature(tx, tx.getSignature(), sender, timeProvider.get())) {
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

                    if (!accountHelper.verifySignature(tx, signature, account, timeProvider.get())) {
                        return ValidationResult.error(new IllegalSignatureException(id.toString()));
                    }
                }
            }

            tx.setVerifiedState();
        }

        return ValidationResult.success;
    }
}
