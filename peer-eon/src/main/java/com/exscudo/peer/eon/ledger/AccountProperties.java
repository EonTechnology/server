package com.exscudo.peer.eon.ledger;

import java.io.IOException;
import java.util.Objects;

import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.eon.ledger.state.serialization.*;
import com.exscudo.peer.eon.ledger.state.*;

public class AccountProperties {

    private static AccountPropertyMapper properties =
            new AccountPropertyMapper().addDeserializer(BalanceProperty.class,
                                                        new BalancePropertyDeserializer())
                                       .addDeserializer(GeneratingBalanceProperty.class,
                                                                             new GeneratingBalancePropertyDeserializer())
                                       .addDeserializer(RegistrationDataProperty.class,
                                                                             new RegistrationDataPropertyDeserializer())
                                       .addDeserializer(ValidationModeProperty.class,
                                                                             new ValidationModePropertyDeserializer())
                                       .addDeserializer(VotePollsProperty.class,
                                                                             new VotePollsPropertyDeserializer())
                                       .addDeserializer(ColoredCoinProperty.class,
                                                                             new ColoredCoinPropertyDeserializer())
                                       .addDeserializer(ColoredBalanceProperty.class,
                                                                             new ColoredBalancePropertyDeserializer())
                                       .addSerializer(new BalancePropertySerializer())
                                       .addSerializer(new GeneratingBalancePropertySerializer())
                                       .addSerializer(new RegistrationDataPropertySerializer())
                                       .addSerializer(new ValidationModePropertySerializer())
                                       .addSerializer(new VotePollsPropertySerializer())
                                       .addSerializer(new ColoredCoinPropertySerializer())
                                       .addSerializer(new ColoredBalancePropertySerializer());

    static <TValue> TValue getProperty(Account account, Class<TValue> clazz) {
        AccountPropertyDeserializer deserializer = properties.findDeserializer(clazz);
        if (deserializer == null) {
            throw new UnsupportedOperationException();
        }
        try {
            return clazz.cast(deserializer.deserialize(account));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <TValue> Account setProperty(Account account, TValue value) {
        Objects.requireNonNull(value);

        AccountPropertySerializer<?> serializer = properties.findSerializer(value.getClass());
        if (serializer == null) {
            throw new UnsupportedOperationException();
        }
        try {
            return serializer.serialize(value, account);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static RegistrationDataProperty getRegistration(Account account) {
        return AccountProperties.getProperty(account, RegistrationDataProperty.class);
    }

    public static BalanceProperty getBalance(Account account) {
        return AccountProperties.getProperty(account, BalanceProperty.class);
    }

    public static GeneratingBalanceProperty getDeposit(Account account) {
        return AccountProperties.getProperty(account, GeneratingBalanceProperty.class);
    }

    public static ValidationModeProperty getValidationMode(Account account) {
        return getProperty(account, ValidationModeProperty.class);
    }

    public static VotePollsProperty getVoter(Account account) {
        return getProperty(account, VotePollsProperty.class);
    }

    public static ColoredCoinProperty getColoredCoin(Account account) {
        return AccountProperties.getProperty(account, ColoredCoinProperty.class);
    }

    public static ColoredBalanceProperty getColoredBalance(Account account) {
        return AccountProperties.getProperty(account, ColoredBalanceProperty.class);
    }
}
