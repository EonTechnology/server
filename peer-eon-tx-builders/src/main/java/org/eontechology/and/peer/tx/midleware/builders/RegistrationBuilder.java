package org.eontechology.and.peer.tx.midleware.builders;

import org.eontechology.and.peer.core.common.Format;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.tx.TransactionType;

/**
 * "Registration" transaction.
 * <p>
 * Publication of the public key. The public key is used to form the recipient
 * field.
 */
public class RegistrationBuilder extends TransactionBuilder<RegistrationBuilder> {

    private RegistrationBuilder() {
        super(TransactionType.Registration);
    }

    public static RegistrationBuilder createNew(byte[] publicKey) {
        AccountID id = new AccountID(publicKey);
        return new RegistrationBuilder().withParam(id.toString(), Format.convert(publicKey));
    }
}
