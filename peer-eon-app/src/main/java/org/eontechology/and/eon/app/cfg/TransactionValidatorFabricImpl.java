package org.eontechology.and.eon.app.cfg;

import java.util.LinkedList;
import java.util.List;

import org.eontechology.and.eon.app.cfg.forks.ForkItem;
import org.eontechology.and.peer.core.common.IAccountHelper;
import org.eontechology.and.peer.core.common.ITimeProvider;
import org.eontechology.and.peer.core.middleware.IValidationRule;
import org.eontechology.and.peer.core.middleware.TransactionValidator;
import org.eontechology.and.peer.core.middleware.TransactionValidatorFabric;
import org.eontechology.and.peer.core.middleware.rules.ConfirmationsSetValidationRule;
import org.eontechology.and.peer.core.middleware.rules.ConfirmationsValidationRule;
import org.eontechology.and.peer.core.middleware.rules.DeadlineValidationRule;
import org.eontechology.and.peer.core.middleware.rules.EmptyPayerValidationRule;
import org.eontechology.and.peer.core.middleware.rules.ExpiredTimestampValidationRule;
import org.eontechology.and.peer.core.middleware.rules.FeePerByteValidationRule;
import org.eontechology.and.peer.core.middleware.rules.FutureTimestampValidationRule;
import org.eontechology.and.peer.core.middleware.rules.LengthValidationRule;
import org.eontechology.and.peer.core.middleware.rules.NestedTransactionsValidationRule;
import org.eontechology.and.peer.core.middleware.rules.NoteValidationRule;
import org.eontechology.and.peer.core.middleware.rules.PayerValidationRule;
import org.eontechology.and.peer.core.middleware.rules.ReferencedTransactionValidationRule;
import org.eontechology.and.peer.core.middleware.rules.SignatureValidationRule;
import org.eontechology.and.peer.core.middleware.rules.TypeValidationRule;
import org.eontechology.and.peer.core.middleware.rules.VersionValidationRule;

public class TransactionValidatorFabricImpl implements TransactionValidatorFabric {

    private final Fork fork;
    private final IAccountHelper accountHelper;

    public TransactionValidatorFabricImpl(Fork fork, IAccountHelper accountHelper) {

        this.fork = fork;
        this.accountHelper = accountHelper;
    }

    @Override
    public TransactionValidator getAllValidators(ITimeProvider blockProvider) {
        return getAllValidators(blockProvider, blockProvider);
    }

    @Override
    public TransactionValidator getAllValidators(ITimeProvider blockProvider, ITimeProvider peerProvider) {

        ForkItem forkItem = fork.getItem(blockProvider.get());

        List<IValidationRule> rules = new LinkedList<>();
        List<String> names = forkItem.getValidationRules();

        if (names.contains(DeadlineValidationRule.class.getName())) {
            rules.add(new DeadlineValidationRule());
        }

        if (names.contains(LengthValidationRule.class.getName())) {
            rules.add(new LengthValidationRule());
        }

        if (names.contains(ReferencedTransactionValidationRule.class.getName())) {
            rules.add(new ReferencedTransactionValidationRule());
        }

        if (names.contains(FeePerByteValidationRule.class.getName())) {
            rules.add(new FeePerByteValidationRule());
        }

        if (names.contains(VersionValidationRule.class.getName())) {
            rules.add(new VersionValidationRule());
        }

        if (names.contains(NoteValidationRule.class.getName())) {
            rules.add(new NoteValidationRule());
        }

        if (names.contains(PayerValidationRule.class.getName())) {
            rules.add(new PayerValidationRule());
        }

        if (names.contains(EmptyPayerValidationRule.class.getName())) {
            rules.add(new EmptyPayerValidationRule());
        }

        if (names.contains(TypeValidationRule.class.getName())) {
            rules.add(new TypeValidationRule(forkItem.getTransactionTypes()));
        }

        if (names.contains(ExpiredTimestampValidationRule.class.getName())) {
            rules.add(new ExpiredTimestampValidationRule(blockProvider));
        }

        if (names.contains(FutureTimestampValidationRule.class.getName())) {
            rules.add(new FutureTimestampValidationRule(peerProvider));
        }

        if (names.contains(ConfirmationsSetValidationRule.class.getName())) {
            rules.add(new ConfirmationsSetValidationRule(blockProvider, accountHelper));
        }

        if (names.contains(ConfirmationsValidationRule.class.getName())) {
            rules.add(new ConfirmationsValidationRule(blockProvider, accountHelper));
        }

        if (names.contains(SignatureValidationRule.class.getName())) {
            rules.add(new SignatureValidationRule(blockProvider, accountHelper));
        }

        if (names.contains(NestedTransactionsValidationRule.class.getName())) {
            rules.add(new NestedTransactionsValidationRule(forkItem.getTransactionTypes(),
                                                           blockProvider,
                                                           accountHelper));
        }

        if (rules.size() != names.size()) {
            throw new ClassCastException();
        }
        return new TransactionValidator(rules.toArray(new IValidationRule[0]));
    }
}
