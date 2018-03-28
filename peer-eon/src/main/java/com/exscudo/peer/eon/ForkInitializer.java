package com.exscudo.peer.eon;

import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.eon.tx.CompositeTransactionParser;
import com.exscudo.peer.eon.tx.TransactionHandler;
import com.exscudo.peer.eon.tx.parsers.ColoredCoinPaymentParser;
import com.exscudo.peer.eon.tx.parsers.ColoredCoinRegistrationParser;
import com.exscudo.peer.eon.tx.parsers.ColoredCoinSupplyParser;
import com.exscudo.peer.eon.tx.parsers.DelegateParser;
import com.exscudo.peer.eon.tx.parsers.DepositParser;
import com.exscudo.peer.eon.tx.parsers.PaymentParser;
import com.exscudo.peer.eon.tx.parsers.PublicationParser;
import com.exscudo.peer.eon.tx.parsers.QuorumParser;
import com.exscudo.peer.eon.tx.parsers.RegistrationParser;
import com.exscudo.peer.eon.tx.parsers.RejectionParser;
import com.exscudo.peer.eon.tx.rules.BaseValidationRule;
import com.exscudo.peer.eon.tx.rules.ConfirmationsValidationRule;
import com.exscudo.peer.eon.tx.rules.NoteValidationRule;
import com.exscudo.peer.eon.tx.rules.ReferencedTransactionValidationRule;
import com.exscudo.peer.eon.tx.rules.SignatureValidationRule;
import com.exscudo.peer.eon.tx.rules.VersionValidationRule;

/**
 * Initializing the current fork
 */
public class ForkInitializer {

    public static final Fork.Item[] items = new Fork.Item[] {

            new Fork.Item(1,
                          "2018-01-01T12:00:00.00Z",
                          "2018-03-29T12:00:00.00Z",
                          new TransactionHandler(CompositeTransactionParser.create()
                                                                           .addParser(TransactionType.Registration,
                                                                                      new RegistrationParser())
                                                                           .addParser(TransactionType.Payment,
                                                                                      new PaymentParser())
                                                                           .addParser(TransactionType.Deposit,
                                                                                      new DepositParser())
                                                                           .addParser(TransactionType.Delegate,
                                                                                      new DelegateParser())
                                                                           .addParser(TransactionType.Quorum,
                                                                                      new QuorumParser())
                                                                           .addParser(TransactionType.Rejection,
                                                                                      new RejectionParser())
                                                                           .addParser(TransactionType.Publication,
                                                                                      new PublicationParser())
                                                                           .addParser(TransactionType.ColoredCoinRegistration,
                                                                                      new ColoredCoinRegistrationParser())
                                                                           .addParser(TransactionType.ColoredCoinPayment,
                                                                                      new ColoredCoinPaymentParser())
                                                                           .addParser(TransactionType.ColoredCoinSupply,
                                                                                      new ColoredCoinSupplyParser())
                                                                           .build(), new IValidationRule[] {

                                  new BaseValidationRule(),
                                  new VersionValidationRule(),
                                  new ReferencedTransactionValidationRule(),
                                  new NoteValidationRule(0),
                                  new SignatureValidationRule(),
                                  new ConfirmationsValidationRule()
                          }),
                          1),
            new Fork.Item(2,
                          "2018-03-29T12:00:00.00Z",
                          "2018-04-25T12:00:00.00Z",
                          new TransactionHandler(CompositeTransactionParser.create()
                                                                           .addParser(TransactionType.Registration,
                                                                                      new RegistrationParser())
                                                                           .addParser(TransactionType.Payment,
                                                                                      new PaymentParser())
                                                                           .addParser(TransactionType.Deposit,
                                                                                      new DepositParser())
                                                                           .addParser(TransactionType.Delegate,
                                                                                      new DelegateParser())
                                                                           .addParser(TransactionType.Quorum,
                                                                                      new QuorumParser())
                                                                           .addParser(TransactionType.Rejection,
                                                                                      new RejectionParser())
                                                                           .addParser(TransactionType.Publication,
                                                                                      new PublicationParser())
                                                                           .addParser(TransactionType.ColoredCoinRegistration,
                                                                                      new ColoredCoinRegistrationParser())
                                                                           .addParser(TransactionType.ColoredCoinPayment,
                                                                                      new ColoredCoinPaymentParser())
                                                                           .addParser(TransactionType.ColoredCoinSupply,
                                                                                      new ColoredCoinSupplyParser())
                                                                           .build(), new IValidationRule[] {

                                  new BaseValidationRule(),
                                  new VersionValidationRule(),
                                  new ReferencedTransactionValidationRule(),
                                  new NoteValidationRule(),
                                  new SignatureValidationRule(),
                                  new ConfirmationsValidationRule()
                          }),
                          1),
            };

    public static Fork init(Storage storage) {
        Fork fork = new Fork(storage.metadata().getGenesisBlockID(), items);
        return fork;
    }
}
