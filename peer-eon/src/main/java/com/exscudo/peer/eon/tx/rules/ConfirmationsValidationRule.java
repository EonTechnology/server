package com.exscudo.peer.eon.tx.rules;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;

public class ConfirmationsValidationRule implements IValidationRule {

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

        Account sender = ledger.getAccount(tx.getSenderID());
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }

        ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);
        if (validationMode.isMultiFactor()) {

            // check signatures
            Set<AccountID> set = new HashSet<>();
            set.add(tx.getSenderID());
            if (tx.getConfirmations() != null) {

                if (tx.getConfirmations().size() > ValidationModeProperty.MAX_DELEGATES) {
                    return ValidationResult.error("Invalid use of the confirmation field.");
                }

                byte[] message = tx.getBytes();
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

                    byte[] publicKey = AccountProperties.getRegistration(account).getPublicKey();
                    if (!CryptoProvider.getInstance().verifySignature(message, signature, publicKey)) {
                        return ValidationResult.error(new IllegalSignatureException(id.toString()));
                    }

                    set.add(id);
                }
            }

            // check quorum
            int maxWeight = 0;
            for (AccountID accountID : set) {

                Account account = ledger.getAccount(accountID);
                if (account == null) {
                    return ValidationResult.error("Unknown account " + accountID.toString());
                }

                if (tx.getSenderID().equals(accountID)) {
                    maxWeight += validationMode.getBaseWeight();
                } else if (validationMode.containWeightForAccount(accountID)) {
                    maxWeight += validationMode.getWeightForAccount(accountID);
                } else {
                    return ValidationResult.error("Account '" + accountID.toString() + "' can not sign transaction.");
                }
            }

            if (validationMode.quorumForType(tx.getType()) <= maxWeight ||
                    (maxWeight == validationMode.getMaxWeight() && maxWeight != 0)) {
                return ValidationResult.success;
            }

            return ValidationResult.error("The quorum is not exist.");
        } else {
            if (tx.getConfirmations() != null) {
                return ValidationResult.error("Invalid use of the confirmation field.");
            }
            return ValidationResult.success;
        }
    }
}
