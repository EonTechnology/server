package com.exscudo.peer.eon.tx.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.eon.ColoredCoinID;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.actions.ColoredCoinSupplyAction;
import com.exscudo.peer.eon.ledger.actions.FeePaymentAction;
import com.exscudo.peer.eon.tx.ITransactionParser;

public class ColoredCoinSupplyParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {
        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 1) {
            throw new ValidateException("Attachment of unknown type.");
        }

        long newMoneySupply;
        try {
            newMoneySupply = Long.parseLong(String.valueOf(data.get("moneySupply")));
        } catch (NumberFormatException e) {
            throw new ValidateException("The 'moneySupply' field value has a unsupported format.");
        }
        if (newMoneySupply < 0) {
            throw new ValidateException("The 'moneySupply' field value is out of range.");
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()),
                new ColoredCoinSupplyAction(new ColoredCoinID(transaction.getSenderID().getValue()), newMoneySupply)
        };
    }
}
