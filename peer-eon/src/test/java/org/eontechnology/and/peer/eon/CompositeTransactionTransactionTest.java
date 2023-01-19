package org.eontechnology.and.peer.eon;

import java.util.Collection;
import java.util.HashMap;
import org.eontechnology.and.peer.Signer;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.eon.midleware.CompositeTransactionParser;
import org.eontechnology.and.peer.eon.midleware.Resources;
import org.junit.Before;
import org.junit.Test;

public class CompositeTransactionTransactionTest extends AbstractTransactionTest {

  private CompositeTransactionParser parser =
      CompositeTransactionParser.create()
          .addParser(
              1,
              new ITransactionParser() {
                @Override
                public ILedgerAction[] parse(Transaction transaction) throws ValidateException {
                  return new ILedgerAction[0];
                }

                @Override
                public Collection<AccountID> getDependencies(Transaction transaction)
                    throws ValidateException {
                  return null;
                }
              })
          .build();

  private Transaction tx;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void unknown_transaction_type() throws Exception {
    expectedException.expect(ValidateException.class);
    expectedException.expectMessage(Resources.TRANSACTION_TYPE_UNKNOWN);

    ISigner signer = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");

    tx = new Transaction();
    tx.setType(10);
    tx.setVersion(1);
    tx.setTimestamp(timeProvider.get());
    tx.setDeadline(3600);
    tx.setReference(null);
    tx.setSenderID(new AccountID(signer.getPublicKey()));
    tx.setFee(1L);
    tx.setData(new HashMap<>());

    byte[] signature = signer.sign(tx, new BlockID(0L));
    tx.setSignature(signature);

    validate(parser, tx);
  }

  @Test
  public void success() throws Exception {

    ISigner signer = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");

    tx = new Transaction();
    tx.setType(1);
    tx.setVersion(1);
    tx.setTimestamp(timeProvider.get());
    tx.setDeadline(3600);
    tx.setReference(null);
    tx.setSenderID(new AccountID(signer.getPublicKey()));
    tx.setFee(1L);
    tx.setData(new HashMap<>());

    byte[] signature = signer.sign(tx, new BlockID(0L));
    tx.setSignature(signature);

    validate(parser, tx);
  }
}
