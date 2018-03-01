package com.exscudo.peer.eon.tx.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.actions.ColoredCoinRegistrationAction;
import com.exscudo.peer.eon.ledger.actions.FeePaymentAction;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.tx.ITransactionParser;

public class ColoredCoinRegistrationParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 2) {
            throw new ValidateException("Attachment of unknown type.");
        }

        long moneySupply;
        try {
            moneySupply = Long.parseLong(String.valueOf(data.get("emission")));
        } catch (NumberFormatException e) {
            throw new ValidateException("The 'emission' field value has a unsupported format.");
        }
        if (moneySupply < ColoredCoinProperty.MIN_EMISSION_SIZE) {
            throw new ValidateException("The 'emission' field value out of range.");
        }

        int decimalPoint;
        try {
            decimalPoint = Integer.parseInt(String.valueOf(data.get("decimalPoint")));
        } catch (NumberFormatException e) {
            throw new ValidateException("The 'decimalPoint' field value has a unsupported format.");
        }
        if (decimalPoint < ColoredCoinProperty.MIN_DECIMAL_POINT ||
                decimalPoint > ColoredCoinProperty.MAX_DECIMAL_POINT) {
            throw new ValidateException("The 'decimalPoint' field value is out of range.");
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()),
                new ColoredCoinRegistrationAction(transaction.getSenderID(), moneySupply, decimalPoint)
        };
    }
}
