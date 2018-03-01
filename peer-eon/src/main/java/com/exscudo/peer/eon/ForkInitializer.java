package com.exscudo.peer.eon;

import com.exscudo.peer.core.TransactionType;
import com.exscudo.peer.core.blockchain.BlockchainService;
import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.eon.tx.CompositeTransactionParser;
import com.exscudo.peer.eon.tx.TransactionHandler;
import com.exscudo.peer.eon.tx.parsers.*;
import com.exscudo.peer.eon.tx.rules.*;

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
                                  new SignatureValidationRule(),
                                  new ReferencedTransactionValidationRule(),
                                  new ConfirmationsValidationRule()
                          }),
                          1)
    };

    public static Fork init(BlockchainService storage) {
        Fork fork = new Fork(storage.getGenesisBlockID(), items);
        return fork;
    }
}
