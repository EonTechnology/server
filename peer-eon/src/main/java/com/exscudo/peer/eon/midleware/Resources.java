package com.exscudo.peer.eon.midleware;

public class Resources {

    // Parsers
    public final static String ATTACHMENT_UNKNOWN_TYPE = "Attachment of unknown type.";
    public final static String TRANSACTION_TYPE_UNKNOWN = "Unknown transaction type.";
    public final static String TRANSACTION_TYPE_INVALID_FORMAT = "Transaction type has unsupported format.";
    public final static String EMISSION_INVALID_FORMAT = "The 'emission' field value has an unsupported format.";
    public final static String EMISSION_OUT_OF_RANGE = "The 'emission' field value out of range.";
    public final static String DECIMAL_POINT_INVALID_FORMAT =
            "The 'decimalPoint' field value has an unsupported format.";
    public final static String DECIMAL_POINT_OUT_OF_RANGE = "The 'decimalPoint' field value is out of range.";
    public final static String MONEY_SUPPLY_INVALID_FORMAT = "The 'moneySupply' field value has an unsupported format.";
    public final static String MONEY_SUPPLY_OUT_OF_RANGE = "The 'moneySupply' field value is out of range.";
    public final static String WEIGHT_INVALID_FORMAT = "The 'weight' field value has an unsupported format.";
    public final static String WEIGHT_OUT_OF_RANGE = "The 'weight' field value is out of range.";
    public final static String AMOUNT_INVALID_FORMAT = "The 'amount' field value has an unsupported format.";
    public final static String AMOUNT_OUT_OF_RANGE = "The 'amount' field value is out of range.";
    public final static String QUORUM_INVALID_FORMAT = "The 'quorum' field value has an unsupported format.";
    public final static String QUORUM_OUT_OF_RANGE = "The 'quorum' field value is out of range.";
    public final static String QUORUM_ILLEGAL_USAGE = "Incorrect setting by type for quorums.";
    public final static String RECIPIENT_INVALID_FORMAT = "The recipient format is not supported.";
    public final static String COLOR_INVALID_FORMAT = "The 'color' field value has an unsupported format.";
    public final static String ACCOUNT_ID_INVALID_FORMAT = "Account ID format is not supported.";
    public final static String ACCOUNT_ID_NOT_MATCH_DATA = "Account ID is not match the data";
    public final static String SEED_NOT_SPECIFIED = "The 'seed' field is not specified.";
    public final static String NESTED_TRANSACTION_NOT_SUPPORTED = "The nested transactions is not supported.";
    public final static String NESTED_TRANSACTION_ILLEGAL_USAGE = "Illegal usage of nested transactions.";
    public final static String NESTED_TRANSACTION_INVALID_LC = "Unable to use L/C.";
    public final static String NESTED_TRANSACTION_SEQUENCE_NOT_FOUND = "The start point of the sequence was not found.";
    public final static String NESTED_TRANSACTION_ILLEGAL_SEQUENCE = "Invalid sequence. Multiple start items.";
    public final static String NESTED_TRANSACTION_UNACCEPTABLE_PARAMS =
            "There are transactions with invalid parameters.";
    public final static String NESTED_TRANSACTION_PAYER_ERROR = "Invalid sequence. Payer not specified.";
    public final static String NESTED_TRANSACTION_PAYER_SEQUENCE_ERROR =
            "Invalid sequence. Referenced transaction with payer.";

    // Actions
    public final static String ACCOUNT_ALREADY_EXISTS = "Account has already existed.";
    public final static String SENDER_ACCOUNT_NOT_FOUND = "Unknown sender.";
    public final static String PAYER_ACCOUNT_NOT_FOUND = "Unknown fee payer.";
    public final static String RECIPIENT_ACCOUNT_NOT_FOUND = "Unknown recipient.";
    public final static String NOT_ENOUGH_FUNDS = "Not enough funds.";
    public final static String NOT_ENOUGH_FEE = "Not enough funds for fee.";
    public final static String VALUE_ALREADY_SET = "Value has already setted.";
    public static final String TOO_MACH_SIZE = "The number has reached the limit.";
    public final static String COLORED_COIN_ACCOUNT_NOT_FOUND = "Unknown colored coin.";
    public final static String COLORED_COIN_ALREADY_EXISTS = "Account has been already associated with the color coin.";
    public final static String COLORED_COIN_NOT_EXISTS = "The colored coin has not been associated with the account.";
    public final static String COLORED_COIN_NOT_ENOUGH_FUNDS = "Insufficient number of colored coins on the balance.";
    public final static String COLORED_COIN_INCOMPLETE_MONEY_SUPPLY =
            "The entire amount of funds must be on the balance.";
    public final static String PRE_PUBLIC_ACCOUNT_CANNOT_CONFIRM = "Account %s can not confirm a transaction.";
    public final static String PUBLIC_ACCOUNT_PROHIBITED_ACTION = "Action is forbidden for public account.";
    public final static String PUBLIC_ACCOUNT_INVALID_SEED = "Invalid seed.";
    public final static String PUBLIC_ACCOUNT_SEED_NOT_MATCH =
            "The seed for sender account must be specified in attachment.";
    public final static String PUBLIC_ACCOUNT_INVALID_WEIGHT =
            "Illegal validation mode. Do not use this seed more for personal operations.";
    public final static String PUBLIC_ACCOUNT_PARTICIPATES_IN_VOTE_POLLS =
            "A public account must not confirm transactions of other accounts. Do not use this seed more for personal operations.";
    public final static String PUBLIC_ACCOUNT_RECENTLY_CHANGED =
            "The confirmation mode has been changed less than the 24 hours period. Do not use this seed more for personal operations.";
    public final static String DELEGATE_ACCOUNT_NOT_FOUND = "Delegate account not found.";
    public static final String TARGET_ACCOUNT_NOT_FOUND = "Unknown target account.";
    public static final String REJECTION_NOT_POSSIBLE = "Rejection is not possible.";
    public static final String ACCOUNT_NOT_IN_VOTE_POLL = "Account does not participate in transaction confirmation.";
    public static final String VOTES_INCORRECT_DISTRIBUTION = "Incorrect distribution of votes.";
    public static final String QUORUM_CAN_NOT_BE_CHANGED = "Unable to set quorum.";
    public static final String QUORUM_FOR_TYPE_CAN_NOT_BE_CHANGED = "Unable to set quorum for transaction type";
}
