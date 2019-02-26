package org.eontechology.and.peer.eon.ledger;

import java.io.IOException;
import java.util.Objects;

import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.eon.ledger.state.BalanceProperty;
import org.eontechology.and.peer.eon.ledger.state.ColoredBalanceProperty;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechology.and.peer.eon.ledger.state.GeneratingBalanceProperty;
import org.eontechology.and.peer.eon.ledger.state.RegistrationDataProperty;
import org.eontechology.and.peer.eon.ledger.state.ValidationModeProperty;
import org.eontechology.and.peer.eon.ledger.state.VotePollsProperty;
import org.eontechology.and.peer.eon.ledger.state.serialization.BalancePropertyDeserializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.BalancePropertySerializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.ColoredBalancePropertyDeserializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.ColoredBalancePropertySerializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.ColoredCoinPropertyDeserializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.ColoredCoinPropertySerializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.GeneratingBalancePropertyDeserializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.GeneratingBalancePropertySerializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.RegistrationDataPropertyDeserializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.RegistrationDataPropertySerializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.ValidationModePropertyDeserializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.ValidationModePropertySerializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.VotePollsPropertyDeserializer;
import org.eontechology.and.peer.eon.ledger.state.serialization.VotePollsPropertySerializer;

public class AccountProperties {

    private static AccountPropertyMapper properties =
            new AccountPropertyMapper().addDeserializer(BalanceProperty.class, new BalancePropertyDeserializer())
                                       .addDeserializer(GeneratingBalanceProperty.class,
                                                        new GeneratingBalancePropertyDeserializer())
                                       .addDeserializer(RegistrationDataProperty.class,
                                                        new RegistrationDataPropertyDeserializer())
                                       .addDeserializer(ValidationModeProperty.class,
                                                        new ValidationModePropertyDeserializer())
                                       .addDeserializer(VotePollsProperty.class, new VotePollsPropertyDeserializer())
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
        return AccountProperties.getProperty(account, ValidationModeProperty.class);
    }

    public static VotePollsProperty getVoter(Account account) {
        return AccountProperties.getProperty(account, VotePollsProperty.class);
    }

    public static ColoredCoinProperty getColoredCoin(Account account) {
        return AccountProperties.getProperty(account, ColoredCoinProperty.class);
    }

    public static ColoredBalanceProperty getColoredBalance(Account account) {
        return AccountProperties.getProperty(account, ColoredBalanceProperty.class);
    }
}
