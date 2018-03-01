package com.exscudo.peer.eon.ledger.actions;

import java.util.Arrays;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ed25519.Ed25519Signer;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.ledger.state.VotePollsProperty;

public class PublicationAction implements ILedgerAction {
    private final AccountID accountID;
    private final String seed;

    public PublicationAction(AccountID accountID, String seed) {
        this.accountID = accountID;
        this.seed = seed;
    }

    private ValidationResult canRun(ILedger ledger, TransactionContext context) {
        Account sender = ledger.getAccount(accountID);
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }

        try {

            byte[] publicKey = new Ed25519Signer(seed).getPublicKey();
            RegistrationDataProperty registration = AccountProperties.getRegistration(sender);

            if (!Arrays.equals(registration.getPublicKey(), publicKey)) {
                return ValidationResult.error("Seed for sender account must be specified in attachment.");
            }
        } catch (Exception e) {
            return ValidationResult.error("Invalid seed.");
        }

        ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);
        if (validationMode.isPublic()) {
            return ValidationResult.error("Already public.");
        }
        if (validationMode.getBaseWeight() != ValidationModeProperty.MIN_WEIGHT ||
                validationMode.getMaxWeight() == validationMode.getBaseWeight()) {
            return ValidationResult.error("Illegal validation mode. Do not use this seed more for personal operations.");
        }

        VotePollsProperty voter = AccountProperties.getVoter(sender);
        if (voter.hasPolls()) {
            return ValidationResult.error("A public account must not confirm transactions of other accounts." +
                                                  " Do not use this seed more for personal operations.");
        }

        int timestamp = context.getTimestamp() - Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
        if (validationMode.getTimestamp() > timestamp || voter.getTimestamp() > timestamp) {
            return ValidationResult.error("The confirmation mode were changed earlier than a day ago." +
                                                  " Do not use this seed more for personal operations.");
        }

        return ValidationResult.success;
    }

    @Override
    public ILedger run(ILedger ledger, TransactionContext context) throws ValidateException {

        ValidationResult r = canRun(ledger, context);
        if (r.hasError) {
            throw r.cause;
        }

        Account sender = ledger.getAccount(accountID);
        ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);
        validationMode.setPublicMode(seed);
        validationMode.setTimestamp(context.getTimestamp());

        sender = AccountProperties.setProperty(sender, validationMode);
        ILedger newLedger = ledger.putAccount(sender);

        return newLedger;
    }
}
